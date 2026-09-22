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

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Test class for JaegerTraceLogger.
 */
public class JaegerTraceLoggerTest {

    @Test
    public void testNoArgConstructorCanBeSilenced() {
        JaegerTraceLogger logger = new JaegerTraceLogger();
        logger.setLogLevel(Level.OFF);

        assertFalse(logger.isLoggable(Level.SEVERE));
    }

    @Test
    public void testConsoleConstructorRespectsLogLevel() {
        JaegerTraceLogger logger = new JaegerTraceLogger(true, null);
        logger.setLogLevel(Level.INFO);

        assertTrue(logger.isLoggable(Level.INFO));
        assertFalse(logger.isLoggable(Level.FINE));
    }

    @Test
    public void testFileConstructorWritesLogFile() throws IOException {
        Path logFile = Files.createTempFile("jaeger-trace-log-test", ".log");
        logFile.toFile().deleteOnExit();

        JaegerTraceLogger logger = new JaegerTraceLogger(false, logFile);
        logger.setLogLevel(Level.ALL);
        logger.printInfo("hello from test");
        flushHandlers();

        String content = Files.readString(logFile);
        assertTrue(content.contains("hello from test"));
    }

    @Test
    public void testFileConstructorWithInvalidPathThrows() throws IOException {
        // A path under a non-existent subdirectory of a real temp dir: FileHandler
        // won't create missing parent directories, so this reliably fails without
        // hard-coding a platform-specific absolute pathname.
        Path invalidPath = Files.createTempDirectory("jaeger-trace-log-test")
                .resolve("does-not-exist").resolve("trace.log");

        RuntimeException e = expectThrows(RuntimeException.class,
                () -> new JaegerTraceLogger(false, invalidPath));
        assertTrue(e.getMessage().contains("failed to setup Jaeger trace log file"));
    }

    @Test
    public void testAllPrintMethodsAreLoggableAtAllLevel() {
        JaegerTraceLogger logger = new JaegerTraceLogger(true, null);
        logger.setLogLevel(Level.ALL);

        // Smoke test: none of these should throw, exercising every log level.
        logger.printSevere("severe message");
        logger.printWarning("warning message");
        logger.printInfo("info message");
        logger.printConfig("config message");
        logger.printFine("fine message");
        logger.printFiner("finer message");
        logger.printFinest("finest message");

        assertTrue(logger.isLoggable(Level.FINEST));
    }

    private void flushHandlers() {
        Logger traceLogger = Logger.getLogger(JaegerTraceLogger.JAEGER_TRACE_LOG);
        for (Handler handler : traceLogger.getHandlers()) {
            handler.flush();
        }
    }
}
