package za.ac.tut.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.scanner.ScannerService;

public class VenueGuardAttendeesServlet extends HttpServlet {

    private final ScannerService scannerService = new ScannerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Integer guardId = session != null ? (Integer) session.getAttribute("userID") : null;
        if (guardId == null || guardId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"ok\":false,\"message\":\"Session expired\",\"rows\":[]}");
            }
            return;
        }

        try {
            List<Map<String, Object>> rows = scannerService.getAttendeeListForGuardEvent(guardId, 250);
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"ok\":true,\"rows\":[");
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> row = rows.get(i);
                    if (i > 0) {
                        out.write(",");
                    }
                    out.write("{");
                    out.write("\"attendeeID\":" + toJsonNumber(row.get("attendeeID")) + ",");
                    out.write("\"username\":" + toJsonText(row.get("username")) + ",");
                    out.write("\"firstname\":" + toJsonText(row.get("firstname")) + ",");
                    out.write("\"lastname\":" + toJsonText(row.get("lastname")) + ",");
                    out.write("\"email\":" + toJsonText(row.get("email")));
                    out.write("}");
                }
                out.write("]}");
            }
        } catch (SQLException ex) {
            log("Failed to fetch venue guard attendee list", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"ok\":false,\"message\":\"Unable to load attendee list\",\"rows\":[]}");
            }
        }
    }

    private String toJsonText(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + text + "\"";
    }

    private String toJsonNumber(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        try {
            return String.valueOf(Integer.parseInt(String.valueOf(value)));
        } catch (Exception ex) {
            return "null";
        }
    }
}
