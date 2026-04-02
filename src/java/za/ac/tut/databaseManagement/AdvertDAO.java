package za.ac.tut.databaseManagement;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;

public class AdvertDAO {

    public List<Map<String, Object>> getActiveSelectedAdverts() throws SQLException {
        List<Map<String, Object>> adverts = new ArrayList<>();
        String sql = "SELECT advertID, organizationName, title, details, venue, eventDate "
                + "FROM advert "
                + "WHERE active = TRUE AND selectedForDisplay = TRUE AND paidOrganization = TRUE "
                + "ORDER BY eventDate ASC, advertID DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("advertID", rs.getInt("advertID"));
                row.put("organizationName", rs.getString("organizationName"));
                row.put("title", rs.getString("title"));
                row.put("details", rs.getString("details"));
                row.put("venue", rs.getString("venue"));
                row.put("eventDate", rs.getDate("eventDate"));
                adverts.add(row);
            }
        }
        return adverts;
    }

    public List<Map<String, Object>> getAllAdverts() throws SQLException {
        List<Map<String, Object>> adverts = new ArrayList<>();
        String sql = "SELECT advertID, organizationName, title, details, venue, eventDate, paidOrganization, selectedForDisplay, active, imageMimeType "
                + "FROM advert ORDER BY advertID DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("advertID", rs.getInt("advertID"));
                row.put("organizationName", rs.getString("organizationName"));
                row.put("title", rs.getString("title"));
                row.put("details", rs.getString("details"));
                row.put("venue", rs.getString("venue"));
                row.put("eventDate", rs.getDate("eventDate"));
                row.put("paidOrganization", rs.getBoolean("paidOrganization"));
                row.put("selectedForDisplay", rs.getBoolean("selectedForDisplay"));
                row.put("active", rs.getBoolean("active"));
                row.put("imageMimeType", rs.getString("imageMimeType"));
                adverts.add(row);
            }
        }
        return adverts;
    }

    public int createAdvert(String organizationName, String title, String details, String venue, Date eventDate,
            boolean paidOrganization, boolean selectedForDisplay, boolean active,
            String imageFilename, String imageMimeType, byte[] imageData) throws SQLException {
        String sql = "INSERT INTO advert(organizationName, title, details, venue, eventDate, paidOrganization, selectedForDisplay, active, imageFilename, imageMimeType, imageData, createdAt) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, organizationName);
            ps.setString(2, title);
            ps.setString(3, details);
            ps.setString(4, venue);
            ps.setDate(5, eventDate);
            ps.setBoolean(6, paidOrganization);
            ps.setBoolean(7, selectedForDisplay);
            ps.setBoolean(8, active);
            ps.setString(9, imageFilename);
            ps.setString(10, imageMimeType);
            ps.setBytes(11, imageData);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void updateAdvertFlags(int advertID, boolean paidOrganization, boolean selectedForDisplay, boolean active) throws SQLException {
        String sql = "UPDATE advert SET paidOrganization = ?, selectedForDisplay = ?, active = ? WHERE advertID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, paidOrganization);
            ps.setBoolean(2, selectedForDisplay);
            ps.setBoolean(3, active);
            ps.setInt(4, advertID);
            ps.executeUpdate();
        }
    }

    public boolean updateAdvert(int advertID, String organizationName, String title, String details, String venue,
            Date eventDate, boolean paidOrganization, boolean selectedForDisplay, boolean active,
            String imageFilename, String imageMimeType, byte[] imageData) throws SQLException {
        String sqlWithImage = "UPDATE advert SET organizationName = ?, title = ?, details = ?, venue = ?, eventDate = ?, "
                + "paidOrganization = ?, selectedForDisplay = ?, active = ?, imageFilename = ?, imageMimeType = ?, imageData = ? "
                + "WHERE advertID = ?";
        String sqlWithoutImage = "UPDATE advert SET organizationName = ?, title = ?, details = ?, venue = ?, eventDate = ?, "
                + "paidOrganization = ?, selectedForDisplay = ?, active = ? WHERE advertID = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (imageData != null && imageData.length > 0) {
                try (PreparedStatement ps = conn.prepareStatement(sqlWithImage)) {
                    ps.setString(1, organizationName);
                    ps.setString(2, title);
                    ps.setString(3, details);
                    ps.setString(4, venue);
                    ps.setDate(5, eventDate);
                    ps.setBoolean(6, paidOrganization);
                    ps.setBoolean(7, selectedForDisplay);
                    ps.setBoolean(8, active);
                    ps.setString(9, imageFilename);
                    ps.setString(10, imageMimeType);
                    ps.setBytes(11, imageData);
                    ps.setInt(12, advertID);
                    return ps.executeUpdate() > 0;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlWithoutImage)) {
                ps.setString(1, organizationName);
                ps.setString(2, title);
                ps.setString(3, details);
                ps.setString(4, venue);
                ps.setDate(5, eventDate);
                ps.setBoolean(6, paidOrganization);
                ps.setBoolean(7, selectedForDisplay);
                ps.setBoolean(8, active);
                ps.setInt(9, advertID);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean deleteAdvert(int advertID) throws SQLException {
        String sql = "DELETE FROM advert WHERE advertID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, advertID);
            return ps.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getAdvertImage(int advertID) throws SQLException {
        String sql = "SELECT imageMimeType, imageData FROM advert WHERE advertID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, advertID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> img = new HashMap<>();
                    img.put("mimeType", rs.getString("imageMimeType"));
                    img.put("data", rs.getBytes("imageData"));
                    return img;
                }
            }
        }
        return null;
    }
}
