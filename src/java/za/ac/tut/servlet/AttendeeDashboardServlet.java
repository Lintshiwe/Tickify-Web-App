/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.media.AdvertService;
import za.ac.tut.application.attendee.AttendeeService;
import za.ac.tut.application.engagement.EngagementCampaignService;


public class AttendeeDashboardServlet extends HttpServlet {

    private final EngagementCampaignService engagementService = new EngagementCampaignService();


   @Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    
    try {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp");
            return;
        }

        String ajax = request.getParameter("ajax");
        if ("stock".equalsIgnoreCase(ajax)) {
            AttendeeService stockService = new AttendeeService();
            writeStockJson(response, stockService.repo().getEventStockSnapshots());
            return;
        }

        AttendeeService attendeeService = new AttendeeService();
        AdvertService advertService = new AdvertService();
        
        List<Map<String, Object>> eventList = attendeeService.repo().getAllEventsForAttendee(attendeeId);
        List<Map<String, Object>> wishlistEvents = attendeeService.repo().getWishlistEvents(attendeeId);
        List<Map<String, Object>> adverts = advertService.repo().getActiveSelectedAdverts();
        Set<Integer> wishlistEventIds = attendeeService.repo().getWishlistEventIds(attendeeId);
        Map<String, Object> engagement = engagementService.getAttendeeSnapshot(attendeeId);
        HttpSession session = request.getSession();
        Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
        double checkoutTotal = calculateCartTotal(cart);
        int cartCount = calculateCartCount(cart);

        for (Map<String, Object> event : eventList) {
            Integer eventId = (Integer) event.get("id");
            event.put("wishlisted", eventId != null && wishlistEventIds.contains(eventId));
        }

        for (Map<String, Object> event : wishlistEvents) {
            event.put("wishlisted", true);
        }

        List<Map<String, Object>> nearSoldOutWishlistEvents = new ArrayList<>();
        for (Map<String, Object> event : wishlistEvents) {
            boolean purchased = Boolean.TRUE.equals(event.get("purchased"));
            boolean nearlySoldOut = Boolean.TRUE.equals(event.get("nearlySoldOut"));
            if (!purchased && nearlySoldOut) {
                nearSoldOutWishlistEvents.add(event);
            }
        }
        
        request.setAttribute("eventList", eventList);
        request.setAttribute("wishlistEvents", wishlistEvents);
        request.setAttribute("adverts", adverts);
        request.setAttribute("wishlistEventIds", wishlistEventIds);
        request.setAttribute("nearSoldOutWishlistEvents", nearSoldOutWishlistEvents);
        request.setAttribute("nearSoldOutWishlistCount", nearSoldOutWishlistEvents.size());
        request.setAttribute("checkoutTotal", checkoutTotal);
        request.setAttribute("cartCount", cartCount);
        request.setAttribute("cartItems", cart.values());
        request.setAttribute("subscribed", Boolean.TRUE.equals(engagement.get("subscribed")));
        request.setAttribute("badgeTitle", engagement.get("badgeTitle"));
        request.setAttribute("badgeLevel", engagement.get("badgeLevel"));
        request.setAttribute("lifetimeTickets", engagement.get("totalTickets"));
        request.setAttribute("lifetimeSpend", engagement.get("totalSpend"));
        request.setAttribute("activeCouponCode", engagement.get("couponCode"));
        request.setAttribute("activeCouponPercent", engagement.get("couponPercent"));
        
        request.getRequestDispatcher("/Attendee/AttendeeDashboard.jsp").forward(request, response);
        
    } catch (SQLException e) {
        log("Error fetching events: " + e.getMessage());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}

    private void writeStockJson(HttpServletResponse response, List<Map<String, Object>> events) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder out = new StringBuilder();
        out.append("{\"ok\":true,\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            Map<String, Object> event = events.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append('{')
                    .append("\"id\":").append(toInt(event.get("id"))).append(',')
                    .append("\"totalTickets\":").append(toInt(event.get("totalTickets"))).append(',')
                    .append("\"soldTickets\":").append(toInt(event.get("soldTickets"))).append(',')
                    .append("\"availableTickets\":").append(toInt(event.get("availableTickets"))).append(',')
                    .append("\"soldPercentage\":").append(toInt(event.get("soldPercentage"))).append(',')
                    .append("\"soldOut\":").append(Boolean.TRUE.equals(event.get("soldOut"))).append(',')
                    .append("\"nearlySoldOut\":").append(Boolean.TRUE.equals(event.get("nearlySoldOut")))
                    .append('}');
        }
        out.append("]}");
        response.getWriter().write(out.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<String, Object>> getOrCreateCart(HttpSession session) {
        Object existing = session.getAttribute("attendeeCart");
        if (existing instanceof Map) {
            return (Map<Integer, Map<String, Object>>) existing;
        }
        Map<Integer, Map<String, Object>> cart = new HashMap<>();
        session.setAttribute("attendeeCart", cart);
        return cart;
    }

    private int calculateCartCount(Map<Integer, Map<String, Object>> cart) {
        int count = 0;
        for (Map<String, Object> item : cart.values()) {
            count += (Integer) item.get("quantity");
        }
        return count;
    }

    private double calculateCartTotal(Map<Integer, Map<String, Object>> cart) {
        double total = 0.0;
        for (Map<String, Object> item : cart.values()) {
            double price = ((Number) item.get("price")).doubleValue();
            int quantity = (Integer) item.get("quantity");
            total += price * quantity;
        }
        return total;
    }

}
