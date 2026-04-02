(function () {
    var STORAGE_KEY_PREFIX = "tk_scroll_pos:";
    var HEARTBEAT_PATH = "/SessionHeartbeat.do";
    var HEARTBEAT_INTERVAL_MS = 4 * 60 * 1000;

    function pageKey() {
        return STORAGE_KEY_PREFIX + window.location.pathname + window.location.search;
    }

    function saveScrollPosition() {
        try {
            sessionStorage.setItem(pageKey(), String(window.scrollY || window.pageYOffset || 0));
        } catch (e) {
            // Ignore storage errors (privacy mode, quota, etc.)
        }
    }

    function restoreScrollPosition() {
        var raw;
        try {
            raw = sessionStorage.getItem(pageKey());
        } catch (e) {
            raw = null;
        }
        if (raw === null || raw === undefined) {
            return;
        }

        var y = parseInt(raw, 10);
        if (isNaN(y) || y < 0) {
            return;
        }

        // Prevent jump-to-top on refresh/back navigation.
        window.requestAnimationFrame(function () {
            window.scrollTo({ top: y, left: 0, behavior: "auto" });
        });
        setTimeout(function () {
            window.scrollTo({ top: y, left: 0, behavior: "auto" });
        }, 80);
    }

    if ("scrollRestoration" in history) {
        history.scrollRestoration = "manual";
    }

    // Keep manual scrolling smooth across pages.
    document.documentElement.style.scrollBehavior = "smooth";

    var saveScheduled = false;
    window.addEventListener("scroll", function () {
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        window.requestAnimationFrame(function () {
            saveScheduled = false;
            saveScrollPosition();
        });
    }, { passive: true });

    window.addEventListener("beforeunload", saveScrollPosition);
    window.addEventListener("pagehide", saveScrollPosition);
    window.addEventListener("pageshow", restoreScrollPosition);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", restoreScrollPosition);
    } else {
        restoreScrollPosition();
    }

    function resolveContextPath() {
        var scripts = document.getElementsByTagName("script");
        for (var i = 0; i < scripts.length; i++) {
            var src = scripts[i].getAttribute("src") || "";
            var marker = "/assets/error-popup.js";
            var idx = src.indexOf(marker);
            if (idx > -1) {
                return src.substring(0, idx);
            }
        }

        var path = window.location.pathname || "";
        var parts = path.split("/").filter(Boolean);
        return parts.length > 0 ? "/" + parts[0] : "";
    }

    function isAuthenticatedArea() {
        var p = window.location.pathname || "";
        return p.indexOf("/Admin/") !== -1
            || p.indexOf("/Attendee/") !== -1
            || p.indexOf("/EventManager/") !== -1
            || p.indexOf("/Presenter/") !== -1
            || p.indexOf("/VenueGuard/") !== -1
            || p.indexOf("/AdminDashboard.do") !== -1
            || p.indexOf("/AttendeeDashboardServlet.do") !== -1
            || p.indexOf("/EventManagerDashboard.do") !== -1
            || p.indexOf("/TertiaryPresenterDashboard.do") !== -1;
    }

    function startSessionHeartbeat() {
        if (!isAuthenticatedArea()) {
            return;
        }

        var contextPath = resolveContextPath();
        var endpoint = contextPath + HEARTBEAT_PATH;
        var lastActivityAt = Date.now();

        function markActive() {
            lastActivityAt = Date.now();
        }

        ["mousemove", "keydown", "click", "scroll", "touchstart"].forEach(function (eventName) {
            window.addEventListener(eventName, markActive, { passive: true });
        });

        setInterval(function () {
            // Only keep alive when user has been active in the last interval window.
            if (Date.now() - lastActivityAt > HEARTBEAT_INTERVAL_MS) {
                return;
            }

            fetch(endpoint, {
                method: "GET",
                credentials: "same-origin",
                cache: "no-store"
            }).catch(function () {
                // Ignore heartbeat transport errors; normal navigation will handle session state.
            });
        }, HEARTBEAT_INTERVAL_MS);
    }

    startSessionHeartbeat();

    var params = new URLSearchParams(window.location.search || "");
    if (params.get("suppressErrorPopup") === "1") {
        return;
    }

    var err = params.get("err");
    if (!err) {
        return;
    }

    var messages = {
        SessionExpired: "Your session expired. Please sign in again.",
        AccessDenied: "Access denied for your current role.",
        MissingFields: "Please complete all required fields.",
        OperationFailed: "Operation failed. Please try again.",
        UnknownAction: "Unknown action requested.",
        RootAuthFailed: "Root password is incorrect.",
        RootPasswordMismatch: "New root password and confirmation do not match.",
        InvalidEvent: "Selected event is invalid.",
        AgeRestricted: "Your account is under 18 and cannot purchase tickets for this event type.",
        CartUpdateFailed: "Unable to update cart. Please try again.",
        CartEmpty: "Your cart is empty.",
        CheckoutFailed: "Checkout failed. Please try again.",
        TermsRequired: "You must accept the no-refund terms before continuing.",
        PaymentFailed: "Payment failed. Please try again.",
        WishlistFailed: "Wishlist update failed. Please try again.",
        CsrfExpired: "Security token expired. Please submit the form again."
    };

    var text = messages[err] || ("Error: " + err.replace(/_/g, " "));
    window.alert(text);

    if (err === "SessionExpired" || err === "AccessDenied") {
        var loginUrl = (window.location.pathname.indexOf("/Tickify-SWP-Web-App/") !== -1)
            ? "/Tickify-SWP-Web-App/Login.jsp"
            : "Login.jsp";
        window.location.href = loginUrl;
    }
})();
