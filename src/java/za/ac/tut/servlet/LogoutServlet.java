package za.ac.tut.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Universal Logout Servlet to clear session data for any user type.
 */
public class LogoutServlet extends HttpServlet {

    /**
     * Handles logout for both GET and POST requests.
     */
    protected void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // 1. Get the current session if it exists (don't create a new one)
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            // 2. Clear all session attributes and kill the session
            session.invalidate();
        }
        
        // 3. Prevent the browser from caching protected pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies
        
        // 4. Redirect to the landing page or login page
        response.sendRedirect("index.html?status=loggedOut"); 
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Clears session and logs out users.";
    }
}