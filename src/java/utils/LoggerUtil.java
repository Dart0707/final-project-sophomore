package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoggerUtil {


    public static void log(String level, String category, Integer userId, String source, String message, String postgre_URL, String postgre_user, String postgre_pass) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        String sql = "INSERT INTO action_logs (log_level, category, user_id, source, message) VALUES (?, ?, ?, ?, ?)";

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(postgre_URL, postgre_user, postgre_pass);
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, level);
            pstmt.setString(2, category);
            
            if (userId != null) {
                pstmt.setInt(3, userId);
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            
            pstmt.setString(4, source);
            pstmt.setString(5, message);

            pstmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Failed to write system action log!");
            e.printStackTrace();
        } finally {
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
    }
}