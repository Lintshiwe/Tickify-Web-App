package za.ac.tut.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.admin.AdminITService;

public class AdminDatabaseServlet extends HttpServlet {

    private final AdminITService adminITService = new AdminITService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String export = param(request, "export");
            if ("users".equalsIgnoreCase(export)) {
                exportUsers(request, response);
                return;
            }
            if ("tickets".equalsIgnoreCase(export)) {
                exportTickets(request, response);
                return;
            }

            populateModel(request, null, null, null);
            request.getRequestDispatcher("/Admin/AdminDatabase.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Failed to load admin database page", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load admin database page");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = param(request, "action");
        Object adminIdObj = request.getSession().getAttribute("userID");
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;

        try {
            if (isMutation(action) && !adminITService.repo().isPrivilegedAdmin(adminId)) {
                request.getSession().invalidate();
                response.sendRedirect(request.getContextPath() + "/Login.jsp?err=AccessDenied");
                return;
            }

            if ("executeSql".equals(action)) {
                String sql = req(request, "sqlText");
                String rootPassword = req(request, "rootPassword");
                if (!adminITService.repo().verifyPrivilegedRootPassword(adminId, rootPassword)) {
                    response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=RootAuthFailed");
                    return;
                }
                Map<String, Object> sqlResult = adminITService.repo().executeCrudSql(adminId, sql, rootPassword);
                populateModel(request, "SQLExecuted", null, sqlResult);
                request.setAttribute("sqlText", sql);
                request.getRequestDispatcher("/Admin/AdminDatabase.jsp").forward(request, response);
                return;
            }

            if ("updateCell".equals(action)) {
                String rootPassword = req(request, "rootPassword");
                if (!adminITService.repo().verifyPrivilegedRootPassword(adminId, rootPassword)) {
                    response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=RootAuthFailed");
                    return;
                }
                boolean ok = adminITService.repo().updateSingleCell(
                        adminId,
                        req(request, "tableName"),
                        req(request, "rowIdColumn"),
                        Integer.parseInt(req(request, "rowId")),
                        req(request, "targetColumn"),
                        req(request, "newValue"),
                        rootPassword
                );
                response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?msg=" + (ok ? "CellUpdated" : "NoChange"));
                return;
            }

            if ("safeDeleteRow".equals(action)) {
                String rootPassword = req(request, "rootPassword");
                if (!adminITService.repo().verifyPrivilegedRootPassword(adminId, rootPassword)) {
                    response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=RootAuthFailed");
                    return;
                }
                boolean ok = adminITService.repo().safeDeleteRow(adminId, req(request, "tableName"), Integer.parseInt(req(request, "rowId")));
                response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?msg=" + (ok ? "RowDeleted" : "NoChange"));
                return;
            }

            response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=UnknownAction");
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=MissingFields");
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Invalid root password")) {
                response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=RootAuthFailed");
                return;
            }
            log("Database page operation failed", ex);
            response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=OperationFailed");
        }
    }

    private void populateModel(HttpServletRequest request, String msg, String err, Map<String, Object> sqlResult) throws SQLException {
        Object adminIdObj = request.getSession().getAttribute("userID");
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
        String previewTable = param(request, "table");
        Map<String, Object> preview = adminITService.repo().getTablePreview(previewTable);
        String exportRole = param(request, "roleFilter");
        String exportCampus = param(request, "campusFilter");
        String exportSearch = param(request, "search");
        String exportScope = param(request, "scope");
        String ticketCampus = param(request, "ticketCampusFilter");
        String ticketSearch = param(request, "ticketSearch");
        String ticketScope = param(request, "ticketScope");
        String ticketEventId = param(request, "ticketEventID");
        Integer ticketEventIdInt = parseOptionalInt(ticketEventId);

        request.setAttribute("metrics", adminITService.repo().getDashboardMetrics());
        request.setAttribute("tableSummary", adminITService.repo().getUserTableSummary());
        request.setAttribute("isPrivilegedAdmin", adminITService.repo().isPrivilegedAdmin(adminId));
        request.setAttribute("previewTable", preview.get("table"));
        request.setAttribute("previewColumns", preview.get("columns"));
        request.setAttribute("previewRows", preview.get("rows"));
        request.setAttribute("previewAllowedTables", preview.get("allowedTables"));
        request.setAttribute("safeDeleteTables", preview.get("safeDeleteTables"));
        request.setAttribute("exportCampusOptions", adminITService.repo().getUserExportCampusNamesForScope(adminId));
        request.setAttribute("exportRole", exportRole == null || exportRole.isEmpty() ? "ALL" : exportRole);
        request.setAttribute("exportCampus", exportCampus == null ? "" : exportCampus);
        request.setAttribute("exportSearch", exportSearch == null ? "" : exportSearch);
        request.setAttribute("exportScope", exportScope == null || exportScope.isEmpty() ? "filtered" : exportScope);
        request.setAttribute("ticketCampusOptions", adminITService.repo().getUserExportCampusNamesForScope(adminId));
        request.setAttribute("ticketEventOptions", adminITService.repo().getEventOptionsForScope(adminId));
        request.setAttribute("ticketCampus", ticketCampus == null ? "" : ticketCampus);
        request.setAttribute("ticketSearch", ticketSearch == null ? "" : ticketSearch);
        request.setAttribute("ticketScope", ticketScope == null || ticketScope.isEmpty() ? "filtered" : ticketScope);
        request.setAttribute("ticketEventID", ticketEventIdInt);

        if (msg != null) {
            request.setAttribute("msg", msg);
        }
        if (err != null) {
            request.setAttribute("err", err);
        }
        if (sqlResult != null) {
            request.setAttribute("sqlColumns", sqlResult.get("columns"));
            request.setAttribute("sqlRows", sqlResult.get("rows"));
            request.setAttribute("sqlUpdateCount", sqlResult.get("updateCount"));
        }
    }

    private String req(HttpServletRequest request, String key) {
        String value = param(request, key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return value;
    }

    private String param(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? null : value.trim();
    }

    private boolean isMutation(String action) {
        return "executeSql".equals(action)
                || "updateCell".equals(action)
                || "safeDeleteRow".equals(action);
    }

    private void exportUsers(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Object adminIdObj = request.getSession().getAttribute("userID");
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
        boolean privileged = adminITService.repo().isPrivilegedAdmin(adminId);
        if (!privileged) {
            response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=PrivilegedRequired");
            return;
        }

        String roleFilter = param(request, "roleFilter");
        String campusFilter = param(request, "campusFilter");
        String search = param(request, "search");
        String scope = param(request, "scope");
        boolean includeAllForPrivileged = privileged && "all".equalsIgnoreCase(scope);
        String format = param(request, "format");

        List<Map<String, Object>> rows = adminITService.repo().getUserDirectoryForExport(
                adminId,
                roleFilter,
                campusFilter,
                search,
                includeAllForPrivileged
        );

        if ("pdf".equalsIgnoreCase(format)) {
            writeUserDirectoryPdf(response, rows, roleFilter, campusFilter, search, includeAllForPrivileged);
            return;
        }
        writeUserDirectoryCsv(response, rows);
    }

    private void exportTickets(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Object adminIdObj = request.getSession().getAttribute("userID");
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
        boolean privileged = adminITService.repo().isPrivilegedAdmin(adminId);
        if (!privileged) {
            response.sendRedirect(request.getContextPath() + "/AdminDatabase.do?err=PrivilegedRequired");
            return;
        }

        String campusFilter = param(request, "ticketCampusFilter");
        String search = param(request, "ticketSearch");
        String scope = param(request, "ticketScope");
        Integer eventId = parseOptionalInt(param(request, "ticketEventID"));
        boolean includeAllForPrivileged = privileged && "all".equalsIgnoreCase(scope);
        String format = param(request, "format");

        List<Map<String, Object>> rows = adminITService.repo().getTicketDirectoryForExport(
                adminId,
                campusFilter,
                eventId,
                search,
                includeAllForPrivileged
        );

        if ("pdf".equalsIgnoreCase(format)) {
            writeTicketDirectoryPdf(response, rows, campusFilter, eventId, search, includeAllForPrivileged);
            return;
        }
        writeTicketDirectoryCsv(response, rows);
    }

    private void writeUserDirectoryCsv(HttpServletResponse response, List<Map<String, Object>> rows) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=user-directory-" + timestamp + ".csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("\uFEFF");
            writer.println("role,userID,username,firstname,lastname,email,campusName,eventID,venueID");
            for (Map<String, Object> row : rows) {
                writer.println(csv(row.get("role")) + ","
                        + csv(row.get("userID")) + ","
                        + csv(row.get("username")) + ","
                        + csv(row.get("firstname")) + ","
                        + csv(row.get("lastname")) + ","
                        + csv(row.get("email")) + ","
                        + csv(row.get("campusName")) + ","
                        + csv(row.get("eventID")) + ","
                        + csv(row.get("venueID")));
            }
        }
    }

    private void writeUserDirectoryPdf(HttpServletResponse response, List<Map<String, Object>> rows,
            String roleFilter, String campusFilter, String search, boolean includeAllForPrivileged) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=user-directory-" + timestamp + ".pdf");

        List<String> lines = new ArrayList<>();
        lines.add("TICKIFY ADMIN");
        lines.add("Logo: tickify-admin-logo.svg");
        lines.add("User Directory Export");
        lines.add("Generated: " + LocalDateTime.now());
        lines.add("Scope: " + (includeAllForPrivileged ? "All Campuses" : "Campus Scoped")
                + " | Role: " + (roleFilter == null || roleFilter.isEmpty() ? "ALL" : roleFilter)
                + " | Campus: " + (campusFilter == null || campusFilter.isEmpty() ? "ALL" : campusFilter)
                + " | Search: " + (search == null || search.isEmpty() ? "None" : search));
        lines.add("");
        lines.add("ROLE | ID | USERNAME | FIRSTNAME | LASTNAME | EMAIL | CAMPUS | EVENT | VENUE");
        lines.add("----------------------------------------------------------------------------------------------");

        if (rows == null || rows.isEmpty()) {
            lines.add("No users matched the selected filters.");
        } else {
            for (Map<String, Object> row : rows) {
                lines.add(trimLine(
                        safe(row.get("role")) + " | "
                        + safe(row.get("userID")) + " | "
                        + safe(row.get("username")) + " | "
                        + safe(row.get("firstname")) + " | "
                        + safe(row.get("lastname")) + " | "
                        + safe(row.get("email")) + " | "
                        + safe(row.get("campusName")) + " | "
                        + safe(row.get("eventID")) + " | "
                        + safe(row.get("venueID")), 120));
            }
        }

        byte[] pdfBytes = buildSimplePdf(lines);
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
    }

    private void writeTicketDirectoryCsv(HttpServletResponse response, List<Map<String, Object>> rows) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=ticket-directory-" + timestamp + ".csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("\uFEFF");
            writer.println("ticketID,ticketName,price,QRcodeID,qrCode,eventID,eventName,venueID,campusName,attendeeID,username,firstname,lastname,email");
            for (Map<String, Object> row : rows) {
                writer.println(csv(row.get("ticketID")) + ","
                        + csv(row.get("ticketName")) + ","
                        + csv(row.get("price")) + ","
                        + csv(row.get("QRcodeID")) + ","
                        + csv(row.get("qrCode")) + ","
                        + csv(row.get("eventID")) + ","
                        + csv(row.get("eventName")) + ","
                        + csv(row.get("venueID")) + ","
                        + csv(row.get("campusName")) + ","
                        + csv(row.get("attendeeID")) + ","
                        + csv(row.get("username")) + ","
                        + csv(row.get("firstname")) + ","
                        + csv(row.get("lastname")) + ","
                        + csv(row.get("email")));
            }
        }
    }

    private void writeTicketDirectoryPdf(HttpServletResponse response, List<Map<String, Object>> rows,
            String campusFilter, Integer eventId, String search, boolean includeAllForPrivileged) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=ticket-directory-" + timestamp + ".pdf");

        List<String> lines = new ArrayList<>();
        lines.add("TICKIFY ADMIN");
        lines.add("Logo: tickify-admin-logo.svg");
        lines.add("Ticket Directory Export");
        lines.add("Generated: " + LocalDateTime.now());
        lines.add("Scope: " + (includeAllForPrivileged ? "All Campuses" : "Campus Scoped")
                + " | Campus: " + (campusFilter == null || campusFilter.isEmpty() ? "ALL" : campusFilter)
                + " | Event ID: " + (eventId == null ? "ALL" : eventId)
                + " | Search: " + (search == null || search.isEmpty() ? "None" : search));
        lines.add("");
        lines.add("TICKETID | TICKET | PRICE | EVENT | CAMPUS | ATTENDEE | EMAIL | QRCODE");
        lines.add("----------------------------------------------------------------------------------------------");

        if (rows == null || rows.isEmpty()) {
            lines.add("No tickets matched the selected filters.");
        } else {
            for (Map<String, Object> row : rows) {
                String attendeeName = (safe(row.get("firstname")) + " " + safe(row.get("lastname"))).trim();
                lines.add(trimLine(
                        safe(row.get("ticketID")) + " | "
                        + safe(row.get("ticketName")) + " | "
                        + safe(row.get("price")) + " | "
                        + safe(row.get("eventName")) + " | "
                        + safe(row.get("campusName")) + " | "
                        + attendeeName + " | "
                        + safe(row.get("email")) + " | "
                        + safe(row.get("qrCode")), 120));
            }
        }

        byte[] pdfBytes = buildSimplePdf(lines);
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
    }

    private Integer parseOptionalInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String h(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trimLine(String line, int maxLen) {
        if (line == null) {
            return "";
        }
        if (line.length() <= maxLen) {
            return line;
        }
        return line.substring(0, maxLen - 3) + "...";
    }

    private byte[] buildSimplePdf(List<String> lines) {
        List<String> contentStreams = new ArrayList<>();
        int perPage = 52;
        for (int start = 0; start < lines.size(); start += perPage) {
            int end = Math.min(start + perPage, lines.size());
            StringBuilder stream = new StringBuilder();
            stream.append("BT\n");
            stream.append("/F1 10 Tf\n");
            stream.append("14 TL\n");
            stream.append("40 800 Td\n");
            for (int i = start; i < end; i++) {
                stream.append("(").append(escapePdf(lines.get(i))).append(") Tj\n");
                if (i < end - 1) {
                    stream.append("T*\n");
                }
            }
            stream.append("\nET\n");
            contentStreams.add(stream.toString());
        }

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < contentStreams.size(); i++) {
            int pageObj = 4 + (i * 2);
            kids.append(pageObj).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [ " + kids + "] /Count " + contentStreams.size() + " >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        for (int i = 0; i < contentStreams.size(); i++) {
            int contentObj = 5 + (i * 2);
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObj + " 0 R >>");
            String stream = contentStreams.get(i);
            int length = stream.getBytes(StandardCharsets.ISO_8859_1).length;
            objects.add("<< /Length " + length + " >>\nstream\n" + stream + "endstream");
        }

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n");
            pdf.append(objects.get(i)).append("\n");
            pdf.append("endobj\n");
        }

        int xrefOffset = pdf.length();
        pdf.append("xref\n");
        pdf.append("0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        pdf.append("trailer\n");
        pdf.append("<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escapePdf(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}
