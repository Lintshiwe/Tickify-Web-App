/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.presenter.TertiaryPresenterService;

/**
 *
 * @author ntoam
 */
public class TertiaryPresenterDashboard extends HttpServlet {

    private final TertiaryPresenterService presenterService = new TertiaryPresenterService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object userIdObj = request.getSession().getAttribute("userID");
        int presenterId = userIdObj instanceof Integer ? (Integer) userIdObj : 0;
        if (presenterId <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        try {
            Map<String, Object> presenterProfile = presenterService.repo().getDashboardProfile(presenterId);
            Map<String, Object> eventSnapshot = presenterService.repo().getEventSnapshot(presenterId);
            List<Map<String, Object>> managerContacts = presenterService.repo().getPresenterTeamContacts(presenterId);
            List<Map<String, Object>> guardContacts = presenterService.repo().getVenueGuardContacts(presenterId);
            List<Map<String, Object>> peerPresenters = presenterService.repo().getPeerPresentersAtVenue(presenterId);
            List<Map<String, Object>> materials = presenterService.repo().getPresenterMaterials(presenterId);
            List<Map<String, Object>> scheduleItems = presenterService.repo().getPresenterScheduleItems(presenterId);
            List<Map<String, Object>> announcements = presenterService.repo().getPresenterAnnouncements(presenterId);
            List<Map<String, Object>> attendeeList = presenterService.repo().getEventAttendeesForPresenter(presenterId);

            request.setAttribute("presenterProfile", presenterProfile);
            request.setAttribute("eventSnapshot", eventSnapshot);
            request.setAttribute("managerContacts", managerContacts);
            request.setAttribute("guardContacts", guardContacts);
            request.setAttribute("peerPresenters", peerPresenters);
            request.setAttribute("materials", materials);
            request.setAttribute("scheduleItems", scheduleItems);
            request.setAttribute("announcements", announcements);
            request.setAttribute("attendeeList", attendeeList);

            request.getRequestDispatcher("/Presenter/PresenterDashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Failed to load presenter dashboard", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load presenter dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object userIdObj = request.getSession().getAttribute("userID");
        int presenterId = userIdObj instanceof Integer ? (Integer) userIdObj : 0;
        if (presenterId <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        String action = param(request, "action");
        try {
            if ("addMaterial".equals(action)) {
                String title = requireText(req(request, "title"), 3, 120, "InvalidMaterialTitle");
                String materialUrl = sanitizeOptionalUrl(param(request, "materialUrl"), "InvalidMaterialUrl");
                String description = sanitizeOptionalText(param(request, "description"), 0, 500, "InvalidMaterialDescription");
                boolean ok = presenterService.repo().addPresenterMaterial(
                        presenterId,
                        title,
                        materialUrl,
                        description
                );
                response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?msg=" + (ok ? "MaterialAdded" : "NoChange"));
                return;
            }

            if ("addScheduleItem".equals(action)) {
                String title = requireText(req(request, "title"), 3, 120, "InvalidScheduleTitle");
                Timestamp startsAt = parseDateTimeLocal(req(request, "startsAt"));
                Timestamp endsAt = parseOptionalDateTimeLocal(param(request, "endsAt"));
                if (endsAt != null && endsAt.before(startsAt)) {
                    throw new IllegalArgumentException("InvalidScheduleRange");
                }
                boolean ok = presenterService.repo().addPresenterScheduleItem(
                        presenterId,
                        title,
                        startsAt,
                        endsAt,
                        sanitizeOptionalText(param(request, "room"), 0, 80, "InvalidScheduleRoom"),
                        sanitizeOptionalText(param(request, "notes"), 0, 500, "InvalidScheduleNotes")
                );
                response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?msg=" + (ok ? "ScheduleAdded" : "NoChange"));
                return;
            }

            if ("addAnnouncement".equals(action)) {
                String title = requireText(req(request, "title"), 3, 120, "InvalidAnnouncementTitle");
                String body = requireText(req(request, "body"), 8, 1000, "InvalidAnnouncementBody");
                boolean ok = presenterService.repo().addPresenterAnnouncement(
                        presenterId,
                        title,
                        body
                );
                response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?msg=" + (ok ? "AnnouncementAdded" : "NoChange"));
                return;
            }

            response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?err=UnknownAction");
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?err=" + ex.getMessage());
        } catch (Exception ex) {
            log("Failed to update presenter dashboard", ex);
            response.sendRedirect(request.getContextPath() + "/TertiaryPresenterDashboard.do?err=OperationFailed");
        }
    }

    @Override
    public String getServletInfo() {
        return "Routes presenter users to dashboard view";
    }

    private String param(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? null : value.trim();
    }

    private String req(HttpServletRequest request, String key) {
        String value = param(request, key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return value;
    }

    private Timestamp parseDateTimeLocal(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("InvalidScheduleDate");
        }
    }

    private Timestamp parseOptionalDateTimeLocal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return parseDateTimeLocal(value);
    }

    private String requireText(String value, int min, int max, String errCode) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() < min || clean.length() > max) {
            throw new IllegalArgumentException(errCode);
        }
        return clean;
    }

    private String sanitizeOptionalText(String value, int min, int max, String errCode) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requireText(value, min, max, errCode);
    }

    private String sanitizeOptionalUrl(String value, String errCode) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim();
        String lower = clean.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
            throw new IllegalArgumentException(errCode);
        }
        if (clean.length() > 300) {
            throw new IllegalArgumentException(errCode);
        }
        return clean;
    }

}
