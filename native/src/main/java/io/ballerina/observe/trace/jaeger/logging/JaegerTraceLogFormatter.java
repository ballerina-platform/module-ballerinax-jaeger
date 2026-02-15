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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A custom log formatter for formatting Jaeger trace log files.
 *
 * @since 0.93
 */
public class JaegerTraceLogFormatter extends Formatter {

    private static final String DEFAULT_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS,%1$tL] %2$s {%3$s} - %4$s%5$s%n";
    private static String format = LogManager.getLogManager().getProperty(
            JaegerTraceLogFormatter.class.getCanonicalName() + ".format");

    static {
        if (format == null || format.isEmpty()) {
            format = DEFAULT_FORMAT;
        }
    }

    @Override
    public String format(LogRecord record) {
        String source = record.getLoggerName();
        String ex = "";

        if (record.getThrown() != null) {
            StringWriter stringWriter = new StringWriter();
            stringWriter.append('\n');
            record.getThrown().printStackTrace(new PrintWriter(stringWriter));
            ex = stringWriter.toString();
        }

        return String.format(format,
                new Date(record.getMillis()),
                LogLevelMapper.getBallerinaLogLevel(record.getLevel()),
                source,
                record.getMessage(),
                ex);
    }
}


