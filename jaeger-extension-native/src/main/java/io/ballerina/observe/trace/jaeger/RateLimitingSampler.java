/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.time.Instant;
import java.util.List;

public class RateLimitingSampler implements Sampler {

    private final int traceCountPerSec;
    private volatile int traceCountInCurrentSec;
    private long currentSec;

    public RateLimitingSampler(int traceCountPerSec) {
        this.traceCountPerSec = traceCountPerSec;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
                                       Attributes attributes, List<LinkData> parentLinks) {

        synchronized (this) {
            long now = Instant.now().getEpochSecond();
            if (now != currentSec) {
                currentSec = now;
                traceCountInCurrentSec = 0;
            }
            traceCountInCurrentSec++;
        }

        if (traceCountInCurrentSec > traceCountPerSec) {
            return SamplingResult.create(SamplingDecision.DROP);
        } else {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
    }

    @Override
    public String getDescription() {
        return "Rate limiting sampler";
    }
}
