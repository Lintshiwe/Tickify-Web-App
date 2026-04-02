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
            header { background:#fff; border-bottom:1px solid var(--line); text-align:center; padding:18px; }
            .site-footer { margin-top:auto; background:#050607; color:#d8dee7; padding:28px 22px 16px; }
            .site-footer a { color:#d8dee7; text-decoration:none; }
            .footer-top { display:grid; grid-template-columns:2fr 1fr 1fr 1.3fr; gap:18px; max-width:1200px; margin:0 auto; }
            .footer-brand img { width:120px; height:auto; margin-bottom:8px; }
            .footer-brand p { margin:0 0 8px; color:#c9d2dc; }
            .footer-title { color:#ffffff; font-size:.95rem; font-weight:800; margin:0 0 8px; }
            .footer-list { display:grid; gap:8px; }
            .social-row { display:flex; gap:10px; margin-top:8px; color:#95e21a; }
            .footer-bottom { border-top:1px solid #2a2f36; margin-top:16px; padding-top:12px; max-width:1200px; margin-left:auto; margin-right:auto; color:#98a4b2; font-size:.9rem; }
            .brand { color:var(--green-dark); }
            main { flex:1; display:grid; place-items:center; padding:20px; }
            .shell { width:min(960px,100%); background:#fff; border:1px solid var(--line); border-radius:22px; padding:24px; box-shadow:0 16px 28px rgba(90,130,90,.08); }
            .intro { text-align:center; color:var(--muted); margin:0 0 18px; }
            .grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
            .card { border:1px solid var(--line); border-radius:16px; background:#fbfef9; padding:22px; text-align:center; }
            .card h2 { margin:0 0 10px; color:var(--green-dark); }
            .card p { margin:0 0 16px; color:var(--muted); }
            .btn { display:inline-block; text-decoration:none; background:var(--green); color:#fff; font-weight:800; padding:12px 24px; border-radius:12px; }
            @media(max-width:900px){ .footer-top{grid-template-columns:1fr 1fr;} }
            @media(max-width:780px){ .grid{grid-template-columns:1fr;} }
            @media(max-width:560px){ .footer-top{grid-template-columns:1fr;} }
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
                        <a href="${pageContext.request.contextPath}/Login.jsp" class="btn">LOGIN</a>
                    </section>
                    <section class="card">
                        <h2>New User</h2>
                        <p>Create your account and start booking in minutes.</p>
                        <a href="${pageContext.request.contextPath}/UserSignUp.jsp" class="btn">SIGN UP</a>
                    </section>
                </div>
            </div>
        </main>
        <footer class="site-footer">
            <div class="footer-top">
                <section class="footer-brand">
                    <img src="${pageContext.request.contextPath}/assets/tickify-logo.svg" alt="Tickify">
                    <p>Tickify is owned and operated by Adventor Global Limited.</p>
                    <p>Trade License: TRAD/DNCC/141845/2022</p>
                    <div class="social-row"><span>f</span><span>ig</span><span>yt</span><span>tt</span><span>wa</span></div>
                </section>
                <section>
                    <h3 class="footer-title">MORE INFO</h3>
                    <div class="footer-list">
                        <a href="#">Contact us</a>
                        <a href="#">FAQ</a>
                    </div>
                </section>
                <section>
                    <h3 class="footer-title">LEGALS</h3>
                    <div class="footer-list">
                        <a href="#">Terms and Conditions</a>
                        <a href="#">Privacy Policy</a>
                        <a href="#">Refund Policy</a>
                    </div>
                </section>
                <section>
                    <h3 class="footer-title">CONTACTS</h3>
                    <div class="footer-list">
                        <span>House 6, Road 16, Block D, Mirpur 6</span>
                        <span>+88 018 35099 555</span>
                        <span>tickify.live@gmail.com</span>
                    </div>
                </section>
            </div>
            <div class="footer-bottom">2026 &copy; Tickify | Owned and Operated by Adventor Global Limited</div>
        </footer>
        <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
        <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
    </body>
</html>
