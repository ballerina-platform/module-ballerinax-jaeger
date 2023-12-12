/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import io.ballerina.observe.trace.jaeger.backend.JaegerServerProtocol;
import io.ballerina.observe.trace.jaeger.model.JaegerProcess;
import io.ballerina.observe.trace.jaeger.model.JaegerQueryResponse;
import io.ballerina.observe.trace.jaeger.model.JaegerQueryResponseTypeToken;
import io.ballerina.observe.trace.jaeger.model.JaegerSpan;
import io.ballerina.observe.trace.jaeger.model.JaegerTag;
import io.ballerina.observe.trace.jaeger.model.JaegerTrace;
import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.context.LogLeecher;
import org.ballerinalang.test.context.Utils;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.runtime.observability.ObservabilityConstants.DEFAULT_SERVICE_NAME;

/**
 * Integration test for Jaeger extension.
 */
public class JaegerTracesTestCase extends BaseTestCase {
    private BServerInstance serverInstance;

    private static final File RESOURCES_DIR = Paths.get("src", "test", "resources", "bal").toFile();
    private static final String TEST_RESOURCE_URL = "http://localhost:9091/test/sum";

    private static final String JAEGER_EXTENSION_LOG_PREFIX = "ballerina: started publishing traces to Jaeger on ";
    private static final String SAMPLE_SERVER_NAME = "/test";
    private static final String JAEGER_PROCESS_ID = "p1";

    @BeforeMethod
    public void setup() throws Exception {
        serverInstance = new BServerInstance(balServer);
    }

    @AfterMethod
    public void cleanUpServer() throws Exception {
        serverInstance.shutdownServer();
        jaegerServer.stopServer();
    }

    @DataProvider(name = "test-jaeger-metrics-data")
    public Object[][] getTestJaegerMetricsData() {
        return new Object[][]{
                {"localhost", 55680, JaegerServerProtocol.OTL_GRPC, "ConfigDefault.toml"},
                {"127.0.0.1", 16831, JaegerServerProtocol.OTL_GRPC, "ConfigAgent.toml"},
                {"localhost", 55680, JaegerServerProtocol.OTL_GRPC, "ConfigInvalidSampler.toml"},
                {"localhost", 55680, JaegerServerProtocol.OTL_GRPC, "ConfigSamplerConst.toml"},
                {"localhost", 55680, JaegerServerProtocol.OTL_GRPC, "ConfigSamplerProbabilistic.toml"},
                {"localhost", 55680, JaegerServerProtocol.OTL_GRPC, "ConfigSamplerRatelimiting.toml"}
        };
    }

    @Test(dataProvider = "test-jaeger-metrics-data")
    public void testJaegerMetrics(String host, int jaegerReportAddress, JaegerServerProtocol jaegerReportProtocol,
                                  String configFilename)
            throws Exception {
        jaegerServer.startServer(host, jaegerReportAddress, jaegerReportProtocol);

        LogLeecher jaegerExtLogLeecher = new LogLeecher(JAEGER_EXTENSION_LOG_PREFIX + host + ":"
                + jaegerReportAddress);
        serverInstance.addLogLeecher(jaegerExtLogLeecher);
        LogLeecher errorLogLeecher = new LogLeecher("error");
        serverInstance.addErrorLogLeecher(errorLogLeecher);
        LogLeecher exceptionLogLeecher = new LogLeecher("Exception");
        serverInstance.addErrorLogLeecher(exceptionLogLeecher);

        String configFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), configFilename).toFile().getAbsolutePath();
        Map<String, String> env = new HashMap<>();
        env.put("BAL_CONFIG_FILES", configFile);

        final String balFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "01_http_svc_test.bal").toFile()
                .getAbsolutePath();
        int[] requiredPorts = {9091};
        serverInstance.startServer(balFile, new String[]{"--observability-included"}, null, env, requiredPorts);
        Utils.waitForPortsToOpen(requiredPorts, 1000 * 60, false, InetAddress.getByName("localhost"));
        jaegerExtLogLeecher.waitForText(10000);

        // Send requests to generate metrics
        long startTimeMicroseconds = Calendar.getInstance().getTimeInMillis() * 1000;
        String responseData = HttpClientRequest.doGet(TEST_RESOURCE_URL).getData();
        Assert.assertEquals(responseData, "Sum: 53");
        long endTimeMicroseconds = (Calendar.getInstance().getTimeInMillis() + 1) * 1000;
        Thread.sleep(12000);

        // Read services from Jaeger query endpoint
        HttpResponse servicesQueryHttpResponse = HttpClientRequest.doGet("http://localhost:16686/api/services");
        Assert.assertEquals(servicesQueryHttpResponse.getResponseCode(), 200);
        Type servicesQueryResponseType = new JaegerQueryResponseTypeToken.Services().getType();
        JaegerQueryResponse<List<String>> servicesQueryResponse = new Gson().fromJson(
                servicesQueryHttpResponse.getData(), servicesQueryResponseType);

        List<String> servicesQueryResponseData = servicesQueryResponse.getData();
        Assert.assertNotNull(servicesQueryResponseData);
        Assert.assertEquals(servicesQueryResponseData.size(), 2);
        Assert.assertEquals(new HashSet<>(servicesQueryResponseData),
                new HashSet<>(Arrays.asList(SAMPLE_SERVER_NAME, DEFAULT_SERVICE_NAME)));

        // Read traces from Jaeger query endpoint
        HttpResponse tracesQueryHttpResponse = HttpClientRequest.doGet("http://localhost:16686/api/traces?end="
                + endTimeMicroseconds + "&limit=20&service=" + SAMPLE_SERVER_NAME + "&start=" + startTimeMicroseconds);
        Assert.assertEquals(tracesQueryHttpResponse.getResponseCode(), 200);
        Type tracesQueryResponseType = new JaegerQueryResponseTypeToken.Traces().getType();
        JaegerQueryResponse<List<JaegerTrace>> tracesQueryResponse = new Gson().fromJson(
                tracesQueryHttpResponse.getData(), tracesQueryResponseType);

        List<JaegerTrace> tracesQueryResponseData = tracesQueryResponse.getData();
        Assert.assertNotNull(tracesQueryResponseData);
        Assert.assertEquals(tracesQueryResponseData.size(), 1);

        JaegerTrace jaegerTrace = tracesQueryResponseData.get(0);
        Assert.assertNotNull(jaegerTrace.getTraceID());
        Assert.assertEquals(jaegerTrace.getSpans().size(), 3);
        Assert.assertEquals(jaegerTrace.getProcesses().size(), 1);

        String span1Position = "01_http_svc_test.bal:22:5";
        JaegerSpan span1 = findSpan(jaegerTrace, span1Position);
        Assert.assertNotNull(span1, "Span from position " + span1Position + " not found");
        Assert.assertEquals(span1.getOperationName(), "get /sum");
        Assert.assertEquals(span1.getReferences().size(), 0);
        Assert.assertEquals(span1.getProcessID(), JAEGER_PROCESS_ID);
        Assert.assertTrue(span1.getStartTime() > startTimeMicroseconds && span1.getStartTime() < endTimeMicroseconds,
                "span with position ID \"" + span1Position + "\" not between start and end time");
        Assert.assertTrue(span1.getDuration() < endTimeMicroseconds - startTimeMicroseconds,
                "span with position ID \"" + span1Position + "\" duration not between start and end time");
        Assert.assertEquals(span1.getTags(), new HashSet<>(Arrays.asList(
                new JaegerTag("src.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("listener.name", "string", "http"),
                new JaegerTag("src.object.name", "string", SAMPLE_SERVER_NAME),
                new JaegerTag("entrypoint.function.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("http.url", "string", "/test/sum"),
                new JaegerTag("src.resource.accessor", "string", "get"),
                new JaegerTag("entrypoint.service.name", "string", SAMPLE_SERVER_NAME),
                new JaegerTag("entrypoint.function.name", "string", "/sum"),
                new JaegerTag("entrypoint.resource.accessor", "string", "get"),
                new JaegerTag("protocol", "string", "http"),
                new JaegerTag("src.service.resource", "string", "true"),
                new JaegerTag("span.kind", "string", "server"),
                new JaegerTag("src.position", "string", span1Position),
                new JaegerTag("src.resource.path", "string", "/sum"),
                new JaegerTag("http.method", "string", "GET"),
                new JaegerTag("http.status_code", "string", "200"),
                new JaegerTag("otlp.instrumentation.library.name", "string", "jaeger"),
                new JaegerTag("status.code", "int64", "0")
        )));

        String span2Position = "01_http_svc_test.bal:24:19";
        JaegerSpan span2 = findSpan(jaegerTrace, span2Position);
        Assert.assertNotNull(span2, "Span from position " + span2Position + " not found");
        Assert.assertEquals(span2.getOperationName(), "$anon/./ObservableAdder:getSum");
        Assert.assertEquals(span2.getReferences().size(), 1);
        Assert.assertEquals(span2.getReferences().get(0).getRefType(), "CHILD_OF");
        Assert.assertEquals(span2.getReferences().get(0).getTraceID(), span1.getTraceID());
        Assert.assertEquals(span2.getReferences().get(0).getSpanID(), span1.getSpanID());
        Assert.assertEquals(span2.getProcessID(), JAEGER_PROCESS_ID);
        Assert.assertTrue(span2.getStartTime() > startTimeMicroseconds && span2.getStartTime() < endTimeMicroseconds,
                "span with position ID \"" + span2Position + "\" not between start and end time");
        Assert.assertTrue(span2.getDuration() < endTimeMicroseconds - startTimeMicroseconds,
                "span with position ID \"" + span2Position + "\" duration not between start and end time");
        Assert.assertEquals(span2.getTags(), new HashSet<>(Arrays.asList(
                new JaegerTag("entrypoint.function.name", "string", "/sum"),
                new JaegerTag("src.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("span.kind", "string", "client"),
                new JaegerTag("src.object.name", "string", "$anon/./ObservableAdder"),
                new JaegerTag("entrypoint.function.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("entrypoint.service.name", "string", SAMPLE_SERVER_NAME),
                new JaegerTag("entrypoint.resource.accessor", "string", "get"),
                new JaegerTag("src.position", "string", span2Position),
                new JaegerTag("src.function.name", "string", "getSum"),
                new JaegerTag("otlp.instrumentation.library.name", "string", "jaeger"),
                new JaegerTag("status.code", "int64", "0")
        )));

        String span3Position = "01_http_svc_test.bal:28:20";
        JaegerSpan span3 = findSpan(jaegerTrace, span3Position);
        Assert.assertNotNull(span3, "Span from position " + span3Position + " not found");
        Assert.assertEquals(span3.getOperationName(), "ballerina/http/Caller:respond");
        Assert.assertEquals(span3.getReferences().size(), 1);
        Assert.assertEquals(span3.getReferences().get(0).getRefType(), "CHILD_OF");
        Assert.assertEquals(span3.getReferences().get(0).getTraceID(), span1.getTraceID());
        Assert.assertEquals(span3.getReferences().get(0).getSpanID(), span1.getSpanID());
        Assert.assertEquals(span3.getProcessID(), JAEGER_PROCESS_ID);
        Assert.assertTrue(span3.getStartTime() > startTimeMicroseconds && span3.getStartTime() < endTimeMicroseconds,
                "span with position ID \"" + span3Position + "\" not between start and end time");
        Assert.assertTrue(span3.getDuration() < endTimeMicroseconds - startTimeMicroseconds,
                "span with position ID \"" + span3Position + "\" duration not between start and end time");
        Assert.assertEquals(span3.getTags(), new HashSet<>(Arrays.asList(
                new JaegerTag("http.status_code", "string", "200"),
                new JaegerTag("entrypoint.function.name", "string", "/sum"),
                new JaegerTag("src.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("span.kind", "string", "client"),
                new JaegerTag("src.object.name", "string", "ballerina/http/Caller"),
                new JaegerTag("entrypoint.function.module", "string", "$anon/.:0.0.0"),
                new JaegerTag("entrypoint.service.name", "string", SAMPLE_SERVER_NAME),
                new JaegerTag("entrypoint.resource.accessor", "string", "get"),
                new JaegerTag("src.position", "string", span3Position),
                new JaegerTag("src.client.remote", "string", "true"),
                new JaegerTag("src.function.name", "string", "respond"),
                new JaegerTag("otlp.instrumentation.library.name", "string", "jaeger"),
                new JaegerTag("status.code", "int64", "0")
        )));

        Assert.assertTrue(jaegerTrace.getProcesses().containsKey(JAEGER_PROCESS_ID),
                "expected key \"" + JAEGER_PROCESS_ID + "\" not found");
        JaegerProcess jaegerProcess = jaegerTrace.getProcesses().get(JAEGER_PROCESS_ID);
        Assert.assertEquals(jaegerProcess.getServiceName(), SAMPLE_SERVER_NAME);

        Assert.assertFalse(errorLogLeecher.isTextFound(), "Unexpected error log found");
        Assert.assertFalse(exceptionLogLeecher.isTextFound(), "Unexpected exception log found");
    }

    @Test
    public void testJaegerDisabled() throws Exception {
        LogLeecher jaegerExtLogLeecher = new LogLeecher(JAEGER_EXTENSION_LOG_PREFIX);
        serverInstance.addLogLeecher(jaegerExtLogLeecher);
        LogLeecher errorLogLeecher = new LogLeecher("error");
        serverInstance.addErrorLogLeecher(errorLogLeecher);
        LogLeecher exceptionLogLeecher = new LogLeecher("Exception");
        serverInstance.addErrorLogLeecher(exceptionLogLeecher);

        final String balFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "01_http_svc_test.bal").toFile()
                .getAbsolutePath();
        int[] requiredPorts = {9091};
        serverInstance.startServer(balFile, null, null, requiredPorts);
        Utils.waitForPortsToOpen(requiredPorts, 1000 * 60, false, InetAddress.getByName("localhost"));

        String responseData = HttpClientRequest.doGet(TEST_RESOURCE_URL).getData();
        Assert.assertEquals(responseData, "Sum: 53");

        Assert.assertFalse(jaegerExtLogLeecher.isTextFound(), "Jaeger extension not expected to enable");
        Assert.assertFalse(errorLogLeecher.isTextFound(), "Unexpected error log found");
        Assert.assertFalse(exceptionLogLeecher.isTextFound(), "Unexpected exception log found");
    }

    @Test
    public void testInvalidTracingProviderName() throws Exception {
        LogLeecher jaegerExtLogLeecher = new LogLeecher(JAEGER_EXTENSION_LOG_PREFIX);
        serverInstance.addLogLeecher(jaegerExtLogLeecher);
        LogLeecher tracerNotFoundLog = new LogLeecher("error: tracer provider invalid not found");
        serverInstance.addErrorLogLeecher(tracerNotFoundLog);
        LogLeecher exceptionLogLeecher = new LogLeecher("Exception");
        serverInstance.addErrorLogLeecher(exceptionLogLeecher);

        String configFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "ConfigInvalidProvider.toml").toFile()
                .getAbsolutePath();
        Map<String, String> env = new HashMap<>();
        env.put("BAL_CONFIG_FILES", configFile);

        final String balFile = Paths.get(RESOURCES_DIR.getAbsolutePath(), "01_http_svc_test.bal").toFile()
                .getAbsolutePath();
        int[] requiredPorts = {9091};
        serverInstance.startServer(balFile, new String[]{"--observability-included"}, null, env, requiredPorts);
        Utils.waitForPortsToOpen(requiredPorts, 1000 * 60, false, InetAddress.getByName("localhost"));
        tracerNotFoundLog.waitForText(10000);

        String responseData = HttpClientRequest.doGet(TEST_RESOURCE_URL).getData();
        Assert.assertEquals(responseData, "Sum: 53");

        Assert.assertFalse(jaegerExtLogLeecher.isTextFound(), "Jaeger extension not expected to enable");
        Assert.assertFalse(exceptionLogLeecher.isTextFound(), "Unexpected exception log found");
    }

    /**
     * Find a span from a jaeger trace by position ID.
     *
     * @param jaegerTrace The jaeger trace in which the spans should be searched
     * @param positionID  The position ID of the span
     * @return The found span or null otherwise
     */
    private JaegerSpan findSpan(JaegerTrace jaegerTrace, String positionID) {
        for (JaegerSpan span : jaegerTrace.getSpans()) {
            Optional<JaegerTag> positionTag =
                    span.getTags().stream().filter(t -> "src.position".equals(t.getKey())).findAny();
            if (positionTag.isPresent() && positionID.equals(positionTag.get().getValue())) {
                return span;
            }
        }
        return null;
    }
}
