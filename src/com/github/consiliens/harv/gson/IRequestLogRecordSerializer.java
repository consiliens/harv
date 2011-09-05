/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.gson;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.github.consiliens.harv.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;

public final class IRequestLogRecordSerializer implements JsonSerializer<IRequestLogRecord> {
    // TODO: Default to true.
    private final String entitiesExternalPath;

    /** Defaults to saving entities as internal strings. **/
    public IRequestLogRecordSerializer() {
        entitiesExternalPath = "";
    }

    /**
     * If entitiesExternalPath is set then entites will be stored as external
     * files. If entitiesExternalPath is empty then entities are stored as
     * internal strings.
     **/
    public IRequestLogRecordSerializer(final String entitiesExternalPath) {
        this.entitiesExternalPath = entitiesExternalPath;
    }

    // TODO: Support more than just string values.
    public static JsonArray paramsToJsonArray(final HttpParams params) {
        JsonArray paramsJson = new JsonArray();

        // BasicHttpParams has no way to access HashMap parameters so use
        // reflection.
        try {
            final Field parameters = BasicHttpParams.class.getDeclaredField(R.parameters);
            parameters.setAccessible(true);
            HashMap<?, ?> targetParameters = (HashMap<?, ?>) parameters.get(params);

            if (!targetParameters.isEmpty()) {
                for (Entry<?, ?> entry : targetParameters.entrySet()) {
                    JsonArray entryJson = new JsonArray();
                    entryJson.add(new JsonPrimitive(entry.getKey().toString()));
                    // TODO: Is there a way to easily support non-string values?
                    entryJson.add(new JsonPrimitive(entry.getValue().toString()));

                    paramsJson.add(entryJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return paramsJson;
    }

    private static JsonArray headersToJsonArray(HttpRequest request) {
        return headersToJsonArray(request.getAllHeaders());
    }

    private static JsonArray headersToJsonArray(HttpResponse response) {
        return headersToJsonArray(response.getAllHeaders());
    }

    private static JsonArray headersToJsonArray(Header[] headers) {
        final JsonArray headersArray = new JsonArray();

        for (Header header : headers) {
            JsonArray nvPair = new JsonArray();

            nvPair.add(new JsonPrimitive(header.getName()));
            nvPair.add(new JsonPrimitive(header.getValue()));

            headersArray.add(nvPair);
        }

        return headersArray;
    }

    public static JsonObject ProtocolVersionToJsonObject(RequestLine requestLine) {
        return ProtocolVersionToJsonObject(requestLine.getProtocolVersion());
    }

    public static JsonObject ProtocolVersionToJsonObject(StatusLine statusLine) {
        return ProtocolVersionToJsonObject(statusLine.getProtocolVersion());
    }

    public static JsonObject ProtocolVersionToJsonObject(ProtocolVersion protocolVersion) {
        JsonObject protocolJson = new JsonObject();

        protocolJson.addProperty(R.protocol, protocolVersion.getProtocol());
        protocolJson.addProperty(R.major, protocolVersion.getMajor());
        protocolJson.addProperty(R.minor, protocolVersion.getMinor());

        return protocolJson;
    }

    @Override
    public JsonElement serialize(IRequestLogRecord src, Type typeOfSrc, JsonSerializationContext context) {
        // Update deserializer when serialize logic changes.
        JsonObject recordJson = new JsonObject();

        recordJson.addProperty(R.requestId, src.getRequestId());
        recordJson.addProperty(R.timestamp, src.getTimestamp());
        recordJson.addProperty(R.requestMilliseconds, src.getRequestMilliseconds());

        // HttpHost getHttpHost();
        JsonObject hostJson = new JsonObject();
        HttpHost host = src.getHttpHost();
        hostJson.addProperty(R.hostName, host.getHostName());
        hostJson.addProperty(R.port, host.getPort());
        hostJson.addProperty(R.schemeName, host.getSchemeName());
        recordJson.add(R.httpHost, hostJson);

        // HttpRequest getRequest();
        JsonObject requestJson = new JsonObject();
        HttpRequest httpRequest = src.getRequest();

        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            final HttpEntity requestEntity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
            try {
                if (entitiesExternalPath.isEmpty()) {
                    // From internal string.
                    requestJson.addProperty(R.entity, Utils.streamToString(requestEntity.getContent()));
                } else {
                    // From file.
                    final String fileName = Utils.streamToFile(requestEntity.getContent(), entitiesExternalPath);
                    requestJson.addProperty(R.entity, R.shaPrefix + fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        requestJson.add(R.allHeaders, headersToJsonArray(httpRequest));
        requestJson.add(R.params, paramsToJsonArray(httpRequest.getParams()));

        JsonObject requestLineJson = new JsonObject();
        RequestLine requestRequestLine = httpRequest.getRequestLine();

        requestLineJson.add(R.protocolVersion, ProtocolVersionToJsonObject(requestRequestLine));
        requestLineJson.addProperty(R.method, requestRequestLine.getMethod());
        requestLineJson.addProperty(R.uri, requestRequestLine.getUri());

        requestJson.add(R.requestLine, requestLineJson);
        recordJson.add(R.request, requestJson);

        // Serialize response.
        JsonObject responseJson = new JsonObject();
        HttpResponse httpResponse = src.getResponse();

        responseJson.add(R.allHeaders, headersToJsonArray(httpResponse));

        // Only write an entity if it's not null.
        HttpEntity responseEntity = httpResponse.getEntity();
        if (responseEntity != null) {
            try {
                if (entitiesExternalPath.isEmpty()) {
                    // Internal string.
                    responseJson.addProperty("entity", Utils.streamToString(responseEntity.getContent()));
                } else {
                    // From file.
                    final String fileName = Utils.streamToFile(responseEntity.getContent(), entitiesExternalPath);
                    responseJson.addProperty(R.entity, R.shaPrefix + fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Locale locale = httpResponse.getLocale();
        JsonObject localeJson = new JsonObject();
        localeJson.addProperty(R.language, locale.getLanguage());
        localeJson.addProperty(R.country, locale.getCountry());

        // Variant is likely to be empty so check before writing.
        final String variant = locale.getVariant();
        if (!variant.isEmpty())
            localeJson.addProperty(R.variant, variant);

        responseJson.add(R.locale, localeJson);
        responseJson.add(R.params, paramsToJsonArray(httpResponse.getParams()));

        StatusLine statusLine = httpResponse.getStatusLine();
        JsonObject statusLineJson = new JsonObject();

        // Match constructor order of BasicStatusLine
        statusLineJson.add(R.protocolVersion, ProtocolVersionToJsonObject(statusLine));
        statusLineJson.addProperty(R.statusCode, statusLine.getStatusCode());
        statusLineJson.addProperty(R.reasonPhrase, statusLine.getReasonPhrase());

        responseJson.add(R.statusLine, statusLineJson);
        recordJson.add(R.response, responseJson);

        return recordJson;
    }
}