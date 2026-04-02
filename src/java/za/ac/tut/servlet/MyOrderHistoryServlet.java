package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.attendee.AttendeeService;

public class MyOrderHistoryServlet extends HttpServlet {

    private final AttendeeService attendeeService = new AttendeeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp");
            return;
        }

        try {
            List<Map<String, Object>> orders = attendeeService.repo().getAttendeeOrderHistory(attendeeId);
            request.setAttribute("orders", orders);
            request.getRequestDispatcher("/Attendee/MyOrderHistory.jsp").forward(request, response);
        } catch (SQLException ex) {
            log("Unable to load order history", ex);
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=OrderHistoryLoadFailed");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
