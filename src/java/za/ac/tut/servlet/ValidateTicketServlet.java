package za.ac.tut.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.scanner.ScannerService;
import za.ac.tut.application.scanner.ScannerService.ScanResult;

public class ValidateTicketServlet extends HttpServlet {

    private final ScannerService scannerService = new ScannerService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Integer userId = session != null ? (Integer) session.getAttribute("userID") : null;
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(response, false, "Session expired. Please login again.", null, null, null, "NOT_PROVIDED");
            return;
        }

        String code = request.getParameter("code");
        String mode = request.getParameter("mode");
        boolean consumeOnSuccess = mode == null || !"verify".equalsIgnoreCase(mode);
        try {
            ScanResult result = scannerService.validateAndLog(code, userId, consumeOnSuccess);
            writeJson(response,
                    result.isValid(),
                    result.getMessage(),
                    result.getTicketId(),
                    result.getEventName(),
                    result.getScannedAt(),
                    result.getAuthenticityStatus());
        } catch (SQLException e) {
            log("Scanner validation DB error: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(response, false, "Validation service is temporarily unavailable.", null, null, null, "NOT_PROVIDED");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST required");
    }

    private void writeJson(HttpServletResponse response, boolean valid, String message,
                           Integer ticketId, String eventName, String scannedAt,
                           String authenticityStatus) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.write("{");
            out.write("\"valid\":" + valid + ",");
            out.write("\"message\":\"" + escapeJson(message) + "\",");
            out.write("\"ticketId\":" + (ticketId == null ? "null" : ticketId) + ",");
            out.write("\"eventName\":" + (eventName == null ? "null" : "\"" + escapeJson(eventName) + "\"") + ",");
            out.write("\"scannedAt\":" + (scannedAt == null ? "null" : "\"" + escapeJson(scannedAt) + "\"") + ",");
            out.write("\"authenticityStatus\":" + (authenticityStatus == null ? "null" : "\"" + escapeJson(authenticityStatus) + "\""));
            out.write("}");
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}