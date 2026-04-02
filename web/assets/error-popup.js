(function () {
    var STORAGE_KEY_PREFIX = "tk_scroll_pos:";

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
        WishlistFailed: "Wishlist update failed. Please try again."
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
