package za.ac.tut.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthzFilter implements Filter {

    private static final Set<String> SCANNER_ALLOWED_ROLES = new HashSet<>(Arrays.asList(
            "VENUE_GUARD",
            "ADMIN"
    ));

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/index.html",
            "/Login.jsp",
            "/UserSelection.jsp",
            "/UserSignUp.jsp",
            "/TwoFactor.jsp",
            "/ClientPasswordReset.jsp",
            "/LoginServlet.do",
            "/VerifyLogin2FA.do",
            "/RegistrationServlet.do",
            "/ClientPasswordReset.do",
                "/VerifyEmail.do",
            "/SessionHeartbeat.do",
            "/LogoutServlet.do",
            "/AdvertImage.do",
            "/Unsubscribe.do"
    ));

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = normalizeUri(req.getRequestURI(), req.getContextPath());
        if (isPublic(uri)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        String userRole = session != null ? (String) session.getAttribute("userRole") : null;
        if (userRole == null) {
            resp.sendRedirect(req.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        String normalizedUserRole = canonicalRole(userRole);
        String requiredRole = resolveRequiredRole(uri);

        if ("/ValidateTicketServlet.do".equals(uri) && !SCANNER_ALLOWED_ROLES.contains(normalizedUserRole)) {
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/Login.jsp?err=AccessDenied");
            return;
        }

        if (requiredRole != null && !requiredRole.equals(normalizedUserRole)) {
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/Login.jsp?err=AccessDenied");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isPublic(String uri) {
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }
        // Support proxy/context variations where request URI may still include app prefix.
        for (String publicPath : PUBLIC_PATHS) {
            if (uri.endsWith(publicPath)) {
                return true;
            }
        }
        return uri.startsWith("/javax.faces.resource/") || uri.startsWith("/resources/");
    }

    private String normalizeUri(String requestUri, String contextPath) {
        String uri = requestUri == null ? "/" : requestUri;

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        int pathParamStart = uri.indexOf(';');
        if (pathParamStart >= 0) {
            uri = uri.substring(0, pathParamStart);
        }

        return uri.isEmpty() ? "/" : uri;
    }

    private String resolveRequiredRole(String uri) {
        if (uri.startsWith("/Admin/")) {
            return "ADMIN";
        }
        if (uri.startsWith("/Attendee/") || uri.equals("/AttendeeDashboardServlet.do")
                || uri.equals("/ViewMyTickets.do") || uri.equals("/AttendeeViewProfileServlet.do")
                || uri.equals("/AttendeeDeleteProfileServlet.do") || uri.equals("/BookTicket.do")
                || uri.equals("/MyOrderHistory.do")
                || uri.equals("/Wishlist.do") || uri.equals("/Checkout.do")
                || uri.equals("/PaymentGateway.do") || uri.equals("/AttendeeSubscription.do")
                || uri.equals("/TicketDownload.do")) {
            return "ATTENDEE";
        }
        if (uri.equals("/AdminAdverts.do")) {
            return "ADMIN";
        }
        if (uri.equals("/AdminDashboard.do")) {
            return "ADMIN";
        }
        if (uri.equals("/AdminRoleConsole.do")) {
            return "ADMIN";
        }
        if (uri.equals("/AdminDatabase.do")) {
            return "ADMIN";
        }
        if (uri.equals("/AdminAlerts.do")) {
            return "ADMIN";
        }
        if (uri.equals("/AdminEventAlbum.do")) {
            return "ADMIN";
        }
        if (uri.startsWith("/Presenter/") || uri.equals("/TertiaryPresenterDashboard.do")) {
            return "TERTIARY_PRESENTER";
        }
        if (uri.startsWith("/EventManager/") || uri.equals("/EventManagerDashboard.do")) {
            return "EVENT_MANAGER";
        }
        if (uri.startsWith("/VenueGuard/")) {
            return "VENUE_GUARD";
        }
        if (uri.equals("/VenueGuardAttendees.do")) {
            return "VENUE_GUARD";
        }
        if (uri.equals("/ValidateTicketServlet.do")) {
            return "VENUE_GUARD";
        }
        return null;
    }

    private String canonicalRole(String role) {
        if (role == null) {
            return "";
        }
        // Normalize separators so values like "venue guard" and "venue-guard" map consistently.
        return role.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String homePathForRole(String role) {
        if ("ADMIN".equals(role)) {
            return "/AdminDashboard.do";
        }
        if ("ATTENDEE".equals(role)) {
            return "/AttendeeDashboardServlet.do";
        }
        if ("EVENT_MANAGER".equals(role)) {
            return "/EventManagerDashboard.do";
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            return "/Presenter/PresenterDashboard.jsp";
        }
        if ("VENUE_GUARD".equals(role)) {
            return "/VenueGuard/VenueGuardDashboard.jsp";
        }
        return "/Login.jsp";
    }
}