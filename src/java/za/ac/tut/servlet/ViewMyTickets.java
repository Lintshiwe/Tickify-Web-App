/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
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

/**
 *
 * @author ntoam
 */
public class ViewMyTickets extends HttpServlet {

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
            List<Map<String, Object>> tickets = attendeeService.repo().getAttendeeTickets(attendeeId);
            request.setAttribute("tickets", tickets);
            request.getRequestDispatcher("/Attendee/MyTickets.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Unable to load attendee tickets", e);
            response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do?err=TicketLoadFailed");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Routes attendee ticket view to dashboard flow";
    }

}
