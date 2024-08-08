/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.internal.values.DecimalValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Objects;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

public class TracerProviderTest {
    private JaegerTracerProvider tracerProvider = new JaegerTracerProvider();
    private static final String BALLERINA_SERVICE_NAME = "hello-world";
    private static final BString AGENT_HOST_NAME = StringUtils.fromString("localhost");
    private static final int AGENT_PORT = 4317;
    private static final BString SAMPLER_TYPE = StringUtils.fromString("const");
    private static final BDecimal SAMPLER_PARAM = DecimalValue.valueOf(1);
    private static final int REPORTER_FLUSH_INTERVAL = 1000;
    private static final int REPORTER_BUFFER_SIZE = 10000;

    public TracerProviderTest() {
        JaegerTracerProvider.initializeConfigurations(AGENT_HOST_NAME, AGENT_PORT, SAMPLER_TYPE, SAMPLER_PARAM,
                REPORTER_FLUSH_INTERVAL, REPORTER_BUFFER_SIZE);
    }

    @Test
    public void testTracerProviderName() {
        Assert.assertEquals(tracerProvider.getName(), "jaeger");
    }

    @Test
    public void testTracerProviderSampler() {
        SdkTracerProvider provider = tracerProvider.getTracerProviderBuilder().setResource(
                        Resource.create(Attributes.of(SERVICE_NAME, BALLERINA_SERVICE_NAME)))
                .build();
        Assert.assertEquals(provider.getSampler().getDescription(), "AlwaysOnSampler");
    }

    @Test
    public void testTracerProviderSpanBuilder() {
        SdkTracerProvider provider = tracerProvider.getTracerProviderBuilder().setResource(
                        Resource.create(Attributes.of(SERVICE_NAME, BALLERINA_SERVICE_NAME)))
                .build();
        Assert.assertTrue(startAndFinishSpan(provider.get("jaeger")));
    }

    private boolean startAndFinishSpan(Tracer tracer) {
        Span span = tracer.spanBuilder("ParentSpan").startSpan();
        try (Scope parentScope = span.makeCurrent()) {
            Span.current().addEvent("Starting the work.");
            Thread.sleep(1000);
            Span childSpan = tracer.spanBuilder("ChildSpan").setParent(Objects.requireNonNull(ContextStorage.get()
                    .current())).startSpan();
            try (Scope childScope = span.makeCurrent()) {
                childSpan.addEvent("Starting the work.");
                Thread.sleep(1000);
                childSpan.addEvent("Finished working.");
            } catch (InterruptedException e) {
                return false;
            } finally {
                childSpan.end();
            }
            Span.current().addEvent("Finished working.");
        } catch (InterruptedException e) {
            return false;
        } finally {
            span.end();
        }
        return true;
    }
}
