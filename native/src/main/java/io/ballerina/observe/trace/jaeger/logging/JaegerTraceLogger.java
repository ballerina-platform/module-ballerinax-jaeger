/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
package io.ballerina.observe.trace.jaeger.logging;


import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JaegerTraceLogger {
    public static final String JAEGER_TRACE_LOG = "jaeger.tracelog";
    private static final Logger traceLogger = Logger.getLogger(JAEGER_TRACE_LOG);

    public JaegerTraceLogger(boolean traceLogConsole, Path logFilePath) {
        if (traceLogConsole) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new JaegerTraceLogFormatter());
            traceLogger.addHandler(consoleHandler);
        }
        if (logFilePath != null) {
            try {
                FileHandler fileHandler = new FileHandler(logFilePath.toString(), true);
                fileHandler.setFormatter(new JaegerTraceLogFormatter());
                traceLogger.addHandler(fileHandler);

            } catch (IOException e) {
                throw new RuntimeException("failed to setup Jaeger trace log file: " + logFilePath, e);
            }
        }
        traceLogger.setUseParentHandlers(false);
    }

    public JaegerTraceLogger() {
        traceLogger.setUseParentHandlers(false);
    }

    public void setLogLevel(Level logLevel) {
        traceLogger.setLevel(logLevel);
    }

    public void printInfo(String message) {
        traceLogger.log(Level.INFO, message);
    }

    public void printFine(String message) {
        traceLogger.log(Level.FINE, message);
    }

    public void printFiner(String message) {
        traceLogger.log(Level.FINER, message);
    }

    public void  printFinest(String message) {
        traceLogger.log(Level.FINEST, message);
    }

    public void printConfig(String message) {
        traceLogger.log(Level.CONFIG, message);
    }

    public void printWarning(String message) {
        traceLogger.log(Level.WARNING, message);
    }

    public void printSevere(String message) {
        traceLogger.log(Level.SEVERE, message);
    }
}
