package com.udaanix.siya.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Lightweight per-IP fixed-window limiter for SIYA API endpoints. Use a shared gateway limiter for multi-node production. */
@WebFilter(filterName="SiyaRateLimitFilter", urlPatterns="/api/siya/*")
public class RateLimitFilter implements Filter {
    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<>();
    private final int limit = Integer.parseInt(SecurityUtil.env("SIYA_RATE_LIMIT_PER_MINUTE", "30"));
    private static final class Window { final long start; final AtomicInteger count = new AtomicInteger(); Window(long s){start=s;} }

    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request;
        HttpServletResponse resp=(HttpServletResponse)response;
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) { chain.doFilter(request,response); return; }
        String key=SecurityUtil.clientIp(req);
        long now=System.currentTimeMillis();
        Window w=WINDOWS.compute(key,(k,old)-> old==null || now-old.start>=60_000 ? new Window(now) : old);
        int n=w.count.incrementAndGet();
        resp.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        if(n>limit){
            resp.setStatus(429); resp.setHeader("Retry-After","60"); resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"error\":\"Too many SIYA requests. Please try again shortly.\"}"); return;
        }
        chain.doFilter(request,response);
    }
}
