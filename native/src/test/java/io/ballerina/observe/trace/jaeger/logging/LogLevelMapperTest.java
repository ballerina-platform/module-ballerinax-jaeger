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

import java.util.logging.Level;

import static org.testng.Assert.assertEquals;

/**
 * Test class for LogLevelMapper.
 */
public class LogLevelMapperTest {

    @Test
    public void testGetBallerinaLogLevel() {
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.SEVERE), "ERROR");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.WARNING), "WARN");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.INFO), "INFO");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.CONFIG), "INFO");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.FINE), "DEBUG");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.FINER), "DEBUG");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.FINEST), "TRACE");
    }

    @Test
    public void testGetBallerinaLogLevelUndefined() {
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.ALL), "<UNDEFINED>");
        assertEquals(LogLevelMapper.getBallerinaLogLevel(Level.OFF), "<UNDEFINED>");
    }

    @Test
    public void testGetLoggerLevel() {
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.OFF), Level.OFF);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.ERROR), Level.SEVERE);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.WARN), Level.WARNING);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.INFO), Level.INFO);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.DEBUG), Level.FINER);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.TRACE), Level.FINEST);
        assertEquals(LogLevelMapper.getLoggerLevel(LogLevel.ALL), Level.ALL);
    }
}
