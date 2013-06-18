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
package com.treasure_data.bulk_import.writer;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

import com.treasure_data.bulk_import.ValueType;
import com.treasure_data.bulk_import.prepare_parts.PrepareConfiguration;
import com.treasure_data.bulk_import.prepare_parts.PreparePartsException;
import com.treasure_data.bulk_import.prepare_parts.PrepareProcessor;

public abstract class FileWriter implements Closeable {
    /**
     * TODO
     * TODO
     * public TDLogTableWriter implements Writer {
     *   public void configure(TDLogTableWriterConfiguration conf) {
     *     this.generator = new MessagePackMapGenerator(conf.getKeys(), conf.getColumnTypes());
     *   }
     *
     *   public void next(Row row) {
     *     if(size is larger than Xxx) {
     *       conf.getOutputFileListener().completeFile(currentFilePath);
     *       currentFilePath = createTempFilePath(conf.getOutputBaseDirectory());
     *       conf.getOutputFileListener().beginFile(currentFilePath);
     *     }
     *     generator.generate(row);
     *     // ...
     *   }
     * }
     *
     */
    /**
     * TODO
     * TODO
     * public interface Writer {
     *   public void next(Row row);
     * }
     */
    private static final Logger LOG = Logger
            .getLogger(FileWriter.class.getName());

    protected PrepareConfiguration conf;
    protected PrepareProcessor.Task task;
    protected long rowNum = 0;

    protected String[] keys;
    protected ValueType[] types;

    protected FileWriter(PrepareConfiguration conf) {
        this.conf = conf;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public void setTypes(ValueType[] types) {
        this.types = types;
    }

    // TODO FIXME
    // the argument type is bad.. we should change to 'Task'?
    public abstract void configure(String infileName)
            throws PreparePartsException;

    public void setTask(PrepareProcessor.Task task) {
        this.task = task;
    }

    public abstract void writeBeginRow(int size) throws PreparePartsException;

    public abstract void write(Object v) throws PreparePartsException;
    public abstract void writeString(String v) throws PreparePartsException;
    public abstract void writeInt(int v) throws PreparePartsException;
    public abstract void writeLong(long v) throws PreparePartsException;
    public abstract void writeDouble(double v) throws PreparePartsException;
    public abstract void writeNil() throws PreparePartsException;

    public abstract void writeEndRow() throws PreparePartsException;

    public void incrementRowNum() {
        rowNum++;
    }

    public long getRowNum() {
        return rowNum;
    }

    public abstract void close() throws IOException;

    public void closeSilently() {
        try {
            close();
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        }
    }
}