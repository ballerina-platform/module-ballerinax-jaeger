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
package io.ballerina.observe.trace.jaeger.model;

import java.util.List;
import java.util.Map;

/**
 * Jaeger Trace model.
 */
public class JaegerTrace {
    private String traceID;
    private List<JaegerSpan> spans;
    private Map<String, JaegerProcess> processes;

    public String getTraceID() {
        return traceID;
    }

    public void setTraceID(String traceID) {
        this.traceID = traceID;
    }

    public List<JaegerSpan> getSpans() {
        return spans;
    }

    public void setSpans(List<JaegerSpan> spans) {
        this.spans = spans;
    }

    public Map<String, JaegerProcess> getProcesses() {
        return processes;
    }

    public void setProcesses(Map<String, JaegerProcess> processes) {
        this.processes = processes;
    }
}
