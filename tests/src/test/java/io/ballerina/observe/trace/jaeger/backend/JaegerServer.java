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
package io.ballerina.observe.trace.jaeger.backend;

/**
 * Interface for a Jaeger Server.
 */
public interface JaegerServer {

    /**
     * Start the Jaeger server.
     *
     * @param interfaceIP The IP of the interface to bind to
     * @param udpBindPort The UDP publishing port to bind to
     * @param protocol    The protocol to be used in the port
     * @throws Exception if starting the server fails
     */
    void startServer(String interfaceIP, int udpBindPort, JaegerServerProtocol protocol) throws Exception;

    /**
     * Stop the Jaeger server which had been started.
     *
     * @throws Exception if stopping the server fails
     */
    void stopServer() throws Exception;

    /**
     * Cleanup the server and all related resources.
     *
     * @throws Exception if cleanup fails
     */
    void cleanUp() throws Exception;
}
