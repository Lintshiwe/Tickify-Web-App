
package za.ac.tut.databaseManagement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.entities.Event;

public class EventDAO {
    
    /**
     * CENTRALIZED MAPPING
     * Pulls data from the ResultSet and populates the Event entity.
     */
    private Event mapRow(ResultSet rs) throws SQLException {
        Event ev = new Event();
        ev.setEventID(rs.getInt("eventID"));
        ev.setName(rs.getString("name"));
        ev.setType(rs.getString("type"));
        ev.setDate(rs.getTimestamp("date"));
        // We set the venueID transient field directly for simple JDBC listing
        ev.setVenueID(rs.getInt("venueID"));
        return ev;
    }

    /**
     * REUSABLE QUERY EXECUTOR
     * Reduces redundancy by handling the connection and statement logic once.
     */
    private List<Event> executeQuery(String sql, Object... params) throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // --- Clean Public Methods ---

    public List<Event> getAllEvents() throws SQLException {
        // Updated to match the sorting in your Entity's @NamedQuery
        return executeQuery("SELECT * FROM event ORDER BY date ASC");
    }

    public Event getEventByID(int id) throws SQLException {
        List<Event> results = executeQuery("SELECT * FROM event WHERE eventID = ?", id);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Event> getEventsByType(String type) throws SQLException {
        return executeQuery("SELECT * FROM event WHERE type = ? ORDER BY date ASC", type);
    }

    public boolean insertEvent(Event ev) throws SQLException {
        String sql = "INSERT INTO event(name, type, date, venueID) VALUES(?,?,?,?)";
        return executeUpdate(sql, 
                ev.getName(), 
                ev.getType(), 
                ev.getDate(), 
                ev.getVenueID());
    }

    public boolean updateEvent(Event ev) throws SQLException {
        String sql = "UPDATE event SET name=?, type=?, date=?, venueID=? WHERE eventID=?";
        return executeUpdate(sql, 
                ev.getName(), 
                ev.getType(), 
                ev.getDate(), 
                ev.getVenueID(), 
                ev.getEventID());
    }

    public boolean deleteEvent(int id) throws SQLException {
        return executeUpdate("DELETE FROM event WHERE eventID = ?", id);
    }

    public boolean updateEventAlbumImage(int eventID, String filename, String mimeType, byte[] imageData) throws SQLException {
        String sql = "UPDATE event SET imageFilename = ?, imageMimeType = ?, imageData = ? WHERE eventID = ?";
        return executeUpdate(sql, filename, mimeType, imageData, eventID);
    }

    public Map<String, Object> getEventAlbumImage(int eventID) throws SQLException {
        String sql = "SELECT imageMimeType, imageData FROM event WHERE eventID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("mimeType", rs.getString("imageMimeType"));
                    result.put("imageData", rs.getBytes("imageData"));
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * REUSABLE UPDATE EXECUTOR
     */
    private boolean executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate() > 0;
        }
    }
    
}
