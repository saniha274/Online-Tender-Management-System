import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class VendorHandler {

    public static void handleVendors(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String sql = """
                SELECT
                    v.id,
                    v.company_name,
                    v.contact_person,
                    v.phone,
                    v.address,
                    v.approval_status,
                    v.created_at,
                    u.email
                FROM vendors v
                INNER JOIN users u
                    ON v.user_id = u.id
                ORDER BY v.created_at DESC
                """;

        StringBuilder json = new StringBuilder();

        json.append("[");

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {

            boolean first = true;

            while (result.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{");

                json.append("\"id\":")
                        .append(result.getInt("id"))
                        .append(",");

                json.append("\"companyName\":\"")
                        .append(escapeJson(
                                result.getString("company_name")
                        ))
                        .append("\",");

                json.append("\"contactPerson\":\"")
                        .append(escapeJson(
                                result.getString("contact_person")
                        ))
                        .append("\",");

                json.append("\"email\":\"")
                        .append(escapeJson(
                                result.getString("email")
                        ))
                        .append("\",");

                json.append("\"phone\":\"")
                        .append(escapeJson(
                                result.getString("phone")
                        ))
                        .append("\",");

                json.append("\"address\":\"")
                        .append(escapeJson(
                                result.getString("address")
                        ))
                        .append("\",");

                json.append("\"approvalStatus\":\"")
                        .append(escapeJson(
                                result.getString("approval_status")
                        ))
                        .append("\",");

                json.append("\"createdAt\":\"")
                        .append(escapeJson(
                                String.valueOf(
                                        result.getTimestamp("created_at")
                                )
                        ))
                        .append("\"");

                json.append("}");
            }

            json.append("]");

            sendResponse(
                    exchange,
                    200,
                    json.toString()
            );

        } catch (Exception e) {

            System.out.println(
                    "Vendor database error:"
            );

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while loading vendors."
            );
        }
    }

    private static String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
                "application/json; charset=UTF-8"
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