package za.ac.tut.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import za.ac.tut.application.media.AdvertService;

@MultipartConfig(maxFileSize = 10 * 1024 * 1024)
public class AdminAdvertsServlet extends HttpServlet {

    private final AdvertService advertService = new AdvertService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Map<String, Object>> adverts = advertService.repo().getAllAdverts();
            request.setAttribute("adverts", adverts);
            request.getRequestDispatcher("/Admin/AdminAdverts.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Unable to fetch adverts", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("toggle".equalsIgnoreCase(action)) {
            handleToggle(request, response);
            return;
        }
        if ("edit".equalsIgnoreCase(action)) {
            handleEdit(request, response);
            return;
        }
        if ("delete".equalsIgnoreCase(action)) {
            handleDelete(request, response);
            return;
        }
        handleCreate(request, response);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String org = trim(request.getParameter("organizationName"));
        String title = trim(request.getParameter("title"));
        String details = trim(request.getParameter("details"));
        String venue = trim(request.getParameter("venue"));
        String eventDateRaw = trim(request.getParameter("eventDate"));

        boolean paid = request.getParameter("paidOrganization") != null;
        boolean selected = request.getParameter("selectedForDisplay") != null;
        boolean active = request.getParameter("active") != null;

        if (org.isEmpty() || title.isEmpty() || venue.isEmpty() || eventDateRaw.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=MissingFields");
            return;
        }

        Date eventDate;
        try {
            eventDate = Date.valueOf(LocalDate.parse(eventDateRaw));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=BadDate");
            return;
        }

        Part imagePart = request.getPart("advertImage");
        if (imagePart == null || imagePart.getSize() == 0) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=MissingImage");
            return;
        }
        String contentType = imagePart.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=InvalidImage");
            return;
        }

        byte[] imageData;
        try {
            imageData = readBytes(imagePart.getInputStream());
        } catch (IOException e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=ImageRead");
            return;
        }

        try {
            advertService.repo().createAdvert(org, title, details, venue, eventDate, paid, selected, active,
                    imagePart.getSubmittedFileName(), contentType, imageData);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?msg=Created");
        } catch (SQLException e) {
            log("Unable to create advert", e);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=CreateFailed");
        }
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int advertID;
        try {
            advertID = Integer.parseInt(request.getParameter("advertID"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=InvalidAdvert");
            return;
        }

        boolean paid = request.getParameter("paidOrganization") != null;
        boolean selected = request.getParameter("selectedForDisplay") != null;
        boolean active = request.getParameter("active") != null;
        try {
            advertService.repo().updateAdvertFlags(advertID, paid, selected, active);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?msg=Updated");
        } catch (SQLException e) {
            log("Unable to update advert", e);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=UpdateFailed");
        }
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int advertID;
        try {
            advertID = Integer.parseInt(trim(request.getParameter("advertID")));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=InvalidAdvert");
            return;
        }

        String org = trim(request.getParameter("organizationName"));
        String title = trim(request.getParameter("title"));
        String details = trim(request.getParameter("details"));
        String venue = trim(request.getParameter("venue"));
        String eventDateRaw = trim(request.getParameter("eventDate"));
        boolean paid = request.getParameter("paidOrganization") != null;
        boolean selected = request.getParameter("selectedForDisplay") != null;
        boolean active = request.getParameter("active") != null;

        if (org.isEmpty() || title.isEmpty() || venue.isEmpty() || eventDateRaw.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=MissingFields");
            return;
        }

        Date eventDate;
        try {
            eventDate = Date.valueOf(LocalDate.parse(eventDateRaw));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=BadDate");
            return;
        }

        String imageFilename = null;
        String imageMimeType = null;
        byte[] imageData = null;
        Part imagePart = request.getPart("advertImage");
        if (imagePart != null && imagePart.getSize() > 0) {
            String contentType = imagePart.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=InvalidImage");
                return;
            }
            imageFilename = imagePart.getSubmittedFileName();
            imageMimeType = contentType;
            try {
                imageData = readBytes(imagePart.getInputStream());
            } catch (IOException e) {
                response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=ImageRead");
                return;
            }
        }

        try {
            boolean ok = advertService.repo().updateAdvert(advertID, org, title, details, venue, eventDate,
                    paid, selected, active, imageFilename, imageMimeType, imageData);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?msg=" + (ok ? "Edited" : "NoChange"));
        } catch (SQLException e) {
            log("Unable to edit advert", e);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=UpdateFailed");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int advertID;
        try {
            advertID = Integer.parseInt(trim(request.getParameter("advertID")));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=InvalidAdvert");
            return;
        }

        try {
            boolean ok = advertService.repo().deleteAdvert(advertID);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?msg=" + (ok ? "Deleted" : "NoChange"));
        } catch (SQLException e) {
            log("Unable to delete advert", e);
            response.sendRedirect(request.getContextPath() + "/AdminAdverts.do?err=DeleteFailed");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
