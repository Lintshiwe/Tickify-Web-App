<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Something Went Wrong</title>
    <style>
        :root { --bg:#f7faf6; --ink:#2f3b31; --muted:#607167; --line:#d8e5d5; --accent:#79c84a; --warn:#a33b2b; }
        * { box-sizing:border-box; }
        body { margin:0; background:var(--bg); font-family:"Trebuchet MS","Segoe UI",sans-serif; color:var(--ink); }
        .wrap { min-height:100vh; display:flex; align-items:center; justify-content:center; padding:22px; }
        .card { width:min(760px, 100%); background:#fff; border:1px solid var(--line); border-radius:16px; padding:22px; box-shadow:0 16px 30px rgba(25,35,25,.08); }
        h1 { margin:0 0 8px; font-size:1.7rem; }
        p { margin:0; color:var(--muted); }
        .meta { margin-top:14px; border:1px solid #edf3ea; background:#fafdfa; border-radius:10px; padding:12px; }
        .meta div { margin-bottom:6px; color:#516154; }
        .meta div:last-child { margin-bottom:0; }
        .label { font-weight:800; color:#354436; }
        .actions { margin-top:16px; display:flex; gap:10px; flex-wrap:wrap; }
        .btn { text-decoration:none; border-radius:10px; padding:10px 14px; font-weight:800; border:1px solid #cfe2c9; }
        .btn-primary { background:var(--accent); color:#fff; border-color:var(--accent); }
        .btn-alt { background:#fff; color:#334335; }
        .warn { color:var(--warn); font-weight:700; margin-top:12px; }
    </style>
</head>
<body>
    <c:set var="statusCode" value="${requestScope['javax.servlet.error.status_code']}" />
    <c:set var="requestUri" value="${requestScope['javax.servlet.error.request_uri']}" />
    <c:set var="errorMessage" value="${requestScope['javax.servlet.error.message']}" />

    <c:set var="dashboardPath" value="/Login.jsp" />
    <c:choose>
        <c:when test="${sessionScope.userRole eq 'ADMIN'}">
            <c:set var="dashboardPath" value="/AdminDashboard.do" />
        </c:when>
        <c:when test="${sessionScope.userRole eq 'ATTENDEE'}">
            <c:set var="dashboardPath" value="/AttendeeDashboardServlet.do" />
        </c:when>
        <c:when test="${sessionScope.userRole eq 'TERTIARY_PRESENTER'}">
            <c:set var="dashboardPath" value="/TertiaryPresenterDashboard.do" />
        </c:when>
        <c:when test="${sessionScope.userRole eq 'EVENT_MANAGER'}">
            <c:set var="dashboardPath" value="/EventManagerDashboard.do" />
        </c:when>
        <c:when test="${sessionScope.userRole eq 'VENUE_GUARD'}">
            <c:set var="dashboardPath" value="/VenueGuard/VenueGuardDashboard.jsp" />
        </c:when>
    </c:choose>

    <div class="wrap">
        <section class="card">
            <h1>We hit an unexpected error</h1>
            <p>The request could not be completed. You can return to your dashboard and continue.</p>

            <div class="meta">
                <div><span class="label">Status:</span> <c:out value="${empty statusCode ? 'N/A' : statusCode}" /></div>
                <div><span class="label">Path:</span> <c:out value="${empty requestUri ? 'Unknown' : requestUri}" /></div>
                <div><span class="label">Message:</span> <c:out value="${empty errorMessage ? 'No additional details.' : errorMessage}" /></div>
            </div>

            <div class="actions">
                <a class="btn btn-primary" href="${pageContext.request.contextPath}${dashboardPath}">Back to Dashboard</a>
                <a class="btn btn-alt" href="${pageContext.request.contextPath}/Login.jsp">Go to Login</a>
            </div>

            <p class="warn">If the problem keeps happening, contact admin support.</p>
        </section>
    </div>
</body>
</html>
