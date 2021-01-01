module io.ballerina.observe.trace.extension.jaeger {
    requires io.ballerina.runtime;
    requires io.ballerina.config;
    requires opentracing.api;
    requires opentracing.noop;
    requires jaeger.core;

    provides io.ballerina.runtime.observability.tracer.spi.TracerProviderFactory
            with io.ballerina.observe.trace.jaeger.JaegerTracerProviderFactory;
}
