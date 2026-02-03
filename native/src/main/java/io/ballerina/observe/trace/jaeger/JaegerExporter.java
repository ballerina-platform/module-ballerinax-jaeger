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

import io.ballerina.runtime.api.Environment;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;

public final class JaegerExporter implements SpanExporter {
    private final String endpoint;
    private final SpanExporter exporter;
    private final boolean isPayloadLoggingEnabled;
    private final boolean isErrorLoggingEnabled;
    private final JaegerLogger logger;

    public JaegerExporter(Environment environment, SpanExporter exporter, String endpoint,
                          boolean isErrorLoggingEnabled, boolean isPayloadLoggingEnabled) {
        this.exporter = exporter;
        this.endpoint = endpoint;
        this.isPayloadLoggingEnabled = isPayloadLoggingEnabled;
        this.isErrorLoggingEnabled = isErrorLoggingEnabled;
        this.logger = new JaegerLogger(environment);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isPayloadLoggingEnabled) {
            logger.printInfoLog("Attempting to export " + spans.size() + " spans to " + endpoint);
            logger.printInfoLog("Span Payload: " + spans);
        }
        CompletableResultCode result = exporter.export(spans);
        result.whenComplete(() -> {
            if (!result.isSuccess()) {
                if (isErrorLoggingEnabled) {
                    logger.printErrorLog("Failed to export spans to " + endpoint);
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
}
