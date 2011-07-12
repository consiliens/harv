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
import java.util.List;

import com.subgraph.vega.api.model.requests.IRequestLogRecord;

import edu.umass.cs.benchlab.har.HarCreator;
import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.tools.HarFileWriter;

/**
 * Manages the creation, feeding, and disposal of a HarLog.
 */
public class HarManager {

    private HarLog log;
    private HarEntries entries;
    private HarvConfig config;

    /**
     * Create a new HarManager.
     * @param creatorName the name of program creating the Har file
     * @param creatorVersion the version of the program creating the Har file
     * @param config the harv configuration settings
     */
    public HarManager(final String creatorName, final String creatorVersion, final HarvConfig config) {
        log = new HarLog(new HarCreator(creatorName, creatorVersion));
        entries = new HarEntries();
        this.config = config;
    }

    public void convertRecordsToHAR(final List<IRequestLogRecord> recordsList) {
        Utils.convertRecordsToHAR(recordsList, this, config);
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
    public void endHAR(final File outputFile) {
        log.setEntries(entries);

        try {
            new HarFileWriter().writeHarFile(log, outputFile);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // Clear out log and entries.
        log = null;
        entries = null;
    }

    public HarvConfig getConfig() {
        return config;
    }

    public void setConfig(HarvConfig config) {
        this.config = config;
    }       
}