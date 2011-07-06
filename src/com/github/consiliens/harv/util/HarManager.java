/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.util;

import java.io.File;
import java.io.IOException;

import edu.umass.cs.benchlab.har.HarCreator;
import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.tools.HarFileWriter;

/**
 * Manages the creation, feeding, and disposal of a HarLog.
 */
public class HarManager {

    HarLog log;
    HarEntries entries;

    /**
     * Creates a new HarManager.
     */
    public HarManager() {
        log = new HarLog(new HarCreator("vega export", "0.1"));
        entries = new HarEntries();
    }

    /**
     * Adds an entry to the managed HarLog.
     * 
     * @param entry
     *            the entry to add
     */
    public void addEntry(final HarEntry entry) {
        entries.addEntry(entry);
    }

    /**
     * Once called the fields of this class are set to null and addEntry will no
     * longer work.
     */
    public void endHAR() {
        log.setEntries(entries);

        try {
            new HarFileWriter().writeHarFile(log, new File("test.har.json"));
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // Clear out log and entries.
        log = null;
        entries = null;
    }
}