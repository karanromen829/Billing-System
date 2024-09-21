package javaProject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/PlaceOrderServlet")
public class PlaceOrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html><head><style>");
        out.println("body { font-family: 'Arial', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }");
        out.println("h2 { color: #333; }");
        out.println("p { color: #555; }");
        out.println("ul { list-style-type: none; padding: 0; }");
        out.println("li { border-bottom: 1px solid #ddd; margin-bottom: 10px; padding-bottom: 10px; }");
        out.println("</style></head><body>");

        out.println("<h2>Order Confirmation</h2>");

        String[] selectedItems = request.getParameterValues("item");

        if (selectedItems != null && selectedItems.length > 0) {
            double totalPrice = 0.0;
            UUID transactionId = UUID.randomUUID(); // Generating a unique transaction_id

            // Retrieve customer name and phone number from the request
            String customerName = request.getParameter("customerName");
            String phoneNumber = request.getParameter("phoneNumber");

            out.println("<p>Customer Name: " + customerName + "</p>");
            out.println("<p>Phone Number: " + phoneNumber + "</p>");
            out.println("<p>Your order (Transaction ID: " + transactionId + "):</p>");
            out.println("<ul>");

            try {
                // Load the JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Establish a connection
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/javaProject",
                        "root", "system");

                // Create order table if not exists
                createOrderTable(connection);

                // Insert customer information into the database
                saveCustomerInfo(connection, transactionId.toString(), customerName, phoneNumber);

                // Insert order data into the database
                for (String itemName : selectedItems) {
                    int quantity = Integer.parseInt(request.getParameter("quantity_" + itemName));
                    double price = getPrice(connection, itemName);
                    double itemTotal = price * quantity;
                    totalPrice += itemTotal;

                    // Save order data to the database
                    saveOrderData(connection, itemName, price, quantity, transactionId.toString());
                }

                // Display order items and total cost
                out.println("<ul>");
                for (String itemName : selectedItems) {
                    int quantity = Integer.parseInt(request.getParameter("quantity_" + itemName));
                    double price = getPrice(connection, itemName);
                    double itemTotal = price * quantity;

                    out.println("<li>" + itemName + " - Quantity: " + quantity + " - Total: $" + itemTotal + "</li>");
                }
                out.println("</ul>");
                out.println("<p>Total Price: $" + totalPrice + "</p>");

                // Close the connection
                connection.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("<p>No items selected.</p>");
        }

        out.println("</body></html>");
    }

    private double getPrice(Connection connection, String itemName) throws SQLException {
        String query = "SELECT price FROM items WHERE itemName = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, itemName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("price");
                }
            }
        }
        return 0.0;
    }

    private void saveOrderData(Connection connection, String itemName, double price, int quantity, String transactionId) throws SQLException {
        String insertOrderQuery = "INSERT INTO `order` (item, price, quantity, transaction_id) VALUES (?, ?, ?, ?)";
        String updateQuantityQuery = "UPDATE items SET availableQuantity = availableQuantity - ? WHERE itemName = ?";

        try (PreparedStatement insertOrderStatement = connection.prepareStatement(insertOrderQuery);
             PreparedStatement updateQuantityStatement = connection.prepareStatement(updateQuantityQuery)) {

            // Save order data
            insertOrderStatement.setString(1, itemName);
            insertOrderStatement.setDouble(2, price);
            insertOrderStatement.setInt(3, quantity);
            insertOrderStatement.setString(4, transactionId);
            insertOrderStatement.executeUpdate();

            // Update availableQuantity
            updateQuantityStatement.setInt(1, quantity);
            updateQuantityStatement.setString(2, itemName);
            updateQuantityStatement.executeUpdate();
        }
    }

    private void saveCustomerInfo(Connection connection, String transactionId, String customerName, String phoneNumber) throws SQLException {
        String insertCustomerInfoQuery = "INSERT INTO `order_customer_info` (transaction_id, customer_name, phone_number) VALUES (?, ?, ?)";

        try (PreparedStatement insertCustomerInfoStatement = connection.prepareStatement(insertCustomerInfoQuery)) {
            insertCustomerInfoStatement.setString(1, transactionId);
            insertCustomerInfoStatement.setString(2, customerName);
            insertCustomerInfoStatement.setString(3, phoneNumber);
            insertCustomerInfoStatement.executeUpdate();
        }
    }

    private void createOrderTable(Connection connection) throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS `order` ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "item VARCHAR(255) NOT NULL, "
                + "price DOUBLE NOT NULL, "
                + "quantity INT NOT NULL, "
                + "transaction_id VARCHAR(36) NOT NULL)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
        }

        // Create order_customer_info table if not exists
        String createCustomerInfoTableQuery = "CREATE TABLE IF NOT EXISTS `order_customer_info` ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "transaction_id VARCHAR(36) NOT NULL, "
                + "customer_name VARCHAR(255) NOT NULL, "
                + "phone_number VARCHAR(15) NOT NULL)";
        try (PreparedStatement customerInfoStatement = connection.prepareStatement(createCustomerInfoTableQuery)) {
            customerInfoStatement.executeUpdate();
        }
    }
}
