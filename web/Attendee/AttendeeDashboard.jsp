<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="manifest" href="${pageContext.request.contextPath}/manifest.webmanifest">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico">
    <title>Tickify | Client Dashboard</title>
    <style>
        :root {
            --green:#7fc342;
            --green-dark:#5da72f;
            --bg:#f9fbf8;
            --panel:#ffffff;
            --ink:#2a2f2a;
            --muted:#6c746c;
            --line:#e8ece6;
            --ok:#1f7c39;
            --ok-bg:#eaf7e7;
            --err:#9b1c1c;
            --err-bg:#ffecec;
            --shadow:0 12px 30px rgba(37,54,28,.08);
        }
        * { box-sizing:border-box; }
        body {
            margin:0;
            font-family:"Trebuchet MS","Segoe UI",sans-serif;
            background-color:#f7faf5;
            background-image:repeating-linear-gradient(
                135deg,
                #f8fbf6 0px,
                #f8fbf6 32px,
                #edf5e8 32px,
                #edf5e8 64px
            );
            color:var(--ink);
        }

        .site-header {
            width:100%;
            background:#f7f8f6;
            border-bottom:1px solid #dfe5dc;
            position:sticky;
            top:0;
            z-index:30;
        }
        .header-inner {
            width:100%;
            max-width:none;
            margin:0;
            padding:14px clamp(12px,2.7vw,36px);
        }
        .header-top {
            display:flex;
            justify-content:space-between;
            align-items:center;
            gap:14px;
            flex-wrap:wrap;
        }
        .brand {
            display:flex;
            align-items:center;
            gap:12px;
            text-decoration:none;
        }
        .brand-logo-svg {
            height:58px;
            width:232px;
            display:block;
        }
        .profile-wrap { position:relative; }
        .profile-btn {
            display:flex;
            align-items:center;
            gap:10px;
            border:1px solid #d7ded3;
            background:#fff;
            border-radius:999px;
            padding:8px 10px;
            color:#2a312b;
            font-weight:700;
            cursor:pointer;
        }
        .profile-meta {
            max-width:0;
            opacity:0;
            overflow:hidden;
            white-space:nowrap;
            transition:max-width .28s ease, opacity .22s ease;
        }
        .profile-wrap:hover .profile-meta,
        .profile-wrap:focus-within .profile-meta {
            max-width:260px;
            opacity:1;
        }
        .profile-icon {
            width:28px;
            height:28px;
            border-radius:50%;
            background:#e6eedf;
            display:flex;
            align-items:center;
            justify-content:center;
            color:#4c5b4b;
            font-weight:800;
        }
        .profile-menu {
            position:absolute;
            right:0;
            top:calc(100% + 10px);
            min-width:230px;
            background:#fff;
            border:1px solid #dee5da;
            border-radius:12px;
            box-shadow:0 14px 26px rgba(24,32,20,.12);
            padding:8px;
            display:none;
        }
        .profile-menu.open { display:block; }
        .profile-menu a {
            display:block;
            text-decoration:none;
            color:#2c342d;
            border-radius:8px;
            padding:10px;
            font-weight:700;
        }
        .profile-menu a:hover { background:#f3f7f1; }
        .profile-menu .danger { color:#9b1c1c; background:#fff5f5; }

        .header-nav {
            margin-top:12px;
            padding-top:12px;
            border-top:1px solid #e4e9e1;
            display:grid;
            grid-template-columns:minmax(260px,1fr) minmax(260px,auto);
            align-items:center;
            gap:12px;
        }
        .nav-links {
            display:flex;
            align-items:center;
            gap:18px;
            flex-wrap:wrap;
        }
        .nav-links a {
            text-decoration:none;
            color:#2d352e;
            font-weight:800;
            font-size:.96rem;
        }
        .cart-chip {
            justify-self:end;
            display:inline-flex;
            align-items:center;
            gap:8px;
            background:#ffffff;
            border:1px solid #d7ded3;
            border-radius:999px;
            padding:8px 14px;
            font-weight:800;
            color:#2f3a32;
            white-space:nowrap;
        }
        .cart-wrap {
            justify-self:end;
            display:flex;
            align-items:center;
            gap:8px;
        }
        .checkout-btn {
            border:none;
            border-radius:999px;
            background:var(--green);
            color:#fff;
            font-weight:800;
            padding:9px 14px;
            cursor:pointer;
        }
        .checkout-btn:disabled {
            background:#c9d8bf;
            cursor:not-allowed;
        }
        .cart-icon {
            color:#7fc342;
            font-size:1.1rem;
            line-height:1;
        }
        .cart-count {
            min-width:14px;
            text-align:center;
            color:#4f5a4f;
            font-weight:700;
            font-size:.95rem;
        }
        .cart-divider {
            width:1px;
            height:16px;
            background:#d7ded3;
            margin:0 2px;
        }
        .cart-amount {
            color:#586359;
            font-weight:700;
            font-size:1.02rem;
            letter-spacing:.01em;
        }
        .search-wrap {
            display:flex;
            align-items:center;
            gap:8px;
            background:#fff;
            border:1px solid #d7ded3;
            border-radius:999px;
            padding:7px 12px;
            min-width:280px;
        }
        .search-wrap input {
            border:none;
            outline:none;
            width:100%;
            background:transparent;
            color:#223026;
            font-size:.95rem;
        }

        .layout {
            width:100%;
            max-width:none;
            margin:0;
            padding:22px clamp(12px,2.7vw,36px) 110px;
        }
        .search-row {
            max-width:560px;
            margin:0 auto 14px;
        }
        .search-row .search-wrap {
            width:100%;
            min-width:0;
            box-shadow:0 8px 20px rgba(33,47,32,.08);
        }
        .panel {
            background:transparent;
            border:none;
            border-radius:0;
            box-shadow:none;
            padding:0;
        }
        .headline { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:14px; }
        .headline h2 { margin:0; font-size:1.65rem; color:#233127; }
        .toggle-row { display:flex; gap:8px; flex-wrap:wrap; }
        .toggle-btn { border:1px solid #d8e0d2; background:#fff; border-radius:999px; padding:8px 14px; font-weight:700; cursor:pointer; color:#354535; }
        .toggle-btn.active { background:var(--green); color:#fff; border-color:var(--green); }
        .control-row {
            margin:0 0 14px;
            display:grid;
            grid-template-columns:repeat(auto-fit,minmax(170px,1fr));
            gap:10px;
        }
        .control-item {
            display:flex;
            flex-direction:column;
            gap:5px;
        }
        .control-item label {
            font-size:.78rem;
            font-weight:800;
            letter-spacing:.03em;
            color:#5a655a;
            text-transform:uppercase;
        }
        .control-item select {
            border:1px solid #d8e0d2;
            background:#fff;
            border-radius:10px;
            padding:8px 10px;
            color:#2f3a32;
            font-weight:700;
            outline:none;
        }
        .flash { margin-bottom:12px; padding:11px 12px; border-radius:10px; font-weight:700; }
        .flash-success { background:var(--ok-bg); color:var(--ok); border:1px solid #cce6c7; }
        .flash-error { background:var(--err-bg); color:var(--err); border:1px solid #f0c2c2; }
        .flash-float {
            position:fixed;
            right:16px;
            top:90px;
            z-index:50;
            min-width:220px;
            box-shadow:0 8px 20px rgba(0,0,0,.15);
        }
        .global-alert {
            margin:0 0 12px;
            padding:12px 14px;
            border-radius:12px;
            border:1px solid #fecdca;
            background:#fff1ef;
            color:#912018;
            font-weight:700;
            box-shadow:0 6px 16px rgba(145,32,24,.08);
        }
        .global-alert a {
            color:#7a271a;
            text-decoration:underline;
            font-weight:800;
        }
        .event-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(320px,1fr)); gap:12px; }
        .event-card {
            background:#fff;
            border:1px solid #dce6d7;
            border-radius:16px;
            box-shadow:0 8px 22px rgba(33,47,32,.08);
            overflow:hidden;
            display:flex;
            flex-direction:column;
            min-height:360px;
        }
        .ticket-hero {
            position:relative;
            width:100%;
            min-height:190px;
            border-bottom:1px solid #dce6d7;
            background:linear-gradient(145deg,#233b21,#3d5f35 55%,#7fc342);
        }
        .ticket-album {
            width:100%;
            height:190px;
            object-fit:cover;
            display:block;
        }
        .ticket-hero::after {
            content:"";
            position:absolute;
            inset:0;
            background:linear-gradient(to top, rgba(0,0,0,.65), rgba(0,0,0,.28), rgba(0,0,0,.05));
            pointer-events:none;
        }
        .ticket-hero-meta {
            position:absolute;
            left:12px;
            right:12px;
            bottom:10px;
            z-index:2;
            color:#fff;
            text-shadow:0 1px 2px rgba(0,0,0,.5);
        }
        .ticket-hero-meta h3 {
            margin:0;
            font-size:1.08rem;
            line-height:1.22;
        }
        .ticket-hero-meta p {
            margin:4px 0 0;
            color:#f0f5ec;
            font-size:.86rem;
        }
        .ticket-hero-actions {
            position:absolute;
            right:10px;
            top:10px;
            z-index:3;
            display:flex;
            gap:8px;
            align-items:center;
        }
        .preview-btn {
            border:1px solid rgba(255,255,255,.55);
            border-radius:999px;
            background:rgba(12,16,12,.58);
            color:#fff;
            font-weight:800;
            font-size:.82rem;
            padding:7px 12px;
            cursor:pointer;
            backdrop-filter:blur(2px);
        }
        .preview-btn:hover { background:rgba(20,28,20,.82); }
        .wish-overlay { margin:0; width:auto !important; }
        .wish-overlay .love-btn {
            width:38px;
            min-height:38px;
            border-radius:999px;
            background:rgba(255,255,255,.92);
            border:1px solid rgba(255,255,255,.95);
        }
        .event-body {
            padding:12px;
            display:flex;
            flex-direction:column;
            gap:8px;
            height:100%;
        }
        .event-type { display:inline-flex; width:fit-content; background:#edf5e8; color:var(--green-dark); border:none; font-weight:700; border-radius:999px; padding:5px 10px; font-size:.82rem; }
        .event-card h3 { margin:0; font-size:1.08rem; }
        .event-card p { margin:0; color:#5f7062; font-size:.92rem; line-height:1.35; }
        .price { margin-top:4px; font-size:1.04rem; font-weight:800; color:var(--green-dark); }
        .sold-status {
            margin-top:6px;
            display:flex;
            flex-direction:column;
            gap:6px;
        }
        .sold-label {
            font-size:.84rem;
            color:#3f5043;
            font-weight:700;
        }
        .event-available-label {
            font-size:.82rem;
            color:#4f6250;
            font-weight:700;
        }
        .sold-track {
            width:100%;
            height:8px;
            border-radius:999px;
            background:#e8eee4;
            overflow:hidden;
        }
        .sold-fill {
            height:100%;
            background:linear-gradient(90deg, #7fc342 0%, #f59e0b 70%, #dc2626 100%);
            border-radius:999px;
        }
        .soldout-warning {
            margin-top:4px;
            font-size:.82rem;
            font-weight:800;
            color:#b42318;
            background:#fee4e2;
            border:1px solid #fecdca;
            border-radius:8px;
            padding:7px 8px;
            line-height:1.3;
        }
        .soldout-warning.soldout-hard {
            background:#fde2e1;
            border-color:#f8b4b1;
            color:#9b1c1c;
        }
        .event-card.sold-out {
            box-shadow:0 8px 20px rgba(120,20,20,.08);
        }
        .actions {
            display:grid;
            grid-template-columns:minmax(0,1fr) auto;
            gap:8px;
            margin-top:auto;
            align-items:stretch;
        }
        .btn { border:none; border-radius:10px; padding:10px; font-weight:800; cursor:pointer; width:100%; }
        .btn-primary { background:var(--green); color:#fff; }
        .cart-form {
            width:100%;
            margin:0;
            display:flex;
            gap:6px;
            align-items:center;
        }
        .cart-form .btn {
            width:auto;
            flex:1 1 auto;
            min-width:100px;
        }
        .wish-form {
            width:auto;
            margin:0;
            display:flex;
            justify-content:stretch;
            align-items:stretch;
        }
        .love-btn {
            width:40px;
            height:100%;
            min-height:36px;
            border:1px solid #f4d6d9;
            border-radius:10px;
            background:#fff6f7;
            color:#d9485f;
            font-size:1.18rem;
            line-height:1;
            font-weight:800;
            cursor:pointer;
            transition:background .2s ease, color .2s ease, transform .2s ease, border-color .2s ease;
        }
        .love-btn:hover {
            background:#ffe9ec;
            border-color:#f1b9c1;
            transform:translateY(-1px);
        }
        .love-btn.active {
            background:#d9485f;
            border-color:#d9485f;
            color:#ffffff;
        }
        .event-hint {
            margin-top:auto;
            background:#f5faef;
            border:1px dashed #cfe0c5;
            border-radius:10px;
            color:#486048;
            font-size:.84rem;
            padding:8px 10px;
            font-weight:700;
        }
        .preview-modal {
            position:fixed;
            inset:0;
            background:rgba(12,16,14,.55);
            display:flex;
            align-items:center;
            justify-content:center;
            padding:18px;
            z-index:120;
        }
        .preview-panel {
            width:min(980px, 100%);
            max-height:92vh;
            overflow:auto;
            background:#fff;
            border-radius:16px;
            box-shadow:0 24px 44px rgba(0,0,0,.28);
        }
        .preview-head {
            position:relative;
            min-height:240px;
            border-bottom:1px solid #dce6d7;
            background:linear-gradient(145deg,#223b21,#3e6037 55%,#7fc342);
        }
        .preview-image {
            width:100%;
            min-height:240px;
            max-height:320px;
            object-fit:cover;
            display:block;
        }
        .preview-head::after {
            content:"";
            position:absolute;
            inset:0;
            background:linear-gradient(to top, rgba(0,0,0,.7), rgba(0,0,0,.3), rgba(0,0,0,0));
            pointer-events:none;
        }
        .preview-title-wrap {
            position:absolute;
            left:16px;
            right:54px;
            bottom:14px;
            color:#fff;
            z-index:2;
            text-shadow:0 1px 2px rgba(0,0,0,.5);
        }
        .preview-title-wrap h3 { margin:0; font-size:1.35rem; }
        .preview-title-wrap p { margin:6px 0 0; color:#eff6ea; }
        .preview-close {
            position:absolute;
            right:12px;
            top:12px;
            width:34px;
            height:34px;
            border:none;
            border-radius:999px;
            background:rgba(20,26,20,.62);
            color:#fff;
            font-size:1.25rem;
            cursor:pointer;
            z-index:3;
        }
        .preview-content {
            padding:16px;
            display:flex;
            flex-direction:column;
            gap:14px;
        }
        .preview-section {
            padding:0;
            background:transparent;
        }
        .preview-section + .preview-section {
            border-top:1px solid #e7efe2;
            padding-top:14px;
        }
        .preview-section h4 {
            margin:0 0 8px;
            color:#324834;
            font-size:1rem;
        }
        .preview-detail-list {
            display:grid;
            grid-template-columns:1fr;
            gap:0;
        }
        .preview-detail {
            border-bottom:1px solid #edf3ea;
            padding:8px 0;
        }
        .preview-detail:last-child {
            border-bottom:none;
        }
        .preview-detail strong {
            display:block;
            color:#4f6250;
            font-size:.76rem;
            text-transform:uppercase;
            letter-spacing:.03em;
            margin-bottom:3px;
        }
        .preview-detail span {
            display:block;
            color:#253126;
            font-weight:700;
            font-size:.92rem;
        }
        .preview-actions {
            margin-top:10px;
            display:grid;
            grid-template-columns:minmax(0,1fr) auto;
            gap:8px;
            align-items:stretch;
        }
        .preview-actions .cart-form {
            margin:0;
            display:flex;
            gap:8px;
            align-items:center;
        }
        .preview-actions .btn-primary {
            min-width:140px;
        }
        .qty-stepper {
            display:inline-flex;
            align-items:center;
            border:1px solid #cfe0c5;
            border-radius:12px;
            overflow:hidden;
            background:#fff;
            box-shadow:inset 0 0 0 1px #eef5e9;
            flex:0 0 auto;
        }
        .qty-stepper button {
            border:none;
            background:#edf6e7;
            color:#2e3b2f;
            width:34px;
            height:36px;
            cursor:pointer;
            font-weight:800;
            font-size:1.08rem;
            line-height:1;
        }
        .qty-stepper button:hover {
            background:#dff0d4;
        }
        .qty-stepper .qty-plus {
            background:#79c84a;
            color:#ffffff;
            border-left:1px solid #6bb63f;
            font-weight:900;
        }
        .qty-stepper .qty-plus:hover {
            background:#5da72f;
        }
        .qty-stepper .qty-minus {
            color:#4c5d4f;
            border-right:1px solid #d6e4cf;
            font-weight:900;
        }
        .qty-stepper input {
            width:52px;
            border:none;
            text-align:center;
            font-weight:700;
            color:#2f3a32;
            background:#fff;
            height:36px;
            outline:none;
            font-size:.95rem;
            -moz-appearance:textfield;
            appearance:textfield;
        }
        .qty-stepper input[type=number]::-webkit-outer-spin-button,
        .qty-stepper input[type=number]::-webkit-inner-spin-button {
            -webkit-appearance:none;
            margin:0;
        }
        .ad-slider-wrap {
            margin:0 0 14px;
            background:#ffffff;
            border:1px solid #e3eee0;
            border-radius:18px;
            box-shadow:0 8px 20px rgba(33,47,32,.08);
            overflow:hidden;
            position:relative;
            width:100%;
            margin-left:0;
        }
        .ad-track {
            display:flex;
            transition:transform .55s ease;
            border-radius:inherit;
        }
        .ad-card {
            min-width:100%;
            position:relative;
            height:330px;
            overflow:hidden;
            border-radius:inherit;
        }
        .ad-image {
            position:absolute;
            inset:0;
            width:100%;
            height:100%;
            object-fit:cover;
            display:block;
        }
        .ad-image:hover {
            transform:none;
        }
        .ad-meta {
            position:absolute;
            left:0;
            right:0;
            bottom:0;
            padding:20px 18px;
            background:linear-gradient(to top, rgba(0,0,0,.72), rgba(0,0,0,.45), rgba(0,0,0,0));
            color:#ffffff;
        }
        .ad-meta h3 { margin:0 0 6px; color:#ffffff; font-size:1.45rem; }
        .ad-meta p { margin:0 0 4px; color:#f2f5ef; text-shadow:0 1px 2px rgba(0,0,0,.55); }
        .ad-dots {
            position:absolute;
            left:0;
            right:0;
            bottom:10px;
            z-index:6;
            display:flex;
            justify-content:center;
            gap:6px;
            padding:0;
        }
        .ad-dot {
            width:8px;
            height:8px;
            border-radius:50%;
            background:#d2dfca;
        }
        .ad-dot.active { background:var(--green-dark); }
        .section { margin-top:18px; }
        .section h3 { margin:0 0 10px; font-size:1.15rem; }
        .empty { background:#fbfef9; border:none; border-radius:12px; padding:14px; color:var(--muted); box-shadow:0 4px 14px rgba(33,47,32,.06); }
        .qty-input {
            width:64px;
            border:1px solid #d2dfca;
            border-radius:8px;
            padding:8px;
            font-weight:700;
            color:#2f3a32;
            background:#fff;
        }
        .cart-list {
            display:grid;
            gap:8px;
        }
        .cart-row {
            display:flex;
            align-items:center;
            justify-content:space-between;
            gap:10px;
            background:#fff;
            border:none;
            border-radius:12px;
            box-shadow:0 4px 14px rgba(33,47,32,.06);
            padding:12px;
        }
        .cart-meta {
            display:flex;
            flex-direction:column;
            gap:2px;
            color:#49574d;
        }
        .cart-remove {
            border:none;
            border-radius:8px;
            background:#ffecec;
            color:#9b1c1c;
            font-weight:700;
            padding:8px 10px;
            cursor:pointer;
        }
        .cookie-banner {
            position:fixed; left:16px; right:16px; bottom:14px; background:#fff; border:1px solid var(--line);
            border-radius:14px; box-shadow:0 16px 26px rgba(60,80,60,.2); padding:14px; display:none; z-index:20;
        }
        .cookie-actions { margin-top:10px; display:flex; gap:8px; }
        .cookie-actions button { border:none; border-radius:10px; padding:10px 12px; cursor:pointer; font-weight:700; }
        .accept-cookie { background:var(--green); color:#fff; }
        .decline-cookie { background:#f3f4f6; color:#344054; }
        .hidden { display:none; }
        @media (max-width:940px){
            .brand-logo-svg { height:46px; width:184px; }
            .search-wrap { min-width:100%; }
            .header-nav {
                grid-template-columns:1fr;
                align-items:stretch;
            }
            .cart-wrap { justify-self:start; }
            .ad-slider-wrap { width:100%; margin-left:0; }
            .ad-card { height:240px; }
            .cart-form, .wish-form { width:100%; }
            .actions { grid-template-columns:1fr; }
            .wish-form { justify-content:flex-start; }
            .preview-actions { grid-template-columns:1fr; }
            .qty-stepper button {
                width:38px;
                height:40px;
                font-size:1.08rem;
            }
            .qty-stepper input {
                width:66px;
                height:40px;
                font-size:1rem;
            }
        }
        @media (max-width:768px){ .search-wrap input, .control-item select, .btn, .toggle-btn, button { font-size:16px; } }
    </style>
</head>
<body data-user-name="${userFullName}" data-interest="Event Discovery">
    <header class="site-header">
        <div class="header-inner">
            <div class="header-top">
                <div>
                    <a href="${pageContext.request.contextPath}/AttendeeDashboardServlet.do" class="brand">
                        <svg class="brand-logo-svg" viewBox="0 0 400 100" xmlns="http://www.w3.org/2000/svg" aria-label="Tickify logo" role="img">
                            <defs>
                                <filter id="tagShadow" x="-10%" y="-10%" width="130%" height="130%">
                                    <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="#C8CDD6" flood-opacity="0.4"/>
                                </filter>
                            </defs>
                            <path d="M 14,22 Q 14,14 22,14 L 70,14 L 92,50 L 70,86 L 22,86 Q 14,86 14,78 Z" fill="#ECEEF2" stroke="#D8DCE4" stroke-width="1.5" filter="url(#tagShadow)"/>
                            <circle cx="30" cy="50" r="5.5" fill="#FFFFFF" stroke="#D8DCE4" stroke-width="1.5"/>
                            <line x1="50" y1="14" x2="50" y2="86" stroke="#D8DCE4" stroke-width="1" stroke-dasharray="3,3"/>
                            <line x1="63" y1="35" x2="82" y2="35" stroke="#A9B3BF" stroke-width="3" stroke-linecap="round"/>
                            <line x1="72" y1="35" x2="72" y2="63" stroke="#A9B3BF" stroke-width="3" stroke-linecap="round"/>
                            <text x="108" y="63" font-family="Sora, Segoe UI, sans-serif" font-size="40" font-weight="400" letter-spacing="-2" fill="#4A5568">Tickify</text>
                            <circle cx="116" cy="76" r="3" fill="#B0BAC8"/>
                        </svg>
                    </a>
                </div>

                <div class="profile-wrap">
                    <button class="profile-btn" id="profileBtn" type="button" onclick="toggleProfileMenu()">
                        <span class="profile-icon">P</span>
                        <span class="profile-meta">${userFullName} | #${userID}</span>
                    </button>
                    <div class="profile-menu" id="profileMenu">
                        <a href="${pageContext.request.contextPath}/AttendeeDashboardServlet.do">Dashboard</a>
                        <a href="${pageContext.request.contextPath}/ViewMyTickets.do">My Tickets</a>
                        <a href="${pageContext.request.contextPath}/MyOrderHistory.do">My Order History</a>
                        <a href="AttendeeViewProfileServlet.do">Update Profile</a>
                        <a href="javascript:void(0);" onclick="confirmDelete()">Delete Account</a>
                        <a href="LogoutServlet.do" class="danger">Logout</a>
                    </div>
                </div>
            </div>

            <div class="header-nav">
                <div class="nav-links">
                    <a href="#allSection" onclick="switchView('all');return false;">EVENTS</a>
                    <a href="${pageContext.request.contextPath}/ViewMyTickets.do">MY TICKETS</a>
                    <a href="${pageContext.request.contextPath}/MyOrderHistory.do">MY ORDER HISTORY</a>
                    <a href="#wishlistSection" onclick="switchView('wishlist')">MY LIKES</a>
                </div>
                <div class="cart-wrap">
                    <div class="cart-chip" aria-label="Checkout total">
                        <span class="cart-icon" aria-hidden="true">🛒</span>
                        <span class="cart-count" id="cartCountValue">${cartCount}</span>
                        <span class="cart-divider" aria-hidden="true"></span>
                        <span class="cart-amount" id="cartAmountValue">R <fmt:formatNumber value="${checkoutTotal}" minFractionDigits="2" maxFractionDigits="2"/></span>
                    </div>
                    <a href="Checkout.do" class="checkout-btn" style="text-decoration:none;display:inline-block;">Checkout</a>
                </div>
            </div>
        </div>
    </header>

    <div class="layout">
        <div class="search-row">
            <label class="search-wrap" for="eventSearch">
                <span>Search</span>
                <input id="eventSearch" type="text" placeholder="Search events, venue, city..." oninput="filterEvents(this.value)">
            </label>
        </div>
        <main class="panel">
            <% String msg = request.getParameter("msg"); %>
            <% String err = request.getParameter("err"); %>

            <section class="ad-slider-wrap" id="adSliderWrap">
                <c:choose>
                    <c:when test="${not empty adverts}">
                        <div class="ad-track" id="adTrack">
                            <c:forEach var="ad" items="${adverts}">
                                <article class="ad-card">
                                    <img class="ad-image" src="AdvertImage.do?id=${ad.advertID}" alt="${ad.title}">
                                    <div class="ad-meta">
                                        <h3>${ad.title}</h3>
                                        <p>${ad.organizationName} | ${ad.venue} | ${ad.eventDate}</p>
                                        <p>${ad.details}</p>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>
                        <div class="ad-dots" id="adDots"></div>
                    </c:when>
                    <c:otherwise>
                        <article class="ad-card"><div class="ad-meta"><h3>Upcoming Adverts</h3><p>Paid organization adverts selected by admins will appear here.</p></div></article>
                    </c:otherwise>
                </c:choose>
            </section>

            <div class="headline">
                <h2>Event Discovery Hub</h2>
                <div class="toggle-row">
                    <button id="allViewBtn" class="toggle-btn active" type="button" onclick="switchView('all')">All Events</button>
                    <button id="wishlistViewBtn" class="toggle-btn" type="button" onclick="switchView('wishlist')">Wishlist</button>
                </div>
            </div>

            <div class="control-row">
                <div class="control-item">
                    <label for="sortSelect">Sort</label>
                    <select id="sortSelect" onchange="applyEventTools()">
                        <option value="dateAsc">Date (Soonest)</option>
                        <option value="dateDesc">Date (Latest)</option>
                        <option value="priceAsc">Price (Low to High)</option>
                        <option value="priceDesc">Price (High to Low)</option>
                        <option value="nameAsc">Name (A-Z)</option>
                    </select>
                </div>
                <div class="control-item">
                    <label for="typeFilter">Event Type</label>
                    <select id="typeFilter" onchange="applyEventTools()">
                        <option value="all">All Types</option>
                    </select>
                </div>
                <div class="control-item">
                    <label for="priceFilter">Price</label>
                    <select id="priceFilter" onchange="applyEventTools()">
                        <option value="all">All Prices</option>
                        <option value="free">Free</option>
                        <option value="paid">Paid</option>
                        <option value="under200">Under R200</option>
                        <option value="200plus">R200 and above</option>
                    </select>
                </div>
            </div>

            <% if ("TicketPurchased".equals(msg)) { %>
                <div class="flash flash-success">Ticket purchased successfully.</div>
            <% } else if ("AddedToCart".equals(msg)) { %>
                <div class="flash flash-success">Ticket(s) added to cart.</div>
            <% } else if ("RemovedFromCart".equals(msg)) { %>
                <div class="flash flash-success">Item removed from cart.</div>
            <% } else if ("CartUpdated".equals(msg)) { %>
                <div class="flash flash-success">Cart quantity updated.</div>
            <% } else if ("CartCleared".equals(msg)) { %>
                <div class="flash flash-success">Cart cleared.</div>
            <% } else if ("CheckoutComplete".equals(msg)) { %>
                <div class="flash flash-success">Checkout completed successfully.</div>
            <% } else if ("CheckoutPartial".equals(msg)) { %>
                <div class="flash flash-success">Checkout partially completed. Some tickets were unavailable.</div>
            <% } else if ("WishlistAdded".equals(msg)) { %>
                <div class="flash flash-success">Event added to wishlist.</div>
            <% } else if ("WishlistRemoved".equals(msg)) { %>
                <div class="flash flash-success">Event removed from wishlist.</div>
            <% } %>

            <% if (err != null) { %>
                <div class="flash flash-error">
                    <% if ("NoTicket".equals(err)) { %>
                        No ticket is currently available for this event.
                    <% } else if ("InvalidEvent".equals(err)) { %>
                        Selected event is invalid.
                    <% } else if ("WishlistFailed".equals(err)) { %>
                        Wishlist update failed. Please try again.
                    <% } else if ("CartUpdateFailed".equals(err)) { %>
                        Unable to update cart. Please try again.
                    <% } else if ("CartEmpty".equals(err)) { %>
                        Your cart is empty. Add events before checkout.
                    <% } else if ("CheckoutFailed".equals(err)) { %>
                        Checkout failed. Please try again.
                    <% } else if ("AgeRestricted".equals(err)) { %>
                        Your account is under 18 and cannot purchase tickets for this event type.
                    <% } else if ("SoldOut".equals(err)) { %>
                        This event is sold out. Live stock changed before your action completed.
                    <% } else { %>
                        Action failed. Please try again.
                    <% } %>
                </div>
            <% } %>

            <c:if test="${nearSoldOutWishlistCount > 0}">
                <div class="global-alert">
                    Alert: ${nearSoldOutWishlistCount} wishlisted event(s) are almost sold out. Visit
                    <a href="#wishlistSection" onclick="switchView('wishlist');return false;">Wishlist</a>
                    to purchase before tickets run out.
                </div>
            </c:if>

            <section id="allSection" class="section">
                <h3>Available Events</h3>
                <div class="event-grid" id="allEventGrid">
                    <c:choose>
                        <c:when test="${not empty eventList}">
                            <c:forEach var="event" items="${eventList}">
                                <article class="event-card searchable-card${event.soldOut ? ' sold-out' : ''}" data-event-id="${event.id}" data-search="${event.name} ${event.type} ${event.venueName} ${event.address} ${event.date} ${event.status} ${event.description}" data-name="${event.name}" data-type="${event.type}" data-price="${event.price}" data-date="${event.date}" data-venue="${event.venueName}" data-address="${event.address}" data-description="${event.description}" data-info-url="${event.infoUrl}" data-status="${event.status}" data-total-tickets="${event.totalTickets}" data-sold-tickets="${event.soldTickets}" data-available-tickets="${event.availableTickets}" data-sold-percentage="${event.soldPercentage}" data-wishlisted="${event.wishlisted}" data-purchased="${event.purchased}" data-album-url="EventAlbumImage.do?eventID=${event.id}">
                                    <div class="ticket-hero">
                                        <img class="ticket-album" src="EventAlbumImage.do?eventID=${event.id}" alt="${event.name} album" onerror="this.style.display='none';">
                                        <div class="ticket-hero-actions">
                                            <button type="button" class="preview-btn" onclick="openEventPreview(this)">Preview</button>
                                            <form action="Wishlist.do" method="POST" class="wish-form wish-overlay">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="eventID" value="${event.id}">
                                                <c:choose>
                                                    <c:when test="${event.wishlisted}"><input type="hidden" name="action" value="remove"><button type="submit" class="love-btn active" aria-label="Remove from favorites">♥</button></c:when>
                                                    <c:otherwise><input type="hidden" name="action" value="add"><button type="submit" class="love-btn" aria-label="Add to favorites">♡</button></c:otherwise>
                                                </c:choose>
                                            </form>
                                        </div>
                                        <div class="ticket-hero-meta">
                                            <h3>${event.name}</h3>
                                            <p>${event.venueName}</p>
                                        </div>
                                    </div>
                                    <div class="event-body">
                                        <span class="event-type">${event.type}</span>
                                        <p>${event.venueName} - ${event.address}</p>
                                        <p>${event.date}</p>
                                        <div class="price"><c:choose><c:when test="${event.price > 0}">R ${event.price}</c:when><c:otherwise>FREE</c:otherwise></c:choose></div>
                                        <div class="sold-status">
                                            <div class="sold-label event-sold-label">Tickets sold: ${event.soldPercentage}%</div>
                                            <div class="sold-track"><div class="sold-fill event-sold-fill" style="width:${event.soldPercentage}%;"></div></div>
                                            <div class="event-available-label">Available: ${event.availableTickets} / ${event.totalTickets}</div>
                                            <c:choose>
                                                <c:when test="${event.soldOut}">
                                                    <div class="soldout-warning soldout-hard event-stock-warning">Sold out</div>
                                                </c:when>
                                                <c:when test="${event.nearlySoldOut}">
                                                    <div class="soldout-warning event-stock-warning">Almost sold out. Buy soon.</div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="soldout-warning event-stock-warning hidden"></div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="event-hint">Use Preview to see full event details, ticket quantity, and add-to-cart controls.</div>
                                    </div>
                                </article>
                            </c:forEach>
                        </c:when>
                        <c:otherwise><div class="empty">No events are currently available.</div></c:otherwise>
                    </c:choose>
                </div>
            </section>

            <section id="wishlistSection" class="section hidden">
                <h3>Saved Wishlist Events</h3>
                <div class="event-grid" id="wishlistEventGrid">
                    <c:choose>
                        <c:when test="${not empty wishlistEvents}">
                            <c:forEach var="event" items="${wishlistEvents}">
                                <article class="event-card searchable-card${event.soldOut ? ' sold-out' : ''}" data-event-id="${event.id}" data-search="${event.name} ${event.type} ${event.venueName} ${event.address} ${event.date} ${event.status} ${event.description}" data-name="${event.name}" data-type="${event.type}" data-price="${event.price}" data-date="${event.date}" data-venue="${event.venueName}" data-address="${event.address}" data-description="${event.description}" data-info-url="${event.infoUrl}" data-status="${event.status}" data-total-tickets="${event.totalTickets}" data-sold-tickets="${event.soldTickets}" data-available-tickets="${event.availableTickets}" data-sold-percentage="${event.soldPercentage}" data-wishlisted="true" data-purchased="${event.purchased}" data-album-url="EventAlbumImage.do?eventID=${event.id}">
                                    <div class="ticket-hero">
                                        <img class="ticket-album" src="EventAlbumImage.do?eventID=${event.id}" alt="${event.name} album" onerror="this.style.display='none';">
                                        <div class="ticket-hero-actions">
                                            <button type="button" class="preview-btn" onclick="openEventPreview(this)">Preview</button>
                                            <form action="Wishlist.do" method="POST" class="wish-form wish-overlay"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="eventID" value="${event.id}"><input type="hidden" name="action" value="remove"><button type="submit" class="love-btn active" aria-label="Remove from favorites">♥</button></form>
                                        </div>
                                        <div class="ticket-hero-meta">
                                            <h3>${event.name}</h3>
                                            <p>${event.venueName}</p>
                                        </div>
                                    </div>
                                    <div class="event-body">
                                        <span class="event-type">${event.type}</span>
                                        <p>${event.venueName} - ${event.address}</p>
                                        <p>${event.date}</p>
                                        <div class="price"><c:choose><c:when test="${event.price > 0}">R ${event.price}</c:when><c:otherwise>FREE</c:otherwise></c:choose></div>
                                        <div class="sold-status">
                                            <div class="sold-label event-sold-label">Tickets sold: ${event.soldPercentage}%</div>
                                            <div class="sold-track"><div class="sold-fill event-sold-fill" style="width:${event.soldPercentage}%;"></div></div>
                                            <div class="event-available-label">Available: ${event.availableTickets} / ${event.totalTickets}</div>
                                            <c:choose>
                                                <c:when test="${event.soldOut}">
                                                    <div class="soldout-warning soldout-hard event-stock-warning">Sold out</div>
                                                </c:when>
                                                <c:when test="${event.nearlySoldOut}">
                                                    <div class="soldout-warning event-stock-warning">Almost sold out. Buy soon.</div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="soldout-warning event-stock-warning hidden"></div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="event-hint">Use Preview to see full event details, ticket quantity, and add-to-cart controls.</div>
                                    </div>
                                </article>
                            </c:forEach>
                        </c:when>
                        <c:otherwise><div class="empty">No wishlist events yet. Add events from All Events.</div></c:otherwise>
                    </c:choose>
                </div>
            </section>
        </main>
    </div>

    <div id="eventPreviewModal" class="preview-modal hidden" aria-hidden="true" onclick="backdropClosePreview(event)">
        <div class="preview-panel" role="dialog" aria-modal="true" aria-labelledby="previewEventTitle">
            <div class="preview-head">
                <img id="previewEventImage" class="preview-image" src="" alt="Event album preview" onerror="this.style.display='none';">
                <button type="button" class="preview-close" aria-label="Close preview" onclick="closeEventPreview()">×</button>
                <div class="preview-title-wrap">
                    <h3 id="previewEventTitle">Event Preview</h3>
                    <p id="previewEventVenue">Venue details</p>
                </div>
            </div>
            <div class="preview-content">
                <section class="preview-section">
                    <h4>Event Details</h4>
                    <div class="preview-detail-list">
                        <div class="preview-detail"><strong>Type</strong><span id="previewEventType">-</span></div>
                        <div class="preview-detail"><strong>Date</strong><span id="previewEventDate">-</span></div>
                        <div class="preview-detail"><strong>Venue</strong><span id="previewEventVenueName">-</span></div>
                        <div class="preview-detail"><strong>Address</strong><span id="previewEventAddress">-</span></div>
                        <div class="preview-detail"><strong>Status</strong><span id="previewEventStatus">-</span></div>
                        <div class="preview-detail"><strong>Ticket Price</strong><span id="previewEventPrice">-</span></div>
                        <div class="preview-detail"><strong>Available Tickets</strong><span id="previewEventAvailable">-</span></div>
                    </div>
                    <div class="preview-detail" style="margin-top:10px;"><strong>Description</strong><span id="previewEventDescription" style="display:block;margin-top:4px;">-</span></div>
                    <div class="preview-detail" style="margin-top:8px;"><strong>More Info</strong><span id="previewEventInfoUrl">-</span></div>
                    <div class="preview-detail" style="margin-top:8px;"><strong>Share</strong>
                        <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:6px;">
                            <button type="button" class="btn btn-alt" onclick="shareEvent('WHATSAPP')">WhatsApp</button>
                            <button type="button" class="btn btn-alt" onclick="shareEvent('TWITTER')">Twitter</button>
                            <button type="button" class="btn btn-alt" onclick="shareEvent('NATIVE')">Share</button>
                        </div>
                    </div>
                    <div class="sold-status" style="margin-top:10px;">
                        <div class="sold-label" id="previewEventSoldLabel">Tickets sold: 0%</div>
                        <div class="sold-track"><div id="previewEventSoldFill" class="sold-fill" style="width:0%;"></div></div>
                    </div>
                </section>
                <section class="preview-section">
                    <h4>Ticket Actions</h4>
                    <p id="previewStockNotice" style="margin:0 0 10px;color:#4f6250;">Pick number of tickets and add to cart without leaving this page.</p>
                    <div class="preview-actions">
                        <form action="BookTicket.do" method="POST" class="cart-form" id="previewCartForm">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="add">
                            <input type="hidden" name="eventID" id="previewCartEventID" value="">
                            <div class="qty-stepper">
                                <button type="button" class="qty-minus" onclick="stepQty(this,-1)" aria-label="Decrease ticket quantity">-</button>
                                <input type="number" name="quantity" min="1" value="1" class="qty-input" aria-label="Ticket quantity" readonly>
                                <button type="button" class="qty-plus" onclick="stepQty(this,1)" aria-label="Increase ticket quantity">+</button>
                            </div>
                            <button type="submit" class="btn btn-primary" id="previewAddButton">Add to Cart</button>
                        </form>
                        <form action="Wishlist.do" method="POST" class="wish-form" id="previewWishForm">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="eventID" id="previewWishEventID" value="">
                            <input type="hidden" name="action" id="previewWishAction" value="add">
                            <button type="submit" class="love-btn" id="previewWishButton" aria-label="Add to favorites">♡</button>
                        </form>
                    </div>
                </section>
            </div>
        </div>
    </div>

    <div id="cookieBanner" class="cookie-banner">
        <strong>Cookie Preferences</strong>
        <div style="margin-top:6px; color:#475467;">We use cookies to remember dashboard view, wishlist timing, and visit counters. We do not store sensitive data in cookies.</div>
        <div class="cookie-actions"><button class="accept-cookie" type="button" onclick="setCookieConsent('yes')">Accept</button><button class="decline-cookie" type="button" onclick="setCookieConsent('no')">Decline</button></div>
    </div>

    <script>
        function confirmDelete() { if (confirm("Permanently delete your account?")) { window.location.href = "AttendeeDeleteProfileServlet.do"; } }
        function stepQty(btn, delta) {
            var input = btn.parentElement.querySelector('input[name="quantity"]');
            if (!input) { return; }
            var current = parseInt(input.value || "1", 10);
            if (isNaN(current)) { current = 1; }
            input.value = Math.max(1, current + delta);
        }
        function showFloatingFlash(message, ok) {
            var existing = document.querySelector('.flash-float');
            if (existing) { existing.remove(); }
            var div = document.createElement('div');
            div.className = 'flash flash-float ' + (ok ? 'flash-success' : 'flash-error');
            div.textContent = message;
            document.body.appendChild(div);
            setTimeout(function () { if (div.parentNode) { div.parentNode.removeChild(div); } }, 1700);
        }
        function updateCartChip(count, total) {
            var countEl = document.getElementById('cartCountValue');
            var amountEl = document.getElementById('cartAmountValue');
            if (countEl && count !== null && count !== undefined) { countEl.textContent = String(count); }
            if (amountEl && total !== null && total !== undefined) {
                var safeTotal = Number(total);
                amountEl.textContent = 'R ' + (isNaN(safeTotal) ? '0.00' : safeTotal.toFixed(2));
            }
        }
        async function submitFormAjax(form) {
            try {
                var endpoint = form.getAttribute('action') || form.action;
                var body = new URLSearchParams(new FormData(form));
                var response = await fetch(endpoint, {
                    method: 'POST',
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
                    body: body,
                    credentials: 'same-origin'
                });

                var contentType = (response.headers.get('content-type') || '').toLowerCase();
                var data = null;
                if (contentType.indexOf('application/json') !== -1) {
                    try {
                        data = await response.json();
                    } catch (e) {
                        data = null;
                    }
                } else {
                    await response.text();
                }

                if (!response.ok) {
                    return {
                        ok: false,
                        message: (data && data.message) || ('Request failed (' + response.status + ')')
                    };
                }

                if (!data || typeof data !== 'object') {
                    return { ok: false, message: 'Unexpected server response' };
                }

                return data;
            } catch (e) {
                return { ok: false, message: 'Network error. Please try again.' };
            }
        }
        function handleSessionExpiry(data) {
            var message = data && data.message ? String(data.message).toLowerCase() : '';
            if (message.indexOf('session expired') !== -1) {
                window.location.href = 'Login.jsp';
                return true;
            }
            return false;
        }
        function refreshWishlistButton(form, wishlisted) {
            var actionInput = form.querySelector('input[name="action"]');
            var btn = form.querySelector('button[type="submit"]');
            if (!actionInput || !btn) { return; }
            if (wishlisted) {
                actionInput.value = 'remove';
                btn.classList.add('active');
                btn.textContent = '♥';
                btn.setAttribute('aria-label', 'Remove from favorites');
            } else {
                actionInput.value = 'add';
                btn.classList.remove('active');
                btn.textContent = '♡';
                btn.setAttribute('aria-label', 'Add to favorites');
            }
            var eventInput = form.querySelector('input[name="eventID"]');
            if (eventInput && eventInput.value) {
                syncWishlistState(eventInput.value, wishlisted, form);
            }
        }
        function syncWishlistState(eventId, wishlisted, sourceForm) {
            if (!eventId) { return; }
            document.querySelectorAll('.event-card[data-event-id="' + eventId + '"]').forEach(function (card) {
                card.setAttribute('data-wishlisted', wishlisted ? 'true' : 'false');
                card.querySelectorAll('.wish-form').forEach(function (form) {
                    if (sourceForm && form === sourceForm) { return; }
                    var actionInput = form.querySelector('input[name="action"]');
                    var btn = form.querySelector('button[type="submit"]');
                    if (!actionInput || !btn) { return; }
                    if (wishlisted) {
                        actionInput.value = 'remove';
                        btn.classList.add('active');
                        btn.textContent = '♥';
                        btn.setAttribute('aria-label', 'Remove from favorites');
                    } else {
                        actionInput.value = 'add';
                        btn.classList.remove('active');
                        btn.textContent = '♡';
                        btn.setAttribute('aria-label', 'Add to favorites');
                    }
                });
            });
            var previewForm = document.getElementById('previewWishForm');
            if (previewForm && (!sourceForm || previewForm !== sourceForm)) {
                var previewAction = document.getElementById('previewWishAction');
                var previewBtn = document.getElementById('previewWishButton');
                var previewEventInput = document.getElementById('previewWishEventID');
                if (previewAction && previewBtn && previewEventInput && previewEventInput.value === String(eventId)) {
                    if (wishlisted) {
                        previewAction.value = 'remove';
                        previewBtn.classList.add('active');
                        previewBtn.textContent = '♥';
                        previewBtn.setAttribute('aria-label', 'Remove from favorites');
                    } else {
                        previewAction.value = 'add';
                        previewBtn.classList.remove('active');
                        previewBtn.textContent = '♡';
                        previewBtn.setAttribute('aria-label', 'Add to favorites');
                    }
                }
            }
        }
        function removeWishlistCardsByEventId(eventId) {
            var grid = document.getElementById('wishlistEventGrid');
            if (!grid || !eventId) { return; }
            grid.querySelectorAll('.event-card[data-event-id="' + eventId + '"]').forEach(function (card) {
                card.remove();
            });
            ensureWishlistEmptyState();
        }
        function formatPreviewPrice(value) {
            var amount = Number(value);
            if (isNaN(amount) || amount <= 0) { return 'FREE'; }
            return 'R ' + amount.toFixed(2);
        }
        function formatStockWarning(available, soldPercentage) {
            if (available <= 0) {
                return { text: 'Sold out', hard: true, show: true };
            }
            if (soldPercentage >= 80) {
                return { text: 'Almost sold out. Buy soon.', hard: false, show: true };
            }
            return { text: '', hard: false, show: false };
        }
        function applyCardStockState(card, totalTickets, soldTickets) {
            if (!card) { return; }
            totalTickets = Math.max(0, toInt(totalTickets));
            soldTickets = Math.max(0, Math.min(totalTickets, toInt(soldTickets)));
            var available = Math.max(0, totalTickets - soldTickets);
            var soldPercentage = totalTickets > 0 ? Math.round((soldTickets * 100) / totalTickets) : 0;

            card.setAttribute('data-total-tickets', String(totalTickets));
            card.setAttribute('data-sold-tickets', String(soldTickets));
            card.setAttribute('data-available-tickets', String(available));
            card.setAttribute('data-sold-percentage', String(soldPercentage));

            var soldLabel = card.querySelector('.event-sold-label');
            if (soldLabel) {
                soldLabel.textContent = 'Tickets sold: ' + soldPercentage + '%';
            }
            var soldFill = card.querySelector('.event-sold-fill');
            if (soldFill) {
                soldFill.style.width = soldPercentage + '%';
            }
            var availableLabel = card.querySelector('.event-available-label');
            if (availableLabel) {
                availableLabel.textContent = 'Available: ' + available + ' / ' + totalTickets;
            }

            var warningMeta = formatStockWarning(available, soldPercentage);
            var warning = card.querySelector('.event-stock-warning');
            if (warning) {
                warning.textContent = warningMeta.text;
                warning.classList.toggle('hidden', !warningMeta.show);
                warning.classList.toggle('soldout-hard', warningMeta.hard);
            }
            card.classList.toggle('sold-out', available <= 0);
        }
        function applyPreviewStockState(totalTickets, soldTickets) {
            totalTickets = Math.max(0, toInt(totalTickets));
            soldTickets = Math.max(0, Math.min(totalTickets, toInt(soldTickets)));
            var available = Math.max(0, totalTickets - soldTickets);
            var soldPercentage = totalTickets > 0 ? Math.round((soldTickets * 100) / totalTickets) : 0;

            document.getElementById('previewEventAvailable').textContent = available + ' (out of ' + totalTickets + ')';
            document.getElementById('previewEventSoldLabel').textContent = 'Tickets sold: ' + soldPercentage + '%';
            document.getElementById('previewEventSoldFill').style.width = soldPercentage + '%';

            var addBtn = document.getElementById('previewAddButton');
            var qtyInput = document.querySelector('#previewCartForm input[name="quantity"]');
            var minusBtn = document.querySelector('#previewCartForm .qty-minus');
            var plusBtn = document.querySelector('#previewCartForm .qty-plus');
            var notice = document.getElementById('previewStockNotice');
            var soldOut = available <= 0;

            if (addBtn) {
                addBtn.disabled = soldOut;
                addBtn.textContent = soldOut ? 'Sold Out' : 'Add to Cart';
            }
            if (qtyInput) { qtyInput.disabled = soldOut; }
            if (minusBtn) { minusBtn.disabled = soldOut; }
            if (plusBtn) { plusBtn.disabled = soldOut; }
            if (notice) {
                notice.textContent = soldOut
                    ? 'This event is sold out right now. Live stock updates will unlock it when tickets become available.'
                    : 'Pick number of tickets and add to cart without leaving this page.';
            }
        }
        async function pollLiveStock() {
            try {
                var response = await fetch('AttendeeDashboardServlet.do?ajax=stock', {
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
                    credentials: 'same-origin'
                });
                if (!response.ok) { return; }
                var data = await response.json();
                if (!data || data.ok !== true || !Array.isArray(data.events)) { return; }

                data.events.forEach(function (item) {
                    var eventId = String(item.id || '');
                    if (!eventId) { return; }
                    document.querySelectorAll('.event-card[data-event-id="' + eventId + '"]').forEach(function (card) {
                        applyCardStockState(card, item.totalTickets, item.soldTickets);
                    });

                    var modal = document.getElementById('eventPreviewModal');
                    if (modal && !modal.classList.contains('hidden')
                            && String(modal.getAttribute('data-event-id') || '') === eventId) {
                        applyPreviewStockState(item.totalTickets, item.soldTickets);
                    }
                });
            } catch (e) {
                // Keep UI responsive even if periodic stock refresh fails.
            }
        }
        function toInt(value) {
            var num = parseInt(value, 10);
            return isNaN(num) ? 0 : num;
        }
        function openEventPreview(btn) {
            var card = btn ? btn.closest('.event-card') : null;
            if (!card) { return; }

            var eventId = card.getAttribute('data-event-id') || '';
            var name = card.getAttribute('data-name') || 'Event';
            var type = card.getAttribute('data-type') || '-';
            var date = card.getAttribute('data-date') || '-';
            var venue = card.getAttribute('data-venue') || '-';
            var address = card.getAttribute('data-address') || '-';
            var description = card.getAttribute('data-description') || '';
            var infoUrl = card.getAttribute('data-info-url') || '';
            var status = card.getAttribute('data-status') || 'ACTIVE';
            var price = card.getAttribute('data-price') || '0';
            var totalTickets = toInt(card.getAttribute('data-total-tickets'));
            var soldTickets = toInt(card.getAttribute('data-sold-tickets'));
            var soldPercentage = Math.max(0, Math.min(100, toInt(card.getAttribute('data-sold-percentage'))));
            var wishlisted = String(card.getAttribute('data-wishlisted')).toLowerCase() === 'true';
            var albumUrl = card.getAttribute('data-album-url') || ('EventAlbumImage.do?eventID=' + eventId);

            document.getElementById('previewEventTitle').textContent = name;
            document.getElementById('previewEventVenue').textContent = venue + ' | ' + date;
            document.getElementById('previewEventType').textContent = type;
            document.getElementById('previewEventDate').textContent = date;
            document.getElementById('previewEventVenueName').textContent = venue;
            document.getElementById('previewEventAddress').textContent = address;
            document.getElementById('previewEventStatus').textContent = status;
            document.getElementById('previewEventDescription').textContent = description ? description : 'No description provided yet.';
            var infoNode = document.getElementById('previewEventInfoUrl');
            if (infoUrl) {
                infoNode.innerHTML = '<a href="' + infoUrl + '" target="_blank" rel="noopener noreferrer">Open event info link</a>';
            } else {
                infoNode.textContent = 'No external info link available.';
            }
            document.getElementById('previewEventPrice').textContent = formatPreviewPrice(price);
            applyPreviewStockState(totalTickets, soldTickets);

            var image = document.getElementById('previewEventImage');
            image.style.display = 'block';
            image.src = albumUrl;
            image.alt = name + ' album';

            document.getElementById('previewCartEventID').value = eventId;
            document.getElementById('previewWishEventID').value = eventId;
            document.getElementById('previewWishAction').value = wishlisted ? 'remove' : 'add';
            var previewWishBtn = document.getElementById('previewWishButton');
            if (wishlisted) {
                previewWishBtn.classList.add('active');
                previewWishBtn.textContent = '♥';
                previewWishBtn.setAttribute('aria-label', 'Remove from favorites');
            } else {
                previewWishBtn.classList.remove('active');
                previewWishBtn.textContent = '♡';
                previewWishBtn.setAttribute('aria-label', 'Add to favorites');
            }

            var qtyInput = document.querySelector('#previewCartForm input[name="quantity"]');
            if (qtyInput) { qtyInput.value = '1'; }

            var modal = document.getElementById('eventPreviewModal');
            modal.setAttribute('data-event-id', eventId);
            modal.setAttribute('data-event-name', name);
            modal.classList.remove('hidden');
            modal.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
            logEventEngagement(eventId, 'PREVIEW', 'WEB');
        }
        function closeEventPreview() {
            var modal = document.getElementById('eventPreviewModal');
            if (!modal) { return; }
            modal.classList.add('hidden');
            modal.setAttribute('aria-hidden', 'true');
            modal.removeAttribute('data-event-id');
            document.body.style.overflow = '';
        }
        function backdropClosePreview(event) {
            if (event && event.target && event.target.id === 'eventPreviewModal') {
                closeEventPreview();
            }
        }
        function ensureWishlistEmptyState() {
            var grid = document.getElementById('wishlistEventGrid');
            if (!grid) { return; }
            var cards = grid.querySelectorAll('.searchable-card');
            var existingEmpty = grid.querySelector('.wishlist-empty-dynamic');
            if (cards.length === 0) {
                if (!existingEmpty) {
                    var empty = document.createElement('div');
                    empty.className = 'empty wishlist-empty-dynamic';
                    empty.textContent = 'No wishlist events yet. Add events from All Events.';
                    grid.appendChild(empty);
                }
            } else if (existingEmpty) {
                existingEmpty.remove();
            }
        }
        function initAjaxActions() {
            document.querySelectorAll('.cart-form').forEach(function (form) {
                form.addEventListener('submit', async function (event) {
                    event.preventDefault();
                    var data = await submitFormAjax(form);
                    if (handleSessionExpiry(data)) { return; }
                    if (data && data.ok) {
                        updateCartChip(data.cartCount, data.checkoutTotal);
                        showFloatingFlash(data.message || 'Added to cart', true);
                        pollLiveStock();
                    } else {
                        showFloatingFlash((data && data.message) || 'Could not add to cart', false);
                    }
                });
            });

            document.querySelectorAll('.wish-form').forEach(function (form) {
                form.addEventListener('submit', async function (event) {
                    event.preventDefault();
                    var data = await submitFormAjax(form);
                    if (handleSessionExpiry(data)) { return; }
                    if (data && data.ok) {
                        refreshWishlistButton(form, !!data.wishlisted);
                        var card = form.closest('.event-card');
                        var grid = form.closest('.event-grid');
                        var eventInput = form.querySelector('input[name="eventID"]');
                        var eventId = eventInput ? eventInput.value : null;
                        if (grid && grid.id === 'wishlistEventGrid' && !data.wishlisted && card) {
                            card.remove();
                            ensureWishlistEmptyState();
                        } else if (!data.wishlisted && eventId) {
                            removeWishlistCardsByEventId(eventId);
                        }
                        showFloatingFlash(data.message || 'Wishlist updated', true);
                    } else {
                        showFloatingFlash((data && data.message) || 'Wishlist update failed', false);
                    }
                });
            });
        }
        var adIndex = 0;
        var adTimer = null;
        function renderAdDots(total) {
            var dotsWrap = document.getElementById("adDots");
            if (!dotsWrap) { return; }
            dotsWrap.innerHTML = "";
            for (var i = 0; i < total; i++) {
                var dot = document.createElement("span");
                dot.className = "ad-dot" + (i === adIndex ? " active" : "");
                dotsWrap.appendChild(dot);
            }
        }
        function moveAds() {
            var track = document.getElementById("adTrack");
            if (!track) { return; }
            var slides = track.children.length;
            if (slides <= 1) { return; }
            adIndex = (adIndex + 1) % slides;
            track.style.transform = "translateX(-" + (adIndex * 100) + "%)";
            renderAdDots(slides);
        }
        function initAdSlider() {
            var track = document.getElementById("adTrack");
            if (!track) { return; }
            var slides = track.children.length;
            renderAdDots(slides);
            if (slides <= 1) { return; }
            var wrap = document.getElementById("adSliderWrap");
            adTimer = setInterval(moveAds, 3800);
            wrap.addEventListener("mouseenter", function () {
                if (adTimer) { clearInterval(adTimer); adTimer = null; }
            });
            wrap.addEventListener("mouseleave", function () {
                if (!adTimer) { adTimer = setInterval(moveAds, 3800); }
            });
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
        function filterEvents(query) {
            var input = document.getElementById("eventSearch");
            if (input && input.value !== query) {
                input.value = query || "";
            }
            applyEventTools();
        }

        function buildEventShareUrl(eventId) {
            return window.location.origin + window.location.pathname + '#event-' + eventId;
        }

        async function logEventEngagement(eventId, action, channel) {
            if (!eventId) { return; }
            try {
                var body = new URLSearchParams();
                body.set('_csrf', '${sessionScope.csrfToken}');
                body.set('eventID', eventId);
                body.set('action', action);
                body.set('channel', channel);
                await fetch('EventEngagement.do', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
                    body: body
                });
            } catch (e) {
                // non-blocking analytics call
            }
        }

        function shareEvent(channel) {
            var modal = document.getElementById('eventPreviewModal');
            if (!modal) { return; }
            var eventId = modal.getAttribute('data-event-id') || '';
            var eventName = modal.getAttribute('data-event-name') || 'Tickify Event';
            var url = buildEventShareUrl(eventId);
            var text = eventName + ' on Tickify: ' + url;

            if (channel === 'WHATSAPP') {
                window.open('https://wa.me/?text=' + encodeURIComponent(text), '_blank');
            } else if (channel === 'TWITTER') {
                window.open('https://twitter.com/intent/tweet?text=' + encodeURIComponent(text), '_blank');
            } else if (navigator.share) {
                navigator.share({ title: eventName, text: 'Check this event on Tickify', url: url }).catch(function () {});
            } else {
                window.prompt('Copy this event link', url);
            }

            logEventEngagement(eventId, 'SHARE', channel);
        }
        function parseEventDate(value) {
            if (!value) { return 0; }
            var t = Date.parse(value);
            return isNaN(t) ? 0 : t;
        }
        function applyEventTools() {
            var searchInput = document.getElementById("eventSearch");
            var typeFilter = document.getElementById("typeFilter");
            var priceFilter = document.getElementById("priceFilter");
            var sortSelect = document.getElementById("sortSelect");
            var query = ((searchInput && searchInput.value) || "").toLowerCase().trim();
            var selectedType = (typeFilter && typeFilter.value) || "all";
            var selectedPrice = (priceFilter && priceFilter.value) || "all";
            var sortMode = (sortSelect && sortSelect.value) || "dateAsc";

            ["allEventGrid", "wishlistEventGrid"].forEach(function (gridId) {
                var grid = document.getElementById(gridId);
                if (!grid) { return; }
                var cards = Array.prototype.slice.call(grid.querySelectorAll(".searchable-card"));

                cards.forEach(function (card) {
                    var haystack = (card.getAttribute("data-search") || "").toLowerCase();
                    var type = (card.getAttribute("data-type") || "").toLowerCase();
                    var price = parseFloat(card.getAttribute("data-price") || "0");
                    var matchesSearch = haystack.indexOf(query) !== -1;
                    var matchesType = selectedType === "all" || type === selectedType;
                    var matchesPrice = selectedPrice === "all"
                        || (selectedPrice === "free" && price === 0)
                        || (selectedPrice === "paid" && price > 0)
                        || (selectedPrice === "under200" && price > 0 && price < 200)
                        || (selectedPrice === "200plus" && price >= 200);
                    card.style.display = (matchesSearch && matchesType && matchesPrice) ? "flex" : "none";
                });

                cards.sort(function (a, b) {
                    var aName = (a.getAttribute("data-name") || "").toLowerCase();
                    var bName = (b.getAttribute("data-name") || "").toLowerCase();
                    var aPrice = parseFloat(a.getAttribute("data-price") || "0");
                    var bPrice = parseFloat(b.getAttribute("data-price") || "0");
                    var aDate = parseEventDate(a.getAttribute("data-date"));
                    var bDate = parseEventDate(b.getAttribute("data-date"));

                    if (sortMode === "priceAsc") { return aPrice - bPrice; }
                    if (sortMode === "priceDesc") { return bPrice - aPrice; }
                    if (sortMode === "nameAsc") { return aName.localeCompare(bName); }
                    if (sortMode === "dateDesc") { return bDate - aDate; }
                    return aDate - bDate;
                });

                cards.forEach(function (card) { grid.appendChild(card); });
            });
        }
        function hydrateTypeFilter() {
            var typeFilter = document.getElementById("typeFilter");
            if (!typeFilter) { return; }
            var seen = {};
            var cards = document.querySelectorAll("#allEventGrid .searchable-card, #wishlistEventGrid .searchable-card");
            cards.forEach(function (card) {
                var rawType = card.getAttribute("data-type") || "";
                var value = rawType.toLowerCase();
                if (!value || seen[value]) { return; }
                seen[value] = true;
                var opt = document.createElement("option");
                opt.value = value;
                opt.textContent = rawType;
                typeFilter.appendChild(opt);
            });
        }
        function setCookie(name, value, days) {
            var expires = "";
            if (days) { var date = new Date(); date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000)); expires = "; expires=" + date.toUTCString(); }
            document.cookie = name + "=" + encodeURIComponent(value) + expires + "; path=/; SameSite=Lax";
        }
        function getCookie(name) {
            var nameEQ = name + "="; var ca = document.cookie.split(';');
            for (var i = 0; i < ca.length; i++) { var c = ca[i]; while (c.charAt(0) === ' ') { c = c.substring(1, c.length); } if (c.indexOf(nameEQ) === 0) { return decodeURIComponent(c.substring(nameEQ.length, c.length)); } }
            return null;
        }
        function hasConsent() { return getCookie("tk_cookie_consent") === "yes"; }
        function setCookieConsent(value) { setCookie("tk_cookie_consent", value, 180); document.getElementById("cookieBanner").style.display = "none"; if (value === "yes") { persistUserContext(); } }
        function persistUserContext() {
            if (!hasConsent()) { return; }
            var lastSeen = Date.now().toString(); setCookie("tk_last_seen", lastSeen, 60);
            var visits = parseInt(getCookie("tk_visit_count") || "0", 10) + 1; setCookie("tk_visit_count", visits.toString(), 60);
            var userContext = "role=attendee|id=${userID}"; setCookie("tk_user_context", userContext, 30);
        }
        function switchView(view) {
            var allSection = document.getElementById("allSection"); var wishlistSection = document.getElementById("wishlistSection");
            var allBtn = document.getElementById("allViewBtn"); var wishlistBtn = document.getElementById("wishlistViewBtn");
            if (view === "wishlist") { allSection.classList.add("hidden"); wishlistSection.classList.remove("hidden"); allBtn.classList.remove("active"); wishlistBtn.classList.add("active"); }
            else { wishlistSection.classList.add("hidden"); allSection.classList.remove("hidden"); wishlistBtn.classList.remove("active"); allBtn.classList.add("active"); }
            if (hasConsent()) { setCookie("tk_dashboard_view", view, 30); }
            applyEventTools();
        }
        function initDashboardState() {
            hydrateTypeFilter();
            initAdSlider();
            initAjaxActions();
            ensureWishlistEmptyState();
            document.querySelectorAll('.event-card').forEach(function (card) {
                applyCardStockState(card,
                        card.getAttribute('data-total-tickets'),
                        card.getAttribute('data-sold-tickets'));
            });
            var consent = getCookie("tk_cookie_consent");
            if (!consent) { document.getElementById("cookieBanner").style.display = "block"; }
            if (consent === "yes") { persistUserContext(); var savedView = getCookie("tk_dashboard_view"); if (savedView === "wishlist") { switchView("wishlist"); } }
            applyEventTools();
            pollLiveStock();
            setInterval(pollLiveStock, 12000);
        }
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeEventPreview();
            }
        });
        initDashboardState();
    </script>
    <script src="${pageContext.request.contextPath}/assets/error-popup.js"></script>
    <script src="${pageContext.request.contextPath}/assets/cookie-consent.js"></script>
        <script src="${pageContext.request.contextPath}/assets/pwa-register.js"></script>
</body>
</html>
