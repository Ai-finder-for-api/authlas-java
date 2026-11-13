package io.authlas;

/**
 * Raw JNI bindings. One method per export in native/authlas_jni.c. Use {@link Authlas} instead; it parses the results.
 * The native library is loaded by {@link NativeLoader} before any method here runs.
 */
final class Native {
    private Native() {}

    static native String version();
    static native String backend();
    static native int abiVersion();

    static native String jwkThumbprint(String key);
    static native String jwkGenerateEc(String crv);

    static native String pkceNew();
    static native void pkceVerify(String challenge, String verifier);

    static native String bearerParse(String header);
    static native String scopeCheck(String requested, String allowed);
}
