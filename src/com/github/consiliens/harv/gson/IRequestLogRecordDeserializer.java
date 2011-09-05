/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.gson;

import static com.github.consiliens.harv.util.Utils.UTF8;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;
import com.subgraph.vega.internal.model.requests.RequestLogRecord;

public final class IRequestLogRecordDeserializer implements JsonDeserializer<IRequestLogRecord> {

    // TODO: Default to true.
    private final String entitiesExternalPath;

    /** Defaults to saving entities as internal strings. **/
    public IRequestLogRecordDeserializer() {
        entitiesExternalPath = "";
    }

    /**
     * If entitiesExternalPath is set then entities will be stored as external
     * files. If entitiesExternalPath is empty then entities are stored as
     * internal strings.
     **/
    public IRequestLogRecordDeserializer(final String entitiesExternalPath) {
        this.entitiesExternalPath = entitiesExternalPath;
    }

    /** Returns entity file name with shaPrefix removed. **/
    public static String getEntityFileName(final String entity) {
        return entity.substring(R.shaPrefix.length());
    }

    /**
     * Params name and value are stored as strings. Params are backed by a
     * HashMap so order is not guaranteed. Duplicate keys are not allowed in a
     * HashMap.
     **/
    public static void setParamsFromJson(final HttpParams httpParams, final JsonObject obj) {
        final JsonArray requestHttpParamsJson = obj.getAsJsonArray(R.params);

        for (int a = 0; a < requestHttpParamsJson.size(); a++) {
            final JsonArray nvPair = requestHttpParamsJson.get(a).getAsJsonArray();

            httpParams.setParameter(nvPair.get(0).getAsString(), nvPair.get(1).getAsString());
        }
    }

    public ProtocolVersion getProtocolVersion(JsonObject protocolVersion) {
        final String protocol = protocolVersion.get(R.protocol).getAsString();
        final int major = protocolVersion.get(R.major).getAsInt();
        final int minor = protocolVersion.get(R.minor).getAsInt();

        return new ProtocolVersion(protocol, major, minor);
    }

    public Header[] getHeaders(final JsonArray allHeadersJsonArray) {
        final int headerCount = allHeadersJsonArray.size();
        final Header[] headerArray = new Header[headerCount];

        for (int a = 0; a < headerCount; a++) {
            final JsonArray nvPair = allHeadersJsonArray.get(a).getAsJsonArray();

            final String name = nvPair.get(0).getAsString();
            final String value = nvPair.get(1).getAsString();

            headerArray[a] = new BasicHeader(name, value);
        }

        return headerArray;
    }

    @Override
    public IRequestLogRecord deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // Follow same order as serializer for sanity.
        IRequestLogRecord record = null;

        final JsonObject recordJson = json.getAsJsonObject();

        final long requestId = recordJson.get(R.requestId).getAsLong();
        final long requestTimestamp = recordJson.get(R.timestamp).getAsLong();
        final long requestMilliseconds = recordJson.get(R.requestMilliseconds).getAsLong();

        // HttpHost
        JsonObject hostJson = recordJson.getAsJsonObject(R.httpHost);

        final String hostName = hostJson.get(R.hostName).getAsString();
        final int port = hostJson.get(R.port).getAsInt();
        final String schemeName = hostJson.get(R.schemeName).getAsString();

        final HttpHost host = new HttpHost(hostName, port, schemeName);

        // Request
        final JsonObject requestJson = recordJson.getAsJsonObject(R.request);

        String requestEntityString = "";
        HttpRequest httpRequest = null;
        if (requestJson.has(R.entity)) {
            requestEntityString = requestJson.get(R.entity).getAsString();
        }

        // Must parse headers here.
        final Header[] requestHeaders = getHeaders(requestJson.getAsJsonArray(R.allHeaders));

        // Request params
        final HttpParams requestHttpParams = new BasicHttpParams();
        setParamsFromJson(requestHttpParams, requestJson);

        // Request RequestLine
        final JsonObject requestLineJson = requestJson.getAsJsonObject(R.requestLine);

        // Request RequestLine Protocol
        final ProtocolVersion requestProtocol = getProtocolVersion(requestLineJson.getAsJsonObject(R.protocolVersion));

        final String method = requestLineJson.get(R.method).getAsString();
        final String uri = requestLineJson.get(R.uri).getAsString();

        // Request is finished. Build the HttpRequest Object.
        if (requestEntityString.isEmpty()) {
            // non-entity request
            httpRequest = new BasicHttpRequest(method, uri, requestProtocol);
        } else {
            httpRequest = new BasicHttpEntityEnclosingRequest(method, uri, requestProtocol);

            ByteArrayEntity requestEntity = null;
            if (entitiesExternalPath.isEmpty()) {
                // From internal string.
                try {
                    requestEntity = new ByteArrayEntity(requestEntityString.getBytes(UTF8));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                // From file.
                final String fileName = getEntityFileName(requestEntityString);
                try {
                    requestEntity = new ByteArrayEntity(Files.toByteArray(new File(entitiesExternalPath, fileName)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(requestEntity);
        }

        httpRequest.setHeaders(requestHeaders);
        httpRequest.setParams(requestHttpParams);

        // HttpResponse
        final JsonObject responseJson = recordJson.getAsJsonObject(R.response);
        final Header[] responseHeaders = getHeaders(requestJson.getAsJsonArray(R.allHeaders));

        // Response Entity
        String responseEntityString = "";
        if (responseJson.has(R.entity)) {
            responseEntityString = responseJson.get(R.entity).getAsString();
        }

        // Locale
        JsonObject localeJson = responseJson.getAsJsonObject(R.locale);
        final String language = localeJson.get(R.language).getAsString();
        final String country = localeJson.get(R.country).getAsString();
        String variant = "";

        // Variant might not exist.
        if (localeJson.has(R.variant))
            variant = localeJson.get(R.variant).getAsString();

        Locale locale = new Locale(language, country, variant);

        // Response params
        final HttpParams responseHttpParams = new BasicHttpParams();
        setParamsFromJson(responseHttpParams, responseJson);

        // StatusLine
        JsonObject statusLineJson = responseJson.getAsJsonObject(R.statusLine);

        // StatusLine Protocol Version
        final ProtocolVersion responseProtocol = getProtocolVersion(statusLineJson.getAsJsonObject(R.protocolVersion));
        final int statusCode = statusLineJson.get(R.statusCode).getAsInt();
        final String reasonPhrase = statusLineJson.get(R.reasonPhrase).getAsString();

        StatusLine responseStatus = new BasicStatusLine(responseProtocol, statusCode, reasonPhrase);

        // Default to using EnglishReasonPhraseCatalog
        final HttpResponse httpResponse = new BasicHttpResponse(responseStatus, EnglishReasonPhraseCatalog.INSTANCE,
                locale);

        httpResponse.setHeaders(responseHeaders);
        httpResponse.setParams(responseHttpParams);

        // Ensure entity exists before processing.
        if (!requestEntityString.isEmpty()) {
            ByteArrayEntity responseEntity = null;

            try {
                if (entitiesExternalPath.isEmpty()) {
                    // From internal string.
                    responseEntity = new ByteArrayEntity(responseEntityString.getBytes(UTF8));
                } else {
                    // From file.
                    final String fileName = getEntityFileName(requestEntityString);
                    responseEntity = new ByteArrayEntity(Files.toByteArray(new File(entitiesExternalPath, fileName)));
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            httpResponse.setEntity(responseEntity);
        }

        // RequestLogRecord's constructors are package private so use
        // reflection.
        try {
            final Constructor<RequestLogRecord> construct = RequestLogRecord.class.getDeclaredConstructor(long.class,
                    HttpRequest.class, HttpResponse.class, HttpHost.class, long.class);
            construct.setAccessible(true);
            record = (RequestLogRecord) construct.newInstance(requestId, httpRequest, httpResponse, host,
                    requestMilliseconds);

            // There's no get or set timestamp so use reflection.
            final Field timestampField = RequestLogRecord.class.getDeclaredField(R.timestamp);
            timestampField.setAccessible(true);
            timestampField.set(record, requestTimestamp);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }
}