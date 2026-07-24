package utils;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
/**
 * Utility class for obtaining a MySQL database connection.
 * Update DB_URL, DB_USER, and DB_PASS to match your environment.
 */
public class MySQLConnection {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }
 
    /**
     * Returns a new connection to the active_learning database.
     *
     * @return a live {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection(String DB_URL, String DB_USER, String DB_PASS) throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
 
    // Prevent instantiation
    private MySQLConnection() {}
}