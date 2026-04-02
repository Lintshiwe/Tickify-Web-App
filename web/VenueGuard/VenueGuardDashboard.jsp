<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Security Scanner</title>
    <style>
        :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; --ok:#1f7c39; --fail:#9b1c1c; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:var(--bg); color:var(--ink); }
        .site-header {
            width:100%;
            background:#f7f8f6;
            border-bottom:1px solid #dfe5dc;
            position:sticky;
            top:0;
            z-index:30;
        }
        .header-inner {
            width:100%;
            max-width:none;
            margin:0;
            padding:14px clamp(12px,2.7vw,36px);
        }
        .header-top {
            display:flex;
            justify-content:space-between;
            align-items:center;
            gap:14px;
            flex-wrap:wrap;
        }
        .brand {
            display:flex;
            align-items:center;
            gap:12px;
            text-decoration:none;
        }
        .brand-logo {
            height:58px;
            width:auto;
            display:block;
        }
        .profile-wrap { position:relative; }
        .profile-btn {
            display:flex;
            align-items:center;
            gap:10px;
            border:1px solid #d7ded3;
            background:#fff;
            border-radius:999px;
            padding:8px 10px;
            color:#2a312b;
            font-weight:700;
            cursor:pointer;
        }
        .profile-meta {
            max-width:0;
            opacity:0;
            overflow:hidden;
            white-space:nowrap;
            transition:max-width .28s ease, opacity .22s ease;
        }
        .profile-wrap:hover .profile-meta,
        .profile-wrap:focus-within .profile-meta {
            max-width:260px;
            opacity:1;
        }
        .profile-icon {
            width:28px;
            height:28px;
            border-radius:50%;
            background:#e6eedf;
            display:flex;
            align-items:center;
            justify-content:center;
            color:#4c5b4b;
            font-weight:800;
        }
        .profile-menu {
            position:absolute;
            right:0;
            top:calc(100% + 10px);
            min-width:220px;
            background:#fff;
            border:1px solid #dee5da;
            border-radius:12px;
            box-shadow:0 14px 26px rgba(24,32,20,.12);
            padding:8px;
            display:none;
        }
        .profile-menu.open { display:block; }
        .profile-menu a {
            display:block;
            text-decoration:none;
            color:#2c342d;
            border-radius:8px;
            padding:10px;
            font-weight:700;
        }
        .profile-menu a:hover { background:#f3f7f1; }
        .profile-menu .danger { color:#9b1c1c; background:#fff5f5; }
        .header-nav {
            margin-top:12px;
            padding-top:12px;
            border-top:1px solid #e4e9e1;
            display:flex;
            gap:14px;
            flex-wrap:wrap;
        }
        .header-nav a {
            text-decoration:none;
            color:#2d352e;
            font-weight:800;
            font-size:.96rem;
        }
        .app { width:100%; max-width:none; margin:0; min-height:100vh; padding:16px clamp(12px,2.7vw,36px); display:flex; flex-direction:column; gap:12px; }
        .card { background:#fff; border:1px solid var(--line); border-radius:14px; padding:14px; }
        .card h1,.card h2{margin:0 0 8px;} .card p{margin:0; color:var(--muted);} 
        .scan-input { width:100%; padding:12px; border:1px solid #cfe2c9; border-radius:10px; font-size:16px; }
        .actions { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:10px; }
        .camera-feed { width:100%; max-height:260px; object-fit:cover; border-radius:10px; margin-top:10px; border:1px solid var(--line); display:none; }
        .camera-hint { margin-top:8px; color:var(--muted); font-size:.88rem; }
        .upload-row { margin-top:10px; display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
        .upload-row input[type="file"] { font-size:.85rem; }
        button { padding:12px; border:none; border-radius:10px; font-weight:800; cursor:pointer; min-height:48px; font-size:16px; touch-action:manipulation; }
        .btn-primary { background:var(--green); color:#fff; }
        .btn-alt { background:#eef8e9; color:var(--green-dark); }
        .status { margin-top:10px; border-radius:10px; padding:10px; font-weight:800; }
        .ok { background:#eaf7e7; color:var(--ok); border:1px solid #cce6c7; }
        .fail { background:#ffecec; color:var(--fail); border:1px solid #f0c2c2; }
        .warn { background:#fff9e8; color:#8a6400; border:1px solid #efdca9; }
        table { width:100%; border-collapse:collapse; margin-top:8px; font-size:.9rem; }
        th, td { padding:8px; border-bottom:1px solid var(--line); text-align:left; }
        th { color:var(--muted); font-weight:700; }
        @media(max-width:900px){
            .brand-logo { height:46px; }
            .app {
                max-width:none;
                width:100%;
                min-height:100dvh;
                padding:10px;
                gap:10px;
            }
            .card {
                padding:12px;
                border-radius:12px;
            }
            .actions {
                grid-template-columns:1fr;
                gap:10px;
            }
            .scan-input {
                min-height:50px;
            }
            .upload-row {
                flex-direction:column;
                align-items:stretch;
            }
            .upload-row input[type="file"],
            .upload-row button {
                width:100%;
            }
            .camera-hint {
                font-size:.96rem;
            }
        }
    </style>
</head>
<body>
    <header class="site-header">
        <div class="header-inner">
            <div class="header-top">
                <a href="${pageContext.request.contextPath}/VenueGuard/VenueGuardDashboard.jsp" class="brand">
                    <img class="brand-logo" src="${pageContext.request.contextPath}/assets/tickify-scanner-logo.svg" alt="Tickify Scanner">
                </a>
                <div class="profile-wrap">
                    <button class="profile-btn" id="profileBtn" type="button" onclick="toggleProfileMenu()">
                        <span class="profile-icon">G</span>
                        <span class="profile-meta">${userFullName} | #${userID}</span>
                    </button>
                    <div class="profile-menu" id="profileMenu">
                        <a href="${pageContext.request.contextPath}/VenueGuard/VenueGuardDashboard.jsp">Scanner Dashboard</a>
                        <a href="${pageContext.request.contextPath}/LogoutServlet.do" class="danger">Logout</a>
                    </div>
                </div>
            </div>
            <nav class="header-nav" aria-label="Venue guard navigation">
                <a href="#">Validate Ticket</a>
                <a href="#">Camera Scan</a>
                <a href="#">Recent Checks</a>
            </nav>
        </div>
    </header>

    <div class="app">
        <section class="card"><h1>Venue Guard Scanner</h1><p>Scan by camera in production, or validate manually if camera access is unavailable.</p></section>
        <section class="card">
            <h2>Validate Ticket</h2>
            <input id="ticketCode" class="scan-input" type="text" placeholder="Enter full QR value or AUTH-XXXXXXXXXXXXXXX" />
            <div class="actions">
                <button class="btn-primary" onclick="validateTicket()">Validate</button>
                <button class="btn-primary" onclick="verifyAuthenticity()">Verify Authenticity</button>
                <button class="btn-alt" onclick="clearScan()">Clear</button>
                <button class="btn-primary" onclick="startCameraScan()">Start Camera Scan</button>
                <button class="btn-alt" onclick="stopCameraScan()">Stop Camera</button>
            </div>
            <video id="cameraFeed" class="camera-feed" playsinline muted></video>
            <canvas id="scanCanvas" style="display:none;"></canvas>
            <div id="cameraHint" class="camera-hint">Use manual code input if camera is unavailable.</div>
            <div class="upload-row">
                <input id="qrImageInput" type="file" accept="image/*" capture="environment">
                <button class="btn-alt" type="button" onclick="scanUploadedImage()">Scan Uploaded QR Image</button>
            </div>
            <div id="statusBox" class="status" style="display:none;"></div>
            <div id="authBox" class="status" style="display:none;"></div>
        </section>
        <section class="card">
            <h2>Recent Checks</h2>
            <table><thead><tr><th>Ticket</th><th>Result</th><th>Time</th></tr></thead><tbody id="recentBody"><tr><td colspan="3">No scans yet.</td></tr></tbody></table>
        </section>
        <section class="card">
            <h2>Attendee Verification List</h2>
            <p>Attendees linked to your assigned event for gate verification.</p>
            <table>
                <thead><tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th></tr></thead>
                <tbody id="guardAttendeeBody"><tr><td colspan="4">Loading attendees...</td></tr></tbody>
            </table>
        </section>
    </div>
    <script>
        const csrfToken = "${sessionScope.csrfToken}";
        const recentResults = [];
        const cameraFeed = document.getElementById("cameraFeed");
        const scanCanvas = document.getElementById("scanCanvas");
        const scanCtx = scanCanvas.getContext("2d", { willReadFrequently: true });
        const cameraHint = document.getElementById("cameraHint");
        const hasBarcodeDetector = "BarcodeDetector" in window;
        const qrDetector = hasBarcodeDetector ? new BarcodeDetector({formats: ["qr_code"]}) : null;
        const hasJsQr = false;
        const isiPhone = /iPhone|iPad|iPod/i.test(navigator.userAgent || "");
        let cameraStream = null;
        let cameraScanRaf = null;
        let scanningActive = false;
        let hasDetectedQr = false;
        let lastDetectAt = 0;
        let scannerFallbackTimer = null;
        let scannerAppOpened = false;
        if (!hasBarcodeDetector && !hasJsQr) {
            cameraHint.textContent = "Use any option below to validate tickets (manual, camera, scanner app, or image upload).";
        }

        function handleScannerReturn() {
            const params = new URLSearchParams(window.location.search || "");
            const scanned = params.get("scanned") || params.get("code");
            if (!scanned) {
                return;
            }

            const decoded = decodeURIComponent(scanned).trim();
            if (decoded) {
                document.getElementById("ticketCode").value = decoded.toUpperCase();
                cameraHint.textContent = "Scanner app returned a code. Validating...";
                validateTicket();
            }

            params.delete("scanned");
            params.delete("code");
            const cleanQuery = params.toString();
            const cleanUrl = window.location.pathname + (cleanQuery ? ("?" + cleanQuery) : "");
            if (window.history && window.history.replaceState) {
                window.history.replaceState({}, document.title, cleanUrl);
            }
        }

        function clearScannerFallbackTimer() {
            if (scannerFallbackTimer) {
                window.clearTimeout(scannerFallbackTimer);
                scannerFallbackTimer = null;
            }
        }

        function markScannerAppOpenedIfHidden() {
            if (document.visibilityState === "hidden") {
                scannerAppOpened = true;
                clearScannerFallbackTimer();
            }
        }

        document.addEventListener("visibilitychange", markScannerAppOpenedIfHidden);
        window.addEventListener("pagehide", function () {
            scannerAppOpened = true;
            clearScannerFallbackTimer();
        });

        function tryIPhoneScannerAppFirst() {
            if (!isiPhone) {
                return false;
            }

            scannerAppOpened = false;
            const baseUrl = window.location.origin + window.location.pathname;
            const retUrl = baseUrl + "?scanned={CODE}";
            const zxingUrl = "zxing://scan/?ret=" + encodeURIComponent(retUrl);

            // Try external scanner app first; fallback to in-page camera if no app handles deep-link.
            window.location.href = zxingUrl;

            clearScannerFallbackTimer();
            scannerFallbackTimer = window.setTimeout(function () {
                if (!scannerAppOpened) {
                    startNativeCameraScan();
                }
            }, 1100);

            return true;
        }
        async function callValidationService(code, mode) {
            const op = mode || "validate";
            const response = await fetch("../ValidateTicketServlet.do", {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "code=" + encodeURIComponent(code)
                    + "&mode=" + encodeURIComponent(op)
                    + "&_csrf=" + encodeURIComponent(csrfToken)
            });
            let result = {};
            try {
                result = await response.json();
            } catch (e) {}
            return { response, result };
        }

        function renderAuthenticity(status) {
            const authBox = document.getElementById("authBox");
            if (status === "VERIFIED") {
                authBox.className = "status ok";
                authBox.textContent = "AUTHENTICITY: VERIFIED (SHA-256 badge matched).";
            } else if (status === "MISMATCH") {
                authBox.className = "status fail";
                authBox.textContent = "AUTHENTICITY: MISMATCH. Potential duplicate or tampered ticket.";
            } else {
                authBox.className = "status warn";
                authBox.textContent = "AUTHENTICITY: NOT PROVIDED. You can scan the full QR value or enter AUTH-... directly.";
            }
            authBox.style.display = "block";
        }

        async function validateTicket() {
            const input = document.getElementById("ticketCode");
            const statusBox = document.getElementById("statusBox");
            const code = (input.value || "").trim().toUpperCase();
            if (!code) { statusBox.className = "status fail"; statusBox.textContent = "Please enter a ticket code first."; statusBox.style.display = "block"; feedback(false); return; }
            try {
                const api = await callValidationService(code, "validate");
                const response = api.response;
                const result = api.result;
                if (response.ok && result.valid) {
                    statusBox.className = "status ok";
                    statusBox.textContent = "VALID TICKET: " + result.message + (result.ticketId ? " (Ticket #" + result.ticketId + ")" : "");
                    feedback(true); pushRecent(code, "VALID", result.scannedAt);
                } else {
                    statusBox.className = "status fail";
                    statusBox.textContent = "INVALID TICKET: " + (result.message || "Access denied.");
                    feedback(false); pushRecent(code, "INVALID", result.scannedAt);
                }
                renderAuthenticity(result.authenticityStatus);
            } catch (err) {
                statusBox.className = "status fail";
                statusBox.textContent = "Scanner service is unavailable. Please retry.";
                feedback(false); pushRecent(code, "ERROR", null);
                renderAuthenticity("NOT_PROVIDED");
            }
            statusBox.style.display = "block";
        }

        async function verifyAuthenticity() {
            const input = document.getElementById("ticketCode");
            const code = (input.value || "").trim().toUpperCase();
            if (!code) {
                renderAuthenticity("NOT_PROVIDED");
                return;
            }
            try {
                const api = await callValidationService(code, "verify");
                renderAuthenticity(api.result.authenticityStatus);
            } catch (err) {
                renderAuthenticity("NOT_PROVIDED");
            }
        }

        function clearScan() {
            document.getElementById("ticketCode").value = "";
            document.getElementById("statusBox").style.display = "none";
            document.getElementById("authBox").style.display = "none";
        }

        function toggleProfileMenu() {
            document.getElementById("profileMenu").classList.toggle("open");
        }

        window.addEventListener("click", function (event) {
            var menu = document.getElementById("profileMenu");
            var btn = document.getElementById("profileBtn");
            if (!menu || !btn) { return; }
            if (!menu.contains(event.target) && !btn.contains(event.target)) {
                menu.classList.remove("open");
            }
        });

        function getUserMediaCompat(constraints) {
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                return navigator.mediaDevices.getUserMedia(constraints);
            }

            const legacyGetUserMedia = navigator.getUserMedia
                || navigator.webkitGetUserMedia
                || navigator.mozGetUserMedia
                || navigator.msGetUserMedia;

            if (!legacyGetUserMedia) {
                return Promise.reject(new Error("getUserMedia not supported"));
            }

            return new Promise(function (resolve, reject) {
                legacyGetUserMedia.call(navigator, constraints, resolve, reject);
            });
        }

        function isSecureCameraContext() {
            if (window.isSecureContext) {
                return true;
            }
            const host = window.location.hostname;
            return host === "localhost" || host === "127.0.0.1";
        }

        async function startNativeCameraScan() {
            if (!isSecureCameraContext()) {
                cameraHint.textContent = "Live camera requires HTTPS on iPhone Safari. Opening photo/camera capture fallback now.";
                const captureInput = document.getElementById("qrImageInput");
                if (captureInput) {
                    captureInput.click();
                }
                return;
            }
            if (cameraStream) { return; }

            const attempts = [
                { video: { facingMode: { ideal: "environment" } }, audio: false },
                { video: { facingMode: "environment" }, audio: false },
                { video: true, audio: false }
            ];

            try {
                let stream = null;
                let lastErr = null;
                for (let i = 0; i < attempts.length; i++) {
                    try {
                        stream = await getUserMediaCompat(attempts[i]);
                        if (stream) {
                            break;
                        }
                    } catch (err) {
                        lastErr = err;
                    }
                }

                if (!stream) {
                    throw lastErr || new Error("Could not open camera");
                }

                cameraStream = stream;
                cameraFeed.setAttribute("playsinline", "true");
                cameraFeed.setAttribute("webkit-playsinline", "true");
                cameraFeed.autoplay = true;
                cameraFeed.muted = true;
                cameraFeed.srcObject = cameraStream;
                cameraFeed.style.display = "block";
                await cameraFeed.play();
                if (hasBarcodeDetector || hasJsQr) {
                    cameraHint.textContent = "Camera active. Point at a QR code to auto-validate.";
                    scanningActive = true;
                    hasDetectedQr = false;
                    startDetectionLoop();
                } else {
                    cameraHint.textContent = "Camera active. Auto QR detection is unavailable here; use manual code input or upload a QR image.";
                }
            } catch (e) {
                cameraHint.textContent = "Unable to access camera. On Safari, allow camera permission and disable Private Browsing restrictions if enabled.";
                stopCameraScan();
            }
        }

        function startCameraScan() {
            if (tryIPhoneScannerAppFirst()) {
                cameraHint.textContent = "Trying your iPhone QR scanner app first. If it does not open, camera fallback starts automatically.";
                return;
            }
            startNativeCameraScan();
        }
        function stopCameraScan() {
            scanningActive = false;
            hasDetectedQr = false;
            if (cameraScanRaf) {
                window.cancelAnimationFrame(cameraScanRaf);
                cameraScanRaf = null;
            }
            if (cameraStream) { cameraStream.getTracks().forEach(function (track) { track.stop(); }); cameraStream = null; }
            cameraFeed.srcObject = null; cameraFeed.style.display = "none";
        }

        function startDetectionLoop() {
            if (!scanningActive || !cameraStream || hasDetectedQr) {
                return;
            }

            cameraScanRaf = window.requestAnimationFrame(async function loop() {
                if (!scanningActive || !cameraStream || hasDetectedQr) {
                    return;
                }

                const now = Date.now();
                if (now - lastDetectAt >= 220) {
                    lastDetectAt = now;
                    await scanFrameForQr();
                }

                if (scanningActive && cameraStream && !hasDetectedQr) {
                    cameraScanRaf = window.requestAnimationFrame(loop);
                }
            });
        }

        async function scanFrameForQr() {
            if (!cameraFeed || !cameraStream || hasDetectedQr) { return; }
            try {
                let rawValue = null;
                if (hasBarcodeDetector && qrDetector) {
                    const detections = await qrDetector.detect(cameraFeed);
                    if (detections && detections.length > 0) {
                        rawValue = detections[0].rawValue;
                    }
                }

                if (!rawValue && hasJsQr && cameraFeed.videoWidth && cameraFeed.videoHeight) {
                    scanCanvas.width = cameraFeed.videoWidth;
                    scanCanvas.height = cameraFeed.videoHeight;
                    scanCtx.drawImage(cameraFeed, 0, 0, scanCanvas.width, scanCanvas.height);
                    const imageData = scanCtx.getImageData(0, 0, scanCanvas.width, scanCanvas.height);
                    const decoded = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: "dontInvert" });
                    if (decoded && decoded.data) {
                        rawValue = decoded.data;
                    }
                }

                if (!rawValue) { return; }
                hasDetectedQr = true;
                document.getElementById("ticketCode").value = rawValue.trim().toUpperCase();
                stopCameraScan(); cameraHint.textContent = "QR captured. Validating ticket..."; validateTicket();
            } catch (e) {}
        }

        function scanUploadedImage() {
            const input = document.getElementById("qrImageInput");
            const file = input.files && input.files[0];
            if (!file) {
                cameraHint.textContent = "Choose a QR image first, or use Start Camera Scan.";
                return;
            }

            const reader = new FileReader();
            reader.onload = function () {
                const img = new Image();
                img.onload = async function () {
                    try {
                        let decodedValue = null;

                        if (hasBarcodeDetector && qrDetector) {
                            const detections = await qrDetector.detect(img);
                            if (detections && detections.length > 0 && detections[0].rawValue) {
                                decodedValue = detections[0].rawValue;
                            }
                        }

                        if (!decodedValue && hasJsQr) {
                            scanCanvas.width = img.width;
                            scanCanvas.height = img.height;
                            scanCtx.drawImage(img, 0, 0, img.width, img.height);
                            const imageData = scanCtx.getImageData(0, 0, img.width, img.height);
                            const decoded = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: "dontInvert" });
                            if (decoded && decoded.data) {
                                decodedValue = decoded.data;
                            }
                        }

                        if (!decodedValue) {
                            if (isiPhone) {
                                cameraHint.textContent = "Could not decode this image. Trying your iPhone scanner app now...";
                                tryIPhoneScannerAppFirst();
                                return;
                            }
                            cameraHint.textContent = "No QR found in this image. Try a clearer image or enter the code manually.";
                            return;
                        }

                        document.getElementById("ticketCode").value = decodedValue.trim().toUpperCase();
                        cameraHint.textContent = "QR loaded from image. Validating ticket...";
                        validateTicket();
                    } catch (e) {
                        if (isiPhone) {
                            cameraHint.textContent = "Image decode failed. Trying your iPhone scanner app...";
                            tryIPhoneScannerAppFirst();
                            return;
                        }
                        cameraHint.textContent = "Could not decode uploaded image. Enter code manually.";
                    }
                };
                img.onerror = function () {
                    cameraHint.textContent = "Could not read uploaded image.";
                };
                img.src = reader.result;
            };
            reader.readAsDataURL(file);
        }
        function feedback(success) {
            if (navigator.vibrate) { navigator.vibrate(success ? [90] : [130, 60, 130]); }
            const context = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = context.createOscillator(); const gainNode = context.createGain();
            oscillator.type = "sine"; oscillator.frequency.value = success ? 860 : 340; gainNode.gain.value = 0.04;
            oscillator.connect(gainNode); gainNode.connect(context.destination); oscillator.start(); oscillator.stop(context.currentTime + 0.11);
        }
        function pushRecent(code, result, scannedAt) {
            const timeLabel = scannedAt ? new Date(scannedAt).toLocaleTimeString() : new Date().toLocaleTimeString();
            recentResults.unshift({ code, result, time: timeLabel });
            if (recentResults.length > 6) { recentResults.pop(); }
            const body = document.getElementById("recentBody");
            body.innerHTML = recentResults.map(function (item) { return "<tr><td>" + item.code + "</td><td>" + item.result + "</td><td>" + item.time + "</td></tr>"; }).join("");
        }

        async function loadGuardAttendees() {
            const tbody = document.getElementById("guardAttendeeBody");
            if (!tbody) {
                return;
            }
            try {
                const res = await fetch("../VenueGuardAttendees.do", { credentials: "same-origin" });
                const payload = await res.json();
                if (!res.ok || !payload.ok) {
                    tbody.innerHTML = "<tr><td colspan=\"4\">Unable to load attendee list.</td></tr>";
                    return;
                }
                const rows = Array.isArray(payload.rows) ? payload.rows : [];
                if (rows.length === 0) {
                    tbody.innerHTML = "<tr><td colspan=\"4\">No attendees found for this event yet.</td></tr>";
                    return;
                }
                tbody.innerHTML = rows.map(function (row) {
                    const fullName = ((row.firstname || "") + " " + (row.lastname || "")).trim();
                    return "<tr>"
                        + "<td>" + (row.attendeeID == null ? "" : row.attendeeID) + "</td>"
                        + "<td>" + (row.username || "") + "</td>"
                        + "<td>" + fullName + "</td>"
                        + "<td>" + (row.email || "") + "</td>"
                        + "</tr>";
                }).join("");
            } catch (e) {
                tbody.innerHTML = "<tr><td colspan=\"4\">Unable to load attendee list.</td></tr>";
            }
        }

        handleScannerReturn();
        loadGuardAttendees();
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
