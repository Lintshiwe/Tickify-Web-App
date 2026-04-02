<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Advert Manager</title>
    <style>
        :root { --green:#79c84a; --line:#d8e5d5; --ink:#2d3a31; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:#f7faf6; color:var(--ink); }
        .site-header {
            width:100%;
            background:#f7f8f6;
            border-bottom:1px solid #dfe5dc;
            position:sticky;
            top:0;
            z-index:30;
        }
        .header-inner { width:100%; max-width:none; margin:0; padding:14px clamp(12px,2.7vw,36px); }
        .header-top { display:flex; justify-content:space-between; align-items:center; gap:14px; flex-wrap:wrap; }
        .brand { display:flex; align-items:center; gap:10px; text-decoration:none; }
        .brand-logo { height:58px; width:auto; display:block; }
        .brand-text { font-weight:900; color:#47596b; letter-spacing:.08em; }
        .profile-wrap { position:relative; }
        .profile-btn { display:flex; align-items:center; gap:10px; border:1px solid #d7ded3; background:#fff; border-radius:999px; padding:8px 10px; color:#2a312b; font-weight:700; cursor:pointer; }
        .profile-meta { max-width:0; opacity:0; overflow:hidden; white-space:nowrap; transition:max-width .28s ease, opacity .22s ease; }
        .profile-wrap:hover .profile-meta, .profile-wrap:focus-within .profile-meta { max-width:260px; opacity:1; }
        .profile-icon { width:28px; height:28px; border-radius:50%; background:#e6eedf; display:flex; align-items:center; justify-content:center; color:#4c5b4b; font-weight:800; }
        .profile-menu { position:absolute; right:0; top:calc(100% + 10px); min-width:220px; background:#fff; border:1px solid #dee5da; border-radius:12px; box-shadow:0 14px 26px rgba(24,32,20,.12); padding:8px; display:none; }
        .profile-menu.open { display:block; }
        .profile-menu a { display:block; text-decoration:none; color:#2c342d; border-radius:8px; padding:10px; font-weight:700; }
        .profile-menu a:hover { background:#f3f7f1; }
        .profile-menu .danger { color:#9b1c1c; background:#fff5f5; }
        .header-nav { margin-top:12px; padding-top:12px; border-top:1px solid #e4e9e1; display:flex; gap:14px; flex-wrap:wrap; justify-content:center; }
        .header-nav a { text-decoration:none; color:#2d352e; font-weight:800; font-size:.95rem; border:1px solid #d8e0d2; background:#fff; border-radius:999px; padding:8px 14px; }
        .wrap { width:100%; max-width:none; margin:0; padding:22px clamp(12px,2.7vw,36px) 40px; }
        .btn { text-decoration:none; border:1px solid var(--line); background:#fff; color:#314236; padding:10px 12px; border-radius:10px; font-weight:800; }
        .logout { text-decoration:none; background:#eef8e9; color:#4b8f2b; border:1px solid #cfe2c9; border-radius:10px; padding:10px 12px; font-weight:800; }
        .grid { margin-top:12px; display:grid; grid-template-columns:1fr 1fr; gap:12px; }
        .card { background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .field { display:flex; flex-direction:column; gap:5px; margin-bottom:10px; }
        .field input, .field textarea { border:1px solid var(--line); border-radius:8px; padding:9px; font:inherit; }
        .row { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
        .save { border:none; background:var(--green); color:#fff; border-radius:10px; padding:10px 12px; font-weight:800; cursor:pointer; }
        .ad { border:1px solid #e3eee0; border-radius:10px; padding:10px; margin-bottom:8px; }
        .muted { color:#617266; font-size:.9rem; }
        @media (max-width:768px) { .field input, .field textarea, .btn, .save, button, select { font-size:16px; } }
        @media (max-width:980px) { .grid { grid-template-columns:1fr; } }
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
                        <span class="profile-meta">${userFullName} | ${sessionScope.userRoleNumberLabel} | ${sessionScope.userCampusName}</span>
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
                <a href="${pageContext.request.contextPath}/Admin/AdminManageAdmins.jsp">Admins</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageGuards.jsp">Guards</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManageManagers.jsp">Managers</a>
                <a href="${pageContext.request.contextPath}/Admin/AdminManagePresenters.jsp">Presenters</a>
                <a href="${pageContext.request.contextPath}/AdminDatabase.do">Database</a>
            </nav>
        </div>
    </header>

    <div class="wrap">
        <h1 style="margin:0 2px 0;">Advert Manager</h1>

        <c:if test="${param.msg != null}">
            <div class="card" style="margin-top:10px; background:#eaf7e7; border-color:#cce6c7; color:#1f7c39; font-weight:700;">
                <c:choose>
                    <c:when test="${param.msg == 'Created'}">Advert created successfully.</c:when>
                    <c:when test="${param.msg == 'Updated'}">Advert updated successfully.</c:when>
                    <c:when test="${param.msg == 'Edited'}">Advert edited successfully.</c:when>
                    <c:when test="${param.msg == 'Deleted'}">Advert deleted successfully.</c:when>
                    <c:when test="${param.msg == 'NoChange'}">No changes were applied.</c:when>
                    <c:otherwise>Operation completed successfully.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${param.err != null}">
            <div class="card" style="margin-top:10px; background:#ffecec; border-color:#f0c2c2; color:#9b1c1c; font-weight:700;">
                <c:choose>
                    <c:when test="${param.err == 'MissingFields'}">Please complete all required fields.</c:when>
                    <c:when test="${param.err == 'BadDate'}">Please provide a valid event date.</c:when>
                    <c:when test="${param.err == 'MissingImage'}">Please upload an advert image.</c:when>
                    <c:when test="${param.err == 'InvalidImage'}">Uploaded file must be a valid image.</c:when>
                    <c:when test="${param.err == 'ImageRead'}">Could not read uploaded image. Please try again.</c:when>
                    <c:when test="${param.err == 'CreateFailed'}">Failed to create advert. Please try again.</c:when>
                    <c:when test="${param.err == 'UpdateFailed'}">Failed to update advert. Please try again.</c:when>
                    <c:when test="${param.err == 'DeleteFailed'}">Failed to delete advert. Please try again.</c:when>
                    <c:when test="${param.err == 'InvalidAdvert'}">Selected advert is invalid.</c:when>
                    <c:otherwise>An error occurred. Please verify input and try again.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="grid">
            <section class="card">
                <h2 style="margin-top:0;">Create Advert</h2>
                <form action="${pageContext.request.contextPath}/AdminAdverts.do" method="POST" enctype="multipart/form-data">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <div class="field"><label>Organization</label><input type="text" name="organizationName" required></div>
                    <div class="field"><label>Title</label><input type="text" name="title" required></div>
                    <div class="field"><label>Details</label><textarea name="details" rows="3"></textarea></div>
                    <div class="field"><label>Venue</label><input type="text" name="venue" required></div>
                    <div class="field"><label>Event Date</label><input type="date" name="eventDate" required></div>
                    <div class="field"><label>Advert Image (all image extensions)</label><input type="file" name="advertImage" accept="image/*" required></div>
                    <div class="row">
                        <label><input type="checkbox" name="paidOrganization" checked> Paid Organization</label>
                        <label><input type="checkbox" name="selectedForDisplay" checked> Selected for Display</label>
                        <label><input type="checkbox" name="active" checked> Active</label>
                    </div>
                    <div style="margin-top:10px;"><button class="save" type="submit">Save Advert</button></div>
                </form>
            </section>

            <section class="card">
                <h2 style="margin-top:0;">Existing Adverts</h2>
                <c:forEach var="ad" items="${adverts}">
                    <div class="ad">
                        <strong>${ad.title}</strong>
                        <div class="muted">${ad.organizationName} | ${ad.venue} | ${ad.eventDate}</div>
                        <div class="row" style="margin-top:8px; align-items:flex-start;">
                            <img src="AdvertImage.do?id=${ad.advertID}" alt="${ad.title}" style="width:120px;height:80px;object-fit:cover;border-radius:8px;border:1px solid #e3eee0;">
                            <form action="${pageContext.request.contextPath}/AdminAdverts.do" method="POST" enctype="multipart/form-data" style="margin:0; flex:1 1 auto;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="edit">
                                <input type="hidden" name="advertID" value="${ad.advertID}">
                                <div class="field"><label>Organization</label><input type="text" name="organizationName" value="${ad.organizationName}" required></div>
                                <div class="field"><label>Title</label><input type="text" name="title" value="${ad.title}" required></div>
                                <div class="field"><label>Details</label><textarea name="details" rows="2">${ad.details}</textarea></div>
                                <div class="field"><label>Venue</label><input type="text" name="venue" value="${ad.venue}" required></div>
                                <div class="field"><label>Event Date</label><input type="date" name="eventDate" value="${ad.eventDate}" required></div>
                                <div class="field"><label>Replace Image (optional)</label><input type="file" name="advertImage" accept="image/*"></div>
                                <label><input type="checkbox" name="paidOrganization" <c:if test="${ad.paidOrganization}">checked</c:if>> Paid</label>
                                <label><input type="checkbox" name="selectedForDisplay" <c:if test="${ad.selectedForDisplay}">checked</c:if>> Display</label>
                                <label><input type="checkbox" name="active" <c:if test="${ad.active}">checked</c:if>> Active</label>
                                <div class="row" style="margin-top:8px;">
                                    <button class="btn" type="submit">Save Edit</button>
                                </div>
                            </form>
                            <form action="${pageContext.request.contextPath}/AdminAdverts.do" method="POST" style="margin:0;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="advertID" value="${ad.advertID}">
                                <button class="btn" type="submit" onclick="return confirm('Delete this advert? This cannot be undone.');">Delete</button>
                            </form>
                        </div>
                    </div>
                </c:forEach>
            </section>
        </div>
    </div>
    <script>
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
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
