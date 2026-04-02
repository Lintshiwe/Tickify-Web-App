<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Checkout</title>
    <style>
        :root {
            --green:#7fc342;
            --green-dark:#5da72f;
            --ink:#2a2f2a;
            --line:#d8e0d2;
            --ok:#1f7c39;
            --ok-bg:#eaf7e7;
            --err:#9b1c1c;
            --err-bg:#ffecec;
        }
        * { box-sizing:border-box; }
        body {
            margin:0;
            font-family:"Trebuchet MS","Segoe UI",sans-serif;
            color:var(--ink);
            background:#f7faf5;
            background-image:repeating-linear-gradient(135deg,#f8fbf6 0px,#f8fbf6 32px,#edf5e8 32px,#edf5e8 64px);
        }
        .wrap { width:100%; max-width:none; margin:0; padding:26px clamp(12px,2.7vw,36px) 60px; }
        .top {
            display:flex; align-items:center; justify-content:space-between; gap:10px; flex-wrap:wrap;
            margin-bottom:18px;
        }
        .title { margin:0; font-size:2rem; color:#233127; }
        .sub { margin-top:4px; color:#5a675d; }
        .actions { display:flex; gap:8px; }
        .link-btn, .solid-btn, .danger-btn {
            text-decoration:none; border:none; border-radius:999px; padding:10px 14px; font-weight:800; cursor:pointer;
        }
        .link-btn { background:#ffffff; border:1px solid var(--line); color:#2f3a32; }
        .solid-btn { background:var(--green); color:#fff; }
        .solid-btn:disabled { background:#c9d8bf; cursor:not-allowed; }
        .danger-btn { background:#ffecec; color:#9b1c1c; }

        .flash { margin:0 0 12px; padding:11px 12px; border-radius:10px; font-weight:700; }
        .flash-ok { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .flash-err { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }

        .card {
            background:#fff; border:none; border-radius:14px; box-shadow:0 6px 18px rgba(33,47,32,.08);
            padding:14px;
        }
        .row {
            display:grid; grid-template-columns:1.5fr 1fr 1fr auto; gap:10px; align-items:center;
            padding:10px 0; border-bottom:1px solid #eef3eb;
        }
        .row:last-child { border-bottom:none; }
        .name { font-weight:800; }
        .muted { color:#5e6b62; }
        .qty { width:74px; border:1px solid var(--line); border-radius:8px; padding:8px; font-weight:700; }
        .sum {
            margin-top:14px; display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap;
            border-top:1px solid #eef3eb; padding-top:14px;
        }
        @media(max-width:768px){ .qty, .link-btn, .solid-btn, .danger-btn, button { font-size:16px; } }
    </style>
</head>
<body data-user-name="${userFullName}" data-interest="Event Tickets">
    <div class="wrap">
        <div class="top">
            <div>
                <h1 class="title">Checkout</h1>
                <div class="sub">Review your cart, then continue to secure payment gateway.</div>
            </div>
            <div class="actions">
                <a class="link-btn" href="AttendeeDashboardServlet.do">Back to Events</a>
                <form action="BookTicket.do" method="POST" style="margin:0;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="clear">
                    <input type="hidden" name="returnTo" value="checkout">
                    <button type="submit" class="danger-btn">Clear Cart</button>
                </form>
            </div>
        </div>

        <% String msg = request.getParameter("msg"); %>
        <% String err = request.getParameter("err"); %>

        <% if ("AddedToCart".equals(msg)) { %>
            <div class="flash flash-ok">Ticket(s) added to cart.</div>
        <% } else if ("RemovedFromCart".equals(msg)) { %>
            <div class="flash flash-ok">Item removed from cart.</div>
        <% } else if ("CartUpdated".equals(msg)) { %>
            <div class="flash flash-ok">Cart quantity updated.</div>
        <% } else if ("CartCleared".equals(msg)) { %>
            <div class="flash flash-ok">Cart cleared.</div>
        <% } else if ("CheckoutComplete".equals(msg)) { %>
            <div class="flash flash-ok">Checkout completed successfully.</div>
        <% } else if ("CheckoutPartial".equals(msg)) { %>
            <div class="flash flash-ok">Checkout partially completed. Some tickets were unavailable.</div>
        <% } else if ("PaymentCancelled".equals(msg)) { %>
            <div class="flash flash-ok">Payment was cancelled. Your cart is still available.</div>
        <% } %>

        <% if (err != null) { %>
            <div class="flash flash-err">
                <% if ("NoTicket".equals(err)) { %>
                    No ticket is currently available for one or more cart items.
                <% } else if ("InvalidEvent".equals(err)) { %>
                    Selected event is invalid.
                <% } else if ("CartUpdateFailed".equals(err)) { %>
                    Unable to update cart. Please try again.
                <% } else if ("CartEmpty".equals(err)) { %>
                    Your cart is empty. Add events before checkout.
                <% } else if ("CheckoutFailed".equals(err)) { %>
                    Checkout failed. Please try again.
                <% } else if ("AgeRestricted".equals(err)) { %>
                    Your account is under 18 and cannot purchase one or more event types in your cart.
                <% } else if ("TermsRequired".equals(err)) { %>
                    You must accept the no-refund terms before proceeding to payment.
                <% } else { %>
                    Action failed. Please try again.
                <% } %>
            </div>
        <% } %>

        <div class="card">
            <c:choose>
                <c:when test="${not empty cartItems}">
                    <c:forEach var="item" items="${cartItems}">
                        <div class="row">
                            <div>
                                <div class="name">${item.eventName}</div>
                                <div class="muted">Price each: R <fmt:formatNumber value="${item.price}" minFractionDigits="2" maxFractionDigits="2"/></div>
                            </div>
                            <form action="BookTicket.do" method="POST" style="margin:0;display:flex;align-items:center;gap:8px;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="eventID" value="${item.eventID}">
                                <input type="hidden" name="returnTo" value="checkout">
                                <input class="qty" type="number" name="quantity" min="0" value="${item.quantity}" aria-label="Quantity for ${item.eventName}">
                                <button type="submit" class="link-btn">Update</button>
                            </form>
                            <div><strong>R <fmt:formatNumber value="${item.price * item.quantity}" minFractionDigits="2" maxFractionDigits="2"/></strong></div>
                            <form action="BookTicket.do" method="POST" style="margin:0;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="remove">
                                <input type="hidden" name="eventID" value="${item.eventID}">
                                <input type="hidden" name="returnTo" value="checkout">
                                <button type="submit" class="danger-btn">Remove</button>
                            </form>
                        </div>
                    </c:forEach>

                    <div class="sum">
                        <div>
                            <strong>Items: ${cartCount}</strong>
                            <div class="muted">Total payable</div>
                        </div>
                        <div style="font-size:1.35rem;font-weight:800;color:#2e3b2f;">
                            R <fmt:formatNumber value="${checkoutTotal}" minFractionDigits="2" maxFractionDigits="2"/>
                        </div>
                        <form action="BookTicket.do" method="POST" style="margin:0;">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="checkout">
                            <input type="hidden" name="returnTo" value="checkout">
                            <button type="submit" class="solid-btn">Proceed to Payment</button>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="muted">Your cart is empty. Add tickets from Events.</div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
    <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
</body>
</html>
