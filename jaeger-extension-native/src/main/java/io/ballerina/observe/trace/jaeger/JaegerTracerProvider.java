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

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;

import java.io.PrintStream;

/**
 * This is the Jaeger tracing extension class for {@link TracerProvider}.
 */
public class JaegerTracerProvider implements TracerProvider {
    private static final String TRACER_NAME = "jaeger";
    private static final PrintStream console = System.out;

    private static Configuration.SamplerConfiguration samplerConfiguration;
    private static Configuration.ReporterConfiguration reporterConfiguration;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public void init() {    // Do Nothing
    }

    public static BError initializeConfigurations(BString agentHostname, int agentPort, BString samplerType,
                                                  BDecimal samplerParam, int reporterFlushInterval,
                                                  int reporterBufferSize) {
        String reporterEndpoint;
        try {
            // Create Sampler Configuration
            samplerConfiguration = new Configuration.SamplerConfiguration()
                    .withType(samplerType.getValue())
                    .withParam(samplerParam.value());

            // Create Sender Configuration
            Configuration.SenderConfiguration senderConfiguration = new Configuration.SenderConfiguration()
                    .withAgentHost(agentHostname.getValue())
                    .withAgentPort(agentPort);
            reporterEndpoint = agentHostname + ":" + agentPort;

            // Create Reporter Configuration
            reporterConfiguration = new Configuration.ReporterConfiguration()
                    .withSender(senderConfiguration)
                    .withFlushInterval(reporterFlushInterval)
                    .withMaxQueueSize(reporterBufferSize);
        } catch (Throwable t) {
            return ErrorCreator.createError(StringUtils.fromString("invalid jaeger configurations"), t);
        }
        console.println("ballerina: started publishing traces to Jaeger on " + reporterEndpoint);
        return null;
    }

    @Override
    public Tracer getTracer(String serviceName) {
        return new Configuration(serviceName)
                .withSampler(samplerConfiguration)
                .withReporter(reporterConfiguration)
                .getTracerBuilder()
                .withScopeManager(NoopTracerFactory.create().scopeManager())
                .build();
    }
}
