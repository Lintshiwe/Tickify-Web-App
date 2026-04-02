package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.media.AdvertService;

public class AdvertImageServlet extends HttpServlet {

    private final AdvertService advertService = new AdvertService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int advertID;
        try {
            advertID = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Map<String, Object> img = advertService.repo().getAdvertImage(advertID);
            if (img == null || img.get("data") == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String mimeType = (String) img.get("mimeType");
            byte[] data = (byte[]) img.get("data");
            response.setContentType(mimeType != null ? mimeType : "image/png");
            response.setHeader("Cache-Control", "public, max-age=300");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
        } catch (SQLException e) {
            log("Unable to load advert image", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
