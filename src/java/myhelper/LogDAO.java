package myhelper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerUtil;
import myhelper.LogEntry;

public class LogDAO {

    private static final String URL = "jdbc:postgresql://localhost:5432/active_learning_logs";
    private static final String USER = "postgres";
    private static final String PASSWORD = "app";
    private final String postgre_URL;
    private final String postgre_user;
    private final String postgre_pass;
    private static final Logger logger = utils.SystemLogger.setupLogger(LogDAO.class.getName(), "");
    
    public LogDAO(String postgre_URL, String postgre_user, String postgre_pass) {
        this.postgre_URL = postgre_URL;
        this.postgre_user = postgre_user;
        this.postgre_pass = postgre_pass;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL JDBC Driver not found.", e);
        }
    }

    private void logAction(String level, String category, Integer userId, String source, String message) {
        LoggerUtil.log(level, category, userId, source, message, postgre_URL, postgre_user, postgre_pass);
    }

    public List<LogEntry> getLogs(String startDate, String endDate, int userid) throws SQLException {
        List<LogEntry> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM action_logs");
        List<Date> parameters = new ArrayList<>();

        boolean hasStartDate = startDate != null && !startDate.trim().isEmpty();
        boolean hasEndDate = endDate != null && !endDate.trim().isEmpty();

        if (hasStartDate && hasEndDate) {
            sql.append(" WHERE log_timestamp::date BETWEEN ? AND ?");
            parameters.add(Date.valueOf(startDate));
            parameters.add(Date.valueOf(endDate));
        } else if (hasStartDate) {
            sql.append(" WHERE log_timestamp::date >= ?");
            parameters.add(Date.valueOf(startDate));
        } else if (hasEndDate) {
            sql.append(" WHERE log_timestamp::date <= ?");
            parameters.add(Date.valueOf(endDate));
        }

        sql.append(" ORDER BY log_timestamp DESC, log_id DESC");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setDate(i + 1, parameters.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogEntry log = new LogEntry();
                    log.setLogId(rs.getInt("log_id"));
                    log.setLogTimestamp(rs.getTimestamp("log_timestamp"));
                    log.setLogLevel(rs.getString("log_level"));
                    log.setCategory(rs.getString("category"));

                    int userId = rs.getInt("user_id");
                    log.setUserId(rs.wasNull() ? null : userId);

                    log.setSource(rs.getString("source"));
                    log.setMessage(rs.getString("message"));
                    logs.add(log);
                }
            }
        }
        logAction("INFO","Logs",userid,"LogDAO.java","Logs viewed by user"+ userid);
        logger.info("Event: Log Retrieval ——— Description: Logs viewed by user" + userid);
        return logs;
    }
}