/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.run;

import static com.github.consiliens.harv.util.Utils.openWorkspaceByNumber;
import static com.github.consiliens.harv.util.Utils.p;

import java.io.File;
import java.util.List;

import com.github.consiliens.harv.util.HarManager;
import com.github.consiliens.harv.util.HarvConfig;
import com.github.consiliens.harv.util.Utils;
import com.subgraph.vega.api.model.IWorkspace;
import com.subgraph.vega.api.model.requests.IRequestLogRecord;

/**
 * 
 * Opens workspace 00 and creates a HAR file.
 * 
 * @author consiliens
 * 
 */
public abstract class Run {

    public static void main(final String[] args) {
        final String saveEntitiesToFolder = new File(Utils.getDefaultWorkspace("00"), "entities").getAbsolutePath();
        final HarvConfig config = new HarvConfig(saveEntitiesToFolder);
        final HarManager har = new HarManager("harv", "1.0", config);

        final IWorkspace space = openWorkspaceByNumber("00");
        final List<IRequestLogRecord> allRecords = space.getRequestLog().getAllRecords();        
        har.convertRecordsToHAR(allRecords);

        space.close();
        har.endHAR(new File("test.har"));
        p("finished!");
        System.exit(0);
    }
}