package io.ballerina.observe.trace.jaeger;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

public class JaegerLogger {
    private static final String BALLERINA_ORG_NAME = "ballerina";
    private static final String BALLERINAX_ORG_NAME = "ballerinax";
    private static final String JAEGER_MODULE_NAME = "jaeger";
    private static final String LOG_MODULE_NAME = "log";
    private final Module logModule;
    private final BMap<BString, Object> keyValues;
    private final Environment environment;

    public JaegerLogger(Environment environment) {
        this.environment = environment;

        logModule = new Module(BALLERINA_ORG_NAME, LOG_MODULE_NAME, "2");
        keyValues = ValueCreator.createMapValue();
        keyValues.put(StringUtils.fromString("module"),
                StringUtils.fromString(BALLERINAX_ORG_NAME + "/" + JAEGER_MODULE_NAME));
    }

    private void printLog(String message, String logPrintFunctionName) {
        Object[] params = new Object[]{ StringUtils.fromString(message), null, null, keyValues };
        environment.getRuntime().callFunction(logModule, logPrintFunctionName, null, params);
    }

    public void printInfoLog(String message) {
        printLog(message, "printInfo");
    }

    public void printDebugLog(String message) {
        printLog(message, "printDebug");
    }

    public void printWarnLog(String message) {
        printLog(message, "printWarn");
    }

    public void printErrorLog(String message) {
        printLog(message, "printError");
    }
}
