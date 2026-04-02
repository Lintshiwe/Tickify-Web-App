package za.ac.tut.databaseManagement;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.entities.TertiaryPresenter;
import za.ac.tut.entities.Event;

public class TertiaryPresenterDAO {

    /**
     * Converts a DB row into a TertiaryPresenter object.
     */
    private TertiaryPresenter mapRow(ResultSet rs) throws SQLException {
        TertiaryPresenter tp = new TertiaryPresenter();
        tp.setTertiaryPresenterID(rs.getInt("tertiaryPresenterID"));
        tp.setUsername(rs.getString("username"));
        tp.setFirstname(rs.getString("firstname"));
        tp.setLastname(rs.getString("lastname"));
        tp.setEmail(rs.getString("email"));
        tp.setPassword(rs.getString("password"));
        tp.setTertiaryInstitution(rs.getString("tertiaryInstitution"));
        tp.setPhoneNumber(rs.getString("phoneNumber"));
        tp.setBiography(rs.getString("biography"));
        // For JDBC, we leave event/venue as null or fetch separately if needed
        return tp;
    }

    public boolean insertPresenter(TertiaryPresenter tp) throws SQLException {
        // 1. Updated SQL to include all 7 columns (excluding the auto-increment PK)
        String sql = "INSERT INTO TERTIARY_PRESENTER (username, firstname, lastname, email, password, tertiaryInstitution, phoneNumber, biography, emailVerified, eventID, venueID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            // 2. Set the basic Strings
            ps.setString(1, tp.getUsername());
            ps.setString(2, tp.getFirstname());
            ps.setString(3, tp.getLastname());
            ps.setString(4, tp.getEmail());
            ps.setString(5, tp.getPassword());
            ps.setString(6, tp.getTertiaryInstitution());
            ps.setString(7, tp.getPhoneNumber());
            ps.setString(8, tp.getBiography());
            ps.setBoolean(9, false);

            ps.setInt(10, 2);

            ps.setInt(11, 3);

            return ps.executeUpdate() > 0;
        }
    }

    public TertiaryPresenter getPresenterByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM tertiary_presenter WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * DELETE ACCOUNT: Removes the presenter from the database by their ID.
     */
    public boolean deletePresenterAccount(int presenterID) throws SQLException {
        String sql = "DELETE FROM tertiary_presenter WHERE tertiaryPresenterID = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, presenterID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * BOOK EVENT: Updates the presenter's record to link them to a specific
     * eventID. In your ERD, the presenter "owns" the relationship via the
     * eventID column.
     */
    public boolean bookEvent(int presenterID, int eventID) throws SQLException {
        String sql = "UPDATE tertiary_presenter SET eventID = ? WHERE tertiaryPresenterID = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventID);
            ps.setInt(2, presenterID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * GET BOOKED EVENTS: Fetches details of the event currently linked to the
     * presenter. Since a presenter is linked to one event at a time
     * (Many-to-One), this returns the specific Event object.
     */
    public Event getBookedEvent(int presenterID) throws SQLException {
        String sql = "SELECT e.* FROM event e "
                + "JOIN tertiary_presenter tp ON e.eventID = tp.eventID "
                + "WHERE tp.tertiaryPresenterID = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, presenterID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Event event = new Event();
                    event.setEventID(rs.getInt("eventID"));
                    event.setName(rs.getString("name"));
                    // Map other event fields as per your Event entity
                    return event;
                }
            }
        }
        return null; // Returns null if no event is booked
    }

    public Map<String, Object> getDashboardProfile(int presenterID) throws SQLException {
        String sql = "SELECT p.tertiaryPresenterID, p.username, p.firstname, p.lastname, p.email, "
                + "p.tertiaryInstitution, p.phoneNumber, p.biography, "
                + "e.eventID, e.name AS eventName, e.type AS eventType, e.date AS eventDate, "
                + "v.venueID, v.name AS venueName, v.address AS venueAddress "
                + "FROM tertiary_presenter p "
                + "LEFT JOIN event e ON e.eventID = p.eventID "
                + "LEFT JOIN venue v ON v.venueID = p.venueID "
                + "WHERE p.tertiaryPresenterID = ?";

        List<Map<String, Object>> rows = runListQuery(sql, presenterID);
        return rows.isEmpty() ? new HashMap<String, Object>() : rows.get(0);
    }

    public Map<String, Object> getEventSnapshot(int presenterID) throws SQLException {
        String sql = "SELECT "
            + "COALESCE(stock.totalTickets, 0) AS totalTickets, "
            + "COALESCE(stock.soldTickets, 0) AS soldTickets, "
            + "COALESCE(stock.revenue, 0) AS revenue, "
            + "COALESCE(wish.wishlistCount, 0) AS wishlistCount "
            + "FROM tertiary_presenter p "
            + "LEFT JOIN ("
            + "    SELECT eht.eventID, "
            + "           COUNT(DISTINCT eht.ticketID) AS totalTickets, "
            + "           COUNT(DISTINCT aht.ticketID) AS soldTickets, "
            + "           COALESCE(SUM(t.price), 0) AS revenue "
            + "    FROM event_has_ticket eht "
            + "    LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
            + "    LEFT JOIN ticket t ON t.ticketID = aht.ticketID "
            + "    GROUP BY eht.eventID"
            + ") stock ON stock.eventID = p.eventID "
            + "LEFT JOIN ("
            + "    SELECT aw.eventID, COUNT(*) AS wishlistCount "
            + "    FROM attendee_wishlist aw "
            + "    GROUP BY aw.eventID"
            + ") wish ON wish.eventID = p.eventID "
            + "WHERE p.tertiaryPresenterID = ?";

        List<Map<String, Object>> rows = runListQuery(sql, presenterID);
        Map<String, Object> out = rows.isEmpty() ? new HashMap<String, Object>() : rows.get(0);

        int total = toInt(out.get("totalTickets"));
        int sold = Math.min(total, toInt(out.get("soldTickets")));
        int available = Math.max(0, total - sold);
        int soldPercentage = total > 0 ? (int) Math.round((sold * 100.0) / total) : 0;

        out.put("totalTickets", total);
        out.put("soldTickets", sold);
        out.put("availableTickets", available);
        out.put("soldPercentage", soldPercentage);
        out.put("soldOut", total > 0 && soldPercentage >= 100);
        out.put("nearlySoldOut", total > 0 && soldPercentage >= 80 && soldPercentage < 100);
        return out;
    }

    public List<Map<String, Object>> getPresenterTeamContacts(int presenterID) throws SQLException {
        String sql = "SELECT DISTINCT m.eventManagerID, m.firstname, m.lastname, m.email, "
                + "e.name AS eventName "
                + "FROM tertiary_presenter p "
                + "LEFT JOIN event e ON e.eventID = p.eventID "
                + "LEFT JOIN event_has_manager ehm ON ehm.eventID = e.eventID "
                + "LEFT JOIN event_manager m ON m.eventManagerID = ehm.eventManagerID "
                + "WHERE p.tertiaryPresenterID = ? "
                + "ORDER BY m.firstname, m.lastname";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getVenueGuardContacts(int presenterID) throws SQLException {
        String sql = "SELECT g.venueGuardID, g.firstname, g.lastname, g.email "
                + "FROM tertiary_presenter p "
                + "JOIN venue_guard g ON g.venueID = p.venueID "
                + "WHERE p.tertiaryPresenterID = ? "
                + "ORDER BY g.firstname, g.lastname";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getPeerPresentersAtVenue(int presenterID) throws SQLException {
        String sql = "SELECT p2.tertiaryPresenterID, p2.firstname, p2.lastname, p2.email, "
                + "p2.tertiaryInstitution, e.name AS eventName "
                + "FROM tertiary_presenter p "
                + "JOIN tertiary_presenter p2 ON p2.venueID = p.venueID "
                + "LEFT JOIN event e ON e.eventID = p2.eventID "
                + "WHERE p.tertiaryPresenterID = ? "
                + "AND p2.tertiaryPresenterID <> p.tertiaryPresenterID "
                + "ORDER BY p2.firstname, p2.lastname";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getPresenterMaterials(int presenterID) throws SQLException {
        String sql = "SELECT materialID, title, materialUrl, description, createdAt "
                + "FROM presenter_material WHERE tertiaryPresenterID = ? ORDER BY createdAt DESC";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getPresenterScheduleItems(int presenterID) throws SQLException {
        String sql = "SELECT scheduleItemID, title, startsAt, endsAt, room, notes, createdAt "
                + "FROM presenter_schedule_item WHERE tertiaryPresenterID = ? ORDER BY startsAt ASC";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getPresenterAnnouncements(int presenterID) throws SQLException {
        String sql = "SELECT announcementID, title, body, createdAt "
                + "FROM presenter_announcement WHERE tertiaryPresenterID = ? ORDER BY createdAt DESC";
        return runListQuery(sql, presenterID);
    }

    public List<Map<String, Object>> getEventAttendeesForPresenter(int presenterID) throws SQLException {
        String sql = "SELECT DISTINCT a.attendeeID, a.username, a.firstname, a.lastname, a.email "
                + "FROM tertiary_presenter p "
                + "JOIN event_has_ticket eht ON eht.eventID = p.eventID "
                + "JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "WHERE p.tertiaryPresenterID = ? "
                + "ORDER BY a.firstname, a.lastname";
        return runListQuery(sql, presenterID);
    }

    public boolean addPresenterMaterial(int presenterID, String title, String materialUrl, String description) throws SQLException {
        if (presenterID <= 0 || title == null || title.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO presenter_material(tertiaryPresenterID, title, materialUrl, description, createdAt) "
                + "VALUES(?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, presenterID);
            ps.setString(2, title.trim());
            ps.setString(3, materialUrl == null ? null : materialUrl.trim());
            ps.setString(4, description == null ? null : description.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean addPresenterScheduleItem(int presenterID, String title, Timestamp startsAt,
            Timestamp endsAt, String room, String notes) throws SQLException {
        if (presenterID <= 0 || startsAt == null || title == null || title.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO presenter_schedule_item(tertiaryPresenterID, title, startsAt, endsAt, room, notes, createdAt) "
                + "VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, presenterID);
            ps.setString(2, title.trim());
            ps.setTimestamp(3, startsAt);
            if (endsAt == null) {
                ps.setNull(4, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(4, endsAt);
            }
            ps.setString(5, room == null ? null : room.trim());
            ps.setString(6, notes == null ? null : notes.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean addPresenterAnnouncement(int presenterID, String title, String body) throws SQLException {
        if (presenterID <= 0 || title == null || title.trim().isEmpty() || body == null || body.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO presenter_announcement(tertiaryPresenterID, title, body, createdAt) "
                + "VALUES(?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, presenterID);
            ps.setString(2, title.trim());
            ps.setString(3, body.trim());
            return ps.executeUpdate() > 0;
        }
    }

    private List<Map<String, Object>> runListQuery(String sql, int presenterID) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, presenterID);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        String label = meta.getColumnLabel(i);
                        if (label == null || label.trim().isEmpty()) {
                            label = meta.getColumnName(i);
                        }
                        row.put(label, rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

}
