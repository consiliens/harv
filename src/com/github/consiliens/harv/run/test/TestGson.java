/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.run.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.apache.http.Header;
import org.junit.Test;

import com.github.consiliens.harv.gson.IRequestLogRecordDeserializer;
import com.github.consiliens.harv.gson.IRequestLogRecordSerializer;
import com.github.consiliens.harv.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;
import com.subgraph.vega.internal.model.requests.RequestLogRecord;

public class TestGson {
    
    public static void a(Header expected, Header actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getValue(), actual.getValue());
    }
    
    public static void a(long expected, long actual) {
        Assert.assertEquals(expected, actual);
    }

    public static void a(String expected, String actual) {
        Assert.assertEquals(expected, actual);
    }
    
    public static IRequestLogRecord getRecord() {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(RequestLogRecord.class, new IRequestLogRecordSerializer());
        gsonBuilder.registerTypeAdapter(RequestLogRecord.class, new IRequestLogRecordDeserializer());
        Gson gson = gsonBuilder.create();

        String json = null;
        try {
            json = Utils.streamToString(new FileInputStream("data/out.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return gson.fromJson(json, RequestLogRecord.class);        
    }
    
    @Test
    public void test() throws FileNotFoundException {
        // TODO:  Update this test.
        // output of CreateTest goes here.
    }
}