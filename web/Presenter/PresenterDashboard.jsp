<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tickify | Presenter Dashboard</title>
    <style>
        :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:var(--bg); color:var(--ink); }
        .wrap { width:100%; max-width:none; margin:0; padding:20px clamp(12px,2.7vw,36px); }
        .top { background:#fff; border:1px solid var(--line); border-radius:14px; padding:14px 16px; display:flex; justify-content:space-between; align-items:center; gap:10px; }
        .brand { color:var(--green-dark); font-weight:900; letter-spacing:.1em; }
        .profile-meta { color:#607167; font-weight:700; font-size:.92rem; }
        .logout { text-decoration:none; background:#eef8e9; color:var(--green-dark); border:1px solid #cfe2c9; border-radius:10px; padding:10px 12px; font-weight:800; }
        .card { margin-top:12px; background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .flash { margin-top:10px; border-radius:10px; padding:10px 12px; font-weight:800; }
        .ok { background:#eaf7e7; color:#1f7c39; border:1px solid #cce6c7; }
        .err { background:#ffecec; color:#9b1c1c; border:1px solid #f0c2c2; }
        .kpis { margin-top:12px; display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; }
        .kpi { background:#fff; border:1px solid var(--line); border-radius:12px; padding:12px; }
        .kpi strong { display:block; font-size:1.28rem; color:#31502f; }
        .kpi span { color:var(--muted); font-size:.9rem; }
        .grid { margin-top:12px; display:grid; grid-template-columns:1fr 1fr; gap:10px; }
        .full { grid-column:1/-1; }
        .meta-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:8px; }
        .meta-item { background:#f9fcf8; border:1px solid #e2ece0; border-radius:10px; padding:10px; }
        .meta-item .label { color:#708173; font-size:.8rem; font-weight:700; text-transform:uppercase; letter-spacing:.02em; }
        .meta-item .value { color:#324437; font-weight:700; margin-top:4px; }
        .status-badge { display:inline-flex; align-items:center; gap:6px; border-radius:999px; padding:6px 10px; font-weight:800; font-size:.82rem; }
        .status-good { background:#edf7e8; color:#2f7f1f; border:1px solid #cfe2c9; }
        .status-warn { background:#fff4df; color:#9a5a00; border:1px solid #f5d9a4; }
        .status-risk { background:#fee4e2; color:#b42318; border:1px solid #fecdca; }
        .table-wrap { margin-top:8px; overflow:auto; border:1px solid #e2ece0; border-radius:10px; }
        table { width:100%; border-collapse:collapse; min-width:620px; }
        th, td { border-bottom:1px solid #edf3ea; padding:8px 10px; text-align:left; font-size:.9rem; }
        th { background:#f7fbf5; color:#5d6f61; }
        .empty { color:#748578; font-weight:700; }
        h1,h2{margin:0 0 8px;} p,li{color:var(--muted);} ul{margin:0;padding-left:18px;}
        @media(max-width:980px){ .kpis{grid-template-columns:repeat(2,minmax(0,1fr));} }
        @media(max-width:780px){ .top{flex-direction:column; align-items:flex-start; gap:10px;} .grid{grid-template-columns:1fr;} .meta-grid{grid-template-columns:1fr;} .kpis{grid-template-columns:1fr;} }
    </style>
</head>
<body>
    <c:set var="totalTickets" value="${eventSnapshot.totalTickets}" />
    <c:set var="soldTickets" value="${eventSnapshot.soldTickets}" />
    <c:set var="availableTickets" value="${eventSnapshot.availableTickets}" />
    <c:set var="soldPercentage" value="${eventSnapshot.soldPercentage}" />
    <c:set var="revenue" value="${eventSnapshot.revenue}" />
    <c:set var="wishlistCount" value="${eventSnapshot.wishlistCount}" />

    <div class="wrap">
        <div class="top">
            <div>
                <div class="brand">TICKIFY PRESENTER</div>
                <div class="profile-meta">${userFullName} | ${sessionScope.userRoleNumberLabel} | ${sessionScope.userCampusName}</div>
            </div>
            <a class="logout" href="${pageContext.request.contextPath}/LogoutServlet.do">Logout</a>
        </div>

        <c:if test="${param.msg != null}">
            <div class="flash ok">
                <c:choose>
                    <c:when test="${param.msg == 'MaterialAdded'}">Material uploaded successfully.</c:when>
                    <c:when test="${param.msg == 'ScheduleAdded'}">Schedule item added successfully.</c:when>
                    <c:when test="${param.msg == 'AnnouncementAdded'}">Announcement published successfully.</c:when>
                    <c:when test="${param.msg == 'NoChange'}">No changes were applied.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null}">
            <div class="flash err">
                <c:choose>
                    <c:when test="${param.err == 'UnknownAction'}">Unknown presenter action requested.</c:when>
                    <c:when test="${param.err == 'InvalidMaterialTitle'}">Material title must be between 3 and 120 characters.</c:when>
                    <c:when test="${param.err == 'InvalidMaterialUrl'}">Material URL must start with http:// or https://.</c:when>
                    <c:when test="${param.err == 'InvalidMaterialDescription'}">Material description is too long.</c:when>
                    <c:when test="${param.err == 'InvalidScheduleTitle'}">Schedule title must be between 3 and 120 characters.</c:when>
                    <c:when test="${param.err == 'InvalidScheduleDate'}">Please provide a valid schedule date and time.</c:when>
                    <c:when test="${param.err == 'InvalidScheduleRange'}">Schedule end time must be after start time.</c:when>
                    <c:when test="${param.err == 'InvalidScheduleRoom'}">Room field is too long.</c:when>
                    <c:when test="${param.err == 'InvalidScheduleNotes'}">Schedule notes are too long.</c:when>
                    <c:when test="${param.err == 'InvalidAnnouncementTitle'}">Announcement title must be between 3 and 120 characters.</c:when>
                    <c:when test="${param.err == 'InvalidAnnouncementBody'}">Announcement body must be between 8 and 1000 characters.</c:when>
                    <c:when test="${param.err == 'OperationFailed'}">Unable to process your request.</c:when>
                    <c:otherwise>Request failed.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <section class="card">
            <h1>Presenter Workspace</h1>
            <p>Live dashboard connected to your presenter, event, venue, team and ticket data from the database.</p>
            <div class="meta-grid">
                <div class="meta-item"><div class="label">Presenter</div><div class="value">${presenterProfile.firstname} ${presenterProfile.lastname}</div></div>
                <div class="meta-item"><div class="label">Institution</div><div class="value">${presenterProfile.tertiaryInstitution}</div></div>
                <div class="meta-item"><div class="label">Email</div><div class="value">${presenterProfile.email}</div></div>
                <div class="meta-item"><div class="label">Phone</div><div class="value">${presenterProfile.phoneNumber}</div></div>
            </div>
        </section>

        <section class="kpis">
            <article class="kpi"><strong>${totalTickets}</strong><span>Total Tickets</span></article>
            <article class="kpi"><strong>${soldTickets}</strong><span>Sold Tickets</span></article>
            <article class="kpi"><strong>${availableTickets}</strong><span>Available Tickets</span></article>
            <article class="kpi"><strong>R <fmt:formatNumber value="${revenue}" minFractionDigits="2" maxFractionDigits="2"/></strong><span>Recorded Revenue</span></article>
        </section>

        <div class="grid">
            <section class="card">
                <h2>Assigned Event</h2>
                <div class="meta-grid">
                    <div class="meta-item"><div class="label">Event Name</div><div class="value">${presenterProfile.eventName}</div></div>
                    <div class="meta-item"><div class="label">Type</div><div class="value">${presenterProfile.eventType}</div></div>
                    <div class="meta-item"><div class="label">Event Date</div><div class="value">${presenterProfile.eventDate}</div></div>
                    <div class="meta-item"><div class="label">Interest (Wishlist)</div><div class="value">${wishlistCount}</div></div>
                </div>
            </section>

            <section class="card">
                <h2>Venue Details</h2>
                <div class="meta-grid">
                    <div class="meta-item"><div class="label">Venue</div><div class="value">${presenterProfile.venueName}</div></div>
                    <div class="meta-item"><div class="label">Address</div><div class="value">${presenterProfile.venueAddress}</div></div>
                    <div class="meta-item"><div class="label">Sell-Through</div><div class="value">${soldPercentage}%</div></div>
                    <div class="meta-item"><div class="label">Status</div>
                        <div class="value">
                            <c:choose>
                                <c:when test="${eventSnapshot.soldOut}"><span class="status-badge status-risk">Sold Out</span></c:when>
                                <c:when test="${eventSnapshot.nearlySoldOut}"><span class="status-badge status-warn">Nearly Sold Out</span></c:when>
                                <c:otherwise><span class="status-badge status-good">Healthy Stock</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </section>

            <section class="card full">
                <h2>Event Managers</h2>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Name</th><th>Email</th><th>Event</th></tr></thead>
                        <tbody>
                            <c:forEach var="m" items="${managerContacts}">
                                <tr>
                                    <td>${m.firstname} ${m.lastname}</td>
                                    <td>${m.email}</td>
                                    <td>${m.eventName}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty managerContacts}"><tr><td colspan="3" class="empty">No manager contacts mapped for this event yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Venue Guards</h2>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Name</th><th>Email</th></tr></thead>
                        <tbody>
                            <c:forEach var="g" items="${guardContacts}">
                                <tr>
                                    <td>${g.firstname} ${g.lastname}</td>
                                    <td>${g.email}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty guardContacts}"><tr><td colspan="2" class="empty">No venue guard contacts found for this venue.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Peer Presenters At This Venue</h2>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Presenter</th><th>Email</th><th>Institution</th><th>Assigned Event</th></tr></thead>
                        <tbody>
                            <c:forEach var="p" items="${peerPresenters}">
                                <tr>
                                    <td>${p.firstname} ${p.lastname}</td>
                                    <td>${p.email}</td>
                                    <td>${p.tertiaryInstitution}</td>
                                    <td>${p.eventName}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty peerPresenters}"><tr><td colspan="4" class="empty">No peer presenters at this venue yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Session Materials</h2>
                <form action="${pageContext.request.contextPath}/TertiaryPresenterDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="addMaterial">
                    <input type="text" name="title" placeholder="Material title" required>
                    <input type="url" name="materialUrl" placeholder="Material URL (optional)">
                    <input type="text" name="description" placeholder="Description">
                    <button type="submit">Add Material</button>
                </form>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Title</th><th>URL</th><th>Description</th><th>Created</th></tr></thead>
                        <tbody>
                            <c:forEach var="m" items="${materials}">
                                <tr>
                                    <td>${m.title}</td>
                                    <td>${m.materialUrl}</td>
                                    <td>${m.description}</td>
                                    <td>${m.createdAt}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty materials}"><tr><td colspan="4" class="empty">No materials uploaded yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Session Schedule</h2>
                <form action="${pageContext.request.contextPath}/TertiaryPresenterDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="addScheduleItem">
                    <input type="text" name="title" placeholder="Session title" required>
                    <input type="datetime-local" name="startsAt" required>
                    <input type="datetime-local" name="endsAt">
                    <input type="text" name="room" placeholder="Room/Stage">
                    <input type="text" name="notes" placeholder="Notes">
                    <button type="submit">Add Schedule Item</button>
                </form>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Title</th><th>Starts</th><th>Ends</th><th>Room</th><th>Notes</th></tr></thead>
                        <tbody>
                            <c:forEach var="s" items="${scheduleItems}">
                                <tr>
                                    <td>${s.title}</td>
                                    <td>${s.startsAt}</td>
                                    <td>${s.endsAt}</td>
                                    <td>${s.room}</td>
                                    <td>${s.notes}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty scheduleItems}"><tr><td colspan="5" class="empty">No schedule items added yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Announcements</h2>
                <form action="${pageContext.request.contextPath}/TertiaryPresenterDashboard.do" method="POST" style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;align-items:end;">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="addAnnouncement">
                    <input type="text" name="title" placeholder="Announcement title" required>
                    <input type="text" name="body" placeholder="Announcement body" required>
                    <button type="submit">Publish Announcement</button>
                </form>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Title</th><th>Body</th><th>Created</th></tr></thead>
                        <tbody>
                            <c:forEach var="a" items="${announcements}">
                                <tr>
                                    <td>${a.title}</td>
                                    <td>${a.body}</td>
                                    <td>${a.createdAt}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty announcements}"><tr><td colspan="3" class="empty">No announcements yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="card full">
                <h2>Attendees For My Session Event</h2>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th></tr></thead>
                        <tbody>
                            <c:forEach var="u" items="${attendeeList}">
                                <tr>
                                    <td>${u.attendeeID}</td>
                                    <td>${u.username}</td>
                                    <td>${u.firstname} ${u.lastname}</td>
                                    <td>${u.email}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty attendeeList}"><tr><td colspan="4" class="empty">No attendees found for your event yet.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
