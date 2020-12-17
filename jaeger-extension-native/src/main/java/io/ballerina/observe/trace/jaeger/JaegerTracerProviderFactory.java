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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.ballerina.runtime.observability.tracer.spi.TracerProviderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import static io.ballerina.observe.trace.jaeger.Constants.PACKAGE_NAME;
import static io.ballerina.observe.trace.jaeger.Constants.PACKAGE_ORG;
import static io.ballerina.observe.trace.jaeger.Constants.PACKAGE_VERSION_PROPERTY_KEY;
import static io.ballerina.observe.trace.jaeger.Constants.TRACER_NAME;
import static io.ballerina.observe.trace.jaeger.Constants.TRACER_PROPERTIES_FILE;

/**
 * This is the Jaeger tracing extension class for {@link TracerProviderFactory}.
 */
public class JaegerTracerProviderFactory implements TracerProviderFactory {
    private static final PrintStream console = System.out;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public BObject getProviderBObject() {
        String jaegerModuleVersion;
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(TRACER_PROPERTIES_FILE);
            Properties tracerProperties = new Properties();
            tracerProperties.load(stream);
            jaegerModuleVersion = (String) tracerProperties.get(PACKAGE_VERSION_PROPERTY_KEY);
        } catch (IOException | ClassCastException e) {
            console.println("ballerina: unexpected failure in detecting Jaeger extension version");
            return null;
        }
        Module jaegerModule = new Module(PACKAGE_ORG, PACKAGE_NAME, jaegerModuleVersion);
        return ValueCreator.createObjectValue(jaegerModule, "TracerProvider");
    }

    @Override
    public TracerProvider getProvider() {
        return new JaegerTracerProvider();
    }
}
