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
package com.treasure_data.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.treasure_data.commands.CommandException;
import com.treasure_data.commands.Config;
import com.treasure_data.commands.bulk_import.CSVPreparePartsRequest;
import com.treasure_data.commands.bulk_import.CSVPreparePartsRequest.ColumnType;
import com.treasure_data.commands.bulk_import.PreparePartsResult;
import com.treasure_data.file.TimeFormatSuggestionProcessor.TimeFormatProcessor;
import com.treasure_data.file.proc.ColumnProcessorGen;

public class CSVFileParser extends
        FileParser<CSVPreparePartsRequest, PreparePartsResult> {
    private static final Logger LOG = Logger.getLogger(CSVFileParser.class.getName());

    static class CellProcessorGen {
        public CellProcessor[] genForSampleReader(String[] columnNames, String[] typeHints,
                int sampleRowSize, int timeIndex, int aliasTimeIndex)
                        throws CommandException {
            TypeSuggestionProcessor[] cprocs = new TypeSuggestionProcessor[columnNames.length];
            for (int i = 0; i < cprocs.length; i++) {
                if (timeIndex == i) {
                    cprocs[i] = new TimeFormatSuggestionProcessor(sampleRowSize);
                } else if (aliasTimeIndex == i) {
                    cprocs[i] = new AlasTimeFormatProcessor(sampleRowSize);
                } else {
                    cprocs[i] = new TypeSuggestionProcessor(sampleRowSize);
                    if (typeHints.length != 0) {
                        cprocs[i].setType(typeHints[i]);
                    }
                }
            }
            return cprocs;
        }

        public CellProcessor[] gen(CSVPreparePartsRequest.ColumnType[] columnTypes,
                TimeFormatProcessor timeFormatProc)
                throws CommandException {
            int len = columnTypes.length;
            List<CellProcessor> cprocs = new ArrayList<CellProcessor>(len);
            for (int i = 0; i < len; i++) {
                CellProcessor cproc;
                switch (columnTypes[i]) { // override 'optional' ?
                case INT:
                    // TODO optimizable as new converter
                    cproc = new ConvertNullTo(null, new ParseInt());
                    break;
                case LONG:
                    // TODO optimizable as new converter
                    cproc = new ConvertNullTo(null, new ParseLong());
                    break;
                case DOUBLE:
                    // TODO optimizable as new converter
                    cproc = new ConvertNullTo(null, new ParseDouble());
                    break;
                case STRING:
                    // TODO optimizable as new converter
                    cproc = new Optional();
                    break;
                case TIME:
                    cproc = timeFormatProc;
                    break;
                default:
                    String msg = String.format("unsupported type: %s",
                            columnTypes[i]);
                    throw new CommandException(msg);
                }
                cprocs.add(cproc);
            }
            return cprocs.toArray(new CellProcessor[0]);
        }
    }

    private ICsvListReader reader;
    private CsvPreference csvPref;

    private int timeIndex = -1;
    private Long timeValue = new Long(-1);
    private int aliasTimeIndex = -1;
    private String[] allColumnNames;
    private List<Integer> extractedColumnIndexes;

    private ColumnType[] allSuggestedColumnTypes;
    private CellProcessor[] cprocessors;
    //private CellProcessor[] cprocessors2;

    private TimeFormatProcessor timeColumnProc = null;
    private TimeFormatProcessor aliasTimeColumnProc = null;

    public CSVFileParser(CSVPreparePartsRequest request,
            PreparePartsResult result) throws CommandException {
        super(request, result);
    }

    @Override
    public void initParser(final CharsetDecoder decoder, InputStream in)
            throws CommandException {
        // CSV preference
        csvPref = new CsvPreference.Builder('"', request.getDelimiterChar(),
                request.getNewline().newline()).build();

        // create sample reader
        CsvListReader sampleReader = new CsvListReader(new InputStreamReader(
                in, decoder), csvPref);

        try {
            // extract all column names
            // e.g. 
            // 1) [ "time", "name", "price" ]
            // 2) [ "timestamp", "name", "price" ]
            // 3) [ "name", "price" ]
            if (request.hasColumnHeader()) {
                List<String> columnList = sampleReader.read();
                allColumnNames = columnList.toArray(new String[0]);
            } else {
                allColumnNames = request.getColumnNames();
            }

            // get index of specified alias time column
            // [ "timestamp", "name", "price" ] as all columns and
            // "timestamp" as alias time column are given, the index is zero.
            if (request.getAliasTimeColumn() != null) {
                for (int i = 0; i < allColumnNames.length; i++) {
                    if (allColumnNames[i].equals(request.getAliasTimeColumn())) {
                        aliasTimeIndex = i;
                        break;
                    }
                }
            }

            // get index of 'time' column
            // [ "time", "name", "price" ] as all columns is given,
            // the index is zero.
            for (int i = 0; i < allColumnNames.length; i++) {
                if (allColumnNames[i].equals(
                        Config.BI_PREPARE_PARTS_TIMECOLUMN_DEFAULTVALUE)) {
                    timeIndex = i;
                    break;
                }
            }

            if (timeIndex < 0) {
                // if 'time' column is not included in all columns
                // e.g. [ "name", "price" ]
                timeValue = request.getTimeValue();
                if (aliasTimeIndex >= 0 || timeValue > 0) {
                    // 'time' column is appended to all columns (last elem) 
                    timeIndex = allColumnNames.length;
                } else {
                    throw new CommandException(
                            "Time column not found. --time-column or --time-value option is required");
                }
            }

            extractedColumnIndexes = new ArrayList<Integer>();
            String[] onlyColumns = request.getOnlyColumns();
            String[] excludeColumns = request.getExcludeColumns();
            for (int i = 0; i < allColumnNames.length; i++) {
                String cname = allColumnNames[i];

                // column is included in exclude-columns?
                if (excludeColumns.length != 0 && !cname.equals("time")) {
                    boolean isExcludeColumn = false;
                    for (int j = 0; j < excludeColumns.length; j++) {
                        if (cname.equals(excludeColumns[j])) {
                            isExcludeColumn = true;
                            break;
                        }
                    }
                    if (isExcludeColumn) {
                        continue;
                    }
                }

                // column is included in only-columns?
                if (onlyColumns.length == 0) {
                    extractedColumnIndexes.add(i);
                    continue;
                } else {
                    boolean isOnlyColumn = false;
                    for (int j = 0; j < onlyColumns.length; j++) {
                        if (cname.equals(onlyColumns[j])) {
                            isOnlyColumn = true;
                            break;
                        }
                    }
                    if (isOnlyColumn) {
                        extractedColumnIndexes.add(i);
                    }
                }
            }

            // check whether time column is included in extracted column
            if (timeValue < 0) {
                if (timeIndex != allColumnNames.length) {
                    boolean hasTimeRepresentedColumn = false;
                    for (Integer i : extractedColumnIndexes) {
                        if (i == timeIndex) {
                            hasTimeRepresentedColumn = true;
                            break;
                        }
                    }
                    if (!hasTimeRepresentedColumn) {
                        throw new CommandException(
                                "'time' represented column is not included in specified columns");
                    }
                }
            }

            // new String[] { "long", "string", "long" }
            String[] columnTypes = request.getColumnTypes();
            int columnTypeSize = columnTypes.length;
            if (columnTypeSize != 0 && columnTypeSize != allColumnNames.length) {
                throw new CommandException(String.format(
                        "mismatched between size of specified column types (%d) and size of columns (%d)",
                        columnTypeSize, allColumnNames.length));
            }

            final int sampleRowSize = request.getSampleRowSize();

            CellProcessor[] cprocs = new CellProcessorGen().genForSampleReader(
                    allColumnNames, columnTypes, sampleRowSize,
                    timeIndex < 0 ? -1 : timeIndex,
                    aliasTimeIndex >= 0 ? aliasTimeIndex : -1);

            List<Object> firstRow = null;
            boolean isFirstRow = false;
            for (int i = 0; i < sampleRowSize; i++) {
                List<Object> row = sampleReader.read(cprocs);
                if (!isFirstRow) {
                    firstRow = row;
                    isFirstRow = true;
                }

                if (row == null || row.isEmpty()) {
                    break;
                }
            }

            allSuggestedColumnTypes = new ColumnType[cprocs.length];
            for (int i = 0; i < cprocs.length; i++) {
                if (i == timeIndex) {
                    timeColumnProc = ((TimeFormatSuggestionProcessor) cprocs[i])
                            .getSuggestedTimeFormatProcessor();
                } else if (i == aliasTimeIndex) {
                    aliasTimeColumnProc = ((AlasTimeFormatProcessor) cprocs[i])
                            .getSuggestedTimeFormatProcessor();
                }
                allSuggestedColumnTypes[i] = ((TypeSuggestionProcessor) cprocs[i])
                        .getSuggestedType();
            }

            // print sample row
            if (firstRow != null) {
                JSONFileWriter w = new JSONFileWriter(request, result);
                parseList(w, firstRow);
                String ret = JSONValue.toJSONString(w.getRecord());
                LOG.info("sample row: " + ret);
            } else {
                LOG.info("sample row is null");
            }
        } catch (IOException e) {
            throw new CommandException(e);
        } finally {
            if (sampleReader != null) {
                try {
                    sampleReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void startParsing(final CharsetDecoder decoder, InputStream in)
            throws CommandException {
        // create reader
        reader = new CsvListReader(new InputStreamReader(in, decoder), csvPref);
        if (request.hasColumnHeader()) {
            // header line is skipped
            try {
                reader.read();
            } catch (IOException e) {
                throw new CommandException(e);
            }
        }

        // create cell processors
        cprocessors = new CellProcessorGen().gen(allSuggestedColumnTypes, timeColumnProc);
        //cprocessors2 = new ColumnProcessorGen().gen(allSuggestedColumnTypes, timeColumnProc);
    }

    @Override
    public boolean parseRow(
            @SuppressWarnings("rawtypes") com.treasure_data.file.FileWriter w)
            throws CommandException {
        List<Object> row = null;
        try {
            row = reader.read(cprocessors);
        } catch (Exception e) {
            // catch IOException and SuperCsvCellProcessorException
            e.printStackTrace();

            // TODO and parsent-encoded row?
            String msg = String.format("reason: %s, line: %d",
                    e.getMessage(), getRowNum());
            writeErrorRecord(msg);

            LOG.warning("Skip row number: " + getRowNum());
            return true;
        }

        if (row == null || row.isEmpty()) {
            return false;
        }

        // increment row number
        incrRowNum();

        return parseList(w, row);
    }

    private boolean parseList(
            @SuppressWarnings("rawtypes") com.treasure_data.file.FileWriter w,
            List<Object> row) throws CommandException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(String.format("lineNo=%s, rowNo=%s, customerList=%s",
                    reader.getLineNumber(), reader.getRowNumber(),
                    row));
        }

        /** DEBUG
        System.out.println(String.format("lineNo=%s, rowNo=%s, customerList=%s",
                reader.getLineNumber(), reader.getRowNumber(), row));
         */

        try {
            int allSize = row.size();

            if (allSize == timeIndex) {
                w.writeBeginRow(extractedColumnIndexes.size() + 1);
            } else {
                w.writeBeginRow(extractedColumnIndexes.size());
            }

            long time = 0;
            for (int i = 0; i < allSize; i++) {
                if (i == aliasTimeIndex) {
                    time = ((Number) aliasTimeColumnProc.execute(row.get(i), null)).longValue();
                }

                // i is included in extractedColumnIndexes?
                boolean included = false;
                for (Integer j : extractedColumnIndexes) {
                    if (i == j) { // TODO optimize
                        included = true;
                        break;
                    }
                }

                // write extracted data with writer
                if (included) {
                    w.write(allColumnNames[i]);
                    w.write(row.get(i));
                }
            }

            if (allSize == timeIndex) {
                w.write(Config.BI_PREPARE_PARTS_TIMECOLUMN_DEFAULTVALUE);
                if (aliasTimeIndex >= 0) {
                    w.write(time);
                } else {
                    w.write(timeValue);
                }
            }

            w.writeEndRow();

            w.incrRowNum();
            return true;
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    public void close() throws CommandException {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new CommandException(e);
            }
        }
    }
}