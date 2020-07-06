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
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.jetty.server.Request;

public class MaxRequestSizeFilter implements Filter
{
    private long maxSize;

    @Override
    public void init(FilterConfig filterConfig)
    {
        maxSize = getInitInteger(filterConfig, "maxSize", 1024);
    }

    private int getInitInteger(FilterConfig config, String key, int defValue)
    {
        Object val = config.getInitParameter(key);
        if (val == null)
            return defValue;
        try
        {
            return Integer.parseInt(val.toString());
        }
        catch (NumberFormatException e)
        {
            return defValue;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (!MaxRequestSizeInterceptor.isIntercepted(request)
            && request.getDispatcherType() == DispatcherType.REQUEST
            && isCoveredRequest(request))
        {
            Request baseRequest = Request.getBaseRequest(request);
            MaxRequestSizeInterceptor.add(baseRequest, maxSize);
        }
        chain.doFilter(request, response);
    }

    private boolean isCoveredRequest(ServletRequest request)
    {
        // TODO: make sure this is the kind of request you want to limit based on information in request
        // Username? Roles?
        // Paths (but you should limit your url-pattern on this filter instead)
        // User-Agent?
        // Remote Address?
        // Etc ...

        return true;
    }

    @Override
    public void destroy()
    {
    }
}
