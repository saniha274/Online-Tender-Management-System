import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginHandler {

    public static void handleLogin(HttpExchange exchange) throws IOException {

        // Only POST requests are allowed
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        // Read data sent from login.html
        String requestBody = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        String[] data = requestBody.split("&");

        String email = "";
        String password = "";
        String role = "";

        // Read each form field
        for (String item : data) {

            String[] parts = item.split("=", 2);

            if (parts.length < 2) {
                continue;
            }

            String key = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
            );

            String value = URLDecoder.decode(
                    parts[1],
                    StandardCharsets.UTF_8
            );

            if (key.equals("email")) {
                email = value;
            } else if (key.equals("password")) {
                password = value;
            } else if (key.equals("role")) {
                role = value;
            }
        }

        // DEBUG INFORMATION
        System.out.println();
        System.out.println("========== LOGIN DEBUG ==========");
        System.out.println("Email: [" + email + "]");
        System.out.println("Role: [" + role + "]");
        System.out.println("Password length: " + password.length());
        System.out.println("=================================");
        System.out.println();

        // SQL query
        String sql = """
                SELECT id, email, role, status
                FROM users
                WHERE email = ?
                AND password = ?
                AND role = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);
            statement.setString(2, password);
            statement.setString(3, role);

            ResultSet result = statement.executeQuery();

            if (result.next()) {

                String status = result.getString("status");

                System.out.println("User found in database.");
                System.out.println("Status: " + status);

                if (status.equals("PENDING")) {

                    sendResponse(
                            exchange,
                            403,
                            "Your account is waiting for admin approval."
                    );

                    return;
                }

                if (status.equals("REJECTED")) {

                    sendResponse(
                            exchange,
                            403,
                            "Your account has been rejected."
                    );

                    return;
                }

                sendResponse(
                        exchange,
                        200,
                        "LOGIN_SUCCESS:" + role
                );

            } else {

                System.out.println("No matching user found in database.");

                sendResponse(
                        exchange,
                        401,
                        "Invalid email, password, or role."
                );
            }

        } catch (Exception e) {

            System.out.println("Database error while processing login:");

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while processing login."
            );
        }
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String message
    ) throws IOException {

        byte[] response =
                message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                response.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(response);
        }
    }
}