/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Search result DTO builder.
 *
 * @author Janek Bevendorff
 * @version 1
 */
public class SearchResultBuilder
{
    /**
     * {@link de.webis.chatnoir2.webclient.search.SearchResultBuilder.SearchResult} to build.
     */
    protected final SearchResult searchResult;

    public SearchResultBuilder()
    {
        searchResult = new SearchResult();
    }

    /**
     * Get built {@link de.webis.chatnoir2.webclient.search.SearchResultBuilder.SearchResult}.
     *
     * @return SearchResult
     */
    public SearchResult build() {
        return searchResult;
    }

    public SearchResultBuilder id(final String id)
    {
        searchResult.id = id;
        return this;
    }

    public SearchResultBuilder trecId(final String trecId)
    {
        searchResult.trecId = trecId;
        return this;
    }

    public SearchResultBuilder title(final String title)
    {
        searchResult.title = title;
        return this;
    }

    public SearchResultBuilder link(final String link)
    {
        searchResult.link = link;
        return this;
    }

    public SearchResultBuilder snippet(final String snippet)
    {
        searchResult.snippet = snippet;
        return this;
    }

    public SearchResultBuilder fullBody(final String fullBody)
    {
        searchResult.fullBody = fullBody;
        return this;
    }

    public SearchResultBuilder rawHTML(final String rawHTML)
    {
        searchResult.rawHTML = rawHTML;
        return this;
    }

    public SearchResultBuilder addMetadata(final String key, final Object value)
    {
        searchResult.metadata.put(key, value);
        return this;
    }

    public SearchResultBuilder suggestGrouping(final boolean group)
    {
        searchResult.groupingSuggested = group;
        return this;
    }

    /**
     * Search result DTO.
     */
    public class SearchResult
    {
        protected String id = "";
        protected String trecId = "";
        protected String title = "";
        protected String link = "";
        protected String snippet = "";
        protected String fullBody = "";
        protected String rawHTML = "";
        protected boolean groupingSuggested = false;
        protected final HashMap<String, Object> metadata = new HashMap<>();

        public String id()
        {
            return id;
        }

        public String trecId()
        {
            return trecId;
        }

        public String title()
        {
            return title;
        }

        public String link()
        {
            return link;
        }

        public String snippet()
        {
            return snippet;
        }

        public String fullBody()
        {
            return fullBody;
        }

        public String rawHTML()
        {
            return rawHTML;
        }

        public boolean groupingSuggested() { return groupingSuggested; }

        public HashMap<String, Object> metaData()
        {
            return metadata;
        }

        public String hostName()
        {
            try {
                return new URI(this.link).getHost();
            } catch (URISyntaxException e) {
                return "unknown";
            }
        }
    }
}