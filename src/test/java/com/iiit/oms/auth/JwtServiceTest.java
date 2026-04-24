package com.iiit.oms.auth;

import com.iiit.oms.model.User;
import com.iiit.oms.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private InMemoryRevocationStore revocationStore;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        revocationStore = new InMemoryRevocationStore();
        jwtService = new JwtService(revocationStore);
    }

    private User makeUser(String role) {
        User u = new User();
        u.setUserID("USR001");
        u.setUsername("testuser");
        u.setRole(role);
        return u;
    }

    // --- generateToken + validateAndExtract ---

    @Test
    void generateToken_containsCorrectClaims() {
        String token = jwtService.generateToken(makeUser("INVESTOR"));
        UserSession session = jwtService.validateAndExtract(token);
        assertNotNull(session);
        assertEquals("USR001", session.getUser().getUserID());
        assertEquals("INVESTOR", session.getUser().getRole());
        assertEquals("testuser", session.getUser().getUsername());
    }

    @Test
    void validateToken_validToken_returnsSession() {
        String token = jwtService.generateToken(makeUser("ADVISOR"));
        UserSession session = jwtService.validateAndExtract(token);
        assertNotNull(session);
        assertEquals("ADVISOR", session.getUser().getRole());
    }

    @Test
    void validateToken_tamperedSignature_returnsNull() {
        String token = jwtService.generateToken(makeUser("INVESTOR"));
        // Corrupt the signature (last segment)
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";
        assertNull(jwtService.validateAndExtract(tampered));
    }

    @Test
    void validateToken_malformedToken_returnsNull() {
        assertNull(jwtService.validateAndExtract("not.a.jwt"));
    }

    @Test
    void validateToken_nullToken_returnsNull() {
        assertNull(jwtService.validateAndExtract(null));
    }

    @Test
    void validateToken_emptyToken_returnsNull() {
        assertNull(jwtService.validateAndExtract(""));
    }

    // --- revocation ---

    @Test
    void revokeToken_revokedJti_returnsNull() {
        String token = jwtService.generateToken(makeUser("INVESTOR"));
        // Confirm valid before revocation
        assertNotNull(jwtService.validateAndExtract(token));

        jwtService.revokeToken(token);

        assertNull(jwtService.validateAndExtract(token));
    }

    @Test
    void revokeToken_otherTokensUnaffected() {
        String token1 = jwtService.generateToken(makeUser("INVESTOR"));
        String token2 = jwtService.generateToken(makeUser("ADVISOR"));

        jwtService.revokeToken(token1);

        assertNull(jwtService.validateAndExtract(token1), "revoked token should be null");
        assertNotNull(jwtService.validateAndExtract(token2), "other token should still be valid");
    }

    @Test
    void remainingTtlSeconds_freshToken_isPositive() {
        String token = jwtService.generateToken(makeUser("INVESTOR"));
        long ttl = jwtService.remainingTtlSeconds(token);
        assertTrue(ttl > 0 && ttl <= 86400, "TTL should be in (0, 86400] seconds");
    }

    // --- BCrypt password hashing ---

    @Test
    void bcryptLogin_correctPassword_succeeds() {
        String plain = "invest123";
        String hashed = BCrypt.hashpw(plain, BCrypt.gensalt());
        assertTrue(BCrypt.checkpw(plain, hashed));
    }

    @Test
    void bcryptLogin_wrongPassword_fails() {
        String plain = "invest123";
        String hashed = BCrypt.hashpw(plain, BCrypt.gensalt());
        assertFalse(BCrypt.checkpw("wrongpass", hashed));
    }

    @Test
    void bcryptLogin_hashIsNotPlaintext() {
        String plain = "invest123";
        String hashed = BCrypt.hashpw(plain, BCrypt.gensalt());
        assertNotEquals(plain, hashed);
    }

    @Test
    void bcryptLogin_twoHashesDiffer() {
        String plain = "invest123";
        String hash1 = BCrypt.hashpw(plain, BCrypt.gensalt());
        String hash2 = BCrypt.hashpw(plain, BCrypt.gensalt());
        // BCrypt salts are random — same plaintext yields different hashes
        assertNotEquals(hash1, hash2);
        // But both verify correctly
        assertTrue(BCrypt.checkpw(plain, hash1));
        assertTrue(BCrypt.checkpw(plain, hash2));
    }

    // --- InMemoryRevocationStore ---

    @Test
    void revocationStore_expiredEntry_notRevoked() throws InterruptedException {
        revocationStore.revoke("jti-test", 1); // 1 second TTL
        assertTrue(revocationStore.isRevoked("jti-test"));
        Thread.sleep(1100);
        assertFalse(revocationStore.isRevoked("jti-test"), "Entry should have expired");
    }

    @Test
    void revocationStore_unknownJti_notRevoked() {
        assertFalse(revocationStore.isRevoked("unknown-jti"));
    }

    @Test
    void revocationStore_nullJti_notRevoked() {
        assertFalse(revocationStore.isRevoked(null));
    }
}
