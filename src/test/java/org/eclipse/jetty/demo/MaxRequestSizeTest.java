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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.FutureResponseListener;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class MaxRequestSizeTest
{
    public static final String X_TEST_INFO = "X-TestInfo";
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
    public void testGetDumpNormal(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException
    {
        ContentResponse response = client
            .newRequest(server.getURI().resolve("/dump/foo"))
            .method(HttpMethod.GET)
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .send();
        assertThat("response.status", response.getStatus(), is(200));
    }

    @Test
    public void testGetUploadNormal(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException
    {
        ContentResponse response = client
            .newRequest(server.getURI().resolve("/foo.upload"))
            .method(HttpMethod.GET)
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .send();
        assertThat("response.status", response.getStatus(), is(HttpStatus.OK_200));
    }

    /**
     * Enforced by the Filter
     */
    @Test
    public void testPostDumpWithContentLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException
    {
        Fields fields = new Fields();
        fields.add("name", "foo");
        char[] bigbuf = new char[4096];
        Arrays.fill(bigbuf, 'x');
        fields.add("buffer", new String(bigbuf));
        FormContentProvider form = new FormContentProvider(fields);

        ContentResponse response = client.POST(server.getURI().resolve("/dump/foo"))
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(form)
            .send();
        // dump(response);
        assertThat("response.status", response.getStatus(), is(400));
        assertThat("response.content", response.getContentAsString(), containsString("Request Size Exceeded"));
    }

    @Test
    public void testPostDumpWithChunkedTransferEncoding(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/dump/foo"))
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] bodyBuf = newFormBodyBuf(4096);
            writeBuffered(bodyBuf, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(400));
        assertThat("response.content", response.getContentAsString(), containsString("Request Size Exceeded"));
    }

    @Test
    public void testPostDumpWithGzipChunkedTransferEncoding(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/dump/foo"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(4096));
            writeBuffered(gzipBuffer, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Request Size Exceeded"));
    }

    @Test
    public void testPostDumpWithGzipChunkedTransferEncodingAllowedSize(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/dump/foo"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(900));
            writeBuffered(gzipBuffer, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.OK_200));
    }

    @Test
    public void testPostDumpWithGzipContentLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/dump/foo"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(4096));
            out.write(gzipBuffer);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Request Size Exceeded"));
    }

    @Test
    public void testPostDumpWithGzipContentLengthAllowedLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/dump/foo"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(900));
            out.write(gzipBuffer);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.OK_200));
    }

    /**
     * Enforced by the Handler
     */
    @Test
    public void testPostUploadWithContentLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException
    {
        Fields fields = new Fields();
        fields.add("name", "foo");
        char[] bigbuf = new char[4096];
        Arrays.fill(bigbuf, 'x');
        fields.add("buffer", new String(bigbuf));
        FormContentProvider form = new FormContentProvider(fields);

        ContentResponse response = client.POST(server.getURI().resolve("/foo.upload"))
            .content(form)
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .send();
        // dump(response);

        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Exceeded max request size"));
    }

    @Test
    public void testPostUploadWithChunkedTransferEncoding(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/foo.upload"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] bodyBuf = newFormBodyBuf(4096);
            writeBuffered(bodyBuf, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Exceeded max request size"));
    }

    @Test
    public void testPostUploadWithGzipChunkedTransferEncoding(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/foo.upload"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(4096));
            writeBuffered(gzipBuffer, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Exceeded max request size"));
    }

    @Test
    public void testPostUploadWithGzipChunkedTransferEncodingAllowedLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/foo.upload"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(900));
            writeBuffered(gzipBuffer, out, 128);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.OK_200));
    }

    @Test
    public void testPostUploadWithGzipContentLength(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/foo.upload"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(4096));
            out.write(gzipBuffer);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.BAD_REQUEST_400));
        assertThat("response.content", response.getContentAsString(), containsString("Exceeded max request size"));
    }

    @Test
    public void testPostUploadWithGzipContentLengthAllowedSize(TestInfo testInfo) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        OutputStreamContentProvider content = new OutputStreamContentProvider();
        Request request = client.POST(server.getURI().resolve("/foo.upload"))
            .header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeader.CONTENT_ENCODING, "gzip")
            .header(X_TEST_INFO, testInfo.getDisplayName())
            .content(content);
        FutureResponseListener listener = new FutureResponseListener(request);
        try (OutputStream out = content.getOutputStream())
        {
            request.send(listener);
            byte[] gzipBuffer = asGzipCompressed(newFormBodyBuf(900));
            out.write(gzipBuffer);
        }

        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        // dump(response);
        assertThat("response.status", response.getStatus(), is(HttpStatus.OK_200));
    }

    private static byte[] newFormBodyBuf(int size)
    {
        StringBuilder body = new StringBuilder();
        body.append("name=foo");
        body.append("&buffer=");
        char[] bigbuf = new char[size];
        Arrays.fill(bigbuf, 'x');
        body.append(bigbuf);
        return body.toString().getBytes(UTF_8);
    }

    private static byte[] asGzipCompressed(byte[] bodyBuf) throws IOException
    {
        try (ByteArrayOutputStream gzipData = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(gzipData))
        {
            gzipOut.write(bodyBuf);
            gzipOut.finish();
            return gzipData.toByteArray();
        }
    }

    private static void writeBuffered(byte[] buf, OutputStream out, int writeMaxSize) throws IOException
    {
        int offset = 0;
        while (offset < buf.length)
        {
            int writeSize = Math.min(buf.length - offset, writeMaxSize);
            out.write(buf, offset, writeSize);
            out.flush();
            offset += writeSize;
        }
    }

    private static void dump(ContentResponse response)
    {
        System.out.printf("%s %s %s%n", response.getVersion(), response.getStatus(), response.getReason());
        System.out.println(response.getHeaders());
        System.out.println(response.getContentAsString());
    }
}
