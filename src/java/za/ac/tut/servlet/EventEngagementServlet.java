package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.databaseManagement.EventEngagementDAO;

public class EventEngagementServlet extends HttpServlet {

    private final EventEngagementDAO dao = new EventEngagementDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        String role = session != null ? String.valueOf(session.getAttribute("userRole")) : null;
        Integer userId = null;
        if (session != null && session.getAttribute("userID") instanceof Integer) {
            userId = (Integer) session.getAttribute("userID");
        }

        int eventId;
        try {
            eventId = Integer.parseInt(request.getParameter("eventID"));
        } catch (Exception ex) {
            writeJson(response, false);
            return;
        }

        String action = request.getParameter("action");
        String channel = request.getParameter("channel");

        try {
            boolean ok = dao.logEventAction(eventId, role, userId, action, channel);
            writeJson(response, ok);
        } catch (SQLException ex) {
            log("Failed to log event engagement", ex);
            writeJson(response, false);
        }
    }

    private void writeJson(HttpServletResponse response, boolean ok) throws IOException {
        response.getWriter().write("{\"ok\":" + ok + "}");
    }
}
