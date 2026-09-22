import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class VendorApprovalHandler {

    public static void handleApproval(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }

        String requestBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

        String[] data =
                requestBody.split("&");

        String vendorId = "";
        String action = "";

        for (String item : data) {

            String[] parts =
                    item.split("=", 2);

            if (parts.length < 2) {
                continue;
            }

            String key =
                    URLDecoder.decode(
                            parts[0],
                            StandardCharsets.UTF_8
                    );

            String value =
                    URLDecoder.decode(
                            parts[1],
                            StandardCharsets.UTF_8
                    );

            if (key.equals("vendorId")) {
                vendorId = value;
            }

            if (key.equals("action")) {
                action = value;
            }
        }

        if (vendorId.isBlank() || action.isBlank()) {

            sendResponse(
                    exchange,
                    400,
                    "Vendor ID and action are required."
            );

            return;
        }

        String approvalStatus;

        if (action.equalsIgnoreCase("APPROVE")) {

            approvalStatus = "APPROVED";

        } else if (action.equalsIgnoreCase("REJECT")) {

            approvalStatus = "REJECTED";

        } else {

            sendResponse(
                    exchange,
                    400,
                    "Invalid action."
            );

            return;
        }

        Connection connection = null;

        try {

            connection =
                    DBConnection.getConnection();

            connection.setAutoCommit(false);

            // Update vendor approval status
            String vendorSQL = """
                    UPDATE vendors
                    SET approval_status = ?
                    WHERE id = ?
                    """;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(vendorSQL)
            ) {

                statement.setString(
                        1,
                        approvalStatus
                );

                statement.setInt(
                        2,
                        Integer.parseInt(vendorId)
                );

                int updated =
                        statement.executeUpdate();

                if (updated == 0) {

                    connection.rollback();

                    sendResponse(
                            exchange,
                            404,
                            "Vendor not found."
                    );

                    return;
                }
            }

            // Keep users table status synchronized
            String userSQL = """
                    UPDATE users u
                    INNER JOIN vendors v
                        ON u.id = v.user_id
                    SET u.status = ?
                    WHERE v.id = ?
                    """;

            String userStatus =
                    approvalStatus.equals("APPROVED")
                            ? "ACTIVE"
                            : "REJECTED";

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(userSQL)
            ) {

                statement.setString(
                        1,
                        userStatus
                );

                statement.setInt(
                        2,
                        Integer.parseInt(vendorId)
                );

                statement.executeUpdate();
            }

            connection.commit();

            sendResponse(
                    exchange,
                    200,
                    "Vendor " + approvalStatus.toLowerCase() + " successfully."
            );

        } catch (Exception e) {

            if (connection != null) {

                try {
                    connection.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
            }

            System.out.println(
                    "Vendor approval error:"
            );

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while updating vendor."
            );

        } finally {

            if (connection != null) {

                try {
                    connection.close();
                } catch (Exception closeError) {
                    closeError.printStackTrace();
                }
            }
        }
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String message
    ) throws IOException {

        byte[] response =
                message.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                response.length
        );

        try (
                OutputStream output =
                        exchange.getResponseBody()
        ) {

            output.write(response);
        }
    }
}