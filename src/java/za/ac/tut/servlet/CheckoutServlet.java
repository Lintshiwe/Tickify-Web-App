package za.ac.tut.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CheckoutServlet extends HttpServlet {

    private static final String CART_KEY = "attendeeCart";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer attendeeId = (Integer) request.getSession().getAttribute("userID");
        if (attendeeId == null) {
            response.sendRedirect(request.getContextPath() + "/Login.jsp");
            return;
        }

        HttpSession session = request.getSession();
        Map<Integer, Map<String, Object>> cart = getOrCreateCart(session);
        double checkoutTotal = calculateCartTotal(cart.values());
        int cartCount = calculateCartCount(cart.values());

        request.setAttribute("cartItems", cart.values());
        request.setAttribute("checkoutTotal", checkoutTotal);
        request.setAttribute("cartCount", cartCount);

        request.getRequestDispatcher("/Attendee/Checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
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
            count += (Integer) item.get("quantity");
        }
        return count;
    }

    private double calculateCartTotal(Collection<Map<String, Object>> items) {
        double total = 0.0;
        for (Map<String, Object> item : items) {
            double price = ((Number) item.get("price")).doubleValue();
            int quantity = (Integer) item.get("quantity");
            total += price * quantity;
        }
        return total;
    }
}
