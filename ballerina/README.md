## Overview

The Jaeger Observability Extension provides an implementation for tracing and publishing traces to a [Jaeger](https://www.jaegertracing.io/) Agent.

### Key Features

- Publish distributed traces to a Jaeger Agent via OpenTelemetry
- Configurable sampler type and parameters
- Support for trace logging to console and file
- Configurable reporter flush interval and buffer size

## Enabling Jaeger Extension

To package the Jaeger extension into the Jar, follow the following steps.
1. Add the following import to your program.
```ballerina
import ballerinax/jaeger as _;
```

2. Add the following to the `Ballerina.toml` when building your program.
```toml
[package]
org = "my_org"
name = "my_package"
version = "1.0.0"

[build-options]
observabilityIncluded=true
```

To enable the extension and publish traces to Jaeger, add the following to the `Config.toml` when running your program.
```toml
[ballerina.observe]
tracingEnabled=true
tracingProvider="jaeger"

[ballerinax.jaeger]
agentHostname="127.0.0.1"       # Optional Configuration. Default value is localhost
agentPort=4317                  # Optional Configuration. Default value is 55680
samplerType="const"             # Optional Configuration. Default value is const
samplerParam=1                  # Optional Configuration. Default value is 1
reporterFlushInterval=1000      # Optional Configuration. Default value is 1000
reporterBufferSize=10000        # Optional Configuration. Default value is 10000
traceLogConsole = false         # Optional Configuration. Default value is false
traceLogFile = ""               # Optional Configuration. Default value is empty string
traceLogLevel = "info"          # Optional Configuration. Default value is info. Possible values are debug, info, warn, error
```
