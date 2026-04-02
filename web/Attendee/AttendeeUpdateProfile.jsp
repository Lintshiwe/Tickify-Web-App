<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Update Profile</title>
    <style>
        :root { --green:#79c84a; --green-dark:#5ca833; --bg:#f7faf6; --ink:#3a4a3e; --muted:#76857a; --line:#d8e5d5; }
        * { box-sizing:border-box; }
        body { margin:0; font-family:"Trebuchet MS","Segoe UI",sans-serif; background:var(--bg); color:var(--ink); }
        .main-content { width:100%; min-height:100vh; }
        header { background:#fff; border-bottom:1px solid var(--line); padding:15px 24px; display:flex; justify-content:space-between; align-items:center; }
        .logo { font-size:1.3rem; font-weight:900; color:var(--green-dark); text-decoration:none; }
        .logout { color:var(--green-dark); text-decoration:none; border:1px solid #cfe2c9; padding:6px 10px; border-radius:8px; background:#eef8e9; font-size:.85rem; }
        .dashboard-body { width:100%; padding:26px clamp(12px,2.7vw,36px); display:flex; flex-direction:column; align-items:stretch; }
        .section-title { text-align:center; margin-bottom:18px; }
        .profile-form { background:#fff; border:1px solid var(--line); border-radius:14px; box-shadow:0 10px 22px rgba(90,130,90,.08); width:100%; max-width:none; padding:22px; }
        .form-group { margin-bottom:12px; }
        .form-group label { display:block; font-weight:700; margin-bottom:6px; }
        .form-group input, .form-group select, .form-group textarea { width:100%; padding:12px; border:1px solid #cfe2c9; border-radius:10px; font:inherit; }
        .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline:none; border-color:var(--green); box-shadow:0 0 0 3px rgba(121,200,74,.2); }
        .btn-save { width:100%; border:none; border-radius:12px; background:var(--green); color:#fff; font-weight:800; padding:12px; margin-top:6px; cursor:pointer; }
        .back-link { margin-top:12px; color:var(--muted); text-decoration:none; }
        .alert { padding:10px; margin-bottom:10px; border-radius:10px; width:100%; max-width:none; text-align:center; font-weight:700; }
        .alert-success { background:#eaf7e7; color:#1f7c39; border:1px solid #cce6c7; }
        .alert-error { background:#ffecec; color:#9b1c1c; border:1px solid #f0c2c2; }
        @media(max-width:768px){ .form-group input, .form-group select, .form-group textarea, .btn-save { font-size:16px; } }
    </style>
</head>
<body>
    <div class="main-content">
        <header>
            <a href="AttendeeDashboardServlet.do" class="logo">TICKIFY</a>
            <div style="display:flex; gap:12px; align-items:center;"><span>Welcome, <strong>${userFullName}</strong></span><a href="LogoutServlet.do" class="logout">LOGOUT</a></div>
        </header>
        <div class="dashboard-body">
            <div class="section-title"><h2>Update Your Profile</h2></div>
            <c:if test="${not empty message}"><div class="alert alert-success">${message}</div></c:if>
            <c:if test="${not empty error}"><div class="alert alert-error">${error}</div></c:if>
            <div class="profile-form">
                <form action="AttendeeViewProfileServlet.do" method="POST">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <div class="form-group"><label for="username">Username</label><input type="text" id="username" name="username" value="${userProfile.username}" pattern="[A-Za-z0-9_.-]{4,30}" required></div>
                    <div class="form-group"><label for="firstName">First Name</label><input type="text" id="firstName" name="firstName" value="${userProfile.firstname}" required></div>
                    <div class="form-group"><label for="lastName">Last Name</label><input type="text" id="lastName" name="lastName" value="${userProfile.lastname}" required></div>
                    <div class="form-group"><label for="email">Email Address (Optional)</label><input type="email" id="email" name="email" value="${userProfile.email}"></div>
                    <div class="form-group"><label for="clientType">Client Type</label><select id="clientType" name="clientType"><option value="STUDENT" <c:if test="${userProfile.clientType == 'STUDENT'}">selected</c:if>>Student</option><option value="GUEST" <c:if test="${userProfile.clientType == 'GUEST'}">selected</c:if>>Guest</option></select></div>
                    <div class="form-group"><label for="tertiary">Tertiary Institution</label><input type="text" id="tertiary" name="tertiary" value="${userProfile.tertiaryInstitution}"></div>
                    <div class="form-group"><label for="phoneNumber">Phone Number</label><input type="text" id="phoneNumber" name="phoneNumber" value="${userProfile.phoneNumber}"></div>
                    <div class="form-group"><label for="studentNumber">Student Number (Optional for guests)</label><input type="text" id="studentNumber" name="studentNumber" value="${userProfile.studentNumber}"></div>
                    <div class="form-group"><label for="idPassportNumber">ID or Passport Number</label><input type="text" id="idPassportNumber" name="idPassportNumber" value="${userProfile.idPassportNumber}"></div>
                    <div class="form-group"><label for="biography">Biography and Trace Details</label><textarea id="biography" name="biography" rows="4">${userProfile.biography}</textarea></div>
                    <button type="submit" class="btn-save">SAVE CHANGES</button>
                </form>
            </div>
            <a href="AttendeeDashboardServlet.do" class="back-link">&larr; Return to Dashboard</a>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
</body>
</html>
