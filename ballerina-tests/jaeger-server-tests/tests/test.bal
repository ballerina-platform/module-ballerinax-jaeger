// Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/test;
import ballerina/lang.runtime;

type ServicePayload record {|
    string[] data;
    int total;
    int 'limit;
    int offset;
    string? 'errors;
|};

type TracePayload record {|
    Trace[] data;
    int total;
    int 'limit;
    int offset;
    string? 'errors;
|};

type Trace record {|
    string traceID;
    Span[] spans;
    json processes;
    string? warnings;
|};

type Span record {|
    string traceID;
    string spanID;
    string operationName;
    Reference[] references;
    int startTime;
    int duration;
    Tag[] tags;
    Log[] logs;
    string processID;
    string[]? warnings;
|};

type Log record {|
    int timestamp;
    Tag[] fields; 
|};

type Tag record {|
    string key;
    string 'type;
    string|int value;
|};

type Reference record {|
    string refType;
    string traceID;
    string spanID;
|};

const string GET_SUM_SPAN_NAME = "get /sum";
const string OBSERVABLE_ADDER_SPAN_NAME = "ballerinax/jaeger_server_tests/ObservableAdder:getSum";
const string HTTP_CALLER_SPAN_NAME = "ballerina/http/Caller:respond";
const string CLIENT_SPAN_NAME = "ballerina/http/Client:get";
const string HTTP_CLIENT_SPAN_NAME = "ballerina/http/HttpClient:get";
const string HTTP_CACHING_CLIENT_SPAN_NAME = "ballerina/http/HttpCachingClient:get";

const string CLIENT_SPAN_KIND = "client";
const string SERVER_SPAN_KIND = "server";

http:Client jaegerClient = check new (string `http://localhost:16686`);
http:Client cl = check new (string `http://localhost:9091`);
http:Response res = new();
string httpModuleVersion = check getHTTPModuleVersion();
Trace? testServiceTrace = null;
map<Span> spanMap = {};

@test:BeforeSuite
function sendRequest() returns error? {
    res = check cl->get("/test/sum");

    runtime:sleep(10);

    json testServiceTracePayloadData = check jaegerClient->get("/api/traces?service=%2Ftest");
    TracePayload testServiceTracePayload = check testServiceTracePayloadData.fromJsonWithType(TracePayload);

    do {
        testServiceTrace = testServiceTracePayload.data[0];
    } on fail {
        test:assertFail("No traces found with service name: \"/test\".");
    }

    foreach Span span in (<Trace> testServiceTrace).spans {
        spanMap[span.operationName] = span;
    }
}

@test:Config
function testResponse() returns error? {
    test:assertEquals(res.statusCode, http:STATUS_OK, "Status code mismatched");
    test:assertEquals(res.getTextPayload(), "Sum: 53", "Payload mismatched");
}

@test:Config
function testServices() returns error? {
    json servicesPayloadData = check jaegerClient->get("/api/services");
    ServicePayload servicesPayload = check servicesPayloadData.fromJsonWithType(ServicePayload);

    test:assertTrue(isContain(servicesPayload.data, "/test"));
    test:assertTrue(isContain(servicesPayload.data, "Ballerina"));
}

@test:Config
function testSpanNames() returns error? {
    string[] spanNames = spanMap.keys();

    test:assertTrue(isContain(spanNames, GET_SUM_SPAN_NAME), "Span name: \""+ GET_SUM_SPAN_NAME +"\" not found");
    test:assertTrue(isContain(spanNames, OBSERVABLE_ADDER_SPAN_NAME), "Span name: \"" + OBSERVABLE_ADDER_SPAN_NAME + "\" not found");
    test:assertTrue(isContain(spanNames, HTTP_CALLER_SPAN_NAME), "Span name: \"" + HTTP_CALLER_SPAN_NAME + "\" not found");

    test:assertTrue(isContain(spanNames, CLIENT_SPAN_NAME), "Span name: \"" + CLIENT_SPAN_NAME + "\" not found");
    test:assertTrue(isContain(spanNames, HTTP_CACHING_CLIENT_SPAN_NAME), "Span name: \"" + HTTP_CACHING_CLIENT_SPAN_NAME + "\" not found");
    test:assertTrue(isContain(spanNames, HTTP_CLIENT_SPAN_NAME), "Span name: \"" + HTTP_CLIENT_SPAN_NAME + "\" not found");
}

@test:Config
function testProcessIDs() returns error? {
    // Spans in the Ballerina service
    test:assertEquals((<Span> spanMap[CLIENT_SPAN_NAME]).processID, 
        (<Span> spanMap[HTTP_CACHING_CLIENT_SPAN_NAME]).processID);
    test:assertEquals((<Span> spanMap[HTTP_CACHING_CLIENT_SPAN_NAME]).processID, 
        (<Span> spanMap[HTTP_CLIENT_SPAN_NAME]).processID);

    // Spans in the /test service
    test:assertEquals((<Span> spanMap[GET_SUM_SPAN_NAME]).processID, 
        (<Span> spanMap[OBSERVABLE_ADDER_SPAN_NAME]).processID);
    test:assertEquals((<Span> spanMap[OBSERVABLE_ADDER_SPAN_NAME]).processID, 
        (<Span> spanMap[HTTP_CALLER_SPAN_NAME]).processID);
}

@test:Config
function testSpanInheritance() returns error? {
    Span clientSpan = <Span> spanMap[CLIENT_SPAN_NAME];
    Span httpCachingClientSpan = <Span> spanMap[HTTP_CACHING_CLIENT_SPAN_NAME];
    Span httpClientSpan = <Span> spanMap[HTTP_CLIENT_SPAN_NAME];
    Span getSumSpan = <Span> spanMap[GET_SUM_SPAN_NAME];
    Span observableAdderSpan = <Span> spanMap[OBSERVABLE_ADDER_SPAN_NAME];
    Span httpCallerSpan = <Span> spanMap[HTTP_CALLER_SPAN_NAME];
    
    test:assertEquals(clientSpan.spanID, httpCachingClientSpan.references[0].spanID, 
        "ParentId mismatched for " + HTTP_CACHING_CLIENT_SPAN_NAME);
    test:assertEquals(httpCachingClientSpan.spanID, httpClientSpan.references[0].spanID, 
        "ParentId mismatched for " + HTTP_CLIENT_SPAN_NAME);
    test:assertEquals(httpClientSpan.spanID, getSumSpan.references[0].spanID, 
        "ParentId mismatched for " + GET_SUM_SPAN_NAME);
    test:assertEquals(getSumSpan.spanID, observableAdderSpan.references[0].spanID, 
        "ParentId mismatched for " + OBSERVABLE_ADDER_SPAN_NAME);
    test:assertEquals(getSumSpan.spanID, httpCallerSpan.references[0].spanID, 
        "ParentId mismatched for " + HTTP_CALLER_SPAN_NAME);
}

@test:Config
function testSpanKind() returns error? {
    string|int clientSpanKind = (<Span> spanMap[CLIENT_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;
    string|int httpCachingClientSpanKind = (<Span> spanMap[HTTP_CACHING_CLIENT_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;
    string|int httpClientSpanKind = (<Span> spanMap[HTTP_CLIENT_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;
    string|int getSumSpanKind = (<Span> spanMap[GET_SUM_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;
    string|int observableAdderSpanKind = (<Span> spanMap[OBSERVABLE_ADDER_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;
    string|int httpCallerSpanKind = (<Span> spanMap[HTTP_CALLER_SPAN_NAME]).tags.filter(tag => tag.key == "span.kind")[0].value;

    test:assertEquals(clientSpanKind, CLIENT_SPAN_KIND, "Span kind mismatched for " + CLIENT_SPAN_NAME);
    test:assertEquals(httpCachingClientSpanKind, CLIENT_SPAN_KIND, "Span kind mismatched for " + HTTP_CACHING_CLIENT_SPAN_NAME);
    test:assertEquals(httpClientSpanKind, CLIENT_SPAN_KIND, "Span kind mismatched for " + HTTP_CLIENT_SPAN_NAME);
    test:assertEquals(getSumSpanKind, SERVER_SPAN_KIND, "Span kind mismatched for " + GET_SUM_SPAN_NAME);
    test:assertEquals(observableAdderSpanKind, CLIENT_SPAN_KIND, "Span kind mismatched for " + OBSERVABLE_ADDER_SPAN_NAME);
    test:assertEquals(httpCallerSpanKind, CLIENT_SPAN_KIND, "Span kind mismatched for " + HTTP_CALLER_SPAN_NAME);
}

@test:Config
function testGetSumSpanTags() returns error? {
    map<string> getSumSpanTags = {};
    foreach Tag tag in (<Span> spanMap[GET_SUM_SPAN_NAME]).tags {
        getSumSpanTags[tag.key] = tag.value.toString();
    }
    string[] getSumSpanTagKeys = getSumSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["entrypoint.function.name"], "/sum");
    test:assertTrue(containsTag("entrypoint.resource.accessor", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["entrypoint.resource.accessor"], "get");
    test:assertTrue(containsTag("entrypoint.service.name", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["entrypoint.service.name"], "/test");
    test:assertTrue(containsTag("http.method", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["http.method"], http:GET);
    test:assertTrue(containsTag("http.status_code", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["http.status_code"], http:STATUS_OK.toString());
    test:assertTrue(containsTag("http.url", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["http.url"], "/test/sum");
    test:assertTrue(containsTag("listener.name", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["listener.name"], "http");
    test:assertTrue(containsTag("otlp.instrumentation.library.name", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("protocol", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["protocol"], "http");
    test:assertTrue(containsTag("span.kind", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["span.kind"], "server");
    test:assertTrue(containsTag("src.module", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("src.object.name", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.object.name"], "/test");
    test:assertTrue(containsTag("src.position", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.position"], "main.bal:26:5");
    test:assertTrue(containsTag("src.resource.accessor", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.resource.accessor"], "get");
    test:assertTrue(containsTag("src.resource.path", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.resource.path"], "/sum");
    test:assertTrue(containsTag("src.service.resource", getSumSpanTagKeys));
    test:assertEquals(getSumSpanTags["src.service.resource"], "true");
}

@test:Config
function testObservableAdderSpanTags() returns error? {
    map<string> observableAdderSpanTags = {};
    foreach Tag tag in (<Span> spanMap[OBSERVABLE_ADDER_SPAN_NAME]).tags {
        observableAdderSpanTags[tag.key] = tag.value.toString();
    }
    string[] observableAdderSpanTagKeys = observableAdderSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["entrypoint.function.name"], "/sum");
    test:assertTrue(containsTag("entrypoint.resource.accessor", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["entrypoint.resource.accessor"], "get");
    test:assertTrue(containsTag("entrypoint.service.name", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["entrypoint.service.name"], "/test");
    test:assertTrue(containsTag("otlp.instrumentation.library.name", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("span.kind", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["span.kind"], "client");
    test:assertTrue(containsTag("src.function.name", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["src.function.name"], "getSum");
    test:assertTrue(containsTag("src.module", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["src.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("src.object.name", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["src.object.name"], "ballerinax/jaeger_server_tests/ObservableAdder");
    test:assertTrue(containsTag("src.position", observableAdderSpanTagKeys));
    test:assertEquals(observableAdderSpanTags["src.position"], "main.bal:28:19");
}

@test:Config
function testHttpCallerSpanTags() returns error? {
    map<string> httpCallerSpanTags = {};
    foreach Tag tag in (<Span> spanMap[HTTP_CALLER_SPAN_NAME]).tags {
        httpCallerSpanTags[tag.key] = tag.value.toString();
    }
    string[] httpCallerSpanTagKeys = httpCallerSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["entrypoint.function.name"], "/sum");
    test:assertTrue(containsTag("entrypoint.resource.accessor", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["entrypoint.resource.accessor"], "get");
    test:assertTrue(containsTag("entrypoint.service.name", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["entrypoint.service.name"], "/test");
    test:assertTrue(containsTag("http.status_code", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["http.status_code"], http:STATUS_OK.toString());
    test:assertTrue(containsTag("otlp.instrumentation.library.name", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("span.kind", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["span.kind"], "client");
    test:assertTrue(containsTag("src.client.remote", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["src.client.remote"], "true");
    test:assertTrue(containsTag("src.function.name", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["src.function.name"], "respond");
    test:assertTrue(containsTag("src.module", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["src.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("src.object.name", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["src.object.name"], "ballerina/http/Caller");
    test:assertTrue(containsTag("src.position", httpCallerSpanTagKeys));
    test:assertEquals(httpCallerSpanTags["src.position"], "main.bal:32:20");
}

@test:Config
function testClientSpanTags() returns error? {
    map<string> clientSpanTags = {};
    foreach Tag tag in (<Span> spanMap[CLIENT_SPAN_NAME]).tags {
        clientSpanTags[tag.key] = tag.value.toString();
    }
    string[] clientSpanTagKeys = clientSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["entrypoint.function.name"], "get");
    test:assertTrue(containsTag("http.base_url", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["http.base_url"], "http://localhost:9091");
    test:assertTrue(containsTag("http.method", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["http.method"], http:GET);
    test:assertTrue(containsTag("http.status_code", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["http.status_code"], http:STATUS_OK.toString());
    test:assertTrue(containsTag("http.url", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["http.url"], "/test/sum");
    test:assertTrue(containsTag("otlp.instrumentation.library.name", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("span.kind", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["span.kind"], "client");
    test:assertTrue(containsTag("src.client.remote", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["src.client.remote"], "true");
    test:assertTrue(containsTag("src.function.name", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["src.function.name"], "get");
    test:assertTrue(containsTag("src.module", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["src.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("src.object.name", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["src.object.name"], "ballerina/http/Client");
    test:assertTrue(containsTag("src.position", clientSpanTagKeys));
    test:assertEquals(clientSpanTags["src.position"], "tests/test.bal:93:17");
}

@test:Config
function testHttpCachingClientSpanTags() returns error? {
    map<string> httpCachingClientSpanTags = {};
    foreach Tag tag in (<Span> spanMap[HTTP_CACHING_CLIENT_SPAN_NAME]).tags {
        httpCachingClientSpanTags[tag.key] = tag.value.toString();
    }
    string[] httpCachingClientSpanTagKeys = httpCachingClientSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["entrypoint.function.name"], "get");
    test:assertTrue(containsTag("otlp.instrumentation.library.name", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("span.kind", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["span.kind"], "client");
    test:assertTrue(containsTag("src.client.remote", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["src.client.remote"], "true");
    test:assertTrue(containsTag("src.function.name", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["src.function.name"], "get");
    test:assertTrue(containsTag("src.module", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["src.module"], string `ballerina/http:${httpModuleVersion}`);
    test:assertTrue(containsTag("src.object.name", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["src.object.name"], "ballerina/http/HttpCachingClient");
    test:assertTrue(containsTag("src.position", httpCachingClientSpanTagKeys));
    test:assertEquals(httpCachingClientSpanTags["src.position"], "http_client_endpoint.bal:91:41");
}

@test:Config
function testHttpClientSpanTags() returns error? {
    map<string> httpClientSpanTags = {};
    foreach Tag tag in (<Span> spanMap[HTTP_CLIENT_SPAN_NAME]).tags {
        httpClientSpanTags[tag.key] = tag.value.toString();
    }
    string[] httpClientSpanTagKeys = httpClientSpanTags.keys();

    test:assertTrue(containsTag("entrypoint.function.module", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["entrypoint.function.module"], "ballerinax/jaeger_server_tests:0.1.0");
    test:assertTrue(containsTag("entrypoint.function.name", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["entrypoint.function.name"], "get");
    test:assertTrue(containsTag("http.method", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["http.method"], http:GET);
    test:assertTrue(containsTag("http.status_code", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["http.status_code"], http:STATUS_OK.toString());
    test:assertTrue(containsTag("http.url", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["http.url"], "/test/sum");
    test:assertTrue(containsTag("otlp.instrumentation.library.name", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["otlp.instrumentation.library.name"], "jaeger");
    test:assertTrue(containsTag("peer.address", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["peer.address"], "localhost:9091");
    test:assertTrue(containsTag("span.kind", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["span.kind"], "client");
    test:assertTrue(containsTag("src.client.remote", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["src.client.remote"], "true");
    test:assertTrue(containsTag("src.function.name", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["src.function.name"], "get");
    test:assertTrue(containsTag("src.module", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["src.module"], string `ballerina/http:${httpModuleVersion}`);
    test:assertTrue(containsTag("src.object.name", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["src.object.name"], "ballerina/http/HttpClient");
    test:assertTrue(containsTag("src.position", httpClientSpanTagKeys));
    test:assertEquals(httpClientSpanTags["src.position"], "caching_http_caching_client.bal:372:16");
}

function isContain(string[] array, string id) returns boolean {
    return array.indexOf(id) != ();
}

function containsTag(string tagKey, string[] traceTagKeys) returns boolean {
    foreach string key in traceTagKeys {
        if (key == tagKey) {
            return true;
        }
    }
    return false;
}
