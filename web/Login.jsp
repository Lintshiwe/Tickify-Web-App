<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="manifest" href="manifest.webmanifest">
        <link rel="icon" type="image/x-icon" href="favicon.ico">
        <title>Tickify | Login</title>
        <style>
            :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; }
            * { box-sizing:border-box; }
            body { margin:0; min-height:100vh; font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); background:radial-gradient(circle at 10% 0%, #eef8e9 0%, transparent 35%), var(--bg); display:flex; flex-direction:column; }
            header { background:#fff; border-bottom:1px solid var(--line); text-align:center; padding:18px; }
            .site-footer { margin-top:auto; background:#050607; color:#d8dee7; padding:28px 22px 16px; }
            .site-footer a { color:#d8dee7; text-decoration:none; }
            .footer-bottom { border-top:1px solid #2a2f36; margin-top:16px; padding-top:12px; max-width:1200px; margin-left:auto; margin-right:auto; color:#98a4b2; font-size:.9rem; }
            .footer-bottom .builder-label { color:#d8dee7; font-weight:700; }
            .footer-bottom .owner-link { display:inline-block; margin-top:4px; color:#9be552; font-weight:800; text-decoration:underline; }
            .gold { color:var(--green-dark); }
            main { flex:1; display:grid; place-items:center; padding:20px; }
            .card { width:min(560px,100%); background:#fff; border:1px solid var(--line); border-radius:20px; padding:24px; box-shadow:0 16px 30px rgba(90,130,90,.1); }
            .intro { margin:0 0 16px; color:var(--muted); text-align:center; }
            .status, .error { border-radius:10px; padding:10px; margin-bottom:12px; text-align:center; font-weight:700; }
            .status { background:#eef8e9; color:#3c7f20; border:1px solid #cce0c5; }
            .error { background:#ffe8e8; color:#922; border:1px solid #efc4c4; }
            .role-selector { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; border:1px solid var(--line); border-radius:12px; padding:12px; background:#fbfef9; margin-bottom:14px; }
            .role-option { display:flex; align-items:center; gap:6px; font-weight:700; font-size:.9rem; }
            .input-group { margin-bottom:12px; }
            label { display:block; font-weight:700; margin-bottom:6px; }
            input[type="text"], input[type="password"] { width:100%; padding:12px; border:1px solid #cfe2c9; border-radius:10px; background:#fff; }
            input[type="text"]:focus, input[type="password"]:focus { outline:none; border-color:var(--green); box-shadow:0 0 0 3px rgba(121,200,74,.2); }
            .btn-login { width:100%; border:none; border-radius:12px; background:var(--green); color:#fff; font-weight:800; padding:12px; cursor:pointer; margin-top:4px; }
            .back-link { display:inline-block; margin-top:12px; color:var(--muted); text-decoration:none; }
            @media(max-width:768px){ input[type="text"], input[type="password"], .btn-login{font-size:16px;} }
            @media(max-width:560px){ .role-selector{grid-template-columns:1fr;} }
        </style>
        <script>
            function updatePlaceholders() {
                const selectedRole = document.querySelector('input[name="userRole"]:checked').value;
                const loginInput = document.getElementById('loginField');
                const passInput = document.getElementById('passwordField');
                const loginLabel = document.getElementById('loginLabel');
                const passLabel = document.getElementById('passLabel');
                const roleNames = {
                    'ATTENDEE': 'Attendee',
                    'TERTIARY_PRESENTER': 'Presenter',
                    'EVENT_MANAGER': 'Manager',
                    'VENUE_GUARD': 'Venue Guard',
                    'ADMIN': 'Administrator'
                };
                const friendlyName = roleNames[selectedRole];
                loginLabel.innerText = friendlyName + ' Username or Email';
                passLabel.innerText = friendlyName + ' Password';
                loginInput.placeholder = 'Enter your ' + friendlyName.toLowerCase() + ' username or email';
                passInput.placeholder = 'Enter your ' + friendlyName.toLowerCase() + ' password';
            }
        </script>
    </head>
    <body onload="updatePlaceholders()" data-interest="Account Access">
        <header><h1><span class="gold">TICKIFY</span> PORTAL LOGIN</h1></header>
        <main>
            <div class="card">
                <p class="intro">Secure sign in for all Tickify roles.</p>
                <% String msg = request.getParameter("msg"); %>
                <% if ("RegSuccess".equals(msg)) { %>
                    <div class="status">Registration successful. You can now sign in.</div>
                <% } else if ("VerifyEmailSent".equals(msg)) { %>
                    <div class="status">Registration successful. Check your inbox and verify your email before signing in.</div>
                <% } else if ("VerifyEmailPending".equals(msg)) { %>
                    <div class="status">Registration successful, but verification email could not be sent. Use password reset after SMTP is fixed, then verify.</div>
                <% } else if ("EmailVerified".equals(msg)) { %>
                    <div class="status">Email verified successfully. You can now sign in.</div>
                <% } else if ("EmailVerifyInvalid".equals(msg)) { %>
                    <div class="error">Verification link is invalid or expired. Request a new verification email by re-registering or contacting support.</div>
                <% } else if ("ResetSuccess".equals(msg)) { %>
                    <div class="status">Password reset successful. Sign in with your new password.</div>
                <% } %>
                <% if(request.getAttribute("error") != null) { %>
                    <div class="error"><%= request.getAttribute("error") %></div>
                <% } %>
                <form action="${pageContext.request.contextPath}/LoginServlet.do" method="POST">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <fieldset class="role-selector" style="margin:0 0 14px;">
                        <legend style="font-weight:700; padding:0 6px; color:var(--green-dark);">Select Role</legend>
                        <label class="role-option"><input type="radio" name="userRole" value="ATTENDEE" checked onclick="updatePlaceholders()"> Attendee</label>
                        <label class="role-option"><input type="radio" name="userRole" value="TERTIARY_PRESENTER" onclick="updatePlaceholders()"> Presenter</label>
                        <label class="role-option"><input type="radio" name="userRole" value="EVENT_MANAGER" onclick="updatePlaceholders()"> Manager</label>
                        <label class="role-option"><input type="radio" name="userRole" value="VENUE_GUARD" onclick="updatePlaceholders()"> Guard</label>
                        <label class="role-option"><input type="radio" name="userRole" value="ADMIN" onclick="updatePlaceholders()"> Admin</label>
                    </fieldset>
                    <div class="input-group"><label id="loginLabel" for="loginField">Username or Email</label><input type="text" name="loginId" id="loginField" autocomplete="username" required></div>
                    <div class="input-group"><label id="passLabel" for="passwordField">Password</label><input type="password" name="password" id="passwordField" autocomplete="current-password" required></div>
                    <button type="submit" class="btn-login">ACCESS PORTAL</button>
                </form>
                <div style="margin-top:10px;text-align:right;">
                    <a class="back-link" href="${pageContext.request.contextPath}/ClientPasswordReset.jsp">Forgot attendee/presenter password?</a>
                </div>
                <a class="back-link" href="${pageContext.request.contextPath}/UserSelection.jsp">&larr; Back to selection</a>
            </div>
        </main>
        <footer class="site-footer">
            <div class="footer-bottom">
                <span class="builder-label">Built by Letsoperate</span><br>
                <a class="owner-link" href="https://letsoperate.vercel.app/" target="_blank" rel="noopener noreferrer">Letsoperate</a>
                <br>
                <span>2026 &copy; Tickify | Owned by Letsoperate organization</span>
            </div>
        </footer>
        <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
        <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
        <script src="${pageContext.request.contextPath}/assets/pwa-register.js"></script>
    </body>
</html>
