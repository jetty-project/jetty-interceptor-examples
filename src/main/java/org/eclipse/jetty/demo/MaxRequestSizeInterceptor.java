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

import javax.servlet.ServletRequest;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;

public class MaxRequestSizeInterceptor implements HttpInput.Interceptor
{
    private static final String ATTR = MaxRequestSizeInterceptor.class.getName() + ".intercepted";
    private final long maxRequestSize;
    private long readBytes;

    public static boolean isIntercepted(ServletRequest request)
    {
        Object interceptedAlready = request.getAttribute(ATTR);
        if (interceptedAlready != null)
        {
            return (boolean)interceptedAlready;
        }
        return false;
    }

    public static void add(Request request, long maxSize)
    {
        assertMaxRequestSize(request.getContentLengthLong(), maxSize);

        HttpInput httpInput = request.getHttpInput();
        httpInput.addInterceptor(new MaxRequestSizeInterceptor(maxSize));
        request.setAttribute(ATTR, true);
    }

    private static void assertMaxRequestSize(long contentLength, long maxRequestSize)
    {
        if (contentLength > maxRequestSize)
        {
            throw new MaxRequestSizeExceededException("Exceeded max request size of " + maxRequestSize);
        }
    }

    public MaxRequestSizeInterceptor(long maxRequestSize)
    {
        this.maxRequestSize = maxRequestSize;
    }

    @Override
    public HttpInput.Content readFrom(HttpInput.Content content)
    {
        readBytes += content.remaining();
        assertMaxRequestSize(readBytes, maxRequestSize);
        return content;
    }
}
