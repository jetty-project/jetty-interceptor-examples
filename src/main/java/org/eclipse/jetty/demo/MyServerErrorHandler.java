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

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Context-less error handling, such as what might occur from a raw Handler.
 */
public class MyServerErrorHandler extends ErrorHandler
{
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        baseRequest.setHandled(true);
        if (request.getDispatcherType() != DispatcherType.ERROR)
        {
            // don't support direct access to this servlet
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        Throwable exception = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        ServletOutputStream out = response.getOutputStream();
        PrintWriter writer = new PrintWriter(out, true, UTF_8);
        writer.printf("ERROR: (%s) %s%n", exception.getClass().getName(), exception.getMessage());
        exception.printStackTrace(writer);
    }
}
