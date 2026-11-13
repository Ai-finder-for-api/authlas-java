package io.authlas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthlasTest {

    @Test
    void loadsCoreAndReportsVersion() {
        assertNotNull(Authlas.version());
        assertFalse(Authlas.version().isEmpty());
        assertEquals(1, Authlas.abiVersion());
    }

    @Test
    void pkceChallengeMatchesRfc7636Vector() {
        // RFC 7636 appendix B
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        Authlas.pkceVerify(challenge, verifier);
        assertThrows(AuthlasException.class, () -> Authlas.pkceVerify(challenge, verifier + "x"));
    }

    @Test
    void newPkcePairVerifies() {
        Authlas.Pkce p = Authlas.newPkce();
        assertEquals("S256", p.method());
        Authlas.pkceVerify(p.challenge(), p.verifier());
    }

    @Test
    void bearerParseReadsScheme() {
        Authlas.Authorization a = Authlas.bearerParse("Bearer abc.def.ghi");
        assertEquals("Bearer", a.scheme());
        assertEquals("abc.def.ghi", a.token());
    }

    @Test
    void scopeCheckReportsExcess() {
        Authlas.ScopeCheck ok = Authlas.scopeCheck("read", "read write");
        assertTrue(ok.ok());
        assertTrue(ok.excess().isEmpty());

        Authlas.ScopeCheck bad = Authlas.scopeCheck("read admin", "read write");
        assertFalse(bad.ok());
        assertEquals(java.util.List.of("admin"), bad.excess());
    }

    @Test
    void generatedEcKeyHasThumbprint() {
        String jwk = Authlas.generateEcKey("P-256");
        assertTrue(jwk.contains("\"crv\""));
        String thumb = Authlas.jwkThumbprint(jwk);
        assertEquals(43, thumb.length(), "base64url SHA-256 is 43 characters");
    }
}
