<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Role Management</title>
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
        .title { margin:0 0 6px; }
        .muted { color:var(--muted); }
        .flash { margin:10px 0; padding:10px 12px; border-radius:10px; font-weight:700; }
        .ok { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .err { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }
        .grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
        .card { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .card h3 { margin:0 0 8px; }
        .field { display:flex; flex-direction:column; gap:5px; margin-bottom:8px; }
        .form-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px; }
        .form-grid .span-2 { grid-column:1 / -1; }
        .field label { font-weight:700; font-size:.9rem; }
        .field input, .field select { border:1px solid #d8e0d2; border-radius:10px; padding:9px 10px; font:inherit; }
        .btn { border:none; border-radius:10px; padding:9px 12px; font-weight:800; background:var(--green); color:#fff; cursor:pointer; }
        .btn-alt { border:1px solid #d8e0d2; background:#fff; color:#304132; }
        .table-wrap { margin-top:12px; overflow:auto; border:1px solid #e5ece2; border-radius:10px; }
        table { width:100%; border-collapse:collapse; min-width:720px; }
        th, td { border-bottom:1px solid #edf3ea; padding:8px 10px; text-align:left; font-size:.9rem; }
        th { background:#f8fbf6; color:#5d6f61; }
        .actions { display:flex; gap:6px; flex-wrap:wrap; }
        @media(max-width:768px){ .field input, .field select, .btn, .btn-alt, .actions input, .actions select, .actions button { font-size:16px; } }
        @media(max-width:980px){ .grid { grid-template-columns:1fr; } .form-grid { grid-template-columns:1fr; } }
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
                <a href="${pageContext.request.contextPath}/Admin/AdminManageAdmins.jsp">Admins</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageGuards.jsp">Guards</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageManagers.jsp">Managers</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManagePresenters.jsp">Presenters</a>
                <a href="${pageContext.request.contextPath}/AdminDatabase.do">Database</a>
            </nav>
        </div>
    </header>

    <main class="layout">
        <h2 class="title">Role Management: ${role}</h2>
        <p class="muted">Main admin can perform all actions. Other admins can create/edit/assign only within their campus scope and submit delete requests for approval.</p>

        <c:if test="${param.msg != null}">
            <div class="flash ok">
                <c:choose>
                    <c:when test="${param.msg == 'UserCreated'}">User account created successfully.</c:when>
                    <c:when test="${param.msg == 'UserUpdated'}">User account updated successfully.</c:when>
                    <c:when test="${param.msg == 'UserDeleted'}">User account deleted successfully.</c:when>
                    <c:when test="${param.msg == 'UserLocked'}">User account locked successfully.</c:when>
                    <c:when test="${param.msg == 'UserUnlocked'}">User account unlocked successfully.</c:when>
                    <c:when test="${param.msg == 'PasswordReset'}">User password reset successfully.</c:when>
                    <c:when test="${param.msg == 'RoleReassigned'}">User role reassigned successfully.</c:when>
                    <c:when test="${param.msg == 'DeleteRequested'}">Delete request submitted to admin for approval.</c:when>
                    <c:when test="${param.msg == 'DeleteApproved'}">Delete request approved and action completed.</c:when>
                    <c:when test="${param.msg == 'DeleteRejected'}">Delete request rejected by admin.</c:when>
                    <c:when test="${param.msg == 'NoChange'}">No changes were applied.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null}">
            <div class="flash err">
                <c:choose>
                    <c:when test="${param.err == 'RootAuthFailed'}">Root password is incorrect.</c:when>
                    <c:when test="${param.err == 'PrivilegedRequired'}">Only the main admin account can perform this operation.</c:when>
                    <c:when test="${param.err == 'CampusScopeDenied'}">This action is restricted to your assigned campus scope.</c:when>
                    <c:when test="${param.err == 'InvalidAssignment'}">Assignments must reference existing events, venues, guards, and institutions.</c:when>
                    <c:when test="${param.err == 'MissingFields'}">Please complete all required fields.</c:when>
                    <c:when test="${param.err == 'UnknownAction'}">Unknown action requested.</c:when>
                    <c:when test="${param.err == 'OperationFailed'}">Operation failed. Please try again.</c:when>
                    <c:otherwise>An error occurred. Check details and try again.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <c:if test="${!isPrivilegedAdmin}">
            <div class="flash err">Scoped admin mode: you can create/edit/assign users in your campus only. Delete is request-only and requires admin approval.</div>
        </c:if>

        <c:if test="${true}">
            <section class="grid">
                <article class="card">
                    <h3>Create ${role}</h3>
                    <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="create">
                        <input type="hidden" name="role" value="${role}">
                        <div class="form-grid">
                            <div class="field"><label>First Name</label><input type="text" name="firstName" autocomplete="given-name" required></div>
                            <div class="field"><label>Last Name</label><input type="text" name="lastName" autocomplete="family-name" required></div>
                            <div class="field"><label>Email</label><input type="email" name="email" autocomplete="email" required></div>
                            <div class="field"><label>Password</label><input type="text" name="password" autocomplete="new-password" required></div>
                            <c:if test="${role == 'ADMIN' || role == 'VENUE_GUARD' || role == 'TERTIARY_PRESENTER'}">
                                <div class="field"><label>Event</label><select name="eventID" required><option value="">Select</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name} (#${ev.eventID})</option></c:forEach></select></div>
                            </c:if>
                            <c:if test="${role == 'VENUE_GUARD' || role == 'TERTIARY_PRESENTER'}">
                                <div class="field"><label>Venue (Campus)</label><select name="venueID" required><option value="">Select campus</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                            </c:if>
                            <c:if test="${role == 'EVENT_MANAGER'}">
                                <div class="field"><label>Guard</label><select name="venueGuardID" required><option value="">Select</option><c:forEach var="g" items="${guardOptions}"><option value="${g.venueGuardID}">${g.firstname} ${g.lastname} (#${g.venueGuardID})</option></c:forEach></select></div>
                            </c:if>
                            <c:if test="${role == 'TERTIARY_PRESENTER'}">
                                <div class="field"><label>Institution</label><input type="text" name="tertiaryInstitution" autocomplete="organization" required></div>
                            </c:if>
                            <c:if test="${isPrivilegedAdmin}"><div class="field span-2"><label>Root Password</label><input type="password" name="rootPassword" autocomplete="current-password" required></div></c:if>
                        </div>
                        <button class="btn" type="submit">Create</button>
                    </form>
                </article>

                <article class="card">
                    <h3>Update / Incident Actions</h3>
                    <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="role" value="${role}">
                        <div class="form-grid">
                            <div class="field"><label>User ID</label><input type="number" name="id" min="1" autocomplete="off" required></div>
                            <div class="field"><label>First Name</label><input type="text" name="firstName" autocomplete="given-name" required></div>
                            <div class="field"><label>Last Name</label><input type="text" name="lastName" autocomplete="family-name" required></div>
                            <div class="field"><label>Email</label><input type="email" name="email" autocomplete="email" required></div>
                            <div class="field"><label>Event (optional)</label><select name="eventID"><option value="">Keep current</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name} (#${ev.eventID})</option></c:forEach></select></div>
                            <div class="field"><label>Venue/Campus (optional)</label><select name="venueID"><option value="">Keep current</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                            <div class="field"><label>Guard (optional)</label><select name="venueGuardID"><option value="">Keep current</option><c:forEach var="g" items="${guardOptions}"><option value="${g.venueGuardID}">${g.firstname} ${g.lastname} (#${g.venueGuardID})</option></c:forEach></select></div>
                            <div class="field"><label>Institution (optional)</label><input type="text" name="tertiaryInstitution" autocomplete="organization"></div>
                            <c:if test="${isPrivilegedAdmin}"><div class="field span-2"><label>Root Password</label><input type="password" name="rootPassword" autocomplete="current-password" required></div></c:if>
                        </div>
                        <button class="btn" type="submit">Update</button>
                    </form>

                    <div class="actions" style="margin-top:10px;">
                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="role" value="${role}"><input type="hidden" name="action" value="lock"><input type="number" name="id" placeholder="User ID" autocomplete="off" min="1" required><c:if test="${isPrivilegedAdmin}"><input type="password" name="rootPassword" autocomplete="current-password" placeholder="Root password" required></c:if><button class="btn" type="submit">Lock</button></form>
                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="role" value="${role}"><input type="hidden" name="action" value="unlock"><input type="number" name="id" placeholder="User ID" autocomplete="off" min="1" required><c:if test="${isPrivilegedAdmin}"><input type="password" name="rootPassword" autocomplete="current-password" placeholder="Root password" required></c:if><button class="btn" type="submit">Unlock</button></form>
                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="role" value="${role}"><input type="hidden" name="action" value="resetPassword"><input type="number" name="id" placeholder="User ID" autocomplete="off" min="1" required><input type="text" name="temporaryPassword" autocomplete="new-password" placeholder="Temp password" required><c:if test="${isPrivilegedAdmin}"><input type="password" name="rootPassword" autocomplete="current-password" placeholder="Root password" required></c:if><button class="btn" type="submit">Reset</button></form>
                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="role" value="${role}"><input type="hidden" name="action" value="delete"><input type="number" name="id" placeholder="User ID" autocomplete="off" min="1" required><input type="text" name="reason" autocomplete="off" placeholder="Reason (optional)"><c:if test="${isPrivilegedAdmin}"><input type="password" name="rootPassword" autocomplete="current-password" placeholder="Root password" required></c:if><button class="btn" type="submit"><c:choose><c:when test="${isPrivilegedAdmin}">Delete</c:when><c:otherwise>Request Delete</c:otherwise></c:choose></button></form>
                    </div>
                </article>
            </section>
        </c:if>

        <section class="card" style="margin-top:12px;">
            <h3>${role} Records</h3>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <c:choose>
                                <c:when test="${role == 'ADMIN'}"><th>adminID</th><th>firstname</th><th>lastname</th><th>email</th><th>eventID</th><th>campus</th></c:when>
                                <c:when test="${role == 'VENUE_GUARD'}"><th>venueGuardID</th><th>firstname</th><th>lastname</th><th>email</th><th>eventID</th><th>venueID</th><th>campus</th></c:when>
                                <c:when test="${role == 'EVENT_MANAGER'}"><th>eventManagerID</th><th>firstname</th><th>lastname</th><th>email</th><th>venueGuardID</th><th>campus</th></c:when>
                                <c:otherwise><th>tertiaryPresenterID</th><th>firstname</th><th>lastname</th><th>email</th><th>tertiaryInstitution</th><th>eventID</th><th>venueID</th><th>campus</th></c:otherwise>
                            </c:choose>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${records}">
                            <tr>
                                <c:choose>
                                    <c:when test="${role == 'ADMIN'}">
                                        <td>${row.adminID}</td><td>${row.firstname}</td><td>${row.lastname}</td><td>${row.email}</td><td>${row.eventID}</td><td>${row.campusName}</td>
                                    </c:when>
                                    <c:when test="${role == 'VENUE_GUARD'}">
                                        <td>${row.venueGuardID}</td><td>${row.firstname}</td><td>${row.lastname}</td><td>${row.email}</td><td>${row.eventID}</td><td>${row.venueID}</td><td>${row.campusName}</td>
                                    </c:when>
                                    <c:when test="${role == 'EVENT_MANAGER'}">
                                        <td>${row.eventManagerID}</td><td>${row.firstname}</td><td>${row.lastname}</td><td>${row.email}</td><td>${row.venueGuardID}</td><td>${row.campusName}</td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>${row.tertiaryPresenterID}</td><td>${row.firstname}</td><td>${row.lastname}</td><td>${row.email}</td><td>${row.tertiaryInstitution}</td><td>${row.eventID}</td><td>${row.venueID}</td><td>${row.campusName}</td>
                                    </c:otherwise>
                                </c:choose>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty records}"><tr><td colspan="9">No records for role ${role}.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <c:if test="${isPrivilegedAdmin}">
            <section class="card" style="margin-top:12px;">
                <h3>Role Reassignment</h3>
                <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST" class="actions">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="reassignRole">
                    <input type="hidden" name="role" value="${role}">
                    <input type="number" name="sourceId" placeholder="Source user ID" autocomplete="off" min="1" required>
                    <select name="targetRole" required>
                        <option value="ADMIN">ADMIN</option>
                        <option value="VENUE_GUARD">VENUE_GUARD</option>
                        <option value="EVENT_MANAGER">EVENT_MANAGER</option>
                        <option value="TERTIARY_PRESENTER">TERTIARY_PRESENTER</option>
                    </select>
                    <select name="eventID"><option value="">Event (optional)</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name}</option></c:forEach></select>
                    <select name="venueID"><option value="">Venue (optional)</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name}</option></c:forEach></select>
                    <select name="venueGuardID"><option value="">Guard (optional)</option><c:forEach var="g" items="${guardOptions}"><option value="${g.venueGuardID}">${g.firstname} ${g.lastname}</option></c:forEach></select>
                    <input type="text" name="tertiaryInstitution" autocomplete="organization" placeholder="Institution">
                    <input type="password" name="rootPassword" autocomplete="current-password" placeholder="Root password" required>
                    <button class="btn" type="submit">Reassign</button>
                </form>
            </section>
        </c:if>

        <section class="card" style="margin-top:12px;">
            <h3>Delete Requests</h3>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>ID</th><th>Requested By</th><th>Target</th><th>Reason</th><th>Status</th><th>Time</th><th>Action</th></tr></thead>
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
                                    <c:if test="${isPrivilegedAdmin && dr.status == 'PENDING'}">
                                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST" class="actions">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="role" value="${role}">
                                            <input type="hidden" name="action" value="resolveDeleteRequest">
                                            <input type="hidden" name="deleteRequestID" value="${dr.deleteRequestID}">
                                            <input type="hidden" name="decision" value="approve">
                                            <button class="btn" type="submit">Approve</button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/AdminRoleConsole.do" method="POST" class="actions" style="margin-top:4px;">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="role" value="${role}">
                                            <input type="hidden" name="action" value="resolveDeleteRequest">
                                            <input type="hidden" name="deleteRequestID" value="${dr.deleteRequestID}">
                                            <input type="hidden" name="decision" value="reject">
                                            <button class="btn btn-alt" type="submit">Reject</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty deleteRequests}"><tr><td colspan="7">No delete requests found.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
