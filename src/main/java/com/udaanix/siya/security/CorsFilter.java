package com.udaanix.siya.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Same-origin by default; optionally allow a small explicit origin list. */
@WebFilter(filterName="SiyaCorsFilter", urlPatterns="/api/siya/*")
public class CorsFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request;
        HttpServletResponse resp=(HttpServletResponse)response;
        String origin=req.getHeader("Origin");
        String allowed=SecurityUtil.env("SIYA_ALLOWED_ORIGINS", "").trim();
        if(origin!=null && !origin.isBlank()){
            boolean ok=false;
            for(String candidate: allowed.split(",")) if(origin.equals(candidate.trim())) {ok=true;break;}
            if(!ok){resp.setStatus(403);resp.setContentType("application/json;charset=UTF-8");resp.getWriter().write("{\"success\":false,\"error\":\"Origin is not allowed.\"}");return;}
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary","Origin");
            resp.setHeader("Access-Control-Allow-Credentials","true");
            resp.setHeader("Access-Control-Allow-Headers","Content-Type, X-Requested-With");
            resp.setHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS");
        }
        if("OPTIONS".equalsIgnoreCase(req.getMethod())) {resp.setStatus(204);return;}
        chain.doFilter(request,response);
    }
}
