/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.run.test.util;

import static com.github.consiliens.harv.util.Utils.p;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;

import com.github.consiliens.harv.gson.IRequestLogRecordDeserializer;
import com.github.consiliens.harv.gson.IRequestLogRecordSerializer;
import com.github.consiliens.harv.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;
import com.subgraph.vega.internal.model.requests.RequestLogRecord;

public abstract class CreateTest {

    public static void a(Object value, String code) {
        p("a(" + value.toString() +", " + code + ");");
    }
    
    public static String q(String string) {
        return "\"" + string + "\"";
    }
    
    // TODO:  Update test based on new changes.
    public static void main(String[] args) throws FileNotFoundException {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(RequestLogRecord.class, new IRequestLogRecordSerializer());
        gsonBuilder.registerTypeAdapter(RequestLogRecord.class, new IRequestLogRecordDeserializer());
        Gson gson = gsonBuilder.create();

        String json = Utils.streamToString(new FileInputStream("data/out.json"));

        IRequestLogRecord record = gson.fromJson(json, RequestLogRecord.class);
        p("IRequestLogRecord record = getRecord();");
        p();
        
        a(record.getRequestId() + "L", "record.getRequestId()");
        a(record.getTimestamp() + "L", "record.getTimestamp()");      
        a(record.getRequestMilliseconds() + "L", "record.getRequestMilliseconds()");
        p();
        
        final HttpHost host = record.getHttpHost();
        p("final HttpHost host = record.getHttpHost();");
        a(q(host.getHostName()), "host.getHostName()");
        a(host.getPort(), "host.getPort()");
        a(q(host.getSchemeName()), "host.getSchemeName()");
        p();
        
        HttpRequest request = record.getRequest();
        Header[] headers = request.getAllHeaders();
        
        p("Header[] expectedHeaders = new Header[" + headers.length + "];");
        int headerCount = 0;
        
        for (Header header : headers) {
            p("expectedHeaders[" + headerCount++ + "] = new BasicHeader(\"" + header.getName() + "\", \""+header.getValue()+"\");");            
        }
        p();
        
        p("HttpRequest request = record.getRequest();");
        p("Header[] headers = request.getAllHeaders();");
        p();
        p("for (int a = 0; a < headers.length; a++) {");
        p("    a(expectedHeaders[a], headers[a]);");
        p("}");
        p();
        
        p("// Test both request.getProtocolVersion and request.getRequestLine().getProtocolVersion()");
        p("// even though the first merely calls the second, both methods must return the correct result.");
        ProtocolVersion protocolVersion = request.getProtocolVersion();
        p("ProtocolVersion protocolVersion = request.getProtocolVersion();");
        a(q(protocolVersion.getProtocol()), "protocolVersion.getProtocol()");
        a(protocolVersion.getMajor(), "protocolVersion.getMajor()");
        a(protocolVersion.getMinor(), "protocolVersion.getMinor()");      
        p();
        
        final RequestLine requestLine = request.getRequestLine();
        protocolVersion = requestLine.getProtocolVersion();
        p("final RequestLine requestLine = request.getRequestLine();");
        p("protocolVersion = requestLine.getProtocolVersion();");
        a(q(protocolVersion.getProtocol()), "protocolVersion.getProtocol()");
        a(protocolVersion.getMajor(), "protocolVersion.getMajor()");
        a(protocolVersion.getMinor(), "protocolVersion.getMinor()");
        p();
        
        a(q(requestLine.getMethod()), "requestLine.getMethod()");
        a(q(requestLine.getUri()), "requestLine.getUri()");
    }
}