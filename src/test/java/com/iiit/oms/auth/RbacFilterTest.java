package com.iiit.oms.auth;

import com.iiit.oms.filter.RbacFilter;
import com.iiit.oms.model.User;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests RBAC enforcement without a live HTTP server, using a mock HttpExchange.
 *
 * Note: if Mockito is not on the classpath these tests compile but are skipped.
 * The important assertions are covered by JwtServiceTest + integration smoke tests.
 */
class RbacFilterTest {

    private JwtService jwtService;
    private RbacFilter rbacFilter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new InMemoryRevocationStore());
        rbacFilter = new RbacFilter(jwtService);
    }

    private User makeUser(String role) {
        User u = new User();
        u.setUserID("USR001");
        u.setUsername("testuser");
        u.setRole(role);
        return u;
    }

    // Helper: build a valid JWT for the given role
    private String tokenFor(String role) {
        return jwtService.generateToken(makeUser(role));
    }

    // --- Role access matrix: verify tokens decode and roles are correct ---

    @Test
    void investorToken_hasRoleINVESTOR() {
        String token = tokenFor("INVESTOR");
        var session = jwtService.validateAndExtract(token);
        assertNotNull(session);
        assertEquals("INVESTOR", session.getUser().getRole());
    }

    @Test
    void advisorToken_hasRoleADVISOR() {
        String token = tokenFor("ADVISOR");
        var session = jwtService.validateAndExtract(token);
        assertNotNull(session);
        assertEquals("ADVISOR", session.getUser().getRole());
    }

    @Test
    void adminToken_hasRoleADMIN() {
        String token = tokenFor("ADMIN");
        var session = jwtService.validateAndExtract(token);
        assertNotNull(session);
        assertEquals("ADMIN", session.getUser().getRole());
    }

    // --- Revoked token is always rejected ---

    @Test
    void revokedToken_notValidatedAsAnyRole() {
        String token = tokenFor("ADMIN");
        jwtService.revokeToken(token);
        assertNull(jwtService.validateAndExtract(token));
    }

    // --- Public path: /auth/login requires no auth ---

    @Test
    void authLoginPath_isPublic_noTokenNeeded() throws IOException {
        StubExchange exchange = new StubExchange("POST", "/auth/login", null);
        assertTrue(rbacFilter.checkAccess(exchange),
                "/auth/login should be accessible without a token");
    }

    // --- Token-based access checks ---

    @Test
    void protectedPath_noAuthHeader_returns401() throws IOException {
        StubExchange exchange = new StubExchange("GET", "/orders", null);
        assertFalse(rbacFilter.checkAccess(exchange));
        assertEquals(401, exchange.sentStatus);
    }

    @Test
    void protectedPath_invalidToken_returns401() throws IOException {
        StubExchange exchange = new StubExchange("GET", "/orders", "Bearer invalid.token.here");
        assertFalse(rbacFilter.checkAccess(exchange));
        assertEquals(401, exchange.sentStatus);
    }

    @Test
    void investorToken_getOrders_isAllowed() throws IOException {
        StubExchange exchange = new StubExchange("GET", "/orders", "Bearer " + tokenFor("INVESTOR"));
        assertTrue(rbacFilter.checkAccess(exchange));
    }

    @Test
    void investorToken_postConfirm_returns403() throws IOException {
        StubExchange exchange = new StubExchange("POST", "/orders/confirm", "Bearer " + tokenFor("INVESTOR"));
        assertFalse(rbacFilter.checkAccess(exchange));
        assertEquals(403, exchange.sentStatus);
    }

    @Test
    void adminToken_postConfirm_isAllowed() throws IOException {
        StubExchange exchange = new StubExchange("POST", "/orders/confirm", "Bearer " + tokenFor("ADMIN"));
        assertTrue(rbacFilter.checkAccess(exchange));
    }

    @Test
    void advisorToken_getAdvisorMe_isAllowed() throws IOException {
        StubExchange exchange = new StubExchange("GET", "/advisor/me", "Bearer " + tokenFor("ADVISOR"));
        assertTrue(rbacFilter.checkAccess(exchange));
    }

    @Test
    void investorToken_getAdvisorMe_returns403() throws IOException {
        StubExchange exchange = new StubExchange("GET", "/advisor/me", "Bearer " + tokenFor("INVESTOR"));
        assertFalse(rbacFilter.checkAccess(exchange));
        assertEquals(403, exchange.sentStatus);
    }

    // --- Minimal stub HttpExchange ---

    static class StubExchange extends HttpExchange {
        private final String method;
        private final URI uri;
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        int sentStatus = -1;

        StubExchange(String method, String path, String authHeader) {
            this.method = method;
            try { this.uri = new URI(path); } catch (Exception e) { throw new RuntimeException(e); }
            if (authHeader != null) requestHeaders.set("Authorization", authHeader);
        }

        @Override public String getRequestMethod()       { return method; }
        @Override public URI getRequestURI()             { return uri; }
        @Override public Headers getRequestHeaders()     { return requestHeaders; }
        @Override public Headers getResponseHeaders()    { return responseHeaders; }
        @Override public java.io.OutputStream getResponseBody() { return responseBody; }
        @Override public void sendResponseHeaders(int status, long len) { this.sentStatus = status; }
        @Override public void close() {}

        // Unused stubs
        @Override public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
        @Override public InetSocketAddress getRemoteAddress()     { return null; }
        @Override public InetSocketAddress getLocalAddress()      { return null; }
        @Override public String getProtocol()                     { return "HTTP/1.1"; }
        @Override public Object getAttribute(String name)         { return null; }
        @Override public void setAttribute(String name, Object v) {}
        @Override public void setStreams(InputStream i, java.io.OutputStream o) {}
        @Override public InputStream getRequestBody()             { return InputStream.nullInputStream(); }
        @Override public HttpPrincipal getPrincipal()             { return null; }
        @Override public int getResponseCode()                    { return sentStatus; }
    }
}
