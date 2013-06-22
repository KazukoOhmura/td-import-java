//
// Treasure Data Bulk-Import Tool in Java
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
package com.treasure_data.bulk_import.prepare_parts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.treasure_data.bulk_import.reader.FileReader;
import com.treasure_data.bulk_import.upload_parts.MultiThreadUploadProcessor;
import com.treasure_data.bulk_import.upload_parts.UploadProcessor;
import com.treasure_data.bulk_import.writer.FileWriter;

public class PrepareProcessor {

    public static class Task {
        private static final String TAG = "__PREPARE_FINISH__";
        static final Task FINISH_TASK = new Task(TAG);

        static boolean endTask(Task t) {
            return t.equals(FINISH_TASK);
        }

        public String fileName;

        boolean isTest = false;
        String testText = null;

        public Task(String fileName) {
            this.fileName = fileName;
        }

        public InputStream createInputStream(
                PrepareConfiguration.CompressionType compressionType)
                throws IOException {
            if (!isTest) {
                return compressionType.createInputStream(new FileInputStream(fileName));
            } else {
                return new ByteArrayInputStream(testText.getBytes());
            }
        }

        public void finishHook(String outputFileName) {
            // do nothing
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Task)) {
                return false;
            }

            Task t = (Task) obj;
            return t.fileName.equals(fileName);
        }

        @Override
        public String toString() {
            return String.format("prepare_task{file=%s}", fileName);
        }
    }

    public static class UploadTask extends Task {
        public String sessionName;

        public UploadTask(String sessionName, String fileName) {
            super(fileName);
            this.sessionName = sessionName;
        }

        @Override
        public void finishHook(String outputFileName) {
            super.finishHook(outputFileName);

            long size = new File(outputFileName).length();
            UploadProcessor.Task task = new UploadProcessor.Task(
                    sessionName, outputFileName, size);
            MultiThreadUploadProcessor.addTask(task);
        }

        @Override
        public String toString() {
            return String.format("prepare_upload_task{file=%s, session=%s}",
                    fileName, sessionName);
        }
    }

    public static class ErrorInfo {
        public Task task;
        public Throwable error = null;

        public long redLines = 0;
        public long redRows = 0;
        public long writtenRows = 0;

        @Override
        public String toString() {
            return String.format(
                    "prepare_error_info{task=%s, redLines=%d, redRows=%d, writtenRows=%d}",
                    task, redLines, redRows, writtenRows);
        }
    }

    private static final Logger LOG = Logger.getLogger(
            PrepareProcessor.class.getName());

    protected PrepareConfiguration conf;

    public PrepareProcessor(PrepareConfiguration conf) {
        this.conf = conf;
    }

    public ErrorInfo execute(final Task task) {
        LOG.info(String.format("Convert file '%s'", task.fileName));
        LOG.fine(String.format("Process task '%s'", task));

        ErrorInfo err = new ErrorInfo();
        err.task = task;

        // create and initialize file writer
        FileWriter w = null;
        try {
            w = conf.getOutputFormat().createFileWriter(conf);
            w.configure(task);
        } catch (Exception e) {
            err.error = e;
            return err;
        }

        // create and initialize file reader
        FileReader r = null;
        try {
            r = conf.getFormat().createFileReader(conf, w);
            r.configure(task);
        } catch (Exception e) {
            err.error = e;
            return err;
        }

        if (w != null && r != null) {
            w.setColumnNames(r.getColumnNames());
            w.setColumnTypes(r.getColumnTypes());
            w.setSkipColumns(r.getSkipColumns());

            try {
                r.resetLineNum();
                r.resetRowNum();
                w.resetRowNum();

                while (r.next()) {
                    ;
                }

                err.redLines = r.getLineNum();
                err.redRows = r.getRowNum();
                err.writtenRows = w.getRowNum();
            } catch (Exception e) {
                e.printStackTrace();
                err.error = e;
            }
        }

        if (r != null) {
            try {
                r.close();
            } catch (IOException e) {
                err.error = e;
                return err;
            }
        }

        if (w != null) {
            try {
                w.close();
            } catch (IOException e) {
                err.error = e;
                return err;
            }
        }

        LOG.info(String.format("Converted file '%s', %d entries",
                task.fileName, err.writtenRows));

        return err;
    }

}
