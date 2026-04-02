<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="icon" type="image/x-icon" href="favicon.ico">
        <title>Tickify | User Selection</title>
        <style>
            :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; }
            * { box-sizing:border-box; }
            body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:var(--bg); color:var(--ink); min-height:100vh; display:flex; flex-direction:column; }
            header, footer { background:#fff; border-bottom:1px solid var(--line); text-align:center; padding:18px; }
            footer { border-top:1px solid var(--line); border-bottom:none; margin-top:auto; color:var(--muted); }
            .brand { color:var(--green-dark); }
            main { flex:1; display:grid; place-items:center; padding:20px; }
            .shell { width:min(960px,100%); background:#fff; border:1px solid var(--line); border-radius:22px; padding:24px; box-shadow:0 16px 28px rgba(90,130,90,.08); }
            .intro { text-align:center; color:var(--muted); margin:0 0 18px; }
            .grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
            .card { border:1px solid var(--line); border-radius:16px; background:#fbfef9; padding:22px; text-align:center; }
            .card h2 { margin:0 0 10px; color:var(--green-dark); }
            .card p { margin:0 0 16px; color:var(--muted); }
            .btn { display:inline-block; text-decoration:none; background:var(--green); color:#fff; font-weight:800; padding:12px 24px; border-radius:12px; }
            @media(max-width:780px){ .grid{grid-template-columns:1fr;} }
        </style>
    </head>
    <body data-interest="Campus Events">
        <header><h1><span class="brand">TICKIFY</span> | SELECT ACTION</h1></header>
        <main>
            <div class="shell">
                <p class="intro">Choose your path to access events, tickets, and campus experiences.</p>
                <div class="grid">
                    <section class="card">
                        <h2>Returning User</h2>
                        <p>Sign in to manage your bookings and account details.</p>
                        <a href="Login.jsp" class="btn">LOGIN</a>
                    </section>
                    <section class="card">
                        <h2>New User</h2>
                        <p>Create your account and start booking in minutes.</p>
                        <a href="UserSignUp.jsp" class="btn">SIGN UP</a>
                    </section>
                </div>
            </div>
        </main>
        <footer>&copy; 2026 <span class="brand">TICKIFY</span> - Secure University Management</footer>
        <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
        <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
    </body>
</html>
