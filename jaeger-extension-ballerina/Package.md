## Package Overview

The Jaeger Observability Extension is one of the tracing extensions of the<a target="_blank" href="https://ballerina.io/"> Ballerina</a> language.

It provides an implementation for tracing and publishing traces to a Jaeger Agent.

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
agentHostname="127.0.0.1"  # Optional Configuration. Default value is localhost
agentPort=6831             # Optional Configuration. Default value is 6831
```
