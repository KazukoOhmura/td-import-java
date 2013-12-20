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
package com.treasure_data.td_import.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.msgpack.MessagePack;
import org.msgpack.type.FloatValue;
import org.msgpack.type.IntegerValue;
import org.msgpack.type.MapValue;
import org.msgpack.type.RawValue;
import org.msgpack.type.Value;
import org.msgpack.unpacker.UnpackerIterator;

import com.treasure_data.td_import.model.ColumnType;
import com.treasure_data.td_import.model.ColumnValue;
import com.treasure_data.td_import.model.Record;
import com.treasure_data.td_import.prepare.MessagePackPrepareConfiguration;
import com.treasure_data.td_import.prepare.PreparePartsException;
import com.treasure_data.td_import.prepare.Task;
import com.treasure_data.td_import.writer.RecordWriter;

public class MessagePackRecordReader extends VariableLengthColumnsRecordReader<MessagePackPrepareConfiguration> {
    private static final Logger LOG = Logger.getLogger(MessagePackRecordReader.class.getName());

    protected MessagePack msgpack;
    protected InputStream in;
    protected UnpackerIterator iterator;

    protected String[] keys;
    protected Object[] values;

    public MessagePackRecordReader(MessagePackPrepareConfiguration conf, RecordWriter writer) {
        super(conf, writer);
        msgpack = new MessagePack();
    }

    @Override
    public void configure(Task task) throws PreparePartsException {
        super.configure(task);

        sample(task);

        try {
            in = task.createInputStream(conf.getCompressionType());
            iterator = msgpack.createUnpacker(in).iterator();
        } catch (IOException e) {
            throw new PreparePartsException(e);
        }
    }

    @Override
    protected void sample(Task task) throws PreparePartsException {
        // TODO FIXME #MN
    }

    @Override
    public void setColumnNames() {
        columnNames = keys;
    }

    @Override
    public void setColumnTypes() {
        columnTypes = new ColumnType[values.length];
        for (int i = 0; i < columnTypes.length; i++) {
            columnTypes[i] = toColumnType(values[i]);
        }
    }

    @Override
    public boolean readRecord() throws IOException {
        if (!iterator.hasNext()) {
            return false;
        }

        incrementLineNum();

        Value v = iterator.next();
        if (v == null || !v.isMapValue()) {
            return false;
        }

        MapValue row = v.asMapValue();

        // keys
        Value[] ks = row.keySet().toArray(new Value[0]);
        keys = new String[ks.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = ks[i].asRawValue().getString();
        }

        // values
        values = new Object[keys.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = toObject(row.get(ks[i]));
        }

        return true;
    }

    private Object toObject(Value value) {
        if (value instanceof IntegerValue) {
            return value.asIntegerValue().getLong();
        } else if (value instanceof FloatValue) {
            return value.asFloatValue().getDouble();
        } else if (value instanceof RawValue) {
            return value.asRawValue().getString();
        } else {
            throw new UnsupportedOperationException("During toColumnType() execution");
        }
    }

    @Override
    public void convertTypesOfColumns() throws PreparePartsException {
        ColumnValue[] columnValues = new ColumnValue[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            columnValues[i] = columnTypes[i].createColumnValue();
            columnTypes[i].setColumnValue(values[i], columnValues[i]);
        }

        convertedRecord = new Record(columnValues);
    }

    @Override
    public String getCurrentRow() {
        return "";
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (in != null) {
            in.close();
        }
    }
}