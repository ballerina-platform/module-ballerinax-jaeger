/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.trace.jaeger.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Log Reader which reads and prints the output of a Process.
 */
public final class ProcessLogReader implements Runnable, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLogReader.class);
    private final InputStream inputStream;
    private final Thread workerThread;
    private boolean isRunning;

    public ProcessLogReader(InputStream inputStream) {
        this.inputStream = inputStream;
        isRunning = true;
        workerThread = new Thread(this);
        workerThread.start();
    }

    @Override
    public void run() {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            while (isRunning) {
                if (bufferedReader.ready()) {
                    String s = bufferedReader.readLine();
                    if (s == null) {
                        break;
                    }
                    LOGGER.info(s);
                } else {
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            String s = bufferedReader.readLine();
            if (s != null) {
                LOGGER.info(s);
            }
        } catch (Throwable t) {
            LOGGER.error("Problem reading process output stream", t);
        }
    }

    @Override
    public void close() throws Exception {
        isRunning = false;
        workerThread.join(10000);
    }
}
