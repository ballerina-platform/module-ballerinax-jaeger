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
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.internal.samplers.RateLimitingSampler;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import org.ballerinalang.config.ConfigRegistry;

import java.io.PrintStream;

import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_AGENT_HOSTNAME;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_AGENT_PORT;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_FLUSH_INTERVAL;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_MAX_BUFFER_SPANS;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_SAMPLER_PARAM;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_SAMPLER_TYPE;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_AGENT_HOSTNAME_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_AGENT_PORT_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_FLUSH_INTERVAL_MS_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_MAX_BUFFER_SPANS_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.SAMPLER_PARAM_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.SAMPLER_TYPE_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.TRACER_NAME;

/**
 * This is the Jaeger tracing extension class for {@link TracerProvider}.
 */
public class JaegerTracerProvider implements TracerProvider {
    private static final PrintStream console = System.out;
    private static final PrintStream consoleError = System.err;

    private Configuration.SamplerConfiguration samplerConfiguration;
    private Configuration.ReporterConfiguration reporterConfiguration;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public void init() {
        String reporterEndpoint;
        try {
            ConfigRegistry configRegistry = ConfigRegistry.getInstance();

            // Create Sampler Configuration
            String samplerType = configRegistry.getConfigOrDefault(SAMPLER_TYPE_CONFIG, DEFAULT_SAMPLER_TYPE);
            if (!(samplerType.equals(ConstSampler.TYPE) || samplerType.equals(RateLimitingSampler.TYPE)
                    || samplerType.equals(ProbabilisticSampler.TYPE))) {
                consoleError.println("error: invalid Jaeger configuration sampler type: " + samplerType
                        + " invalid. using default const sampling");
                samplerType = DEFAULT_SAMPLER_TYPE;
            }
            Number samplerParam = Float.valueOf(
                    configRegistry.getConfigOrDefault(SAMPLER_PARAM_CONFIG, String.valueOf(DEFAULT_SAMPLER_PARAM)));
            samplerConfiguration = new Configuration.SamplerConfiguration()
                    .withType(samplerType)
                    .withParam(samplerParam);

            // Create Sender Configuration
            String agentHostname = configRegistry.getConfigOrDefault(REPORTER_AGENT_HOSTNAME_CONFIG,
                    DEFAULT_REPORTER_AGENT_HOSTNAME);
            int agentPort = Integer.parseInt(configRegistry.getConfigOrDefault(REPORTER_AGENT_PORT_CONFIG,
                    String.valueOf(DEFAULT_REPORTER_AGENT_PORT)));
            Configuration.SenderConfiguration senderConfiguration = new Configuration.SenderConfiguration()
                    .withAgentHost(agentHostname)
                    .withAgentPort(agentPort);
            reporterEndpoint = agentHostname + ":" + agentPort;

            // Create Reporter Configuration
            int reporterFlushInterval = Integer.parseInt(configRegistry.getConfigOrDefault(
                    REPORTER_FLUSH_INTERVAL_MS_CONFIG, String.valueOf(DEFAULT_REPORTER_FLUSH_INTERVAL)));
            int reporterBufferSize = Integer.parseInt(configRegistry.getConfigOrDefault(
                    REPORTER_MAX_BUFFER_SPANS_CONFIG, String.valueOf(DEFAULT_REPORTER_MAX_BUFFER_SPANS)));
            reporterConfiguration = new Configuration.ReporterConfiguration()
                    .withSender(senderConfiguration)
                    .withFlushInterval(reporterFlushInterval)
                    .withMaxQueueSize(reporterBufferSize);
        } catch (Throwable t) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid jaeger configurations"), t);
        }
        console.println("ballerina: started publishing traces to Jaeger on " + reporterEndpoint);
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
