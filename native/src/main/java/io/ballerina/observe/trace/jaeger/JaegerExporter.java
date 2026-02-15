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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;

public final class JaegerExporter implements SpanExporter {
    private final String endpoint;
    private final SpanExporter exporter;
    private final JaegerTraceLogger tracerLogger;

    public JaegerExporter(SpanExporter exporter, String endpoint,
                          boolean traceLogConsole, String traceLogFile, String traceLogLevel) {
        this.exporter = exporter;
        this.endpoint = endpoint;
        if (traceLogConsole || !Objects.equals(traceLogFile, "")) {
            Path logFilePath = Objects.equals(traceLogFile, "") ? null : Path.of(traceLogFile);
            this.tracerLogger = new JaegerTraceLogger(traceLogConsole, logFilePath);
            this.tracerLogger.setLogLevel(getTraceLogLevel(traceLogLevel));
        } else {
            this.tracerLogger = new JaegerTraceLogger();
            tracerLogger.setLogLevel(Level.OFF);
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        tracerLogger.printInfo("Attempting to export " + spans.size() + " spans to " + endpoint);
        tracerLogger.printInfo("Span Payload: " + spans);

        CompletableResultCode result = exporter.export(spans);
        result.whenComplete(() -> {
            if (!result.isSuccess()) {
                tracerLogger.printSevere("Failed to export spans to " + endpoint);
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

    private Level getTraceLogLevel(String traceLogLevel) {
        switch (traceLogLevel) {
            case "error":
                return Level.SEVERE;
            case "warn":
                return Level.WARNING;
            case "info":
                return Level.INFO;
            case "debug":
                return Level.CONFIG;
            default:
                return Level.OFF;
        }
    }
}
