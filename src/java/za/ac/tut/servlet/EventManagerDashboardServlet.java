package za.ac.tut.servlet;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import za.ac.tut.application.eventmanager.EventManagerDashboardService;

@MultipartConfig(maxFileSize = 12 * 1024 * 1024)
public class EventManagerDashboardServlet extends HttpServlet {

    private final EventManagerDashboardService eventManagerService = new EventManagerDashboardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
                if (!hasManagerRole(request)) {
                        response.sendRedirect(request.getContextPath() + "/Login.jsp?err=AccessDenied");
                        return;
                }

        Object userIdObj = request.getSession().getAttribute("userID");
        int eventManagerId = userIdObj instanceof Integer ? (Integer) userIdObj : 0;

        if (eventManagerId <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        try {
            Map<String, Object> profile = eventManagerService.repo().getManagerProfile(eventManagerId);
            List<Map<String, Object>> assignedEvents = eventManagerService.repo().getAssignedEvents(eventManagerId);
            List<Map<String, Object>> guardCoverage = eventManagerService.repo().getVenueGuardCoverage(eventManagerId);
            List<Map<String, Object>> presenterSessions = eventManagerService.repo().getPresenterSessions(eventManagerId);

            int invalidScans24h = eventManagerService.repo().countInvalidScansLast24h(eventManagerId);
            int validScans24h = eventManagerService.repo().countValidScansLast24h(eventManagerId);
            int eventsWithoutTickets = eventManagerService.repo().countEventsWithoutTickets(eventManagerId);
            int guardsWithoutScans = eventManagerService.repo().countGuardsWithNoScans(eventManagerId);
            int presentersWithoutEvent = eventManagerService.repo().countPresentersWithoutMappedEvent(eventManagerId);

            request.setAttribute("managerProfile", profile);
            request.setAttribute("assignedEvents", assignedEvents);
            request.setAttribute("guardCoverage", guardCoverage);
            request.setAttribute("presenterSessions", presenterSessions);

            request.setAttribute("eventCount", assignedEvents.size());
            request.setAttribute("guardCount", guardCoverage.size());
            request.setAttribute("presenterCount", presenterSessions.size());
            request.setAttribute("invalidScans24h", invalidScans24h);
            request.setAttribute("validScans24h", validScans24h);

            request.setAttribute("planningItems", buildPlanningItems(eventsWithoutTickets, guardsWithoutScans, presentersWithoutEvent));
            request.setAttribute("riskItems", buildRiskItems(invalidScans24h, validScans24h));

            request.getRequestDispatcher("/EventManager/EventManagerDashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Failed to load Event Manager dashboard", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load Event Manager dashboard");
        }
    }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                if (!hasManagerRole(request)) {
                        response.sendRedirect(request.getContextPath() + "/Login.jsp?err=AccessDenied");
                        return;
                }

                Object userIdObj = request.getSession().getAttribute("userID");
                int eventManagerId = userIdObj instanceof Integer ? (Integer) userIdObj : 0;
                if (eventManagerId <= 0) {
                        response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
                        return;
                }

                String action = param(request, "action");
                try {
                        if ("createManagedEvent".equals(action)) {
                                String eventName = requireText(req(request, "eventName"), 3, 120, "InvalidEventName");
                                String eventType = requireText(req(request, "eventType"), 2, 80, "InvalidEventType");
                                String description = optionalText(param(request, "description"), 0, 1200, "InvalidEventDescription");
                                String infoUrl = optionalUrl(param(request, "infoUrl"), 255, "InvalidEventInfoUrl");
                                String status = parseEventStatus(param(request, "eventStatus"));
                                boolean ok = eventManagerService.repo().createEventForManager(
                                                eventManagerId,
                                                eventName,
                                                eventType,
                                                parseDateTimeLocal(req(request, "eventDate")),
                                                description,
                                                infoUrl,
                                                status
                                );
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?" + (ok ? "msg=EventCreated" : "err=CreateEventNotAllowed"));
                                return;
                        }

                        if ("updateAssignedEvent".equals(action)) {
                                String eventName = requireText(req(request, "eventName"), 3, 120, "InvalidEventName");
                                String eventType = requireText(req(request, "eventType"), 2, 80, "InvalidEventType");
                                String description = optionalText(param(request, "description"), 0, 1200, "InvalidEventDescription");
                                String infoUrl = optionalUrl(param(request, "infoUrl"), 255, "InvalidEventInfoUrl");
                                String status = parseEventStatus(param(request, "eventStatus"));
                                boolean ok = eventManagerService.repo().updateAssignedEventDetails(
                                                eventManagerId,
                                                parsePositiveInt(req(request, "eventID"), "InvalidEventId"),
                                                eventName,
                                                eventType,
                                                parseDateTimeLocal(req(request, "eventDate")),
                                                description,
                                                infoUrl,
                                                status
                                );
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=" + (ok ? "EventUpdated" : "NoChange"));
                                return;
                        }

                        if ("uploadAssignedEventAlbum".equals(action)) {
                                int eventId = parsePositiveInt(req(request, "eventID"), "InvalidEventId");
                                Part imagePart = request.getPart("eventAlbumImage");
                                if (imagePart == null || imagePart.getSize() == 0) {
                                        throw new IllegalArgumentException("MissingImage");
                                }
                                String contentType = imagePart.getContentType();
                                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                                        throw new IllegalArgumentException("InvalidImage");
                                }
                                byte[] imageBytes;
                                try {
                                        imageBytes = readBytes(imagePart.getInputStream());
                                } catch (IOException ex) {
                                        throw new IllegalArgumentException("ImageRead");
                                }
                                boolean ok = eventManagerService.repo().updateAssignedEventAlbumImage(
                                                eventManagerId,
                                                eventId,
                                                imagePart.getSubmittedFileName(),
                                                contentType,
                                                imageBytes
                                );
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=" + (ok ? "AlbumUploaded" : "NoChange"));
                                return;
                        }

                        if ("createTicketTier".equals(action)) {
                                String tierName = requireText(req(request, "tierName"), 2, 80, "InvalidTierName");
                                boolean ok = eventManagerService.repo().addTicketTierForAssignedEvent(
                                                eventManagerId,
                                                parsePositiveInt(req(request, "eventID"), "InvalidEventId"),
                                                tierName,
                                                parsePositivePrice(req(request, "tierPrice"), "InvalidTierPrice"),
                                                parsePositiveInt(req(request, "tierQuantity"), "InvalidTierQuantity")
                                );
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=" + (ok ? "TierCreated" : "NoChange"));
                                return;
                        }

                        if ("clearUnsoldTickets".equals(action)) {
                                int cleared = eventManagerService.repo().clearUnsoldTicketTemplatesForAssignedEvent(
                                                eventManagerId,
                                                parsePositiveInt(req(request, "eventID"), "InvalidEventId")
                                );
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=TicketsCleared&count=" + cleared);
                                return;
                        }

                        if ("createConcertPack".equals(action)) {
                                int eventId = parsePositiveInt(req(request, "eventID"), "InvalidEventId");
                                int total = 0;
                                if (eventManagerService.repo().addTicketTierForAssignedEvent(eventManagerId, eventId, "VIP Front Row", new BigDecimal("950.00"), 30)) {
                                        total += 30;
                                }
                                if (eventManagerService.repo().addTicketTierForAssignedEvent(eventManagerId, eventId, "Golden Circle", new BigDecimal("650.00"), 80)) {
                                        total += 80;
                                }
                                if (eventManagerService.repo().addTicketTierForAssignedEvent(eventManagerId, eventId, "General Access", new BigDecimal("350.00"), 250)) {
                                        total += 250;
                                }
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=ConcertPackCreated&count=" + total);
                                return;
                        }

                        if ("createConcertSeries".equals(action)) {
                                int total = eventManagerService.repo().createConcertSeriesForManager(eventManagerId);
                                response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?msg=ConcertSeriesCreated&count=" + total);
                                return;
                        }

                        response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?err=UnknownAction");
                } catch (IllegalArgumentException ex) {
                        response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?err=" + ex.getMessage());
                } catch (Exception ex) {
                        log("Manager dashboard update failed", ex);
                        response.sendRedirect(request.getContextPath() + "/EventManagerDashboard.do?err=OperationFailed");
                }
        }

    private List<Map<String, Object>> buildPlanningItems(int eventsWithoutTickets, int guardsWithoutScans,
            int presentersWithoutEvent) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildItem(
                eventsWithoutTickets > 0 ? "Ticket setup pending for " + eventsWithoutTickets + " event(s)."
                        : "All assigned events already have ticket templates.",
                eventsWithoutTickets > 0 ? "attention" : "ok"
        ));
        items.add(buildItem(
                guardsWithoutScans > 0 ? guardsWithoutScans + " guard profile(s) have no scan activity yet."
                        : "All guard profiles show scan activity.",
                guardsWithoutScans > 0 ? "attention" : "ok"
        ));
        items.add(buildItem(
                presentersWithoutEvent > 0 ? presentersWithoutEvent + " presenter profile(s) need event mapping."
                        : "All presenter profiles are mapped to events.",
                presentersWithoutEvent > 0 ? "attention" : "ok"
        ));
        return items;
    }

    private List<Map<String, Object>> buildRiskItems(int invalidScans24h, int validScans24h) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildItem("Invalid scans in last 24h: " + invalidScans24h,
                invalidScans24h > 0 ? "attention" : "ok"));
        items.add(buildItem("Valid scans in last 24h: " + validScans24h,
                "ok"));
        items.add(buildItem(
                invalidScans24h > 15
                        ? "Scanner rejection rate is elevated. Review gate operations immediately."
                        : "Scanner rejection rate is within expected range.",
                invalidScans24h > 15 ? "attention" : "ok"
        ));
        return items;
    }

    private Map<String, Object> buildItem(String text, String state) {
        Map<String, Object> row = new HashMap<>();
        row.put("text", text);
        row.put("state", state);
        return row;
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

        private int parseInt(String value) {
                return Integer.parseInt(value.trim());
        }

        private int parsePositiveInt(String value, String errCode) {
                try {
                        int parsed = Integer.parseInt(value.trim());
                        if (parsed <= 0) {
                                throw new IllegalArgumentException(errCode);
                        }
                        return parsed;
                } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(errCode);
                }
        }

        private BigDecimal parsePositivePrice(String value, String errCode) {
                try {
                        BigDecimal parsed = new BigDecimal(value.trim());
                        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                                throw new IllegalArgumentException(errCode);
                        }
                        return parsed;
                } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(errCode);
                }
        }

        private String requireText(String value, int min, int max, String errCode) {
                String clean = value == null ? "" : value.trim();
                if (clean.length() < min || clean.length() > max) {
                        throw new IllegalArgumentException(errCode);
                }
                return clean;
        }

        private String optionalText(String value, int min, int max, String errCode) {
                String clean = value == null ? "" : value.trim();
                if (clean.isEmpty()) {
                        return null;
                }
                if (clean.length() < min || clean.length() > max) {
                        throw new IllegalArgumentException(errCode);
                }
                return clean;
        }

        private String optionalUrl(String value, int max, String errCode) {
                String clean = value == null ? "" : value.trim();
                if (clean.isEmpty()) {
                        return null;
                }
                if (clean.length() > max) {
                        throw new IllegalArgumentException(errCode);
                }
                String lower = clean.toLowerCase();
                if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
                        throw new IllegalArgumentException(errCode);
                }
                return clean;
        }

        private String parseEventStatus(String value) {
                String normalized = value == null ? "" : value.trim().toUpperCase();
                if ("ACTIVE".equals(normalized) || "CANCELLED".equals(normalized) || "PASSED".equals(normalized)) {
                        return normalized;
                }
                throw new IllegalArgumentException("InvalidEventStatus");
        }

        private Timestamp parseDateTimeLocal(String value) {
                try {
                        LocalDateTime dateTime = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                        return Timestamp.valueOf(dateTime);
                } catch (DateTimeParseException ex) {
                        throw new IllegalArgumentException("InvalidEventDate");
                }
        }

        private byte[] readBytes(InputStream input) throws IOException {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                }
                return output.toByteArray();
        }

        private boolean hasManagerRole(HttpServletRequest request) {
                Object roleObj = request.getSession().getAttribute("userRole");
                if (!(roleObj instanceof String)) {
                        return false;
                }
                String normalized = ((String) roleObj).trim().toUpperCase().replace('-', '_').replace(' ', '_');
                return "EVENT_MANAGER".equals(normalized);
        }
}
