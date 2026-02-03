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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
    public void testExportWithLoggingEnabled() {
        // Setup
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, true, true);
        List<SpanData> spans = createMockSpans(1);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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
        jaegerExporter = new JaegerExporter(null, mockExporter, TEST_ENDPOINT, false, false);
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

    private List<SpanData> createMockSpans(int count) {
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            spans.add(mock(SpanData.class));
        }
        return spans;
    }
}

