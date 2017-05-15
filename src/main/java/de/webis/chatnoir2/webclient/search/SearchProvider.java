/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Public interface for search providers.
 *
 * @author Janek Bevendorff
 */
public abstract class SearchProvider extends IndexRetrievalOperator
{
    public SearchProvider(final String[] indices)
    {
        super(indices);
    }

    public SearchProvider()
    {
        this(null);
    }

    /**
     * Run a search based on given search fields.
     *
     * @param query search query
     * @param from first result to return
     * @param size number of results to return
     */
    public abstract void doSearch(String query, int from, int size);

    /**
     * Return a list of SearchResult objects for the executed search.
     *
     * @return list of search results
     */
    public abstract ArrayList<SearchResultBuilder.SearchResult> getResults();

    /**
     * Get the total number of results found for the last search.
     *
     * @return the number of results
     */
    public abstract long getTotalResultNumber();

    /**
     * Parse an ElasticSearch search result explanation string and return a JSON representation of it.
     * TODO: port this to XContentBuilder
     *
     * @param explanation the raw explanation string
     * @return the parsed JSON
     */
    @SuppressWarnings("unchecked")
    protected JSONArray parseExplanationStringToJson(String explanation)
    {
        final String[] lines = explanation.split("(\n|\r\n)");
        final JSONArray outputJson = new JSONArray();
        final Deque<JSONArray> treeLevels = new ArrayDeque<>();
        treeLevels.push(outputJson);
        int indentLevel = 0;
        for (int i = 0; i < lines.length; ++i) {
            // fix erroneous line breaks before doing anything else
            if (i + 1 < lines.length && -1 == lines[i + 1].indexOf('=')) {
                lines[i + 1] = lines[i] + lines[i + 1];
                ++i;
            }

            final int splitIndex = lines[i].indexOf('=');
            String numberPart = lines[i].substring(0, splitIndex - 1);
            final String descPart = lines[i].substring(splitIndex + 2).trim();
            int numSpaces = numberPart.length();
            numberPart = numberPart.trim();
            numSpaces -= numberPart.length();

            if (indentLevel < numSpaces / 2) {
                final JSONArray newLevel = new JSONArray();
                treeLevels.peek().put(newLevel);
                treeLevels.push(newLevel);
                ++indentLevel;
            }

            while (indentLevel > numSpaces / 2) {
                if (treeLevels.isEmpty()) {
                    break;
                }
                treeLevels.pop();
                --indentLevel;
            }

            final JSONObject tmpObj = new JSONObject();
            tmpObj.put(numberPart, descPart);
            treeLevels.peek().put(tmpObj);
        }

        return outputJson;
    }
}
