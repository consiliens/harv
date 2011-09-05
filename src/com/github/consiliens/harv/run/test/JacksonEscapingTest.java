/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.consiliens.harv.run.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import com.github.consiliens.harv.run.test.util.BagOfPrimitives;
import com.github.consiliens.harv.util.CustomEscapes;

/**
 * Performs some functional test involving JSON output escaping.
 * 
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author consiliens
 * 
 * This is a port of GSON's EscapingTest to Jackson.
 */
public class JacksonEscapingTest {
    private ObjectMapper mapper = new ObjectMapper();
    private static String UTF8 = "UTF-8";

    enum G {
        ESCAPE, NO_ESCAPE
    }

    private JsonGenerator json;

    @Before
    public void setUp() {
        json = getGenerator(G.ESCAPE);
    }
    
    @After
    public void tearDown() {
        try {
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JacksonEscapingTest() {
        // Ignore unrecognized fields. For example "expectedJson" in
        // testEscapingObjectFields.
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonGenerator getGenerator(final G escape) {
        return getGenerator(escape, new ByteArrayOutputStream());
    }
    
    private JsonGenerator getGenerator(final G escape, final ByteArrayOutputStream stream) {
        JsonFactory f = new JsonFactory();

        try {
            // Don't use pretty printer for these tests.
            JsonGenerator generator = f.createJsonGenerator(stream, JsonEncoding.UTF8);

            if (escape == G.ESCAPE) {
                generator.setCharacterEscapes(new CustomEscapes());
                generator.configure(Feature.ESCAPE_NON_ASCII, true);
            } else {
                generator.setCharacterEscapes(null);
                generator.configure(Feature.ESCAPE_NON_ASCII, false);
            }
            
            // Have JsonGenerator close the stream when generator.close() is called.
            generator.configure(Feature.AUTO_CLOSE_TARGET, true);
            
            return generator;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String toJson(final JsonGenerator generator, final Object object) {
        String jsonResult = null;
        try {
            mapper.writeValue(generator, object);
            final ByteArrayOutputStream stream = ((ByteArrayOutputStream) generator.getOutputTarget());
            jsonResult = stream.toString(UTF8);
            stream.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResult;
    }

    private <Type> Type fromJson(final String json, final Class<Type> klass) {
        Type object = null;
        try {
            object = mapper.readValue(json, klass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    @Test
    public void testEscapingQuotesInStringArray() throws Exception {
        String[] valueWithQuotes = { "beforeQuote\"afterQuote" };
        String jsonRepresentation = toJson(json, valueWithQuotes);
        String[] target = fromJson(jsonRepresentation, String[].class);
        assertEquals(1, target.length);
        assertEquals(valueWithQuotes[0], target[0]);
    }

    @Test
    public void testEscapeAllHtmlCharacters() throws Exception {
        List<String> strings = new ArrayList<String>();
        strings.add("<");
        strings.add(">");
        strings.add("=");
        strings.add("&");
        strings.add("'");
        strings.add("\"");
        // GSON produces 003c while Jackson produces 003C so use toLowerCase();.
        assertEquals("[\"\\u003c\",\"\\u003e\",\"\\u003d\",\"\\u0026\",\"\\u0027\",\"\\\"\"]", toJson(json, strings)
                .toLowerCase());
    }

    @Test
    public void testEscapingObjectFields() throws Exception {
        BagOfPrimitives objWithPrimitives = new BagOfPrimitives(1L, 1, true, "test with\" <script>");
        String jsonRepresentation = toJson(json, objWithPrimitives);
        assertFalse(jsonRepresentation.contains("<"));
        assertFalse(jsonRepresentation.contains(">"));
        assertTrue(jsonRepresentation.contains("\\\""));

        BagOfPrimitives expectedObject = fromJson(jsonRepresentation, BagOfPrimitives.class);
        assertEquals(objWithPrimitives.getExpectedJson(), expectedObject.getExpectedJson());
    }

    @Test
    public void testGsonAcceptsEscapedAndNonEscapedJsonDeserialization() throws Exception {
        // json is escaped by default in setUp();
        JsonGenerator escapeHtmlGson = json;
        JsonGenerator noEscapeHtmlGson = getGenerator(G.NO_ESCAPE);

        BagOfPrimitives target = new BagOfPrimitives(1L, 1, true, "test' / w'ith\" / \\ <script>");
        String escapedJsonForm = toJson(escapeHtmlGson, target);
        String nonEscapedJsonForm = toJson(noEscapeHtmlGson, target);
        assertFalse(escapedJsonForm.equals(nonEscapedJsonForm));
        
        assertEquals(target, fromJson(escapedJsonForm, BagOfPrimitives.class));
        assertEquals(target, fromJson(nonEscapedJsonForm, BagOfPrimitives.class));;
        
        // noEscapeHtmlGson won't be closed by tearDown so close it here.
        noEscapeHtmlGson.close();
    }

    @Test
    public void testGsonDoubleDeserialization() {
        JsonGenerator jsonGen = json;

        BagOfPrimitives expected = new BagOfPrimitives(3L, 4, true, "value1");
        String json = toJson(jsonGen, toJson(jsonGen, expected));
        String value = fromJson(json, String.class);
        BagOfPrimitives actual = fromJson(value, BagOfPrimitives.class);
        assertEquals(expected, actual);
    }
}