package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.attendee.AttendeeService;

public class WishlistServlet extends HttpServlet {

    private final AttendeeService attendeeService = new AttendeeService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean ajax = isAjax(request);
        try {
            HttpSession session = request.getSession(false);
            Integer attendeeId = session != null ? (Integer) session.getAttribute("userID") : null;
            if (attendeeId == null) {
                if (ajax) {
                    writeJson(response, false, false, "Session expired");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/Login.jsp");
                return;
            }

            String action = request.getParameter("action");
            String eventIdParam = request.getParameter("eventID");

            int eventId;
            try {
                eventId = Integer.parseInt(eventIdParam);
            } catch (NumberFormatException e) {
                if (ajax) {
                    writeJson(response, false, false, "Invalid event");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=InvalidEvent");
                return;
            }

            try {
                boolean ok;
                if ("remove".equalsIgnoreCase(action)) {
                    ok = attendeeService.repo().removeEventFromWishlist(attendeeId, eventId);
                    if (ajax) {
                        writeJson(response, ok, false, ok ? "Removed from wishlist" : "Wishlist update failed");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + (ok
                            ? "/AttendeeDashboardServlet.do?msg=WishlistRemoved"
                            : "/AttendeeDashboardServlet.do?err=WishlistFailed"));
                    return;
                }

                ok = attendeeService.repo().addEventToWishlist(attendeeId, eventId);
                if (ok) {
                    writeWishlistTrackingCookie(request, response);
                    if (ajax) {
                        writeJson(response, true, true, "Added to wishlist");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?msg=WishlistAdded");
                } else {
                    if (ajax) {
                        writeJson(response, false, false, "Wishlist update failed");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=WishlistFailed");
                }
            } catch (SQLException e) {
                log("Wishlist update failed", e);
                if (ajax) {
                    writeJson(response, false, false, "Wishlist update failed");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=WishlistFailed");
            }
        } catch (Exception e) {
            log("Unexpected wishlist processing error", e);
            if (ajax) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(response, false, false, "Unexpected server error");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=WishlistFailed");
        }
    }

    private void writeWishlistTrackingCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean consented = false;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("tk_cookie_consent".equals(cookie.getName()) && "yes".equals(cookie.getValue())) {
                    consented = true;
                    break;
                }
            }
        }

        if (!consented) {
            return;
        }

        Cookie activityCookie = new Cookie("tk_wishlist_last_action", Long.toString(System.currentTimeMillis()));
        activityCookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        activityCookie.setMaxAge(60 * 60 * 24 * 30);
        activityCookie.setHttpOnly(false);
        response.addCookie(activityCookie);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do");
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("application/json");
    }

    private void writeJson(HttpServletResponse response, boolean ok, boolean wishlisted, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String msg = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"ok\":" + ok
                + ",\"wishlisted\":" + wishlisted
                + ",\"message\":\"" + msg + "\"}");
    }
}
