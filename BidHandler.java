import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BidHandler {

    public static void handleBids(
            HttpExchange exchange
    ) throws IOException {

        String method =
                exchange.getRequestMethod();

        if (method.equalsIgnoreCase("POST")) {

            submitBid(exchange);

            return;
        }

        if (method.equalsIgnoreCase("GET")) {

            getBids(exchange);

            return;
        }

        sendResponse(
                exchange,
                405,
                "Method Not Allowed"
        );
    }


    // ==========================================
    // SUBMIT BID
    // ==========================================

    private static void submitBid(
            HttpExchange exchange
    ) throws IOException {

        String requestBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

        String[] data =
                requestBody.split("&");

        String tenderId = "";
        String vendorId = "";
        String bidAmount = "";
        String proposal = "";

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

            switch (key) {

                case "tenderId":

                    tenderId = value;

                    break;

                case "vendorId":

                    vendorId = value;

                    break;

                case "bidAmount":

                    bidAmount = value;

                    break;

                case "proposal":

                    proposal = value;

                    break;
            }
        }


        if (
                tenderId.isBlank()
                || vendorId.isBlank()
                || bidAmount.isBlank()
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Tender ID, Vendor ID and bid amount are required."
            );

            return;
        }


        Connection connection = null;


        try {

            connection =
                    DBConnection.getConnection();


            // ==========================================
            // CHECK TENDER
            // ==========================================

            String tenderSQL = """
                    SELECT id, status, deadline
                    FROM tenders
                    WHERE id = ?
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    tenderSQL
                            )
            ) {

                statement.setInt(
                        1,
                        Integer.parseInt(tenderId)
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (!result.next()) {

                        sendResponse(
                                exchange,
                                404,
                                "Tender not found."
                        );

                        return;
                    }


                    String status =
                            result.getString(
                                    "status"
                            );


                    if (!status.equals(
                            "PUBLISHED"
                    )) {

                        sendResponse(
                                exchange,
                                400,
                                "This tender is not currently open for bidding."
                        );

                        return;
                    }


                    java.sql.Date deadline =
                            result.getDate(
                                    "deadline"
                            );


                    if (
                            deadline != null
                            && deadline
                                    .toLocalDate()
                                    .isBefore(
                                            java.time.LocalDate.now()
                                    )
                    ) {

                        sendResponse(
                                exchange,
                                400,
                                "The bidding deadline has passed."
                        );

                        return;
                    }
                }
            }


            // ==========================================
            // CHECK APPROVED VENDOR
            // ==========================================

            String vendorSQL = """
                    SELECT id
                    FROM vendors
                    WHERE id = ?
                    AND approval_status = 'APPROVED'
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    vendorSQL
                            )
            ) {

                statement.setInt(
                        1,
                        Integer.parseInt(vendorId)
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (!result.next()) {

                        sendResponse(
                                exchange,
                                403,
                                "Vendor is not approved."
                        );

                        return;
                    }
                }
            }


            // ==========================================
            // CHECK DUPLICATE BID
            // ==========================================

            String duplicateSQL = """
                    SELECT id
                    FROM bids
                    WHERE tender_id = ?
                    AND vendor_id = ?
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    duplicateSQL
                            )
            ) {

                statement.setInt(
                        1,
                        Integer.parseInt(tenderId)
                );

                statement.setInt(
                        2,
                        Integer.parseInt(vendorId)
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (result.next()) {

                        sendResponse(
                                exchange,
                                409,
                                "You have already submitted a bid for this tender."
                        );

                        return;
                    }
                }
            }


            // ==========================================
            // INSERT BID
            // ==========================================

            String insertSQL = """
                    INSERT INTO bids
                    (
                        tender_id,
                        vendor_id,
                        bid_amount,
                        proposal,
                        status
                    )
                    VALUES (?, ?, ?, ?, 'SUBMITTED')
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    insertSQL
                            )
            ) {

                statement.setInt(
                        1,
                        Integer.parseInt(tenderId)
                );

                statement.setInt(
                        2,
                        Integer.parseInt(vendorId)
                );

                statement.setBigDecimal(
                        3,
                        new java.math.BigDecimal(
                                bidAmount
                        )
                );

                statement.setString(
                        4,
                        proposal
                );

                statement.executeUpdate();
            }


            sendResponse(
                    exchange,
                    201,
                    "Bid submitted successfully."
            );


        } catch (
                NumberFormatException e
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Invalid tender ID, vendor ID or bid amount."
            );


        } catch (Exception e) {

            System.out.println(
                    "Bid submission error:"
            );

            e.printStackTrace();


            sendResponse(
                    exchange,
                    500,
                    "Server error while submitting bid."
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


    // ==========================================
    // GET BIDS
    // ==========================================

    private static void getBids(
            HttpExchange exchange
    ) throws IOException {


        /*
         * Example:
         *
         * /api/bids
         *
         * Returns all bids.
         *
         * /api/bids?vendorId=1
         *
         * Returns only vendor 1's bids.
         */


        String query =
                exchange
                        .getRequestURI()
                        .getRawQuery();


        String vendorId = null;


        if (
                query != null
                && !query.isBlank()
        ) {

            String[] parameters =
                    query.split("&");


            for (String parameter :
                    parameters) {

                String[] parts =
                        parameter.split(
                                "=",
                                2
                        );


                if (
                        parts.length == 2
                        && parts[0]
                            .equals("vendorId")
                ) {

                    vendorId =
                            URLDecoder.decode(
                                    parts[1],
                                    StandardCharsets.UTF_8
                            );

                    break;
                }
            }
        }


        String sql;


        if (
                vendorId != null
                && !vendorId.isBlank()
        ) {

            sql = """
                    SELECT
                        b.id,
                        b.tender_id,
                        b.vendor_id,
                        b.bid_amount,
                        b.proposal,
                        b.status,
                        b.submitted_at,
                        t.title AS tender_title,
                        v.company_name
                    FROM bids b
                    INNER JOIN tenders t
                        ON b.tender_id = t.id
                    INNER JOIN vendors v
                        ON b.vendor_id = v.id
                    WHERE b.vendor_id = ?
                    ORDER BY b.submitted_at DESC
                    """;

        } else {

            sql = """
                    SELECT
                        b.id,
                        b.tender_id,
                        b.vendor_id,
                        b.bid_amount,
                        b.proposal,
                        b.status,
                        b.submitted_at,
                        t.title AS tender_title,
                        v.company_name
                    FROM bids b
                    INNER JOIN tenders t
                        ON b.tender_id = t.id
                    INNER JOIN vendors v
                        ON b.vendor_id = v.id
                    ORDER BY b.submitted_at DESC
                    """;
        }


        StringBuilder json =
                new StringBuilder();


        json.append("[");


        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {


            if (
                    vendorId != null
                    && !vendorId.isBlank()
            ) {

                statement.setInt(
                        1,
                        Integer.parseInt(vendorId)
                );
            }


            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {

                boolean first = true;


                while (result.next()) {


                    if (!first) {

                        json.append(",");
                    }


                    first = false;


                    json.append("{");


                    json.append("\"id\":")
                            .append(
                                    result.getInt(
                                            "id"
                                    )
                            )
                            .append(",");


                    json.append("\"tenderId\":")
                            .append(
                                    result.getInt(
                                            "tender_id"
                                    )
                            )
                            .append(",");


                    json.append("\"vendorId\":")
                            .append(
                                    result.getInt(
                                            "vendor_id"
                                    )
                            )
                            .append(",");


                    json.append("\"tenderTitle\":\"")
                            .append(
                                    escapeJson(
                                            result.getString(
                                                    "tender_title"
                                            )
                                    )
                            )
                            .append("\",");


                    json.append("\"companyName\":\"")
                            .append(
                                    escapeJson(
                                            result.getString(
                                                    "company_name"
                                            )
                                    )
                            )
                            .append("\",");


                    json.append("\"bidAmount\":")
                            .append(
                                    result.getBigDecimal(
                                            "bid_amount"
                                    )
                            )
                            .append(",");


                    json.append("\"proposal\":\"")
                            .append(
                                    escapeJson(
                                            result.getString(
                                                    "proposal"
                                            )
                                    )
                            )
                            .append("\",");


                    // FIXED STATUS JSON
                    json.append("\"status\":\"")
                            .append(
                                    escapeJson(
                                            result.getString(
                                                    "status"
                                            )
                                    )
                            )
                            .append("\",");


                    json.append("\"submittedAt\":\"")
                            .append(
                                    escapeJson(
                                            String.valueOf(
                                                    result.getTimestamp(
                                                            "submitted_at"
                                                    )
                                            )
                                    )
                            )
                            .append("\"");


                    json.append("}");
                }
            }


            json.append("]");


            sendResponse(
                    exchange,
                    200,
                    json.toString()
            );


        } catch (
                NumberFormatException e
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Invalid vendor ID."
            );


        } catch (Exception e) {

            System.out.println(
                    "Bid database error:"
            );

            e.printStackTrace();


            sendResponse(
                    exchange,
                    500,
                    "Server error while loading bids."
            );
        }
    }


    // ==========================================
    // JSON ESCAPE
    // ==========================================

    private static String escapeJson(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }


    // ==========================================
    // RESPONSE
    // ==========================================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String message
    ) throws IOException {


        byte[] response =
                message.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
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