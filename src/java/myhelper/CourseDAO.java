package myhelper;

import utils.MySQLConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerUtil;

/**
 * Data Access Object for all course and enrollment database operations.
 */
public class CourseDAO {

    private static String derby_url;
    private static String derby_user;
    private static String derby_pass;
    private static String mysql_url;
    private static String mysql_user;
    private static String mysql_pass;
    private final String postgre_URL;
    private final String postgre_user;
    private final String postgre_pass;
    private static final Logger logger = utils.SystemLogger.setupLogger(CourseDAO.class.getName(), "");

    ;

    static {
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Derby JDBC Driver not found: " + e.getMessage());
        }
    }
    
    public CourseDAO(String DERBY_URL, String DERBY_USER, String DERBY_PASS, String mysql_url, String mysql_user, String mysql_pass, String postgre_URL, String postgre_user, String postgre_pass) {
        this.derby_url = DERBY_URL;
        this.derby_user = DERBY_USER;
        this.derby_pass = DERBY_PASS;
        this.mysql_url = mysql_url;
        this.mysql_user=mysql_user;
        this.mysql_pass=mysql_pass;
        this.postgre_URL = postgre_URL;
        this.postgre_user = postgre_user;
        this.postgre_pass = postgre_pass;
    }

    private void logAction(String level, String category, Integer userId, String source, String message) {
        LoggerUtil.log(level, category, userId, source, message, postgre_URL, postgre_user, postgre_pass);
    }

    // ── Instructor queries ────────────────────────────────────────────────────────
    /**
     * Counts courses owned by the given instructor that match the search term.
     *
     * @param instructorId the instructor's user id
     * @param search partial title filter
     * @return total row count
     * @throws SQLException on DB error
     */
    public int countInstructorCourses(int instructorId, String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM courses "
                + "WHERE instructor_id = ? AND title LIKE ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            ps.setString(2, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Returns one page of courses for the given instructor.
     *
     * @param instructorId the instructor's user id
     * @param search partial title filter
     * @param limit page size
     * @param offset row offset
     * @return list of {@link Course} objects with participantCount and status
     * populated
     * @throws SQLException on DB error
     */
    public List<Course> getInstructorCourses(int instructorId, String search,
            String sortBy, int limit, int offset) throws SQLException {
        String orderBy = buildInstructorOrderBy(sortBy);
        String sql
                = "SELECT c.id, c.title, c.course_date, c.status, COUNT(e.id) AS participants "
                + "FROM courses c LEFT JOIN enrollments e ON e.course_id = c.id "
                + "WHERE c.instructor_id = ? AND c.title LIKE ? "
                + "GROUP BY c.id " + orderBy + " LIMIT ? OFFSET ?";

        List<Course> list = new ArrayList<>();
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            ps.setString(2, "%" + search + "%");
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Course c = new Course();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setCourseDate(rs.getString("course_date"));
                c.setStatus(rs.getString("status"));
                c.setParticipantCount(rs.getInt("participants"));
                list.add(c);
            }
        }
        return list;
    }

    /**
     * Returns the course row for the given id.
     *
     * @param courseId the course id
     * @return course metadata or {@code null} when missing
     * @throws SQLException on DB error
     */
    public Course getCourseById(int courseId) throws SQLException {
        String sql = "SELECT id, title, course_date, instructor_id, status FROM courses WHERE id = ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Course course = new Course();
                course.setId(rs.getInt("id"));
                course.setTitle(rs.getString("title"));
                course.setCourseDate(rs.getString("course_date"));
                course.setInstructorId(rs.getInt("instructor_id"));
                course.setStatus(rs.getString("status"));
                return course;
            }
        }
        return null;
    }

    /**
     * Returns all enrolled participants for the given course.
     *
     * @param courseId the course id
     * @return list of users enrolled in that course
     * @throws SQLException on DB error
     */
    public List<User> getCourseParticipants(int courseId) throws SQLException {
        List<User> list = new ArrayList<>();
        String enrollmentSql = "SELECT student_id FROM enrollments WHERE course_id = ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(enrollmentSql)) {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = getUserByIdFromDerby(rs.getInt("student_id"));
                if (user != null) {
                    list.add(user);
                }
            }
        }
        list.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /**
     * Returns enrolled participants for the given course filtered by enrollment
     * date range. Filters by the enrolled_at column to show only participants
     * who registered within the date range.
     *
     * @param courseId the course id
     * @param startDate the start date (format: yyyy-MM-dd)
     * @param endDate the end date (format: yyyy-MM-dd)
     * @return list of users enrolled within the date range
     * @throws SQLException on DB error
     */
    public List<User> getCourseParticipantsByDateRange(int courseId, String startDate, String endDate)
            throws SQLException {
        List<User> list = new ArrayList<>();
        String sql
                = "SELECT student_id FROM enrollments "
                + "WHERE course_id = ? "
                + "  AND enrolled_at >= ? "
                + "  AND enrolled_at < ?";

        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setString(2, startDate);
            ps.setString(3, endDate + " 23:59:59");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = getUserByIdFromDerby(rs.getInt("student_id"));
                if (user != null) {
                    list.add(user);
                }
            }
        }
        list.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /**
     * Returns enrolled participants for the given course filtered by a single
     * enrollment day.
     *
     * @param courseId the course id
     * @param selectedDate the selected date (format: yyyy-MM-dd)
     * @return list of users enrolled on that day
     * @throws SQLException on DB error
     */
    public List<User> getCourseParticipantsByEnrollmentDay(int courseId, String selectedDate)
            throws SQLException {
        List<User> list = new ArrayList<>();
        String sql
                = "SELECT student_id FROM enrollments "
                + "WHERE course_id = ? "
                + "  AND created_at >= ? "
                + "  AND created_at < ?";

        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setString(2, selectedDate + " 00:00:00");
            ps.setString(3, selectedDate + " 23:59:59");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = getUserByIdFromDerby(rs.getInt("student_id"));
                if (user != null) {
                    list.add(user);
                }
            }
        }
        list.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /**
     * Inserts a new course owned by the given instructor with ACTIVE status.
     *
     * @param title course title
     * @param courseDate date string (yyyy-MM-dd)
     * @param instructorId the instructor's user id
     * @throws SQLException on DB error
     */
    public void createCourse(String title, String courseDate, int instructorId)
            throws SQLException {
        String sql = "INSERT INTO courses (title, course_date, instructor_id, status) VALUES (?, ?, ?, 'ACTIVE')";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, courseDate);
            ps.setInt(3, instructorId);
            ps.executeUpdate();
        }
        logger.info("Event: Course Creation ——— Description: New course created with title: '" + title);
        logAction("INFO", "createCourse", instructorId, "CourseDAO.java", "New course created with title: '" + title);
    }

    /**
     * Toggles a course status between ACTIVE and INACTIVE. Only allows toggling
     * if the instructor owns the course. Keeps all participants enrolled during
     * inactivation.
     *
     * @param courseId the course id
     * @param instructorId the instructor's user id (ownership check)
     * @throws SQLException on DB error
     */
    public void toggleCourseStatus(int courseId, int instructorId) throws SQLException {
        String sql = "UPDATE courses SET status = IF(status = 'ACTIVE', 'INACTIVE', 'ACTIVE') "
                + "WHERE id = ? AND instructor_id = ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, instructorId);
            ps.executeUpdate();
        }
    }

    public void updateEndedCoursesStatus() throws SQLException {
        String sql = "UPDATE courses SET status = 'ENDED' WHERE course_date < CURRENT_DATE AND status = 'ACTIVE'";

        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();
        }
    }

    // ── Student queries ───────────────────────────────────────────────────────────
    /**
     * Counts all courses available to students matching the search term.
     *
     * @param search partial title filter
     * @return total row count
     * @throws SQLException on DB error
     */
    public int countAllCourses(String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM courses c WHERE c.title LIKE ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Returns one page of all available courses, with enrollment flag for the
     * given student.
     *
     * @param studentId the student's user id
     * @param search partial title filter
     * @param limit page size
     * @param offset row offset
     * @return list of {@link Course} objects with instructorName, enrolled
     * flag, and status populated
     * @throws SQLException on DB error
     */
    public List<Course> getAllCoursesForStudent(int studentId, String search,
            String sortBy, int limit, int offset) throws SQLException {
        String orderBy = buildStudentOrderBy(sortBy);
        String sql
                = "SELECT c.id, c.title, c.course_date, c.status, c.instructor_id, "
                + "       (SELECT COUNT(*) FROM enrollments e2 "
                + "        WHERE e2.course_id = c.id AND e2.student_id = ?) AS enrolled "
                + "FROM courses c "
                + "WHERE c.title LIKE ? " + orderBy + " LIMIT ? OFFSET ?";

        List<Course> list = new ArrayList<>();
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, "%" + search + "%");
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Course c = new Course();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setCourseDate(rs.getString("course_date"));
                c.setStatus(rs.getString("status"));
                User instructor = getUserByIdFromDerby(rs.getInt("instructor_id"));
                c.setInstructorName(instructor == null ? null : instructor.getUsername());
                c.setEnrolled(rs.getInt("enrolled") > 0);
                list.add(c);
            }
        }
        return list;
    }

    private String buildInstructorOrderBy(String sortBy) {
        if (sortBy == null) {
            sortBy = "participants";
        }

        switch (sortBy) {
            case "name":
                return "ORDER BY c.title ASC, c.id ASC";
            case "status":
                return "ORDER BY CASE WHEN c.status = 'ACTIVE' THEN 0 WHEN c.status = 'INACTIVE' THEN 1 ELSE 2 END ASC, c.title ASC, c.id ASC";
            case "participants":
            default:
                return "ORDER BY COUNT(e.id) DESC, c.title ASC, c.id ASC";
        }
    }

    private String buildStudentOrderBy(String sortBy) {
        if (sortBy == null) {
            sortBy = "name";
        }

        switch (sortBy) {
            case "status":
                return "ORDER BY CASE WHEN c.status = 'ACTIVE' THEN 0 WHEN c.status = 'INACTIVE' THEN 1 ELSE 2 END ASC, c.title ASC, c.id ASC";
            case "name":
            default:
                return "ORDER BY c.title ASC, c.id ASC";
        }
    }

    // ── Enrollment operations ─────────────────────────────────────────────────────
    /**
     * Enrolls a student in a course. Silently skips if already enrolled.
     *
     * @param studentId the student's user id
     * @param courseId the course id
     * @return {@code true} if a new row was inserted, {@code false} if already
     * enrolled
     * @throws SQLException on DB error
     */
    public boolean enroll(int studentId, int courseId) throws SQLException {
        // Duplicate-check
        String chkSql = "SELECT id FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement chk = conn.prepareStatement(chkSql)) {
            chk.setInt(1, studentId);
            chk.setInt(2, courseId);
            if (chk.executeQuery().next()) {
                return false; // already enrolled
            }
        }

        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        }
        logger.info("Event: Enrollment ——— Description: Student" + studentId + " enrolled into course with id: " + courseId);
        logAction("INFO", "enroll", studentId, "CourseDAO.java", "Student" + studentId + " enrolled into course with id: " + courseId);
        return true;
    }

    /**
     * Removes a student's enrollment from a course.
     *
     * @param studentId the student's user id
     * @param courseId the course id
     * @throws SQLException on DB error
     */
    public void unenroll(int studentId, int courseId) throws SQLException {
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        }
        logger.info("Event: Unenrollment ——— Description: Student" + studentId + " dropped course with id: " + courseId);
        logAction("INFO", "unenroll", studentId, "CourseDAO.java", "Student" + studentId + " dropped course with id: " + courseId);
    }

    public void updateCourse(int courseId, String title, String courseDate, int instructorId) throws SQLException {
        // check instructorId to make sure an instructor can only edit their own course
        String sql = "UPDATE courses SET title = ?, course_date = ? WHERE id = ? AND instructor_id = ?";

        try (Connection conn = MySQLConnection.getConnection(mysql_url,mysql_user,mysql_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, courseDate);
            ps.setInt(3, courseId);
            ps.setInt(4, instructorId);

            ps.executeUpdate();
        }
        logger.info("Event: Course Update ——— Description: Course title: '" + title + " updated.");
        logAction("INFO", "updateCourse", instructorId, "CourseDAO.java", "Course title: '" + title + " updated.");
    }

    private User getUserByIdFromDerby(int userId) throws SQLException {
        String sql = "SELECT ID, EMAIL, USERROLE FROM USERS WHERE ID = ?";
        try (Connection conn = DriverManager.getConnection(derby_url, derby_user, derby_pass);
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("USERROLE")
                );
            }
        }
        return null;
    }
}
