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

import com.google.common.reflect.TypeToken;

import java.util.List;

/**
 * Jaeger Query endpoint response related Gson type tokens.
 */
public class JaegerQueryResponseTypeToken {

    /**
     * Type token for the data in the response of /api/services query endpoint.
     */
    public static class Services extends TypeToken<JaegerQueryResponse<List<String>>> {
        private static final long serialVersionUID = -2338626252352177485L;
    }

    /**
     * Type token for the data in the response of /api/traces query endpoint.
     */
    public static class Traces extends TypeToken<JaegerQueryResponse<List<JaegerTrace>>> {
        private static final long serialVersionUID = -2338626252327477485L;
    }
}
