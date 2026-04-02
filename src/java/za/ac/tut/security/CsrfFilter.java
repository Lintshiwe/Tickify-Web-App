package za.ac.tut.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

public class CsrfFilter implements Filter {

    private static final String CSRF_SESSION_KEY = "csrfToken";
    private static final String CSRF_PARAM = "_csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(true);
        String sessionToken = (String) session.getAttribute(CSRF_SESSION_KEY);
        if (sessionToken == null || sessionToken.isEmpty()) {
            sessionToken = generateToken();
            session.setAttribute(CSRF_SESSION_KEY, sessionToken);
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String requestToken = resolveRequestToken(req);
            if (requestToken == null || !sessionToken.equals(requestToken)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resolveRequestToken(HttpServletRequest req) {
        String token = req.getParameter(CSRF_PARAM);
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }

        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return token;
        }

        try {
            Part csrfPart = req.getPart(CSRF_PARAM);
            if (csrfPart == null || csrfPart.getSize() <= 0) {
                return token;
            }
            try (InputStream in = csrfPart.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[256];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                String partValue = out.toString("UTF-8").trim();
                return partValue.isEmpty() ? token : partValue;
            }
        } catch (Exception ex) {
            return token;
        }
    }
}