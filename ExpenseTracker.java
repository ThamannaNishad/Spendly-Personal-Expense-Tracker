import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class ExpenseTracker {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/expense_tracker";
        String user = System.getenv("EXPENSE_DB_USER");
        String password = System.getenv("EXPENSE_DB_PASSWORD");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(
                url, user, password
            );

            System.out.println("MySQL Connected Successfully!");

            String sql = "INSERT INTO transactions (title, amount, category, type) VALUES (?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "Test Expense");
            ps.setBigDecimal(2, new java.math.BigDecimal("100.00"));
            ps.setString(3, "Food");
            ps.setString(4, "Expense");

            int rows = ps.executeUpdate();

            System.out.println(rows + " transaction saved!");

            ps.close();
            con.close();
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver Not Found!");
            e.printStackTrace();

        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
        }
    }
}