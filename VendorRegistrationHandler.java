import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class VendorRegistrationHandler {

    public static void handleRegistration(
            HttpExchange exchange
    ) throws IOException {

        // Only POST requests are allowed
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        // Read form data
        String requestBody =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );


        String[] data =
                requestBody.split("&");


        String companyName = "";
        String contactPerson = "";
        String email = "";
        String phone = "";
        String address = "";
        String password = "";


        // Read each form field
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

                case "companyName":
                    companyName = value;
                    break;

                case "contactPerson":
                    contactPerson = value;
                    break;

                case "email":
                    email = value;
                    break;

                case "phone":
                    phone = value;
                    break;

                case "address":
                    address = value;
                    break;

                case "password":
                    password = value;
                    break;
            }
        }


        // Basic validation
        if (
                companyName.isBlank()
                || contactPerson.isBlank()
                || email.isBlank()
                || password.isBlank()
        ) {

            sendResponse(
                    exchange,
                    400,
                    "Please fill in all required fields."
            );

            return;
        }


        Connection connection = null;


        try {

            connection =
                    DBConnection.getConnection();


            // Check whether email already exists
            String checkSQL =
                    "SELECT id FROM users WHERE email = ?";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(checkSQL)
            ) {

                statement.setString(
                        1,
                        email
                );


                try (
                        ResultSet result =
                                statement.executeQuery()
                ) {

                    if (result.next()) {

                        sendResponse(
                                exchange,
                                409,
                                "An account with this email already exists."
                        );

                        return;
                    }
                }
            }


            // Start transaction
            connection.setAutoCommit(false);


            // Insert user account
            String userSQL = """
                    INSERT INTO users
                    (email, password, role, status)
                    VALUES (?, ?, 'VENDOR', 'PENDING')
                    """;


            int userId;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    userSQL,
                                    java.sql.Statement.RETURN_GENERATED_KEYS
                            )
            ) {

                statement.setString(
                        1,
                        email
                );

                statement.setString(
                        2,
                        password
                );


                statement.executeUpdate();


                try (
                        ResultSet generatedKeys =
                                statement.getGeneratedKeys()
                ) {

                    if (!generatedKeys.next()) {

                        throw new Exception(
                                "Unable to create user account."
                        );
                    }


                    userId =
                            generatedKeys.getInt(1);
                }
            }


            // Insert vendor information
            String vendorSQL = """
                    INSERT INTO vendors
                    (
                        user_id,
                        company_name,
                        contact_person,
                        phone,
                        address,
                        approval_status
                    )
                    VALUES (?, ?, ?, ?, ?, 'PENDING')
                    """;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    vendorSQL
                            )
            ) {

                statement.setInt(
                        1,
                        userId
                );

                statement.setString(
                        2,
                        companyName
                );

                statement.setString(
                        3,
                        contactPerson
                );

                statement.setString(
                        4,
                        phone
                );

                statement.setString(
                        5,
                        address
                );


                statement.executeUpdate();
            }


            // Save transaction
            connection.commit();


            sendResponse(
                    exchange,
                    201,
                    "Registration successful! Your vendor account is waiting for admin approval."
            );


        } catch (Exception e) {

            // Rollback if something goes wrong
            if (connection != null) {

                try {
                    connection.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
            }


            System.out.println(
                    "Vendor registration error:"
            );

            e.printStackTrace();


            sendResponse(
                    exchange,
                    500,
                    "Server error while registering vendor."
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