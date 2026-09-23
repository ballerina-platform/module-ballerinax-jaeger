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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.testng.Assert.assertTrue;

/**
 * Test class for JaegerTraceLogFormatter.
 */
public class JaegerTraceLogFormatterTest {

    private final JaegerTraceLogFormatter formatter = new JaegerTraceLogFormatter();

    @Test
    public void testFormatIncludesLevelSourceAndMessage() {
        LogRecord record = new LogRecord(Level.WARNING, "connection refused");
        record.setLoggerName("jaeger.tracelog");

        String formatted = formatter.format(record);

        assertTrue(formatted.contains("WARN"));
        assertTrue(formatted.contains("jaeger.tracelog"));
        assertTrue(formatted.contains("connection refused"));
    }

    @Test
    public void testFormatWithoutThrowableHasNoStackTrace() {
        LogRecord record = new LogRecord(Level.INFO, "started");
        record.setLoggerName("jaeger.tracelog");

        String formatted = formatter.format(record);

        assertTrue(formatted.contains("INFO"));
        assertTrue(formatted.contains("started"));
        assertTrue(formatted.trim().endsWith("started"));
    }

    @Test
    public void testFormatWithThrowableAppendsStackTrace() {
        LogRecord record = new LogRecord(Level.SEVERE, "export failed");
        record.setLoggerName("jaeger.tracelog");
        record.setThrown(new RuntimeException("boom"));

        String formatted = formatter.format(record);

        assertTrue(formatted.contains("ERROR"));
        assertTrue(formatted.contains("export failed"));
        assertTrue(formatted.contains("RuntimeException: boom"));
    }
}
