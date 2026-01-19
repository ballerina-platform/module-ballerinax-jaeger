package io.ballerina.observe.trace.jaeger;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JaegerExporter implements SpanExporter {
    private static final Logger logger = Logger.getLogger(JaegerExporter.class.getName());
    private String endpoint;
    private SpanExporter exporter;

    public JaegerExporter(SpanExporter exporter, String endpoint, boolean isTraceLoggingEnabled) {
        this.exporter = exporter;
        this.endpoint = endpoint;

        if (isTraceLoggingEnabled) {
            logger.setLevel(Level.FINE);
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        logger.info("ballerina: attempting to export " + spans.size() + " spans to " + endpoint);
        CompletableResultCode result = exporter.export(spans);
        result.whenComplete(() -> {
            if (result.isSuccess()) {
                logger.info("ballerina: successfully exported spans to " + endpoint);
            } else {
                logger.severe("ballerina: failed to export spans to " + endpoint);
            }
        });
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return exporter.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return exporter.shutdown();
    }
}
