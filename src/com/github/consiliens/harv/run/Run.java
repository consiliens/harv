/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.run;

import static com.github.consiliens.harv.util.Utils.convertRecordsToHAR;
import static com.github.consiliens.harv.util.Utils.openWorkspaceByNumber;
import static com.github.consiliens.harv.util.Utils.p;

import com.github.consiliens.harv.util.HarManager;
import com.subgraph.vega.api.model.IWorkspace;

/**
 * 
 * Opens workspace 00 and creates a HAR file.
 * 
 * @author consiliens
 * 
 */
public abstract class Run {

    public static void main(String[] args) {
        final HarManager har = new HarManager();
        final IWorkspace space = openWorkspaceByNumber("00");

        convertRecordsToHAR(space.getRequestLog().getAllRecords(), har);

        space.close();
        har.endHAR();
        p("finished!");
        System.exit(0);
    }
}