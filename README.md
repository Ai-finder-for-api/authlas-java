# authlas-java

Java wrapper for authlas: OAuth 2.0 and JOSE. The cryptography and token rules run in the authlas Rust core, which this
library calls through JNI. The native libraries ship inside the jar, so consumers need no install.

## Build (one command)

Windows, with JDK 17+, Maven, Rust (cargo), Visual Studio Build Tools (C++ workload), OpenSSL 3, and a checkout of
[authlas-rust](https://github.com/Ai-finder-for-api/authlas-rust) next to this folder (or set `AUTHLAS_RUST_DIR`):

    build.bat

This builds the Rust core, compiles the JNI bridge, bundles the native libraries, and runs the tests.

## Use

    <dependency>
      <groupId>io.github.quantum-suite-labs</groupId>
      <artifactId>authlas</artifactId>
      <version>0.1.0</version>
    </dependency>

    Authlas.Pkce p = Authlas.newPkce();
    Authlas.pkceVerify(p.challenge(), p.verifier());

The jar currently bundles Windows x64 only.
