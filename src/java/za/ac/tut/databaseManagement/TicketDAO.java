
package za.ac.tut.databaseManagement;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.entities.Ticket;
 

public class TicketDAO {
    
// OPTION: CENTRALIZED MAPPING (Prevents Redundancy)
    // This is the only place where rs.get... happens. 
    // If a column name changes in Derby, you fix it once here.
    private Ticket mapRow(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setTicketID(rs.getInt("ticketID"));
        t.setName(rs.getString("name"));
        t.setPrice(rs.getBigDecimal("price"));
        // Assuming your Ticket entity has a way to hold the QRcode object or ID
        // To keep it simple for JDBC:
        return t;
    }

    // OPTION: REUSABLE EXECUTION LOGIC
    // This private helper handles the connection and the loop, 
    // so your public methods stay short.
    private List<Ticket> executeQuery(String sql, Object... params) throws SQLException {
        List<Ticket> list = new ArrayList<>();
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

    public List<Ticket> getAllTickets() throws SQLException {
        return executeQuery("SELECT * FROM ticket ORDER BY ticketID");
    }

    public Ticket getTicketByID(int id) throws SQLException {
        List<Ticket> results = executeQuery("SELECT * FROM ticket WHERE ticketID = ?", id);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Ticket> getTicketsByMaxPrice(BigDecimal maxPrice) throws SQLException {
        return executeQuery("SELECT * FROM ticket WHERE price <= ? ORDER BY price", maxPrice);
    }

    // UPDATE/INSERT/DELETE use executeUpdate
    public boolean insertTicket(Ticket t) throws SQLException {
        String sql = "INSERT INTO ticket(name, price, QRcodeID) VALUES(?,?,?)";
        return executeUpdate(sql, t.getName(), t.getPrice(), t.getQrCode().getQrCodeID());
    }

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
