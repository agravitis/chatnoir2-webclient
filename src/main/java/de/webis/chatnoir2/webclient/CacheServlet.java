/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import de.webis.chatnoir2.webclient.hdfs.MapFileReader;
import de.webis.chatnoir2.webclient.response.Renderer;
import de.webis.chatnoir2.webclient.search.DocumentRetriever;
import de.webis.chatnoir2.webclient.util.PlainTextRenderer;

/**
 * Index Servlet for Chatnoir 2.
 *
 * @author Janek Bevendorff
 * @version 1
 */
@WebServlet(CacheServlet.ROUTE)
public class CacheServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/cache";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-cache.mustache";

    private static final String TEMPLATE_REDIRECT = "/templates/chatnoir2-cache-redirect.mustache";

    @Override
    public void init() throws ServletException
    {
        super.init();
        if (!MapFileReader.isInitialized())
            MapFileReader.init();
    }

    /**
     * GET action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        String uuidParam  = request.getParameter("uuid");
        String indexParam = request.getParameter("index");
        String uriParam   = request.getParameter("uri");
        if (null == uuidParam && uriParam == null) {
            getServletContext().getRequestDispatcher(SearchServlet.ROUTE).forward(request, response);
            return;
        }

        final DocumentRetriever retriever = new DocumentRetriever(true, true);

        if (null == indexParam) {
            String[] effectiveIndices = retriever.getEffectiveIndices();
            if (effectiveIndices.length == 0) {
                forwardError(request, response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            indexParam = effectiveIndices[0];
        }

        if (!retriever.isIndexAllowed(indexParam)) {
            forwardError(request, response, HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // rendering modes
        final boolean rawMode = (null != request.getParameter("raw"));
        final boolean plainTextMode = (null != request.getParameter("plain"));

        DocumentRetriever.Document doc = null;
        if (null != uuidParam) {
            // first try direct retrieval by UUID
            try {
                doc = retriever.getByUUID(indexParam, UUID.fromString(uuidParam));
            } catch (IllegalArgumentException ignored) {}

            // if document not found, try retrieval by Elasticsearch document ID
            if (null == doc) {
                doc = retriever.getByIndexDocID(indexParam, uuidParam);

                if (null == doc) {
                    forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } else {
            // retrieval by URI
            doc = retriever.getByURI(indexParam, uriParam);

            // redirect into the open web if no cache entry found
            if (null == doc) {
                final HashMap<String, String> templateVars = new HashMap<>();
                templateVars.put("uri", uriParam);
                Renderer.render(getServletContext(), request, response, TEMPLATE_REDIRECT, templateVars);
                return;
            }
        }

        // raw output without frame
        if (rawMode) {
            if (plainTextMode) {
                String plainText = PlainTextRenderer.getBasicHtml(doc.getBody());
                response.setContentType("text/html");
                response.getWriter().print(plainText);
                return;
            } else {
                response.setContentType("text/html");
                response.getWriter().print(doc.getBody());
                return;
            }
        }

        // else: show framed page
        final HashMap<String, String> templateVars = new HashMap<>();
        templateVars.put("uuid", doc.getDocUUID().toString());
        templateVars.put("uuidUrlEnc", URLEncoder.encode(doc.getDocUUID().toString(), "UTF-8"));
        templateVars.put("origUri", doc.getTargetURI());
        templateVars.put("index", URLEncoder.encode(indexParam, "UTF-8"));
        if (plainTextMode) {
            templateVars.put("plainTextMode", "1");
        }
        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars);
    }
}
