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
package com.treasure_data.bulk_import.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.treasure_data.bulk_import.model.ColumnType;
import com.treasure_data.bulk_import.model.DoubleColumnValue;
import com.treasure_data.bulk_import.model.IntColumnValue;
import com.treasure_data.bulk_import.model.LongColumnValue;
import com.treasure_data.bulk_import.model.StringColumnValue;
import com.treasure_data.bulk_import.model.TimeColumnValue;
import com.treasure_data.bulk_import.prepare_parts.ApachePrepareConfiguration;
import com.treasure_data.bulk_import.prepare_parts.ExtStrftime;
import com.treasure_data.bulk_import.prepare_parts.PrepareConfiguration;
import com.treasure_data.bulk_import.prepare_parts.PreparePartsException;
import com.treasure_data.bulk_import.prepare_parts.SyslogPrepareConfiguration;
import com.treasure_data.bulk_import.prepare_parts.Task;
import com.treasure_data.bulk_import.writer.FileWriter;
import com.treasure_data.bulk_import.writer.MsgpackGZIPFileWriter;

public class SyslogFileReader extends RegexFileReader<SyslogPrepareConfiguration> {

    private static final Logger LOG = Logger.getLogger(SyslogFileReader.class
            .getName());

    private static final String syslogPatString =
            "^([^ ]* [^ ]* [^ ]*) ([^ ]*) ([a-zA-Z0-9_\\/\\.\\-]*)(?:\\([a-zA-Z0-9_\\/\\.\\-]*\\))(?:\\[([0-9]+)\\])?[^\\:]*\\: *(.*)$";

    public static class ExtFileWriter extends MsgpackGZIPFileWriter {
        protected long currentYear;

        public ExtFileWriter(PrepareConfiguration conf) {
            super(conf);
            currentYear = getCurrentYear();
        }

        @Override
        public void write(TimeColumnValue filter, StringColumnValue v)
                throws PreparePartsException {
            long time = filter.getTimeFormat().getTime(v.getString());
            time += currentYear;
            write(time);
        }

        private long getCurrentYear() {
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy");
                Calendar cal = Calendar.getInstance();
                Date d = f.parse("" + cal.get(Calendar.YEAR));
                return d.getTime() / 1000;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected BufferedReader reader;
    protected Pattern syslogPat;

    protected String line;
    protected List<String> row = new ArrayList<String>();

    public SyslogFileReader(SyslogPrepareConfiguration conf, FileWriter writer)
            throws PreparePartsException {
        super(conf, writer, syslogPatString);
    }

    protected void updateColumnNames() {
        columnNames = new String[] { "time", "host", "ident", "pid", "message" };
    }

    protected void updateColumnTypes() {
        columnTypes = new ColumnType[] { ColumnType.STRING, ColumnType.STRING,
                ColumnType.STRING, ColumnType.INT, ColumnType.STRING, };
    }

    protected void updateTimeColumnValue() {
        timeColumnValue = new TimeColumnValue(2, new ExtStrftime("%b %d %H:%M:%S"));
    }
}