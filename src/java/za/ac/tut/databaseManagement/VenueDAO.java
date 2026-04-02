
package za.ac.tut.databaseManagement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.entities.Venue;


public class VenueDAO {
    
/**
     * CENTRALIZED MAPPING
     * This is the single source of truth for turning a DB row into a Java Object.
     */
    private Venue mapRow(ResultSet rs) throws SQLException {
        return new Venue(
            rs.getInt("venueID"), 
            rs.getString("name"), 
            rs.getString("address")
        );
    }

    /**
     * REUSABLE QUERY EXECUTOR
     * Handles the boilerplate code for all SELECT operations.
     */
    private List<Venue> executeQuery(String sql, Object... params) throws SQLException {
        List<Venue> list = new ArrayList<>();
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

    /**
     * REUSABLE UPDATE EXECUTOR
     * Handles boilerplate for INSERT, UPDATE, and DELETE.
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

    // --- Clean Public Methods ---

    public List<Venue> getAllVenues() throws SQLException {
        return executeQuery("SELECT * FROM venue ORDER BY venueID");
    }

    public Venue getVenueByID(int id) throws SQLException {
        List<Venue> results = executeQuery("SELECT * FROM venue WHERE venueID = ?", id);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean insertVenue(Venue v) throws SQLException {
        String sql = "INSERT INTO venue(name, address) VALUES(?, ?)";
        return executeUpdate(sql, v.getName(), v.getAddress());
    }

    public boolean updateVenue(Venue v) throws SQLException {
        String sql = "UPDATE venue SET name=?, address=? WHERE venueID=?";
        return executeUpdate(sql, v.getName(), v.getAddress(), v.getVenueID());
    }

    public boolean deleteVenue(int id) throws SQLException {
        return executeUpdate("DELETE FROM venue WHERE venueID = ?", id);
    }
}
