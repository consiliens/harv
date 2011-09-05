/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.util;

import org.codehaus.jackson.SerializableString;
import org.codehaus.jackson.io.CharacterEscapes;

public final class CustomEscapes extends CharacterEscapes {
    private final int[] characterEscapes;

    private void e(char escapedChar) {
        characterEscapes[escapedChar] = CharacterEscapes.ESCAPE_STANDARD;
    }

    public CustomEscapes() {
        characterEscapes = standardAsciiEscapesForJSON();

        // Escape the same characters as EscapingTest.java testEscapeAllHtmlCharacters
        e('<');
        e('>');
        e('=');
        e('&');
        e('\'');
        
        // Quotes are slash escaped by default.
        //e('"');
    }

    @Override
    public int[] getEscapeCodesForAscii() {
        return characterEscapes;
    }

    @Override
    public SerializableString getEscapeSequence(int ch) {
        // We're only using ESCAPE_STANDARD so just return null.
        return null;
    }
}