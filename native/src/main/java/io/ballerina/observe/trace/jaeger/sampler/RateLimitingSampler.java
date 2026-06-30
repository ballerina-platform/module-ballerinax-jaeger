/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.ballerina.observe.trace.jaeger.sampler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * This class is copied from https://github.com/open-telemetry/opentelemetry-java/blob/v1.32.0/sdk-extensions/
 * jaeger-remote-sampler/src/main/java/io/opentelemetry/sdk/extension/trace/jaeger/sampler/RateLimitingSampler.java.
 * This sampler uses a leaky bucket rate limiter to ensure that traces are sampled with a certain constant rate.
 */
public class RateLimitingSampler implements Sampler {
    public static final String TYPE = "ratelimiting";
    private static final AttributeKey<String> SAMPLER_TYPE = stringKey("sampler.type");
    private static final AttributeKey<Double> SAMPLER_PARAM = doubleKey("sampler.param");

    private final double maxUsagePerSecond;
    private final double maxBalance;
    private final Clock clock;
    private double balance;
    private long lastUpdateNanos;
    private final SamplingResult onSamplingResult;
    private final SamplingResult offSamplingResult;
    private final String description;

    /**
     * Creates rate limiting sampler.
     *
     * @param maxTracesPerSecond the maximum number of sampled traces per second.
     */
    public RateLimitingSampler(int maxTracesPerSecond) {
        this.maxUsagePerSecond = maxTracesPerSecond;
        this.maxBalance = maxTracesPerSecond < 1.0 ? 1.0 : maxTracesPerSecond;
        this.clock = Clock.getDefault();
        this.balance = this.maxBalance;
        this.lastUpdateNanos = clock.now();
        Attributes attributes =
                Attributes.of(SAMPLER_TYPE, TYPE, SAMPLER_PARAM, (double) maxTracesPerSecond);
        this.onSamplingResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, attributes);
        this.offSamplingResult = SamplingResult.create(SamplingDecision.DROP, attributes);
        description = "RateLimitingSampler{" + decimalFormat(maxTracesPerSecond) + "}";
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        return trySpend(1.0) ? onSamplingResult : offSamplingResult;
    }

    private synchronized boolean trySpend(double itemCost) {
        long currentTime = clock.now();
        balance += (currentTime - lastUpdateNanos) * 1e-9 * maxUsagePerSecond;
        lastUpdateNanos = currentTime;
        if (balance > maxBalance) {
            balance = maxBalance;
        }
        if (balance >= itemCost) {
            balance -= itemCost;
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    private static String decimalFormat(double value) {
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator('.');

        DecimalFormat decimalFormat = new DecimalFormat("0.00", decimalFormatSymbols);
        return decimalFormat.format(value);
    }
}
