package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.engagement.EngagementCampaignService;
import za.ac.tut.application.attendee.AttendeeService;
import za.ac.tut.entities.Attendee;
import za.ac.tut.notification.EmailService;
import za.ac.tut.payment.PaymentProvider;
import za.ac.tut.payment.PaymentProviderFactory;
import za.ac.tut.payment.PaymentRequest;
import za.ac.tut.payment.PaymentResult;

public class PaymentGatewayServlet extends HttpServlet {

    private static final String CART_KEY = "attendeeCart";
    private static final String PENDING_CHECKOUT_KEY = "pendingCheckoutCart";
    private static final String AGE_RESTRICTED_ERR = "AgeRestricted";
    private static final String SOLD_OUT_ERR = "SoldOut";
    private final AttendeeService attendeeService = new AttendeeService();
    private final EngagementCampaignService engagementCampaignService = new EngagementCampaignService();
    private final EmailService emailService = new EmailService();
    private final PaymentProvider paymentProvider = PaymentProviderFactory.resolveProvider();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp");
            return;
        }

        HttpSession session = request.getSession();
        Map<Integer, Map<String, Object>> pending = getOrCreatePendingCheckout(session);
        if (pending.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/Checkout.do?err=CartEmpty");
            return;
        }

        request.setAttribute("cartItems", pending.values());
        request.setAttribute("cartCount", calculateCartCount(pending.values()));
        request.setAttribute("checkoutTotal", calculateCartTotal(pending.values()));
        request.setAttribute("paymentMode", paymentProvider.modeName());
        request.getRequestDispatcher("/Attendee/PaymentGateway.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp");
            return;
        }

        String action = request.getParameter("paymentAction");
        HttpSession session = request.getSession();
        Map<Integer, Map<String, Object>> pending = getOrCreatePendingCheckout(session);
        if (pending.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/Checkout.do?err=CartEmpty");
            return;
        }

        if ("cancel".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/Checkout.do?msg=PaymentCancelled");
            return;
        }

        try {
            if (containsMinorRestrictedItems(attendeeId, pending)) {
                response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=" + AGE_RESTRICTED_ERR);
                return;
            }
        } catch (SQLException ex) {
            log("Unable to validate attendee age restriction before payment", ex);
            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=OperationFailed");
            return;
        }

        String acceptedTerms = request.getParameter("acceptNoRefund");
        if (!"yes".equalsIgnoreCase(acceptedTerms)) {
            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=TermsRequired");
            return;
        }

        try {
            if (!hasSufficientStock(pending)) {
                response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=" + SOLD_OUT_ERR);
                return;
            }
        } catch (SQLException ex) {
            log("Unable to validate stock before payment", ex);
            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=OperationFailed");
            return;
        }

        double checkoutTotal = calculateCartTotal(pending.values());
        PaymentRequest paymentRequest = new PaymentRequest(
                request.getParameter("holder"),
                request.getParameter("number"),
                request.getParameter("expiry"),
                request.getParameter("cvv"),
                request.getParameter("country"),
                checkoutTotal
        );

        PaymentResult paymentResult = paymentProvider.processPayment(paymentRequest);
        if (!paymentResult.isSuccessful()) {
            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=PaymentFailed");
            return;
        }

        int requested = 0;
        int purchased = 0;
        try {
            List<Map<String, Object>> purchasedTickets = new ArrayList<>();
            List<Integer> touchedEventIds = new ArrayList<>();
            for (Map<String, Object> item : pending.values()) {
                int eventId = ((Number) item.get("eventID")).intValue();
                int quantity = ((Number) item.get("quantity")).intValue();
                requested += quantity;
                touchedEventIds.add(Integer.valueOf(eventId));
                int createdForEvent = attendeeService.repo().purchaseTicketsForEvent(attendeeId, eventId, quantity);
                purchased += createdForEvent;
                if (createdForEvent > 0) {
                    purchasedTickets.addAll(attendeeService.repo().getRecentAttendeeTicketsForEvent(attendeeId, eventId, createdForEvent));
                }
            }

            if (purchased > 0) {
                try {
                    attendeeService.repo().recordOrderHistory(attendeeId, paymentResult.getTransactionRef(), pending.values(), checkoutTotal);
                } catch (SQLException historyEx) {
                    log("Order history persistence failed after successful payment", historyEx);
                }

                boolean emailSent = sendPurchaseEmailBestEffort(request, attendeeId, paymentResult.getTransactionRef(), purchasedTickets);
                session.setAttribute("purchaseConfirmationMessage", "Your payment was successful and " + purchased + " ticket(s) were confirmed.");
                session.setAttribute("purchaseEmailStatus", emailSent ? "SENT" : "FAILED");

                try {
                    engagementCampaignService.refreshBuyerProgressAndReward(attendeeId);
                } catch (SQLException badgeEx) {
                    log("Unable to refresh buyer badge/coupon status", badgeEx);
                }
                engagementCampaignService.processWishlistLowStockAlerts(request, touchedEventIds);

                pending.clear();
                getOrCreateCart(session).clear();
                if (purchased < requested) {
                    request.setAttribute("ticketWindowUrl",
                            request.getContextPath() + "/ViewMyTickets.do?msg=CheckoutPartial&popup=1");
                    request.setAttribute("nextUrl",
                        request.getContextPath() + "/ViewMyTickets.do?msg=CheckoutPartial");
                    request.setAttribute("transactionRef", paymentResult.getTransactionRef());
                } else {
                    request.setAttribute("ticketWindowUrl",
                            request.getContextPath() + "/ViewMyTickets.do?msg=PaymentSuccess&popup=1");
                    request.setAttribute("nextUrl",
                        request.getContextPath() + "/ViewMyTickets.do?msg=PaymentSuccess");
                    request.setAttribute("transactionRef", paymentResult.getTransactionRef());
                }
                request.getRequestDispatcher("/Attendee/PaymentSuccessRedirect.jsp").forward(request, response);
                return;
            }

            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=PaymentFailed");
        } catch (SQLException e) {
            log("Payment processing failed", e);
            response.sendRedirect(request.getContextPath() + "/PaymentGateway.do?err=PaymentFailed");
        }
    }

    private boolean hasSufficientStock(Map<Integer, Map<String, Object>> pending) throws SQLException {
        if (pending == null || pending.isEmpty()) {
            return false;
        }
        for (Map<String, Object> item : pending.values()) {
            if (item == null) {
                continue;
            }
            int eventId = ((Number) item.get("eventID")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            int available = attendeeService.repo().countAvailableTicketStockForEvent(eventId);
            if (available < quantity) {
                return false;
            }
        }
        return true;
    }

    private boolean sendPurchaseEmailBestEffort(HttpServletRequest request, int attendeeId,
            String transactionRef, List<Map<String, Object>> purchasedTickets) {
        if (purchasedTickets == null || purchasedTickets.isEmpty()) {
            return false;
        }
        try {
            Attendee attendee = attendeeService.repo().getAttendeeByID(attendeeId);
            if (attendee == null || attendee.getEmail() == null || attendee.getEmail().trim().isEmpty()) {
                return false;
            }

            String first = attendee.getFirstname() == null ? "" : attendee.getFirstname().trim();
            String last = attendee.getLastname() == null ? "" : attendee.getLastname().trim();
            String attendeeName = (first + " " + last).trim();
            String myTicketsLink = resolveAppBaseUrl(request)
                    + request.getContextPath()
                    + "/ViewMyTickets.do?msg=PaymentSuccess";

            emailService.sendTicketPurchaseEmail(attendee.getEmail(), attendeeName,
                    transactionRef, purchasedTickets, myTicketsLink);
            return true;
        } catch (Exception ex) {
            // Ticket delivery should not block successful checkout.
            log("Ticket purchase email delivery failed", ex);
            return false;
        }
    }

    private String resolveAppBaseUrl(HttpServletRequest request) {
        String configured = System.getProperty("tickify.app.baseUrl");
        if (configured != null) {
            configured = configured.trim();
            if (!configured.isEmpty()) {
                if (configured.endsWith("/")) {
                    return configured.substring(0, configured.length() - 1);
                }
                return configured;
            }
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<String, Object>> getOrCreatePendingCheckout(HttpSession session) {
        Object existing = session.getAttribute(PENDING_CHECKOUT_KEY);
        if (existing instanceof Map) {
            return (Map<Integer, Map<String, Object>>) existing;
        }
        Map<Integer, Map<String, Object>> pending = new HashMap<>();
        session.setAttribute(PENDING_CHECKOUT_KEY, pending);
        return pending;
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

    private int calculateCartCount(Collection<Map<String, Object>> items) {
        int count = 0;
        for (Map<String, Object> item : items) {
            count += ((Number) item.get("quantity")).intValue();
        }
        return count;
    }

    private double calculateCartTotal(Collection<Map<String, Object>> items) {
        double total = 0.0;
        for (Map<String, Object> item : items) {
            total += ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue();
        }
        return total;
    }

    private boolean containsMinorRestrictedItems(int attendeeId, Map<Integer, Map<String, Object>> pending) throws SQLException {
        if (!attendeeService.repo().isAttendeeUnder18(attendeeId)) {
            return false;
        }
        for (Map.Entry<Integer, Map<String, Object>> entry : pending.entrySet()) {
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
}
