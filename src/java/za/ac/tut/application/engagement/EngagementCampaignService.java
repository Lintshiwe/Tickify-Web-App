package za.ac.tut.application.engagement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import za.ac.tut.databaseManagement.EngagementDAO;
import za.ac.tut.notification.EmailService;

public class EngagementCampaignService {

    private final EngagementDAO engagementDAO = new EngagementDAO();
    private final EmailService emailService = new EmailService();

    public Map<String, Object> getAttendeeSnapshot(int attendeeId) throws SQLException {
        return engagementDAO.getAttendeeEngagementSnapshot(attendeeId);
    }

    public boolean setSubscription(int attendeeId, boolean subscribed) throws SQLException {
        return engagementDAO.setAttendeeSubscription(attendeeId, subscribed);
    }

    public boolean unsubscribeByToken(String token) throws SQLException {
        return engagementDAO.unsubscribeByToken(token);
    }

    public void notifySubscribersForNewEvent(HttpServletRequest request, int eventId) {
        try {
            Map<String, Object> eventPayload = engagementDAO.getEventCampaignPayload(eventId);
            if (eventPayload.isEmpty()) {
                return;
            }

            List<Map<String, Object>> adverts = engagementDAO.getFeaturedCampaignAdverts(2);
            List<Map<String, Object>> subscribers = engagementDAO.getActiveSubscribers();
            String appBaseUrl = resolveAppBaseUrl(request) + request.getContextPath();
            String subject = "New Tickify Event: " + safe(eventPayload.get("name"));

            for (Map<String, Object> subscriber : subscribers) {
                Integer attendeeId = intOrNull(subscriber.get("attendeeID"));
                String email = safe(subscriber.get("email"));
                String token = safe(subscriber.get("unsubscribeToken"));
                if (email.isEmpty() || token.isEmpty() || attendeeId == null) {
                    continue;
                }

                Map<String, Object> snapshot = engagementDAO.getAttendeeEngagementSnapshot(attendeeId.intValue());
                String name = (safe(subscriber.get("firstname")) + " " + safe(subscriber.get("lastname"))).trim();
                String unsubscribeLink = appBaseUrl + "/Unsubscribe.do?token=" + token;

                try {
                    emailService.sendEventCampaignEmail(
                            email,
                            name,
                            subject,
                            eventPayload,
                            adverts,
                            unsubscribeLink,
                            safe(snapshot.get("couponCode")),
                            intOrNull(snapshot.get("couponPercent"))
                    );
                    engagementDAO.logCampaignResult("EVENT_UPDATE", attendeeId, Integer.valueOf(eventId), email, subject, "SENT");
                } catch (Exception ex) {
                    engagementDAO.logCampaignResult("EVENT_UPDATE", attendeeId, Integer.valueOf(eventId), email, subject, "FAILED");
                }
            }
        } catch (SQLException ex) {
            // Notification fan-out should not break the manager workflow.
        }
    }

    public void processWishlistLowStockAlerts(HttpServletRequest request, List<Integer> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return;
        }

        Set<Integer> unique = new HashSet<Integer>();
        for (Integer eventId : eventIds) {
            if (eventId != null && eventId.intValue() > 0) {
                unique.add(eventId);
            }
        }
        if (unique.isEmpty()) {
            return;
        }

        try {
            List<Integer> ids = new ArrayList<Integer>(unique);
            List<Map<String, Object>> recipients = engagementDAO.getWishlistLowStockCandidates(ids, 20, 6);
            String appBaseUrl = resolveAppBaseUrl(request) + request.getContextPath();

            for (Map<String, Object> row : recipients) {
                Integer attendeeId = intOrNull(row.get("attendeeID"));
                Integer eventId = intOrNull(row.get("eventID"));
                Integer remaining = intOrNull(row.get("remainingTickets"));
                String email = safe(row.get("email"));
                if (attendeeId == null || eventId == null || remaining == null || email.isEmpty()) {
                    continue;
                }

                try {
                    String eventLink = appBaseUrl + "/AttendeeDashboardServlet.do";
                    emailService.sendWishlistLowStockEmail(email,
                            (safe(row.get("firstname")) + " " + safe(row.get("lastname"))).trim(),
                            safe(row.get("eventName")),
                            safe(row.get("venueName")),
                            row.get("eventDate"),
                            remaining.intValue(),
                            eventLink);
                    engagementDAO.markWishlistStockAlertSent(attendeeId.intValue(), eventId.intValue(), remaining.intValue());
                    engagementDAO.logCampaignResult("WISHLIST_LOW_STOCK", attendeeId, eventId, email,
                            "Low stock alert: " + safe(row.get("eventName")), "SENT");
                } catch (Exception ex) {
                    engagementDAO.logCampaignResult("WISHLIST_LOW_STOCK", attendeeId, eventId, email,
                            "Low stock alert: " + safe(row.get("eventName")), "FAILED");
                }
            }
        } catch (SQLException ex) {
            // Best-effort alerting only.
        }
    }

    public Map<String, Object> refreshBuyerProgressAndReward(int attendeeId) throws SQLException {
        return engagementDAO.updateBadgeProgressAndIssueCoupon(attendeeId);
    }

    private Integer intOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        try {
            return Integer.valueOf(Integer.parseInt(String.valueOf(value).trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String resolveAppBaseUrl(HttpServletRequest request) {
        String configured = System.getProperty("tickify.app.baseUrl");
        if (configured != null) {
            configured = configured.trim();
            if (!configured.isEmpty()) {
                if (configured.endsWith("/")) {
                    return configured.substring(0, configured.length() - 1);
                }
                return configured;
            }
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }
}
