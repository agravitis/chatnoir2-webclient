/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.response.Renderer;
import de.webis.chatnoir2.webclient.search.SearchProvider;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.search.SimpleSearch;

/**
 * Search Servlet for Chatnoir 2.
 *
 * @author Janek Bevendorff
 * @version 1
 */
@WebServlet(SearchServlet.ROUTE)
public class SearchServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/search/*";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-search.mustache";

    /**
     * Number results to show per page.
     */
    private int mResultsPerPage = 10;

    /**
     * Initialize servlet.
     */
    @Override
    public void init()
    {
        try {
            // load configuration
            final ConfigLoader loader = ConfigLoader.getInstance();
            mResultsPerPage = loader.getConfig().get("serp").get("pagination").getInteger("results_per_page", mResultsPerPage);
        } catch (IOException | ConfigLoader.ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        super.doGet(request, response);

        final String searchQueryString = getParameter("q", request);
        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.sendRedirect(IndexServlet.ROUTE);
            return;
        }

        final String indicesString = getParameter("i", request);
        final List<String> indices;
        if (null != indicesString) {
            System.out.println(indicesString);
            indices = Arrays.asList(indicesString.split(","));
        } else {
            indices = null;
        }

        final HashMap<String, Object> templateVars = new HashMap<>();
        templateVars.put("search-query", searchQueryString);
        templateVars.put("search-query-urlencoded", URLEncoder.encode(searchQueryString, "UTF-8"));
        templateVars.put("page-url", request.getAttribute("javax.servlet.forward.request_uri"));
        templateVars.put("query-string", request.getAttribute("javax.servlet.forward.query_string"));

        final SimpleSearch search = new SimpleSearch(indices);
        if (null != indices) {
            templateVars.put("indices", search.getEffectiveIndexList());
            templateVars.put("indices-string", String.join(",", search.getEffectiveIndexList()));
            templateVars.put("indices-urlstring", URLEncoder.encode(String.join(",", search.getEffectiveIndexList()), "UTF-8"));
        }


        int currentPage  = 1;
        long numResults  = 0;

        final String pageNumber = getParameter("p", request);
        if (null != pageNumber) {
            try {
                currentPage = Math.max(Integer.parseInt(pageNumber), 1);
            } catch (NumberFormatException ignored) { }
        }

        final long startTime = System.nanoTime();
        search.setExplain(null != getParameter("explain", request));
        search.doSearch(searchQueryString, (currentPage - 1) * mResultsPerPage, mResultsPerPage);
        final long elapsedTime = System.nanoTime() - startTime;
        templateVars.put("query-time", String.format("%.1fms", elapsedTime * 0.000001));

        numResults = search.getTotalResultNumber();
        final long currentPageCapped = Math.max(1, Math.min((long) Math.ceil((double) numResults / mResultsPerPage), currentPage));

        // if user navigated past last page
        if (currentPage != currentPageCapped) {
            response.sendRedirect(String.format("%s?q=%s&p=%d",
                    request.getAttribute("javax.servlet.forward.request_uri"),
                    URLEncoder.encode(request.getParameter("q"), "UTF-8"),
                    currentPageCapped));
            return;
        }

        final SERPContext serpContext = new SERPContext();
        serpContext.setResults(search.getResults());
        serpContext.setPagination(numResults, mResultsPerPage, currentPage);

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars, serpContext);
    }

    /**
     * Mustache context class for search results page.
     *
     * @author Janek Bevendorff
     */
    public static class SERPContext
    {
        /**
         * The registered individual results.
         */
        private List<SearchResultBuilder.SearchResult> results = new ArrayList<>();

        /**
         * Total number of results.
         */
        private long numResults;

        /**
         * How many results to show per page.
         */
        private int resultsPerPage;

        /**
         * Number of the current page.
         */
        private long currentPage;

        /**
         * Mustache accessor for search results.
         * @return list of search results
         */
        public List<SearchResultBuilder.SearchResult> searchResults()
        {
            return results;
        }

        /**
         * Mustache accessor for pagination.
         * @return hashmap list with pagination info, null if there are 0 results
         */
        public List<HashMap<String, String>> pagination()
        {
            if (0 == numResults) {
                return null;
            }

            final List<HashMap<String, String>> pagination = new ArrayList<>();
            int numPages      = Math.min((int) Math.ceil((double) numResults / resultsPerPage), 1000);
            long displayPages = Math.min(Math.min(currentPage + 5, numPages), 1000);
            long startingPage = Math.max(currentPage - 4, 1);

            // go to first page
            if (5 < currentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pagenumber", Integer.toString(1));
                page.put("ariahiddenlabel", "←");
                page.put("hiddenlabel", "First");
                pagination.add(page);
            }

            // go to previous page
            if (1 != currentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pagenumber", Long.toString(currentPage - 1));
                page.put("ariahiddenlabel", "«");
                page.put("hiddenlabel", "Previous");
                pagination.add(page);
            }

            // page numbers
            for (long i = startingPage; i <= displayPages; ++i) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pagenumber", Long.toString(i));
                page.put("label", Long.toString(i));
                if (i == currentPage) {
                    page.put("active", "1");
                }
                pagination.add(page);
            }

            // go to next page
            if (numPages != currentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pagenumber", Long.toString(currentPage + 1));
                page.put("ariahiddenlabel", "»");
                page.put("hiddenlabel", "Next");
                pagination.add(page);
            }

            return pagination;
        }

        /**
         * Mustache accessor for current page.
         *
         * @return page number
         */
        public long currentPage()
        {
            return currentPage;
        }

        /**
         * Mustache accessor returning true if search results have explanations.
         *
         * @return true if explain mode is on
         */
        public boolean isExplainMode()
        {
            return (0 != results.size()) && ((Boolean) results.get(0).metaData().get("has_explanation"));
        }

        /**
         * Mustache accessor for general pagination info.
         * @return meta information for pagination
         */
        public HashMap<String, String> paginationInfo()
        {
            HashMap<String, String> paginationInfo = new HashMap<>();
            paginationInfo.put("current-page", Long.toString(currentPage));
            paginationInfo.put("results-range-start", Long.toString(1 + (currentPage - 1) * resultsPerPage));
            paginationInfo.put("results-range-end", Long.toString(Math.min((currentPage - 1) * resultsPerPage + resultsPerPage, numResults)));
            paginationInfo.put("num-results", Long.toString(numResults));

            return paginationInfo;
        }

        /**
         * Mustache accessor for whether there are any search results.
         */
        public boolean resultsFound()
        {
            return numResults != 0;
        }

        /**
         * Add search result.
         *
         * @param result the result
         */
        public void addResult(final SearchResultBuilder.SearchResult result)
        {
            results.add(result);
        }

        /**
         * Set search results. Overwrites any existing results
         *
         * @param results the result
         */
        public void setResults(final ArrayList<SearchResultBuilder.SearchResult> results)
        {
            this.results = results;
        }

        /**
         * Set pagination info.
         *
         * @param numResults     total number of results
         * @param resultsPerPage number of results to show per page
         * @param currentPage    the current page
         */
        public void setPagination(final long numResults, final int resultsPerPage, final long currentPage)
        {
            this.numResults     = numResults;
            this.resultsPerPage = resultsPerPage;
            this.currentPage    = Math.min(currentPage, 1000);
        }
    }
}
