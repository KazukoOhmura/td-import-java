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
package com.treasure_data.td_import;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.treasure_data.client.TreasureDataClient;
import com.treasure_data.client.bulkimport.BulkImportClient;
import com.treasure_data.model.bulkimport.SessionSummary;
import com.treasure_data.td_import.TaskResult;
import com.treasure_data.td_import.prepare.MultiThreadPrepareProcessor;
import com.treasure_data.td_import.prepare.PrepareConfiguration;
import com.treasure_data.td_import.prepare.SequentialUploadTask;
import com.treasure_data.td_import.upload.MultiThreadUploadProcessor;
import com.treasure_data.td_import.upload.UploadConfiguration;
import com.treasure_data.td_import.upload.UploadProcessor;

public class BulkImportCommand extends BulkImport {
    private static final Logger LOG = Logger.getLogger(BulkImportCommand.class.getName());

    protected CommandHelper commandHelper;

    public BulkImportCommand(Properties props) {
        super(props);
        commandHelper = new CommandHelper();
    }

    public void doCommand(final Configuration.Command cmd, final String[] args) throws Exception {
        if (cmd.equals(Configuration.Command.PREPARE)) {
            doPrepareCommand(args);
        } else if (cmd.equals(Configuration.Command.UPLOAD)) {
            doUploadCommand(args);
        } else if (cmd.equals(Configuration.Command.AUTO)) {
            String[] realargs = new String[args.length + 3];
            realargs[args.length] = Configuration.BI_UPLOAD_AUTO_COMMIT_HYPHEN;
            realargs[args.length + 1] = Configuration.BI_UPLOAD_AUTO_PERFORM_HYPHEN;
            realargs[args.length + 2] = Configuration.BI_UPLOAD_AUTO_DELETE_HYPHEN;
            System.arraycopy(args, 0, realargs, 0, args.length);

            props.setProperty(Configuration.CMD_AUTO_ENABLE, "true");

            doUploadCommand(realargs);
        } else {
            throw new UnsupportedOperationException("Fatal error");
        }
    }

    public void doPrepareCommand(final String[] args) throws Exception {
        LOG.info(String.format("Start '%s' command", Configuration.CMD_PREPARE));

        // create configuration for 'prepare' processing
        PrepareConfiguration conf = createPrepareConf(props, args);

        // extract and get file names from command-line arguments
        String[] fileNames = getFileNames(conf, 1);

        commandHelper.showPrepare(fileNames, conf.getOutputDirName());

        MultiThreadPrepareProcessor proc =
                createAndStartMultiThreadPrepareProcessor(conf);

        // create prepare tasks
        com.treasure_data.td_import.prepare.Task[] tasks = createPrepareTasks(conf, fileNames);

        // start prepare tasks
        startPrepareTasks(conf, tasks);

        // wait for finishing prepare processing
        proc.joinWorkers();

        // extract task results of each prepare processing
        List<com.treasure_data.td_import.prepare.TaskResult> prepareResults =
                proc.getTaskResults();

        commandHelper.showPrepareResults(prepareResults);
        commandHelper.listNextStepOfPrepareProc(prepareResults);

        LOG.info(String.format("Finished '%s' command", Configuration.CMD_PREPARE));
    }

    public void doUploadCommand(final String[] args) throws Exception {
        LOG.info(String.format("Start '%s' command", Configuration.CMD_UPLOAD));

        // create configuration for 'upload' processing
        UploadConfiguration uploadConf = createUploadConf(props, args);

        // create TreasureDataClient and BulkImportClient objects
        TreasureDataClient tdClient = new TreasureDataClient(uploadConf.getProperties());
        BulkImportClient biClient = new BulkImportClient(tdClient);

        // configure session name
        TaskResult<?> e = null;
        String sessionName;
        int filePos;
        if (uploadConf.autoCreate()) { // 'auto-create-session'
            // create session automatically
            sessionName = createBulkImportSessionName(uploadConf, tdClient, biClient);

            filePos = 1;
        } else {
            // get session name from command-line arguments
            sessionName = getBulkImportSessionName(uploadConf);

            // validate that the session is live or not
            e = UploadProcessor.checkSession(biClient, uploadConf, sessionName);
            if (e.error != null) {
                throw new IllegalArgumentException(e.error);
            }

            filePos = 2;
        }

        // if session is already freezed, exception is thrown.
        SessionSummary sess = UploadProcessor.showSession(biClient, uploadConf, sessionName);
        if (sess.uploadFrozen()) {
            throw new IllegalArgumentException(String.format(
                    "Bulk import session %s is already freezed. Please check it with 'td import:show %s'",
                    sessionName, sessionName));
        }

        // get and extract uploaded files from command-line arguments
        final String[] fileNames = getFileNames(uploadConf, filePos);

        commandHelper.showUpload(fileNames, sessionName);

        MultiThreadUploadProcessor uploadProc =
                createAndStartMultiThreadUploadProcessor(uploadConf);

        if (!uploadConf.hasPrepareOptions()) {
            // create upload tasks
            com.treasure_data.td_import.upload.UploadTask[] tasks = createUploadTasks(
                    sessionName, fileNames);

            // start upload tasks
            startUploadTasks(uploadConf, tasks);
        } else {
            // create configuration for 'prepare' processing
            PrepareConfiguration prepareConf = createPrepareConf(props, args, true);

            MultiThreadPrepareProcessor prepareProc =
                    createAndStartMultiThreadPrepareProcessor(prepareConf);

            // create sequential upload (prepare) tasks
            com.treasure_data.td_import.prepare.Task[] tasks = createSequentialUploadTasks(
                    sessionName, fileNames);

            // start sequential upload (prepare) tasks
            startPrepareTasks(prepareConf, tasks);

            List<com.treasure_data.td_import.prepare.TaskResult> prepareResults =
                    stopMultiThreadPrepareProcessor(prepareProc);

            commandHelper.showPrepareResults(prepareResults);
            commandHelper.listNextStepOfPrepareProc(prepareResults);

            if (!hasNoPrepareError(prepareResults)) {
                return;
            }

            // end of file list
            try {
                MultiThreadUploadProcessor.addFinishTask(uploadConf);
            } catch (Throwable t) {
                LOG.severe("Error occurred During 'addFinishTask' method call");
                LOG.throwing("Main", "addFinishTask", t);
            }
        }

        List<com.treasure_data.td_import.upload.TaskResult> uploadResults =
                stopMultiThreadUploadProcessor(uploadProc);

        commandHelper.showUploadResults(uploadResults);
        commandHelper.listNextStepOfUploadProc(uploadResults, sessionName);

        if (!hasNoUploadError(uploadResults)) {
            return;
        }

        // 'auto-perform' and 'auto-commit'
        UploadProcessor.processAfterUploading(biClient, uploadConf, sessionName);

        if (uploadConf.autoDelete()) {
            // 'auto-delete-session'
            UploadProcessor.deleteSession(biClient, uploadConf, sessionName);
        }

        LOG.info(String.format("Finished '%s' command", Configuration.CMD_UPLOAD));
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Command not specified");
        }

        String cmdName = args[0].toLowerCase();
        Configuration.Command cmd = Configuration.Command.fromString(cmdName);
        if (cmd == null) {
            throw new IllegalArgumentException(String.format("Command not support: %s", cmdName));
        }

        Properties props = System.getProperties();
        new BulkImportCommand(props).doCommand(cmd, args);
    }
}
