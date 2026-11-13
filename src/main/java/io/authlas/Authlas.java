package io.authlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Public API for authlas. Every call runs in the native core; this class only parses the JSON it returns. */
public final class Authlas {
    static {
        NativeLoader.load();
    }

    private Authlas() {}

    /** Version of the authlas core. */
    public static String version() {
        return Native.version();
    }

    /** Name of the crypto backend in use, e.g. "openssl". */
    public static String backend() {
        return Native.backend();
    }

    /** ABI version of the C interface. */
    public static int abiVersion() {
        return Native.abiVersion();
    }

    /** RFC 7638 thumbprint (base64url SHA-256) of a JWK given as JSON. */
    public static String jwkThumbprint(String jwkJson) {
        return Native.jwkThumbprint(jwkJson);
    }

    /** Generates an EC key pair on curve {@code crv} ("P-256", "P-384" or "P-521"). Returns the private JWK as JSON. */
    public static String generateEcKey(String crv) {
        return Native.jwkGenerateEc(crv);
    }

    /** A PKCE verifier and its S256 challenge. */
    public record Pkce(String verifier, String challenge, String method) {}

    /** Creates a new PKCE pair (RFC 7636, S256). */
    public static Pkce newPkce() {
        Map<String, Object> m = Json.object(Native.pkceNew());
        return new Pkce((String) m.get("verifier"), (String) m.get("challenge"), (String) m.get("method"));
    }

    /** Checks a verifier against a challenge. Throws {@link AuthlasException} if they do not match. */
    public static void pkceVerify(String challenge, String verifier) {
        Native.pkceVerify(challenge, verifier);
    }

    /** A parsed Authorization header. */
    public record Authorization(String scheme, String token) {}

    /** Parses {@code Authorization: Bearer ...} or {@code DPoP ...}. */
    public static Authorization bearerParse(String header) {
        Map<String, Object> m = Json.object(Native.bearerParse(header));
        return new Authorization((String) m.get("scheme"), (String) m.get("token"));
    }

    /** Result of a scope check. {@code excess} lists requested scopes the client may not use. */
    public record ScopeCheck(boolean ok, List<String> excess) {}

    /** Checks that every requested scope is in the allowed set (space-separated scope strings). */
    public static ScopeCheck scopeCheck(String requested, String allowed) {
        Map<String, Object> m = Json.object(Native.scopeCheck(requested, allowed));
        Object ex = m.get("excess");
        List<String> excess = new ArrayList<>();
        if (ex instanceof List<?> list) {
            for (Object o : list) excess.add(String.valueOf(o));
        }
        return new ScopeCheck((Boolean) m.get("ok"), excess);
    }
}
