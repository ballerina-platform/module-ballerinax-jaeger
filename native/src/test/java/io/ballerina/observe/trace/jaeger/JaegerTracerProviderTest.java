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

import io.ballerina.observe.trace.jaeger.sampler.RateLimitingSampler;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test class for JaegerTracerProvider.
 */
public class JaegerTracerProviderTest {

    private JaegerTracerProvider tracerProvider;

    @BeforeMethod
    public void setUp() {
        tracerProvider = new JaegerTracerProvider();
    }

    @Test
    public void testGetName() {
        assertEquals(tracerProvider.getName(), "jaeger");
    }

    @Test
    public void testInit() {
        // init() does nothing, just ensure it doesn't throw an exception
        tracerProvider.init();
    }


    @Test
    public void testGetPropagators() {
        ContextPropagators propagators = tracerProvider.getPropagators();
        assertNotNull(propagators);
    }

    @Test
    public void testInitializeConfigurationsAndGetTracer() {
        // alwaysOff sampler (const/0) so no span export is ever attempted -
        // safe to run without a live Jaeger endpoint.
        JaegerTracerProvider.initializeConfigurations(
                StringUtils.fromString("localhost"), 55680,
                StringUtils.fromString("const"), BDecimal.valueOf(0),
                1000, 10000, false, StringUtils.fromString(""), StringUtils.fromString("info"));

        Tracer tracer = tracerProvider.getTracer("jaeger-tracer-provider-test");
        assertNotNull(tracer);

        Span span = tracer.spanBuilder("test-span").startSpan();
        span.end();
    }

    @Test
    public void testSelectSamplerConstOn() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("const", BDecimal.valueOf(1));
        assertEquals(sampler.getDescription(), Sampler.alwaysOn().getDescription());
    }

    @Test
    public void testSelectSamplerConstOff() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("const", BDecimal.valueOf(0));
        assertEquals(sampler.getDescription(), Sampler.alwaysOff().getDescription());
    }

    @Test
    public void testSelectSamplerProbabilistic() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("probabilistic", BDecimal.valueOf(0.5));
        assertEquals(sampler.getDescription(), Sampler.traceIdRatioBased(0.5).getDescription());
    }

    @Test
    public void testSelectSamplerRateLimiting() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler(RateLimitingSampler.TYPE, BDecimal.valueOf(5));
        assertTrue(sampler instanceof RateLimitingSampler);
    }

    @Test
    public void testSelectSamplerUnknownTypeDefaultsToConst() throws ReflectiveOperationException {
        Sampler sampler = invokeSelectSampler("unrecognized-type", BDecimal.valueOf(1));
        assertEquals(sampler.getDescription(), Sampler.alwaysOn().getDescription());
    }

    private Sampler invokeSelectSampler(String samplerType, BDecimal samplerParam) throws ReflectiveOperationException {
        Method method = JaegerTracerProvider.class.getDeclaredMethod("selectSampler",
                io.ballerina.runtime.api.values.BString.class, BDecimal.class);
        method.setAccessible(true);
        return (Sampler) method.invoke(null, StringUtils.fromString(samplerType), samplerParam);
    }
}
