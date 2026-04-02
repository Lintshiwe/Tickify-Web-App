package za.ac.tut.security;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Permissions-Policy", "camera=(self), geolocation=(), microphone=(), payment=()");
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: https://api.qrserver.com; connect-src 'self'; frame-ancestors 'none';");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}