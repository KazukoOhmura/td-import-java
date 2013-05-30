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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.treasure_data.client.TreasureDataClient;
import com.treasure_data.client.bulkimport.BulkImportClient;

public class MultiThreadUploadProcessor {
    static class Worker extends Thread {
        private MultiThreadUploadProcessor parent;
        private UploadProcessor proc;
        AtomicBoolean isFinished = new AtomicBoolean(false);

        public Worker(MultiThreadUploadProcessor parent, UploadProcessor proc) {
            this.parent = parent;
            this.proc = proc;
        }

        @Override
        public void run() {
            while (true) {
                UploadProcessor.Task t = parent.taskQueue.poll();
                if (t == null) {
                    continue;
                } else if (UploadProcessor.Task.endTask(t)) {
                    break;
                }

                UploadProcessor.ErrorInfo error = proc.execute(t);
                if (error != null) {
                    parent.setErrors(error);
                }
            }
            isFinished.set(true);
        }
    }

    private static BlockingQueue<UploadProcessor.Task> taskQueue;

    static {
        taskQueue = new LinkedBlockingQueue<UploadProcessor.Task>();
    }

    public static synchronized void addTask(UploadProcessor.Task task) {
        taskQueue.add(task);
    }

    public static synchronized void addFinishTask(UploadConfig conf) {
        for (int i = 0; i < conf.getNumOfUploadThreads(); i++) {
            taskQueue.add(UploadProcessor.Task.FINISH_TASK);
        }
    }

    private UploadConfig conf;
    private List<Worker> workers;
    private List<UploadProcessor.ErrorInfo> errors;

    public MultiThreadUploadProcessor(UploadConfig conf) {
        this.conf = conf;
        workers = new ArrayList<Worker>();
        errors = new ArrayList<UploadProcessor.ErrorInfo>();
    }

    protected void setErrors(UploadProcessor.ErrorInfo error) {
        errors.add(error);
    }

    public List<UploadProcessor.ErrorInfo> getErrors() {
        return errors;
    }

    public void registerWorkers() {
        for (int i = 0; i < conf.getNumOfUploadThreads(); i++) {
            addWorker(createWorker(conf));
        }
    }

    protected Worker createWorker(UploadConfig conf) {
        return new Worker(this, createUploadProcessor(conf));
    }

    protected void addWorker(Worker w) {
        workers.add(w);
    }

    protected UploadProcessor createUploadProcessor(UploadConfig conf) {
        return new UploadProcessor(createBulkImportClient(conf), conf);
    }

    protected BulkImportClient createBulkImportClient(UploadConfig conf) {
        return new BulkImportClient(new TreasureDataClient(conf.getProperties()));
    }

    public void startWorkers() {
        for (int i = 0; i < workers.size(); i++) {
            workers.get(i).start();
        }
    }

    public void joinWorkers() {
        long waitSec = 1 * 1000;
        while (!workers.isEmpty()) {
            Worker last = workers.get(workers.size() - 1);
            if (last.isFinished.get()) {
                workers.remove(workers.size() - 1);
            }

            try {
                Thread.sleep(waitSec);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    // auto perform

    // freeze

    // perform
}
