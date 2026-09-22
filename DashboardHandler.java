import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardHandler {

    public static void handleDashboard(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        int totalVendors = 0;
        int totalTenders = 0;
        int totalBids = 0;
        int pendingApprovals = 0;

        try (Connection connection = DBConnection.getConnection()) {

            // Total vendors
            String vendorSQL = "SELECT COUNT(*) FROM vendors";

            try (PreparedStatement statement =
                         connection.prepareStatement(vendorSQL);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    totalVendors = result.getInt(1);
                }
            }

            // Total tenders
            String tenderSQL = "SELECT COUNT(*) FROM tenders";

            try (PreparedStatement statement =
                         connection.prepareStatement(tenderSQL);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    totalTenders = result.getInt(1);
                }
            }

            // Total bids
            String bidSQL = "SELECT COUNT(*) FROM bids";

            try (PreparedStatement statement =
                         connection.prepareStatement(bidSQL);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    totalBids = result.getInt(1);
                }
            }

            // Pending vendor approvals
            String pendingSQL =
                    "SELECT COUNT(*) FROM vendors WHERE approval_status = 'PENDING'";

            try (PreparedStatement statement =
                         connection.prepareStatement(pendingSQL);
                 ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    pendingApprovals = result.getInt(1);
                }
            }

            String response = "{"
                    + "\"totalVendors\":" + totalVendors + ","
                    + "\"totalTenders\":" + totalTenders + ","
                    + "\"totalBids\":" + totalBids + ","
                    + "\"pendingApprovals\":" + pendingApprovals
                    + "}";

            sendResponse(exchange, 200, response);

        } catch (Exception e) {

            System.out.println("Dashboard database error:");
            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while loading dashboard."
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