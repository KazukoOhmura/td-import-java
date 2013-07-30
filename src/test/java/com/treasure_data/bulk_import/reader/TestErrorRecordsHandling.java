package com.treasure_data.bulk_import.reader;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.treasure_data.bulk_import.Configuration;
import com.treasure_data.bulk_import.prepare_parts.PrepareConfiguration;
import com.treasure_data.bulk_import.prepare_parts.PreparePartsException;
import com.treasure_data.bulk_import.prepare_parts.Task;
import com.treasure_data.bulk_import.writer.FileWriterTestUtil;

public class TestErrorRecordsHandling {

    protected Properties props;
    protected PrepareConfiguration conf;
    protected FileWriterTestUtil writer;
    protected FileReader reader;

    @Before
    public void createResources() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }

    @Test
    public void workCSVFileReaderWithNormalCSVFile() throws Exception {
        // create property and configuration
        props = new Properties();
        props.setProperty(Configuration.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Configuration.BI_PREPARE_PARTS_SAMPLE_ROWSIZE, "1");
        conf = PrepareConfiguration.Format.CSV.createPrepareConfiguration();
        conf.configure(props);

        // create reader and writer
        createCSVFileReader(createNormalCSVFileTask());

        doRead();

        assertEquals(4, reader.getLineNum());
        assertEquals(3, writer.getRowNum());
    }

    @Test
    public void skipCSVFileReaderWithInvalidCSVFile() throws Exception {
        // create property and configuration
        props = new Properties();
        props.setProperty(Configuration.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Configuration.BI_PREPARE_PARTS_ERROR_RECORDS_HANDLING, "skip"); // default value
        props.setProperty(Configuration.BI_PREPARE_PARTS_SAMPLE_ROWSIZE, "1");
        conf = PrepareConfiguration.Format.CSV.createPrepareConfiguration();
        conf.configure(props);

        // create reader and writer
        createCSVFileReader(createInvalidCSVFileTask());

        doRead();

        assertEquals(4, reader.getLineNum());
        assertEquals(2, writer.getRowNum());
    }

    @Test
    public void abortCSVFileReaderWithInvalidCSVFile() throws Exception {
        // create property and configuration
        props = new Properties();
        props.setProperty(Configuration.BI_PREPARE_PARTS_COLUMNHEADER, "true");
        props.setProperty(Configuration.BI_PREPARE_PARTS_ERROR_RECORDS_HANDLING, "abort"); // default value
        props.setProperty(Configuration.BI_PREPARE_PARTS_SAMPLE_ROWSIZE, "1");
        conf = PrepareConfiguration.Format.CSV.createPrepareConfiguration();
        conf.configure(props);

        // create reader and writer
        createCSVFileReader(createInvalidCSVFileTask());

        try {
            doRead();
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof PreparePartsException);
        }

        assertEquals(3, reader.getLineNum());
        assertEquals(1, writer.getRowNum());
    }

    private void createCSVFileReader(Task task) throws Exception {
        writer = new FileWriterTestUtil(conf);
        reader = PrepareConfiguration.Format.CSV.createFileReader(conf, writer);
        reader.configure(task);
        writer.setColumnNames(reader.getColumnNames());
        writer.setColumnTypes(reader.getColumnTypes());
        writer.setSkipColumns(reader.getSkipColumns());
    }

    private void doRead() throws Exception {
        try {
            reader.next();
            writer.clear();

            reader.next();
            writer.clear();

            reader.next();
            writer.clear();
        } finally {
            reader.close();
        }
    }

    private Task createNormalCSVFileTask() {
        Task task = new Task("dummy.txt");
        task = spy(task);
        task.isTest = true;
        task.testBinary = "name,count,time\nbar,10,1000\nfoo,11,1100\nbaz,12,1200".getBytes();
        return task;
    }

    private Task createInvalidCSVFileTask() {
        Task task = new Task("dummy.txt");
        task = spy(task);
        task.isTest = true;
        task.testBinary = "name,count,time\nbar,10,1000\nfoo,111100\nbaz,12,1200".getBytes();
        return task;
    }

}