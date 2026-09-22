import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TenderHandler {

    public static void handleTenders(
            HttpExchange exchange
    ) throws IOException {

        String method =
                exchange.getRequestMethod();

        // =========================
        // GET - VIEW TENDERS
        // =========================

        if (method.equalsIgnoreCase("GET")) {

            getTenders(exchange);

            return;
        }

        // =========================
        // POST - CREATE TENDER
        // =========================

        if (method.equalsIgnoreCase("POST")) {

            createTender(exchange);

            return;
        }

        sendResponse(
                exchange,
                405,
                "Method Not Allowed"
        );
    }


    // ==========================================
    // GET TENDERS
    // ==========================================

    private static void getTenders(
            HttpExchange exchange
    ) throws IOException {

        String sql = """
                SELECT
                    id,
                    title,
                    description,
                    category,
                    estimated_budget,
                    deadline,
                    status,
                    created_at
                FROM tenders
                ORDER BY created_at DESC
                """;

        StringBuilder json =
                new StringBuilder();

        json.append("[");

        try (
                Connection connection =
                        DBConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

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
                                result.getInt("id")
                        )
                        .append(",");

                json.append("\"title\":\"")
                        .append(
                                escapeJson(
                                        result.getString("title")
                                )
                        )
                        .append("\",");

                json.append("\"description\":\"")
                        .append(
                                escapeJson(
                                        result.getString("description")
                                )
                        )
                        .append("\",");

                json.append("\"category\":\"")
                        .append(
                                escapeJson(
                                        result.getString("category")
                                )
                        )
                        .append("\",");

                json.append("\"estimatedBudget\":")
                        .append(
                                result.getBigDecimal(
                                        "estimated_budget"
                                )
                        )
                        .append(",");

                json.append("\"deadline\":\"")
                        .append(
                                escapeJson(
                                        String.valueOf(
                                                result.getDate(
                                                        "deadline"
                                                )
                                        )
                                )
                        )
                        .append("\",");

                json.append("\"status\":\"")
                        .append(
                                escapeJson(
                                        result.getString("status")
                                )
                        )
                        .append("\",");

                json.append("\"createdAt\":\"")
                        .append(
                                escapeJson(
                                        String.valueOf(
                                                result.getTimestamp(
                                                        "created_at"
                                                )
                                        )
                                )
                        )
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
                    "Tender database error:"
            );

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while loading tenders."
            );
        }
    }


    // ==========================================
    // CREATE TENDER
    // ==========================================

    private static void createTender(
            HttpExchange exchange
    ) throws IOException {

        String requestBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

        String[] data =
                requestBody.split("&");

        String title = "";
        String description = "";
        String category = "";
        String estimatedBudget = "";
        String deadline = "";
        String status = "DRAFT";

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

                case "title":
                    title = value;
                    break;

                case "description":
                    description = value;
                    break;

                case "category":
                    category = value;
                    break;

                case "estimatedBudget":
                    estimatedBudget = value;
                    break;

                case "deadline":
                    deadline = value;
                    break;

                case "status":
                    status = value;
                    break;
            }
        }


        // ==========================================
        // VALIDATION
        // ==========================================

        if (
                title.isBlank()
                || description.isBlank()
                || deadline.isBlank()
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Title, description and deadline are required."
            );

            return;
        }


        if (
                !status.equals("DRAFT")
                && !status.equals("PUBLISHED")
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Invalid tender status."
            );

            return;
        }


        Connection connection = null;

        try {

            connection =
                    DBConnection.getConnection();


            String sql = """
                    INSERT INTO tenders
                    (
                        title,
                        description,
                        category,
                        estimated_budget,
                        deadline,
                        status
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql)
            ) {

                statement.setString(
                        1,
                        title
                );

                statement.setString(
                        2,
                        description
                );

                statement.setString(
                        3,
                        category
                );


                if (estimatedBudget.isBlank()) {

                    statement.setNull(
                            4,
                            java.sql.Types.DECIMAL
                    );

                } else {

                    statement.setBigDecimal(
                            4,
                            new java.math.BigDecimal(
                                    estimatedBudget
                            )
                    );
                }


                statement.setDate(
                        5,
                        java.sql.Date.valueOf(
                                deadline
                        )
                );

                statement.setString(
                        6,
                        status
                );

                statement.executeUpdate();
            }


            sendResponse(
                    exchange,
                    201,
                    "Tender created successfully."
            );


        } catch (IllegalArgumentException e) {

            sendResponse(
                    exchange,
                    400,
                    "Invalid budget or deadline format."
            );


        } catch (Exception e) {

            System.out.println(
                    "Tender creation error:"
            );

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Server error while creating tender."
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
    // JSON ESCAPE
    // ==========================================

    private static String escapeJson(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

        exchange.getResponseHeaders().set(
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