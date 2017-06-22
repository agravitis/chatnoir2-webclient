/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.api.exceptions.NotImplementedException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST API Servlet for ChatNoir 2.
 */
@WebServlet(ApiServlet.ROUTE)
public class ApiServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/api/*";

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        ApiModuleBase apiHandler = ApiBootstrap.bootstrapApiModule(request, response);

        // remove cookie header
        response.setHeader("Set-Cookie", null);

        String requestMethod = request.getMethod().toUpperCase();
        switch (requestMethod) {
            case "HEAD":
                doHead(request, response);
                break;
            case "GET":
            case "POST":
                apiHandler.service(request, response);
                break;
            case "OPTIONS":
                doOptions(request, response);
                response.getWriter().flush();
                break;
            default:
                throw new NotImplementedException("Method not implemented");
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
    }

    @Override
    protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Allow", "HEAD,GET,POST,OPTIONS");
    }
}
