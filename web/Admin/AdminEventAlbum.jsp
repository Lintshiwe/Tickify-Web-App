<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Event Album Upload</title>
    <style>
        :root { --green:#79c84a; --line:#d8e5d5; --ink:#2d3a31; --muted:#617266; }
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
        .card { margin-top:12px; background:#fff; border:1px solid var(--line); border-radius:12px; padding:14px; }
        .field { display:flex; flex-direction:column; gap:5px; margin-bottom:10px; }
        .field input, .field select { border:1px solid var(--line); border-radius:8px; padding:9px; font:inherit; }
        .dropzone {
            border:2px dashed #74b84f;
            border-radius:12px;
            background:#f3f9f1;
            padding:18px;
            text-align:center;
            color:#355143;
            cursor:pointer;
            transition:border-color .2s ease, background-color .2s ease, transform .2s ease;
        }
        .dropzone strong { display:block; font-size:1.02rem; margin-bottom:4px; }
        .dropzone small { color:var(--muted); }
        .dropzone.dragover {
            border-color:#3e8f22;
            background:#e8f4e5;
            transform:scale(1.01);
        }
        .file-meta {
            margin-top:8px;
            color:#496158;
            font-size:.9rem;
            min-height:18px;
        }
        .visually-hidden {
            position:absolute;
            width:1px;
            height:1px;
            padding:0;
            margin:-1px;
            overflow:hidden;
            clip:rect(0, 0, 0, 0);
            border:0;
            white-space:nowrap;
        }
        .save { border:none; background:var(--green); color:#fff; border-radius:10px; padding:10px 12px; font-weight:800; cursor:pointer; }
        .status { margin-top:10px; padding:10px; border-radius:10px; font-weight:700; }
        .ok { background:#eaf7e7; color:#1f7c39; border:1px solid #cce6c7; }
        .err { background:#ffecec; color:#9b1c1c; border:1px solid #f0c2c2; }
        .simple-grid { display:grid; gap:12px; grid-template-columns:repeat(2,minmax(0,1fr)); }
        .mini-card { border:1px solid #e3eee0; border-radius:10px; padding:12px; background:#fcfefb; }
        .mini-card h3 { margin:0 0 8px; font-size:1.05rem; }
        .grid { margin-top:12px; display:grid; grid-template-columns:repeat(auto-fill,minmax(220px,1fr)); gap:10px; }
        .event-card { border:1px solid #e3eee0; border-radius:10px; overflow:hidden; background:#fff; }
        .event-card img { width:100%; height:140px; object-fit:cover; display:block; background:#edf4ea; }
        .event-meta { padding:9px; }
        .event-meta strong { display:block; margin-bottom:4px; }
        .event-meta span { color:var(--muted); font-size:.88rem; }
        @media (max-width:900px) { .simple-grid { grid-template-columns:1fr; } }
        @media (max-width:768px) { .field input, .field select, .save, button { font-size:16px; } }
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
        <h1 style="margin:0 2px 0;">Event Album Upload</h1>

        <% String msg = request.getParameter("msg"); %>
        <% String err = request.getParameter("err"); %>
        <% if ("Uploaded".equals(msg)) { %>
            <div class="status ok">Event album image uploaded successfully.</div>
        <% } %>
        <% if ("CoverRemoved".equals(msg)) { %>
            <div class="status ok">Event album image removed successfully.</div>
        <% } %>
        <% if ("EventCreated".equals(msg)) { %>
            <div class="status ok">Event created successfully.</div>
        <% } %>
        <% if ("EventUpdated".equals(msg)) { %>
            <div class="status ok">Event updated successfully.</div>
        <% } %>
        <% if (err != null) { %>
            <div class="status err">
                <%= "InvalidEvent".equals(err)
                        ? "Selected event is invalid."
                        : ("MissingFields".equals(err)
                            ? "Please complete all required event fields."
                            : ("InvalidDate".equals(err)
                                ? "Please choose a valid date and time."
                        : ("MissingImage".equals(err)
                            ? "Please upload an image file."
                            : ("InvalidImage".equals(err)
                                ? "Uploaded file must be a valid image format."
                                : ("CampusScopeDenied".equals(err)
                                    ? "You can only manage event covers for your campus scope."
                                    : ("InvalidAssignment".equals(err)
                                        ? "Please select a valid venue for this event."
                                : ("ImageRead".equals(err)
                                    ? "Could not read uploaded image. Please try again."
                                    : "Upload failed. Please try again."))))))) %>
            </div>
        <% } %>

        <section class="card">
            <h2 style="margin-top:0;">Simple Event Setup (Create + Update)</h2>
            <p style="margin:0 0 10px;color:var(--muted);">Use Date/Time to schedule events quickly on this page.</p>
            <div class="simple-grid">
                <div class="mini-card">
                    <h3>Create Event</h3>
                    <form action="${pageContext.request.contextPath}/AdminEventAlbum.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="createEvent">
                        <div class="field"><label>Event Name</label><input type="text" name="eventName" required></div>
                        <div class="field"><label>Event Type</label><input type="text" name="eventType" required></div>
                        <div class="field"><label>Date + Time</label><input type="datetime-local" name="eventDate" required></div>
                        <div class="field"><label>Venue</label><select name="venueID" required><option value="">Select Venue</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                        <div class="field"><label>Status</label><select name="eventStatus"><option value="ACTIVE">ACTIVE</option><option value="CANCELLED">CANCELLED</option><option value="PASSED">PASSED</option></select></div>
                        <div class="field"><label>Description (optional)</label><input type="text" name="eventDescription" maxlength="1200"></div>
                        <button type="submit" class="save">Create Event</button>
                    </form>
                </div>
                <div class="mini-card">
                    <h3>Update Event</h3>
                    <form action="${pageContext.request.contextPath}/AdminEventAlbum.do" method="POST">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="updateEvent">
                        <div class="field"><label>Event</label><select name="eventID" required><option value="">Select Event</option><c:forEach var="ev" items="${events}"><option value="${ev.eventID}">${ev.name} (#${ev.eventID})</option></c:forEach></select></div>
                        <div class="field"><label>Event Name</label><input type="text" name="eventName" required></div>
                        <div class="field"><label>Event Type</label><input type="text" name="eventType" required></div>
                        <div class="field"><label>Date + Time</label><input type="datetime-local" name="eventDate" required></div>
                        <div class="field"><label>Venue</label><select name="venueID" required><option value="">Select Venue</option><c:forEach var="v" items="${venues}"><option value="${v.venueID}">${v.name} (#${v.venueID})</option></c:forEach></select></div>
                        <div class="field"><label>Status</label><select name="eventStatus"><option value="ACTIVE">ACTIVE</option><option value="CANCELLED">CANCELLED</option><option value="PASSED">PASSED</option></select></div>
                        <div class="field"><label>Description (optional)</label><input type="text" name="eventDescription" maxlength="1200"></div>
                        <button type="submit" class="save">Update Event</button>
                    </form>
                </div>
            </div>
        </section>

        <section class="card">
            <h2 style="margin-top:0;">Upload Event Album Image (File Upload)</h2>
            <form action="${pageContext.request.contextPath}/AdminEventAlbum.do" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <div class="field">
                    <label>Event</label>
                    <select name="eventID" required>
                        <option value="">Select Event</option>
                        <c:forEach var="ev" items="${events}">
                            <option value="${ev.eventID}">${ev.name} | ${ev.type} | ${ev.campusName}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label>Image File</label>
                    <div id="dropzone" class="dropzone" tabindex="0" role="button" aria-label="Drop image or click to select">
                        <strong>Drop image here</strong>
                        <small>or click to browse (any image format)</small>
                        <div id="fileMeta" class="file-meta">No file selected</div>
                    </div>
                    <input id="eventAlbumImage" class="visually-hidden" type="file" name="eventAlbumImage" accept="image/*" required>
                    <small style="color:var(--muted);">All image formats are allowed. Raster images are auto-cropped/resized for ticket dimensions.</small>
                </div>
                <button type="submit" class="save">Upload Album Image</button>
            </form>
        </section>

        <section class="card">
            <h2 style="margin-top:0;">Current Event Album Images</h2>
            <div class="grid">
                <c:forEach var="ev" items="${events}">
                    <article class="event-card">
                        <img src="EventAlbumImage.do?eventID=${ev.eventID}" alt="${ev.name}" onerror="this.style.display='none'; this.parentElement.style.background='#eef5ea';">
                        <div class="event-meta">
                            <strong>${ev.name}</strong>
                            <span>${ev.type} | ${ev.campusName}</span>
                            <form action="${pageContext.request.contextPath}/AdminEventAlbum.do" method="POST" enctype="multipart/form-data" style="margin-top:8px;display:grid;gap:6px;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="upload">
                                <input type="hidden" name="eventID" value="${ev.eventID}">
                                <input type="file" name="eventAlbumImage" accept="image/*" required>
                                <button type="submit" class="save" style="padding:7px 10px;">Replace Cover</button>
                            </form>
                            <form action="${pageContext.request.contextPath}/AdminEventAlbum.do" method="POST" style="margin-top:6px;">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="clear">
                                <input type="hidden" name="eventID" value="${ev.eventID}">
                                <button type="submit" class="btn" style="width:100%;">Remove Cover</button>
                            </form>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </section>
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

        (function () {
            var dropzone = document.getElementById('dropzone');
            var fileInput = document.getElementById('eventAlbumImage');
            var fileMeta = document.getElementById('fileMeta');

            function bytesToHuman(bytes) {
                if (!bytes && bytes !== 0) return '';
                var units = ['B', 'KB', 'MB', 'GB'];
                var i = 0;
                var value = bytes;
                while (value >= 1024 && i < units.length - 1) {
                    value /= 1024;
                    i++;
                }
                return value.toFixed(i === 0 ? 0 : 1) + ' ' + units[i];
            }

            function showFile(file) {
                fileMeta.textContent = file ? (file.name + ' (' + bytesToHuman(file.size) + ')') : 'No file selected';
            }

            function preventDefaults(e) {
                e.preventDefault();
                e.stopPropagation();
            }

            dropzone.addEventListener('click', function () {
                fileInput.click();
            });

            dropzone.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    fileInput.click();
                }
            });

            fileInput.addEventListener('change', function () {
                showFile(fileInput.files && fileInput.files[0]);
            });

            ['dragenter', 'dragover'].forEach(function (eventName) {
                dropzone.addEventListener(eventName, function (e) {
                    preventDefaults(e);
                    dropzone.classList.add('dragover');
                });
            });

            ['dragleave', 'drop'].forEach(function (eventName) {
                dropzone.addEventListener(eventName, function (e) {
                    preventDefaults(e);
                    dropzone.classList.remove('dragover');
                });
            });

            dropzone.addEventListener('drop', function (e) {
                var files = e.dataTransfer && e.dataTransfer.files;
                if (!files || !files.length) return;
                fileInput.files = files;
                showFile(files[0]);
            });
        })();
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
