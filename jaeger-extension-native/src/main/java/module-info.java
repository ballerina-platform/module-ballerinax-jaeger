module io.ballerina.observe.trace.extension.jaeger {
    requires io.ballerina.runtime;
    requires jaeger.core;
    requires jaeger.thrift;
    requires io.opentelemetry.api;
    requires io.opentelemetry.api.metrics;
    requires io.opentelemetry.context;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.exporter.jaeger.thrift;
    requires okhttp3;
    requires okio.jvm;
    requires kotlin.stdlib;

    provides io.ballerina.runtime.observability.tracer.spi.TracerProvider
            with io.ballerina.observe.trace.jaeger.JaegerTracerProvider;
}
