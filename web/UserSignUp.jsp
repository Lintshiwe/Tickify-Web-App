<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="icon" type="image/x-icon" href="favicon.ico">
        <title>Tickify | Registration</title>
        <style>
            :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; }
            * { box-sizing:border-box; }
            body { margin:0; min-height:100vh; font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); background:radial-gradient(circle at 10% 0%, #eef8e9 0%, transparent 35%), var(--bg); display:flex; flex-direction:column; }
            header, footer { background:#fff; border-bottom:1px solid var(--line); text-align:center; padding:18px; }
            footer { margin-top:auto; border-top:1px solid var(--line); border-bottom:none; color:var(--muted); }
            .yellow { color:var(--green-dark); }
            main { flex:1; display:grid; place-items:center; padding:20px; }
            .form-container { width:min(560px,100%); background:#fff; border:1px solid var(--line); border-radius:20px; padding:24px; box-shadow:0 16px 30px rgba(90,130,90,.1); }
            .intro { margin:0 0 16px; color:var(--muted); text-align:center; }
            .status.error { background:#ffe8e8; color:#922; border:1px solid #efc4c4; border-radius:10px; padding:10px; margin-bottom:12px; font-weight:700; }
            .role-selection { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; border:1px solid var(--line); border-radius:12px; padding:12px; background:#fbfef9; margin-bottom:12px; }
            .input-group { margin-bottom:12px; display:flex; flex-direction:column; }
            label { font-weight:700; margin-bottom:6px; }
            input[type="text"], input[type="email"], input[type="password"], input[type="date"], textarea, select { padding:12px; border:1px solid #cfe2c9; border-radius:10px; font:inherit; }
            input[type="text"]:focus, input[type="email"]:focus, input[type="password"]:focus, input[type="date"]:focus, textarea:focus, select:focus { outline:none; border-color:var(--green); box-shadow:0 0 0 3px rgba(121,200,74,.2); }
            .btn-submit { width:100%; border:none; border-radius:12px; background:var(--green); color:#fff; font-weight:800; padding:12px; cursor:pointer; }
            .meta-links { display:flex; justify-content:space-between; margin-top:12px; }
            .meta-links a { color:var(--muted); text-decoration:none; }
            .hidden { display:none; }
            .hp-field { position:absolute; left:-10000px; width:1px; height:1px; overflow:hidden; }
            @media(max-width:768px){ input[type="text"], input[type="email"], input[type="password"], input[type="date"], textarea, select, .btn-submit{font-size:16px;} }
            @media(max-width:560px){ .role-selection{grid-template-columns:1fr;} }
        </style>
        <script>
            function toggleFields() {
                const isAttendee = document.getElementById('roleAttendee').checked;
                const attendeeSection = document.getElementById('attendeeFields');
                const presenterSection = document.getElementById('presenterFields');
                const lblFirst = document.getElementById('lblFirstname');
                const lblLast = document.getElementById('lblLastname');
                const lblEmail = document.getElementById('lblEmail');
                const lblPass = document.getElementById('lblPassword');
                if (isAttendee) {
                    attendeeSection.classList.remove('hidden'); presenterSection.classList.add('hidden');
                    lblFirst.innerText = 'Attendee First Name'; lblLast.innerText = 'Attendee Last Name';
                    lblEmail.innerText = 'Attendee Contact Email'; lblPass.innerText = 'Attendee Account Password';
                } else {
                    attendeeSection.classList.add('hidden'); presenterSection.classList.remove('hidden');
                    lblFirst.innerText = 'Tertiary Presenter First Name'; lblLast.innerText = 'Tertiary Presenter Last Name';
                    lblEmail.innerText = 'Professional Presenter Email'; lblPass.innerText = 'Presenter Security Password';
                }
            }
        </script>
    </head>
    <body onload="toggleFields()" data-interest="Account Registration">
        <header><h1><span class="yellow">TICKIFY</span> | REGISTRATION</h1></header>
        <main>
            <div class="form-container">
                <p class="intro">Create your Tickify account and start booking verified campus events.</p>
                <% String err = request.getParameter("err"); %>
                <% if (err != null) { %>
                    <div class="status error">
                        <%= "Validation".equals(err)
                                ? "Please complete all required fields with a valid email, username, and password."
                                : ("Duplicate".equals(err)
                                    ? "Username or email already exists for the selected role."
                                : ("RateLimit".equals(err)
                                    ? "Too many signup attempts. Please wait and retry."
                                    : "Registration could not be completed. Please try again.")) %>
                    </div>
                <% } %>
                <form action="RegistrationServlet.do" method="POST">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <div class="hp-field" aria-hidden="true"><label for="website">Website</label><input type="text" id="website" name="website" tabindex="-1" autocomplete="off"></div>
                    <fieldset class="role-selection" style="margin:0 0 12px;">
                        <legend style="font-weight:700; padding:0 6px; color:var(--green-dark);">Select account type</legend>
                        <label><input type="radio" name="userRole" id="roleAttendee" value="ATTENDEE" checked onclick="toggleFields()"> Attendee</label>
                        <label><input type="radio" name="userRole" id="rolePresenter" value="TERTIARY_PRESENTER" onclick="toggleFields()"> Presenter</label>
                    </fieldset>
                    <div class="input-group"><label id="lblFirstname" for="firstName">First Name</label><input type="text" id="firstName" name="firstname" autocomplete="given-name" required></div>
                    <div class="input-group"><label id="lblLastname" for="lastName">Last Name</label><input type="text" id="lastName" name="lastname" autocomplete="family-name" required></div>
                    <div class="input-group"><label for="username">Username</label><input type="text" id="username" name="username" autocomplete="username" pattern="[A-Za-z0-9_.-]{4,30}" title="Use 4-30 letters, numbers, dots, dashes, or underscores." required></div>
                    <div class="input-group"><label id="lblEmail" for="email">Email</label><input type="email" id="email" name="email" autocomplete="email" required></div>
                    <div class="input-group"><label id="lblPassword" for="password">Password</label><input type="password" id="password" name="password" autocomplete="new-password" required></div>
                    <div class="input-group"><label for="phoneNumber">Phone Number</label><input type="text" id="phoneNumber" name="phoneNumber" autocomplete="tel" required></div>
                    <div class="input-group"><label for="biography">Biography and Trace Details</label><textarea id="biography" name="biography" rows="3" placeholder="Tell us about yourself, your background, and any details that help verify your profile."></textarea></div>

                    <div id="attendeeFields">
                        <div class="input-group">
                            <label for="clientType">Client Type</label>
                            <select id="clientType" name="clientType" required>
                                <option value="STUDENT">Student</option>
                                <option value="GUEST">Guest</option>
                            </select>
                        </div>
                        <div class="input-group"><label for="attendeeInstitution">Which tertiary institution are you from: (Optional)</label><input type="text" id="attendeeInstitution" name="attendeeInstitution" autocomplete="organization"></div>
                        <div class="input-group"><label for="studentNumber">Student Number (Optional for guests)</label><input type="text" id="studentNumber" name="studentNumber" autocomplete="off"></div>
                        <div class="input-group"><label for="idPassportNumber">ID or Passport Number</label><input type="text" id="idPassportNumber" name="idPassportNumber" autocomplete="off" required></div>
                        <div class="input-group"><label for="dateOfBirth">Date of Birth</label><input type="date" id="dateOfBirth" name="dateOfBirth" autocomplete="bday" required></div>
                    </div>

                    <div id="presenterFields" class="hidden">
                        <div class="input-group"><label for="presenterInstitution">Which tertiary institution are you representing: (Required)</label><input type="text" id="presenterInstitution" name="presenterInstitution" autocomplete="organization"></div>
                    </div>
                    <button type="submit" class="btn-submit">CREATE ACCOUNT</button>
                </form>
                <div class="meta-links"><a href="${pageContext.request.contextPath}/Login.jsp">Already registered? Login</a><a href="${pageContext.request.contextPath}/UserSelection.jsp">Back to selection</a></div>
            </div>
        </main>
        <footer>&copy; 2026 <span class="yellow">Tickify</span> | Secure University Portal</footer>
        <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
        <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
    </body>
</html>
