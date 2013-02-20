//
// Java Extension to CUI for Treasure Data
//
// Copyright (C) 2012 - 2013 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.treasure_data.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class CommandRequest {

    private static final Logger LOG = Logger.getLogger(
            CommandRequest.class.getName());

    private Properties props;
    private File[] files;

    protected CommandRequest(Properties props) throws CommandException {
        this.props = props;
    }

    protected abstract String getName();

    public void setFiles(String[] fileNames) throws CommandException {
        // validation for file names
        List<File> fileList = new ArrayList<File>(fileNames.length);
        for (int i = 0; i < fileNames.length; i++) {
            String fname = fileNames[i];
            File f = new File(fname);
            if (!f.isFile()) {
                LOG.severe("No such file: " + fname);
            } else {
                fileList.add(f);
            }
        }
        files = fileList.toArray(new File[0]);
    }

    public File[] getFiles() {
        return files;
    }

    public Properties getProperties() {
        return props;
    }
}
