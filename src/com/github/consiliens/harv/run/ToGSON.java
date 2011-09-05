/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.run;

import static com.github.consiliens.harv.util.Utils.p;
import static com.github.consiliens.harv.util.Utils.openWorkspaceByNumber;

import java.io.File;
import java.io.FileWriter;

import com.github.consiliens.harv.gson.IRequestLogRecordDeserializer;
import com.github.consiliens.harv.gson.IRequestLogRecordSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.subgraph.vega.api.model.IWorkspace;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;
import com.subgraph.vega.internal.model.requests.RequestLogRecord;

public final class ToGSON {

    public static final Class<RequestLogRecord> recordClass = RequestLogRecord.class;

    /** Constructs a Gson object. **/
    public static Gson getGson(final String externalEntityPath) {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(recordClass, new IRequestLogRecordSerializer(externalEntityPath));
        gsonBuilder.registerTypeAdapter(recordClass, new IRequestLogRecordDeserializer(externalEntityPath));

        return gsonBuilder.create();
    }

    /** Writes json to filePath. **/
    public static void jsonToFile(String json, String filePath) {
        try {
            FileWriter writer = new FileWriter(new File(filePath));
            writer.write(json);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean mkdir(String dir) {
        return new File(dir).mkdirs();
    }

    /** Converts one record to Json. **/
    public static void oneRecordToJson(int index) {
        /**
         * Must keep space open when processing records or database errors will
         * be raised. Call space.close() when finished.
         **/
        final IWorkspace space = openWorkspaceByNumber("00");
        final IRequestLogRecord record = space.getRequestLog().getAllRecords().get(index);

        // Setup paths.
        // Save entities to and load entities from /tmp/entities
        final String jsonPath = "/tmp/gson/";
        mkdir(jsonPath);
        final String entitiesFolder = jsonPath + "entities/";
        mkdir(entitiesFolder);
        final String jsonFile = jsonPath + "out.json";

        final Gson gson = getGson(entitiesFolder);

        try {
            final String json = gson.toJson(record);
            jsonToFile(json, jsonFile);

            // Rebuild record and make sure no exceptions are raised.
            gson.fromJson(json, recordClass);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (space != null)
                space.close();
        }
    }

    public static void main(String[] args) throws Exception {
        final int index = 5;
        oneRecordToJson(index);

        p("finished!");
        System.exit(0);
    }
}