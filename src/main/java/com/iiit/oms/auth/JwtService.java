package com.iiit.oms.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.iiit.oms.model.User;
import com.iiit.oms.model.UserSession;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public class JwtService {

    private static final Logger LOGGER = Logger.getLogger(JwtService.class.getName());
    private static final long TTL_MS = 24L * 60 * 60 * 1000; // 24 hours
    private static final String CLAIM_ROLE = "role";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final RevocationStore revocationStore;

    public JwtService(RevocationStore revocationStore) {
        String secret = System.getenv("OMS_JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = "oms-dev-secret-change-in-production";
            LOGGER.warning("OMS_JWT_SECRET not set — using insecure default. Set this env var in production.");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.revocationStore = revocationStore;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + TTL_MS);
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(user.getUserID())
                .withClaim(CLAIM_ROLE, user.getRole())
                .withClaim("username", user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    /**
     * Validates the token signature, expiry, and revocation list.
     * Returns null if invalid for any reason.
     */
    public UserSession validateAndExtract(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            DecodedJWT decoded = verifier.verify(token);
            String jti = decoded.getId();
            if (revocationStore.isRevoked(jti)) {
                LOGGER.fine("JWT jti=" + jti + " is revoked");
                return null;
            }
            User user = new User();
            user.setUserID(decoded.getSubject());
            user.setRole(decoded.getClaim(CLAIM_ROLE).asString());
            user.setUsername(decoded.getClaim("username").asString());
            return new UserSession(token, user);
        } catch (JWTVerificationException ex) {
            LOGGER.fine("JWT validation failed: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Revokes the token by blacklisting its jti for its remaining lifetime.
     * Safe to call with an already-expired or invalid token.
     */
    public void revokeToken(String token) {
        try {
            DecodedJWT decoded = JWT.decode(token);
            String jti = decoded.getId();
            long ttl = remainingTtlSeconds(token);
            if (jti != null && ttl > 0) {
                revocationStore.revoke(jti, ttl);
            }
        } catch (Exception ex) {
            LOGGER.fine("Could not revoke token: " + ex.getMessage());
        }
    }

    /**
     * Extracts the jti from a token without full verification.
     * Used during logout to revoke before verifying expiry.
     */
    public String extractJti(String token) {
        try {
            return JWT.decode(token).getId();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Remaining lifetime of the token in seconds, or 0 if expired/invalid.
     */
    public long remainingTtlSeconds(String token) {
        try {
            DecodedJWT decoded = JWT.decode(token);
            long expMs = decoded.getExpiresAt().getTime();
            long remaining = (expMs - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception ex) {
            return 0;
        }
    }
}
