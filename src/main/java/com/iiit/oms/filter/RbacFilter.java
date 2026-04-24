package com.iiit.oms.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.auth.JwtService;
import com.iiit.oms.model.UserSession;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * HTTP filter that enforces role-based access control on every request.
 * Endpoints not listed in the access matrix are open (e.g. /auth/login).
 */
public class RbacFilter extends Filter {

    private static final Logger LOGGER = Logger.getLogger(RbacFilter.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum Role { INVESTOR, ADVISOR, ADMIN }

    // Path-prefix → set of roles allowed. Checked in declaration order (longest prefix first).
    private static final Map<String, Set<Role>> ACCESS_MAP = new LinkedAccessMap();

    static {
        // Admin-only endpoints
        ACCESS_MAP.put("POST:/orders/confirm",          EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("POST:/orders/book",             EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("GET:/accounts",                 EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("POST:/funds",                   EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("GET:/view/users",               EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("GET:/view/audit-archive",       EnumSet.of(Role.ADMIN));
        ACCESS_MAP.put("POST:/transfer-agent",          EnumSet.of(Role.ADMIN));

        // Investor + Advisor endpoints
        ACCESS_MAP.put("POST:/orders/plan",             EnumSet.of(Role.INVESTOR, Role.ADVISOR));
        ACCESS_MAP.put("POST:/orders/cancel",           EnumSet.of(Role.INVESTOR, Role.ADVISOR));

        // Advisor-only endpoints
        ACCESS_MAP.put("GET:/advisor",                  EnumSet.of(Role.ADVISOR));
        ACCESS_MAP.put("POST:/advisor",                 EnumSet.of(Role.ADVISOR));

        // Any authenticated user
        ACCESS_MAP.put("GET:/orders",                   EnumSet.allOf(Role.class));
        ACCESS_MAP.put("GET:/view",                     EnumSet.allOf(Role.class));
        ACCESS_MAP.put("GET:/funds",                    EnumSet.allOf(Role.class));
        ACCESS_MAP.put("GET:/auth/me",                  EnumSet.allOf(Role.class));
        ACCESS_MAP.put("POST:/auth/logout",             EnumSet.allOf(Role.class));
    }

    // Paths that require no authentication at all
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login"
    );

    private volatile JwtService jwtService;

    public RbacFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void setJwtService(JwtService jwtService) {
        if (jwtService != null) this.jwtService = jwtService;
    }

    @Override
    public String description() { return "RBAC role enforcement filter"; }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (checkAccess(exchange)) chain.doFilter(exchange);
    }

    /**
     * Returns true if the request is allowed, false if a 401/403 has already been sent.
     */
    public boolean checkAccess(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path   = exchange.getRequestURI().getPath();

        // Public paths skip auth entirely
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return true;

        // Resolve the required roles for this (method, path) combination
        Set<Role> required = resolveRequired(method, path);
        if (required == null) return true; // open endpoint

        // Authenticate
        String token = extractToken(exchange);
        if (token == null || token.isBlank()) {
            reject(exchange, 401, "Authentication required");
            return false;
        }
        UserSession session = jwtService.validateAndExtract(token);
        if (session == null) {
            reject(exchange, 401, "Invalid or expired token");
            return false;
        }

        // Authorise
        Role userRole;
        try {
            userRole = Role.valueOf(session.getUser().getRole());
        } catch (IllegalArgumentException ex) {
            reject(exchange, 403, "Unknown role: " + session.getUser().getRole());
            return false;
        }
        // ADMIN is a superuser — bypasses all role-specific restrictions
        if (userRole == Role.ADMIN) return true;

        if (!required.contains(userRole)) {
            LOGGER.warning("RBAC denied: user=" + session.getUser().getUsername()
                    + " role=" + userRole + " path=" + method + ":" + path);
            reject(exchange, 403, "Forbidden: role " + userRole + " cannot access this endpoint");
            return false;
        }

        return true;
    }

    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) return null;

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Set<Role> resolveRequired(String method, String path) {
        // Try method-specific match first, then method-wildcard
        String methodPath = method + ":" + path;
        for (Map.Entry<String, Set<Role>> entry : ACCESS_MAP.entrySet()) {
            String key = entry.getKey();
            if (methodPath.startsWith(key)) return entry.getValue();
        }
        // No match → treat as open
        return null;
    }

    private void reject(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = MAPPER.writeValueAsBytes(Map.of("message", message));
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    // Insertion-ordered map so longest-prefix rules are checked in declaration order
    private static class LinkedAccessMap extends java.util.LinkedHashMap<String, Set<Role>> {}
}
