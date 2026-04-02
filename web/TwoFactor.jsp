<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Tickify | Login 2FA</title>
        <style>
            body { font-family:"Trebuchet MS","Segoe UI",sans-serif; margin:0; min-height:100vh; display:grid; place-items:center; background:#f7faf6; color:#2f4033; }
            .card { width:min(460px,100%); background:#fff; border:1px solid #d8e5d5; border-radius:16px; padding:22px; box-shadow:0 14px 28px rgba(90,130,90,.1); }
            h1 { margin:0 0 8px; color:#5ca833; font-size:1.5rem; }
            .hint { color:#657768; margin:0 0 14px; }
            .status, .error { border-radius:10px; padding:10px; margin-bottom:10px; font-weight:700; }
            .status { background:#eef8e9; color:#2f7f20; border:1px solid #cce0c5; }
            .error { background:#ffe8e8; color:#922; border:1px solid #efc4c4; }
            .input { width:100%; font-size:1.2rem; letter-spacing:4px; text-align:center; padding:12px; border:1px solid #cfe2c9; border-radius:10px; }
            .btn { width:100%; margin-top:12px; border:none; border-radius:10px; padding:12px; background:#79c84a; color:#fff; font-weight:800; cursor:pointer; }
            a { display:inline-block; margin-top:12px; color:#6f7d73; text-decoration:none; }
        </style>
    </head>
    <body>
        <div class="card">
            <h1>Two-Factor Verification</h1>
            <p class="hint">Enter the 6-digit code sent to your email.</p>
            <% String msg = request.getParameter("msg"); %>
            <% String err = request.getParameter("err"); %>
            <% if ("CodeSent".equals(msg)) { %><div class="status">Verification code sent.</div><% } %>
            <% if ("CodeInvalid".equals(err)) { %><div class="error">Invalid code. Try again.</div><% } %>
            <% if ("CodeExpired".equals(err)) { %><div class="error">Code expired. Please log in again.</div><% } %>
            <form action="${pageContext.request.contextPath}/VerifyLogin2FA.do" method="POST">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input class="input" type="text" name="code" maxlength="6" pattern="[0-9]{6}" inputmode="numeric" required>
                <button class="btn" type="submit">Verify & Continue</button>
            </form>
            <a href="${pageContext.request.contextPath}/Login.jsp">Back to Login</a>
        </div>
    </body>
</html>
