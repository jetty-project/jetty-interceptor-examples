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

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.pathmap.PathSpecSet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.IncludeExclude;

public class MaxRequestSizeHandler extends AbstractHandler
{
    private final long maxSize;
    private final IncludeExclude<String> methods = new IncludeExclude<>();
    private final IncludeExclude<String> paths = new IncludeExclude<>(PathSpecSet.class);

    public MaxRequestSizeHandler(long maxSize)
    {
        this.maxSize = maxSize;
        this.methods.getIncluded().add("POST");
    }

    public void addExcludePath(String path)
    {
        paths.getExcluded().add(path);
    }

    public void addIncludePath(String path)
    {
        paths.getIncluded().add(path);
    }

    protected boolean isLimited(Request request)
    {
        if (!methods.test(request.getMethod()))
            return false;

        String requestURI = request.getRequestURI();
        if (requestURI == null)
            return false;

        return paths.test(requestURI);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
    {
        if (!MaxRequestSizeInterceptor.isIntercepted(baseRequest)
            && baseRequest.getDispatcherType() == DispatcherType.REQUEST
            && isLimited(baseRequest))
        {
            MaxRequestSizeInterceptor.add(baseRequest, maxSize);
        }
    }
}
