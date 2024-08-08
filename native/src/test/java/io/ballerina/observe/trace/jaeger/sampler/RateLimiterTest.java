package io.ballerina.observe.trace.jaeger.sampler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

public class RateLimiterTest {
    private RateLimiter rateLimiter;
    private RateLimitingSampler rateLimitingSampler;

    public RateLimiterTest() {
        this.rateLimiter = new RateLimiter(100, 1000, Clock.getDefault());
        this.rateLimitingSampler = new RateLimitingSampler(100);
    }

    @Test
    public void testCheckCredit() {
        Assert.assertTrue(rateLimiter.checkCredit(1000));
        Assert.assertFalse(rateLimiter.checkCredit(1001));
    }

    @Test
    public void testRateLimitingSample() {
        String serviceName = "hello-world";
        List<LinkData> parentLinks = new ArrayList<>();

        SamplingResult samplingResult = rateLimitingSampler.shouldSample(Context.current(), "11", serviceName,
                SpanKind.SERVER, Attributes.of(SERVICE_NAME, serviceName), parentLinks);

        Assert.assertEquals(samplingResult.getDecision().name(), "RECORD_AND_SAMPLE");
        Assert.assertEquals(samplingResult.getDecision().ordinal(), 2);
        Assert.assertEquals(samplingResult.getAttributes().asMap().get(AttributeKey.doubleKey("sampler.param")), 100.0);
        Assert.assertEquals(samplingResult.getAttributes().asMap().get(AttributeKey.stringKey("sampler.type")),
                "ratelimiting");
    }

    @Test
    public void testRateLimiterSampleDescription() {
        Assert.assertEquals(rateLimitingSampler.getDescription(), "RateLimitingSampler{100.00}");
    }
}
