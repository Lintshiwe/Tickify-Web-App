package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.user.UserAccountService;
import za.ac.tut.application.user.UserAccountService.ClientAccount;
import za.ac.tut.security.EmailVerificationTokenUtil;

public class VerifyEmailServlet extends HttpServlet {

    private final UserAccountService userAccountService = new UserAccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerifyInvalid");
            return;
        }

        EmailVerificationTokenUtil.TokenPayload payload = EmailVerificationTokenUtil.verify(token);
        if (payload == null || !userAccountService.isClientRole(payload.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerifyInvalid");
            return;
        }

        try {
            ClientAccount account = userAccountService.findClientByRoleAndId(payload.getRole(), payload.getUserId());
            if (account == null || account.getEmail() == null
                    || !account.getEmail().trim().equalsIgnoreCase(payload.getEmail())) {
                response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerifyInvalid");
                return;
            }

            boolean updated = userAccountService.verifyClientEmailAddress(payload.getRole(), payload.getUserId(), payload.getEmail());
            if (updated) {
                response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerified");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerifyInvalid");
        } catch (SQLException ex) {
            log("Failed to verify email", ex);
            response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=EmailVerifyInvalid");
        }
    }
}
