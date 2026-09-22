import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new java.net.InetSocketAddress(8080), 0
        );

        // =========================
        // WEBSITE PAGES
        // =========================

        server.createContext(
                "/",
                Main::handleRequest
        );

        // =========================
        // API ENDPOINTS
        // =========================

        // Login
        server.createContext(
                "/api/login",
                LoginHandler::handleLogin
        );

        // Admin Dashboard
        server.createContext(
                "/api/dashboard",
                DashboardHandler::handleDashboard
        );

        // Vendors
        server.createContext(
                "/api/vendors",
                VendorHandler::handleVendors
        );

        // Vendor Registration
        server.createContext(
                "/api/vendor/register",
                VendorRegistrationHandler::handleRegistration
        );

        // Vendor Approval / Rejection
        server.createContext(
                "/api/vendor/approval",
                VendorApprovalHandler::handleApproval
        );

        // Tenders
        server.createContext(
                "/api/tenders",
                TenderHandler::handleTenders
        );

        // Bids
        server.createContext(
                "/api/bids",
                BidHandler::handleBids
        );

        // =========================
        // START SERVER
        // =========================

        server.start();

        System.out.println();
        System.out.println("==============================");
        System.out.println("TMS Server started!");
        System.out.println("Open: http://localhost:8080");
        System.out.println("==============================");
        System.out.println();
    }


    private static void handleRequest(
            HttpExchange exchange
    ) throws IOException {

        String path =
                exchange.getRequestURI().getPath();


        // Open index.html for /
        if (path.equals("/")) {

            path = "/index.html";
        }


        Path filePath =
                Path.of("web" + path);


        // File not found
        if (
                !Files.exists(filePath)
                || Files.isDirectory(filePath)
        ) {

            String response =
                    "404 - Page Not Found";


            byte[] responseBytes =
                    response.getBytes();


            exchange.sendResponseHeaders(
                    404,
                    responseBytes.length
            );


            try (
                    OutputStream output =
                            exchange.getResponseBody()
            ) {

                output.write(
                        responseBytes
                );
            }


            return;
        }


        // Read file
        byte[] response =
                Files.readAllBytes(
                        filePath
                );


        // Default content type
        String contentType =
                "text/html; charset=UTF-8";


        // CSS
        if (
                path.endsWith(".css")
        ) {

            contentType =
                    "text/css; charset=UTF-8";


        // JavaScript
        } else if (
                path.endsWith(".js")
        ) {

            contentType =
                    "application/javascript; charset=UTF-8";


        // PNG
        } else if (
                path.endsWith(".png")
        ) {

            contentType =
                    "image/png";


        // JPG
        } else if (
                path.endsWith(".jpg")
                || path.endsWith(".jpeg")
        ) {

            contentType =
                    "image/jpeg";


        // GIF
        } else if (
                path.endsWith(".gif")
        ) {

            contentType =
                    "image/gif";


        // SVG
        } else if (
                path.endsWith(".svg")
        ) {

            contentType =
                    "image/svg+xml";
        }


        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );


        exchange.sendResponseHeaders(
                200,
                response.length
        );


        try (
                OutputStream output =
                        exchange.getResponseBody()
        ) {

            output.write(
                    response
            );
        }
    }
}