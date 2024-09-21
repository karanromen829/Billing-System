package javaProject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/FetchItemsServlet")
public class FetchItemsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish a connection
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/javaProject",
                    "root", "system");

            // Fetch items from the database
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM items");

            // Display items and customer information form in HTML
            out.println("<html><head>");
            out.println("<style>");
            out.println("body { font-family: 'Arial', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }");
            out.println("h2 { color: #333; }");
            out.println("form { max-width: 600px; margin: 50px auto; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); }");
            out.println("input[type='text'], input[type='number'] { width: 100%; padding: 10px; margin: 8px 0; display: inline-block; border: 1px solid #ccc; box-sizing: border-box; border-radius: 4px; }");
            out.println("input[type='submit'] { background-color: #4caf50; color: white; padding: 14px 20px; margin: 8px 0; border: none; border-radius: 4px; cursor: pointer; }");
            out.println("input[type='submit']:hover { background-color: #45a049; }");
            out.println("</style>");
            out.println("</head><body>");

            out.println("<h2><center>Available Items</center></h2>");
            out.println("<form action='PlaceOrderServlet' method='post'>");

            // Add input fields for customer name and phone number
            out.println("Customer Name: <input type='text' name='customerName' required><br>");
            out.println("Phone Number: <input type='text' name='phoneNumber' required pattern='\\d{10}' title='Please enter a 10-digit phone number'><br>");

            while (resultSet.next()) {
                String itemName = resultSet.getString("itemName");
                double price = resultSet.getDouble("price");
                int availableQuantity = resultSet.getInt("availableQuantity");

                out.println("<input type='checkbox' name='item' value='" + itemName + "' "
                        + (availableQuantity > 0 ? "" : "disabled") + ">" + itemName + " - $" + price
                        + " (Price: $" + price + ") Available Quantity: " + availableQuantity);

                if (availableQuantity > 0) {
                    out.println(" Quantity: <input type='number' name='quantity_" + itemName + "' min='1' "
                            + "max='" + availableQuantity + "' oninput='validateQuantity(this, " + availableQuantity + ")'><br>");
                } else {
                    out.println(" Quantity: <input type='number' name='quantity_" + itemName + "' min='1' disabled><br>");
                }
            }

            out.println("<br><input type='submit' value='Place Order'>");
            out.println("</form>");

            // Change "Update Items" link to a button
            out.println("<form action='UpdateItemsServlet'>");
            out.println("<input type='submit' value='Update Items'>");
            out.println("</form>");

            out.println("<script>"
                    + "function validateQuantity(input, max) {"
                    + "   if (parseInt(input.value) > max) {"
                    + "       input.setCustomValidity('Quantity cannot be greater than available quantity.');"
                    + "   } else {"
                    + "       input.setCustomValidity('');"
                    + "   }"
                    + "}"
                    + "</script>"
                    + "</body></html>");

            // Close the connection
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
