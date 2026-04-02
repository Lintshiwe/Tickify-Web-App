package za.ac.tut.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class VerifyLogin2FAServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        String expectedCode = asString(session.getAttribute("pending2faCode"));
        Long expiresAt = asLong(session.getAttribute("pending2faExpiresAt"));
        Integer attempts = asInt(session.getAttribute("pending2faAttempts"));

        if (expectedCode == null || expiresAt == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        if (System.currentTimeMillis() > expiresAt) {
            clearPending(session);
            response.sendRedirect(request.getContextPath() + "/TwoFactor.jsp?err=CodeExpired");
            return;
        }

        String providedCode = request.getParameter("code");
        int nextAttempts = (attempts == null ? 0 : attempts) + 1;
        session.setAttribute("pending2faAttempts", nextAttempts);

        if (providedCode == null || !expectedCode.equals(providedCode.trim())) {
            if (nextAttempts >= 5) {
                clearPending(session);
                response.sendRedirect(request.getContextPath() + "/Login.jsp?err=TwoFactorLocked");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/TwoFactor.jsp?err=CodeInvalid");
            return;
        }

        session.setAttribute("userEmail", session.getAttribute("pending2faEmail"));
        session.setAttribute("userRole", session.getAttribute("pending2faRole"));
        session.setAttribute("userID", session.getAttribute("pending2faUserId"));
        session.setAttribute("userFullName", session.getAttribute("pending2faDisplayName"));
        session.setAttribute("userLegalName", session.getAttribute("pending2faFullName"));
        session.setAttribute("userLoginId", session.getAttribute("pending2faLoginId"));
        session.setAttribute("userCampusName", session.getAttribute("pending2faCampusName"));
        session.setAttribute("userRoleNumberLabel", session.getAttribute("pending2faRoleNumberLabel"));

        String role = asString(session.getAttribute("pending2faRole"));
        clearPending(session);

        if ("ADMIN".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/AdminDashboard.do");
            return;
        }
        if ("EVENT_MANAGER".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do");
            return;
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/Login.jsp");
    }

    private void clearPending(HttpSession session) {
        session.removeAttribute("pending2faRole");
        session.removeAttribute("pending2faUserId");
        session.removeAttribute("pending2faDisplayName");
        session.removeAttribute("pending2faFullName");
        session.removeAttribute("pending2faEmail");
        session.removeAttribute("pending2faCampusName");
        session.removeAttribute("pending2faRoleNumberLabel");
        session.removeAttribute("pending2faLoginId");
        session.removeAttribute("pending2faCode");
        session.removeAttribute("pending2faExpiresAt");
        session.removeAttribute("pending2faAttempts");
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Integer asInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
