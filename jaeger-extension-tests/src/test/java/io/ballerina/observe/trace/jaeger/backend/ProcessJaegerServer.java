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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Executable program based Jaeger server.
 *
 * This starts and stops the server using scripts. The script paths need to be provided as environment variables.
 */
public class ProcessJaegerServer implements JaegerServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessJaegerServer.class);
    public static final String EXECUTABLE_ENV_VAR_KEY = "JAEGER_SERVER_EXECUTABLE";

    private final String executableFile;
    private Process jaegerServerProcess;
    private ProcessLogReader processOutputLogReader;
    private ProcessLogReader processErrorLogReader;

    public ProcessJaegerServer() {
        executableFile = System.getenv(EXECUTABLE_ENV_VAR_KEY);
        File executableFileFile = new File(executableFile);
        if (!executableFileFile.exists()) {
            throw new IllegalStateException("Jaeger executable " + executableFile + " does not exist");
        }
        if (!executableFileFile.isFile()) {
            throw new IllegalStateException("Jaeger executable " + executableFile + " is not a file");
        }
    }

    @Override
    public void startServer(String interfaceIP, int udpBindPort, JaegerServerProtocol protocol) throws IOException {
        if (jaegerServerProcess != null || processOutputLogReader != null || processErrorLogReader != null) {
            throw new IllegalStateException("Jaeger server already started");
        }
        String processorFlag;
        switch (protocol) {
            case UDP_COMPACT_THRIFT:
                processorFlag = "--processor.jaeger-compact.server-host-port";
                break;
            default:
                throw new IllegalArgumentException("Unknown Jaeger Protocol type " + protocol);
        }
        String bindPort = interfaceIP + ":" + udpBindPort;
        jaegerServerProcess = new ProcessBuilder()
                .command(executableFile, processorFlag, bindPort)
                .start();
        LOGGER.info("Started Jaeger process with process ID " + jaegerServerProcess.pid());

        processOutputLogReader = new ProcessLogReader(jaegerServerProcess.getInputStream());
        processErrorLogReader = new ProcessLogReader(jaegerServerProcess.getErrorStream());
    }

    @Override
    public void stopServer() throws Exception {
        if (jaegerServerProcess != null) {
            long processID = jaegerServerProcess.pid();
            processOutputLogReader.close();
            processErrorLogReader.close();
            jaegerServerProcess.destroy();
            jaegerServerProcess.waitFor(10000, TimeUnit.SECONDS);
            LOGGER.info("Stopped Jaeger process with process ID " + processID);

            jaegerServerProcess = null;
            processOutputLogReader = null;
            processErrorLogReader = null;
        }
    }

    @Override
    public void cleanUp() {
        // Do Nothing
    }
}
