/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.observe.trace.jaeger.sampler.RateLimitingSampler;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

/**
 * This is the Jaeger tracing extension class for {@link TracerProvider}.
 */
public class JaegerTracerProvider implements TracerProvider {
    private static final String TRACER_NAME = "jaeger";
    private static final PrintStream console = System.out;

    static SdkTracerProviderBuilder tracerProviderBuilder;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public void init() {    // Do Nothing
    }

    public static void initializeConfigurations(BString agentHostname, int agentPort, BString samplerType,
                                                BDecimal samplerParam, int reporterFlushInterval,
                                                int reporterBufferSize) {

        String reporterEndpoint = agentHostname + ":" + agentPort;

        ManagedChannel jaegerChannel = new NettyChannelProvider()
                .builderForTarget(reporterEndpoint)
                .usePlaintext()
                .build();

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setChannel(jaegerChannel)
                .build();

        tracerProviderBuilder = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor
                        .builder(exporter)
                        .setMaxExportBatchSize(reporterBufferSize)
                        .setExporterTimeout(reporterFlushInterval, TimeUnit.MILLISECONDS)
                        .build());

        tracerProviderBuilder.setSampler(selectSampler(samplerType, samplerParam));

        console.println("ballerina: started publishing traces to Jaeger on " + reporterEndpoint);
    }

    private static Sampler selectSampler(BString samplerType, BDecimal samplerParam) {
        switch (samplerType.getValue()) {
            default:
            case "const":
                if (samplerParam.value().intValue() == 0) {
                    return Sampler.alwaysOff();
                } else {
                    return Sampler.alwaysOn();
                }
            case "probabilistic":
                return Sampler.traceIdRatioBased(samplerParam.value().doubleValue());
            case RateLimitingSampler.TYPE:
                return new RateLimitingSampler(samplerParam.value().intValue());
        }
    }

    @Override
    public Tracer getTracer(String serviceName) {

        return tracerProviderBuilder.setResource(
                Resource.create(Attributes.of(SERVICE_NAME, serviceName)))
                .build().get("jaeger");
    }

    @Override
    public ContextPropagators getPropagators() {

        return ContextPropagators.create(W3CTraceContextPropagator.getInstance());
    }
}
