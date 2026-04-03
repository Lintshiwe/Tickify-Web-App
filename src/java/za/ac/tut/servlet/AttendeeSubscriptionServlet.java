package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.engagement.EngagementCampaignService;

public class AttendeeSubscriptionServlet extends HttpServlet {

    private final EngagementCampaignService engagementService = new EngagementCampaignService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null || attendeeId.intValue() <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        String action = request.getParameter("action");
        boolean subscribed = "subscribe".equalsIgnoreCase(action) || "enable".equalsIgnoreCase(action);
        if (!subscribed && !"unsubscribe".equalsIgnoreCase(action) && !"disable".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=SubscriptionActionInvalid");
            return;
        }

        try {
            boolean ok = engagementService.setSubscription(attendeeId.intValue(), subscribed);
            if (!ok) {
                response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=SubscriptionUpdateFailed");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?msg="
                    + (subscribed ? "Subscribed" : "Unsubscribed"));
        } catch (SQLException ex) {
            log("Unable to update attendee subscription", ex);
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=SubscriptionUpdateFailed");
        }
    }
}
