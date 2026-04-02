(function () {
    var CONSENT_KEY = "tk_cookie_consent";
    var PROFILE_KEY = "tk_cookie_profile";

    function getCookie(name) {
        var prefix = name + "=";
        var parts = (document.cookie || "").split(";");
        for (var i = 0; i < parts.length; i++) {
            var item = parts[i].trim();
            if (item.indexOf(prefix) === 0) {
                return decodeURIComponent(item.substring(prefix.length));
            }
        }
        return "";
    }

    function setCookie(name, value, days) {
        var expires = "";
        if (typeof days === "number") {
            var date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = "; expires=" + date.toUTCString();
        }
        var path = "; path=/";
        var secure = window.location.protocol === "https:" ? "; Secure" : "";
        document.cookie = name + "=" + encodeURIComponent(value) + expires + path + "; SameSite=Lax" + secure;
    }

    function saveProfileData(consentValue) {
        if (consentValue !== "yes") {
            return;
        }

        var body = document.body;
        var existing = {};
        try {
            existing = JSON.parse(localStorage.getItem(PROFILE_KEY) || "{}");
        } catch (e) {
            existing = {};
        }

        existing.updatedAt = new Date().toISOString();
        existing.deviceSystem = navigator.platform || "";
        existing.deviceAgent = navigator.userAgent || "";
        existing.language = navigator.language || "";
        existing.timezone = (Intl.DateTimeFormat && Intl.DateTimeFormat().resolvedOptions)
            ? (Intl.DateTimeFormat().resolvedOptions().timeZone || "")
            : "";
        existing.userName = body && body.getAttribute("data-user-name") ? body.getAttribute("data-user-name") : existing.userName || "";
        existing.lastPath = window.location.pathname || "";
        existing.interest = body && body.getAttribute("data-interest") ? body.getAttribute("data-interest") : (existing.interest || "General Events");

        localStorage.setItem(PROFILE_KEY, JSON.stringify(existing));
        setCookie("tk_last_path", existing.lastPath, 30);
        setCookie("tk_interest", existing.interest, 30);
        if (existing.userName) {
            setCookie("tk_user_name", existing.userName, 30);
        }

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function (pos) {
                var next = existing;
                next.location = {
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                    accuracy: pos.coords.accuracy
                };
                localStorage.setItem(PROFILE_KEY, JSON.stringify(next));
            }, function () {
                // User denied location or unavailable; keep other data only.
            }, {enableHighAccuracy: false, timeout: 4000, maximumAge: 86400000});
        }
    }

    function createBanner() {
        var banner = document.createElement("div");
        banner.id = "tk-cookie-banner";
        banner.style.position = "fixed";
        banner.style.left = "16px";
        banner.style.right = "16px";
        banner.style.bottom = "16px";
        banner.style.zIndex = "9999";
        banner.style.background = "#ffffff";
        banner.style.border = "1px solid #d8e0d2";
        banner.style.borderRadius = "12px";
        banner.style.boxShadow = "0 8px 22px rgba(33,47,32,.16)";
        banner.style.padding = "12px";
        banner.style.fontFamily = '"Trebuchet MS","Segoe UI",sans-serif';
        banner.innerHTML = ""
            + "<div style='font-weight:800;color:#223028;margin-bottom:6px;'>Important Notice</div>"
            + "<div style='color:#4f5c53;font-size:.92rem;line-height:1.35;margin-bottom:8px;'>"
            + "We use cookies to keep your session secure, save your preferences, and improve recommendations. "
            + "If you accept, Tickify can autosave your profile name, interests, and device/location details on this device."
            + "</div>"
            + "<div style='display:flex;gap:8px;flex-wrap:wrap;'>"
            + "<button id='tk-cookie-accept' style='border:none;border-radius:10px;background:#7fc342;color:#fff;padding:9px 12px;font-weight:800;cursor:pointer;'>Accept and Continue</button>"
            + "<button id='tk-cookie-decline' style='border:1px solid #d8e0d2;border-radius:10px;background:#fff;color:#304132;padding:9px 12px;font-weight:800;cursor:pointer;'>Decline</button>"
            + "</div>";
        document.body.appendChild(banner);

        var acceptBtn = document.getElementById("tk-cookie-accept");
        var declineBtn = document.getElementById("tk-cookie-decline");

        acceptBtn.addEventListener("click", function () {
            setCookie(CONSENT_KEY, "yes", 365);
            saveProfileData("yes");
            banner.remove();
        });

        declineBtn.addEventListener("click", function () {
            setCookie(CONSENT_KEY, "no", 365);
            banner.remove();
        });
    }

    function initConsent() {
        var consent = getCookie(CONSENT_KEY);
        if (!consent) {
            createBanner();
            return;
        }
        if (consent === "yes") {
            saveProfileData("yes");
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initConsent);
    } else {
        initConsent();
    }
})();
