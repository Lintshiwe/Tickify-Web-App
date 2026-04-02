<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | My Order History</title>
    <style>
        :root { --green:#79c84a; --line:#d7e2d1; --ink:#223028; --muted:#5d6a60; --panel:#fff; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); background:#f7faf6; }
        .wrap { width:100%; max-width:none; margin:0; padding:22px clamp(12px,2.7vw,36px) 50px; }
        .head { display:flex; justify-content:space-between; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:14px; }
        .btn { text-decoration:none; border:1px solid var(--line); background:#fff; color:#2f3a32; border-radius:10px; padding:10px 12px; font-weight:800; }
        .order-card { background:var(--panel); border:1px solid var(--line); border-radius:14px; padding:14px; margin-bottom:12px; box-shadow:0 8px 20px rgba(20,30,20,.06); }
        .order-meta { display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:8px; margin-bottom:10px; }
        .meta-block { border:1px dashed #d9e5d3; border-radius:10px; padding:8px; }
        .meta-block strong { display:block; font-size:.82rem; color:var(--muted); }
        .meta-block span { font-weight:800; }
        table { width:100%; border-collapse:collapse; min-width:640px; }
        th, td { border-bottom:1px solid #edf3ea; text-align:left; padding:8px; font-size:.9rem; }
        th { background:#f8fbf6; color:#5d6f61; }
        .table-wrap { overflow:auto; border:1px solid #e5ece2; border-radius:10px; }
        .empty { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; color:var(--muted); }
    </style>
</head>
<body>
    <div class="wrap">
        <div class="head">
            <div>
                <h1 style="margin:0;">My Order History</h1>
                <div style="color:var(--muted);">View all your completed Tickify purchases.</div>
            </div>
            <div>
                <a class="btn" href="${pageContext.request.contextPath}/AttendeeDashboardServlet.do">Back to Dashboard</a>
                <a class="btn" href="${pageContext.request.contextPath}/ViewMyTickets.do">My Tickets</a>
            </div>
        </div>

        <c:choose>
            <c:when test="${not empty orders}">
                <c:forEach var="order" items="${orders}">
                    <article class="order-card">
                        <div class="order-meta">
                            <div class="meta-block"><strong>Order ID</strong><span>#${order.orderID}</span></div>
                            <div class="meta-block"><strong>Transaction Ref</strong><span><c:out value="${order.transactionRef}" default="N/A"/></span></div>
                            <div class="meta-block"><strong>Status</strong><span>${order.status}</span></div>
                            <div class="meta-block"><strong>Date</strong><span><fmt:formatDate value="${order.createdAt}" pattern="yyyy-MM-dd HH:mm"/></span></div>
                            <div class="meta-block"><strong>Total</strong><span>R <fmt:formatNumber value="${order.totalAmount}" minFractionDigits="2" maxFractionDigits="2"/></span></div>
                        </div>
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr><th>Event</th><th>Type</th><th>Venue</th><th>Event Date</th><th>Qty</th><th>Unit Price</th><th>Line Total</th></tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="item" items="${order.items}">
                                        <tr>
                                            <td>${item.eventName}</td>
                                            <td>${item.eventType}</td>
                                            <td>${item.venueName}</td>
                                            <td><fmt:formatDate value="${item.eventDate}" pattern="yyyy-MM-dd HH:mm"/></td>
                                            <td>${item.quantity}</td>
                                            <td>R <fmt:formatNumber value="${item.unitPrice}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                            <td>R <fmt:formatNumber value="${item.lineTotal}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </article>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <div class="empty">No order history yet. Complete checkout to see orders listed here.</div>
            </c:otherwise>
        </c:choose>
    </div>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
