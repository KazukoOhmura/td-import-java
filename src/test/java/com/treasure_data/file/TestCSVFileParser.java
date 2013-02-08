package com.treasure_data.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.treasure_data.commands.Config;
import com.treasure_data.commands.bulk_import.CSVPreparePartsRequest;
import com.treasure_data.commands.bulk_import.PreparePartsRequest;
import com.treasure_data.file.CSVFileParser;
import com.treasure_data.file.CSVFileParser.TypeSuggestionProcessor;

public class TestCSVFileParser {

    private CSVPreparePartsRequest request;
    private CSVFileParser parser;
    private FileWriterTestUtil writer;

    @Before
    public void createResources() throws Exception {
        request = new CSVPreparePartsRequest();
        request.setSampleRowSize(Integer.parseInt(Config.BI_PREPARE_PARTS_SAMPLE_ROWSIZE_DEFAULTVALUE));
        request.setSampleHintScore(Integer.parseInt(Config.BI_PREPARE_PARTS_SAMPLE_HINT_SCORE_DEFAULTVALUE));

        parser = new CSVFileParser(request);

        writer = new FileWriterTestUtil(request);
    }

    @After
    public void deleteResources() throws Exception {
        parser.close();
        parser = null;

        writer.close();
        writer = null;
    }

    @Test
    public void testTypeSuggestion() throws Exception {
        int hintScore = 3;
        {
            String[] values = new String[] {
                    "v0\n", "v1\n", "v2\n", "v3\n", "v4\n",
            };
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sbuf.append(values[i]);
            }

            String text = sbuf.toString();
            byte[] bytes = text.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            CsvPreference pref = new CsvPreference.Builder('"', ',', "\n").build();
            CsvListReader sampleReader = new CsvListReader(
                    new InputStreamReader(in), pref);

            TypeSuggestionProcessor TSP = new TypeSuggestionProcessor(
                    values.length, hintScore);
            TSP.addHint("string");
            CellProcessor[] procs = new CellProcessor[] { TSP };

            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.close();

            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.INT), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.LONG), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.DOUBLE), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.STRING),
                    hintScore + values.length);

            assertEquals(CSVPreparePartsRequest.ColumnType.STRING, TSP.getSuggestedType());
        }
        {
            String[] values = new String[] { "v0\n", "v1\n", "v2\n", "v3\n", "v4\n", };
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sbuf.append(values[i]);
            }

            String text = sbuf.toString();
            byte[] bytes = text.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            CsvPreference pref = new CsvPreference.Builder('"', ',', "\n")
                    .build();
            CsvListReader sampleReader = new CsvListReader(
                    new InputStreamReader(in), pref);

            TypeSuggestionProcessor TSP = new TypeSuggestionProcessor(
                    values.length, hintScore);
            TSP.addHint("int"); // int
            CellProcessor[] procs = new CellProcessor[] { TSP };

            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.close();

            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.INT), hintScore);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.LONG), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.DOUBLE), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.STRING), values.length);

            assertEquals(CSVPreparePartsRequest.ColumnType.STRING, TSP.getSuggestedType());
        }
        {
            String[] values = new String[] { "v0\n", "v1\n", "v2\n", "v3\n", "v4\n", };
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sbuf.append(values[i]);
            }

            String text = sbuf.toString();
            byte[] bytes = text.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            CsvPreference pref = new CsvPreference.Builder('"', ',', "\n")
                    .build();
            CsvListReader sampleReader = new CsvListReader(
                    new InputStreamReader(in), pref);

            TypeSuggestionProcessor TSP = new TypeSuggestionProcessor(
                    values.length, hintScore);
            TSP.addHint("long");
            CellProcessor[] procs = new CellProcessor[] { TSP };

            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.close();

            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.INT), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.LONG), hintScore);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.DOUBLE), 0);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.STRING), values.length);

            assertEquals(CSVPreparePartsRequest.ColumnType.STRING, TSP.getSuggestedType());
        }
        {
            String[] values = new String[] { "0\n", "1\n", "2\n", "3\n", "4\n", };
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sbuf.append(values[i]);
            }

            String text = sbuf.toString();
            byte[] bytes = text.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            CsvPreference pref = new CsvPreference.Builder('"', ',', "\n")
                    .build();
            CsvListReader sampleReader = new CsvListReader(
                    new InputStreamReader(in), pref);

            TypeSuggestionProcessor TSP = new TypeSuggestionProcessor(
                    values.length, hintScore);
            TSP.addHint("int"); // int
            CellProcessor[] procs = new CellProcessor[] { TSP };

            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.read(procs);
            sampleReader.close();

            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.INT), hintScore
                    + values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.LONG), values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.DOUBLE), values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.STRING), values.length);

            assertEquals(CSVPreparePartsRequest.ColumnType.INT, TSP.getSuggestedType());
        }
        {
            String[] values = new String[] { "0\n", "1\n", "2\n", "3\n", "4\n", };
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                sbuf.append(values[i]);
            }

            String text = sbuf.toString();
            byte[] bytes = text.getBytes();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            CsvPreference pref = new CsvPreference.Builder('"', ',', "\n")
                    .build();
            CsvListReader sampleReader = new CsvListReader(
                    new InputStreamReader(in), pref);

            TypeSuggestionProcessor TSP = new TypeSuggestionProcessor(
                    values.length, hintScore);
            TSP.addHint("int"); // int
            CellProcessor[] procs = new CellProcessor[] { TSP };

            for (int i = 0; i < values.length; i++) {
                sampleReader.read(procs);
            }
            sampleReader.close();

            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.INT), hintScore
                    + values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.LONG), values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.DOUBLE), values.length);
            assertEquals(TSP.getScore(CSVPreparePartsRequest.ColumnType.STRING), values.length);

            assertEquals(CSVPreparePartsRequest.ColumnType.INT, TSP.getSuggestedType());
        }
    }

    @Test
    public void parseSeveralTypesOfColumns() throws Exception {
        // request setting
        request.setDelimiterChar(Config.BI_PREPARE_PARTS_DELIMITER_CSV_DEFAULTVALUE.charAt(0)); // ','
        request.setNewLine(CSVPreparePartsRequest.NewLine.LF); // '\n'
        request.setHasColumnHeader(true);
        request.setColumnNames(new String[0]);
        request.setAliasTimeColumn(null);
        request.setOnlyColumns(new String[0]);
        request.setExcludeColumns(new String[0]);
        request.setColumnTypeHints(new String[0]);

        // parser setting
        String text =
                "v0,v1,v2,v3,time\n" +
                "c00,0,0,0.0,12345\n" +
                "c10,1,1,1.1,12345\n" +
                "c20,2,2,2.2,12345\n";
        byte[] bytes = text.getBytes();
        parser.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));
        parser.startParsing(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        // writer
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", "c00", "v1", 0, "v2", 0, "v3", 0.0, "time", 12345 });
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", "c10", "v1", 1, "v2", 1, "v3", 1.1, "time", 12345 });
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", "c20", "v1", 2, "v2", 2, "v3", 2.2, "time", 12345 });

        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertFalse(parser.parseRow(writer));

        assertEquals(3, parser.getRowNum());
    }

    @Test
    public void parseColumnsThatIncludeNullValue() throws Exception {
        // request setting
        request.setDelimiterChar(Config.BI_PREPARE_PARTS_DELIMITER_CSV_DEFAULTVALUE.charAt(0)); // ','
        request.setNewLine(CSVPreparePartsRequest.NewLine.LF); // '\n'
        request.setHasColumnHeader(true);
        request.setColumnNames(new String[0]);
        request.setAliasTimeColumn(null);
        request.setOnlyColumns(new String[0]);
        request.setExcludeColumns(new String[0]);
        request.setColumnTypeHints(new String[0]);

        // parser setting
        String text =
                "v0,v1,v2,v3,time\n" +
                "c00,0,0,0.0,12345\n" +
                ",,,,12345\n" +
                "c20,2,2,2.2,12345\n";
        byte[] bytes = text.getBytes();
        parser.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));
        parser.startParsing(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        // writer
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", "c00", "v1", 0, "v2", 0, "v3", 0.0, "time", 12345 });
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", null, "v1", null, "v2", null, "v3", null, "time", 12345 });
        writer.setColSize(5);
        writer.setRow(new Object[] { "v0", "c20", "v1", 2, "v2", 2, "v3", 2.2, "time", 12345 });

        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertFalse(parser.parseRow(writer));

        assertEquals(3, parser.getRowNum());
    }

    @Test
    public void parseColumnThatIncludedSeveralTypesValues() throws Exception {
        // request setting
        request.setDelimiterChar(Config.BI_PREPARE_PARTS_DELIMITER_CSV_DEFAULTVALUE.charAt(0)); // ','
        request.setNewLine(CSVPreparePartsRequest.NewLine.LF); // '\n'
        request.setHasColumnHeader(true);
        request.setColumnNames(new String[0]);
        request.setAliasTimeColumn(null);
        request.setOnlyColumns(new String[0]);
        request.setExcludeColumns(new String[0]);
        request.setColumnTypeHints(new String[0]);

        // parser setting
        String text =
                "v0,v1,time\n" +
                "0,0,12345\n" +
                "c10,,12345\n" +
                ",2,12345\n";
        byte[] bytes = text.getBytes();
        parser.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));
        parser.startParsing(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        // writer
        writer.setColSize(3);
        writer.setRow(new Object[] { "v0", "0", "v1", 0, "time", 12345 });
        writer.setColSize(3);
        writer.setRow(new Object[] { "v0", "c10", "v1", null, "time", 12345 });
        writer.setColSize(3);
        writer.setRow(new Object[] { "v0", null, "v1", 2, "time", 12345 });

        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertTrue(parser.parseRow(writer));
        assertFalse(parser.parseRow(writer));

        assertEquals(3, parser.getRowNum());
    }

    @Test @Ignore
    public void parseHeaderlessCSVText() throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNS, "v0,v1,time");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "string,string,long");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "c00,c01,12345\n" + "c10,c11,12345\n" + "c20,c21,12345\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseHeaderlessTSVText() throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "tsv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNS, "v0,v1,time");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "string,string,long");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "c00\tc01\t12345\n" + "c10\tc11\t12345\r\n" + "c20\tc21\t12345\r\n";
        byte[] bytes = text.getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseNotSpecifiedTimeColumnHeaderlessCSVTextWithAliasColumnName()
            throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNS, "v0,v1,timestamp");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "string,string,long");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMECOLUMN, "timestamp");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "c00,c01,12345\n" + "c10,c11,12345\n" + "c20,c21,12345\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "timestamp", 12345L,
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "timestamp", 12345L,
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "timestamp", 12345L,
                "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseNotSpecifiedTimeColumnHeaderlessCSVTextWithTimeValue()
            throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNS, "v0,v1");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES, "string,string");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMEVALUE, "12345");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "c00,c01\n" + "c10,c11\n" + "c20,c21\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseHeaderedCSVText() throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "string,string,long");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "v0,v1,time\n" + "c00,c01,12345\n" + "c10,c11,12345\n"
                + "c20,c21,12345\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseNotSpecifiedTimeColumnHeaderedCSVTextWithAliasColumnName01()
            throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "string,string,long");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMECOLUMN, "timestamp");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "v0,v1,timestamp\n" + "c00,c01,12345\n"
                + "c10,c11,12345\n" + "c20,c21,12345\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "timestamp", 12345L,
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "timestamp", 12345L,
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "timestamp", 12345L,
                "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseNotSpecifiedTimeColumnHeaderedCSVTextWithAliasColumnName02()
            throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES,
                "long,string,string");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMECOLUMN, "timestamp");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "timestamp,v0,v1\n" + "12345,c00,c01\n"
                + "12345,c10,c11\n" + "12345,c20,c21\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(4);
        w.setRow(new Object[] { "timestamp", 12345L, "v0", "c00", "v1", "c01",
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "timestamp", 12345L, "v0", "c10", "v1", "c11",
                "time", 12345L });
        w.setColSize(4);
        w.setRow(new Object[] { "timestamp", 12345L, "v0", "c20", "v1", "c21",
                "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }

    @Test @Ignore
    public void parseNotSpecifiedTimeColumnHeaderedCSVTextWithTimeValue()
            throws Exception {
        Properties props = new Properties();
        props.setProperty(Config.BI_PREPARE_PARTS_FORMAT, "csv");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Config.BI_PREPARE_PARTS_COLUMNTYPES, "string,string");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMEVALUE, "12345");
        props.setProperty(Config.BI_PREPARE_PARTS_OUTPUTDIR, "out");
        props.setProperty(Config.BI_PREPARE_PARTS_SPLIT_SIZE, "" + (16 * 1024));
        CSVPreparePartsRequest request = new CSVPreparePartsRequest(
                PreparePartsRequest.Format.CSV, new String[0], props);

        String text = "v0,v1\n" + "c00,c01\n" + "c10,c11\n" + "c20,c21\n";
        byte[] bytes = text.getBytes();
        CSVFileParser p = new CSVFileParser(request);
        p.initParser(FileParser.UTF_8, new ByteArrayInputStream(bytes));

        FileWriterTestUtil w = new FileWriterTestUtil(request);
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c00", "v1", "c01", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c10", "v1", "c11", "time", 12345L });
        w.setColSize(3);
        w.setRow(new Object[] { "v0", "c20", "v1", "c21", "time", 12345L });

        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertTrue(p.parseRow(w));
        assertFalse(p.parseRow(w));

        assertEquals(3, p.getRowNum());

        p.close();
        w.close();
    }
}
