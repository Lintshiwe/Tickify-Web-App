package za.ac.tut.databaseManagement;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;

public class EngagementDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Map<String, Object> getAttendeeEngagementSnapshot(int attendeeId) throws SQLException {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("subscribed", false);
        snapshot.put("badgeLevel", "NEW");
        snapshot.put("badgeTitle", "New Buyer");
        snapshot.put("totalTickets", 0);
        snapshot.put("totalSpend", BigDecimal.ZERO);

        if (attendeeId <= 0) {
            return snapshot;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            Map<String, Object> sub = getSubscriptionRow(conn, attendeeId);
            if (!sub.isEmpty()) {
                snapshot.put("subscribed", Boolean.TRUE.equals(sub.get("subscribed")));
            }

            Map<String, Object> badge = getBadgeRow(conn, attendeeId);
            if (!badge.isEmpty()) {
                snapshot.put("badgeLevel", badge.get("badgeLevel"));
                snapshot.put("badgeTitle", badge.get("badgeTitle"));
                snapshot.put("totalTickets", badge.get("totalTickets"));
                snapshot.put("totalSpend", badge.get("totalSpend"));
            }

            Map<String, Object> coupon = getLatestActiveCoupon(conn, attendeeId);
            if (!coupon.isEmpty()) {
                snapshot.put("couponCode", coupon.get("couponCode"));
                snapshot.put("couponPercent", coupon.get("discountPercent"));
            }
        }

        return snapshot;
    }

    public boolean setAttendeeSubscription(int attendeeId, boolean subscribed) throws SQLException {
        if (attendeeId <= 0) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String email = attendeeEmail(conn, attendeeId);
            if (email == null || email.trim().isEmpty()) {
                return false;
            }

            Map<String, Object> row = getSubscriptionRow(conn, attendeeId);
            if (row.isEmpty()) {
                String insertSql = "INSERT INTO attendee_subscription(attendeeID, email, subscribed, unsubscribeToken, subscribedAt, unsubscribedAt, lastCampaignAt) "
                        + "VALUES(?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, attendeeId);
                    ps.setString(2, email.trim().toLowerCase(Locale.ENGLISH));
                    ps.setBoolean(3, subscribed);
                    ps.setString(4, randomToken());
                    if (subscribed) {
                        ps.setTimestamp(5, Timestamp.from(Instant.now()));
                        ps.setTimestamp(6, null);
                    } else {
                        ps.setTimestamp(5, null);
                        ps.setTimestamp(6, Timestamp.from(Instant.now()));
                    }
                    ps.setTimestamp(7, null);
                    return ps.executeUpdate() > 0;
                }
            }

            String updateSql = "UPDATE attendee_subscription SET email = ?, subscribed = ?, "
                    + "subscribedAt = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE subscribedAt END, "
                    + "unsubscribedAt = CASE WHEN ? THEN unsubscribedAt ELSE CURRENT_TIMESTAMP END "
                    + "WHERE attendeeID = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, email.trim().toLowerCase(Locale.ENGLISH));
                ps.setBoolean(2, subscribed);
                ps.setBoolean(3, subscribed);
                ps.setBoolean(4, subscribed);
                ps.setInt(5, attendeeId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean unsubscribeByToken(String token) throws SQLException {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE attendee_subscription SET subscribed = FALSE, unsubscribedAt = CURRENT_TIMESTAMP "
                + "WHERE unsubscribeToken = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> getActiveSubscribers() throws SQLException {
        List<Map<String, Object>> subscribers = new ArrayList<>();
        String sql = "SELECT s.attendeeID, a.firstname, a.lastname, s.email, s.unsubscribeToken "
                + "FROM attendee_subscription s "
                + "JOIN attendee a ON a.attendeeID = s.attendeeID "
                + "WHERE s.subscribed = TRUE AND s.email IS NOT NULL AND TRIM(s.email) <> '' "
                + "ORDER BY s.attendeeID ASC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("attendeeID", rs.getInt("attendeeID"));
                row.put("firstname", rs.getString("firstname"));
                row.put("lastname", rs.getString("lastname"));
                row.put("email", rs.getString("email"));
                row.put("unsubscribeToken", rs.getString("unsubscribeToken"));
                subscribers.add(row);
            }
        }

        return subscribers;
    }

    public Map<String, Object> getEventCampaignPayload(int eventId) throws SQLException {
        Map<String, Object> payload = new HashMap<>();
        if (eventId <= 0) {
            return payload;
        }

        String sql = "SELECT e.eventID, e.name, e.type, e.date, e.description, e.infoUrl, e.imageMimeType, e.imageData, "
                + "v.name AS venueName, v.address "
                + "FROM event e JOIN venue v ON v.venueID = e.venueID WHERE e.eventID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return payload;
                }
                payload.put("eventID", rs.getInt("eventID"));
                payload.put("name", rs.getString("name"));
                payload.put("type", rs.getString("type"));
                payload.put("date", rs.getTimestamp("date"));
                payload.put("description", rs.getString("description"));
                payload.put("infoUrl", rs.getString("infoUrl"));
                payload.put("venueName", rs.getString("venueName"));
                payload.put("address", rs.getString("address"));
                payload.put("imageMimeType", rs.getString("imageMimeType"));
                payload.put("imageData", rs.getBytes("imageData"));
            }
        }

        return payload;
    }

    public List<Map<String, Object>> getFeaturedCampaignAdverts(int limit) throws SQLException {
        List<Map<String, Object>> adverts = new ArrayList<>();
        String sql = "SELECT advertID, organizationName, title, details, venue, eventDate, imageMimeType, imageData "
                + "FROM advert WHERE active = TRUE AND selectedForDisplay = TRUE AND paidOrganization = TRUE "
                + "ORDER BY eventDate ASC, advertID DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("advertID", rs.getInt("advertID"));
                    row.put("organizationName", rs.getString("organizationName"));
                    row.put("title", rs.getString("title"));
                    row.put("details", rs.getString("details"));
                    row.put("venue", rs.getString("venue"));
                    row.put("eventDate", rs.getDate("eventDate"));
                    row.put("imageMimeType", rs.getString("imageMimeType"));
                    row.put("imageData", rs.getBytes("imageData"));
                    adverts.add(row);
                }
            }
        }

        return adverts;
    }

    public Map<String, Object> updateBadgeProgressAndIssueCoupon(int attendeeId) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        if (attendeeId <= 0) {
            return result;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalTickets = countTickets(conn, attendeeId);
            BigDecimal totalSpend = totalSpend(conn, attendeeId);
            String level = badgeLevel(totalTickets);
            String title = badgeTitle(level);

            if (badgeExists(conn, attendeeId)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE attendee_badge SET badgeLevel = ?, badgeTitle = ?, totalTickets = ?, totalSpend = ?, lastUpdated = CURRENT_TIMESTAMP WHERE attendeeID = ?")) {
                    ps.setString(1, level);
                    ps.setString(2, title);
                    ps.setInt(3, totalTickets);
                    ps.setBigDecimal(4, totalSpend);
                    ps.setInt(5, attendeeId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO attendee_badge(attendeeID, badgeLevel, badgeTitle, totalTickets, totalSpend, lastUpdated) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)")) {
                    ps.setInt(1, attendeeId);
                    ps.setString(2, level);
                    ps.setString(3, title);
                    ps.setInt(4, totalTickets);
                    ps.setBigDecimal(5, totalSpend);
                    ps.executeUpdate();
                }
            }

            result.put("badgeLevel", level);
            result.put("badgeTitle", title);
            result.put("totalTickets", totalTickets);
            result.put("totalSpend", totalSpend);

            Map<String, Object> coupon = maybeIssueLevelCoupon(conn, attendeeId, totalTickets);
            if (!coupon.isEmpty()) {
                result.putAll(coupon);
            }
        }

        return result;
    }

    public List<Map<String, Object>> getWishlistLowStockCandidates(List<Integer> eventIds, int threshold, int cooldownHours)
            throws SQLException {
        List<Map<String, Object>> candidates = new ArrayList<>();
        if (eventIds == null || eventIds.isEmpty()) {
            return candidates;
        }

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < eventIds.size(); i++) {
            if (i > 0) {
                in.append(',');
            }
            in.append('?');
        }

        String sql = "SELECT aw.attendeeID, a.firstname, a.lastname, a.email, aw.eventID, e.name AS eventName, e.date AS eventDate, "
                + "v.name AS venueName, COUNT(DISTINCT eht.ticketID) AS totalTickets, COUNT(DISTINCT aht.ticketID) AS soldTickets, "
                + "MAX(CASE WHEN ahe.attendeeID IS NOT NULL THEN 1 ELSE 0 END) AS purchasedByAttendee, "
                + "MAX(l.lastAlertAt) AS lastAlertAt "
                + "FROM attendee_wishlist aw "
                + "JOIN attendee a ON a.attendeeID = aw.attendeeID "
                + "JOIN event e ON e.eventID = aw.eventID "
                + "JOIN venue v ON v.venueID = e.venueID "
                + "LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_event ahe ON ahe.attendeeID = aw.attendeeID AND ahe.eventID = aw.eventID "
                + "LEFT JOIN wishlist_stock_alert_log l ON l.attendeeID = aw.attendeeID AND l.eventID = aw.eventID "
                + "WHERE aw.eventID IN (" + in + ") "
                + "GROUP BY aw.attendeeID, a.firstname, a.lastname, a.email, aw.eventID, e.name, e.date, v.name";

        Timestamp cooldownCutoff = Timestamp.from(Instant.now().minus(cooldownHours, ChronoUnit.HOURS));

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer eventId : eventIds) {
                ps.setInt(index++, eventId == null ? 0 : eventId.intValue());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int totalTickets = rs.getInt("totalTickets");
                    int soldTickets = Math.min(totalTickets, rs.getInt("soldTickets"));
                    int remaining = Math.max(0, totalTickets - soldTickets);
                    boolean purchased = rs.getInt("purchasedByAttendee") == 1;
                    Timestamp lastAlertAt = rs.getTimestamp("lastAlertAt");

                    if (purchased || remaining <= 0 || remaining > threshold) {
                        continue;
                    }
                    if (lastAlertAt != null && lastAlertAt.after(cooldownCutoff)) {
                        continue;
                    }

                    Map<String, Object> row = new HashMap<>();
                    row.put("attendeeID", rs.getInt("attendeeID"));
                    row.put("firstname", rs.getString("firstname"));
                    row.put("lastname", rs.getString("lastname"));
                    row.put("email", rs.getString("email"));
                    row.put("eventID", rs.getInt("eventID"));
                    row.put("eventName", rs.getString("eventName"));
                    row.put("eventDate", rs.getTimestamp("eventDate"));
                    row.put("venueName", rs.getString("venueName"));
                    row.put("remainingTickets", remaining);
                    row.put("totalTickets", totalTickets);
                    candidates.add(row);
                }
            }
        }

        return candidates;
    }

    public void markWishlistStockAlertSent(int attendeeId, int eventId, int remainingTickets) throws SQLException {
        if (attendeeId <= 0 || eventId <= 0) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM wishlist_stock_alert_log WHERE attendeeID = ? AND eventID = ?")) {
                check.setInt(1, attendeeId);
                check.setInt(2, eventId);
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE wishlist_stock_alert_log SET remainingTickets = ?, lastAlertAt = CURRENT_TIMESTAMP WHERE attendeeID = ? AND eventID = ?")) {
                    update.setInt(1, remainingTickets);
                    update.setInt(2, attendeeId);
                    update.setInt(3, eventId);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO wishlist_stock_alert_log(attendeeID, eventID, remainingTickets, lastAlertAt) VALUES(?,?,?,CURRENT_TIMESTAMP)")) {
                    insert.setInt(1, attendeeId);
                    insert.setInt(2, eventId);
                    insert.setInt(3, remainingTickets);
                    insert.executeUpdate();
                }
            }
        }
    }

    public void logCampaignResult(String campaignType, Integer attendeeId, Integer eventId,
            String recipientEmail, String subject, String status) {
        String sql = "INSERT INTO email_campaign_log(campaignType, attendeeID, eventID, recipientEmail, subject, status, sentAt) "
                + "VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, campaignType);
            if (attendeeId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, attendeeId.intValue());
            }
            if (eventId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, eventId.intValue());
            }
            ps.setString(4, recipientEmail);
            ps.setString(5, subject);
            ps.setString(6, status);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // Logging should never block user workflows.
        }
    }

    private Map<String, Object> getSubscriptionRow(Connection conn, int attendeeId) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        String sql = "SELECT attendeeID, email, subscribed, unsubscribeToken FROM attendee_subscription WHERE attendeeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    row.put("attendeeID", rs.getInt("attendeeID"));
                    row.put("email", rs.getString("email"));
                    row.put("subscribed", rs.getBoolean("subscribed"));
                    row.put("unsubscribeToken", rs.getString("unsubscribeToken"));
                }
            }
        }
        return row;
    }

    private Map<String, Object> getBadgeRow(Connection conn, int attendeeId) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        String sql = "SELECT badgeLevel, badgeTitle, totalTickets, totalSpend FROM attendee_badge WHERE attendeeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    row.put("badgeLevel", rs.getString("badgeLevel"));
                    row.put("badgeTitle", rs.getString("badgeTitle"));
                    row.put("totalTickets", rs.getInt("totalTickets"));
                    row.put("totalSpend", rs.getBigDecimal("totalSpend"));
                }
            }
        }
        return row;
    }

    private Map<String, Object> getLatestActiveCoupon(Connection conn, int attendeeId) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        String sql = "SELECT couponCode, discountPercent FROM attendee_coupon "
                + "WHERE attendeeID = ? AND status = 'ACTIVE' "
                + "ORDER BY createdAt DESC FETCH FIRST ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    row.put("couponCode", rs.getString("couponCode"));
                    row.put("discountPercent", rs.getInt("discountPercent"));
                }
            }
        }
        return row;
    }

    private String attendeeEmail(Connection conn, int attendeeId) throws SQLException {
        String sql = "SELECT email FROM attendee WHERE attendeeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        return null;
    }

    private int countTickets(Connection conn, int attendeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendee_has_ticket WHERE attendeeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private BigDecimal totalSpend(Connection conn, int attendeeId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(totalAmount), 0) FROM attendee_order WHERE attendeeID = ? AND status = 'PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(1);
                    return val == null ? BigDecimal.ZERO : val;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean badgeExists(Connection conn, int attendeeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM attendee_badge WHERE attendeeID = ?")) {
            ps.setInt(1, attendeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Map<String, Object> maybeIssueLevelCoupon(Connection conn, int attendeeId, int totalTickets) throws SQLException {
        String level;
        int discount;
        if (totalTickets >= 30) {
            level = "PLATINUM";
            discount = 20;
        } else if (totalTickets >= 15) {
            level = "GOLD";
            discount = 15;
        } else if (totalTickets >= 5) {
            level = "SILVER";
            discount = 10;
        } else {
            return new HashMap<>();
        }

        String reason = "LEVEL_" + level;
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT couponCode, discountPercent FROM attendee_coupon WHERE attendeeID = ? AND reason = ?")) {
            check.setInt(1, attendeeId);
            check.setString(2, reason);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return new HashMap<>();
                }
            }
        }

        String couponCode = "TKY-" + level.substring(0, 3) + "-"
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(("A" + attendeeId + "-" + System.currentTimeMillis())
                                .getBytes(StandardCharsets.UTF_8))
                        .substring(0, 8)
                        .toUpperCase(Locale.ENGLISH);

        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO attendee_coupon(attendeeID, couponCode, discountPercent, status, reason, createdAt, expiresAt) "
                + "VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setInt(1, attendeeId);
            insert.setString(2, couponCode);
            insert.setInt(3, discount);
            insert.setString(4, "ACTIVE");
            insert.setString(5, reason);
            insert.setTimestamp(6, Timestamp.from(Instant.now().plus(45, ChronoUnit.DAYS)));
            insert.executeUpdate();
        }

        Map<String, Object> created = new HashMap<>();
        created.put("newCouponCode", couponCode);
        created.put("newCouponPercent", discount);
        created.put("newCouponReason", reason);
        return created;
    }

    private String badgeLevel(int totalTickets) {
        if (totalTickets >= 30) {
            return "PLATINUM";
        }
        if (totalTickets >= 15) {
            return "GOLD";
        }
        if (totalTickets >= 5) {
            return "SILVER";
        }
        if (totalTickets >= 1) {
            return "BRONZE";
        }
        return "NEW";
    }

    private String badgeTitle(String level) {
        if ("PLATINUM".equals(level)) {
            return "Most Buyer - Platinum";
        }
        if ("GOLD".equals(level)) {
            return "Most Buyer - Gold";
        }
        if ("SILVER".equals(level)) {
            return "Most Buyer - Silver";
        }
        if ("BRONZE".equals(level)) {
            return "Most Buyer - Bronze";
        }
        return "New Buyer";
    }

    private String randomToken() {
        byte[] bytes = new byte[36];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
