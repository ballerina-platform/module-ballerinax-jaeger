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
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.internal.samplers.RateLimitingSampler;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import org.ballerinalang.config.ConfigRegistry;

import java.io.PrintStream;
import java.util.Objects;

import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_FLUSH_INTERVAL;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_HOSTNAME;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_MAX_BUFFER_SPANS;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_REPORTER_PORT;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_SAMPLER_PARAM;
import static io.ballerina.observe.trace.jaeger.Constants.DEFAULT_SAMPLER_TYPE;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_FLUSH_INTERVAL_MS_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_HOST_NAME_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_MAX_BUFFER_SPANS_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.REPORTER_PORT_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.SAMPLER_PARAM_CONFIG;
import static io.ballerina.observe.trace.jaeger.Constants.SAMPLER_TYPE_CONFIG;

/**
 * This is the Jaeger tracing extension class for {@link TracerProvider}.
 */
public class JaegerTracerProvider implements TracerProvider {

    private static ConfigRegistry configRegistry;
    private static String hostname;
    private static int port;
    private static String samplerType;
    private static Number samplerParam;
    private static int reporterFlushInterval;
    private static int reporterBufferSize;

    private static final PrintStream console = System.out;
    private static final PrintStream consoleError = System.err;

    /**
     * Initialize Jaeger configurations.
     * This is called by the TracerProvider ballerina object.
     *
     * @return Error if initializing configurations fails
     */
    public static BError initializeConfigurations() {
        configRegistry = ConfigRegistry.getInstance();
        try {
            port = Integer.parseInt(
                    configRegistry.getConfigOrDefault(REPORTER_PORT_CONFIG, String.valueOf(DEFAULT_REPORTER_PORT)));
            hostname = configRegistry.getConfigOrDefault(REPORTER_HOST_NAME_CONFIG, DEFAULT_REPORTER_HOSTNAME);

            samplerType = configRegistry.getConfigOrDefault(SAMPLER_TYPE_CONFIG, DEFAULT_SAMPLER_TYPE);
            if (!(samplerType.equals(ConstSampler.TYPE) || samplerType.equals(RateLimitingSampler.TYPE)
                    || samplerType.equals(ProbabilisticSampler.TYPE))) {
                consoleError.println("error: invalid Jaeger configuration sampler type: " + samplerType
                        + " invalid. using default const sampling");
                samplerType = DEFAULT_SAMPLER_TYPE;
            }

            samplerParam = Float.valueOf(
                    configRegistry.getConfigOrDefault(SAMPLER_PARAM_CONFIG, String.valueOf(DEFAULT_SAMPLER_PARAM)));
            reporterFlushInterval = Integer.parseInt(configRegistry.getConfigOrDefault(
                    REPORTER_FLUSH_INTERVAL_MS_CONFIG, String.valueOf(DEFAULT_REPORTER_FLUSH_INTERVAL)));
            reporterBufferSize = Integer.parseInt(configRegistry.getConfigOrDefault
                    (REPORTER_MAX_BUFFER_SPANS_CONFIG, String.valueOf(DEFAULT_REPORTER_MAX_BUFFER_SPANS)));
        } catch (IllegalArgumentException | ArithmeticException e) {
            return ErrorCreator.createError(StringUtils.fromString("reading Jaeger configurations failed: "
                    + e.getMessage()));
        }
        console.println("ballerina: started publishing traces to Jaeger on " + hostname + ":" + port);
        return null;
    }

    @Override
    public Tracer getTracer(String serviceName) {
        if (Objects.isNull(configRegistry)) {
            throw new IllegalStateException("invalid Jaeger configurations");
        }

        return new Configuration(serviceName)
                .withSampler(new Configuration.SamplerConfiguration()
                        .withType(samplerType)
                        .withParam(samplerParam))
                .withReporter(new Configuration.ReporterConfiguration()
                        .withLogSpans(Boolean.FALSE)
                        .withSender(new Configuration.SenderConfiguration()
                                .withAgentHost(hostname)
                                .withAgentPort(port))
                        .withFlushInterval(reporterFlushInterval)
                        .withMaxQueueSize(reporterBufferSize))
                .getTracerBuilder()
                .withScopeManager(NoopTracerFactory.create().scopeManager())
                .build();
    }
}
