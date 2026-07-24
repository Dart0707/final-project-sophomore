package myhelper;

import utils.EncryptionUtil;
import utils.MySQLConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerUtil;

public class UserDAO {

    private final String cipherAlgorithm;
    private final String secretKey;
    private final String URL;
    private final String dbUser;
    private final String dbPass;
    private final String dbDriver;
    private String mySQL_URL;
    private String mySQL_user;
    private String mySQL_pass;
    private final String postgre_URL;
    private final String postgre_user;
    private final String postgre_pass;
    private static Logger logger;

    public UserDAO(String cipherAlgorithm, String secretKey, String URL, String dbUser, String dbPass,String dbDriver,String mySQL_URL, String mySQL_user, String mySQL_pass, String postgre_URL, String postgre_user, String postgre_pass) 
    {
        
        this.cipherAlgorithm = cipherAlgorithm;
        this.secretKey = secretKey;
        this.URL = URL;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.dbDriver = dbDriver;
        this.mySQL_URL = mySQL_URL;
        this.mySQL_user = mySQL_user;
        this.mySQL_pass = mySQL_pass;
        this.postgre_URL = postgre_URL;
        this.postgre_user = postgre_user;
        this.postgre_pass = postgre_pass;

        if (dbDriver != null && !dbDriver.trim().isEmpty()) {
            try {
                Class.forName(dbDriver);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("DB Driver not found: " + dbDriver, e);
            }
        }
        logger = utils.SystemLogger.setupLogger(UserDAO.class.getName(), "");
    }

    private void logAction(String level, String category, Integer userId, String source, String message) {
        LoggerUtil.log(level, category, userId, source, message, postgre_URL, postgre_user, postgre_pass);
    }
    

    public int countUsers(String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM USERS WHERE EMAIL LIKE ?";
        try (Connection conn = openDerbyConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<User> getUsers(String search, int limit, int offset) throws SQLException {
        String sql = "SELECT ID, EMAIL, USERROLE FROM USERS "
                + "WHERE LOWER(EMAIL) LIKE LOWER(?) ORDER BY EMAIL OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<User> list = new ArrayList<>();
        try (Connection conn = openDerbyConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + search.toLowerCase() + "%");
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("USERROLE")
                ));
            }
        }
        return list;
    }

    public void createUser(String username, String password, String role, Integer id) throws SQLException {
        String sql = "INSERT INTO USERS (EMAIL, PASSWORD, USERROLE) VALUES (?, ?, ?)";
        try (Connection conn = openDerbyConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String encryptedPassword;
            try {
                encryptedPassword = EncryptionUtil.encrypt(password, secretKey, cipherAlgorithm);
            } catch (Exception e) {
                throw new SQLException("Failed to encrypt password", e);
            }

            ps.setString(1, username);
            ps.setString(2, encryptedPassword);
            ps.setString(3, role);
            ps.executeUpdate();
        }
        
        logger.info("Event: User Creation ——— Description: New user account created with Email: '" + username + "' (Role: " + role + ").");
        logAction("INFO","createUser",id,"UserDAO.java","New user account created with Email: '" + username + "' (Role: " + role + ").");
    }

    public void deleteUser(int userId, int adminId) throws SQLException {
        String deleteEnrollments = "DELETE FROM enrollments WHERE student_id = ?";
        String deleteCourses = "DELETE FROM courses WHERE instructor_id = ?";
        
        try (Connection conn = MySQLConnection.getConnection(mySQL_URL,mySQL_user,mySQL_pass)) {
            try(PreparedStatement ps1 = conn.prepareStatement(deleteEnrollments)) {
                ps1.setInt(1, userId);
                ps1.executeUpdate();
            }
            try(PreparedStatement ps2 = conn.prepareStatement(deleteCourses)) {
                ps2.setInt(1, userId);
                ps2.executeUpdate();
            }
        }
        
        String sql = "DELETE FROM USERS WHERE ID = ?";
        try (Connection conn = openDerbyConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        
        logger.info("Event: User Deletion ——— Description: User ID " + userId + " permanently deleted from the system.");
        logAction("INFO","deleteUser",adminId,"UserDAO.java","User ID " + userId + " permanently deleted from the system.");
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT ID, EMAIL, USERROLE FROM USERS ORDER BY EMAIL";
        List<User> list = new ArrayList<>();
        try (Connection conn = openDerbyConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("USERROLE")
                ));
            }
        }
        return list;
    }
    
    public void updateUser(int userId, String username, String password, String role, int adminId) throws SQLException {
        // Get current user to check if they are an instructor
        User currentUser = null;
        String getCurrentUserSql = "SELECT ID, USERROLE FROM USERS WHERE ID = ?";
        try (Connection conn = openDerbyConnection();
             PreparedStatement ps = conn.prepareStatement(getCurrentUserSql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentUser = new User();
                currentUser.setId(rs.getInt("ID"));
                currentUser.setRole(rs.getString("USERROLE"));
            }
        }
        
        // If user is an instructor and role is changing away from instructor, delete their courses
        if (currentUser != null && currentUser.getRole().equalsIgnoreCase("Instructor") 
                && !role.equalsIgnoreCase("Instructor")) {
            String inactivateCoursesScript = "UPDATE courses SET status = 'INACTIVE' WHERE instructor_id = ?";
            try (Connection conn = MySQLConnection.getConnection(mySQL_URL,mySQL_user,mySQL_pass);
                 PreparedStatement deletePs = conn.prepareStatement(inactivateCoursesScript)) {
                deletePs.setInt(1, userId);
                 deletePs.executeUpdate();
             }
         }
        // If password is blank, only update username and role
        boolean updatePassword = (password != null && !password.trim().isEmpty());
        
        String sql = updatePassword 
            ? "UPDATE USERS SET EMAIL = ?, PASSWORD = ?, USERROLE = ? WHERE ID = ?"
            : "UPDATE USERS SET EMAIL = ?, USERROLE = ? WHERE ID = ?";
                
        try (Connection conn = openDerbyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            
            if (updatePassword) {
                String encryptedPassword;
                try {
                    encryptedPassword = EncryptionUtil.encrypt(password, secretKey, cipherAlgorithm);
                } catch (Exception e) {
                    throw new SQLException("Failed to encrypt password during update", e);
                }
                ps.setString(2, encryptedPassword);
                ps.setString(3, role);
                ps.setInt(4, userId);
            } else {
                ps.setString(2, role);
                ps.setInt(3, userId);
            }
            
            ps.executeUpdate();
        }
        logger.info("Event: User Update ——— Description: User ID " + userId + " successfully updated their account profile details.");
        logAction("INFO","updateUser",adminId,"UserDAO.java","User ID " + userId + " successfully updated their account profile details.");
    }

    private Connection openDerbyConnection() throws SQLException {
        return DriverManager.getConnection(URL, dbUser, dbPass);
    }
}
