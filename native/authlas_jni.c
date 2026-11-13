/* authlas JNI shim. Each Java `native` method calls one function of the Authlas C ABI (authlas.h) and returns
 * its JSON result as a Java String. A non-zero status becomes an io.authlas.AuthlasException with the core's JSON
 * error. Every result buffer is released with authlas_buf_free. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "authlas.h"

#ifdef _WIN32
#  define JNI_EXPORT_FN __declspec(dllexport)
#else
#  define JNI_EXPORT_FN __attribute__((visibility("default")))
#endif

static void throw_status(JNIEnv *env, int status) {
    const char *err = authlas_last_error();
    jclass cls = (*env)->FindClass(env, "io/authlas/AuthlasException");
    if (cls == NULL) return;
    char msg[512];
    snprintf(msg, sizeof msg, "authlas status %d: %s", status, err ? err : "unknown error");
    (*env)->ThrowNew(env, cls, msg);
}

/* Convert a Java string to UTF-8 (NULL stays NULL). Caller releases with release_utf. */
static const char *get_utf(JNIEnv *env, jstring s) {
    if (s == NULL) return NULL;
    return (*env)->GetStringUTFChars(env, s, NULL);
}

static void release_utf(JNIEnv *env, jstring s, const char *c) {
    if (s != NULL && c != NULL) (*env)->ReleaseStringUTFChars(env, s, c);
}

/* Build a Java String from an AuthlasBuf, then free the buffer. */
static jstring buf_to_jstring(JNIEnv *env, AuthlasBuf *buf) {
    jstring out = NULL;
    if (buf->data != NULL) {
        out = (*env)->NewStringUTF(env, (const char *)buf->data);
    }
    authlas_buf_free(buf);
    return out;
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_version(JNIEnv *env, jclass cls) {
    (void)cls;
    return (*env)->NewStringUTF(env, authlas_version());
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_backend(JNIEnv *env, jclass cls) {
    (void)cls;
    return (*env)->NewStringUTF(env, authlas_backend());
}

JNI_EXPORT_FN jint JNICALL Java_io_authlas_Native_abiVersion(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    return (jint)authlas_abi_version();
}

/* Generic helper for the common shape: one JSON/string input, one JSON output. */
JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_jwkThumbprint(JNIEnv *env, jclass cls, jstring key) {
    (void)cls;
    const char *k = get_utf(env, key);
    AuthlasBuf out = {NULL, 0};
    int st = authlas_jwk_thumbprint(k, &out);
    release_utf(env, key, k);
    if (st != 0) { authlas_buf_free(&out); throw_status(env, st); return NULL; }
    return buf_to_jstring(env, &out);
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_jwkGenerateEc(JNIEnv *env, jclass cls, jstring crv) {
    (void)cls;
    const char *c = get_utf(env, crv);
    AuthlasBuf out = {NULL, 0};
    int st = authlas_jwk_generate_ec(c, &out);
    release_utf(env, crv, c);
    if (st != 0) { authlas_buf_free(&out); throw_status(env, st); return NULL; }
    return buf_to_jstring(env, &out);
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_pkceNew(JNIEnv *env, jclass cls) {
    (void)cls;
    AuthlasBuf out = {NULL, 0};
    int st = authlas_pkce_new(&out);
    if (st != 0) { authlas_buf_free(&out); throw_status(env, st); return NULL; }
    return buf_to_jstring(env, &out);
}

JNI_EXPORT_FN void JNICALL Java_io_authlas_Native_pkceVerify(JNIEnv *env, jclass cls, jstring challenge, jstring verifier) {
    (void)cls;
    const char *c = get_utf(env, challenge);
    const char *v = get_utf(env, verifier);
    AuthlasBuf out = {NULL, 0};
    int st = authlas_pkce_verify(c, v, &out);
    release_utf(env, challenge, c);
    release_utf(env, verifier, v);
    authlas_buf_free(&out);
    if (st != 0) throw_status(env, st);
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_bearerParse(JNIEnv *env, jclass cls, jstring header) {
    (void)cls;
    const char *h = get_utf(env, header);
    AuthlasBuf out = {NULL, 0};
    int st = authlas_bearer_parse(h, &out);
    release_utf(env, header, h);
    if (st != 0) { authlas_buf_free(&out); throw_status(env, st); return NULL; }
    return buf_to_jstring(env, &out);
}

JNI_EXPORT_FN jstring JNICALL Java_io_authlas_Native_scopeCheck(JNIEnv *env, jclass cls, jstring requested, jstring allowed) {
    (void)cls;
    const char *r = get_utf(env, requested);
    const char *a = get_utf(env, allowed);
    AuthlasBuf out = {NULL, 0};
    int st = authlas_scope_check(r, a, &out);
    release_utf(env, requested, r);
    release_utf(env, allowed, a);
    if (st != 0) { authlas_buf_free(&out); throw_status(env, st); return NULL; }
    return buf_to_jstring(env, &out);
}
