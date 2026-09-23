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
package io.ballerina.observe.trace.jaeger;

import io.ballerina.observe.trace.jaeger.logging.JaegerTraceLogger;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test class for JaegerExporter.
 */
public class JaegerExporterTest {

    private SpanExporter mockExporter;
    private JaegerExporter jaegerExporter;
    private static final String TEST_ENDPOINT = "localhost:14250";

    @BeforeMethod
    public void setUp() {
        mockExporter = mock(SpanExporter.class);
    }

    @Test
    public void testExportSuccess() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        List<SpanData> spans = createMockSpans(3);
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute
        CompletableResultCode result = jaegerExporter.export(spans);

        // Verify
        verify(mockExporter, times(1)).export(spans);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testExportFailure() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        List<SpanData> spans = createMockSpans(2);
        CompletableResultCode failureCode = CompletableResultCode.ofFailure();

        when(mockExporter.export(any())).thenReturn(failureCode);

        // Execute
        CompletableResultCode result = jaegerExporter.export(spans);

        // Verify
        verify(mockExporter, times(1)).export(spans);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testExportWithEmptySpans() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        List<SpanData> spans = new ArrayList<>();
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute
        CompletableResultCode result = jaegerExporter.export(spans);

        // Verify
        verify(mockExporter, times(1)).export(spans);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testExportSpansArePassed() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        List<SpanData> spans = createMockSpans(5);
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute
        jaegerExporter.export(spans);

        // Verify - capture the argument and verify it's the same collection
        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(mockExporter).export(captor.capture());
        assertEquals(captor.getValue().size(), 5);
    }

    @Test
    public void testFlush() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.flush()).thenReturn(successCode);

        // Execute
        CompletableResultCode result = jaegerExporter.flush();

        // Verify
        verify(mockExporter, times(1)).flush();
        assertTrue(result.isSuccess());
    }

    @Test
    public void testShutdown() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.shutdown()).thenReturn(successCode);

        // Execute
        CompletableResultCode result = jaegerExporter.shutdown();

        // Verify
        verify(mockExporter, times(1)).shutdown();
        assertTrue(result.isSuccess());
    }

    @Test
    public void testMultipleExports() {
        // Setup
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, "", "info");
        List<SpanData> spans1 = createMockSpans(2);
        List<SpanData> spans2 = createMockSpans(3);
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute
        jaegerExporter.export(spans1);
        jaegerExporter.export(spans2);

        // Verify
        verify(mockExporter, times(2)).export(any());
    }

    @Test
    public void testExportWithConsoleLoggingEnabledLogsInfoOnSuccess() {
        // Setup - console logging at "info" makes both the info-level export
        // log lines and export() itself loggable.
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, true, "", "info");
        List<SpanData> spans = createMockSpans(2);
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();

        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute, capturing what actually gets logged to jaeger.tracelog -
        // asserting only delegation/result status would still pass if
        // printInfo/printSevere were deleted from export() entirely.
        List<LogRecord> records = new ArrayList<>();
        CompletableResultCode result = withTraceLogCapture(records, () -> jaegerExporter.export(spans));

        // Verify
        verify(mockExporter, times(1)).export(spans);
        assertTrue(result.isSuccess());
        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.INFO
                && r.getMessage().equals("Attempting to export 2 spans to " + TEST_ENDPOINT)));
        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.INFO
                && r.getMessage().startsWith("Span Payload: ")));
        assertTrue(records.stream().noneMatch(r -> r.getLevel() == Level.SEVERE));
    }

    @Test
    public void testExportWithConsoleLoggingEnabledLogsSevereOnFailure() {
        // Setup - "info" level also makes the higher-severity failure log
        // line loggable, exercising the isLoggable(SEVERE)/printSevere path.
        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, true, "", "info");
        List<SpanData> spans = createMockSpans(1);
        CompletableResultCode failureCode = CompletableResultCode.ofFailure();

        when(mockExporter.export(any())).thenReturn(failureCode);

        // Execute, capturing the actual log records emitted.
        List<LogRecord> records = new ArrayList<>();
        CompletableResultCode result = withTraceLogCapture(records, () -> jaegerExporter.export(spans));

        // Verify
        verify(mockExporter, times(1)).export(spans);
        assertFalse(result.isSuccess());
        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.INFO
                && r.getMessage().equals("Attempting to export 1 spans to " + TEST_ENDPOINT)));
        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.SEVERE
                && r.getMessage().equals("Failed to export spans to " + TEST_ENDPOINT)));
    }

    /**
     * Runs {@code operation}, capturing every {@link LogRecord} it emits via
     * {@link JaegerTraceLogger#JAEGER_TRACE_LOG} into {@code sink}. The
     * capturing handler is registered after the exporter under test is
     * constructed (JaegerTraceLogger's own constructor clears existing
     * handlers) and removed afterwards so it doesn't leak into other tests.
     */
    private CompletableResultCode withTraceLogCapture(List<LogRecord> sink,
            Supplier<CompletableResultCode> operation) {
        Logger traceLogger = Logger.getLogger(JaegerTraceLogger.JAEGER_TRACE_LOG);
        Handler captureHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                sink.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        captureHandler.setLevel(Level.ALL);
        traceLogger.addHandler(captureHandler);
        try {
            return operation.get();
        } finally {
            traceLogger.removeHandler(captureHandler);
        }
    }

    @Test
    public void testConstructorWithFileLogging() throws IOException {
        // Setup - a non-empty traceLogFile exercises the file-handler branch
        // of the constructor, independent of the console flag.
        Path logFile = Files.createTempFile("jaeger-exporter-test", ".log");
        logFile.toFile().deleteOnExit();

        jaegerExporter = new JaegerExporter(mockExporter, TEST_ENDPOINT, false, logFile.toString(), "debug");
        List<SpanData> spans = createMockSpans(1);
        CompletableResultCode successCode = CompletableResultCode.ofSuccess();
        when(mockExporter.export(any())).thenReturn(successCode);

        // Execute - should not throw regardless of what gets written to the file.
        jaegerExporter.export(spans);
    }

    @Test
    public void testConstructorAcceptsAllTraceLogLevels() {
        // Smoke test every getTraceLogLevel branch, including the
        // unrecognized-value default.
        for (String level : new String[] {"error", "warn", "info", "debug", "unrecognized"}) {
            new JaegerExporter(mockExporter, TEST_ENDPOINT, true, "", level);
        }
    }

    private List<SpanData> createMockSpans(int count) {
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            spans.add(mock(SpanData.class));
        }
        return spans;
    }
}

