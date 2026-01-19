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
package io.ballerina.observe.trace.jaeger.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test class for RateLimitingSampler.
 */
public class RateLimitingSamplerTest {

    @Test
    public void testSamplerWithPositiveRate() {
        RateLimitingSampler sampler = new RateLimitingSampler(10);
        assertNotNull(sampler);
        assertEquals(sampler.getDescription(), "RateLimitingSampler{10.00}");
    }

    @Test
    public void testSamplerWithZeroRate() {
        // Even with 0, it should create with max balance of 1.0
        RateLimitingSampler sampler = new RateLimitingSampler(0);
        assertNotNull(sampler);
        assertEquals(sampler.getDescription(), "RateLimitingSampler{0.00}");
    }

    @Test
    public void testSamplerWithHighRate() {
        RateLimitingSampler sampler = new RateLimitingSampler(1000);
        assertNotNull(sampler);
        assertEquals(sampler.getDescription(), "RateLimitingSampler{1000.00}");
    }

    @Test
    public void testShouldSampleAllowsInitialRequests() {
        RateLimitingSampler sampler = new RateLimitingSampler(2);

        Context parentContext = Context.root();
        String traceId = "00000000000000000000000000000001";
        String name = "test-span";
        SpanKind spanKind = SpanKind.INTERNAL;
        Attributes attributes = Attributes.empty();
        List<LinkData> parentLinks = Collections.emptyList();

        // First request should be sampled
        SamplingResult result1 = sampler.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks
        );
        assertEquals(result1.getDecision(), SamplingDecision.RECORD_AND_SAMPLE);

        // Second request should be sampled
        SamplingResult result2 = sampler.shouldSample(
            parentContext, traceId + "2", name, spanKind, attributes, parentLinks
        );
        assertEquals(result2.getDecision(), SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    public void testShouldSampleDropsExcessRequests() throws InterruptedException {
        RateLimitingSampler sampler = new RateLimitingSampler(2);

        Context parentContext = Context.root();
        String name = "test-span";
        SpanKind spanKind = SpanKind.INTERNAL;
        Attributes attributes = Attributes.empty();
        List<LinkData> parentLinks = Collections.emptyList();

        int sampledCount = 0;
        int droppedCount = 0;

        // Try to sample 10 requests quickly
        for (int i = 0; i < 10; i++) {
            SamplingResult result = sampler.shouldSample(
                parentContext,
                String.format("0000000000000000000000000000000%d", i),
                name,
                spanKind,
                attributes,
                parentLinks
            );

            if (result.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
                sampledCount++;
            } else {
                droppedCount++;
            }
        }

        // Should have dropped some requests
        assertTrue(droppedCount > 0, "Expected some requests to be dropped");
        assertTrue(sampledCount > 0, "Expected some requests to be sampled");
    }

    @Test
    public void testShouldSampleRecoversOverTime() throws InterruptedException {
        RateLimitingSampler sampler = new RateLimitingSampler(10);

        Context parentContext = Context.root();
        String name = "test-span";
        SpanKind spanKind = SpanKind.INTERNAL;
        Attributes attributes = Attributes.empty();
        List<LinkData> parentLinks = Collections.emptyList();

        // Exhaust the initial budget
        for (int i = 0; i < 20; i++) {
            sampler.shouldSample(
                parentContext,
                String.format("trace-%d", i),
                name,
                spanKind,
                attributes,
                parentLinks
            );
        }

        // Wait for the rate limiter to recover
        Thread.sleep(200);

        // Should be able to sample again
        SamplingResult result = sampler.shouldSample(
            parentContext,
            "trace-after-wait",
            name,
            spanKind,
            attributes,
            parentLinks
        );

        assertEquals(result.getDecision(), SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    public void testSamplingResultContainsAttributes() {
        RateLimitingSampler sampler = new RateLimitingSampler(5);

        SamplingResult result = sampler.shouldSample(
            Context.root(),
            "00000000000000000000000000000001",
            "test-span",
            SpanKind.INTERNAL,
            Attributes.empty(),
            Collections.emptyList()
        );

        Attributes attrs = result.getAttributes();
        assertNotNull(attrs);
        assertEquals(attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("sampler.type")),
                     "ratelimiting");
        assertEquals(attrs.get(io.opentelemetry.api.common.AttributeKey.doubleKey("sampler.param")),
                     5.0);
    }

    @Test
    public void testToString() {
        RateLimitingSampler sampler = new RateLimitingSampler(15);
        String description = sampler.toString();
        assertEquals(description, "RateLimitingSampler{15.00}");
    }

    @Test
    public void testGetDescription() {
        RateLimitingSampler sampler = new RateLimitingSampler(20);
        String description = sampler.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("RateLimitingSampler"));
        assertTrue(description.contains("20.00"));
    }

    @Test
    public void testDecimalFormatting() {
        RateLimitingSampler sampler1 = new RateLimitingSampler(1);
        assertEquals(sampler1.getDescription(), "RateLimitingSampler{1.00}");

        RateLimitingSampler sampler2 = new RateLimitingSampler(100);
        assertEquals(sampler2.getDescription(), "RateLimitingSampler{100.00}");
    }

    @Test
    public void testSamplingWithDifferentSpanKinds() {
        RateLimitingSampler sampler = new RateLimitingSampler(5);

        Context parentContext = Context.root();
        String traceId = "00000000000000000000000000000001";
        String name = "test-span";
        Attributes attributes = Attributes.empty();
        List<LinkData> parentLinks = Collections.emptyList();

        // Test with different span kinds
        SpanKind[] spanKinds = {
            SpanKind.INTERNAL,
            SpanKind.SERVER,
            SpanKind.CLIENT,
            SpanKind.PRODUCER,
            SpanKind.CONSUMER
        };

        for (SpanKind spanKind : spanKinds) {
            SamplingResult result = sampler.shouldSample(
                parentContext, traceId, name, spanKind, attributes, parentLinks
            );
            assertNotNull(result);
        }
    }

    @Test
    public void testType() {
        assertEquals(RateLimitingSampler.TYPE, "ratelimiting");
    }
}

