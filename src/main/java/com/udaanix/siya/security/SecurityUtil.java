package com.udaanix.siya.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Central security helpers for SIYA. Production mode requires an authenticated Udaanix session. */
public final class SecurityUtil {
    private SecurityUtil() {}

    public static boolean productionMode() {
        return Boolean.parseBoolean(env("SIYA_REQUIRE_AUTH", "false"));
    }

    public static String currentUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return "";
        Object id = s.getAttribute("udaanixUserId");
        if (id == null) id = s.getAttribute("userId");
        if (id == null) id = s.getAttribute("studentId");
        return id == null ? "" : String.valueOf(id).trim();
    }

    public static String currentRole(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return "";
        Object role = s.getAttribute("udaanixRole");
        if (role == null) role = s.getAttribute("role");
        return role == null ? "" : String.valueOf(role).trim().toUpperCase();
    }

    public static String resolveStudentId(HttpServletRequest req, String requestedId) {
        String sessionId = currentUserId(req);
        if (!sessionId.isBlank()) return sessionId;
        if (productionMode()) throw new SecurityException("Please log in to Udaanix.");
        return requestedId == null ? "" : requestedId.trim();
    }

    public static void requireStudent(HttpServletRequest req, String requestedId) {
        String sessionId = currentUserId(req);
        if (!sessionId.isBlank()) {
            if (requestedId != null && !requestedId.isBlank() && !sessionId.equals(requestedId.trim()))
                throw new SecurityException("Student identity does not match the active session.");
            return;
        }
        if (productionMode()) throw new SecurityException("Please log in to Udaanix.");
    }

    public static void requireRole(HttpServletRequest req, String... roles) {
        String role = currentRole(req);
        if (!role.isBlank()) {
            for (String allowed : roles) if (role.equalsIgnoreCase(allowed)) return;
            throw new SecurityException("You are not authorized for this resource.");
        }
        if (productionMode()) throw new SecurityException("Authenticated administrator access is required.");
    }

    public static String clientIp(HttpServletRequest req) {
        return req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
    }

    public static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
