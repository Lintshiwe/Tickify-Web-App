package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.admin.AdminITService;

public class AdminAlertsServlet extends HttpServlet {

    private final AdminITService adminITService = new AdminITService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object adminIdObj = request.getSession(false) != null ? request.getSession(false).getAttribute("userID") : null;
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
        if (adminId <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }
        String campusFilter = param(request, "campus");

        try {
            boolean privileged = adminITService.repo().isPrivilegedAdmin(adminId);
            List<Map<String, Object>> lockedAccounts = adminITService.repo().getLockedAccountsForUnblockQueue(adminId);
            List<Map<String, Object>> deleteRequests = adminITService.repo().getDeletionRequestsForAlerts(adminId);

            if (privileged && campusFilter != null && !campusFilter.isEmpty()) {
                lockedAccounts = filterByCampus(lockedAccounts, campusFilter);
                deleteRequests = filterByCampus(deleteRequests, campusFilter);
            }

            request.setAttribute("isPrivilegedAdmin", privileged);
            request.setAttribute("campusFilter", campusFilter);
            request.setAttribute("campusOptions", adminITService.repo().getCampusNames());
            request.setAttribute("minorRestrictedKeywords", adminITService.repo().getMinorRestrictedKeywordsSetting());
            request.setAttribute("lockedAccounts", lockedAccounts);
            request.setAttribute("deleteRequests", deleteRequests);
            request.setAttribute("authLockAlerts", adminITService.repo().getAuthLockoutAlerts(adminId));
            request.getRequestDispatcher("/Admin/AdminAlerts.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Failed to load admin alerts", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load alerts page");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object adminIdObj = request.getSession(false) != null ? request.getSession(false).getAttribute("userID") : null;
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
        if (adminId <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }
        String action = param(request, "action");
        String campusFilter = param(request, "campus");
        String campusQuery = (campusFilter != null && !campusFilter.isEmpty()) ? ("&campus=" + encode(campusFilter)) : "";

        try {
            if ("unlockUser".equals(action)) {
                boolean ok = adminITService.repo().setAccountLock(adminId, req(request, "role"), parseInt(req(request, "id")), false);
                response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?msg=" + (ok ? "UserUnlocked" : "NoChange") + campusQuery);
                return;
            }

            if ("resolveDeleteRequest".equals(action)) {
                int requestId = parseInt(req(request, "deleteRequestID"));
                boolean approve = "approve".equalsIgnoreCase(req(request, "decision"));
                boolean resolved = adminITService.repo().resolveDeletionRequest(adminId, requestId, approve, param(request, "resolutionNote"));
                response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?msg=" + (resolved ? (approve ? "DeleteApproved" : "DeleteRejected") : "NoChange") + campusQuery);
                return;
            }

            if ("saveMinorKeywords".equals(action)) {
                boolean saved = adminITService.repo().updateMinorRestrictedKeywordsSetting(adminId, param(request, "minorRestrictedKeywords"));
                response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?msg=" + (saved ? "MinorKeywordsSaved" : "NoChange"));
                return;
            }

            response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?err=UnknownAction" + campusQuery);
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?err=MissingFields" + campusQuery);
        } catch (SQLException ex) {
            log("Admin alerts operation failed", ex);
            if (ex.getMessage() != null && ex.getMessage().contains("PrivilegedRequired")) {
                response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?err=PrivilegedRequired" + campusQuery);
                return;
            }
            if (ex.getMessage() != null && ex.getMessage().contains("CampusScopeDenied")) {
                response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?err=CampusScopeDenied" + campusQuery);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/AdminAlerts.do?err=OperationFailed" + campusQuery);
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

    private int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception ex) {
            return "";
        }
    }

    private List<Map<String, Object>> filterByCampus(List<Map<String, Object>> rows, String campus) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("campusName");
            String rowCampus = value == null ? "" : String.valueOf(value).trim();
            if (rowCampus.equalsIgnoreCase(campus)) {
                filtered.add(row);
            }
        }
        return filtered;
    }
}
