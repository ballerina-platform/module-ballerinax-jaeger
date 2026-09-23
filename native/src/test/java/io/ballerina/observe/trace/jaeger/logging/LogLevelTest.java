/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
package io.ballerina.observe.trace.jaeger.logging;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

/**
 * Test class for LogLevel.
 */
public class LogLevelTest {

    @Test
    public void testValues() {
        assertEquals(LogLevel.OFF.value(), Integer.MAX_VALUE);
        assertEquals(LogLevel.ERROR.value(), 1000);
        assertEquals(LogLevel.WARN.value(), 900);
        assertEquals(LogLevel.INFO.value(), 800);
        assertEquals(LogLevel.DEBUG.value(), 700);
        assertEquals(LogLevel.TRACE.value(), 600);
        assertEquals(LogLevel.ALL.value(), Integer.MIN_VALUE);
    }

    @Test
    public void testToLogLevelValidNames() {
        assertEquals(LogLevel.toLogLevel("OFF"), LogLevel.OFF);
        assertEquals(LogLevel.toLogLevel("ERROR"), LogLevel.ERROR);
        assertEquals(LogLevel.toLogLevel("WARN"), LogLevel.WARN);
        assertEquals(LogLevel.toLogLevel("INFO"), LogLevel.INFO);
        assertEquals(LogLevel.toLogLevel("DEBUG"), LogLevel.DEBUG);
        assertEquals(LogLevel.toLogLevel("TRACE"), LogLevel.TRACE);
        assertEquals(LogLevel.toLogLevel("ALL"), LogLevel.ALL);
    }

    @Test
    public void testToLogLevelInvalidNameThrows() {
        RuntimeException e = expectThrows(RuntimeException.class, () -> LogLevel.toLogLevel("VERBOSE"));
        assertEquals(e.getMessage(), "invalid log level: VERBOSE");
    }
}
