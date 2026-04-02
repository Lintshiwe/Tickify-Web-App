package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.attendee.AttendeeService;

/**
 * Servlet to handle the permanent deletion of an Attendee profile.
 */
public class AttendeeDeleteProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // We route both GET and POST to the same logic for this action
        processDeletion(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processDeletion(request, response);
    }

    /**
     * Logic to remove user from DB and clear session.
     */
    protected void processDeletion(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Get current session without creating a new one
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userID") != null) {
            int attendeeID = (int) session.getAttribute("userID");

            try {
                AttendeeService attendeeService = new AttendeeService();

                // 2. Execute deletion in the Database
                boolean isDeleted = attendeeService.repo().deleteAttendee(attendeeID);

                if (isDeleted) {
                    // 3. IMPORTANT: Kill the session so the user is logged out
                    session.invalidate();

                    // 4. Redirect to index.html with the message for your JavaScript
                    response.sendRedirect("index.html?msg=AccountDeleted");
                } else {
                    // If DB fails to delete, send them back to the dashboard with an error
                    response.sendRedirect("AttendeeDashboard.do?error=DeleteFailed");
                }

            } catch (SQLException e) {
                Logger.getLogger(AttendeeDeleteProfileServlet.class.getName()).log(Level.SEVERE, "Deletion Error", e);
                // Redirect back to dashboard if a database error occurs
                response.sendRedirect("AttendeeDashboard.do?error=DatabaseError");
            }
        } else {
            // No valid session found, send to landing page
            response.sendRedirect("index.html");
        }
    }

    @Override
    public String getServletInfo() {
        return "Handles permanent account removal for Attendees.";
    }
}
