import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TruncateDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ecommerce", "root", "sohail");
            Statement stmt = conn.createStatement();
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
            stmt.execute("TRUNCATE TABLE products;");
            stmt.execute("TRUNCATE TABLE categories;");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            System.out.println("Truncated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
