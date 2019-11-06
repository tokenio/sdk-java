Overview
========
Token open source SDKs simplify the interactions with the Token global open banking REST/gRPC API. 
The Token SDKs handle digital signatures and, where applicable, chain Token API calls. This makes 
it easier to develop Token- integrated applications, while providing most of the flexibility of 
the full Token API.
More information at [https://developer.token.io/docs/](https://developer.token.io/docs/)

Requirements
============
The SDK requires Java 7. To run on android, add the following line to your gradle properties file,
at ~/.gradle/gradle.properties:

```
android.injected.build.model.only.versioned=3
```

Building
========
To build the SDK execute:

```
./gradlew build
```

