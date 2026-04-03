<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Database Console</title>
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
        .flash { margin:10px 0; padding:10px 12px; border-radius:10px; font-weight:700; }
        .ok { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .err { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }
        .grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
        .card { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .card h3 { margin:0 0 8px; }
        .field { display:flex; flex-direction:column; gap:5px; margin-bottom:8px; }
        .field label { font-weight:700; font-size:.9rem; }
        .field input, .field select, .field textarea { border:1px solid #d8e0d2; border-radius:10px; padding:9px 10px; font:inherit; }
        .btn { border:none; border-radius:10px; padding:9px 12px; font-weight:800; background:var(--green); color:#fff; cursor:pointer; }
        .summary-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }
        .summary-item { background:#f9fcf7; border:1px solid #e4ece0; border-radius:10px; padding:10px; }
        .summary-item strong { color:#2f4a31; font-size:1.08rem; display:block; }
        .table-wrap { margin-top:10px; overflow:auto; border:1px solid #e5ece2; border-radius:10px; }
        table { width:100%; border-collapse:collapse; min-width:760px; }
        th, td { border-bottom:1px solid #edf3ea; padding:8px 10px; text-align:left; font-size:.9rem; }
        th { background:#f8fbf6; color:#5d6f61; }
        .muted { color:var(--muted); }
        @media(max-width:768px){ .field input, .field select, .field textarea, .btn, button { font-size:16px; } }
        @media(max-width:980px){ .grid { grid-template-columns:1fr; } .summary-grid{grid-template-columns:repeat(2,minmax(0,1fr));} }
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
                <a class="btn" href="${pageContext.request.contextPath}/LogoutServlet.do">Logout</a>
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
        <h2 style="margin:0 0 4px;">Database Console</h2>
        <p class="muted">Database writes require root password. Read operations are allowed for admin review.</p>

        <c:if test="${param.msg != null || msg != null}">
            <div class="flash ok">
                <c:choose>
                    <c:when test="${param.msg == 'CellUpdated'}">Cell value updated successfully.</c:when>
                    <c:when test="${param.msg == 'RowDeleted'}">Row deleted successfully.</c:when>
                    <c:when test="${param.msg == 'NoChange'}">No changes were applied.</c:when>
                    <c:when test="${msg == 'SQLExecuted'}">SQL executed successfully.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null || err != null}">
            <div class="flash err">
                <c:choose>
                    <c:when test="${param.err == 'RootAuthFailed'}">Root password is incorrect.</c:when>
                    <c:when test="${param.err == 'PrivilegedRequired'}">Only the main admin account can run database mutation actions.</c:when>
                    <c:when test="${param.err == 'MissingFields'}">Please complete all required fields.</c:when>
                    <c:when test="${param.err == 'UnknownAction'}">Unknown action requested.</c:when>
                    <c:when test="${param.err == 'OperationFailed'}">Operation failed. Check root password, SQL, and constraints.</c:when>
                    <c:otherwise>An error occurred. Check input and try again.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <c:if test="${!isPrivilegedAdmin}">
            <div class="flash err">Read-only mode: only the main admin account can execute SQL, update cells, or delete rows.</div>
        </c:if>

        <section class="card">
            <h3>Database Summary</h3>
            <div class="summary-grid">
                <c:forEach var="row" items="${tableSummary}">
                    <div class="summary-item"><strong>${row.count}</strong><span class="muted">${row.label}</span></div>
                </c:forEach>
            </div>
        </section>

        <c:if test="${isPrivilegedAdmin}">
        <section class="card" style="margin-top:12px;">
            <h3>User Export (CSV / PDF)</h3>
            <p class="muted" style="margin-top:0;">Export all or filtered user records. Main admin can export across all campuses; other admins export within their assigned campus scope.</p>
            <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="GET" style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:8px;align-items:end;">
                <input type="hidden" name="export" value="users">

                <div class="field">
                    <label for="format">Format</label>
                    <select id="format" name="format">
                        <option value="csv">CSV (Excel)</option>
                        <option value="pdf">PDF</option>
                    </select>
                </div>

                <div class="field">
                    <label for="roleFilter">Role Filter</label>
                    <select id="roleFilter" name="roleFilter">
                        <option value="ALL" <c:if test="${exportRole == 'ALL'}">selected</c:if>>All Roles</option>
                        <option value="ADMIN" <c:if test="${exportRole == 'ADMIN'}">selected</c:if>>Admin</option>
                        <option value="VENUE_GUARD" <c:if test="${exportRole == 'VENUE_GUARD'}">selected</c:if>>Venue Guard</option>
                        <option value="EVENT_MANAGER" <c:if test="${exportRole == 'EVENT_MANAGER'}">selected</c:if>>Event Manager</option>
                        <option value="TERTIARY_PRESENTER" <c:if test="${exportRole == 'TERTIARY_PRESENTER'}">selected</c:if>>Tertiary Presenter</option>
                        <option value="ATTENDEE" <c:if test="${exportRole == 'ATTENDEE'}">selected</c:if>>Attendee</option>
                    </select>
                </div>

                <div class="field">
                    <label for="campusFilter">Campus Filter</label>
                    <select id="campusFilter" name="campusFilter">
                        <option value="">All Campuses</option>
                        <c:forEach var="campusName" items="${exportCampusOptions}">
                            <option value="${campusName}" <c:if test="${campusName == exportCampus}">selected</c:if>>${campusName}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="field">
                    <label for="search">Search</label>
                    <input id="search" type="text" name="search" value="${exportSearch}" placeholder="name, username, email">
                </div>

                <div class="field">
                    <label for="scope">Scope</label>
                    <c:choose>
                        <c:when test="${isPrivilegedAdmin}">
                            <select id="scope" name="scope">
                                <option value="all" <c:if test="${exportScope == 'all'}">selected</c:if>>All Users (All Campuses)</option>
                                <option value="filtered" <c:if test="${exportScope != 'all'}">selected</c:if>>Filtered / Campus Scope</option>
                            </select>
                        </c:when>
                        <c:otherwise>
                            <input type="hidden" name="scope" value="filtered">
                            <input type="text" value="Campus Scope Only" disabled>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div style="grid-column:1 / -1;display:flex;gap:8px;flex-wrap:wrap;">
                    <button class="btn" type="submit">Download Export</button>
                    <span class="muted" style="align-self:center;">PDF export downloads directly as a .pdf file.</span>
                </div>
            </form>
        </section>
        </c:if>

        <c:if test="${isPrivilegedAdmin}">
        <section class="card" style="margin-top:12px;">
            <h3>Ticket Export (CSV / PDF)</h3>
            <p class="muted" style="margin-top:0;">Export all or filtered ticket records. Main admin can export across all campuses; other admins export within their assigned campus scope.</p>
            <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="GET" style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:8px;align-items:end;">
                <input type="hidden" name="export" value="tickets">

                <div class="field">
                    <label for="ticketFormat">Format</label>
                    <select id="ticketFormat" name="format">
                        <option value="csv">CSV (Excel)</option>
                        <option value="pdf">PDF</option>
                    </select>
                </div>

                <div class="field">
                    <label for="ticketCampusFilter">Campus Filter</label>
                    <select id="ticketCampusFilter" name="ticketCampusFilter">
                        <option value="">All Campuses</option>
                        <c:forEach var="campusName" items="${ticketCampusOptions}">
                            <option value="${campusName}" <c:if test="${campusName == ticketCampus}">selected</c:if>>${campusName}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="field">
                    <label for="ticketEventID">Event Filter</label>
                    <select id="ticketEventID" name="ticketEventID">
                        <option value="">All Events</option>
                        <c:forEach var="ev" items="${ticketEventOptions}">
                            <option value="${ev.eventID}" <c:if test="${ticketEventID == ev.eventID}">selected</c:if>>${ev.name} (#${ev.eventID})</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="field">
                    <label for="ticketSearch">Search</label>
                    <input id="ticketSearch" type="text" name="ticketSearch" value="${ticketSearch}" placeholder="ticket, event, attendee, QR">
                </div>

                <div class="field">
                    <label for="ticketScope">Scope</label>
                    <c:choose>
                        <c:when test="${isPrivilegedAdmin}">
                            <select id="ticketScope" name="ticketScope">
                                <option value="all" <c:if test="${ticketScope == 'all'}">selected</c:if>>All Tickets (All Campuses)</option>
                                <option value="filtered" <c:if test="${ticketScope != 'all'}">selected</c:if>>Filtered / Campus Scope</option>
                            </select>
                        </c:when>
                        <c:otherwise>
                            <input type="hidden" name="ticketScope" value="filtered">
                            <input type="text" value="Campus Scope Only" disabled>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div style="grid-column:1 / -1;display:flex;gap:8px;flex-wrap:wrap;">
                    <button class="btn" type="submit">Download Ticket Export</button>
                    <span class="muted" style="align-self:center;">PDF export downloads directly as a .pdf file.</span>
                </div>
            </form>
        </section>
        </c:if>

        <c:if test="${!isPrivilegedAdmin}">
            <div class="flash err">Privacy lock enabled: data exports (CSV/PDF) are restricted to the main admin account only.</div>
        </c:if>

        <c:if test="${isPrivilegedAdmin}">
            <section class="grid" style="margin-top:12px;">
                <article class="card">
                    <h3>SQL CRUD Console</h3>
                    <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="executeSql">
                        <div class="field"><label>SQL (SELECT/INSERT/UPDATE/DELETE only)</label><textarea name="sqlText" rows="6" required>${sqlText}</textarea></div>
                        <div class="field"><label>Root Password</label><input type="password" name="rootPassword" required></div>
                        <button class="btn" type="submit">Execute SQL</button>
                    </form>
                    <c:if test="${sqlUpdateCount != null}"><p class="muted" style="margin-top:8px;">Rows affected: ${sqlUpdateCount}</p></c:if>
                </article>

                <article class="card">
                    <h3>Cell Update and Safe Delete</h3>
                    <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="POST" style="margin-bottom:10px;">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="updateCell">
                        <div class="field"><label>Table</label><select name="tableName"><c:forEach var="tbl" items="${previewAllowedTables}"><option value="${tbl}">${tbl}</option></c:forEach></select></div>
                        <div class="field"><label>Row ID Column</label><input type="text" name="rowIdColumn" placeholder="adminID" required></div>
                        <div class="field"><label>Row ID</label><input type="number" name="rowId" min="1" required></div>
                        <div class="field"><label>Target Column</label><input type="text" name="targetColumn" required></div>
                        <div class="field"><label>New Value</label><input type="text" name="newValue" required></div>
                        <div class="field"><label>Root Password</label><input type="password" name="rootPassword" required></div>
                        <button class="btn" type="submit">Update Cell</button>
                    </form>

                    <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="safeDeleteRow">
                        <div class="field"><label>Safe Delete Table</label><select name="tableName"><c:forEach var="tbl" items="${safeDeleteTables}"><option value="${tbl}">${tbl}</option></c:forEach></select></div>
                        <div class="field"><label>Row ID</label><input type="number" name="rowId" min="1" required></div>
                        <div class="field"><label>Root Password</label><input type="password" name="rootPassword" required></div>
                        <button class="btn" type="submit">Delete Row</button>
                    </form>
                </article>
            </section>
        </c:if>

        <section class="card" style="margin-top:12px;">
            <h3>Table Preview</h3>
            <form action="${pageContext.request.contextPath}/AdminDatabase.do" method="GET" style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                <label class="muted" for="table">Inspect table</label>
                <select id="table" name="table"><c:forEach var="tbl" items="${previewAllowedTables}"><option value="${tbl}" <c:if test="${tbl == previewTable}">selected</c:if>>${tbl}</option></c:forEach></select>
                <button class="btn" type="submit">Load</button>
            </form>
            <div class="table-wrap">
                <table>
                    <thead><tr><c:forEach var="col" items="${previewColumns}"><th>${col}</th></c:forEach></tr></thead>
                    <tbody>
                        <c:forEach var="row" items="${previewRows}"><tr><c:forEach var="col" items="${previewColumns}"><td>${row[col]}</td></c:forEach></tr></c:forEach>
                        <c:if test="${empty previewRows}"><tr><td>No rows found.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>

        <c:if test="${not empty sqlRows}">
            <section class="card" style="margin-top:12px;">
                <h3>SQL Result Set</h3>
                <div class="table-wrap">
                    <table>
                        <thead><tr><c:forEach var="col" items="${sqlColumns}"><th>${col}</th></c:forEach></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${sqlRows}"><tr><c:forEach var="col" items="${sqlColumns}"><td>${row[col]}</td></c:forEach></tr></c:forEach>
                        </tbody>
                    </table>
                </div>
            </section>
        </c:if>
    </main>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
