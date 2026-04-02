
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
import za.ac.tut.entities.Attendee;


public class AttendeeViewProfileServlet extends HttpServlet {

  
   
   @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        Integer attendeeID = (Integer) session.getAttribute("userID");

        // Safety check: redirect to login if session expired
        if (attendeeID == null) {
            response.sendRedirect("Login.jsp");
            return;
        }

        try {
            AttendeeService attendeeService = new AttendeeService();
            // Fetch the most recent data from the database
            Attendee attendee = attendeeService.repo().getAttendeeByID(attendeeID);
            
            if (attendee != null) {
                // Set the object in request scope for the JSP to access
                request.setAttribute("userProfile", attendee);
                request.getRequestDispatcher("Attendee/AttendeeUpdateProfile.jsp").forward(request, response);
            } else {
                response.sendRedirect("AttendeeDashboard.jsp");
            }
            
        } catch (SQLException e) {
            Logger.getLogger(AttendeeViewProfileServlet.class.getName()).log(Level.SEVERE, null, e);
            throw new ServletException("Error retrieving profile data", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        Integer attendeeID = (Integer) session.getAttribute("userID");

        // 1. Capture the data from the form textfields
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String tertiary = request.getParameter("tertiary");
        String clientType = request.getParameter("clientType");
        String phoneNumber = request.getParameter("phoneNumber");
        String studentNumber = request.getParameter("studentNumber");
        String idPassportNumber = request.getParameter("idPassportNumber");
        String biography = request.getParameter("biography");

        try {
            AttendeeService attendeeService = new AttendeeService();
            
            // 2. Fetch the current attendee to preserve password and QR code info
            Attendee a = attendeeService.repo().getAttendeeByID(attendeeID);
            
            if (a != null) {
                // 3. Update the fields with the new form data
                a.setFirstname(firstName);
                a.setLastname(lastName);
                a.setUsername(username);
                a.setEmail(email);
                a.setTertiaryInstitution(tertiary);
                a.setClientType(clientType);
                a.setPhoneNumber(phoneNumber);
                a.setStudentNumber(studentNumber);
                a.setIdPassportNumber(idPassportNumber);
                a.setBiography(biography);

                boolean isUpdated = attendeeService.repo().updateAttendee(a);

                if (isUpdated) {
                    // 4. Update the session display name so the header stays current
                    String displayName = (username != null && !username.trim().isEmpty())
                            ? username.trim().toLowerCase()
                            : firstName + " " + lastName;
                    session.setAttribute("userFullName", displayName);
                    request.setAttribute("message", "Profile successfully updated!");
                } else {
                    request.setAttribute("error", "Failed to update profile. Please try again.");
                }
            }
            
            // 5. Refresh the page by calling doGet to show updated values
            doGet(request, response);
            
        } catch (SQLException e) {
            Logger.getLogger(AttendeeViewProfileServlet.class.getName()).log(Level.SEVERE, null, e);
            request.setAttribute("error", "Database error: " + e.getMessage());
            doGet(request, response);
        }
    }

   
 
   

}
