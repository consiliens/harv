/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.gson;

/** A list of string constants mainly used by GSON serializer/deserializer. **/
public abstract class R {

    public static final String requestId = "requestId";
    public static final String timestamp = "timestamp";
    public static final String requestMilliseconds = "requestMilliseconds";
    public static final String hostName = "hostName";
    public static final String port = "port";
    public static final String schemeName = "schemeName";
    public static final String httpHost = "httpHost";
    public static final String protocol = "protocol";
    public static final String major = "major";
    public static final String minor = "minor";
    public static final String protocolVersion = "protocolVersion";
    public static final String method = "method";
    public static final String uri = "uri";
    public static final String requestLine = "requestLine";
    public static final String request = "request";
    public static final String response = "response";
    public static final String allHeaders = "allHeaders";
    public static final String parameters = "parameters";
    public static final String locale = "locale";
    public static final String language = "language";
    public static final String country = "country";
    public static final String variant = "variant";
    public static final String reasonPhrase = "reasonPhrase";
    public static final String statusCode = "statusCode";
    public static final String statusLine = "statusLine";
    public static final String entity = "entity";
    public static final String params = "params";

    // '=' will be serialized as '\u003d' so use ':' instead.
    public static final String shaPrefix = "sha256:";
}