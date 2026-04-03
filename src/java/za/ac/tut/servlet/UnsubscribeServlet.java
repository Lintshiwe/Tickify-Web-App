package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.engagement.EngagementCampaignService;

public class UnsubscribeServlet extends HttpServlet {

    private final EngagementCampaignService engagementService = new EngagementCampaignService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        boolean ok = false;

        if (token != null && !token.trim().isEmpty()) {
            try {
                ok = engagementService.unsubscribeByToken(token.trim());
            } catch (SQLException ex) {
                log("Unable to process unsubscribe token", ex);
            }
        }

        response.setContentType("text/html;charset=UTF-8");
        String title = ok ? "You are unsubscribed" : "Unsubscribe link invalid";
        String message = ok
                ? "You will no longer receive Tickify campaign and event-update emails."
                : "This unsubscribe link is invalid or already expired.";

        response.getWriter().write("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>Tickify Unsubscribe</title></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;background:#f4f8f3;color:#1f2c22;padding:24px;'>"
                + "<div style='max-width:620px;margin:0 auto;background:#fff;border:1px solid #d8e5d2;border-radius:14px;padding:22px;'>"
                + "<h1 style='margin:0 0 10px;color:#2a5634;'>" + escapeHtml(title) + "</h1>"
                + "<p style='margin:0;color:#44554b;line-height:1.6;'>" + escapeHtml(message) + "</p>"
                + "</div></body></html>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
