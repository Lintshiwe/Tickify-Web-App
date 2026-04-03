package za.ac.tut.servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.attendee.AttendeeService;

public class TicketDownloadServlet extends HttpServlet {

    private final AttendeeService attendeeService = new AttendeeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null || attendeeId.intValue() <= 0) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp?err=SessionExpired");
            return;
        }

        String scope = trim(request.getParameter("scope"));
        List<Map<String, Object>> tickets;
        try {
            tickets = attendeeService.repo().getAttendeeTickets(attendeeId.intValue());
        } catch (SQLException ex) {
            log("Unable to load attendee tickets for download", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate ticket download.");
            return;
        }

        if (tickets == null || tickets.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/ViewMyTickets.do?err=NoTicket");
            return;
        }

        List<Map<String, Object>> selected = new ArrayList<Map<String, Object>>();
        String filename;
        if ("single".equalsIgnoreCase(scope)) {
            int ticketId = parseInt(request.getParameter("ticketID"));
            for (Map<String, Object> ticket : tickets) {
                Object idObj = ticket.get("ticketID");
                if (idObj instanceof Number && ((Number) idObj).intValue() == ticketId) {
                    selected.add(ticket);
                    break;
                }
            }
            if (selected.isEmpty()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ticket not found for attendee.");
                return;
            }
            filename = "Tickify_Ticket_" + sanitizeFilenamePart(String.valueOf(selected.get(0).get("ticketNumber"))) + ".pdf";
        } else {
            selected.addAll(tickets);
            String attendee = String.valueOf(selected.get(0).get("attendeeName"));
            filename = "Tickify_Tickets_" + sanitizeFilenamePart(attendee) + ".pdf";
        }

        boolean flagged = isTrue(request.getParameter("flagged"));
        boolean freshRequested = isTrue(request.getParameter("fresh"));
        if (freshRequested) {
            flagged = false;
        }
        String downloadToken = freshRequested
            ? "DL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ENGLISH)
            : "";

        byte[] pdfBytes = buildTicketsPdf(selected, flagged, freshRequested, downloadToken);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
    }

    private byte[] buildTicketsPdf(List<Map<String, Object>> tickets, boolean flagged, boolean freshRequested, String downloadToken) {
        List<byte[]> images = new ArrayList<byte[]>();
        for (int i = 0; i < tickets.size(); i++) {
            images.add(toJpegBytes(renderTicketCardImage(tickets.get(i), i + 1, tickets.size(), flagged, freshRequested, downloadToken)));
        }

        List<String> objects = new ArrayList<String>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            int pageObj = 3 + (i * 3);
            kids.append(pageObj).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [ " + kids + "] /Count " + images.size() + " >>");

        for (int i = 0; i < images.size(); i++) {
            int pageObjNum = 3 + (i * 3);
            int imageObjNum = pageObjNum + 1;
            int contentObjNum = pageObjNum + 2;
            byte[] jpeg = images.get(i);

            String content = "q 900 0 0 420 0 0 cm /Im1 Do Q\n";
            int length = content.getBytes(StandardCharsets.ISO_8859_1).length;
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 900 420] /Resources << /XObject << /Im1 " + imageObjNum + " 0 R >> >> /Contents " + contentObjNum + " 0 R >>");
            objects.add("<< /Type /XObject /Subtype /Image /Width 1800 /Height 840 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpeg.length + " >>\nstream\n"
                    + new String(jpeg, StandardCharsets.ISO_8859_1) + "\nendstream");
            objects.add("<< /Length " + length + " >>\nstream\n" + content + "endstream");
        }

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<Integer>();
        offsets.add(Integer.valueOf(0));

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(Integer.valueOf(pdf.length()));
            pdf.append(i + 1).append(" 0 obj\n");
            pdf.append(objects.get(i)).append("\n");
            pdf.append("endobj\n");
        }

        int xrefOffset = pdf.length();
        pdf.append("xref\n");
        pdf.append("0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            pdf.append(String.format(Locale.ENGLISH, "%010d 00000 n \n", offsets.get(i).intValue()));
        }
        pdf.append("trailer\n");
        pdf.append("<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private BufferedImage renderTicketCardImage(Map<String, Object> ticket, int index, int total, boolean flagged, boolean freshRequested, String downloadToken) {
        int width = 1800;
        int height = 840;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(new Color(245, 249, 244));
            g.fillRect(0, 0, width, height);

            g.setColor(Color.WHITE);
            g.fillRoundRect(36, 36, 1728, 768, 28, 28);
            g.setColor(new Color(207, 223, 198));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(36, 36, 1728, 768, 28, 28);

            g.setColor(new Color(127, 195, 66));
            g.fillRoundRect(36, 36, 1728, 116, 28, 28);
            g.setColor(new Color(73, 104, 55));
            g.drawRoundRect(36, 36, 1728, 116, 28, 28);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 50));
            g.drawString("TICKIFY EVENT TICKET", 76, 108);

            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            g.drawString("Ticket " + index + " of " + total, 1450, 108);

            drawHeaderBadges(g, ticket);
            drawLogoBlock(g);

            g.setColor(new Color(102, 122, 93, 70));
            g.setFont(new Font("SansSerif", Font.BOLD, 88));
            g.drawString("TICKIFY", 640, 430);

            g.setColor(new Color(118, 145, 106));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{16f, 12f}, 0f));
            g.drawLine(1285, 188, 1285, 748);

            drawAlbum(g, ticket);

            int x = 560;
            drawText(g, "Event", truncate(safe(ticket.get("eventName")), 64), x, 232);
            drawText(g, "Type", truncate(safe(ticket.get("eventType")), 38), x, 294);
            drawText(g, "Date", truncate(formatDate(ticket.get("eventDate")), 38), x, 356);
            drawText(g, "Venue", truncate(safe(ticket.get("venueName")), 56), x, 418);
            drawText(g, "Address", truncate(safe(ticket.get("venueAddress")), 56), x, 480);
            drawText(g, "Ticket #", truncate(safe(ticket.get("ticketNumber")), 36), x, 542);
            drawText(g, "Price", "R " + truncate(safe(ticket.get("price")), 20), x, 604);
            drawText(g, "Client", truncate(safe(ticket.get("attendeeName")), 42), x, 666);
            drawText(g, "Email", truncate(safe(ticket.get("attendeeEmail")), 42), x, 728);

            drawQrPanel(g, ticket, flagged, freshRequested, downloadToken);
        } finally {
            g.dispose();
        }
        return image;
    }

    private void drawHeaderBadges(Graphics2D g, Map<String, Object> ticket) {
        String type = truncate(safe(ticket.get("eventType")), 22);
        String auth = truncate(safe(ticket.get("authToken")), 26);

        g.setColor(new Color(240, 249, 235));
        g.fillRoundRect(58, 164, 210, 44, 20, 20);
        g.setColor(new Color(112, 172, 86));
        g.drawRoundRect(58, 164, 210, 44, 20, 20);
        g.setColor(new Color(61, 145, 41));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(type.isEmpty() ? "Event" : type, 76, 195);

        g.setColor(new Color(32, 76, 160));
        g.fillRoundRect(284, 164, 380, 44, 20, 20);
        g.setColor(new Color(205, 164, 87));
        g.fillRoundRect(642, 164, 22, 44, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(auth.isEmpty() ? "AUTH" : auth, 302, 194);
    }

    private void drawLogoBlock(Graphics2D g) {
        g.setColor(new Color(233, 236, 238));
        g.fillRoundRect(1540, 62, 190, 50, 12, 12);
        g.setColor(new Color(186, 196, 204));
        g.drawRoundRect(1540, 62, 190, 50, 12, 12);
        g.setColor(new Color(126, 138, 147));
        g.fillRoundRect(1556, 74, 24, 24, 6, 6);
        g.setColor(new Color(77, 91, 104));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Tickify", 1592, 95);
    }

    private void drawAlbum(Graphics2D g, Map<String, Object> ticket) {
        int x = 78;
        int y = 206;
        int w = 430;
        int h = 510;
        BufferedImage album = loadEventAlbumImage(ticket);
        if (album != null) {
            g.drawImage(album, x, y, w, h, null);
        } else {
            g.setColor(new Color(236, 244, 232));
            g.fillRoundRect(x, y, w, h, 24, 24);
            g.setColor(new Color(161, 183, 150));
            g.drawRoundRect(x, y, w, h, 24, 24);
            g.setColor(new Color(97, 122, 88));
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.drawString("EVENT ART", x + 130, y + 250);
        }

        g.setColor(new Color(129, 169, 106));
        g.setStroke(new BasicStroke(2.4f));
        g.drawRoundRect(x, y, w, h, 24, 24);
    }

    private void drawQrPanel(Graphics2D g, Map<String, Object> ticket, boolean flagged, boolean freshRequested, String downloadToken) {
        int x = 1325;
        int y = 218;
        int w = 390;
        int h = 430;
        g.setColor(new Color(241, 248, 238));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(new Color(161, 182, 149));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setColor(new Color(45, 73, 39));
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString("SCAN CODE", x + 102, y + 48);

        BufferedImage qr = loadQrImage(safe(ticket.get("scannableCode")));
        if (qr != null) {
            g.drawImage(qr, x + 56, y + 74, 278, 278, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(x + 56, y + 74, 278, 278);
            g.setColor(new Color(130, 153, 119));
            g.drawRect(x + 56, y + 74, 278, 278);
            g.setColor(new Color(87, 111, 79));
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            g.drawString("QR unavailable", x + 120, y + 220);
        }

        g.setColor(new Color(74, 97, 68));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(truncate("Payload: " + safe(ticket.get("scannableCode")), 52), x + 26, y + 380);

        if (flagged) {
            g.setColor(new Color(239, 228, 228));
            g.fillRoundRect(x + 18, y + 392, 356, 34, 12, 12);
            g.setColor(new Color(196, 128, 128));
            g.drawRoundRect(x + 18, y + 392, 356, 34, 12, 12);
            g.setColor(new Color(142, 33, 33));
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString("SCREENSHOT BLOCKED AND FLAGGED", x + 28, y + 417);
        } else if (freshRequested && !downloadToken.isEmpty()) {
            g.drawString("Fresh download token issued", x + 26, y + 406);
        } else {
            g.drawString("Status: CONFIRMED", x + 26, y + 406);
        }
    }

    private void drawText(Graphics2D g, String label, String value, int x, int y) {
        g.setColor(new Color(61, 90, 55));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(label, x, y);
        g.setColor(new Color(38, 48, 35));
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString(value, x + 180, y);
    }

    private byte[] toJpegBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to encode ticket image", ex);
        }
    }

    private BufferedImage loadEventAlbumImage(Map<String, Object> ticket) {
        Object eventIdObj = ticket.get("eventID");
        if (!(eventIdObj instanceof Number)) {
            return null;
        }
        int eventId = ((Number) eventIdObj).intValue();
        if (eventId <= 0) {
            return null;
        }

        String base = resolveAppBaseUrl();
        if (base.isEmpty()) {
            return null;
        }

        String url = base + "/EventAlbumImage.do?eventID=" + eventId;
        return readRemoteImage(url);
    }

    private BufferedImage loadQrImage(String payload) {
        String clean = trim(payload);
        if (clean.isEmpty()) {
            return null;
        }
        try {
            String url = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data="
                    + URLEncoder.encode(clean, "UTF-8");
            return readRemoteImage(url);
        } catch (Exception ex) {
            return null;
        }
    }

    private BufferedImage readRemoteImage(String sourceUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(sourceUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Tickify-Ticket-Renderer/1.0");
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (IOException ex) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String resolveAppBaseUrl() {
        String configured = System.getProperty("tickify.app.baseUrl");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("TICKIFY_APP_BASE_URL");
        }
        if (configured == null || configured.trim().isEmpty()) {
            return "";
        }

        String base = configured.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.contains("/Tickify-SWP-Web-App")) {
            base = base + "/Tickify-SWP-Web-App";
        }
        return base;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, Math.max(0, max - 1)) + "...";
    }

    private String formatDate(Object value) {
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format((Date) value);
        }
        return safe(value);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(trim(value));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isTrue(String value) {
        String clean = trim(value).toLowerCase(Locale.ENGLISH);
        return "1".equals(clean) || "true".equals(clean) || "yes".equals(clean);
    }

    private String sanitizeFilenamePart(String value) {
        String safe = trim(value).replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-]", "");
        return safe.isEmpty() ? "ticket" : safe;
    }
}
