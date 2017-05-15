/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.search.SimpleSearch;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Base class for ChatNoir REST API modules.
 */
@ApiModule("_simple")
public class SimpleSearchApiModule extends ApiModuleBase
{
    /**
     * Handle GET request to API endpoint.
     * If GET is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        final String searchQueryString = ChatNoirServlet.getParameter("q", request);
        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            final JSONObject errObj = generateErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Empty search query");
            writeResponse(response, errObj, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String indicesString = getParameter("i", request);
        String[] indices = null;
        if (null != indicesString) {
            indices = indicesString.split(",");
        }

        final SimpleSearch search = new SimpleSearch(indices);

        int from  = 0;
        int size  = 10;

        final String fromStr = ChatNoirServlet.getParameter("from", request);
        if (null != fromStr) {
            try {
                from = Math.max(Integer.parseInt(fromStr), 1);
            } catch (NumberFormatException ignored) { }
        }
        final String sizeStr = ChatNoirServlet.getParameter("size", request);
        if (null != sizeStr) {
            try {
                size = Math.max(Integer.parseInt(sizeStr), 1);
            } catch (NumberFormatException ignored) { }
        }

        final JSONObject responseObj = new JSONObject();

        final long startTime = System.nanoTime();
        search.setExplain(null != ChatNoirServlet.getParameter("explain", request));
        search.doSearch(searchQueryString, from, size);
        final long elapsedTime = System.nanoTime() - startTime;

        final ArrayList<SearchResultBuilder.SearchResult> results = search.getResults();
        final JSONArray resultsJson = new JSONArray();
        for (final SearchResultBuilder.SearchResult result : results) {
            final JSONObject current = new JSONObject();
            current.put("id", result.id());
            current.put("trec_id", result.trecId());
            current.put("link", result.link());
            current.put("title", result.title());
            current.put("snippet", result.snippet());
            final HashMap<String, Object> resMeta = result.metaData();
            for (final String key : resMeta.keySet()) {
                if (key.equals("explanation") && null == resMeta.get(key)) {
                    continue;
                }
                current.put(key, resMeta.get(key));
            }
            resultsJson.put(current);
        }

        final JSONObject searchMeta = new JSONObject();
        searchMeta.put("query_time", Float.parseFloat(String.format("%.3f", elapsedTime * 0.000000001)));
        searchMeta.put("total_results", search.getTotalResultNumber());
        searchMeta.put("searched_indices", search.getEffectiveIndices());

        responseObj.put("results", resultsJson);
        responseObj.put("meta", searchMeta);

        writeResponse(response, responseObj);
    }

    /**
     * Handle POST request to API endpoint.
     * If POST is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        rejectMethod(response);
    }
}