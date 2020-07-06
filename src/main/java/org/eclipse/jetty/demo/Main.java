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

package org.eclipse.jetty.demo;

import java.util.EnumSet;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.demo.servlets.DumpServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        Server server = newServer(8080);
        server.start();
        server.join();
    }

    public static Server newServer(int port)
    {
        Server server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        FilterHolder maxRequestSizeFilterHolder = contextHandler.addFilter(MaxRequestSizeFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        maxRequestSizeFilterHolder.setInitParameter("maxSize", "1024");
        contextHandler.addServlet(DumpServlet.class, "/dump/*");
        contextHandler.addServlet(DumpServlet.class, "*.upload");
        contextHandler.addServlet(MaxRequestSizeErrorHandler.class, "/error/max-request-size");

        ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
        errorPageErrorHandler.addErrorPage(MaxRequestSizeExceededException.class, "/error/max-request-size");
        contextHandler.setErrorHandler(errorPageErrorHandler);

        MaxRequestSizeHandler maxRequestSizeHandler = new MaxRequestSizeHandler(1024);
        maxRequestSizeHandler.addIncludePath("*.upload");
        maxRequestSizeHandler.addExcludePath("/dump/*");

        HandlerList handlers = new HandlerList();
        handlers.addHandler(maxRequestSizeHandler);
        handlers.addHandler(contextHandler);
        handlers.addHandler(new DefaultHandler());

        server.setHandler(handlers);
        return server;
    }
}
