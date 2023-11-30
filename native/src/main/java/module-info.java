module io.ballerina.observe.trace.extension.jaeger {
    requires io.ballerina.runtime;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.extension.trace.propagation;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.exporter.otlp.trace;
    requires grpc.api;
    requires grpc.netty.shaded;

    provides io.ballerina.runtime.observability.tracer.spi.TracerProvider
            with io.ballerina.observe.trace.jaeger.JaegerTracerProvider;
}
