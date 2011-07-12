/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.util;

import java.io.File;

public final class HarvConfig {
    private String saveEntitiesToFolder = null;

    /**
     * If saveEntitiesToFolder is null, entities default to being stored
     * internally as UTF-8 Strings. When saveEntitiesToFolder is set then
     * entities are saved in the entityParentFolder using their SHA256 value for
     * a name.
     */
    public HarvConfig(final String saveEntitiesToFolder) {
        final File folder = new File(saveEntitiesToFolder);

        if (!folder.exists() || !folder.isDirectory() || !folder.canWrite())
            throw new IllegalArgumentException("saveEntitiesToFolder must be an existing writable directory.");
        else {
            this.saveEntitiesToFolder = saveEntitiesToFolder;
        }
    }

    /** Entities are saved externally if saveEntitiesToFolder isn't null. **/
    public boolean isExternal() {
        return !(saveEntitiesToFolder == null);
    }

    public String getEntityParentFolder() {
        return saveEntitiesToFolder;
    }

    public void setEntityParentFolder(final String entityParentFolder) {
        saveEntitiesToFolder = entityParentFolder;
    }
}