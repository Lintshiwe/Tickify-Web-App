<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Admin Console</title>
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
        .profile-wrap { position:relative; }
        .profile-btn { display:flex; align-items:center; gap:10px; border:1px solid #d7ded3; background:#fff; border-radius:999px; padding:8px 10px; color:#2a312b; font-weight:700; cursor:pointer; }
        .profile-meta { max-width:0; opacity:0; overflow:hidden; white-space:nowrap; transition:max-width .28s ease, opacity .22s ease; }
        .profile-wrap:hover .profile-meta, .profile-wrap:focus-within .profile-meta { max-width:260px; opacity:1; }
        .profile-icon { width:28px; height:28px; border-radius:50%; background:#e6eedf; display:flex; align-items:center; justify-content:center; color:#4c5b4b; font-weight:800; }
        .profile-badge { border:1px solid #d6e8c8; background:#eff9e8; color:#2f5a22; border-radius:999px; font-size:.72rem; padding:3px 8px; font-weight:900; letter-spacing:.04em; }
        .profile-menu { position:absolute; right:0; top:calc(100% + 10px); min-width:220px; background:#fff; border:1px solid #dee5da; border-radius:12px; box-shadow:0 14px 26px rgba(24,32,20,.12); padding:8px; display:none; }
        .profile-menu.open { display:block; }
        .profile-menu a { display:block; text-decoration:none; color:#2c342d; border-radius:8px; padding:10px; font-weight:700; }
        .profile-menu a:hover { background:#f3f7f1; }
        .profile-menu .danger { color:#9b1c1c; background:#fff5f5; }
        .header-nav { margin-top:10px; padding-top:10px; border-top:1px solid #e4e9e1; display:flex; gap:14px; flex-wrap:wrap; justify-content:center; }
        .header-nav a { text-decoration:none; color:#2d352e; font-weight:800; font-size:.95rem; border:1px solid #d8e0d2; background:#fff; border-radius:999px; padding:8px 14px; }
        .layout { width:100%; max-width:none; margin:0; padding:18px clamp(12px,2.7vw,36px) 60px; }
        .hero { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .hero h1 { margin:0 0 6px; }
        .hero p { margin:0; color:var(--muted); }
        .flash { margin:10px 0; padding:10px 12px; border-radius:10px; font-weight:700; }
        .ok { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .err { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }
        .metrics { margin-top:10px; display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }
        .metric { background:#fff; border:1px solid var(--line); border-radius:12px; padding:12px; }
        .metric strong { font-size:1.3rem; display:block; color:#3a5c3c; }
        .metric span { color:var(--muted); font-size:.9rem; }
        .grid { margin-top:10px; display:grid; grid-template-columns:1fr 1fr; gap:10px; }
        .card { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .card h3 { margin:0 0 8px; }
        .actions { display:flex; gap:8px; flex-wrap:wrap; }
        .btn { border:none; border-radius:10px; padding:9px 12px; font-weight:800; background:var(--green); color:#fff; text-decoration:none; cursor:pointer; }
        .btn-alt { border:1px solid #d8e0d2; background:#fff; color:#304132; }
        .summary-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }
        .summary-item { background:#f9fcf7; border:1px solid #e4ece0; border-radius:10px; padding:10px; }
        .summary-item strong { color:#2f4a31; font-size:1.08rem; display:block; }
        .table-wrap { overflow:auto; border:1px solid #e5ece2; border-radius:10px; margin-top:8px; }
        table { width:100%; border-collapse:collapse; min-width:760px; }
        th, td { border-bottom:1px solid #edf3ea; padding:8px 10px; text-align:left; font-size:.9rem; }
        th { background:#f8fbf6; color:#5d6f61; }
        .field { display:flex; flex-direction:column; gap:5px; }
        .field input, .field select { border:1px solid #d8e0d2; border-radius:10px; padding:8px 10px; }
        .table-pager { margin-top:8px; display:flex; justify-content:space-between; align-items:center; gap:8px; flex-wrap:wrap; color:var(--muted); }
        .table-pager .pager-links { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
        .table-pager .pager-size { display:flex; gap:8px; align-items:center; }
        .table-pager .pager-size select { border:1px solid #d8e0d2; border-radius:8px; padding:5px 8px; background:#fff; }
        .modal-backdrop { display:none; position:fixed; inset:0; background:rgba(24,34,28,.45); z-index:90; align-items:center; justify-content:center; padding:14px; }
        .modal-card { width:min(460px,100%); background:#fff; border:1px solid #dbe5d8; border-radius:14px; padding:14px; box-shadow:0 18px 34px rgba(10,18,12,.22); }
        .modal-card h3 { margin:0 0 8px; }
        .modal-card p { margin:0 0 12px; color:var(--muted); }
        .modal-actions { display:flex; gap:8px; justify-content:flex-end; flex-wrap:wrap; }
        .root-form-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; align-items:end; }
        .audit-form-grid { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:8px; align-items:end; }
        @media(max-width:980px){ .metrics{grid-template-columns:repeat(2,minmax(0,1fr));} .grid{grid-template-columns:1fr;} .summary-grid{grid-template-columns:repeat(2,minmax(0,1fr));} .root-form-grid{grid-template-columns:1fr 1fr;} .audit-form-grid{grid-template-columns:1fr 1fr;} }
        @media(max-width:768px){ .field input, .field select, .btn{font-size:16px;} }
        @media(max-width:620px){ .root-form-grid, .audit-form-grid{grid-template-columns:1fr;} }
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
                <div class="profile-wrap">
                    <button class="profile-btn" id="profileBtn" type="button" onclick="toggleProfileMenu()">
                        <span class="profile-icon">A</span>
                        <span class="profile-meta">${sessionScope.userFullName} | ${sessionScope.userRoleNumberLabel} | ${adminCampusDisplayName}</span>
                        <c:if test="${isPrivilegedAdmin}">
                            <span class="profile-badge">MAIN ADMIN</span>
                        </c:if>
                    </button>
                    <div class="profile-menu" id="profileMenu">
                        <a href="${pageContext.request.contextPath}/AdminDashboard.do">Dashboard</a>
                        <a href="${pageContext.request.contextPath}/AdminAdverts.do">Adverts</a>
                        <a href="${pageContext.request.contextPath}/AdminEventAlbum.do">Event Albums</a>
                        <a href="${pageContext.request.contextPath}/LogoutServlet.do" class="danger">Logout</a>
                    </div>
                </div>
            </div>
            <nav class="header-nav" aria-label="Admin navigation">
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
        <section class="hero">
            <h1>Admin Console</h1>
            <p>Operations dashboard with dedicated audit visibility and role/database management pages.</p>
        </section>

        <c:if test="${param.msg != null}">
            <div class="flash ok">
                <c:choose>
                    <c:when test="${param.msg == 'UserCreated'}">User account created successfully.</c:when>
                    <c:when test="${param.msg == 'GuardProvisioned'}">Venue guard provisioned successfully.</c:when>
                    <c:when test="${param.msg == 'ManagerProvisioned'}">Event manager provisioned successfully.</c:when>
                    <c:when test="${param.msg == 'EventCreated'}">Event created successfully.</c:when>
                    <c:when test="${param.msg == 'EventUpdated'}">Event updated successfully.</c:when>
                    <c:when test="${param.msg == 'EventCoverUpdated'}">Event album cover updated successfully.</c:when>
                    <c:when test="${param.msg == 'EventDeleted'}">Event deleted successfully.</c:when>
                    <c:when test="${param.msg == 'TicketCreated'}">Ticket batch created successfully.</c:when>
                    <c:when test="${param.msg == 'TicketUpdated'}">Ticket updated successfully.</c:when>
                    <c:when test="${param.msg == 'TicketDeleted'}">Ticket deleted successfully.</c:when>
                    <c:when test="${param.msg == 'UserUpdated'}">User account updated successfully.</c:when>
                    <c:when test="${param.msg == 'UserDeleted'}">User account deleted successfully.</c:when>
                    <c:when test="${param.msg == 'UserLocked'}">User account locked successfully.</c:when>
                    <c:when test="${param.msg == 'UserUnlocked'}">User account unlocked successfully.</c:when>
                    <c:when test="${param.msg == 'PasswordReset'}">User password reset successfully.</c:when>
                    <c:when test="${param.msg == 'RoleReassigned'}">User role reassigned successfully.</c:when>
                    <c:when test="${param.msg == 'DeleteRequested'}">Delete request submitted to admin for approval.</c:when>
                    <c:when test="${param.msg == 'DeleteApproved'}">Delete request approved and action completed.</c:when>
                    <c:when test="${param.msg == 'DeleteRejected'}">Delete request rejected by admin.</c:when>
                    <c:when test="${param.msg == 'ProposalCreated'}">Event proposal submitted successfully.</c:when>
                    <c:when test="${param.msg == 'ProposalApproved'}">Event proposal approved and event created.</c:when>
                    <c:when test="${param.msg == 'ProposalRejected'}">Event proposal rejected.</c:when>
                    <c:when test="${param.msg == 'RefundCaseCreated'}">Refund case logged for campus support.</c:when>
                    <c:when test="${param.msg == 'RefundApproved'}">Refund case approved.</c:when>
                    <c:when test="${param.msg == 'RefundRejected'}">Refund case rejected.</c:when>
                    <c:when test="${param.msg == 'RowDeleted'}">Database row deleted successfully.</c:when>
                    <c:when test="${param.msg == 'RootPasswordRotated'}">Root password rotated successfully.</c:when>
                    <c:when test="${param.msg == 'ScanLogsPurged'}">Scan logs purged successfully.</c:when>
                    <c:when test="${param.msg == 'EmailHealthCheckPassed'}">Email health check passed and message sent successfully.</c:when>
                    <c:when test="${param.msg == 'RetiredEventsDeleted'}">Retired events cleanup completed. Removed ${param.rows} event(s).</c:when>
                    <c:when test="${param.msg == 'ProfileUpdated'}">Your admin profile was updated successfully.</c:when>
                    <c:when test="${param.msg == 'NoChange'}">No changes were applied.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null}">
            <div class="flash err">
                <c:choose>
                    <c:when test="${param.err == 'RootAuthFailed'}">Current root password is incorrect.</c:when>
                    <c:when test="${param.err == 'RootPasswordMismatch'}">New root password and confirmation do not match.</c:when>
                    <c:when test="${param.err == 'PrivilegedRequired'}">Only the main admin account can perform this operation.</c:when>
                    <c:when test="${param.err == 'CampusScopeDenied'}">This action is restricted to your assigned campus scope.</c:when>
                    <c:when test="${param.err == 'InvalidAssignment'}">Assignment values must reference existing events, venues, guards, and institutions.</c:when>
                    <c:when test="${param.err == 'EventHasSales'}">Event cannot be deleted because tickets were already sold.</c:when>
                    <c:when test="${param.err == 'TicketHasSales'}">Ticket cannot be changed or deleted because it was sold.</c:when>
                    <c:when test="${param.err == 'TicketHasScans'}">Ticket cannot be deleted because scan history exists.</c:when>
                    <c:when test="${param.err == 'InvalidImage'}">Please upload a valid image file for the event album cover.</c:when>
                    <c:when test="${param.err == 'MissingFields'}">Please complete all required fields.</c:when>
                    <c:when test="${param.err == 'EmailInUse'}">That email address is already used by another admin account.</c:when>
                    <c:when test="${param.err == 'UnknownAction'}">Unknown action requested.</c:when>
                    <c:when test="${param.err == 'EmailHealthCheckFailed'}">Email health check failed. Verify SMTP settings and recipient email.</c:when>
                    <c:when test="${param.err == 'OperationFailed'}">Operation failed. Please try again.</c:when>
                    <c:otherwise>An error occurred. Please verify your input and try again.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <section class="metrics">
            <article class="metric"><strong>${metrics.activeEvents}</strong><span>Active events</span></article>
            <article class="metric"><strong>${metrics.ticketsSold}</strong><span>Tickets sold</span></article>
            <article class="metric"><strong>R <fmt:formatNumber value="${metrics.revenue}" minFractionDigits="2" maxFractionDigits="2"/></strong><span>Revenue</span></article>
            <article class="metric"><strong><fmt:formatNumber value="${metrics.scannerUptime}" minFractionDigits="2" maxFractionDigits="2"/>%</strong><span>Scanner uptime</span></article>
        </section>

        <section class="grid">
            <article class="card">
                <h3>Role Management Pages</h3>
                <p style="color:#5f6f63;margin:0 0 8px;">Admin can do all actions. Other admins can create/edit/assign only in their campus and submit delete requests.</p>
                <div class="actions">
                    <a class="btn" href="${pageContext.request.contextPath}/Admin/AdminManageAdmins.jsp">Admins</a>
                    <a class="btn" href="${pageContext.request.contextPath}/Admin/AdminManageGuards.jsp">Guards</a>
                    <a class="btn" href="${pageContext.request.contextPath}/Admin/AdminManageManagers.jsp">Managers</a>
                    <a class="btn" href="${pageContext.request.contextPath}/Admin/AdminManagePresenters.jsp">Presenters</a>
                </div>
                <div class="actions" style="margin-top:8px;">
                    <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDatabase.do">Open Database Page</a>
                    <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminAlerts.do">Open Alerts Page</a>
                </div>
                <c:if test="${!isPrivilegedAdmin}">
                    <div class="flash err" style="margin:8px 0 0;">Scoped admin mode: operations outside your campus and direct delete actions are blocked.</div>
                </c:if>

                <h3 style="margin-top:12px;">Provisioning Workflow</h3>
                <p style="margin:0 0 8px;color:#5f6f63;">Create guard and manager accounts directly from operations control.</p>
                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="provisionGuard">
                    <div class="field"><label>Guard First Name</label><input type="text" name="firstName" required></div>
                    <div class="field"><label>Guard Last Name</label><input type="text" name="lastName" required></div>
                    <div class="field"><label>Guard Email</label><input type="email" name="email" required></div>
                    <div class="field"><label>Temporary Password</label><input type="text" name="password" required></div>
                    <div class="field"><label>Event</label><select name="eventID" required><option value="">Select</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name} (#${ev.eventID})</option></c:forEach></select></div>
                    <div class="field"><label>Campus Venue</label><select name="venueID" required><option value="">Select</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                    <div class="field"><button class="btn" type="submit">Provision Guard</button></div>
                </form>

                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="provisionManager">
                    <div class="field"><label>Manager First Name</label><input type="text" name="firstName" required></div>
                    <div class="field"><label>Manager Last Name</label><input type="text" name="lastName" required></div>
                    <div class="field"><label>Manager Email</label><input type="email" name="email" required></div>
                    <div class="field"><label>Temporary Password</label><input type="text" name="password" required></div>
                    <div class="field"><label>Assigned Guard</label><select name="venueGuardID" required><option value="">Select</option><c:forEach var="g" items="${guardOptions}"><option value="${g.venueGuardID}">${g.firstname} ${g.lastname} (#${g.venueGuardID})</option></c:forEach></select></div>
                    <div class="field"><button class="btn" type="submit">Provision Manager</button></div>
                </form>
            </article>
            <article class="card">
                <h3>My Admin Profile</h3>
                <p style="margin:0 0 8px;color:#5f6f63;">Update your admin identity details. Leave password blank to keep your current password.</p>
                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="updateMyProfile">
                    <div class="field"><label>First Name</label><input type="text" name="firstName" value="${myAdminProfile.firstname}" required></div>
                    <div class="field"><label>Last Name</label><input type="text" name="lastName" value="${myAdminProfile.lastname}" required></div>
                    <div class="field"><label>Email</label><input type="email" name="email" value="${myAdminProfile.email}" required></div>
                    <div class="field"><label>New Password (Optional)</label><input type="password" name="newPassword" minlength="6" placeholder="Leave blank to keep current"></div>
                    <div class="field"><button class="btn" type="submit">Save Profile</button></div>
                </form>

                <h3>Database Summary</h3>
                <div class="summary-grid">
                    <c:forEach var="row" items="${tableSummary}">
                        <div class="summary-item"><strong>${row.count}</strong><span style="color:#5f6f63;">${row.label}</span></div>
                    </c:forEach>
                </div>
            </article>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Event Operations</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">Create, update, and delete events with full metadata, validation, and campus scope checks.</p>
            <c:if test="${autoRetiredCleanupRows != null && autoRetiredCleanupRows > 0}">
                <div class="flash ok" style="margin-top:0;">Automatic cleanup removed ${autoRetiredCleanupRows} passed/cancelled event(s).</div>
            </c:if>

            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" enctype="multipart/form-data" style="display:grid;grid-template-columns:repeat(8,minmax(0,1fr));gap:8px;align-items:end;">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="createEvent">
                <div class="field"><label>Event Name</label><input type="text" name="eventName" required></div>
                <div class="field"><label>Type</label><input type="text" name="eventType" required></div>
                <div class="field"><label>Date/Time</label><input type="datetime-local" name="eventDate" required></div>
                <div class="field"><label>Venue</label><select name="venueID" required><option value="">Select</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                <div class="field"><label>Status</label><select name="eventStatus"><option value="ACTIVE">Active</option><option value="CANCELLED">Cancelled</option><option value="PASSED">Passed</option></select></div>
                <div class="field" style="grid-column:span 2;"><label>Description</label><input type="text" name="eventDescription" maxlength="1200" placeholder="Event details"></div>
                <div class="field"><label>Info URL</label><input type="url" name="eventInfoUrl" maxlength="255" placeholder="https://"></div>
                <div class="field" style="grid-column:span 2;"><label>Album Cover (all image formats)</label><input type="file" name="eventAlbumImage" accept="image/*"></div>
                <div class="field"><label>Initial Ticket Name</label><input type="text" name="initialTicketName" maxlength="45" placeholder="Optional"></div>
                <div class="field"><label>Initial Ticket Price</label><input type="number" name="initialTicketPrice" step="0.01" min="0" placeholder="Optional"></div>
                <div class="field"><label>Number of Tickets</label><input type="number" name="initialTicketQuantity" min="1" placeholder="Optional"></div>
                <div class="field"><button class="btn" type="submit">Create Event</button></div>
            </form>

            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" enctype="multipart/form-data" style="display:grid;grid-template-columns:repeat(8,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="updateEvent">
                <div class="field"><label>Event ID</label><input type="number" name="eventID" min="1" required></div>
                <div class="field"><label>Event Name</label><input type="text" name="eventName" required></div>
                <div class="field"><label>Type</label><input type="text" name="eventType" required></div>
                <div class="field"><label>Date/Time</label><input type="datetime-local" name="eventDate" required></div>
                <div class="field"><label>Venue</label><select name="venueID" required><option value="">Select</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                <div class="field"><label>Status</label><select name="eventStatus"><option value="ACTIVE">Active</option><option value="CANCELLED">Cancelled</option><option value="PASSED">Passed</option></select></div>
                <div class="field" style="grid-column:span 2;"><label>Description</label><input type="text" name="eventDescription" maxlength="1200" placeholder="Event details"></div>
                <div class="field"><label>Info URL</label><input type="url" name="eventInfoUrl" maxlength="255" placeholder="https://"></div>
                <div class="field" style="grid-column:span 2;"><label>Album Cover (all image formats)</label><input type="file" name="eventAlbumImage" accept="image/*"></div>
                <div class="field"><button class="btn" type="submit">Update Event</button></div>
            </form>

            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="deleteEvent">
                <div class="field"><label>Event ID</label><input type="number" name="eventID" min="1" required></div>
                <div class="field"><button class="btn btn-alt" type="button" onclick="openDeleteEventModal(this.form)">Delete Event</button></div>
            </form>

            <c:if test="${isPrivilegedAdmin}">
                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="cleanupRetiredEvents">
                    <div class="field" style="grid-column:span 2;"><label>Retired Event Cleanup</label><input type="text" value="Deletes passed/cancelled events with no sold tickets and no user references" readonly></div>
                    <div class="field"><button class="btn" type="submit">Delete Passed/Cancelled Events</button></div>
                </form>
            </c:if>

            <div class="table-wrap">
                <div class="actions" style="padding:8px; border-bottom:1px solid #e5ece1;">
                    <label for="eventStatusQuickFilter" style="color:#5f6f63;font-weight:700;">Quick Filter:</label>
                    <select id="eventStatusQuickFilter" onchange="applyEventQuickFilter()" style="margin-left:8px; border:1px solid #d8e0d2; border-radius:8px; padding:6px 8px;">
                        <option value="ALL">All Events</option>
                        <option value="ACTIVE">Active</option>
                        <option value="CANCELLED">Cancelled</option>
                        <option value="PASSED">Passed</option>
                        <option value="CLEANUP">Cleanup Candidates (Passed/Cancelled)</option>
                    </select>
                    <span id="eventQuickFilterSummary" style="margin-left:10px;color:#5f6f63;">Showing all events</span>
                </div>
                <table>
                    <thead><tr><th>Event ID</th><th>Cover</th><th>Replace Cover</th><th>Name</th><th>Type</th><th>Status</th><th>Date</th><th>Description</th><th>Info URL</th><th>Venue ID</th><th>Campus</th></tr></thead>
                    <tbody>
                        <c:forEach var="ev" items="${eventRows}">
                            <tr class="event-row" data-status="${ev.status}" data-date="${ev.date}">
                                <td>${ev.eventID}</td>
                                <td><img src="${pageContext.request.contextPath}/EventAlbumImage.do?eventID=${ev.eventID}" alt="Event cover" style="width:72px;height:42px;object-fit:cover;border-radius:6px;border:1px solid #d8e0d2;"></td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" enctype="multipart/form-data" style="display:flex;gap:6px;align-items:center;">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="uploadEventCoverOnly">
                                        <input type="hidden" name="eventID" value="${ev.eventID}">
                                        <input type="file" name="eventAlbumImage" accept="image/*" required style="max-width:170px;">
                                        <button class="btn btn-alt" type="submit">Replace</button>
                                    </form>
                                </td>
                                <td>${ev.name}</td>
                                <td>${ev.type}</td>
                                <td>${ev.status}</td>
                                <td>${ev.date}</td>
                                <td>${ev.description}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty ev.infoUrl}"><a href="${ev.infoUrl}" target="_blank" rel="noopener noreferrer">Open</a></c:when>
                                        <c:otherwise>-</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${ev.venueID}</td>
                                <td>${ev.campusName}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty eventRows}"><tr><td colspan="11">No events found for your scope.</td></tr></c:if>
                    </tbody>
                </table>
            </div>

            <div class="table-pager">
                <div>Showing ${fn:length(eventRows)} of ${eventTotalRows} events | Page ${eventPage} of ${eventTotalPages}</div>
                <div class="pager-links">
                    <c:if test="${eventPage > 1}">
                        <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?eventPage=${eventPage - 1}&eventPageSize=${eventPageSize}&reconPage=${reconPage}&reconPageSize=${reconPageSize}&eventStatusQuickFilter=${param.eventStatusQuickFilter}">Previous</a>
                    </c:if>
                    <c:if test="${eventPage < eventTotalPages}">
                        <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?eventPage=${eventPage + 1}&eventPageSize=${eventPageSize}&reconPage=${reconPage}&reconPageSize=${reconPageSize}&eventStatusQuickFilter=${param.eventStatusQuickFilter}">Next</a>
                    </c:if>
                </div>
                <form class="pager-size" action="${pageContext.request.contextPath}/AdminDashboard.do" method="GET">
                    <input type="hidden" name="eventPage" value="1">
                    <input type="hidden" name="reconPage" value="${reconPage}">
                    <input type="hidden" name="reconPageSize" value="${reconPageSize}">
                    <input type="hidden" id="eventStatusQuickFilterHidden" name="eventStatusQuickFilter" value="${param.eventStatusQuickFilter}">
                    <label for="eventPageSize">Rows</label>
                    <select id="eventPageSize" name="eventPageSize" onchange="this.form.submit()">
                        <option value="10" <c:if test="${eventPageSize == 10}">selected</c:if>>10</option>
                        <option value="25" <c:if test="${eventPageSize == 25}">selected</c:if>>25</option>
                        <option value="50" <c:if test="${eventPageSize == 50}">selected</c:if>>50</option>
                    </select>
                </form>
            </div>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Delete Requests</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">Other admins submit delete requests. Admin reviews and approves/rejects.</p>
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
                                        <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="resolveDeleteRequest">
                                            <input type="hidden" name="deleteRequestID" value="${dr.deleteRequestID}">
                                            <input type="hidden" name="decision" value="approve">
                                            <button class="btn" type="submit">Approve</button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions" style="margin-top:4px;">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
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

        <section class="grid">
            <article class="card">
                <h3>Event Proposals</h3>
                <p style="margin:0 0 8px;color:#5f6f63;">Campus admins can propose events and review pending proposals in their campus scope.</p>

                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="createEventProposal">
                    <div class="field"><label>Event Name</label><input type="text" name="eventName" required></div>
                    <div class="field"><label>Type</label><input type="text" name="eventType" required></div>
                    <div class="field"><label>Date/Time</label><input type="datetime-local" name="eventDate" required></div>
                    <div class="field"><label>Venue</label><select name="venueID" required><option value="">Select</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                    <div class="field" style="grid-column:span 2;"><label>Notes</label><input type="text" name="notes" placeholder="Optional context for reviewers"></div>
                    <div class="field"><button class="btn" type="submit">Submit Proposal</button></div>
                </form>

                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>Event</th><th>Campus</th><th>Status</th><th>Submitted By</th><th>Action</th></tr></thead>
                        <tbody>
                            <c:forEach var="p" items="${eventProposals}">
                                <tr>
                                    <td>${p.proposalID}</td>
                                    <td>${p.eventName} (${p.eventType})</td>
                                    <td>${p.campusName}</td>
                                    <td>${p.status}</td>
                                    <td>${p.submittedFirst} ${p.submittedLast} (#${p.submittedByAdminID})</td>
                                    <td>
                                        <c:if test="${p.status == 'PENDING'}">
                                            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="reviewEventProposal">
                                                <input type="hidden" name="proposalID" value="${p.proposalID}">
                                                <input type="hidden" name="decision" value="approve">
                                                <button class="btn" type="submit">Approve</button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions" style="margin-top:4px;">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="reviewEventProposal">
                                                <input type="hidden" name="proposalID" value="${p.proposalID}">
                                                <input type="hidden" name="decision" value="reject">
                                                <button class="btn btn-alt" type="submit">Reject</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty eventProposals}"><tr><td colspan="6">No event proposals found.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </article>

            <article class="card">
                <h3>Refund Cases</h3>
                <p style="margin:0 0 8px;color:#5f6f63;">Track and resolve attendee refund/support requests by campus.</p>

                <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="createRefundCase">
                    <div class="field"><label>Attendee ID</label><input type="number" name="attendeeID" min="1" required></div>
                    <div class="field"><label>Order ID (optional)</label><input type="number" name="orderID" min="1"></div>
                    <div class="field"><label>Event ID (optional)</label><input type="number" name="eventID" min="1"></div>
                    <div class="field" style="grid-column:span 2;"><label>Reason</label><input type="text" name="reason" required></div>
                    <div class="field"><button class="btn" type="submit">Log Refund Case</button></div>
                </form>

                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>Attendee</th><th>Event</th><th>Status</th><th>Reason</th><th>Action</th></tr></thead>
                        <tbody>
                            <c:forEach var="r" items="${refundRequests}">
                                <tr>
                                    <td>${r.refundRequestID}</td>
                                    <td>${r.attendeeFirst} ${r.attendeeLast} (#${r.attendeeID})</td>
                                    <td>${r.eventName}</td>
                                    <td>${r.status}</td>
                                    <td>${r.reason}</td>
                                    <td>
                                        <c:if test="${r.status == 'PENDING'}">
                                            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="resolveRefundCase">
                                                <input type="hidden" name="refundRequestID" value="${r.refundRequestID}">
                                                <input type="hidden" name="decision" value="approve">
                                                <button class="btn" type="submit">Approve</button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="actions" style="margin-top:4px;">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="resolveRefundCase">
                                                <input type="hidden" name="refundRequestID" value="${r.refundRequestID}">
                                                <input type="hidden" name="decision" value="reject">
                                                <button class="btn btn-alt" type="submit">Reject</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty refundRequests}"><tr><td colspan="6">No refund cases found.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </article>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Root Password Settings</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">For privileged admin, root password is the same as login password.</p>
            <p style="margin:0 0 10px;color:#5f6f63;">
                Current source: <strong>${rootPasswordStatus.source}</strong>
                <c:if test="${rootPasswordStatus.updatedAt != null}">| Updated: ${rootPasswordStatus.updatedAt}</c:if>
            </p>
            <c:choose>
                <c:when test="${isPrivilegedAdmin}">
                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="root-form-grid">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="rotateRootPassword">
                        <div class="field"><label>Current Login Password</label><input type="password" name="currentRootPassword" required></div>
                        <div class="field"><label>New Login/Root Password</label><input type="password" name="newRootPassword" minlength="6" required></div>
                        <div class="field"><label>Confirm New Password</label><input type="password" name="confirmRootPassword" minlength="6" required oninput="validateRootConfirm(this)"></div>
                        <div class="field"><button class="btn" type="submit">Rotate Password</button></div>
                    </form>
                </c:when>
            </c:choose>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Email Health Check</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">Sends a live SMTP password-reset test email to verify outbound email flow.</p>
            <c:choose>
                <c:when test="${isPrivilegedAdmin}">
                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" class="root-form-grid">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="runEmailHealthCheck">
                        <div class="field" style="grid-column:span 3;"><label>Recipient Email</label><input type="email" name="healthCheckEmail" value="${sessionScope.userEmail}" required></div>
                        <div class="field"><button class="btn" type="submit">Run Email Health Check</button></div>
                    </form>
                </c:when>
                <c:otherwise>
                    <p style="margin:0;color:#5f6f63;">Only main admin can run this operation.</p>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Ticket Management</h3>
            <c:choose>
                <c:when test="${isPrivilegedAdmin}">
                    <p style="margin:0 0 8px;color:#5f6f63;">Main admin can create, update, and delete existing ticket records.</p>

                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:8px;align-items:end;">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="createTicket">
                        <div class="field"><label>Event</label><select name="eventID" required><option value="">Select</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name} (#${ev.eventID})</option></c:forEach></select></div>
                        <div class="field"><label>Ticket Name</label><input type="text" name="ticketName" maxlength="45" required></div>
                        <div class="field"><label>Price</label><input type="number" name="ticketPrice" step="0.01" min="0" required></div>
                        <div class="field"><label>Quantity</label><input type="number" name="ticketQuantity" min="1" value="1" required></div>
                        <div class="field"><button class="btn" type="submit">Create Ticket Batch</button></div>
                    </form>

                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="updateTicket">
                        <div class="field"><label>Ticket ID</label><input type="number" name="ticketID" min="1" required></div>
                        <div class="field"><label>Event ID</label><input type="number" name="eventID" min="1" required></div>
                        <div class="field"><label>Ticket Name</label><input type="text" name="ticketName" maxlength="45" required></div>
                        <div class="field"><label>Price</label><input type="number" name="ticketPrice" step="0.01" min="0" required></div>
                        <div class="field"><button class="btn" type="submit">Update Ticket</button></div>
                    </form>

                    <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;align-items:end;margin-top:8px;">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="deleteTicket">
                        <div class="field"><label>Ticket ID</label><input type="number" name="ticketID" min="1" required></div>
                        <div class="field"><button class="btn btn-alt" type="submit">Delete Ticket</button></div>
                    </form>
                </c:when>
                <c:otherwise>
                    <p style="margin:0;color:#5f6f63;">Only main admin can perform ticket CRUD. Campus admins can still edit event details in their scope.</p>
                </c:otherwise>
            </c:choose>

            <div class="table-wrap" style="margin-top:8px;">
                <table>
                    <thead><tr><th>Ticket ID</th><th>Ticket</th><th>Event</th><th>Campus</th><th>Price</th><th>Sold</th><th>Scans</th></tr></thead>
                    <tbody>
                        <c:forEach var="t" items="${ticketRows}">
                            <tr>
                                <td>${t.ticketID}</td>
                                <td>${t.ticketName}</td>
                                <td>${t.eventName} (#${t.eventID})</td>
                                <td>${t.campusName}</td>
                                <td>R <fmt:formatNumber value="${t.price}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td>${t.soldCount}</td>
                                <td>${t.scanCount}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty ticketRows}"><tr><td colspan="7">No tickets found for your scope.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Ticket Intelligence</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">Tracks purchaser, ticket, scan result, and guard accountability from database records.</p>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Ticket</th><th>Purchaser</th><th>Purchaser Email</th><th>Event</th><th>Campus</th><th>Price</th><th>Scan Result</th><th>Guard</th><th>Scanned At</th></tr></thead>
                    <tbody>
                        <c:forEach var="row" items="${ticketIntelligence}">
                            <tr>
                                <td>${row.ticketNumber} (#${row.ticketID})</td>
                                <td>${row.attendeeFirst} ${row.attendeeLast}</td>
                                <td>${row.attendeeEmail}</td>
                                <td>${row.eventName}</td>
                                <td>${row.venueName}</td>
                                <td>R <fmt:formatNumber value="${row.price}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td>${row.scanResult}</td>
                                <td>${row.guardName}</td>
                                <td>${row.scannedAt}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty ticketIntelligence}"><tr><td colspan="9">No ticket intelligence rows found.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Event Popularity</h3>
            <p style="margin:0 0 8px;color:#5f6f63;">Live engagement ranking based on attendee preview opens and social shares.</p>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Event</th><th>Preview Opens</th><th>Social Shares</th><th>Popularity Score</th></tr></thead>
                    <tbody>
                        <c:forEach var="row" items="${eventPopularity}">
                            <tr>
                                <td>${row.eventName} (#${row.eventID})</td>
                                <td>${row.previews}</td>
                                <td>${row.shares}</td>
                                <td>${row.shares * 3 + row.previews}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty eventPopularity}"><tr><td colspan="4">No popularity data available yet.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="grid">
            <article class="card">
                <h3>Campus Revenue</h3>
                <div class="actions" style="margin-bottom:8px;">
                    <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?export=finance">Export Revenue CSV</a>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Campus</th><th>Address</th><th>Tickets Sold</th><th>Revenue</th></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${campusRevenue}">
                                <tr>
                                    <td>${row.campusName}</td>
                                    <td>${row.campusAddress}</td>
                                    <td>${row.ticketsSold}</td>
                                    <td>R <fmt:formatNumber value="${row.revenue}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty campusRevenue}"><tr><td colspan="4">No campus revenue rows found.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </article>
            <article class="card">
                <h3>Financial Reconciliation</h3>
                <div class="actions" style="margin-bottom:8px;">
                    <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?export=reconciliation">Export Reconciliation CSV</a>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Campus</th><th>Sold</th><th>Validated</th><th>Ticket Delta</th><th>Recorded Revenue</th><th>Validated Revenue</th><th>Revenue Delta</th><th>Status</th></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${reconciliation}">
                                <tr>
                                    <td>${row.campusName}</td>
                                    <td>${row.soldTickets}</td>
                                    <td>${row.validatedTickets}</td>
                                    <td>${row.ticketDelta}</td>
                                    <td>R <fmt:formatNumber value="${row.recordedRevenue}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                    <td>R <fmt:formatNumber value="${row.validatedRevenue}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                    <td>R <fmt:formatNumber value="${row.revenueDelta}" minFractionDigits="2" maxFractionDigits="2"/></td>
                                    <td>${row.status}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty reconciliation}"><tr><td colspan="8">No reconciliation rows found.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>

                <div class="table-pager">
                    <div>Showing ${fn:length(reconciliation)} of ${reconTotalRows} campuses | Page ${reconPage} of ${reconTotalPages}</div>
                    <div class="pager-links">
                        <c:if test="${reconPage > 1}">
                            <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?eventPage=${eventPage}&eventPageSize=${eventPageSize}&reconPage=${reconPage - 1}&reconPageSize=${reconPageSize}">Previous</a>
                        </c:if>
                        <c:if test="${reconPage < reconTotalPages}">
                            <a class="btn btn-alt" href="${pageContext.request.contextPath}/AdminDashboard.do?eventPage=${eventPage}&eventPageSize=${eventPageSize}&reconPage=${reconPage + 1}&reconPageSize=${reconPageSize}">Next</a>
                        </c:if>
                    </div>
                    <form class="pager-size" action="${pageContext.request.contextPath}/AdminDashboard.do" method="GET">
                        <input type="hidden" name="reconPage" value="1">
                        <input type="hidden" name="eventPage" value="${eventPage}">
                        <input type="hidden" name="eventPageSize" value="${eventPageSize}">
                        <label for="reconPageSize">Rows</label>
                        <select id="reconPageSize" name="reconPageSize" onchange="this.form.submit()">
                            <option value="10" <c:if test="${reconPageSize == 10}">selected</c:if>>10</option>
                            <option value="25" <c:if test="${reconPageSize == 25}">selected</c:if>>25</option>
                            <option value="50" <c:if test="${reconPageSize == 50}">selected</c:if>>50</option>
                        </select>
                    </form>
                </div>

                <h3>Campus Ownership and Responsibility</h3>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Campus</th><th>Database</th><th>Admins</th><th>Managers</th><th>Students</th></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${campusOwnership}">
                                <tr>
                                    <td>${row.campusName}</td>
                                    <td>${row.databaseOwner}</td>
                                    <td>${row.admins}</td>
                                    <td>${row.managers}</td>
                                    <td>${row.studentCount}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty campusOwnership}"><tr><td colspan="5">No campus ownership rows found.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </article>
        </section>

        <section class="card" style="margin-top:10px;">
            <h3>Audit Viewer</h3>
            <form action="${pageContext.request.contextPath}/AdminDashboard.do" method="GET" class="audit-form-grid">
                <div class="field">
                    <label>Admin User</label>
                    <select name="auditAdminID">
                        <option value="">All admins</option>
                        <c:forEach var="actor" items="${auditActors}">
                            <option value="${actor.adminID}" <c:if test="${auditAdminID != null && auditAdminID == actor.adminID}">selected</c:if>>${actor.firstname} ${actor.lastname} (#${actor.adminID})</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label>Action Type</label>
                    <input type="text" name="auditAction" value="${auditAction}" placeholder="CREATE_ADMIN, SQL_MUTATION...">
                </div>
                <div class="field">
                    <label>From Date</label>
                    <input type="date" name="auditFrom" value="${auditFrom}">
                </div>
                <div class="field">
                    <label>To Date</label>
                    <input type="date" name="auditTo" value="${auditTo}">
                </div>
                <div class="field">
                    <button class="btn" type="submit">Filter Audit Logs</button>
                </div>
            </form>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Log ID</th><th>Admin</th><th>Action</th><th>Target</th><th>Details</th><th>Time</th></tr></thead>
                    <tbody>
                        <c:forEach var="log" items="${auditLogs}">
                            <tr>
                                <td>${log.adminAuditLogID}</td>
                                <td>${log.firstname} ${log.lastname} (#${log.adminID})</td>
                                <td>${log.actionType}</td>
                                <td>${log.targetTable} / ${log.targetID}</td>
                                <td>${log.details}</td>
                                <td>${log.createdAt}</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty auditLogs}"><tr><td colspan="6">No audit logs found for selected filters.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </main>

    <div id="deleteEventModal" class="modal-backdrop" onclick="if (event.target === this) { closeDeleteEventModal(); }">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="deleteEventModalTitle">
            <h3 id="deleteEventModalTitle">Confirm Event Deletion</h3>
            <p>This action permanently removes the event. Continue only if you are sure this event ID is correct.</p>
            <div class="modal-actions">
                <button type="button" class="btn btn-alt" onclick="closeDeleteEventModal()">Cancel</button>
                <button type="button" class="btn" onclick="confirmDeleteEvent()">Yes, Delete Event</button>
            </div>
        </div>
    </div>

    <script>
        var pendingDeleteEventForm = null;

        function openDeleteEventModal(form) {
            pendingDeleteEventForm = form;
            var modal = document.getElementById("deleteEventModal");
            if (modal) {
                modal.style.display = "flex";
            }
        }

        function closeDeleteEventModal() {
            pendingDeleteEventForm = null;
            var modal = document.getElementById("deleteEventModal");
            if (modal) {
                modal.style.display = "none";
            }
        }

        function confirmDeleteEvent() {
            if (pendingDeleteEventForm) {
                pendingDeleteEventForm.submit();
            }
            closeDeleteEventModal();
        }

        function validateRootConfirm(el) {
            var form = el.form;
            if (!form) { return; }
            var next = form.querySelector('input[name="newRootPassword"]');
            if (!next) { return; }
            el.setCustomValidity(el.value === next.value ? '' : 'Passwords do not match');
        }

        function normalizeEventStatus(status) {
            var value = (status || '').toString().trim().toUpperCase();
            return value ? value : 'ACTIVE';
        }

        function looksPastDate(rawDate) {
            if (!rawDate) {
                return false;
            }
            var parsed = new Date(rawDate);
            if (isNaN(parsed.getTime())) {
                return false;
            }
            return parsed.getTime() < Date.now();
        }

        function applyEventQuickFilter() {
            var filter = document.getElementById('eventStatusQuickFilter');
            var summary = document.getElementById('eventQuickFilterSummary');
            if (!filter) {
                return;
            }

            var selected = normalizeEventStatus(filter.value);
            var params = new URLSearchParams(window.location.search || '');
            params.set('eventStatusQuickFilter', selected);
            window.history.replaceState({}, '', window.location.pathname + '?' + params.toString());

            var hidden = document.getElementById('eventStatusQuickFilterHidden');
            if (hidden) {
                hidden.value = selected;
            }

            var rows = document.querySelectorAll('tr.event-row');
            var visible = 0;

            rows.forEach(function (row) {
                var status = normalizeEventStatus(row.getAttribute('data-status'));
                var isPast = looksPastDate(row.getAttribute('data-date'));
                var retired = status === 'CANCELLED' || status === 'PASSED' || isPast;
                var show;

                if (selected === 'ALL') {
                    show = true;
                } else if (selected === 'CLEANUP') {
                    show = retired;
                } else {
                    show = status === selected;
                }

                row.style.display = show ? '' : 'none';
                if (show) {
                    visible++;
                }
            });

            if (summary) {
                if (selected === 'ALL') {
                    summary.textContent = 'Showing all events (' + visible + ')';
                } else if (selected === 'CLEANUP') {
                    summary.textContent = 'Showing cleanup candidates (' + visible + ')';
                } else {
                    summary.textContent = 'Showing ' + selected.toLowerCase() + ' events (' + visible + ')';
                }
            }
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
        window.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closeDeleteEventModal();
            }
        });
        (function initEventQuickFilterFromUrl() {
            var params = new URLSearchParams(window.location.search || '');
            var fromUrl = normalizeEventStatus(params.get('eventStatusQuickFilter') || 'ALL');
            var filter = document.getElementById('eventStatusQuickFilter');
            if (filter) {
                if (fromUrl === 'CLEANUP' || fromUrl === 'ALL' || fromUrl === 'ACTIVE' || fromUrl === 'CANCELLED' || fromUrl === 'PASSED') {
                    filter.value = fromUrl;
                } else {
                    filter.value = 'ALL';
                }
            }
        })();
        applyEventQuickFilter();
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
