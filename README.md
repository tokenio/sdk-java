Overview
========
Token open source SDKs simplify the interactions with the Token global open banking REST/gRPC API. The Token SDKs handle digital signatures and, where applicable, chain Token API calls. This makes it easier to develop Token- integrated applications, while providing most of the flexibility of the full Token API.

Requirements
============
The SDK requires Java 7.

Building
========
To build the SDK execute:

```
./gradlew :sdk:build
```

Testing
=======
The SDK comes with an integration test suite. The Test suite can be run against multiple environments and partner setups. Each setup is tracked with a dedicated configuration file. The configuration files can be found under:

```
integration/src/test/resources
```

Example: To run the tests against development environment execute:

```
./gradlew -DTOKEN_ENV=staging :integration:cleanTest :integration:test
```

The -DTOKEN-ENV property instructs the tests to load the specified environment configuration (staging in the example above).

The environment config files are also used to disable/blacklist certain tests. This is used to gradually enable test coverage when a new environment is being build up with functionality expanding over time.
