<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="icon" type="image/x-icon" href="favicon.ico">
        <title>Tickify | Client Password Reset</title>
        <style>
            :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; --err:#922; --err-bg:#ffe8e8; --ok:#2f7f20; --ok-bg:#eef8e9; }
            * { box-sizing:border-box; }
            body { margin:0; min-height:100vh; font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); background:radial-gradient(circle at 10% 0%, #eef8e9 0%, transparent 35%), var(--bg); display:flex; flex-direction:column; }
            header, footer { background:#fff; border-bottom:1px solid var(--line); text-align:center; padding:18px; }
            footer { margin-top:auto; border-top:1px solid var(--line); border-bottom:none; color:var(--muted); }
            .gold { color:var(--green-dark); }
            main { flex:1; display:grid; place-items:center; padding:20px; }
            .card { width:min(700px,100%); background:#fff; border:1px solid var(--line); border-radius:20px; padding:24px; box-shadow:0 16px 30px rgba(90,130,90,.1); }
            h2 { margin:0 0 8px; }
            p { color:var(--muted); }
            .status, .error { border-radius:10px; padding:10px; margin-bottom:12px; font-weight:700; }
            .status { background:var(--ok-bg); color:var(--ok); border:1px solid #cce0c5; }
            .error { background:var(--err-bg); color:var(--err); border:1px solid #efc4c4; }
            .fallback { margin-bottom:12px; padding:10px; border-radius:10px; border:1px solid #cfe2c9; background:#f2faf0; }
            .fallback a { color:#2f7f20; word-break:break-all; font-weight:700; }
            .panel { border:1px solid var(--line); border-radius:12px; padding:14px; margin-top:10px; background:#fbfef9; }
            .input-group { margin-bottom:12px; }
            label { display:block; font-weight:700; margin-bottom:6px; }
            input[type="text"], input[type="password"], select, textarea { width:100%; padding:12px; border:1px solid #cfe2c9; border-radius:10px; background:#fff; font:inherit; }
            .btn { border:none; border-radius:12px; background:var(--green); color:#fff; font-weight:800; padding:12px 16px; cursor:pointer; }
            .btn-row { display:flex; gap:8px; flex-wrap:wrap; }
            .meta-links { display:flex; justify-content:space-between; margin-top:12px; }
            .meta-links a { color:var(--muted); text-decoration:none; }
            @media(max-width:768px){ input[type="text"], input[type="password"], .btn { font-size:16px; } }
        </style>
    </head>
    <body>
        <header><h1><span class="gold">TICKIFY</span> CLIENT PASSWORD RESET</h1></header>
        <main>
            <div class="card">
                <p>Request a signed reset token by email and complete password reset for attendee or presenter accounts.</p>

                <c:if test="${not empty error}"><div class="error">${error}</div></c:if>
                <c:if test="${not empty status}"><div class="status">${status}</div></c:if>
                <c:if test="${not empty fallbackResetLink}">
                    <div class="fallback">
                        <div><strong>${fallbackHint}</strong></div>
                        <a href="${fallbackResetLink}">${fallbackResetLink}</a>
                    </div>
                </c:if>

                <div class="panel">
                    <h2>1. Send Reset Email</h2>
                    <form action="ClientPasswordReset.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="requestToken">
                        <div class="input-group">
                            <label for="userRole">Client Role</label>
                            <select id="userRole" name="userRole" required>
                                <option value="ATTENDEE">Attendee</option>
                                <option value="TERTIARY_PRESENTER">Presenter</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label for="identifier">Username or Email</label>
                            <input type="text" id="identifier" name="identifier" required>
                        </div>
                        <button class="btn" type="submit">Send Reset Email</button>
                    </form>
                </div>

                <div class="panel">
                    <h2>2. Set New Password</h2>
                    <c:choose>
                        <c:when test="${tokenValid == true}">
                            <p>Token accepted for role ${tokenRole} user #${tokenUserId}.</p>
                            <form action="ClientPasswordReset.do" method="POST">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="resetPassword">
                                <input type="hidden" name="token" value="${token}">
                                <div class="input-group">
                                    <label for="newPassword">New Password</label>
                                    <input type="password" id="newPassword" name="newPassword" minlength="8" required>
                                </div>
                                <div class="input-group">
                                    <label for="confirmPassword">Confirm Password</label>
                                    <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" required>
                                </div>
                                <div class="btn-row">
                                    <button class="btn" type="submit">Reset Password</button>
                                </div>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p>Open this page using a valid reset link containing a token to complete step 2.</p>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="meta-links">
                    <a href="Login.jsp">Back to login</a>
                    <a href="UserSelection.jsp">Back to selection</a>
                </div>
            </div>
        </main>
        <footer>&copy; 2026 <span class="gold">Tickify</span> | Secure University Portal</footer>
        <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
        <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
    </body>
</html>
