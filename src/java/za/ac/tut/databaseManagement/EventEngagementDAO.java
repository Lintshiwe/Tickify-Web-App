package za.ac.tut.databaseManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;

public class EventEngagementDAO {

    public boolean logEventAction(int eventId, String actorRole, Integer actorId, String actionType, String channel) throws SQLException {
        if (eventId <= 0 || actionType == null || actionType.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO event_engagement(eventID, actorRole, actorId, actionType, channel, createdAt) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, actorRole == null ? null : actorRole.trim());
            if (actorId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, actorId);
            }
            ps.setString(4, actionType.trim().toUpperCase());
            ps.setString(5, channel == null ? null : channel.trim().toUpperCase());
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            return ps.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> getTopPopularityRows(int maxRows) throws SQLException {
        int limit = maxRows <= 0 ? 10 : maxRows;
        String sql = "SELECT e.eventID, e.name AS eventName, "
                + "SUM(CASE WHEN ee.actionType='PREVIEW' THEN 1 ELSE 0 END) AS previews, "
                + "SUM(CASE WHEN ee.actionType='SHARE' THEN 1 ELSE 0 END) AS shares "
                + "FROM event e "
                + "LEFT JOIN event_engagement ee ON ee.eventID = e.eventID "
                + "GROUP BY e.eventID, e.name "
                + "ORDER BY shares DESC, previews DESC";

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("eventID", rs.getInt("eventID"));
                    row.put("eventName", rs.getString("eventName"));
                    row.put("previews", rs.getInt("previews"));
                    row.put("shares", rs.getInt("shares"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
