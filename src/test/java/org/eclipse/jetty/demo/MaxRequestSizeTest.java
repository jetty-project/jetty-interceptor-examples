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

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class MaxRequestSizeTest
{
    private Server server;
    private HttpClient client;

    @BeforeEach
    public void setup() throws Exception
    {
        server = Main.newServer(0);
        server.start();

        client = new HttpClient();
        client.start();
    }

    @AfterEach
    public void teardown()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    @Test
    public void testGetDump() throws InterruptedException, ExecutionException, TimeoutException
    {
        ContentResponse response = client.GET(server.getURI().resolve("/dump/foo"));
        assertThat("response.status", response.getStatus(), is(200));
    }

    @Test
    public void testGetUpload() throws InterruptedException, ExecutionException, TimeoutException
    {
        ContentResponse response = client.GET(server.getURI().resolve("/foo.upload"));
        assertThat("response.status", response.getStatus(), is(200));
    }

    /**
     * Enforced by the Filter
     */
    @Test
    public void testPostDump() throws InterruptedException, ExecutionException, TimeoutException
    {
        Fields fields = new Fields();
        fields.add("name", "foo");
        char[] bigbuf = new char[4096];
        Arrays.fill(bigbuf, 'x');
        fields.add("buffer", new String(bigbuf));
        FormContentProvider form = new FormContentProvider(fields);

        ContentResponse response = client.POST(server.getURI().resolve("/dump/foo"))
            .content(form)
            .send();
        // dump(response);
        assertThat("response.status", response.getStatus(), is(400));
        assertThat("response.content", response.getContentAsString(), containsString("Request Size Exceeded"));
    }

    /**
     * Enforced by the Handler
     */
    @Test
    public void testPostUpload() throws InterruptedException, ExecutionException, TimeoutException
    {
        Fields fields = new Fields();
        fields.add("name", "foo");
        char[] bigbuf = new char[4096];
        Arrays.fill(bigbuf, 'x');
        fields.add("buffer", new String(bigbuf));
        FormContentProvider form = new FormContentProvider(fields);

        ContentResponse response = client.POST(server.getURI().resolve("/foo.upload"))
            .content(form)
            .send();
        dump(response);

        assertThat("response.status", response.getStatus(), is(400));
        assertThat("response.content", response.getContentAsString(), containsString("Exceeded max request size"));
    }

    private static void dump(ContentResponse response)
    {
        System.out.printf("%s %s %s%n", response.getVersion(), response.getStatus(), response.getReason());
        System.out.println(response.getHeaders());
        System.out.println(response.getContentAsString());
    }
}
