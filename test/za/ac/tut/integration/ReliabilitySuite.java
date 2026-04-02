package za.ac.tut.integration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ReliabilitySuite {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final String CONTEXT = "/Tickify-SWP-Web-App";

    public static void main(String[] args) throws Exception {
        System.out.println("[1/5] Running DAO/auth integration smoke...");
        AuthAndRoleFlowIntegrationTest.main(new String[0]);

        System.out.println("[2/5] Running service endpoint smoke...");
        runEndpointSmoke();

        System.out.println("[3/5] Running sequential load probe...");
        runSequentialProbe(150);

        System.out.println("[4/5] Running concurrent load probe...");
        runConcurrentProbe(12, 240);

        System.out.println("[5/5] Running email health check...");
        EmailHealthCheck.main(new String[0]);

        System.out.println("ReliabilitySuite: PASS");
    }

    private static void runEndpointSmoke() throws Exception {
        assertStatus(CONTEXT + "/index.html", 200);
        assertStatus(CONTEXT + "/Login.jsp", 200);

        assertRedirectToLogin(CONTEXT + "/AdminAdverts.do");
        assertRedirectToLogin(CONTEXT + "/AdminDashboard.do");
        assertRedirectToLogin(CONTEXT + "/TertiaryPresenterDashboard.do");
        assertRedirectToLogin(CONTEXT + "/EventManagerDashboard.do");
        assertRedirectToLogin(CONTEXT + "/VenueGuardAttendees.do");
        assertRedirectToLogin(CONTEXT + "/ValidateTicketServlet.do");
        assertRedirectToLogin(CONTEXT + "/Checkout.do");
        assertRedirectToLogin(CONTEXT + "/PaymentGateway.do");
    }

    private static void runSequentialProbe(int attempts) throws Exception {
        int ok = 0;
        int fail = 0;
        for (int i = 0; i < attempts; i++) {
            Response res = sendGet(CONTEXT + "/index.html");
            if (res.statusCode == 200) {
                ok++;
            } else {
                fail++;
            }
        }
        if (fail > 0) {
            throw new IllegalStateException("Sequential probe failed. ok=" + ok + " fail=" + fail);
        }
        System.out.println("Sequential probe ok=" + ok + " fail=" + fail);
    }

    private static void runConcurrentProbe(int workers, int totalRequests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < totalRequests; i++) {
            tasks.add(() -> sendGet(CONTEXT + "/index.html").statusCode);
        }

        int ok = 0;
        int fail = 0;
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);

        for (Future<Integer> f : futures) {
            int code = f.get();
            if (code == 200) {
                ok++;
            } else {
                fail++;
            }
        }

        if (fail > 0) {
            throw new IllegalStateException("Concurrent probe failed. ok=" + ok + " fail=" + fail);
        }
        System.out.println("Concurrent probe ok=" + ok + " fail=" + fail + " workers=" + workers + " total=" + totalRequests);
    }

    private static void assertStatus(String path, int expected) throws Exception {
        Response res = sendGet(path);
        if (res.statusCode != expected) {
            throw new IllegalStateException("Expected " + expected + " for " + path + " but got " + res.statusCode);
        }
    }

    private static void assertRedirectToLogin(String path) throws Exception {
        Response res = sendGet(path);
        if (res.statusCode != 302) {
            throw new IllegalStateException("Expected 302 for protected path " + path + " but got " + res.statusCode);
        }
        String location = res.location == null ? "" : res.location;
        if (!location.contains("/Login.jsp")) {
            throw new IllegalStateException("Expected redirect to Login.jsp for " + path + " but got location=" + location);
        }
    }

    private static Response sendGet(String path) throws Exception {
        try (Socket socket = new Socket(HOST, PORT)) {
            socket.setSoTimeout(8000);
            OutputStream out = socket.getOutputStream();
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + HOST + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String statusLine = reader.readLine();
                if (statusLine == null || !statusLine.startsWith("HTTP/")) {
                    throw new IllegalStateException("No valid HTTP response for path " + path);
                }

                int statusCode = parseStatusCode(statusLine);
                String location = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        break;
                    }
                    if (line.regionMatches(true, 0, "Location:", 0, 9)) {
                        location = line.substring(9).trim();
                    }
                }
                return new Response(statusCode, location);
            }
        }
    }

    private static int parseStatusCode(String statusLine) {
        String[] parts = statusLine.split(" ");
        if (parts.length < 2) {
            throw new IllegalStateException("Invalid HTTP status line: " + statusLine);
        }
        return Integer.parseInt(parts[1]);
    }

    private static class Response {
        private final int statusCode;
        private final String location;

        private Response(int statusCode, String location) {
            this.statusCode = statusCode;
            this.location = location;
        }
    }
}
