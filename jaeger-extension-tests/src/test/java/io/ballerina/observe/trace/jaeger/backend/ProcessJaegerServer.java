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

import org.ballerinalang.test.context.Utils;

import java.io.File;
import java.util.logging.Logger;

/**
 * Executable program based Jaeger server.
 *
 * This starts and stops the server using scripts. The script paths need to be provided as environment variables.
 */
public class ProcessJaegerServer implements JaegerServer {
    private static final Logger LOGGER = Logger.getLogger(ContainerizedJaegerServer.class.getName());
    public static final String EXECUTABLE_ENV_VAR_KEY = "JAEGER_SERVER_EXECUTABLE";

    private final String executableFile;
    private Process jaegerServerProcess;

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
    public void startServer(String interfaceIP, int udpBindPort) throws Exception {
        if (jaegerServerProcess != null) {
            throw new IllegalStateException("Jaeger server already started");
        }
        jaegerServerProcess = new ProcessBuilder()
                .command(executableFile, "--collector.zipkin.http-port=9411",
                        "--processor.zipkin-compact.server-host-port=5775")
                .start();
        Utils.waitForPortsToOpen(new int[]{udpBindPort}, 30000, true, interfaceIP);
        LOGGER.info("Started Jaeger process with process ID " + jaegerServerProcess.pid());
    }

    @Override
    public void stopServer() {
        if (jaegerServerProcess != null) {
            long processID = jaegerServerProcess.pid();
            jaegerServerProcess.destroy();
            LOGGER.info("Stopped Jaeger process with process ID " + processID);
            jaegerServerProcess = null;
        }
    }

    @Override
    public void cleanUp() {
        // Do Nothing
    }
}
