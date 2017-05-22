/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.TextCleanser;
import org.apache.commons.lang.StringEscapeUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescoreMode;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;

import java.nio.file.Paths;
import java.util.*;


/**
 * Provider for simple universal search.
 *
 * @author Janek Bevendorff
 */
public class SimpleSearch extends SearchProvider
{
    /**
     * (Default) snippet length.
     */
    private int mSnippetLength = 200;

    /**
     * (Default) title length.
     */
    private int mTitleLength = 60;

    /**
     * Whether to explain query.
     */
    private boolean mDoExplain = false;

    /**
     * Search mResponse of the last search.
     */
    private SearchResponse mResponse = new SearchResponse();

    /**
     * Config object shortcut.
     */
    private final ConfigLoader.Config mSimpleSearchConfig;

    public SimpleSearch(final String[] indices)
    {
        super(indices);

        mSnippetLength = getConf().getInteger("serp.snippet_length", mSnippetLength);
        mTitleLength   = getConf().getInteger("serp.title_length", mTitleLength);
        mSimpleSearchConfig = getConf().get("search.default_simple");
    }

    public SimpleSearch()
    {
        this(null);
    }

    /**
     * Set whether to explain search queries.
     *
     * @param doExplain true if query shall be explained
     */
    public void setExplain(final boolean doExplain) {
        this.mDoExplain = doExplain;
    }

    /**
     * Perform a simple search.
     */
    @Override
    public void doSearch(String query, int from, int size)
    {
        StringBuffer queryBuffer = new StringBuffer(query);

        QueryBuilder preQuery = buildPreQuery(queryBuffer);
        QueryBuilder rescoreQuery = buildRescoreQuery(queryBuffer);

        mResponse = getClient()
                .prepareSearch(getEffectiveIndices())
                .setQuery(preQuery)
                .setRescorer(buildRescorer(rescoreQuery),
                        mSimpleSearchConfig.getInteger("rescore_window", size))
                .setFrom(from)
                .setSize(size)
                .highlighter(new HighlightBuilder()
                        .field("title_lang." + getSearchLanguage(), mTitleLength, 1)
                        .field("body_lang." + getSearchLanguage(), mSnippetLength, 1)
                        .encoder("html"))
                .setExplain(mDoExplain)
                .setTerminateAfter(mSimpleSearchConfig.getInteger("node_limit", 200000))
                .setProfile(false)
                .get();
    }

    @Override
    public List<SearchResultBuilder.SearchResult> getResults()
    {
        List<SearchResultBuilder.SearchResult> results = new ArrayList<>();

        for (final SearchHit hit : mResponse.getHits()) {
            final Map<String, Object> source = hit.getSource();

            String snippet = "";
            if (null != hit.getHighlightFields().get("body_lang." + getSearchLanguage())) {
                final Text[] fragments = hit.getHighlightFields().get("body_lang." + getSearchLanguage()).fragments();
                if (1 >= fragments.length) {
                    snippet = fragments[0].string();
                }
            }

            // use meta description or first body part if no highlighted snippet available
            if (snippet.isEmpty()) {
                if (!source.get("meta_desc_lang." + getSearchLanguage()).toString().isEmpty()) {
                    snippet = StringEscapeUtils.escapeHtml(
                            truncateSnippet((String) source.get("meta_desc_lang." + getSearchLanguage()), mSnippetLength));
                } else {
                    snippet = StringEscapeUtils.escapeHtml(
                            truncateSnippet((String) source.get("body_lang." + getSearchLanguage()), mSnippetLength));
                }
            }
            snippet = TextCleanser.cleanseAll(snippet, true);

            // use highlighted title if available
            String title = StringEscapeUtils.escapeHtml(
                    truncateSnippet((String) source.get("title_lang." + getSearchLanguage()), mTitleLength));
            if (null != hit.getHighlightFields().get("title_lang." + getSearchLanguage())) {
                final Text[] fragments = hit.getHighlightFields().get("title_lang." + getSearchLanguage()).fragments();
                if (1 >= fragments.length) {
                    title = fragments[0].string();
                }
            }
            title = TextCleanser.cleanseAll(title, true);

            String targetPath = (String) source.get("warc_target_path");
            if (null != targetPath) {
                targetPath = Paths.get("/", targetPath).normalize().toString();
            } else {
                targetPath = "/";
            }

            final SearchResultBuilder.SearchResult result = new SearchResultBuilder()
                    .score(hit.getScore())
                    .index(hit.index())
                    .documentId(hit.getId())
                    .trecId((String) source.get("warc_trec_id"))
                    .title(title)
                    .targetHostname((String) source.get("warc_target_hostname"))
                    .targetPath(targetPath)
                    .targetUri((String) source.get("warc_target_uri"))
                    .snippet(snippet)
                    .fullBody((String) source.get("body_lang." + getSearchLanguage()))
                    .pageRank((Double) source.get("page_rank"))
                    .spamRank((Integer) source.get("spam_rank"))
                    .explanation(hit.explanation())
                    .build();
            results.add(result);
        }

        if (isGroupByHostname()) {
            results = groupResults(results);
        }

        return results;
    }

    @Override
    public long getTotalResultNumber()
    {
        return mResponse.getHits().getTotalHits();
    }

    /**
     * Assemble the fast pre-query for use with a rescorer.
     *
     * @return assembled pre-query
     */
    private QueryBuilder buildPreQuery(StringBuffer userQueryString)
    {
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        // parse query string filters
        QueryBuilder queryStringFilter = parseQueryStringFilters(userQueryString);
        if (null != queryStringFilter) {
            mainQuery.filter(queryStringFilter);
        }

        mainQuery.filter(QueryBuilders.termQuery("lang", getSearchLanguage()));

        if (!userQueryString.toString().trim().isEmpty()) {
            final SimpleQueryStringBuilder searchQuery = QueryBuilders.simpleQueryStringQuery(userQueryString.toString());
            searchQuery
                    .defaultOperator(Operator.AND)
                    .flags(SimpleQueryStringFlag.AND,
                            SimpleQueryStringFlag.OR,
                            SimpleQueryStringFlag.NOT,
                            SimpleQueryStringFlag.WHITESPACE);

            final ConfigLoader.Config[] mainFields = mSimpleSearchConfig.getArray("main_fields");
            for (final ConfigLoader.Config field : mainFields) {
                searchQuery.field(replaceLocalePlaceholders(field.getString("name", "")));
            }
            mainQuery.must(searchQuery);
        } else {
            MatchAllQueryBuilder searchQuery = QueryBuilders.matchAllQuery();
            mainQuery.must(searchQuery);
        }

        // add range filters (e.g. to filter by minimal content length)
        final ConfigLoader.Config[] rangeFilters = mSimpleSearchConfig.getArray("range_filters");
        for (final ConfigLoader.Config filterConfig : rangeFilters) {
            final RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(
                    replaceLocalePlaceholders(filterConfig.getString("name", "")));

            if (null != filterConfig.getDouble("gt")) {
                rangeFilter.gt(filterConfig.getDouble("gt"));
            }
            if (null != filterConfig.getDouble("gte")) {
                rangeFilter.gte(filterConfig.getDouble("gte"));
            }
            if (null != filterConfig.getDouble("lt")) {
                rangeFilter.lt(filterConfig.getDouble("lt"));
            }
            if (null != filterConfig.getDouble("lte")) {
                rangeFilter.lte(filterConfig.getDouble("lte"));
            }
            final Boolean negate = filterConfig.getBoolean("negate", false);
            if (negate)
                mainQuery.mustNot(rangeFilter);
            else
                mainQuery.filter(rangeFilter);
        }

        // field value boosts
        final ConfigLoader.Config[] boosts = mSimpleSearchConfig.getArray("boosts");
        for (ConfigLoader.Config c: boosts) {
            if (!c.getBoolean("match")) {
                continue;
            }
            RegexpQueryBuilder regExpQuery = QueryBuilders.regexpQuery(
                    replaceLocalePlaceholders(c.getString("name")),
                    replaceLocalePlaceholders(c.getString("value")));
            regExpQuery.boost(c.getFloat("match_boost", 1.0f));
            mainQuery.should(regExpQuery);
        }

        return mainQuery;
    }

    /**
     * Parse configured filters from the query string such as site:example.com
     * and delete the filters from the given query StringBuffer.
     *
     * @param queryString user query string
     * @return filter query
     */
    private QueryBuilder parseQueryStringFilters(StringBuffer queryString)
    {
        ConfigLoader.Config[] filterConf = mSimpleSearchConfig.getArray("query_filters");
        if (filterConf.length == 0) {
            return null;
        }

        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        for (ConfigLoader.Config c: filterConf) {
            String filterKey = c.getString("keyword");
            String filterField = c.getString("field");

            int pos = queryString.indexOf(filterKey + ":");
            if (-1 == pos) {
                continue;
            }

            // do not group results if they are filtered by hostname
            if (filterField.equals("warc_target_hostname.raw")) {
                setGroupByHostname(false);
            }

            int filterStartPos = pos;
            pos +=  filterKey.length() + 1;
            int valueStartPos = pos;
            while (Character.isWhitespace(queryString.charAt(pos))) {
                // skip initial white space
                ++pos;
            }
            while (pos < queryString.length() && !Character.isWhitespace(queryString.charAt(pos))) {
                // walk up to the next white space or string end
                ++pos;
            }
            String filterValue = queryString.substring(valueStartPos, pos).trim();

            // strip filter from query string
            queryString.replace(filterStartPos, pos, "");

            // trim whitespace
            int trimEnd = 0;
            for (int i = 0; i < queryString.length(); ++i) {
                if (!Character.isWhitespace(queryString.charAt(i))) {
                    break;
                }
                ++trimEnd;
            }
            queryString.replace(0, trimEnd, "");
            int trimStart = queryString.length();
            for (int i = queryString.length(); i > 0; --i) {
                if (!Character.isWhitespace(queryString.charAt(i - 1))) {
                    break;
                }
                --trimStart;
            }
            queryString.replace(trimStart, queryString.length(), "");

            // apply filters
            if (!filterField.isEmpty() && !filterField.startsWith("#")) {
                TermQueryBuilder termQuery = QueryBuilders.termQuery(filterField, filterValue);

                if (filterField.equals("lang")) {
                    setSearchLanguage(filterValue);
                }

                filterQuery.filter(termQuery);
            } else if (!filterField.isEmpty()) {
                if (filterField.equals("#index")) {
                    setActiveIndices(filterValue.split(","));
                }
            }
        }

        return filterQuery;
    }

    /**
     * Build query rescorer used to run more expensive query on pre-query results.
     *
     * @param mainQuery query to rescore
     * @return assembled RescoreBuilder
     */
    private QueryRescorerBuilder buildRescorer(final QueryBuilder mainQuery)
    {
        final QueryRescorerBuilder rescorer = RescoreBuilder.queryRescorer(mainQuery);
        rescorer.setQueryWeight(0.0f).
                setRescoreQueryWeight(1.0f).
                setScoreMode(QueryRescoreMode.Total);
        return rescorer;
    }

    /**
     * Assemble the more expensive query for rescoring the results returned by the pre-query.
     *
     * @return rescore query
     */
    private QueryBuilder buildRescoreQuery(StringBuffer userQueryString)
    {
        // parse query string
        final SimpleQueryStringBuilder simpleQuery = QueryBuilders.simpleQueryStringQuery(userQueryString.toString());
        simpleQuery.minimumShouldMatch("30%");

        final ConfigLoader.Config[] mainFields = mSimpleSearchConfig.getArray("main_fields");

        final List<Object[]> proximityFields = new ArrayList<>();
        final List<String>   fuzzyFields     = new ArrayList<>();

        for (final ConfigLoader.Config field : mainFields) {
            simpleQuery.field(
                    replaceLocalePlaceholders(field.getString("name", "")),
                    field.getFloat("boost", 1.0f)).
                    defaultOperator(Operator.AND).
                    flags(SimpleQueryStringFlag.AND,
                            SimpleQueryStringFlag.OR,
                            SimpleQueryStringFlag.NOT,
                            SimpleQueryStringFlag.PHRASE,
                            SimpleQueryStringFlag.PREFIX,
                            SimpleQueryStringFlag.WHITESPACE);

            // add field to list of proximity-aware fields for later processing
            if (field.getBoolean("proximity_matching", false)) {
                proximityFields.add(new Object[] {
                        replaceLocalePlaceholders(field.getString("name")),
                        field.getInteger("proximity_slop", 1),
                        field.getFloat("proximity_boost", 1.0f)
                });
            }

            if (field.getBoolean("fuzzy_matching", false)) {
                fuzzyFields.add(field.getString("name"));
            }
        }

        // assemble main query
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        // proximity matching
        for (Object[] o : proximityFields) {
            final MatchPhraseQueryBuilder proximityQuery = QueryBuilders.matchPhraseQuery(
                    replaceLocalePlaceholders((String) o[0]),
                    userQueryString.toString()
            );
            proximityQuery
                    .slop((Integer) o[1])
                    .boost((Float) o[2] / 2.0f);
            mainQuery.should(proximityQuery);
        }

        // fuzzy fields
        for (String f: fuzzyFields) {
            final FuzzyQueryBuilder fuzzyQuery = QueryBuilders.fuzzyQuery(f, userQueryString.toString());
            fuzzyQuery.fuzziness(Fuzziness.AUTO);
            mainQuery.should(fuzzyQuery);
        }

        mainQuery.must(simpleQuery);

        // field value boosts
        final ConfigLoader.Config[] boosts = mSimpleSearchConfig.getArray("boosts");
        for (ConfigLoader.Config c: boosts) {
            RegexpQueryBuilder regExpQuery = QueryBuilders.regexpQuery(
                    replaceLocalePlaceholders(c.getString("name")),
                    replaceLocalePlaceholders(c.getString("value")));
            regExpQuery.boost(c.getFloat("boost", 2.0f));
            mainQuery.should(regExpQuery);
        }

        // we start wrapping the main query here, so we upcast its reference
        QueryBuilder mainQueryGeneral = mainQuery;

        // field value factor function scores
        ConfigLoader.Config[] funcScoreCfgs = mSimpleSearchConfig.getArray("field_value_factors");
        for (ConfigLoader.Config c: funcScoreCfgs) {
            FieldValueFactorFunctionBuilder valueFactor = new FieldValueFactorFunctionBuilder(c.getString("name"));
            valueFactor
                    .factor(c.getFloat("factor", 1.0f))
                    .modifier(FieldValueFactorFunction.Modifier.fromString(c.getString("modifier", "")))
                    .missing(c.getFloat("missing", 1.0f));
            mainQueryGeneral = QueryBuilders.functionScoreQuery(mainQueryGeneral, valueFactor);
        }

        // field value penalties
        final ConfigLoader.Config penalties = mSimpleSearchConfig.get("penalties");
        final ConfigLoader.Config[] penaltyFields = penalties.getArray("fields");
        if (penaltyFields.length > 0) {
            BoolQueryBuilder penaltyQuery = QueryBuilders.boolQuery();
            for (ConfigLoader.Config c: penaltyFields) {
                RegexpQueryBuilder regExpQuery = QueryBuilders.regexpQuery(
                        replaceLocalePlaceholders(c.getString("name")),
                        replaceLocalePlaceholders(c.getString("value")));
                regExpQuery.boost(c.getFloat("boost", 2.0f));
                penaltyQuery.should(regExpQuery);
            }

            BoostingQueryBuilder boostingQuery = QueryBuilders.boostingQuery(mainQuery, penaltyQuery);
            boostingQuery.negativeBoost(penalties.getFloat("penalty_factor", 0.2f));
            mainQueryGeneral = boostingQuery;
        }

        return mainQueryGeneral;
    }

    /**
     * Truncate a snippet after a certain number of characters, trying to preserve full words.
     * Will cut the string hard after the specified amount of characters if no spaces could be
     * found or cutting after words would reduce the size more than 2/3 of the desired length.
     *
     * @param snippet the snippet
     * @param numCharacters number of characters after which to truncate
     * @return the truncated snippet
     */
    private String truncateSnippet(String snippet, final int numCharacters)
    {
        if (snippet.length() > numCharacters) {
            final boolean wordEnded = (snippet.charAt(numCharacters) == ' ');
            snippet = snippet.substring(0, numCharacters);

            // get rid of incomplete words
            final int pos = snippet.lastIndexOf(' ');
            if (!wordEnded && -1 != pos) {
                // shorten snippet if it doesn't become too short then
                if ((int)(.6 * numCharacters) <= pos) {
                    snippet = snippet.substring(0, pos);
                }
            }
        }

        return snippet.trim();
    }
}
