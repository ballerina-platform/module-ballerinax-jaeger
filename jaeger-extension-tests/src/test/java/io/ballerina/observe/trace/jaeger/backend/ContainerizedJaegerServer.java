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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Container based Jaeger Server.
 *
 * This is a Jaeger server implementation based on a linux Jaeger container.
 */
public class ContainerizedJaegerServer implements JaegerServer {
    private static final String JAEGER_IMAGE = "jaegertracing/all-in-one:1.18";

    private DockerClient dockerClient;
    private String jaegerContainerId;

    public ContainerizedJaegerServer() throws Exception {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        dockerClient = DockerClientImpl.getInstance(dockerClientConfig,
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(dockerClientConfig.getDockerHost())
                        .sslConfig(dockerClientConfig.getSSLConfig())
                        .build());
        dockerClient.pullImageCmd(JAEGER_IMAGE)
                .start()
                .awaitCompletion(20, TimeUnit.SECONDS);
    }

    @Override
    public void startServer(String udpBindPort) {
        if (jaegerContainerId != null) {
            throw new IllegalStateException("Jaeger server already started");
        }
        if (dockerClient == null) {
            throw new IllegalStateException("Containerized Jaeger server cannot be started after " +
                    "cleaning up docker client");
        }
        jaegerContainerId = dockerClient.createContainerCmd(JAEGER_IMAGE)
                .withName("ballerina-test-jaeger-" + System.currentTimeMillis())
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(PortBinding.parse(udpBindPort + ":5775/udp"),
                                PortBinding.parse("16686:16686/tcp")))
                .exec()
                .getId();
        dockerClient.startContainerCmd(jaegerContainerId).exec();
    }

    @Override
    public void stopServer() {
        if (jaegerContainerId != null) {
            dockerClient.stopContainerCmd(jaegerContainerId).exec();
            dockerClient.removeContainerCmd(jaegerContainerId).exec();
            jaegerContainerId = null;
        }
    }

    @Override
    public void cleanUp() throws Exception {
        dockerClient.close();
        dockerClient = null;
    }
}