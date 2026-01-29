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

import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public final class JaegerExporter implements SpanExporter {
    private static final PrintStream console = System.out;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final String MODULE_NAME = "ballerinax/jaeger";
    private final String endpoint;
    private final SpanExporter exporter;
    private final boolean isPayloadLoggingEnabled;
    private final boolean isTraceLoggingEnabled;

    public JaegerExporter(SpanExporter exporter, String endpoint,
                          boolean isTraceLoggingEnabled, boolean isPayloadLoggingEnabled) {
        this.exporter = exporter;
        this.endpoint = endpoint;
        this.isPayloadLoggingEnabled = isPayloadLoggingEnabled;
        this.isTraceLoggingEnabled = isTraceLoggingEnabled || isPayloadLoggingEnabled;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isPayloadLoggingEnabled) {
            printDebugLog("attempting to export " + spans.size() + " spans to " + endpoint);
            printDebugLog("span Payload: " + spans);
        }
        CompletableResultCode result = exporter.export(spans);
        result.whenComplete(() -> {
            if (!result.isSuccess()) {
                if (isTraceLoggingEnabled) {
                    printDebugLog("failed to export spans to " + endpoint);
                }
            }
        });
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return exporter.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return exporter.shutdown();
    }

    private void printDebugLog(String message) {
        ZonedDateTime now = ZonedDateTime.now();
        String timestamp = now.format(formatter);
        String logMessage = String.format("time=%s level=DEBUG module=%s message=\"%s\"",
                timestamp, MODULE_NAME, message);
        console.println(logMessage);
    }
}
