module io.ballerina.observe.trace.extension.jaeger {
    requires io.ballerina.runtime;
    requires io.ballerina.config;
    requires opentracing.api;
    requires jaeger.core;

    exports io.ballerina.observe.trace.jaeger;
}
