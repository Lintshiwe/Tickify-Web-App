package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.attendee.AttendeeService;

public class BookTicketServlet extends HttpServlet {

    private static final String CART_KEY = "attendeeCart";
    private static final String PENDING_CHECKOUT_KEY = "pendingCheckoutCart";
    private static final String AGE_RESTRICTED_ERR = "AgeRestricted";
    private final AttendeeService attendeeService = new AttendeeService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean ajax = isAjax(request);
        String returnTo = request.getParameter("returnTo");
        try {
            HttpSession session = request.getSession(false);
            Integer attendeeId = session != null ? (Integer) session.getAttribute("userID") : null;
            if (attendeeId == null) {
                if (ajax) {
                    writeJson(response, false, "Session expired", null, null);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/Login.jsp");
                return;
            }

            String action = request.getParameter("action");
            if ("checkout".equalsIgnoreCase(action)) {
                checkoutCart(request, response, session, attendeeId, returnTo);
                return;
            }
            if ("remove".equalsIgnoreCase(action)) {
                removeFromCart(request, response, session, returnTo);
                return;
            }
            if ("update".equalsIgnoreCase(action)) {
                updateCartItem(request, response, session, returnTo);
                return;
            }
            if ("clear".equalsIgnoreCase(action)) {
                clearCart(request, response, session, returnTo);
                return;
            }

            String eventIdParam = request.getParameter("eventID");
            int eventId;
            try {
                eventId = Integer.parseInt(eventIdParam);
            } catch (NumberFormatException e) {
                if (ajax) {
                    writeJson(response, false, "Invalid event", null, null);
                    return;
                }
                redirectWithStatus(request, response, returnTo, "err=InvalidEvent");
                return;
            }

            int quantity;
            try {
                quantity = Math.max(1, Integer.parseInt(request.getParameter("quantity")));
            } catch (NumberFormatException e) {
                quantity = 1;
            }

            try {
                Map<String, Object> eventInfo = attendeeService.repo().getEventCartDetails(eventId);
                if (eventInfo == null) {
                    if (ajax) {
                        writeJson(response, false, "Invalid event", null, null);
                        return;
                    }
                    redirectWithStatus(request, response, returnTo, "err=InvalidEvent");
                    return;
                }

                int availableStock = attendeeService.repo().countAvailableTicketStockForEvent(eventId);
                if (availableStock <= 0) {
                    if (ajax) {
                        writeJson(response, false, "This event is sold out.", null, null);
                        return;
                    }
                    redirectWithStatus(request, response, returnTo, "err=SoldOut");
                    return;
                }

                if (isRestrictedForMinor(attendeeId, eventInfo)) {
                    if (ajax) {
                        writeJson(response, false, "This event is age-restricted for under-18 accounts.", null, null);
                        return;
                    }
                    redirectWithStatus(request, response, returnTo, "err=" + AGE_RESTRICTED_ERR);
                    return;
                }

                Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
                Map<String, Object> line = cart.get(eventId);
                if (line == null) {
                    line = new HashMap<>();
                    line.put("eventID", eventId);
                    line.put("eventName", eventInfo.get("name"));
                    line.put("price", eventInfo.get("price"));
                    line.put("quantity", quantity);
                    cart.put(eventId, line);
                } else {
                    int currentQty = (Integer) line.get("quantity");
                    line.put("quantity", currentQty + quantity);
                }

                if (ajax) {
                    writeJson(response, true, "Added to cart", calculateCartCount(cart), calculateCartTotal(cart));
                    return;
                }

                redirectWithStatus(request, response, returnTo, "msg=AddedToCart");
            } catch (SQLException e) {
                log("Cart update failed", e);
                if (ajax) {
                    writeJson(response, false, "Cart update failed", null, null);
                    return;
                }
                redirectWithStatus(request, response, returnTo, "err=CartUpdateFailed");
            }
        } catch (Exception e) {
            log("Unexpected cart processing error", e);
            if (ajax) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(response, false, "Unexpected server error", null, null);
                return;
            }
            redirectWithStatus(request, response, returnTo, "err=CartUpdateFailed");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/AttendeeDashboardServlet.do");
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<String, Object>> getOrCreateCart(HttpSession session) {
        Object existing = session.getAttribute(CART_KEY);
        if (existing instanceof Map) {
            return (Map<Integer, Map<String, Object>>) existing;
        }
        Map<Integer, Map<String, Object>> cart = new HashMap<>();
        session.setAttribute(CART_KEY, cart);
        return cart;
    }

    private void removeFromCart(HttpServletRequest request, HttpServletResponse response, HttpSession session, String returnTo)
            throws IOException {
        String eventIdParam = request.getParameter("eventID");
        boolean ajax = isAjax(request);
        try {
            int eventId = Integer.parseInt(eventIdParam);
            Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
            cart.remove(eventId);
            if (ajax) {
                writeJson(response, true, "Removed from cart", calculateCartCount(cart), calculateCartTotal(cart));
                return;
            }
            redirectWithStatus(request, response, returnTo, "msg=RemovedFromCart");
        } catch (NumberFormatException e) {
            if (ajax) {
                writeJson(response, false, "Invalid event", null, null);
                return;
            }
            redirectWithStatus(request, response, returnTo, "err=InvalidEvent");
        }
    }

        private void checkoutCart(HttpServletRequest request, HttpServletResponse response, HttpSession session, int attendeeId, String returnTo)
            throws IOException, SQLException {
        boolean ajax = isAjax(request);
        Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
        if (cart.isEmpty()) {
            if (ajax) {
                writeJson(response, false, "Cart empty", 0, 0.0);
                return;
            }
            redirectWithStatus(request, response, returnTo, "err=CartEmpty");
            return;
        }

        if (containsMinorRestrictedItems(attendeeId, cart)) {
            if (ajax) {
                writeJson(response, false, "One or more events are restricted for under-18 accounts.", null, null);
                return;
            }
            redirectWithStatus(request, response, returnTo, "err=" + AGE_RESTRICTED_ERR);
            return;
        }

        session.setAttribute(PENDING_CHECKOUT_KEY, cloneCart(cart));
        if (ajax) {
            writeJson(response, true, "Proceed to payment", calculateCartCount(cart), calculateCartTotal(cart));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/PaymentGateway.do");
    }

    private boolean isRestrictedForMinor(int attendeeId, Map<String, Object> eventInfo) throws SQLException {
        if (!attendeeService.repo().isAttendeeUnder18(attendeeId)) {
            return false;
        }
        Object type = eventInfo.get("type");
        String eventType = type == null ? "" : String.valueOf(type);
        return attendeeService.repo().isRestrictedForMinorByEventType(eventType);
    }

    private boolean containsMinorRestrictedItems(int attendeeId, Map<Integer, Map<String, Object>> cart) throws SQLException {
        if (!attendeeService.repo().isAttendeeUnder18(attendeeId)) {
            return false;
        }
        for (Map.Entry<Integer, Map<String, Object>> entry : cart.entrySet()) {
            Integer eventId = entry.getKey();
            if (eventId == null) {
                continue;
            }
            Map<String, Object> eventInfo = attendeeService.repo().getEventCartDetails(eventId);
            if (eventInfo == null) {
                continue;
            }
            Object type = eventInfo.get("type");
            String eventType = type == null ? "" : String.valueOf(type);
            if (attendeeService.repo().isRestrictedForMinorByEventType(eventType)) {
                return true;
            }
        }
        return false;
    }

    private void updateCartItem(HttpServletRequest request, HttpServletResponse response, HttpSession session, String returnTo)
            throws IOException {
        boolean ajax = isAjax(request);
        String eventIdParam = request.getParameter("eventID");
        String quantityParam = request.getParameter("quantity");
        try {
            int eventId = Integer.parseInt(eventIdParam);
            int quantity = Integer.parseInt(quantityParam);
            Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
            Map<String, Object> line = cart.get(eventId);
            if (line == null) {
                if (ajax) {
                    writeJson(response, false, "Invalid event", null, null);
                    return;
                }
                redirectWithStatus(request, response, returnTo, "err=InvalidEvent");
                return;
            }

            if (quantity <= 0) {
                cart.remove(eventId);
                if (ajax) {
                    writeJson(response, true, "Removed from cart", calculateCartCount(cart), calculateCartTotal(cart));
                    return;
                }
                redirectWithStatus(request, response, returnTo, "msg=RemovedFromCart");
            } else {
                line.put("quantity", quantity);
                if (ajax) {
                    writeJson(response, true, "Cart updated", calculateCartCount(cart), calculateCartTotal(cart));
                    return;
                }
                redirectWithStatus(request, response, returnTo, "msg=CartUpdated");
            }
        } catch (NumberFormatException e) {
            if (ajax) {
                writeJson(response, false, "Cart update failed", null, null);
                return;
            }
            redirectWithStatus(request, response, returnTo, "err=CartUpdateFailed");
        }
    }

    private void clearCart(HttpServletRequest request, HttpServletResponse response, HttpSession session, String returnTo)
            throws IOException {
        boolean ajax = isAjax(request);
        Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
        cart.clear();
        if (ajax) {
            writeJson(response, true, "Cart cleared", 0, 0.0);
            return;
        }
        redirectWithStatus(request, response, returnTo, "msg=CartCleared");
    }

    private void redirectWithStatus(HttpServletRequest request, HttpServletResponse response, String returnTo, String query)
            throws IOException {
        String base = "checkout".equalsIgnoreCase(returnTo)
                ? "/Checkout.do"
                : "/AttendeeDashboardServlet.do";
        response.sendRedirect(request.getContextPath() + base + "?" + query);
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("application/json");
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

    private Map<Integer, Map<String, Object>> cloneCart(Map<Integer, Map<String, Object>> original) {
        Map<Integer, Map<String, Object>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Object>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    private void writeJson(HttpServletResponse response, boolean ok, String message, Integer cartCount, Double checkoutTotal)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String msg = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        String countValue = cartCount == null ? "null" : Integer.toString(cartCount);
        String totalValue = checkoutTotal == null ? "null" : Double.toString(checkoutTotal);
        response.getWriter().write("{\"ok\":" + ok
                + ",\"message\":\"" + msg + "\""
                + ",\"cartCount\":" + countValue
                + ",\"checkoutTotal\":" + totalValue
                + "}");
    }
}