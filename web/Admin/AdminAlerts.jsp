<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Admin Alerts</title>
    <style>
        :root { --green:#7fc342; --line:#e8ece6; --muted:#5f6f63; --ink:#243228; --ok:#1f7c39; --ok-bg:#eaf7e7; --err:#9b1c1c; --err-bg:#ffecec; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:#f7faf5; color:var(--ink); }
        .site-header { position:sticky; top:0; z-index:30; background:#f7f8f6; border-bottom:1px solid #dfe5dc; }
        .header-inner { padding:14px clamp(12px,2.7vw,36px); }
        .header-top { display:flex; justify-content:space-between; align-items:center; gap:14px; flex-wrap:wrap; }
        .brand { display:flex; align-items:center; gap:10px; text-decoration:none; }
        .brand-logo { height:50px; width:auto; display:block; }
        .brand-text { font-weight:900; color:#47596b; letter-spacing:.08em; }
        .nav { margin-top:10px; padding-top:10px; border-top:1px solid #e4e9e1; display:flex; gap:14px; flex-wrap:wrap; justify-content:center; }
        .nav a { text-decoration:none; color:#2d352e; font-weight:800; font-size:.95rem; border:1px solid #d8e0d2; background:#fff; border-radius:999px; padding:8px 14px; }
        .layout { width:100%; max-width:none; margin:0; padding:18px clamp(12px,2.7vw,36px) 60px; }
        .top { display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; }
        .btn { border:none; border-radius:10px; padding:9px 12px; font-weight:800; background:var(--green); color:#fff; text-decoration:none; cursor:pointer; }
        .btn-alt { border:1px solid #d8e0d2; background:#fff; color:#304132; }
        .flash { margin:10px 0; padding:10px 12px; border-radius:10px; font-weight:700; }
        .ok { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .err { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }
        .card { margin-top:10px; background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .table-wrap { overflow:auto; border:1px solid #e5ece2; border-radius:10px; margin-top:8px; }
        table { width:100%; border-collapse:collapse; min-width:760px; }
        th, td { border-bottom:1px solid #edf3ea; padding:8px 10px; text-align:left; font-size:.9rem; }
        th { background:#f8fbf6; color:#5d6f61; }
        .actions { display:flex; gap:8px; flex-wrap:wrap; }
        .emergency-badge { display:inline-block; background:#9b1c1c; color:#fff; font-weight:800; padding:4px 8px; border-radius:999px; font-size:.78rem; }
    </style>
</head>
<body>
    <header class="site-header">
        <div class="header-inner">
            <div class="header-top">
                <a class="brand" href="${pageContext.request.contextPath}/AdminDashboard.do">
                    <img class="brand-logo" src="${pageContext.request.contextPath}/assets/tickify-admin-logo.svg" alt="Tickify Admin" onerror="this.style.display='none';">
                    <span class="brand-text">TICKIFY ADMIN</span>
                </a>
                <a class="btn btn-alt" href="${pageContext.request.contextPath}/LogoutServlet.do">Logout</a>
            </div>
            <nav class="nav">
                <a href="${pageContext.request.contextPath}/AdminDashboard.do">Dashboard</a>
                <a href="${pageContext.request.contextPath}/AdminAlerts.do">Alerts</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageAdmins.jsp">Admins</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageGuards.jsp">Guards</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageManagers.jsp">Managers</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManagePresenters.jsp">Presenters</a>
                <a href="${pageContext.request.contextPath}/AdminDatabase.do">Database</a>
            </nav>
        </div>
    </header>

    <main class="layout">
        <div class="top">
            <div>
                <h1 style="margin:0;">Admin Alerts</h1>
                <p style="margin:6px 0 0;color:#5f6f63;">Queue for locked profiles needing unblock and requests submitted by other admins.</p>
            </div>
            <div class="actions"></div>
        </div>

        <c:if test="${isPrivilegedAdmin}">
            <section class="card" style="margin-top:10px;">
                <h3 style="margin:0;">Campus Filter</h3>
                <form action="${pageContext.request.contextPath}/AdminAlerts.do" method="GET" class="actions" style="margin-top:8px;">
                    <select name="campus" style="border:1px solid #d8e0d2;border-radius:10px;padding:8px 10px;min-width:230px;">
                        <option value="">All campuses</option>
                        <c:forEach var="c" items="${campusOptions}">
                            <option value="${c.campusName}" <c:if test="${campusFilter eq c.campusName}">selected</c:if>>${c.campusName}</option>
                        </c:forEach>
                    </select>
                    <button class="btn" type="submit">Apply Filter</button>
                    <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminAlerts.do">Reset</a>
                </form>
            </section>
        </c:if>

        <c:if test="${param.msg != null}">
            <div class="flash ok">
                <c:choose>
                    <c:when test="${param.msg eq 'UserUnlocked'}">User unlocked successfully.</c:when>
                    <c:when test="${param.msg eq 'DeleteApproved'}">Delete request approved.</c:when>
                    <c:when test="${param.msg eq 'DeleteRejected'}">Delete request rejected.</c:when>
                    <c:when test="${param.msg eq 'MinorKeywordsSaved'}">Under-18 restriction keywords updated.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null}">
            <div class="flash err">
                <c:choose>
                    <c:when test="${param.err eq 'PrivilegedRequired'}">Only admin can perform this action.</c:when>
                    <c:when test="${param.err eq 'CampusScopeDenied'}">Action blocked outside your campus scope.</c:when>
                    <c:otherwise>Operation failed.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <section class="card">
            <h3 style="margin:0;">Accounts Pending Unblock</h3>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Role</th><th>User ID</th><th>Name</th><th>Email</th><th>Campus</th><th>Locked At</th><th>Action</th></tr></thead>
                    <tbody>
                        <c:forEach var="row" items="${lockedAccounts}">
                            <tr>
                                <td>${row.roleName}</td>
                                <td>${row.userID}</td>
                                <td>${row.firstname} ${row.lastname}</td>
                                <td>${row.email}</td>
                                <td>${row.campusName}</td>
                                <td>${row.updatedAt}</td>
                                <td>
                                    <c:if test="${isPrivilegedAdmin}">
                                        <form action="${pageContext.request.contextPath}/AdminAlerts.do" method="POST" class="actions">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="unlockUser">
                                            <input type="hidden" name="role" value="${row.roleName}">
                                            <input type="hidden" name="id" value="${row.userID}">
                                            <input type="hidden" name="campus" value="${campusFilter}">
                                            <button class="btn" type="submit">Unblock User</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty lockedAccounts}"><tr><td colspan="7">No locked accounts awaiting unblock.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="card">
            <h3 style="margin:0;">Other Admin Requests</h3>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Request ID</th><th>Requested By</th><th>Target</th><th>Query/Reason</th><th>Status</th><th>Requested At</th><th>Action</th></tr></thead>
                    <tbody>
                        <c:forEach var="dr" items="${deleteRequests}">
                            <tr>
                                <td>${dr.deleteRequestID}</td>
                                <td>${dr.requestedByFirst} ${dr.requestedByLast} (#${dr.requestedByAdminID})</td>
                                <td>${dr.targetRole}:${dr.targetUserID}</td>
                                <td>${dr.reason}</td>
                                <td>${dr.status}</td>
                                <td>${dr.requestedAt}</td>
                                <td>
                                    <c:if test="${isPrivilegedAdmin and dr.status eq 'PENDING'}">
                                        <form action="${pageContext.request.contextPath}/AdminAlerts.do" method="POST" class="actions">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="resolveDeleteRequest">
                                            <input type="hidden" name="deleteRequestID" value="${dr.deleteRequestID}">
                                            <input type="hidden" name="decision" value="approve">
                                            <input type="hidden" name="campus" value="${campusFilter}">
                                            <button class="btn" type="submit">Approve</button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/AdminAlerts.do" method="POST" class="actions" style="margin-top:4px;">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="resolveDeleteRequest">
                                            <input type="hidden" name="deleteRequestID" value="${dr.deleteRequestID}">
                                            <input type="hidden" name="decision" value="reject">
                                            <input type="hidden" name="campus" value="${campusFilter}">
                                            <button class="btn btn-alt" type="submit">Reject</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty deleteRequests}"><tr><td colspan="7">No admin requests found.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="card">
            <h3 style="margin:0;">Authentication Lockout Alerts</h3>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Alert ID</th><th>Action</th><th>Target</th><th>Details</th><th>Time</th></tr></thead>
                    <tbody>
                        <c:forEach var="log" items="${authLockAlerts}">
                            <tr>
                                <td>${log.adminAuditLogID}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty log.actionType or log.actionType eq '/' or log.actionType eq 'AUTH_LOCKOUT_ALERT' or fn:containsIgnoreCase(log.actionType, 'LOCKOUT')}">
                                            <span class="emergency-badge">EMERGENCY LOCKOUT</span>
                                        </c:when>
                                        <c:otherwise>${log.actionType}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty log.targetTable or log.targetTable eq '/'}">Account Control</c:when>
                                        <c:otherwise>${log.targetTable}/${log.targetID}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${log.details}</td>
                                <td>${log.createdAt}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty authLockAlerts}"><tr><td colspan="5">No lockout alerts found.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <c:if test="${isPrivilegedAdmin}">
            <section class="card">
                <h3 style="margin:0;">Under-18 Restriction Keywords</h3>
                <p style="margin:8px 0 0;color:#5f6f63;">Comma-separated event-type keywords that block purchases for under-18 attendees.</p>
                <form action="${pageContext.request.contextPath}/AdminAlerts.do" method="POST" style="margin-top:8px;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="saveMinorKeywords">
                    <textarea name="minorRestrictedKeywords" rows="3" style="width:100%;border:1px solid #d8e0d2;border-radius:10px;padding:10px;">${minorRestrictedKeywords}</textarea>
                    <div class="actions" style="margin-top:8px;">
                        <button class="btn" type="submit">Save Keywords</button>
                    </div>
                </form>
            </section>
        </c:if>
    </main>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
