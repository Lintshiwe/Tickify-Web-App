package za.ac.tut.notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    public void sendLoginTwoFactorCodeEmail(String toEmail, String code) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("2FA code is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;background:#f6faf4;color:#1f2c22;padding:20px;'>"
                + "<div style='max-width:520px;margin:0 auto;background:#fff;border:1px solid #dce7d8;border-radius:12px;padding:18px;'>"
                + "<h2 style='margin:0 0 8px;color:#2a5634;'>Tickify Login Verification</h2>"
                + "<p style='margin:0 0 12px;'>Use this one-time code to complete your sign in:</p>"
                + "<div style='font-size:28px;font-weight:800;letter-spacing:4px;background:#eef8e9;border:1px solid #cde2c7;border-radius:10px;padding:12px;text-align:center;'>"
                + escapeHtml(code)
                + "</div>"
                + "<p style='margin:12px 0 0;color:#5d7263;'>This code expires in 10 minutes.</p>"
                + "</div></body></html>";

        String text = "Tickify Login Verification\n\n"
                + "Your one-time verification code is: " + code + "\n"
                + "This code expires in 10 minutes.";

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Security"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Tickify Login 2FA Code", "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, "Tickify Login 2FA Code", text, html,
                        defaultInlineLogoSvg(), null, host, port, user, pass, from, "Tickify_Security", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationLink) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (verificationLink == null || verificationLink.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification link is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String logoUrl = optional("TICKIFY_LOGO_URL", "tickify.logo.url", "https://tickify.example/assets/tickify-logo.svg");
        String inlineLogoSvg = defaultInlineLogoSvg();
        String html = buildEmailVerificationHtml(logoUrl, verificationLink, inlineLogoSvg != null && !inlineLogoSvg.trim().isEmpty());

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Verify"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Verify Your Tickify Email", "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, "Verify Your Tickify Email", buildPlainTextVerificationBody(verificationLink), html,
                        inlineLogoSvg, null, host, port, user, pass, from, "Tickify_Verify", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (resetLink == null || resetLink.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset link is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String logoUrl = optional("TICKIFY_LOGO_URL", "tickify.logo.url", "https://tickify.example/assets/tickify-logo.svg");
        String inlineLogoSvg = defaultInlineLogoSvg();
        String html = buildProfessionalResetHtml(logoUrl, resetLink, inlineLogoSvg != null && !inlineLogoSvg.trim().isEmpty());

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Reset"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Tickify Password Reset Request", "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (Throwable primaryFailure) {
            // Work around JVM TLS classpath conflicts by relaying via python3 smtplib.
            try {
                sendViaPythonSmtp(toEmail, "Tickify Password Reset Request", buildPlainTextResetBody(resetLink), html,
                        inlineLogoSvg, null, host, port, user, pass, from, "Tickify_Reset", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendTicketPurchaseEmail(String toEmail, String attendeeName, String transactionRef,
            List<Map<String, Object>> tickets, String myTicketsLink) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalArgumentException("At least one ticket is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String logoUrl = optional("TICKIFY_LOGO_URL", "tickify.logo.url", "https://tickify.example/assets/tickify-logo.svg");
        String inlineLogoSvg = defaultInlineLogoSvg();
        String html = buildTicketPurchaseHtml(logoUrl, inlineLogoSvg != null && !inlineLogoSvg.trim().isEmpty(), attendeeName,
                transactionRef, tickets, myTicketsLink);
        String text = buildTicketPurchaseText(attendeeName, transactionRef, tickets, myTicketsLink);
        List<MailAttachment> attachments = buildTicketPdfAttachments(tickets);

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Confirmation"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your Tickify Ticket Purchase", "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, "Your Tickify Ticket Purchase", text, html, inlineLogoSvg,
                        attachments, host, port, user, pass, from, "Tickify_Confirmation", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendEventCampaignEmail(String toEmail, String attendeeName, String subject,
            Map<String, Object> event, List<Map<String, Object>> adverts,
            String unsubscribeLink, String couponCode, Integer couponPercent) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (event == null || event.isEmpty()) {
            throw new IllegalArgumentException("Event payload is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String resolvedSubject = (subject == null || subject.trim().isEmpty())
                ? "Tickify Event Update"
                : subject.trim();
        String html = buildEventCampaignHtml(attendeeName, event, adverts, unsubscribeLink, couponCode, couponPercent);
        String text = buildEventCampaignText(attendeeName, event, unsubscribeLink, couponCode, couponPercent);

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Campaigns"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(resolvedSubject, "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, resolvedSubject, text, html,
                        defaultInlineLogoSvg(), null, host, port, user, pass, from, "Tickify_Campaigns", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendWishlistLowStockEmail(String toEmail, String attendeeName, String eventName,
            String venueName, Object eventDate, int remainingTickets, String actionLink) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String subject = "Low stock alert: " + safe(eventName);
        String html = buildWishlistLowStockHtml(attendeeName, eventName, venueName, eventDate, remainingTickets, actionLink);
        String text = "Tickify wishlist low stock alert\n\n"
                + "Event: " + safe(eventName) + "\n"
                + "Venue: " + safe(venueName) + "\n"
                + "Date: " + formatDate(eventDate) + "\n"
                + "Remaining tickets: " + remainingTickets + "\n"
                + "Buy now: " + safe(actionLink);

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Alerts"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, subject, text, html,
                        defaultInlineLogoSvg(), null, host, port, user, pass, from, "Tickify_Alerts", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public void sendRoleAssignmentEmail(String toEmail, String firstName, String assignedRole) throws Exception {
        sendRoleAssignmentEmail(toEmail, firstName, assignedRole, null, null);
    }

    public void sendRoleAssignmentEmail(String toEmail, String firstName, String assignedRole,
            String temporaryPassword, String resetLink) throws Exception {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }

        String host = required("TICKIFY_SMTP_HOST", "tickify.smtp.host");
        String user = required("TICKIFY_SMTP_USER", "tickify.smtp.user");
        String pass = required("TICKIFY_SMTP_PASSWORD", "tickify.smtp.password");
        String from = required("TICKIFY_SMTP_FROM", "tickify.smtp.from");
        String port = optional("TICKIFY_SMTP_PORT", "tickify.smtp.port", "587");
        boolean tls = Boolean.parseBoolean(optional("TICKIFY_SMTP_STARTTLS", "tickify.smtp.starttls", "true"));
        boolean ssl = Boolean.parseBoolean(optional("TICKIFY_SMTP_SSL", "tickify.smtp.ssl", "false"));

        String roleLabel = formatAssignedRoleLabel(assignedRole);
        String recipientName = (firstName == null || firstName.trim().isEmpty()) ? "there" : firstName.trim();
        String subject = "Tickify role assignment: " + roleLabel;
        String tempPasswordText = (temporaryPassword == null || temporaryPassword.trim().isEmpty())
            ? ""
            : temporaryPassword.trim();
        String resetUrl = (resetLink == null || resetLink.trim().isEmpty())
            ? ""
            : resetLink.trim();
        String resetLinkBlock = resetUrl.isEmpty()
            ? ""
            : "<p style='margin:14px 0 0;'><a href='" + escapeHtml(resetUrl)
                + "' style='display:inline-block;padding:12px 20px;border-radius:999px;background:#7ec949;color:#153121;text-decoration:none;font-weight:800;letter-spacing:.02em;'>Reset Password Now</a></p>";
        String passwordBlock = tempPasswordText.isEmpty()
            ? ""
            : "<div style='margin-top:14px;background:#f8fbf6;border:1px solid #d7e4d2;border-radius:12px;padding:14px;'>"
                + "<div style='font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#5d7263;'>Temporary password</div>"
                + "<div style='margin-top:4px;font-size:24px;font-weight:800;color:#1b3c2a;letter-spacing:.04em;'>" + escapeHtml(tempPasswordText) + "</div>"
                + "</div>";
        String resetFallbackBlock = resetUrl.isEmpty()
            ? "<div style='margin-top:12px;padding:12px;border-radius:10px;background:#fff6eb;border:1px solid #f2d6b7;color:#7d5221;'>Reset link is currently unavailable. Please use the password reset page in Tickify to set your new password.</div>"
            : "";
        String resetUrlLine = resetUrl.isEmpty() ? "" : "\nReset link: " + resetUrl;
        String tempPasswordLine = tempPasswordText.isEmpty() ? "" : "\nTemporary password: " + tempPasswordText;

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'></head>"
                + "<body style='margin:0;padding:18px;background:#eef5ea;font-family:Segoe UI,Arial,sans-serif;color:#1f2c22;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;width:100%;background:#fff;border:1px solid #d9e7d2;border-radius:16px;overflow:hidden;'>"
                + "<tr><td style='padding:18px 22px;background:linear-gradient(130deg,#113321,#2f6a41,#8ad456);color:#f2ffef;'>"
                + "<div style='font-size:12px;letter-spacing:.12em;text-transform:uppercase;opacity:.95;'>Tickify Alerts</div>"
                + "<h1 style='margin:8px 0 0;font-size:28px;line-height:1.2;'>Main Admin Account Setup</h1>"
                + "</td></tr>"
                + "<tr><td style='padding:20px 22px;'>"
                + "<p style='margin:0 0 10px;font-size:16px;'>Hi " + escapeHtml(recipientName) + ",</p>"
                + "<p style='margin:0 0 14px;color:#415546;'>Your account has been created by an administrator and is now ready.</p>"
                + "<div style='background:#eef8e9;border:1px solid #cde2c7;border-radius:12px;padding:14px;'>"
                + "<div style='font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#5d7263;'>Assigned role</div>"
                + "<div style='margin-top:4px;font-size:22px;font-weight:800;color:#1d4a31;'>" + escapeHtml(roleLabel) + "</div>"
                + "<div style='margin-top:6px;font-size:14px;color:#486152;'>Account email: <strong>" + escapeHtml(toEmail) + "</strong></div>"
                + "</div>"
                + passwordBlock
                + "<div style='margin-top:14px;padding:12px;border-radius:10px;background:#f3f8ff;border:1px solid #cfe0f7;color:#28476d;'>For security, reset this temporary password immediately and choose your own password.</div>"
                + resetLinkBlock
                + resetFallbackBlock
                + "<p style='margin:16px 0 0;color:#5d7263;font-size:13px;'>If you did not expect this email, contact Tickify support immediately.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";

        String text = "Tickify role assignment\n\n"
                + "Hi " + recipientName + ",\n"
                + "Your account has been created by an administrator.\n"
                + "Assigned role: " + roleLabel + "\n\n"
            + (tempPasswordLine.isEmpty() ? "" : tempPasswordLine + "\n")
            + "For security, please reset this temporary password to one of your choice immediately."
            + (resetUrlLine.isEmpty() ? "" : "\n" + resetUrlLine)
            + "\n\n"
                + "If you did not expect this email, please contact Tickify support immediately.";

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(brandedFromAddress(from, "Tickify_Alerts"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Throwable primaryFailure) {
            try {
                sendViaPythonSmtp(toEmail, subject, text, html,
                        defaultInlineLogoSvg(), null, host, port, user, pass, from, "Tickify_Alerts", ssl, tls);
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    private String formatAssignedRoleLabel(String assignedRole) {
        if (assignedRole == null || assignedRole.trim().isEmpty()) {
            return "Assigned Role";
        }
        String normalized = assignedRole.trim().toUpperCase(Locale.ENGLISH);
        if ("ADMIN".equals(normalized)) {
            return "Administrator";
        }
        if ("VENUE_GUARD".equals(normalized)) {
            return "Venue Guard";
        }
        if ("EVENT_MANAGER".equals(normalized)) {
            return "Event Manager";
        }
        if ("TERTIARY_PRESENTER".equals(normalized)) {
            return "Tertiary Presenter";
        }
        return assignedRole.trim();
    }

    private String buildEventCampaignText(String attendeeName, Map<String, Object> event,
            String unsubscribeLink, String couponCode, Integer couponPercent) {
        StringBuilder body = new StringBuilder();
        body.append("Tickify Event Update\n\n");
        body.append("Hi ").append(attendeeName == null || attendeeName.trim().isEmpty() ? "there" : attendeeName.trim()).append(",\n");
        body.append("A new event just dropped: ").append(safe(event.get("name"))).append("\n");
        body.append("Type: ").append(safe(event.get("type"))).append("\n");
        body.append("Date: ").append(formatDate(event.get("date"))).append("\n");
        body.append("Venue: ").append(safe(event.get("venueName"))).append("\n");
        String infoUrl = safe(event.get("infoUrl"));
        if (!infoUrl.isEmpty()) {
            body.append("More details: ").append(infoUrl).append("\n");
        }
        if (couponCode != null && !couponCode.trim().isEmpty() && couponPercent != null && couponPercent.intValue() > 0) {
            body.append("\nYour active coupon: ").append(couponCode.trim())
                    .append(" (").append(couponPercent.intValue()).append("% off)\n");
        }
        if (unsubscribeLink != null && !unsubscribeLink.trim().isEmpty()) {
            body.append("\nUnsubscribe: ").append(unsubscribeLink.trim()).append("\n");
        }
        return body.toString();
    }

    private String buildEventCampaignHtml(String attendeeName, Map<String, Object> event,
            List<Map<String, Object>> adverts, String unsubscribeLink,
            String couponCode, Integer couponPercent) {
        String eventImage = toDataImageSrc(event.get("imageMimeType"), event.get("imageData"));
        String eventImageBlock = eventImage.isEmpty()
                ? "<div style='height:220px;border-radius:14px;background:linear-gradient(120deg,#112417,#2c5e38,#7ec949);display:flex;align-items:center;justify-content:center;color:#f4fff3;font-size:28px;font-weight:700;'>"
                        + escapeHtml(safe(event.get("name"))) + "</div>"
                : "<img src='" + eventImage + "' alt='Event cover' style='width:100%;height:220px;object-fit:cover;border-radius:14px;'/>";

        StringBuilder advertBlocks = new StringBuilder();
        if (adverts != null) {
            for (Map<String, Object> advert : adverts) {
                String adImg = toDataImageSrc(advert.get("imageMimeType"), advert.get("imageData"));
                String media = adImg.isEmpty()
                        ? "<div style='width:100%;height:132px;border-radius:12px;background:linear-gradient(130deg,#1f2d1f,#365437);'></div>"
                        : "<img src='" + adImg + "' alt='Advert' style='width:100%;height:132px;object-fit:cover;border-radius:12px;'/>";
                advertBlocks.append("<div style='border:1px solid #d5e2d0;background:#fff;border-radius:12px;padding:10px;flex:1;min-width:220px;'>")
                        .append(media)
                        .append("<h4 style='margin:10px 0 4px;font-size:15px;color:#21442d;'>").append(escapeHtml(safe(advert.get("title")))).append("</h4>")
                        .append("<p style='margin:0;color:#486353;font-size:12px;'>").append(escapeHtml(safe(advert.get("organizationName")))).append("</p>")
                        .append("</div>");
            }
        }

        String greeting = attendeeName == null || attendeeName.trim().isEmpty()
                ? "Hi there"
                : "Hi " + escapeHtml(attendeeName.trim());
        String couponBlock = "";
        if (couponCode != null && !couponCode.trim().isEmpty() && couponPercent != null && couponPercent.intValue() > 0) {
            couponBlock = "<div style='margin-top:14px;padding:12px 14px;border-radius:12px;background:#eef9e8;border:1px dashed #84c763;'>"
                    + "<div style='font-size:12px;color:#356244;text-transform:uppercase;letter-spacing:.08em;'>Reward Coupon</div>"
                    + "<div style='font-size:21px;font-weight:800;letter-spacing:.06em;color:#1f4d2d;'>" + escapeHtml(couponCode.trim()) + "</div>"
                    + "<div style='font-size:13px;color:#395845;'>Save " + couponPercent.intValue() + "% on your next purchase.</div>"
                    + "</div>";
        }

        String unsubscribe = "";
        if (unsubscribeLink != null && !unsubscribeLink.trim().isEmpty()) {
            unsubscribe = "<p style='margin:18px 0 0;font-size:12px;color:#607867;'>Prefer fewer updates? <a href='"
                    + escapeHtml(unsubscribeLink.trim())
                    + "' style='color:#2f6d43;'>Unsubscribe here</a>.</p>";
        }

        String infoUrl = safe(event.get("infoUrl"));
        String cta = infoUrl.isEmpty()
                ? ""
                : "<p style='margin:16px 0 0;'><a href='" + escapeHtml(infoUrl) + "' style='display:inline-block;padding:11px 18px;border-radius:10px;background:#7ec949;color:#17311f;text-decoration:none;font-weight:700;'>Open Event Page</a></p>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>@keyframes pulseHero{0%{transform:scale(1);}50%{transform:scale(1.015);}100%{transform:scale(1);}}"
                + "@keyframes shine{0%{background-position:-180px 0;}100%{background-position:380px 0;}}</style></head>"
                + "<body style='margin:0;background:#edf4ea;font-family:Segoe UI,Arial,sans-serif;color:#1f3124;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='padding:22px 10px;background:#edf4ea;'><tr><td align='center'>"
                + "<table role='presentation' width='700' cellspacing='0' cellpadding='0' style='max-width:700px;width:100%;background:#ffffff;border:1px solid #d8e4d4;border-radius:18px;overflow:hidden;'>"
                + "<tr><td style='padding:16px 18px;background:linear-gradient(130deg,#0f2a1b,#2f5a39,#88cf5f);color:#f3fff0;'>"
                + "<div style='font-size:11px;letter-spacing:.14em;text-transform:uppercase;opacity:.9;'>Tickify Event Bulletin</div>"
                + "<h1 style='margin:6px 0 0;font-size:27px;'>" + greeting + ", your next event is live</h1>"
                + "</td></tr>"
                + "<tr><td style='padding:18px;'>"
                + "<div style='animation:pulseHero 4s ease-in-out infinite;'>" + eventImageBlock + "</div>"
                + "<h2 style='margin:14px 0 4px;font-size:24px;color:#1d3f2a;'>" + escapeHtml(safe(event.get("name"))) + "</h2>"
                + "<p style='margin:0;color:#4a6555;font-size:14px;'>" + escapeHtml(safe(event.get("type"))) + " | "
                + escapeHtml(formatDate(event.get("date"))) + " | " + escapeHtml(safe(event.get("venueName"))) + "</p>"
                + "<p style='margin:12px 0 0;color:#2d4636;line-height:1.6;'>" + escapeHtml(safe(event.get("description"))) + "</p>"
                + cta
                + couponBlock
                + "<div style='margin-top:16px;padding:1px;border-radius:14px;background:linear-gradient(90deg,#8fd061,#d6f3bf,#8fd061);background-size:300% 100%;animation:shine 3.8s linear infinite;'>"
                + "<div style='background:#f8fff4;border-radius:13px;padding:12px;'>"
                + "<h3 style='margin:0 0 10px;color:#285137;'>Featured Adverts</h3>"
                + "<div style='display:flex;gap:10px;flex-wrap:wrap;'>" + advertBlocks + "</div>"
                + "</div></div>"
                + unsubscribe
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildWishlistLowStockHtml(String attendeeName, String eventName,
            String venueName, Object eventDate, int remainingTickets, String actionLink) {
        String greeting = attendeeName == null || attendeeName.trim().isEmpty()
                ? "Hi there"
                : "Hi " + escapeHtml(attendeeName.trim());
        String cta = actionLink == null || actionLink.trim().isEmpty()
                ? ""
                : "<a href='" + escapeHtml(actionLink.trim()) + "' style='display:inline-block;margin-top:14px;padding:11px 16px;border-radius:10px;background:#79c84a;color:#193322;text-decoration:none;font-weight:700;'>Go to Wishlist</a>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='margin:0;padding:0;background:#f4f8f3;font-family:Segoe UI,Arial,sans-serif;color:#203126;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='padding:24px 12px;background:#f4f8f3;'><tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;width:100%;background:#fff;border:1px solid #dae5d5;border-radius:14px;overflow:hidden;'>"
                + "<tr><td style='padding:20px;background:linear-gradient(130deg,#f1fbeb,#ffffff);'>"
                + "<div style='font-size:12px;text-transform:uppercase;letter-spacing:.08em;color:#3d6550;'>Wishlist Alert</div>"
                + "<h2 style='margin:8px 0 6px;color:#275136;'>" + greeting + "</h2>"
                + "<p style='margin:0;color:#3f5849;'>Your wishlisted event is running low on stock.</p>"
                + "</td></tr>"
                + "<tr><td style='padding:18px 20px;'>"
                + "<div style='font-size:19px;font-weight:700;color:#1f442d;'>" + escapeHtml(safe(eventName)) + "</div>"
                + "<p style='margin:8px 0;color:#4c6557;'>" + escapeHtml(safe(venueName)) + " | " + escapeHtml(formatDate(eventDate)) + "</p>"
                + "<div style='display:inline-block;padding:8px 12px;border-radius:999px;background:#fff2e5;color:#8f4d09;border:1px solid #f5d3b6;font-weight:700;'>"
                + remainingTickets + " ticket(s) left</div>"
                + cta
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private String toDataImageSrc(Object mimeType, Object data) {
        if (!(data instanceof byte[])) {
            return "";
        }
        byte[] bytes = (byte[]) data;
        if (bytes.length == 0) {
            return "";
        }
        String mime = safe(mimeType);
        if (mime.isEmpty()) {
            mime = "image/png";
        }
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mime + ";base64," + encoded;
    }

    private String buildProfessionalResetHtml(String logoUrl, String resetLink, boolean useInlineCidLogo) {
        String logoBlock = renderLogoBlock(logoUrl, useInlineCidLogo);
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background:#f4f8f3;font-family:Segoe UI,Arial,sans-serif;color:#243228;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#f4f8f3;padding:28px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;width:100%;background:#ffffff;border:1px solid #dce7d8;border-radius:16px;overflow:hidden;'>"
                + "<tr><td style='background:linear-gradient(135deg,#e6f4dc,#ffffff);padding:24px 24px 10px 24px;text-align:center;'>"
            + logoBlock
                + "<h1 style='margin:14px 0 6px;font-size:22px;line-height:1.3;color:#24412b;'>Password Reset Requested</h1>"
                + "<p style='margin:0;color:#4d6355;font-size:15px;line-height:1.5;'>A secure reset link was generated for your Tickify account.</p>"
                + "</td></tr>"
                + "<tr><td style='padding:22px 24px 20px 24px;'>"
                + "<p style='margin:0 0 14px;color:#304537;font-size:15px;line-height:1.6;'>If you requested this change, click the button below. This link expires in <strong>20 minutes</strong>.</p>"
                + "<p style='text-align:center;margin:18px 0 22px;'>"
                + "<a href='" + escapeHtml(resetLink) + "' style='display:inline-block;background:#79c84a;color:#ffffff;text-decoration:none;font-weight:700;padding:12px 22px;border-radius:10px;'>Reset Password</a>"
                + "</p>"
                + "<p style='margin:0;color:#4d6355;font-size:13px;line-height:1.6;'>If the button does not work, copy and paste this link into your browser:</p>"
                + "<p style='margin:8px 0 0;word-break:break-all;color:#2a5b37;font-size:12px;line-height:1.5;'>" + escapeHtml(resetLink) + "</p>"
                + "</td></tr>"
                + "<tr><td style='background:#f6faf4;padding:14px 24px;color:#5d7263;font-size:12px;line-height:1.6;border-top:1px solid #e3ece0;'>"
                + "If you did not request this reset, you can ignore this email. Your password remains unchanged."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String buildEmailVerificationHtml(String logoUrl, String verifyLink, boolean useInlineCidLogo) {
        String logoBlock = renderLogoBlock(logoUrl, useInlineCidLogo);
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background:#f4f8f3;font-family:Segoe UI,Arial,sans-serif;color:#243228;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#f4f8f3;padding:28px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='620' cellspacing='0' cellpadding='0' style='max-width:620px;width:100%;background:#ffffff;border:1px solid #dce7d8;border-radius:16px;overflow:hidden;'>"
                + "<tr><td style='background:linear-gradient(135deg,#e6f4dc,#ffffff);padding:24px 24px 10px 24px;text-align:center;'>"
                + logoBlock
                + "<h1 style='margin:14px 0 6px;font-size:22px;line-height:1.3;color:#24412b;'>Verify Your Email</h1>"
                + "<p style='margin:0;color:#4d6355;font-size:15px;line-height:1.5;'>Activate your Tickify account before first login.</p>"
                + "</td></tr>"
                + "<tr><td style='padding:22px 24px 20px 24px;'>"
                + "<p style='margin:0 0 14px;color:#304537;font-size:15px;line-height:1.6;'>Click below to confirm your email address. This link expires in <strong>24 hours</strong>.</p>"
                + "<p style='text-align:center;margin:18px 0 22px;'>"
                + "<a href='" + escapeHtml(verifyLink) + "' style='display:inline-block;background:#79c84a;color:#ffffff;text-decoration:none;font-weight:700;padding:12px 22px;border-radius:10px;'>Verify Email</a>"
                + "</p>"
                + "<p style='margin:0;color:#4d6355;font-size:13px;line-height:1.6;'>If the button does not work, copy and paste this link into your browser:</p>"
                + "<p style='margin:8px 0 0;word-break:break-all;color:#2a5b37;font-size:12px;line-height:1.5;'>" + escapeHtml(verifyLink) + "</p>"
                + "</td></tr>"
                + "<tr><td style='background:#f6faf4;padding:14px 24px;color:#5d7263;font-size:12px;line-height:1.6;border-top:1px solid #e3ece0;'>"
                + "If you did not create a Tickify account, you can ignore this email."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String renderLogoBlock(String logoUrl, boolean useInlineCidLogo) {
        if (useInlineCidLogo) {
            return "<div style='text-align:center;'>"
                    + "<img src='cid:tickifylogo' alt='Tickify' style='height:52px;max-width:220px;width:auto;display:inline-block;'/>"
                    + "<div style='font-size:11px;color:#5d7263;margin-top:6px;'>Tickify</div>"
                    + "</div>";
        }

        if (logoUrl != null) {
            String normalized = logoUrl.trim().toLowerCase();
            if (!normalized.isEmpty() && !normalized.contains("tickify.example")) {
                return "<img src='" + escapeHtml(logoUrl) + "' alt='Tickify' style='height:52px;max-width:220px;width:auto;display:inline-block;'/>";
            }
        }

        return "<div style='display:inline-block;border:2px solid #79c84a;border-radius:12px;padding:8px 14px;background:#ffffff;'>"
                + "<span style='font-size:26px;line-height:1;font-weight:800;color:#2d5a36;letter-spacing:1px;'>TICKIFY</span>"
                + "</div>";
    }

    private String buildPlainTextResetBody(String resetLink) {
        return "Tickify Password Reset Request\n\n"
                + "A secure reset link was generated for your Tickify account.\n"
                + "This link expires in 20 minutes.\n\n"
                + "Reset Password: " + resetLink + "\n\n"
                + "If you did not request this reset, you can ignore this email. "
                + "Your password remains unchanged.";
    }

    private String buildPlainTextVerificationBody(String verifyLink) {
        return "Tickify Email Verification\n\n"
                + "Thank you for registering with Tickify.\n"
                + "Please verify your email address using the link below.\n"
                + "This link expires in 24 hours.\n\n"
                + "Verify Email: " + verifyLink + "\n\n"
                + "If you did not create this account, you can ignore this email.";
    }

    private String buildTicketPurchaseText(String attendeeName, String transactionRef,
            List<Map<String, Object>> tickets, String myTicketsLink) {
        StringBuilder body = new StringBuilder();
        body.append("Tickify Ticket Purchase Confirmation\n\n");
        body.append("Hi ").append(attendeeName == null || attendeeName.trim().isEmpty() ? "there" : attendeeName.trim()).append(",\n");
        body.append("Your payment was successful and your tickets are ready.\n");
        if (transactionRef != null && !transactionRef.trim().isEmpty()) {
            body.append("Transaction Ref: ").append(transactionRef.trim()).append("\n");
        }
        body.append("\nTickets:\n");

        for (Map<String, Object> ticket : tickets) {
            body.append("- ").append(safe(ticket.get("ticketNumber")))
                    .append(" | Event: ").append(safe(ticket.get("eventName")))
                    .append(" | Date: ").append(formatDate(ticket.get("eventDate")))
                    .append(" | Venue: ").append(safe(ticket.get("venueName")))
                    .append(" | QR: ").append(safe(ticket.get("qrCode")))
                    .append("\n");
        }

        if (myTicketsLink != null && !myTicketsLink.trim().isEmpty()) {
            body.append("\nView all tickets: ").append(myTicketsLink.trim()).append("\n");
        }
        body.append("\nThank you for booking with Tickify.");
        return body.toString();
    }

    private String buildTicketPurchaseHtml(String logoUrl, boolean useInlineCidLogo,
            String attendeeName, String transactionRef, List<Map<String, Object>> tickets, String myTicketsLink) {
        StringBuilder rows = new StringBuilder();
        for (Map<String, Object> ticket : tickets) {
            rows.append("<tr>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e7efe3;'>").append(escapeHtml(safe(ticket.get("ticketNumber")))).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e7efe3;'>").append(escapeHtml(safe(ticket.get("eventName")))).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e7efe3;'>").append(escapeHtml(formatDate(ticket.get("eventDate")))).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e7efe3;'>").append(escapeHtml(safe(ticket.get("venueName")))).append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #e7efe3;font-family:monospace;'>").append(escapeHtml(safe(ticket.get("qrCode")))).append("</td>")
                    .append("</tr>");
        }

        String logoBlock = renderLogoBlock(logoUrl, useInlineCidLogo);
        String greeting = attendeeName == null || attendeeName.trim().isEmpty() ? "Hi there," : "Hi " + escapeHtml(attendeeName.trim()) + ",";
        String tx = transactionRef == null || transactionRef.trim().isEmpty()
                ? ""
                : "<p style='margin:8px 0 0;color:#4d6355;font-size:14px;'>Transaction Ref: <strong>" + escapeHtml(transactionRef.trim()) + "</strong></p>";
        String cta = (myTicketsLink == null || myTicketsLink.trim().isEmpty())
                ? ""
                : "<p style='margin:16px 0 4px;'><a href='" + escapeHtml(myTicketsLink.trim())
                + "' style='display:inline-block;background:#79c84a;color:#fff;text-decoration:none;padding:10px 18px;border-radius:10px;font-weight:700;'>Open My Tickets</a></p>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background:#f4f8f3;font-family:Segoe UI,Arial,sans-serif;color:#243228;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#f4f8f3;padding:28px 12px;'><tr><td align='center'>"
                + "<table role='presentation' width='680' cellspacing='0' cellpadding='0' style='max-width:680px;width:100%;background:#fff;border:1px solid #dce7d8;border-radius:16px;overflow:hidden;'>"
                + "<tr><td style='background:linear-gradient(135deg,#e6f4dc,#ffffff);padding:24px 24px 10px 24px;text-align:center;'>"
                + logoBlock
                + "<h1 style='margin:14px 0 6px;font-size:22px;line-height:1.3;color:#24412b;'>Your Tickets Are Ready</h1>"
                + "<p style='margin:0;color:#4d6355;font-size:15px;line-height:1.5;'>" + greeting + " Your purchase was successful.</p>"
                + tx
                + "</td></tr>"
                + "<tr><td style='padding:18px 24px 16px 24px;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;font-size:13px;'>"
                + "<thead><tr style='background:#f0f7ec;color:#2e4f37;'>"
                + "<th align='left' style='padding:8px;border-bottom:1px solid #cfe2c9;'>Ticket</th>"
                + "<th align='left' style='padding:8px;border-bottom:1px solid #cfe2c9;'>Event</th>"
                + "<th align='left' style='padding:8px;border-bottom:1px solid #cfe2c9;'>Date</th>"
                + "<th align='left' style='padding:8px;border-bottom:1px solid #cfe2c9;'>Venue</th>"
                + "<th align='left' style='padding:8px;border-bottom:1px solid #cfe2c9;'>QR Code</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + cta
                + "<p style='margin:12px 0 0;color:#5d7263;font-size:12px;'>Please keep this email for check-in. Each ticket has a unique QR code.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private void sendViaPythonSmtp(String to, String subject, String textBody, String htmlBody, String logoSvg,
            List<MailAttachment> attachments,
            String host, String port, String user, String pass, String from, String senderName,
            boolean ssl, boolean starttls) throws Exception {
        String script = "import os, smtplib, ssl, sys\n"
                + "from email import encoders\n"
                + "from email.mime.application import MIMEApplication\n"
                + "from email.mime.base import MIMEBase\n"
                + "from email.mime.multipart import MIMEMultipart\n"
                + "from email.mime.text import MIMEText\n"
                + "to = sys.argv[1]\n"
                + "subject = sys.argv[2]\n"
                + "msg = MIMEMultipart('related')\n"
                + "alt = MIMEMultipart('alternative')\n"
                + "msg.attach(alt)\n"
                + "msg['Subject'] = subject\n"
                + "msg['From'] = os.environ['TICKIFY_PY_FROM_HEADER']\n"
                + "msg['To'] = to\n"
                + "alt.attach(MIMEText(os.environ['TICKIFY_PY_TEXT'], 'plain', 'utf-8'))\n"
                + "alt.attach(MIMEText(os.environ['TICKIFY_PY_HTML'], 'html', 'utf-8'))\n"
                + "logo_svg = os.environ.get('TICKIFY_PY_LOGO_SVG', '').strip()\n"
                + "if logo_svg:\n"
                + "    logo = MIMEBase('image', 'svg+xml')\n"
                + "    logo.set_payload(logo_svg.encode('utf-8'))\n"
                + "    encoders.encode_base64(logo)\n"
                + "    logo.add_header('Content-ID', '<tickifylogo>')\n"
                + "    logo.add_header('Content-Disposition', 'inline', filename='tickify-logo.svg')\n"
                + "    logo.add_header('Content-Type', 'image/svg+xml; name=tickify-logo.svg')\n"
                + "    msg.attach(logo)\n"
                + "attach_dir = os.environ.get('TICKIFY_PY_ATTACH_DIR', '').strip()\n"
                + "if attach_dir and os.path.isdir(attach_dir):\n"
                + "    for name in sorted(os.listdir(attach_dir)):\n"
                + "        fp = os.path.join(attach_dir, name)\n"
                + "        if not os.path.isfile(fp):\n"
                + "            continue\n"
                + "        with open(fp, 'rb') as f:\n"
                + "            part = MIMEApplication(f.read(), _subtype='pdf')\n"
                + "        part.add_header('Content-Disposition', 'attachment', filename=name)\n"
                + "        msg.attach(part)\n"
                + "host = os.environ['TICKIFY_PY_HOST']\n"
                + "port = int(os.environ['TICKIFY_PY_PORT'])\n"
                + "user = os.environ['TICKIFY_PY_USER']\n"
                + "password = os.environ['TICKIFY_PY_PASS']\n"
                + "use_ssl = os.environ['TICKIFY_PY_SSL'].lower() == 'true'\n"
                + "use_starttls = os.environ['TICKIFY_PY_STARTTLS'].lower() == 'true'\n"
                + "if use_ssl:\n"
                + "    server = smtplib.SMTP_SSL(host, port, timeout=20)\n"
                + "else:\n"
                + "    server = smtplib.SMTP(host, port, timeout=20)\n"
                + "try:\n"
                + "    server.ehlo()\n"
                + "    if use_starttls:\n"
                + "        server.starttls(context=ssl.create_default_context())\n"
                + "        server.ehlo()\n"
                + "    server.login(user, password)\n"
                + "    server.sendmail(os.environ['TICKIFY_PY_FROM_ENVELOPE'], [to], msg.as_string())\n"
                + "finally:\n"
                + "    server.quit()\n";

        Path attachDir = null;
        if (attachments != null && !attachments.isEmpty()) {
            attachDir = Files.createTempDirectory("tickify-mail-attach-");
            int index = 1;
            for (MailAttachment attachment : attachments) {
                if (attachment == null || attachment.filename == null || attachment.content == null) {
                    continue;
                }
                String safeName = sanitizeFilename(attachment.filename);
                if (!safeName.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
                    safeName = safeName + ".pdf";
                }
                String prefixed = String.format(Locale.ENGLISH, "%02d-%s", index++, safeName);
                Files.write(attachDir.resolve(prefixed), attachment.content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList("python3", "-c", script, to, subject));
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("TICKIFY_PY_HOST", host);
        env.put("TICKIFY_PY_PORT", port);
        env.put("TICKIFY_PY_USER", user);
        env.put("TICKIFY_PY_PASS", pass);
        String fromAddress = normalizeFromAddress(from);
        String fromDisplay = normalizeSenderName(senderName);
        env.put("TICKIFY_PY_FROM_HEADER", fromDisplay + " <" + fromAddress + ">");
        env.put("TICKIFY_PY_FROM_ENVELOPE", user);
        env.put("TICKIFY_PY_SSL", String.valueOf(ssl));
        env.put("TICKIFY_PY_STARTTLS", String.valueOf(starttls));
        env.put("TICKIFY_PY_TEXT", textBody);
        env.put("TICKIFY_PY_HTML", htmlBody);
        env.put("TICKIFY_PY_LOGO_SVG", logoSvg == null ? "" : logoSvg);
        if (attachDir != null) {
            env.put("TICKIFY_PY_ATTACH_DIR", attachDir.toAbsolutePath().toString());
        }

        try {
            Process p = pb.start();
            String output = readAll(p);
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("Python SMTP relay timed out.");
            }
            if (p.exitValue() != 0) {
                throw new IOException("Python SMTP relay failed: " + output);
            }
        } finally {
            deleteDirectoryQuietly(attachDir);
        }
    }

    private String readAll(Process p) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(line);
            }
        }
        return out.toString();
    }

    private String formatDate(Object dateValue) {
        if (dateValue instanceof Date) {
            return new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format((Date) dateValue);
        }
        return safe(dateValue);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<MailAttachment> buildTicketPdfAttachments(List<Map<String, Object>> tickets) {
        List<MailAttachment> attachments = new ArrayList<>();
        for (Map<String, Object> ticket : tickets) {
            List<String> lines = new ArrayList<>();
            lines.add("Tickify Ticket");
            lines.add("");
            lines.add("Ticket Number: " + safe(ticket.get("ticketNumber")));
            lines.add("Event: " + safe(ticket.get("eventName")));
            lines.add("Date: " + formatDate(ticket.get("eventDate")));
            lines.add("Venue: " + safe(ticket.get("venueName")));
            lines.add("QR Code: " + safe(ticket.get("qrCode")));
            lines.add("");
            lines.add("Status: CONFIRMED");
            lines.add("Generated by Tickify");

            byte[] pdf = buildSimplePdf(lines);
            String ticketNumber = safe(ticket.get("ticketNumber"));
            if (ticketNumber.trim().isEmpty()) {
                ticketNumber = "ticket";
            }
            String filename = "tickify-" + ticketNumber.replace(' ', '-') + ".pdf";
            attachments.add(new MailAttachment(filename, pdf));
        }
        return attachments;
    }

    private byte[] buildSimplePdf(List<String> lines) {
        List<String> contentStreams = new ArrayList<>();
        int perPage = 52;
        for (int start = 0; start < lines.size(); start += perPage) {
            int end = Math.min(start + perPage, lines.size());
            StringBuilder stream = new StringBuilder();
            stream.append("BT\n");
            stream.append("/F1 10 Tf\n");
            stream.append("14 TL\n");
            stream.append("40 800 Td\n");
            for (int i = start; i < end; i++) {
                stream.append("(").append(escapePdf(lines.get(i))).append(") Tj\n");
                if (i < end - 1) {
                    stream.append("T*\n");
                }
            }
            stream.append("\nET\n");
            contentStreams.add(stream.toString());
        }

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < contentStreams.size(); i++) {
            int pageObj = 4 + (i * 2);
            kids.append(pageObj).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [ " + kids + "] /Count " + contentStreams.size() + " >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        for (int i = 0; i < contentStreams.size(); i++) {
            int contentObj = 5 + (i * 2);
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObj + " 0 R >>");
            String stream = contentStreams.get(i);
            int length = stream.getBytes(StandardCharsets.ISO_8859_1).length;
            objects.add("<< /Length " + length + " >>\nstream\n" + stream + "endstream");
        }

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n");
            pdf.append(objects.get(i)).append("\n");
            pdf.append("endobj\n");
        }

        int xrefOffset = pdf.length();
        pdf.append("xref\n");
        pdf.append("0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            pdf.append(String.format(Locale.ENGLISH, "%010d 00000 n \n", offsets.get(i)));
        }
        pdf.append("trailer\n");
        pdf.append("<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escapePdf(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "ticket.pdf";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private InternetAddress brandedFromAddress(String configuredFrom, String senderName) throws Exception {
        return new InternetAddress(normalizeFromAddress(configuredFrom), normalizeSenderName(senderName));
    }

    private String normalizeFromAddress(String configuredFrom) {
        String fallback = "no-reply@tickify.ac.za";
        if (configuredFrom == null) {
            return fallback;
        }

        String value = configuredFrom.trim();
        if (value.isEmpty()) {
            return fallback;
        }

        int lt = value.indexOf('<');
        int gt = value.indexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt).trim();
        }

        if (!value.contains("@") || value.startsWith("@") || value.endsWith("@")) {
            return fallback;
        }

        return value;
    }

    private String normalizeSenderName(String senderName) {
        if (senderName == null) {
            return "Tickify";
        }

        String clean = senderName.trim();
        if (clean.isEmpty()) {
            return "Tickify";
        }

        return clean;
    }

    private void deleteDirectoryQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try {
            if (Files.exists(dir)) {
                Files.list(dir).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
                Files.deleteIfExists(dir);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class MailAttachment {
        private final String filename;
        private final byte[] content;

        private MailAttachment(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }
    }

    private String defaultInlineLogoSvg() {
        return "<svg viewBox='0 0 400 100' xmlns='http://www.w3.org/2000/svg' width='400' height='100'>"
                + "<defs><filter id='tagShadow' x='-10%' y='-10%' width='130%' height='130%'>"
                + "<feDropShadow dx='0' dy='2' stdDeviation='4' flood-color='#C8CDD6' flood-opacity='0.4'/></filter></defs>"
                + "<path d='M 14,22 Q 14,14 22,14 L 70,14 L 92,50 L 70,86 L 22,86 Q 14,86 14,78 Z' fill='#ECEEF2' stroke='#D8DCE4' stroke-width='1.5' filter='url(#tagShadow)'/>"
                + "<circle cx='30' cy='50' r='5.5' fill='#FFFFFF' stroke='#D8DCE4' stroke-width='1.5'/>"
                + "<line x1='50' y1='14' x2='50' y2='86' stroke='#D8DCE4' stroke-width='1' stroke-dasharray='3,3'/>"
                + "<line x1='63' y1='35' x2='82' y2='35' stroke='#A9B3BF' stroke-width='3' stroke-linecap='round'/>"
                + "<line x1='72' y1='35' x2='72' y2='63' stroke='#A9B3BF' stroke-width='3' stroke-linecap='round'/>"
                + "<text x='108' y='63' font-family='Segoe UI, Trebuchet MS, Arial, sans-serif' font-size='40' font-weight='400' letter-spacing='-2' fill='#4A5568'>Tickify</text>"
                + "<circle cx='116' cy='76' r='3' fill='#B0BAC8'/></svg>";
    }

    private String required(String envKey, String sysKey) {
        String value = optional(envKey, sysKey, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required mail setting: " + envKey + " or -D" + sysKey);
        }
        return value.trim();
    }

    private String optional(String envKey, String sysKey, String fallback) {
        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String prop = System.getProperty(sysKey);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        return fallback;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
