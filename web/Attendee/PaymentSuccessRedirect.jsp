<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Payment Complete</title>
    <style>
        :root { --green:#7fc342; --green-dark:#4f8f2b; --line:#d7e2d1; --ink:#223028; --muted:#5d6a60; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:#f7faf6; color:var(--ink); }
        .wrap { min-height:100vh; width:100%; display:grid; place-items:center; padding:20px clamp(12px,2.7vw,36px); }
        .card {
            background:#fff; border:1px solid var(--line); border-radius:18px; padding:22px;
            box-shadow:0 14px 34px rgba(33,47,32,.1); max-width:760px; width:100%; text-align:center;
        }
        .btn-row { display:flex; gap:10px; justify-content:center; flex-wrap:wrap; margin-top:14px; }
        .btn { display:inline-block; padding:10px 14px; border-radius:10px; font-weight:800; text-decoration:none; border:1px solid var(--line); color:#2f3a32; background:#fff; }
        .btn.primary { border-color:var(--green); background:var(--green); color:#fff; }
        .status { margin:8px 0 0; color:var(--muted); }
        .ref { margin:8px 0 0; color:var(--muted); }

        .anim-wrap { margin:2px auto 14px; width:92px; height:92px; position:relative; }
        .spinner {
            width:92px; height:92px; border-radius:50%;
            border:6px solid #e6efe2; border-top-color:var(--green);
            animation:spin 1s linear infinite;
        }
        .check-badge {
            position:absolute; inset:0; display:none; align-items:center; justify-content:center;
        }
        .check-ring {
            width:92px; height:92px; border-radius:50%; background:#eef8e9; border:2px solid #cde3c0;
            transform:scale(.84); opacity:0; animation:ringPop .45s ease forwards;
        }
        .check-svg {
            position:absolute; width:52px; height:52px;
        }
        .check-path {
            fill:none; stroke:var(--green-dark); stroke-width:6; stroke-linecap:round; stroke-linejoin:round;
            stroke-dasharray:72; stroke-dashoffset:72; animation:drawCheck .42s .12s ease forwards;
        }
        .hidden { display:none !important; }

        .confetti {
            position:absolute; inset:0; pointer-events:none; display:none;
        }
        .confetti i {
            position:absolute; top:44px; left:44px; width:8px; height:12px; border-radius:2px;
            opacity:0;
            transform:translate(0,0) rotate(0deg);
            animation:burst .85s ease-out forwards;
        }
        .confetti i:nth-child(1){ background:#7fc342; animation-delay:.02s; --dx:-34px; --dy:-42px; }
        .confetti i:nth-child(2){ background:#a7d95f; animation-delay:.04s; --dx:-12px; --dy:-48px; }
        .confetti i:nth-child(3){ background:#4f8f2b; animation-delay:.06s; --dx:14px; --dy:-46px; }
        .confetti i:nth-child(4){ background:#d8ebbf; animation-delay:.08s; --dx:36px; --dy:-34px; }
        .confetti i:nth-child(5){ background:#7fc342; animation-delay:.10s; --dx:42px; --dy:-8px; }
        .confetti i:nth-child(6){ background:#4f8f2b; animation-delay:.12s; --dx:34px; --dy:20px; }
        .confetti i:nth-child(7){ background:#a7d95f; animation-delay:.14s; --dx:10px; --dy:34px; }
        .confetti i:nth-child(8){ background:#d8ebbf; animation-delay:.16s; --dx:-14px; --dy:36px; }
        .confetti i:nth-child(9){ background:#7fc342; animation-delay:.18s; --dx:-32px; --dy:20px; }
        .confetti i:nth-child(10){ background:#4f8f2b; animation-delay:.20s; --dx:-40px; --dy:-8px; }

        @keyframes spin { to { transform:rotate(360deg); } }
        @keyframes ringPop { to { transform:scale(1); opacity:1; } }
        @keyframes drawCheck { to { stroke-dashoffset:0; } }
        @keyframes burst {
            0% { opacity:0; transform:translate(0,0) rotate(0deg) scale(.3); }
            15% { opacity:1; }
            100% { opacity:0; transform:translate(var(--dx), var(--dy)) rotate(240deg) scale(1); }
        }
    </style>
</head>
<body>
    <div class="wrap">
        <div class="card">
            <h2 style="margin:0 0 8px;">Payment Complete</h2>
            <div class="anim-wrap" aria-live="polite" aria-busy="true">
                <div id="spinner" class="spinner"></div>
                <div id="checkBadge" class="check-badge">
                    <div class="check-ring"></div>
                    <svg class="check-svg" viewBox="0 0 52 52" aria-hidden="true">
                        <path class="check-path" d="M14 27 L23 36 L39 18"></path>
                    </svg>
                </div>
                <div id="confetti" class="confetti" aria-hidden="true">
                    <i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i>
                </div>
            </div>

            <p id="statusText" class="status">Finalizing purchase and loading your tickets...</p>
            <p class="ref">Reference: <strong>${transactionRef}</strong></p>
            <p class="status">You will be redirected to your tickets automatically.</p>
            <div class="btn-row">
                <a class="btn primary" href="${nextUrl}">Open Tickets</a>
                <a class="btn" href="${nextUrl}">Go to My Tickets</a>
            </div>
        </div>
    </div>

    <script>
        (function () {
            var nextUrl = "${nextUrl}";
            var spinner = document.getElementById("spinner");
            var checkBadge = document.getElementById("checkBadge");
            var confetti = document.getElementById("confetti");
            var statusText = document.getElementById("statusText");

            setTimeout(function () {
                if (spinner) {
                    spinner.classList.add("hidden");
                }
                if (checkBadge) {
                    checkBadge.style.display = "flex";
                }
                if (confetti) {
                    confetti.style.display = "block";
                }
                if (statusText) {
                    statusText.textContent = "Purchase confirmed. Redirecting to your tickets...";
                }
            }, 1100);

            setTimeout(function () {
                window.location.href = nextUrl;
            }, 2600);
        })();
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
