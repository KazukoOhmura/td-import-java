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
package com.treasure_data.bulk_import.upload_parts;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import com.treasure_data.client.ClientException;
import com.treasure_data.client.TreasureDataClient;
import com.treasure_data.client.bulkimport.BulkImportClient;
import com.treasure_data.model.DatabaseSummary;
import com.treasure_data.model.TableSummary;
import com.treasure_data.model.bulkimport.Session;
import com.treasure_data.model.bulkimport.SessionSummary;

public class UploadProcessor {

    static interface Retryable2 {
        void doTry() throws ClientException, IOException;
    }

    static class RetryClient2 {
        public void retry(Retryable2 r, String sessionName,
                int retryCount, long waitSec) throws IOException {
            ClientException firstException = null;
            int count = 0;
            while (true) {
                try {
                    r.doTry();
                    if (count > 0) {
                        String msg = String.format("Retry succeeded. %s", sessionName);
                        System.out.println(msg);
                        LOG.warning(msg);
                    }
                    break;
                } catch (ClientException e) {
                    if (firstException == null) {
                        firstException = e;
                    }
                    String msg = String.format("ClientError occurred. the cause is '%s'. %s",
                            e.getMessage(), sessionName);
                    System.out.println(msg);
                    LOG.warning(msg);
                    if (count >= retryCount) {
                        String msg2 = String.format("Retry count exceeded limit. %s",
                                sessionName);
                        System.out.println(msg);
                        LOG.warning(msg2);
                        throw new IOException("Retry failed", firstException);
                    } else {
                        count++;
                        String msg2 = String.format("Retrying. %s", sessionName);
                        System.out.println(msg2);
                        LOG.warning(msg2);
                        try {
                            Thread.sleep(waitSec);
                        } catch (InterruptedException ex) { // ignore
                        }
                    }
                }
            }
        }
    }

    static class RetryClient3 {
        public void retry(Retryable2 r, String sessionName, String partID,
                int retryCount, long waitSec) throws IOException {
            ClientException firstException = null;
            int count = 0;
            while (true) {
                try {
                    r.doTry();
                    if (count > 0) {
                        LOG.warning(String.format("Retry succeeded. %s.'%s'",
                                sessionName, partID));
                    }
                    break;
                } catch (ClientException e) {
                    if (firstException == null) {
                        firstException = e;
                    }
                    LOG.warning(String.format(
                            "ClientError occurred. the cause is '%s'. %s.'%s'",
                            e.getMessage(), sessionName, partID));
                    if (count >= retryCount) {
                        LOG.warning(String.format(
                                "Retry count exceeded limit. %s.'%s'",
                                sessionName, partID));
                        throw new IOException("Retry failed", firstException);
                    } else {
                        count++;
                        LOG.warning(String.format("Retrying. %s.'%s'",
                                sessionName, partID));
                        try {
                            Thread.sleep(waitSec);
                        } catch (InterruptedException ex) { // ignore
                        }
                    }
                }
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(UploadProcessor.class.getName());

    private static SessionSummary summary;
    private static RetryClient2 retryClient = new RetryClient2();

    private BulkImportClient client;
    private UploadConfiguration conf;

    public UploadProcessor(BulkImportClient client, UploadConfiguration conf) {
        this.conf = conf;
        this.client = client;
    }

    public ErrorInfo execute(final Task task) {
        ErrorInfo err = new ErrorInfo();
        err.task = task;
        try {
            String startMessage = String.format(
                    "Upload file '%s' (size %d) to session '%s' as part '%s'",
                    task.fileName, task.size, task.sessName, task.partName);
            System.out.println(startMessage);
            LOG.info(startMessage);

            long time = System.currentTimeMillis();
            new RetryClient3().retry(new Retryable2() {
                @Override
                public void doTry() throws ClientException, IOException {
                    executeUpload(task);
                }
            }, task.sessName, task.partName, conf.getRetryCount(),
                    conf.getWaitSec() * 1000);
            time = System.currentTimeMillis() - time;

            String endMessage = String.format(
                    "Uploaded file '%s' (size %d) to session '%s' as part '%s' (time: %d sec.)", 
                    task.fileName, task.size, task.sessName, task.partName, (time / 1000));
            System.out.println(endMessage);
            LOG.info(endMessage);
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            err.error = e;
        }
        return err;
    }

    protected void executeUpload(final Task task) throws ClientException, IOException {
        LOG.fine(String
                .format("Upload file '%s' (size %d) to session '%s' as part '%s' by thread '%s'",
                        task.fileName, task.size, task.sessName, task.partName, Thread.currentThread().getName()));

        long time = System.currentTimeMillis();
        Session session = new Session(task.sessName, null, null);
        client.uploadPart(session, task.partName, createInputStream(task),
                (int) task.size);
        time = System.currentTimeMillis() - time;

        LOG.fine(String
                .format("Uploaded file '%s' (size %d) to session '%s' as part '%s' by thread '%s' (time: %d sec.)",
                        task.fileName, task.size, task.sessName, task.partName,
                        Thread.currentThread().getName(), (time / 1000)));
    }

    protected InputStream createInputStream(final Task task) throws IOException {
        if (!task.isTest) {
            return new BufferedInputStream(new FileInputStream(task.fileName));
        } else {
            return new ByteArrayInputStream(task.testBinary);
        }
    }

    public static SessionSummary showSession(final BulkImportClient client,
            final UploadConfiguration conf, final String sessName) throws UploadPartsException {
        LOG.fine(String.format("Show session '%s'", sessName));

        summary = null;
        try {
            retryClient.retry(new Retryable2(){
                @Override
                public void doTry() throws ClientException, IOException {
                    summary = client.showSession(sessName);
                }
            }, sessName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            throw new UploadPartsException(e);
        }
        return summary;
    }

    public static ErrorInfo freezeSession(final BulkImportClient client,
            final UploadConfiguration conf, final String sessName) throws UploadPartsException {
        LOG.info(String.format("Freeze session '%s'", sessName));

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2(){
                @Override
                public void doTry() throws ClientException, IOException {
                    Session session = new Session(sessName, null, null);
                    client.freezeSession(session);
                }
            }, sessName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            err.error = e;
        }
        return err;
    }

    public static ErrorInfo performSession(final BulkImportClient client,
            final UploadConfiguration conf, final String sessName) throws UploadPartsException {
        LOG.info(String.format("Perform session '%s'", sessName));

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2(){
                @Override
                public void doTry() throws ClientException, IOException {
                    Session session = new Session(sessName, null, null);
                    client.performSession(session);
                }
            }, sessName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            err.error = e;
        }
        return err;
    }

    public static ErrorInfo waitPerform(final BulkImportClient client,
            final UploadConfiguration conf, final String sessName) throws UploadPartsException {
        LOG.info(String.format("Wait session performing '%s'", sessName));

        ErrorInfo err = new ErrorInfo();
        long waitTime = System.currentTimeMillis();
        while (true) {
            try {
                retryClient.retry(new Retryable2(){
                    @Override
                    public void doTry() throws ClientException, IOException {
                        summary = client.showSession(sessName);
                    }
                }, sessName, conf.getRetryCount(), conf.getWaitSec());

                if (summary.getStatus() == "ready") {
                    break;
                } else if (summary.getStatus() == "uploading") {
                    throw new IOException("performing failed");
                }

                try {
                    long deltaTime = System.currentTimeMillis() - waitTime;
                    LOG.fine(String.format("Waiting for %d sec.", (deltaTime / 1000)));
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            } catch (IOException e) {
                LOG.severe(e.getMessage());
                err.error = e;
                break;
            }
        }

        return err;
    }

    public static ErrorInfo commitSession(final BulkImportClient client,
            final UploadConfiguration conf, final String sessName) throws UploadPartsException {
        LOG.info(String.format("Commit session '%s'", sessName));

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2(){
                @Override
                public void doTry() throws ClientException, IOException {
                    Session session = new Session(sessName, null, null);
                    client.commitSession(session);
                }
            }, sessName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            err.error = e;
        }
        return err;
    }

    public static ErrorInfo validateDatabase(final TreasureDataClient client, final UploadConfiguration conf,
            final String sessionName, final String databaseName) throws UploadPartsException {
        String m = String.format("Check database '%s'", databaseName);
        System.out.println(m);
        LOG.info(m);

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2() {
                @Override
                public void doTry() throws ClientException, IOException {
                    List<DatabaseSummary> dbs = client.listDatabases();
                    boolean exist = false;
                    for (DatabaseSummary db : dbs) {
                        if (db.getName().equals(databaseName)) {
                            exist = true;
                            break;
                        }
                    }

                    if (!exist) {
                        throw new IOException(String.format(
                                "Not found database '%s'", databaseName));
                    }
                }
            }, sessionName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            String msg = String.format("Cannot access database '%s', %s", databaseName, e.getMessage());
            System.out.println(msg);
            LOG.severe(msg);
            err.error = e;
        }

        return err;
    }

    public static ErrorInfo validateTable(final TreasureDataClient client, final UploadConfiguration conf,
            final String sessionName, final String databaseName, final String tableName) throws UploadPartsException {
        String m = String.format("Check table '%s'", tableName);
        System.out.println(m);
        LOG.info(m);

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2() {
                @Override
                public void doTry() throws ClientException, IOException {
                    List<TableSummary> tbls = client.listTables(databaseName);
                    boolean exist = false;
                    for (TableSummary tbl : tbls) {
                        if (tbl.getName().equals(tableName)) {
                            exist = true;
                            break;
                        }
                    }

                    if (!exist) {
                        throw new IOException(String.format(
                                "Not found table '%s'", tableName));
                    }
                }
            }, sessionName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            String msg = String.format("Cannot access table '%s', %s", tableName, e.getMessage());
            System.out.println(msg);
            LOG.severe(msg);
            err.error = e;
        }
        return err;
    }

    public static ErrorInfo createSession(final BulkImportClient client, final UploadConfiguration conf,
            final String sessionName, final String databaseName, final String tableName) throws UploadPartsException {
        String m = String.format("Create bulk_import session '%s'", sessionName);
        System.out.println(m);
        LOG.info(m);

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2() {
                @Override
                public void doTry() throws ClientException, IOException {
                    client.createSession(sessionName, databaseName, tableName);
                }
            }, sessionName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            String msg = String.format("Cannot create bulk_import session '%s' by using '%s:%s', %s",
                    sessionName, databaseName, tableName, e.getMessage());
            System.out.println(msg);
            LOG.severe(msg);
            err.error = e;
        }
        return err;
    }

    public static ErrorInfo validateSession(final BulkImportClient client, final UploadConfiguration conf,
            final String sessionName) throws UploadPartsException {
        String m = String.format("Check bulk_import session '%s'", sessionName);
        System.out.println(m);
        LOG.info(m);

        ErrorInfo err = new ErrorInfo();
        try {
            retryClient.retry(new Retryable2() {
                @Override
                public void doTry() throws ClientException, IOException {
                    client.showSession(sessionName);
                }
            }, sessionName, conf.getRetryCount(), conf.getWaitSec());
        } catch (IOException e) {
            String msg = String.format("Cannot access bulk_import session '%s', %s", sessionName, e.getMessage());
            System.out.println(msg);
            LOG.severe(msg);
            err.error = e;
        }
        return err;
    }

}
