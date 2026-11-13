package io.authlas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Loads the authlas native libraries bundled in the jar under /natives/{os}-{arch}/. The Rust core (authlas) loads
 * first, because the JNI bridge calls into it. Libraries are copied to a temp folder, then loaded from there.
 */
final class NativeLoader {
    private static volatile boolean loaded;

    private NativeLoader() {}

    static synchronized void load() {
        if (loaded) return;
        String dir = platformDir();
        try {
            Path tmp = Files.createTempDirectory("authlas-natives-");
            tmp.toFile().deleteOnExit();
            for (String lib : libraryNames()) {
                Path target = tmp.resolve(lib);
                try (InputStream in = NativeLoader.class.getResourceAsStream("/natives/" + dir + "/" + lib)) {
                    if (in == null) {
                        throw new UnsatisfiedLinkError("authlas: no native library " + lib + " for " + dir + " in this jar");
                    }
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                target.toFile().deleteOnExit();
                System.load(target.toAbsolutePath().toString());
            }
            loaded = true;
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("authlas: could not extract native libraries: " + e.getMessage());
        }
    }

    /** Dependencies first (OpenSSL 3), then the Rust core, then the JNI bridge. Each file is copied and loaded in order. */
    private static String[] libraryNames() {
        if (isWindows()) return new String[] {"libcrypto-3-x64.dll", "libssl-3-x64.dll", "authlas.dll", "authlas_jni.dll"};
        if (isMac()) return new String[] {"libcrypto.3.dylib", "libssl.3.dylib", "libauthlas.dylib", "libauthlas_jni.dylib"};
        return new String[] {"libcrypto.so.3", "libssl.so.3", "libauthlas.so", "libauthlas_jni.so"};
    }

    static String platformDir() {
        String os = isWindows() ? "windows" : isMac() ? "macos" : "linux";
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String a = (arch.equals("amd64") || arch.equals("x86_64")) ? "x64" : (arch.equals("aarch64") || arch.equals("arm64")) ? "arm64" : arch;
        return os + "-" + a;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
    }
}
