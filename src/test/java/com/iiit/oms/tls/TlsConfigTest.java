package com.iiit.oms.tls;

import com.iiit.oms.db.inmemory.InMemoryOrderDatabase;
import com.iiit.oms.interfaces.OrderRestServer;
import com.iiit.oms.repository.OrderRepository;
import com.iiit.oms.repository.inmemory.InMemoryOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the server starts on HTTPS when a keystore is present,
 * enforces TLS 1.2+, and rejects TLS 1.0/1.1 connections.
 */
class TlsConfigTest {

    private static final int TEST_PORT = 18443;
    private static final String KEYSTORE_PATH = "src/main/resources/keystore.jks";
    private static final String KEYSTORE_PASS = "changeit";

    private OrderRestServer server;

    @BeforeEach
    void startServer() throws Exception {
        System.setProperty("OMS_KEYSTORE_PATH", KEYSTORE_PATH);
        System.setProperty("OMS_KEYSTORE_PASS", KEYSTORE_PASS);
        // env vars aren't settable at runtime via System.setProperty for getenv(),
        // so we set them here as system properties and rely on the fact that
        // the test env already has the keystore at the expected path.
        OrderRepository repo = new InMemoryOrderRepository(new InMemoryOrderDatabase());
        server = new OrderRestServer(TEST_PORT, repo);
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    // --- TLS connectivity ---

    @Test
    void httpsEndpoint_respondsOn_testPort() throws Exception {
        SSLContext ctx = buildTrustAllSslContext();
        HttpsURLConnection conn = openHttps("https://localhost:" + TEST_PORT + "/auth/login", ctx);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        // /auth/login with GET → 405 (only POST supported), but importantly NOT a connection failure
        assertTrue(code > 0, "Expected a valid HTTP status from HTTPS endpoint, got: " + code);
    }

    @Test
    void httpsEndpoint_tlsHandshake_succeeds() throws Exception {
        SSLContext ctx = buildTrustAllSslContext();
        SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", TEST_PORT);
        socket.startHandshake();
        assertNotNull(socket.getSession().getCipherSuite(),
                "Should have negotiated a cipher suite (TLS handshake succeeded)");
        socket.close();
    }

    @Test
    void httpsEndpoint_negotiatedProtocol_isTls12orTls13() throws Exception {
        SSLContext ctx = buildTrustAllSslContext();
        SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", TEST_PORT);
        socket.startHandshake();
        String proto = socket.getSession().getProtocol();
        socket.close();
        assertTrue(proto.equals("TLSv1.2") || proto.equals("TLSv1.3"),
                "Expected TLSv1.2 or TLSv1.3, got: " + proto);
    }

    @Test
    void tls10Connection_isRejected() {
        assertThrows(Exception.class, () -> {
            SSLContext ctx = SSLContext.getInstance("TLSv1");
            ctx.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            SSLSocketFactory factory = ctx.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket("localhost", TEST_PORT);
            socket.setEnabledProtocols(new String[]{"TLSv1"});
            // Should throw because server disables TLS 1.0
            socket.startHandshake();
        }, "TLS 1.0 connection should be rejected by server");
    }

    @Test
    void tls11Connection_isRejected() {
        assertThrows(Exception.class, () -> {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", TEST_PORT);
            socket.setEnabledProtocols(new String[]{"TLSv1.1"});
            socket.startHandshake();
        }, "TLS 1.1 connection should be rejected by server");
    }

    @Test
    void tls12Connection_succeeds() throws Exception {
        SSLContext ctx = buildTrustAllSslContext();
        SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", TEST_PORT);
        socket.setEnabledProtocols(new String[]{"TLSv1.2"});
        socket.startHandshake(); // must not throw
        assertEquals("TLSv1.2", socket.getSession().getProtocol());
        socket.close();
    }

    @Test
    void tls13Connection_succeeds() throws Exception {
        SSLContext ctx = buildTrustAllSslContext();
        SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", TEST_PORT);
        socket.setEnabledProtocols(new String[]{"TLSv1.3"});
        socket.startHandshake(); // must not throw
        assertEquals("TLSv1.3", socket.getSession().getProtocol());
        socket.close();
    }

    // --- Vite proxy config sanity check (static file content) ---

    @Test
    void viteProxyConfig_frontendPointsToHttps() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("frontend/vite.config.js")));
        assertTrue(content.contains("https://localhost"),
                "frontend/vite.config.js should proxy to https://localhost");
        assertTrue(content.contains("secure: false"),
                "frontend/vite.config.js should set secure:false for self-signed cert");
    }

    @Test
    void viteProxyConfig_userFrontendPointsToHttps() throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("user-frontend/vite.config.js")));
        assertTrue(content.contains("https://localhost"),
                "user-frontend/vite.config.js should proxy to https://localhost");
        assertTrue(content.contains("secure: false"),
                "user-frontend/vite.config.js should set secure:false for self-signed cert");
    }

    // --- Helpers ---

    private static HttpsURLConnection openHttps(String url, SSLContext ctx) throws Exception {
        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
        conn.setSSLSocketFactory(ctx.getSocketFactory());
        conn.setHostnameVerifier((h, s) -> true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        return conn;
    }

    private static SSLContext buildTrustAllSslContext() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
        return ctx;
    }

    static class TrustAllManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] c, String a) {}
        @Override public void checkServerTrusted(X509Certificate[] c, String a) {}
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
}
