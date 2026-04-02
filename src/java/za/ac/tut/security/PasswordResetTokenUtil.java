package za.ac.tut.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PasswordResetTokenUtil {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String SECRET = resolveSecret();

    private PasswordResetTokenUtil() {
    }

    public static String generate(String role, int userId, long expiresAtEpochMs, String currentPasswordHash) {
        String payload = safe(role) + "|" + userId + "|" + expiresAtEpochMs;
        String signature = sign(payload + "|" + safe(currentPasswordHash));
        return b64(payload) + "." + b64(signature);
    }

    public static TokenPayload verify(String token, String currentPasswordHash) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 2) {
            return null;
        }

        String payload;
        String providedSig;
        try {
            payload = unb64(parts[0]);
            providedSig = unb64(parts[1]);
        } catch (RuntimeException ex) {
            return null;
        }

        String expectedSig = sign(payload + "|" + safe(currentPasswordHash));
        if (!MessageDigest.isEqual(providedSig.getBytes(StandardCharsets.UTF_8), expectedSig.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        String[] pieces = payload.split("\\|");
        if (pieces.length != 3) {
            return null;
        }

        try {
            String role = pieces[0];
            int userId = Integer.parseInt(pieces[1]);
            long expiresAt = Long.parseLong(pieces[2]);
            if (System.currentTimeMillis() > expiresAt) {
                return null;
            }
            return new TokenPayload(role, userId, expiresAt);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign reset token", ex);
        }
    }

    private static String b64(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String resolveSecret() {
        String env = System.getenv("TICKIFY_RESET_TOKEN_SECRET");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String property = System.getProperty("tickify.reset.token.secret");
        if (property != null && !property.trim().isEmpty()) {
            return property.trim();
        }
        return "tickify-reset-dev-secret-change-me";
    }

    public static final class TokenPayload {
        private final String role;
        private final int userId;
        private final long expiresAtEpochMs;

        public TokenPayload(String role, int userId, long expiresAtEpochMs) {
            this.role = role;
            this.userId = userId;
            this.expiresAtEpochMs = expiresAtEpochMs;
        }

        public String getRole() {
            return role;
        }

        public int getUserId() {
            return userId;
        }

        public long getExpiresAtEpochMs() {
            return expiresAtEpochMs;
        }
    }
}
