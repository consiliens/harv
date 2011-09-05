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

import com.github.consiliens.harv.util.Harv;
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
public abstract class ToHAR {

    public static void main(final String[] args) {
        final String ws00 = "00";

        final String saveEntitiesToFolder = new File(Utils.getDefaultWorkspace(ws00), "entities").getAbsolutePath();
        final HarvConfig config = new HarvConfig(saveEntitiesToFolder);
        final Harv har = new Harv(config);

        final IWorkspace space = openWorkspaceByNumber(ws00);
        final List<IRequestLogRecord> allRecords = space.getRequestLog().getAllRecords();

        // har.convertRecordsToHAR(allRecords);
        har.convertOneRecordToHAR(allRecords.get(5));

        space.close();
        har.endHAR(new File("test.har"));
        p("finished!");
        System.exit(0);
    }
}