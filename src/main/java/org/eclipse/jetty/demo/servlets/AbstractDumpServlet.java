//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.demo.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractDumpServlet extends HttpServlet
{
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        out.printf("%s:%n", this.getClass().getSimpleName());
        out.println("method=" + request.getMethod());
        out.println("dispatcherType=" + request.getDispatcherType());
        out.println("requestURI=" + request.getRequestURI());
        out.println("requestURL=" + request.getRequestURL().toString());
        out.println("contextPath=" + request.getContextPath());
        out.println("servletPath=" + request.getServletPath());
        out.println("pathInfo=" + request.getPathInfo());
        out.printf("remote=%s:%d%n", request.getRemoteAddr(), request.getRemotePort());
        HttpSession session = request.getSession(false);
        if (session == null)
        {
            out.println("session=<null>");
        }
        else
        {
            out.println("session=" + session.getId());
        }

        Collections.list(request.getHeaderNames())
            .forEach((name) ->
                out.printf("request.header[%s]=%s%n", name, request.getHeader(name)));

        ServletContext servletContext = getServletContext();

        Collections.list(request.getParameterNames())
            .stream()
            .sorted()
            .forEach((name) ->
                out.printf("request.parameter[%s]=%s%n", name, request.getParameter(name)));

        Collections.list(request.getAttributeNames())
            .stream()
            .sorted()
            .forEach((name) ->
                out.println("request.attribute[" + name + "]=" + request.getAttribute(name)));

        Collections.list(servletContext.getAttributeNames())
            .stream()
            .sorted()
            .forEach((name) ->
                out.println("servletContext.attribute[" + name + "]=" + servletContext.getAttribute(name)));
    }
}
