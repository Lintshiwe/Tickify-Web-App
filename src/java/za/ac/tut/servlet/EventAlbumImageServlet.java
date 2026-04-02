package za.ac.tut.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.media.EventMediaService;

public class EventAlbumImageServlet extends HttpServlet {

    private final EventMediaService eventMediaService = new EventMediaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int eventID;
        try {
            eventID = Integer.parseInt(request.getParameter("eventID"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Map<String, Object> image = eventMediaService.repo().getEventAlbumImage(eventID);
            if (image == null || image.get("imageData") == null) {
                byte[] placeholder = buildPlaceholderSvg(eventID);
                response.setContentType("image/svg+xml");
                response.setHeader("Cache-Control", "public, max-age=300");
                response.setContentLength(placeholder.length);
                response.getOutputStream().write(placeholder);
                return;
            }

            String mimeType = (String) image.get("mimeType");
            byte[] data = (byte[]) image.get("imageData");
            response.setContentType(mimeType != null ? mimeType : "image/jpeg");
            response.setHeader("Cache-Control", "public, max-age=300");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
        } catch (SQLException e) {
            log("Unable to load event album image", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] buildPlaceholderSvg(int eventID) {
        String idText = eventID > 0 ? String.valueOf(eventID) : "-";
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='1200' height='675' viewBox='0 0 1200 675'>"
                + "<defs><linearGradient id='bg' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0%' stop-color='#1f3d1b'/><stop offset='100%' stop-color='#7fc342'/></linearGradient></defs>"
                + "<rect width='1200' height='675' fill='url(#bg)'/>"
                + "<text x='80' y='300' fill='#f4faef' font-size='64' font-family='Segoe UI, Arial, sans-serif' font-weight='700'>Tickify Event</text>"
                + "<text x='80' y='370' fill='#e5f2db' font-size='34' font-family='Segoe UI, Arial, sans-serif'>Album image not uploaded yet</text>"
                + "<text x='80' y='430' fill='#d8ebcc' font-size='24' font-family='Segoe UI, Arial, sans-serif'>Event ID: " + idText + "</text>"
                + "</svg>";
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
