package za.ac.tut.servlet;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import za.ac.tut.application.admin.AdminITService;

@MultipartConfig(maxFileSize = 12 * 1024 * 1024)
public class AdminEventAlbumServlet extends HttpServlet {

    private static final int TARGET_WIDTH = 960;
    private static final int TARGET_HEIGHT = 700;

    private final AdminITService adminITService = new AdminITService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Object adminIdObj = request.getSession().getAttribute("userID");
            int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;
            List<java.util.Map<String, Object>> events = adminITService.repo().getEventControlRowsForScope(adminId);
            List<java.util.Map<String, Object>> venues = adminITService.repo().getVenueOptionsForScope(adminId);
            request.setAttribute("events", events);
            request.setAttribute("venues", venues);
            request.getRequestDispatcher("/Admin/AdminEventAlbum.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Unable to load events for album management", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object adminIdObj = request.getSession().getAttribute("userID");
        int adminId = adminIdObj instanceof Integer ? (Integer) adminIdObj : 0;

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "upload";
        }

        if ("createEvent".equalsIgnoreCase(action)) {
            try {
                int created = adminITService.repo().createEvent(
                        adminId,
                        req(request, "eventName"),
                        req(request, "eventType"),
                        parseDateTimeLocal(req(request, "eventDate")),
                        parsePositiveInt(req(request, "venueID")),
                        param(request, "eventDescription"),
                        param(request, "eventInfoUrl"),
                        param(request, "eventStatus")
                );
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?msg=" + (created > 0 ? "EventCreated" : "NoChange"));
            } catch (IllegalArgumentException ex) {
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=" + ex.getMessage());
            } catch (SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("CampusScopeDenied")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=CampusScopeDenied");
                    return;
                }
                if (ex.getMessage() != null && ex.getMessage().contains("MissingFields")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=MissingFields");
                    return;
                }
                if (ex.getMessage() != null && ex.getMessage().contains("InvalidAssignment")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=InvalidAssignment");
                    return;
                }
                log("Unable to create event from album page", ex);
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=UploadFailed");
            }
            return;
        }

        if ("updateEvent".equalsIgnoreCase(action)) {
            try {
                boolean updated = adminITService.repo().updateEvent(
                        adminId,
                        parsePositiveInt(req(request, "eventID")),
                        req(request, "eventName"),
                        req(request, "eventType"),
                        parseDateTimeLocal(req(request, "eventDate")),
                        parsePositiveInt(req(request, "venueID")),
                        param(request, "eventDescription"),
                        param(request, "eventInfoUrl"),
                        param(request, "eventStatus")
                );
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?msg=" + (updated ? "EventUpdated" : "NoChange"));
            } catch (IllegalArgumentException ex) {
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=" + ex.getMessage());
            } catch (SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("CampusScopeDenied")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=CampusScopeDenied");
                    return;
                }
                if (ex.getMessage() != null && ex.getMessage().contains("MissingFields")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=MissingFields");
                    return;
                }
                if (ex.getMessage() != null && ex.getMessage().contains("InvalidAssignment")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=InvalidAssignment");
                    return;
                }
                log("Unable to update event from album page", ex);
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=UploadFailed");
            }
            return;
        }

        int eventID;
        try {
            eventID = Integer.parseInt(request.getParameter("eventID"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=InvalidEvent");
            return;
        }

        if ("clear".equalsIgnoreCase(action)) {
            try {
                boolean cleared = adminITService.repo().clearEventAlbumImageForScope(adminId, eventID);
                response.sendRedirect(request.getContextPath() + (cleared
                        ? "/AdminEventAlbum.do?msg=CoverRemoved"
                        : "/AdminEventAlbum.do?err=UploadFailed"));
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("CampusScopeDenied")) {
                    response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=CampusScopeDenied");
                    return;
                }
                log("Unable to clear event album image", e);
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=UploadFailed");
            }
            return;
        }

        Part imagePart = request.getPart("eventAlbumImage");
        if (imagePart == null || imagePart.getSize() == 0) {
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=MissingImage");
            return;
        }

        String contentType = imagePart.getContentType();
        String guessed = imagePart.getSubmittedFileName() == null
                ? null
                : java.net.URLConnection.guessContentTypeFromName(imagePart.getSubmittedFileName());
        String normalized = contentType == null ? "" : contentType.toLowerCase();
        if (!normalized.startsWith("image/") && guessed != null && guessed.toLowerCase().startsWith("image/")) {
            contentType = guessed;
            normalized = guessed.toLowerCase();
        }
        if (!normalized.startsWith("image/")) {
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=InvalidImage");
            return;
        }

        byte[] imageBytes;
        try {
            imageBytes = readBytes(imagePart.getInputStream());
        } catch (IOException e) {
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=ImageRead");
            return;
        }

        try {
            imageBytes = optimizeForTicketAlbum(imageBytes, contentType);
        } catch (IOException e) {
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=ImageRead");
            return;
        }

        try {
            boolean updated = adminITService.repo().updateEventAlbumImageForScope(
                    adminId,
                    eventID,
                    imagePart.getSubmittedFileName(),
                    contentType,
                    imageBytes
            );
            response.sendRedirect(request.getContextPath() + (updated
                    ? "/AdminEventAlbum.do?msg=Uploaded"
                    : "/AdminEventAlbum.do?err=UploadFailed"));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("CampusScopeDenied")) {
                response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=CampusScopeDenied");
                return;
            }
            log("Unable to save event album image", e);
            response.sendRedirect(request.getContextPath() + "/AdminEventAlbum.do?err=UploadFailed");
        }
    }

    private String param(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? null : value.trim();
    }

    private String req(HttpServletRequest request, String key) {
        String value = param(request, key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("MissingFields");
        }
        return value;
    }

    private int parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("MissingFields");
            }
            return parsed;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("MissingFields");
        }
    }

    private Timestamp parseDateTimeLocal(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            return Timestamp.valueOf(dateTime);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("InvalidDate");
        }
    }

    private byte[] optimizeForTicketAlbum(byte[] originalBytes, String mimeType) throws IOException {
        if (originalBytes == null || originalBytes.length == 0) {
            return originalBytes;
        }

        // Keep SVG uploads as-is; raster optimization is handled by ImageIO.
        if (mimeType != null && mimeType.toLowerCase().contains("svg")) {
            return originalBytes;
        }

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (source == null) {
            return originalBytes;
        }

        double targetRatio = (double) TARGET_WIDTH / (double) TARGET_HEIGHT;
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        double srcRatio = (double) srcW / (double) srcH;

        int cropW = srcW;
        int cropH = srcH;
        int x = 0;
        int y = 0;

        if (srcRatio > targetRatio) {
            cropW = (int) Math.round(srcH * targetRatio);
            x = Math.max(0, (srcW - cropW) / 2);
        } else if (srcRatio < targetRatio) {
            cropH = (int) Math.round(srcW / targetRatio);
            y = Math.max(0, (srcH - cropH) / 2);
        }

        BufferedImage cropped = source.getSubimage(x, y, cropW, cropH);
        BufferedImage resized = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(cropped, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String format = (mimeType != null && mimeType.toLowerCase().contains("png")) ? "png" : "jpg";
        ImageIO.write(resized, format, out);
        return out.toByteArray();
    }

    private byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
