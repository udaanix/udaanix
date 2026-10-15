package com.udaanix.siya.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Adds browser-side security headers to every SIYA API response. */
@WebFilter(filterName="SiyaSecurityHeadersFilter", urlPatterns="/api/siya/*")
public class SecurityHeadersFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse r = (HttpServletResponse) response;
        r.setHeader("X-Content-Type-Options", "nosniff");
        r.setHeader("X-Frame-Options", "DENY");
        r.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        r.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        r.setHeader("Cache-Control", "no-store");
        r.setHeader("Pragma", "no-cache");
        String csp = SecurityUtil.env("SIYA_CSP", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        r.setHeader("Content-Security-Policy", csp);
        chain.doFilter(request, response);
    }
}
