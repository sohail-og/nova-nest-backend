import java.sql.*;
import java.io.*;

public class DataExporter {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ecommerce?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "sohail";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             PrintWriter out = new PrintWriter(new FileWriter("export.sql"))) {

            System.out.println("Connected to local DB");
            
            exportTable(conn, out, "categories");
            exportTable(conn, out, "products");
            exportTable(conn, out, "productimages");

            System.out.println("Export completed to export.sql");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void exportTable(Connection conn, PrintWriter out, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
             
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            
            while (rs.next()) {
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();
                
                for (int i = 1; i <= columnCount; i++) {
                    columns.append(rsmd.getColumnName(i));
                    
                    Object value = rs.getObject(i);
                    if (value == null) {
                        values.append("NULL");
                    } else if (value instanceof String || value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                        values.append("\u0027").append(escape(value.toString())).append("\u0027");
                    } else if (value instanceof Boolean) {
                        values.append(((Boolean) value) ? "1" : "0");
                    } else {
                        values.append(value.toString());
                    }
                    
                    if (i < columnCount) {
                        columns.append(", ");
                        values.append(", ");
                    }
                }
                
                String sql = String.format("INSERT INTO %s (%s) VALUES (%s);", tableName, columns.toString(), values.toString());
                out.println(sql);
            }
        }
    }
    
    private static String escape(String s) {
        if (s == null) return "NULL";
        return s.replace("\u0027", "\u0027\u0027").replace("\\", "\\\\");
    }
}
