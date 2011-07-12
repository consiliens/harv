/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.util;

import static com.github.consiliens.harv.util.Invoke.invoke;
import static com.github.consiliens.harv.util.Invoke.invokeStatic;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDateFormat;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.subgraph.vega.api.model.IModel;
import com.subgraph.vega.api.model.IWorkspace;
import com.subgraph.vega.api.model.IWorkspaceEntry;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;
import com.subgraph.vega.internal.model.Model;
import com.subgraph.vega.internal.model.WorkspaceEntry;

import edu.umass.cs.benchlab.har.HarCache;
import edu.umass.cs.benchlab.har.HarContent;
import edu.umass.cs.benchlab.har.HarCookie;
import edu.umass.cs.benchlab.har.HarCookies;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarEntryTimings;
import edu.umass.cs.benchlab.har.HarHeader;
import edu.umass.cs.benchlab.har.HarHeaders;
import edu.umass.cs.benchlab.har.HarPostData;
import edu.umass.cs.benchlab.har.HarPostDataParam;
import edu.umass.cs.benchlab.har.HarPostDataParams;
import edu.umass.cs.benchlab.har.HarQueryParam;
import edu.umass.cs.benchlab.har.HarQueryString;
import edu.umass.cs.benchlab.har.HarRequest;
import edu.umass.cs.benchlab.har.HarResponse;
import edu.umass.cs.benchlab.har.ISO8601DateFormatter;

public class Utils {

    public static final String UTF8 = "UTF-8";
    public static final String SHA256 = "SHA-256";

    public static void p(final Object o) {
        if (o == null) {
            System.out.println("null");
            return;
        }

        System.out.println(o.toString());
    }

    /** Create a fake object until Vega adds support for timings. **/
    public static HarEntryTimings getFakeHarEntryTimings() {
        final long send = 0L;
        final long wait = 0L;
        final long receive = 0L;

        return new HarEntryTimings(send, wait, receive);
    }

    public static long extractHeadersAndCookies(final Header[] allHeaders, final HarHeaders harHeaders,
            final HarCookies harCookies) {

        long headersSize = 0;
        final CookieDecoder decoder = new CookieDecoder();
        final CookieDateFormat format = new CookieDateFormat();

        for (final Header header : allHeaders) {
            final String headerName = header.getName();
            final String headerValue = header.getValue();

            try {
                headersSize += headerName.getBytes(UTF8).length + headerValue.getBytes(UTF8).length;
            } catch (final UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            harHeaders.addHeader(new HarHeader(headerName, headerValue));

            if (headerValue != null && headerName.equalsIgnoreCase("Cookie")) {
                final Set<Cookie> cookies = decoder.decode(headerValue);

                for (final Cookie cookie : cookies) {
                    Date expires = null;
                    final int maxAge = cookie.getMaxAge();

                    if (maxAge != -1) {
                        try {
                            // TODO: Is CookieDateFormat formatting maxAge
                            // properly?
                            expires = format.parse(format.format(maxAge));
                        } catch (final ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    harCookies.addCookie(new HarCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie
                            .getDomain(), expires, cookie.isHttpOnly(), cookie.isSecure(), cookie.getComment()));
                }
            }
        }

        return headersSize;
    }

    public static String headerValue(final Header header) {
        if (header != null)
            return header.getValue();

        return null;
    }

    /** Request. **/
    public static HarRequest createHarRequest(final HttpRequest httpRequest, final HarvConfig config) {
        final RequestLine line = httpRequest.getRequestLine();

        final String method = line.getMethod();
        final String uri = line.getUri();
        final String httpVersion = line.getProtocolVersion().toString();

        final HarHeaders headers = new HarHeaders();
        final HarCookies cookies = new HarCookies();
        final long headersSize = extractHeadersAndCookies(httpRequest.getAllHeaders(), headers, cookies);

        final HarQueryString queryString = new HarQueryString();
        final QueryStringDecoder decoder = new QueryStringDecoder(uri);
        final List<HarQueryParam> queryStringParams = new ArrayList<HarQueryParam>();

        for (final Map.Entry<String, List<String>> param : decoder.getParameters().entrySet()) {
            final String name = param.getKey();
            // Create separate params for multiple values with the same key.
            // /test?t=1&t=2
            for (final String value : param.getValue()) {
                queryStringParams.add(new HarQueryParam(name, value));
            }
        }

        queryString.setQueryParams(queryStringParams);

        String comment = null;
        final String mimeType = headerValue(httpRequest.getFirstHeader("Content-Type"));
        String text = null;
        final HarPostDataParams postDataParams = new HarPostDataParams();
        long bodySize = -1;

        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            try {
                final HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
                // Handle post data params.
                if (mimeType.toLowerCase().contains("application/x-www-form-urlencoded")) {
                    final String streamString = streamToString(entity.getContent());

                    for (final String nvString : streamString.split("&")) {
                        final String[] nvPair = nvString.split("=");
                        final String name = nvPair[0];
                        final String value = nvPair.length == 1 ? "" : nvPair[1];
                        postDataParams.addPostDataParam(new HarPostDataParam(name, value));
                    }
                } else {
                    if (config.isExternal()) {
                        final String sha256 = streamToFile(entity.getContent(), config.getEntityParentFolder());
                        comment = "sha256=" + sha256;
                    } else {
                        text = streamToString(entity.getContent());
                    }
                }

                bodySize = entity.getContentLength();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        // Either params or text is set, never both.
        final HarPostData postData = new HarPostData(mimeType, postDataParams, text, comment);

        // Set null comment for HarRequest.
        return new HarRequest(method, uri, httpVersion, cookies, headers, queryString, postData, headersSize, bodySize,
                null);
    }

    /** Converts stream to string. **/
    public static String streamToString(final InputStream input) {
        try {
            final byte[] dataBytes = new byte[input.available()];
            ByteStreams.readFully(input, dataBytes);

            return new String(dataBytes, UTF8);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Calculate SHA-256 value of given input stream. Save input stream to file
     * using the digest for a name. Return the digest value as a string
     **/
    public static String streamToFile(final InputStream input, final String entityParentFolder) {
        String fileName = null;

        try {
            final byte[] dataBytes = new byte[input.available()];
            ByteStreams.readFully(input, dataBytes);

            // shasum -a 256 file.txt
            final MessageDigest sha256 = MessageDigest.getInstance(SHA256);
            final byte[] digest = ByteStreams.getDigest(ByteStreams.newInputStreamSupplier(dataBytes), sha256);

            fileName = Hex.encodeHexString(digest);
            Files.write(dataBytes, new File(entityParentFolder, fileName));
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }

    /** Response. **/
    public static HarResponse createHarResponse(final HttpResponse httpResponse, final HarvConfig config) {
        final StatusLine responseStatus = httpResponse.getStatusLine();
        final int status = responseStatus.getStatusCode();
        final String statusText = responseStatus.getReasonPhrase();
        final String responseHttpVersion = httpResponse.getProtocolVersion().toString();

        final HarCookies cookies = new HarCookies();
        final HarHeaders headers = new HarHeaders();

        final long headersSize = extractHeadersAndCookies(httpResponse.getAllHeaders(), headers, cookies);

        long size = -1;
        String mimeType = "";
        String encoding = "";

        final HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            size = entity.getContentLength();
            // TODO: Will mimeType be set properly?
            mimeType = headerValue(entity.getContentType());
            encoding = headerValue(entity.getContentEncoding());
        }

        // Compression is not implemented.
        final long compression = 0;
        String text = null;
        String comment = null;
        try {
            if (config.isExternal()) {
                final String sha256 = streamToFile(entity.getContent(), config.getEntityParentFolder());
                comment = "sha256=" + sha256;
            } else {
                text = streamToString(entity.getContent());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Does Location always represent the redirect URL?
        final String redirectURL = headerValue(httpResponse.getFirstHeader("Location"));
        final long bodySize = size;

        final HarContent content = new HarContent(size, compression, mimeType, text, encoding, comment);

        // Set null comment for HarResponse.
        return new HarResponse(status, statusText, responseHttpVersion, cookies, headers, content, redirectURL,
                headersSize, bodySize, null);

    }

    public static void convertRecordsToHAR(final List<IRequestLogRecord> recordsList, final Harv har,
            final HarvConfig config) {

        for (final IRequestLogRecord record : recordsList) {

            final String pageRef = null;
            Date startedDateTime = null;

            try {
                startedDateTime = ISO8601DateFormatter.parseDate(ISO8601DateFormatter.format(new Date(record
                        .getTimestamp())));
            } catch (final ParseException e) {
                e.printStackTrace();
            }

            final long time = record.getRequestMilliseconds();
            final HarRequest request = createHarRequest(record.getRequest(), config);
            final HarResponse response = createHarResponse(record.getResponse(), config);
            final HarCache cache = null;
            final HarEntryTimings timings = getFakeHarEntryTimings();
            final String serverIPAddress = null;
            final String connection = String.valueOf(record.getRequestId());
            final String comment = null;

            // Har entry is now complete.
            har.addEntry(new HarEntry(pageRef, startedDateTime, time, request, response, cache, timings,
                    serverIPAddress, connection, comment));
        }
    }

    /** wsNumber must be a string because "00" converts to "0" as an int. **/
    public static IWorkspace openWorkspaceByNumber(final String wsNumber) {
        final File ws = getDefaultWorkspace(wsNumber);
        p("Workspace: " + ws);

        final IWorkspaceEntry entry = (IWorkspaceEntry) invokeStatic(WorkspaceEntry.class, "createFromPath", ws);

        final IModel model = new Model();
        invoke(model, "openWorkspaceEntry", entry);

        final IWorkspace openWorkspace = model.getCurrentWorkspace();

        if (openWorkspace == null) {
            p("open workspace failed");
            return null;
        }

        return openWorkspace;
    }

    /** wsNumber must be a string because "00" converts to "0" as an int. **/
    public static File getDefaultWorkspace(final String wsNumber) {
        final File ws = new File(System.getProperty("user.home"), ".vega" + File.separator + "workspaces"
                + File.separator + wsNumber);
        return ws;
    }
}