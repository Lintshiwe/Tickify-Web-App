<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | My Tickets</title>
    <style>
        :root {
            --green:#79c84a;
            --green-bright:#9be552;
            --sun:#f8de57;
            --orange:#f7a531;
            --ink:#223028;
            --muted:#5d6a60;
            --line:#d7e2d1;
        }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:#f7faf6; color:var(--ink); }
        .wrap { width:100%; max-width:none; margin:0; padding:22px clamp(12px,2.7vw,36px) 50px; }
        .head { display:flex; justify-content:space-between; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:10px; }
        .head-actions { display:flex; gap:8px; flex-wrap:wrap; }
        .btn { text-decoration:none; border:1px solid var(--line); background:#fff; color:#2f3a32; border-radius:10px; padding:10px 12px; font-weight:800; }
        .btn-action { border:none; border-radius:10px; padding:10px 12px; font-weight:800; cursor:pointer; background:#79c84a; color:#fff; }
        .btn-secondary { border:1px solid var(--line); border-radius:10px; padding:10px 12px; font-weight:800; cursor:pointer; background:#fff; color:#2f3a32; }
        .flash { margin-bottom:10px; padding:10px; border-radius:10px; font-weight:700; background:#eaf7e7; color:#1f7c39; border:1px solid #cce6c7; }
        .tickets {
            display:grid;
            grid-template-columns:1fr;
            gap:12px;
        }
        .ticket {
            width:100%;
            max-width:none;
            margin:0;
        }
        .ticket {
            background:#fff; border:1px solid var(--line); border-radius:16px; overflow:hidden;
            box-shadow:0 8px 22px rgba(33,47,32,.08); position:relative;
        }
        .ticket-watermark {
            position:absolute;
            inset:0;
            display:flex;
            align-items:center;
            justify-content:center;
            pointer-events:none;
            z-index:0;
        }
        .ticket-watermark img {
            width:min(68%, 620px);
            max-height:72%;
            object-fit:contain;
            opacity:.14;
            filter:grayscale(1) contrast(1.1);
        }
        .ticket > *:not(.ticket-watermark) {
            position:relative;
            z-index:1;
        }
        .ticket:before {
            content:""; position:absolute; left:-7px; top:50%; transform:translateY(-50%);
            width:14px; height:14px; border-radius:50%; background:#f7faf6; border:1px solid #d7e2d1;
        }
        .ticket:after {
            content:""; position:absolute; right:-7px; top:50%; transform:translateY(-50%);
            width:14px; height:14px; border-radius:50%; background:#f7faf6; border:1px solid #d7e2d1;
        }
        .album {
            background:linear-gradient(120deg,#e9f4e2,#f6fbf3 60%);
            padding:12px 14px; border-bottom:1px solid #e3eddc;
            display:flex; justify-content:space-between; align-items:center; gap:8px;
        }
        .album img { height:34px; }
        .brand-logo { display:block; width:auto; }
        .badge { background:#edf5e8; color:#5da72f; border-radius:999px; padding:5px 9px; font-weight:800; font-size:.8rem; }
        .auth-badge {
            margin-left:8px;
            display:inline-block;
            background:linear-gradient(130deg,#173f8f,#2a5ec7 60%,#f7a531);
            color:#fff;
            border-radius:999px;
            padding:5px 9px;
            font-size:.72rem;
            font-weight:900;
            letter-spacing:.04em;
        }
        .ticket-body {
            display:grid;
            grid-template-columns:1.55fr .95fr;
            gap:10px;
            padding:12px 14px;
        }
        .content { display:grid; gap:6px; }
        .ticket-side {
            border-left:1px dashed #d3dfcd;
            padding:10px 10px 10px 12px;
            display:flex;
            flex-direction:column;
            justify-content:center;
            background:linear-gradient(160deg,var(--green-bright),var(--sun) 58%,var(--orange));
            border-top-right-radius:12px;
            max-width:none;
            margin:0;
            border-bottom-right-radius:12px;
        }
        .event-hero {
            display:grid;
            grid-template-columns:150px 1fr;
            gap:10px;
            align-items:stretch;
        }
        .album-art {
            border-radius:12px;
            border:1px solid #d4e2cc;
            position:relative;
            min-height:120px;
            overflow:hidden;
            background:radial-gradient(circle at 12% 20%, #ffffffcc 0 12%, transparent 13%),
                       linear-gradient(150deg,#1f2c1e 0%,#2c4a26 40%,#79c84a 100%);
        }
        .album-art img {
            width:100%;
            height:100%;
            min-height:120px;
            object-fit:cover;
            display:block;
        }
        .album-art.no-image {
            border:1px solid #d4e2cc;
        }
        .album-art:after {
            content:"";
            position:absolute;
            right:-24px;
            bottom:-28px;
            width:120px;
            height:120px;
            border-radius:50%;
            background:rgba(248, 222, 87, .35);
        }
        .album-art-label {
            position:absolute;
            left:8px;
            bottom:8px;
            padding:4px 8px;
            border-radius:999px;
            background:rgba(0,0,0,.58);
            color:#fff;
            font-size:.72rem;
            font-weight:800;
            letter-spacing:.04em;
        }
        .hero-meta {
            border:1px solid #dbe6d5;
            border-radius:12px;
            padding:8px;
            background:#fbfdf9;
            display:grid;
            align-content:center;
            gap:4px;
        }
        .meta-title { margin:0; font-size:1.12rem; line-height:1.2; }
        .ticket-actions { display:flex; gap:8px; margin-top:6px; flex-wrap:wrap; }
        .title { margin:0; font-size:1.2rem; }
        .muted { color:var(--muted); }
        .grid { display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:6px; }
        .slot { border:1px dashed #d9e5d3; border-radius:10px; padding:8px; }
        .slot strong { display:block; font-size:.83rem; color:#5d6a60; }
        .slot span { font-weight:800; color:#223028; word-break:break-word; }
        .qr {
            border:1px solid #d7e2d1; border-radius:12px; padding:8px; margin-top:6px;
            background:repeating-linear-gradient(45deg,#f4faf0,#f4faf0 8px,#edf6e8 8px,#edf6e8 16px);
            font-family:"Consolas","Courier New",monospace;
            font-size:.82rem;
            word-break:break-all;
            overflow-wrap:anywhere;
        }
        .qr img {
            width:156px;
            height:156px;
            border:1px solid #d5e2cf;
            border-radius:10px;
            background:#fff;
            display:block;
            margin:6px 0 8px;
        }
        .qr-redacted {
            position:relative;
        }
        .qr-redacted img {
            filter:blur(9px) grayscale(1) brightness(.6);
        }
        .qr-redacted .qr-redaction-note {
            display:block;
            margin-top:6px;
            padding:6px 8px;
            border-radius:8px;
            background:#ffeaea;
            border:1px solid #f2b7b7;
            color:#8f1f1f;
            font-size:.78rem;
            font-family:"Trebuchet MS","Segoe UI",sans-serif;
            font-weight:800;
            text-transform:uppercase;
            letter-spacing:.03em;
        }
        @media print {
            @page {
                size:A4 landscape;
                margin:10mm;
            }
            .head, .flash, .ticket-actions { display:none !important; }
            .tickets { grid-template-columns:1fr; }
            .ticket {
                break-inside:avoid;
                page-break-inside:avoid;
                box-shadow:none;
                margin-bottom:10mm;
                width:100%;
            }
            .ticket-body { grid-template-columns:1.75fr 1fr; }
            .event-hero { grid-template-columns:170px 1fr; }
            body { background:#fff; }
        }
        @media (max-width:860px) {
            .ticket-body { grid-template-columns:1fr; }
            .event-hero { grid-template-columns:1fr; }
            .ticket-side { border-left:none; border-top:1px dashed #d3dfcd; padding-left:0; padding-top:10px; }
        }
    </style>
</head>
<body data-user-name="${userFullName}" data-interest="My Tickets">
    <c:url var="tickifyLogoUrl" value="/assets/tickify-logo.svg"/>
    <% boolean popupMode = "1".equals(request.getParameter("popup")); %>
    <div class="wrap">
        <div class="head">
            <div>
                <h1 style="margin:0;">My Tickets</h1>
                <div class="muted">Generated after payment with unique ticket numbers and QR payloads for security verification.</div>
            </div>
            <div class="head-actions">
                <a class="btn-action" style="display:inline-block;text-decoration:none;" href="${pageContext.request.contextPath}/TicketDownload.do?scope=all&fresh=true">Download All Tickets PDF</a>
                <a class="btn" href="${pageContext.request.contextPath}/MyOrderHistory.do">My Order History</a>
                <% if (popupMode) { %>
                    <button type="button" class="btn-secondary" onclick="window.close()">Close Window</button>
                <% } else { %>
                    <a class="btn" href="${pageContext.request.contextPath}/AttendeeDashboardServlet.do">Back to Events</a>
                <% } %>
            </div>
        </div>

        <% String msg = request.getParameter("msg"); %>
        <% String purchaseConfirmationMessage = (String) session.getAttribute("purchaseConfirmationMessage"); %>
        <% String purchaseEmailStatus = (String) session.getAttribute("purchaseEmailStatus"); %>
        <% if (purchaseConfirmationMessage != null) { %>
            <div class="flash"><%= purchaseConfirmationMessage %></div>
        <% } %>
        <% if (purchaseEmailStatus != null && "FAILED".equals(purchaseEmailStatus)) { %>
            <div class="flash" style="background:#fff1ef;color:#912018;border-color:#f0c2c2;">Payment succeeded, but email confirmation could not be delivered right now. Your tickets are saved below.</div>
        <% } %>
        <% session.removeAttribute("purchaseConfirmationMessage"); %>
        <% session.removeAttribute("purchaseEmailStatus"); %>
        <% if ("PaymentSuccess".equals(msg)) { %>
            <div class="flash">Payment successful. Your tickets were generated and saved to your profile.</div>
        <% } else if ("CheckoutPartial".equals(msg)) { %>
            <div class="flash">Payment completed with partial ticket generation.</div>
        <% } %>

        <div class="tickets">
            <c:forEach var="t" items="${tickets}" varStatus="status">
                <article class="ticket" id="ticket-${status.index}" data-attendee="${t.attendeeName}" data-event-id="${t.eventID}">
                    <div class="ticket-watermark">
                        <img src="${tickifyLogoUrl}" alt="" onerror="this.style.display='none';">
                    </div>
                    <div class="album">
                        <div>
                            <span class="badge">${t.eventType}</span>
                            <span class="auth-badge">${t.authToken}</span>
                        </div>
                        <img class="brand-logo" src="${tickifyLogoUrl}" alt="" onerror="this.style.display='none';">
                    </div>
                    <div class="ticket-body">
                        <div class="content">
                            <div class="event-hero">
                                <div class="album-art">
                                    <img src="EventAlbumImage.do?eventID=${t.eventID}" alt="" onerror="this.style.display='none'; this.parentElement.classList.add('no-image');">
                                </div>
                                <div class="hero-meta">
                                    <h3 class="meta-title">${t.eventName}</h3>
                                    <div class="muted">${t.venueName} - ${t.venueAddress}</div>
                                    <div class="muted"><fmt:formatDate value="${t.eventDate}" pattern="yyyy-MM-dd HH:mm"/></div>
                                </div>
                            </div>

                            <div class="grid">
                                <div class="slot"><strong>Ticket Number</strong><span>${t.ticketNumber}</span></div>
                                <div class="slot"><strong>Price</strong><span>R <fmt:formatNumber value="${t.price}" minFractionDigits="2" maxFractionDigits="2"/></span></div>
                                <div class="slot"><strong>Client</strong><span>${t.attendeeName}</span></div>
                                <div class="slot"><strong>Institution</strong><span>${t.attendeeInstitution}</span></div>
                                <div class="slot"><strong>Secret Code</strong><span>${t.secretCode}</span></div>
                            </div>

                            <div class="slot" style="margin-top:6px;"><strong>Client Email</strong><span>${t.attendeeEmail}</span></div>

                            <div class="ticket-actions">
                                <a class="btn-action download-ticket-link" style="display:inline-block;text-decoration:none;" href="${pageContext.request.contextPath}/TicketDownload.do?scope=single&ticketID=${t.ticketID}&fresh=true" data-ticket-id="${t.ticketID}">Download Ticket PDF</a>
                            </div>
                        </div>
                        <div class="ticket-side">
                            <div class="qr">
                                <strong style="display:block;font-family:inherit;margin-bottom:4px;color:#1f2b1d;">SITE QR CODE</strong>
                                <img class="qr-image" data-qr="${t.scannableCode}" alt="Ticket QR code for ${t.ticketNumber}">
                                ${t.scannableCode}
                                <span class="qr-redaction-note" style="display:none;">Screenshot blocked and flagged</span>
                            </div>
                        </div>
                    </div>
                </article>
            </c:forEach>
            <c:if test="${empty tickets}">
                <div class="ticket"><div class="content"><strong>No tickets yet.</strong><div class="muted">Tickets appear here after successful payment.</div></div></div>
            </c:if>
        </div>
    </div>

    <script>
        function ensureQrImagesRendered(root) {
            var scope = root || document;
            var qrImages = scope.querySelectorAll('.qr-image');
            for (var i = 0; i < qrImages.length; i++) {
                var img = qrImages[i];
                var data = img.getAttribute('data-qr') || '';
                if (!data) {
                    continue;
                }
                var encoded = encodeURIComponent(data);
                img.setAttribute('src', 'https://api.qrserver.com/v1/create-qr-code/?size=170x170&data=' + encoded);
            }
        }

        function redactQrOnScreenshotAttempt(reason) {
            var cards = document.querySelectorAll('.ticket[data-event-id]');
            var eventFlags = {};
            for (var i = 0; i < cards.length; i++) {
                var qr = cards[i].querySelector('.qr');
                if (!qr) {
                    continue;
                }
                qr.classList.add('qr-redacted');
                var note = qr.querySelector('.qr-redaction-note');
                if (note) {
                    note.style.display = 'block';
                }
                var eventId = cards[i].getAttribute('data-event-id');
                if (eventId) {
                    eventFlags[eventId] = true;
                }

                cards[i].querySelectorAll('.download-ticket-link').forEach(function (link) {
                    if (!link || !link.href) { return; }
                    try {
                        var u = new URL(link.href, window.location.origin);
                        u.searchParams.set('flagged', 'true');
                        u.searchParams.delete('fresh');
                        link.href = u.pathname + '?' + u.searchParams.toString();
                    } catch (e) {
                    }
                });
            }

            Object.keys(eventFlags).forEach(function (eventId) {
                try {
                    var body = "eventID=" + encodeURIComponent(eventId)
                        + "&action=" + encodeURIComponent('SCREENSHOT_ATTEMPT')
                        + "&channel=" + encodeURIComponent(reason || 'MY_TICKETS');
                    fetch('EventEngagement.do', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                        body: body,
                        keepalive: true
                    });
                } catch (e) {
                }
            });
        }

        document.addEventListener('keydown', function (event) {
            if (event && (event.key === 'PrintScreen' || event.code === 'PrintScreen')) {
                redactQrOnScreenshotAttempt('PRINTSCREEN_KEY');
            }
        });

        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'hidden') {
                redactQrOnScreenshotAttempt('VISIBILITY_HIDDEN');
            }
        });

        window.addEventListener('blur', function () {
            redactQrOnScreenshotAttempt('WINDOW_BLUR');
        });

        ensureQrImagesRendered(document);
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
