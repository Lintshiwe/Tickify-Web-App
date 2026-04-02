<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Payment Gateway</title>
    <style>
        :root { --green:#7fc342; --line:#d8e0d2; --ink:#2a2f2a; --muted:#667267; --err:#9b1c1c; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); background:#f7faf5; }
        .wrap { width:100%; max-width:none; margin:0; padding:24px clamp(12px,2.7vw,36px) 48px; }
        .head { display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:12px; }
        .chip { border:1px solid #cfe2c9; background:#eef8e9; color:#3d7b22; border-radius:999px; padding:7px 12px; font-weight:800; }
        .card { background:#fff; border:1px solid var(--line); border-radius:14px; padding:14px; box-shadow:0 8px 22px rgba(33,47,32,.08); }
        .grid { display:grid; grid-template-columns:1.1fr .9fr; gap:12px; }
        .field { margin-bottom:10px; }
        .field label { display:block; font-weight:700; margin-bottom:6px; }
        .field input, .field select { width:100%; border:1px solid var(--line); border-radius:10px; padding:10px; font:inherit; }
        .sum { margin-top:12px; border-top:1px solid #edf3e8; padding-top:12px; display:flex; justify-content:space-between; align-items:center; }
        .btns { display:flex; gap:8px; margin-top:12px; }
        .btn { border:none; border-radius:10px; font-weight:800; padding:10px 12px; cursor:pointer; }
        .btn-pay { background:var(--green); color:#fff; }
        .btn-pay:disabled { background:#bdd4ac; cursor:not-allowed; }
        .btn-cancel { background:#ffecec; color:var(--err); }
        .ticket-row { display:flex; justify-content:space-between; gap:8px; border-bottom:1px solid #edf3e8; padding:8px 0; }
        .ticket-row:last-child { border-bottom:none; }
        .muted { color:var(--muted); }
        .note { margin:0 0 10px; padding:10px; border:1px dashed #c6d7bd; border-radius:10px; background:#f7fbf4; color:#4d5f50; }
        .processing {
            position:fixed; inset:0; background:rgba(23,33,21,.38); display:none;
            align-items:center; justify-content:center; z-index:99;
        }
        .processing-card {
            background:#fff; border:1px solid #dbe8d4; border-radius:14px; padding:16px; min-width:300px;
            box-shadow:0 10px 24px rgba(0,0,0,.18);
        }
        .progress {
            margin-top:10px; height:8px; border-radius:999px; overflow:hidden; background:#eef4ea;
        }
        .progress > span {
            display:block; height:100%; width:0; background:#7fc342;
            animation:loadBar 1.6s linear forwards;
        }
        @keyframes loadBar {
            from { width:0; }
            to { width:100%; }
        }
        @media (max-width:768px){ .field input, .field select, .btn, button { font-size:16px; } }
        @media (max-width:900px){ .grid{grid-template-columns:1fr;} }
    </style>
</head>
<body data-user-name="${userFullName}" data-interest="Event Tickets">
    <div class="wrap">
        <div class="head">
            <div>
                <h1 style="margin:0;">Payment Gateway</h1>
                <div class="muted">Temporary secure checkout simulation until official gateway integration.</div>
            </div>
            <div class="chip">Mode: ${paymentMode}</div>
        </div>

        <% String err = request.getParameter("err"); %>
        <% if ("PaymentFailed".equals(err)) { %>
            <div class="card" style="border-color:#f0c2c2; color:#9b1c1c; margin-bottom:10px;">Payment failed. Please try again.</div>
        <% } else if ("SoldOut".equals(err)) { %>
            <div class="card" style="border-color:#f0c2c2; color:#9b1c1c; margin-bottom:10px;">This event is sold out or stock changed while you were checking out. Refresh and try another event.</div>
        <% } else if ("AgeRestricted".equals(err)) { %>
            <div class="card" style="border-color:#f0c2c2; color:#9b1c1c; margin-bottom:10px;">Your account is under 18 and cannot purchase this event type.</div>
        <% } else if ("TermsRequired".equals(err)) { %>
            <div class="card" style="border-color:#f0c2c2; color:#9b1c1c; margin-bottom:10px;">You must accept the no-refund terms before confirming payment.</div>
        <% } %>

        <div class="grid">
            <section class="card">
                <h3 style="margin:0 0 10px;">Demo Checkout Interface</h3>
                <p class="note">
                    This is a fake/sandbox checkout for now. After confirmation, Tickify generates and stores your tickets.
                    PayFast API integration can be plugged in later without changing the checkout route.
                </p>
                <form id="demoPaymentForm" action="${pageContext.request.contextPath}/PaymentGateway.do" method="POST" onsubmit="return submitDemoPayment(event)">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="paymentAction" value="pay">
                    <div class="field"><label>Card Holder</label><input type="text" name="holder" value="${userFullName}" required></div>
                    <div class="field"><label>Card Number</label><input type="text" name="number" placeholder="4111 1111 1111 1111" required></div>
                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
                        <div class="field"><label>Expiry</label><input type="text" name="expiry" placeholder="MM/YY" required></div>
                        <div class="field"><label>CVV</label><input type="password" name="cvv" placeholder="123" required></div>
                    </div>
                    <div class="field"><label>Billing Country</label><select name="country"><option>South Africa</option><option>Namibia</option><option>Botswana</option></select></div>
                    <div class="field" style="display:flex;align-items:center;gap:8px;">
                        <input id="confirmDemo" type="checkbox" required style="width:auto;">
                        <label for="confirmDemo" style="margin:0;">I understand this is a demo payment and ticket generation flow.</label>
                    </div>
                    <div class="field" style="display:flex;align-items:flex-start;gap:8px;">
                        <input id="acceptNoRefund" type="checkbox" name="acceptNoRefund" value="yes" required style="width:auto;margin-top:2px;">
                        <label for="acceptNoRefund" style="margin:0;line-height:1.35;">I accept the terms: tickets are non-refundable in all situations, except if the event is cancelled or cannot proceed due to event-related issues.</label>
                    </div>
                    <div class="btns">
                        <button id="payBtn" class="btn btn-pay" type="submit">Confirm Demo Payment</button>
                    </div>
                </form>
                <form action="${pageContext.request.contextPath}/PaymentGateway.do" method="POST" style="margin-top:8px;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="paymentAction" value="cancel">
                    <button class="btn btn-cancel" type="submit">Cancel</button>
                </form>
            </section>

            <section class="card">
                <h3 style="margin:0 0 10px;">Order Summary</h3>
                <c:forEach var="item" items="${cartItems}">
                    <div class="ticket-row">
                        <div>
                            <div style="font-weight:800;">${item.eventName}</div>
                            <div class="muted">Qty: ${item.quantity}</div>
                        </div>
                        <div style="font-weight:800;">R <fmt:formatNumber value="${item.price * item.quantity}" minFractionDigits="2" maxFractionDigits="2"/></div>
                    </div>
                </c:forEach>
                <div class="sum">
                    <div><strong>Items:</strong> ${cartCount}</div>
                    <div style="font-size:1.15rem;font-weight:900;">R <fmt:formatNumber value="${checkoutTotal}" minFractionDigits="2" maxFractionDigits="2"/></div>
                </div>
            </section>
        </div>
    </div>

    <div class="processing" id="processingOverlay" aria-live="polite" aria-busy="true">
        <div class="processing-card">
            <strong>Processing Demo Payment...</strong>
            <div class="muted" style="margin-top:6px;">Please wait while we generate your tickets.</div>
            <div class="progress"><span></span></div>
        </div>
    </div>

    <script>
        function submitDemoPayment(event) {
            event.preventDefault();
            var form = document.getElementById('demoPaymentForm');
            var overlay = document.getElementById('processingOverlay');
            var payBtn = document.getElementById('payBtn');

            // Preserve browser required-field validation before delayed submit.
            if (!form.reportValidity()) {
                return false;
            }

            payBtn.disabled = true;
            overlay.style.display = 'flex';

            setTimeout(function () {
                HTMLFormElement.prototype.submit.call(form);
            }, 1600);
            return false;
        }
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
    <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
</body>
</html>
