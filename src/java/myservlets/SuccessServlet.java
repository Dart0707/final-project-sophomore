package myservlets;

import myhelper.CourseDAO;
import myhelper.UserDAO;
import myhelper.Course;
import myhelper.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletConfig;

/**
 * Controller servlet for the main dashboard page (success.jsp).
 *
 * Handles all three roles — Admin, Instructor, Student — by: GET → fetching the
 * right data and forwarding to success.jsp for rendering POST → processing the
 * submitted action, setting a flash message, then redirecting back to GET
 * (Post-Redirect-Get pattern prevents duplicate submissions)
 *
 * URL mapping: /success (registered via @WebServlet or in web.xml)
 */
@WebServlet("/success")
public class SuccessServlet extends HttpServlet {

    private String cipherAlgorithm;
    private String secretKey;
    private String URL;
    private String dbUser;
    private String dbPass;
    private String dbDriver;
    private String mySQL_URL;
    private String mySQL_pass;
    private String mySQL_user;
    private String postgre_URL;
    private String postgre_pass;
    private String postgre_user;
    private static final int PAGE_SIZE = 6;
    private UserDAO userDAO;
    private CourseDAO courseDAO;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        cipherAlgorithm = config.getServletContext().getInitParameter("cipherAlgorithm");
        secretKey = config.getServletContext().getInitParameter("secretKey");
        // Prefer servlet init params, fall back to context params if missing
        URL = config.getInitParameter("URL");
        if (URL == null) URL = config.getServletContext().getInitParameter("URL");
        dbUser = config.getInitParameter("dbUser");
        if (dbUser == null) dbUser = config.getServletContext().getInitParameter("dbUser");
        dbPass = config.getInitParameter("dbPass");
        if (dbPass == null) dbPass = config.getServletContext().getInitParameter("dbPass");
        dbDriver = config.getInitParameter("dbDriver");
        if (dbDriver == null) dbDriver = config.getServletContext().getInitParameter("dbDriver");
        mySQL_URL = getServletContext().getInitParameter("mySQLURL");
        mySQL_pass = getServletContext().getInitParameter("mySQLPass");
        mySQL_user = getServletContext().getInitParameter("mySQLUser");
        postgre_URL = getServletContext().getInitParameter("postgreURL");
        postgre_pass = getServletContext().getInitParameter("postgrePass");
        postgre_user = getServletContext().getInitParameter("postgreUser");
        userDAO = new UserDAO(cipherAlgorithm, secretKey,URL,dbUser,dbPass,dbDriver,mySQL_URL,mySQL_user,mySQL_pass,postgre_URL,postgre_user,postgre_pass);
        courseDAO = new CourseDAO(URL, dbUser, dbPass,mySQL_URL,mySQL_user,mySQL_pass,postgre_URL,postgre_user,postgre_pass);
        
    }

    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
    }

    // ── GET — load data and forward to JSP ───────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);

        // ── Session guard ────────────────────────────────────────────────────────
        if (session == null
                || session.getAttribute("username") == null
                || session.getAttribute("role") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        String loggedRole = (String) session.getAttribute("role");
        Integer loggedId = (Integer) session.getAttribute("userId");

        // ── Pagination & search params ───────────────────────────────────────────
        String search = req.getParameter("search");
        if (search == null) {
            search = "";
        }

        String sortBy = normalizeSortBy(loggedRole, req.getParameter("sortBy"));

        int currentPage = parsePage(req.getParameter("page"));
        int offset = (currentPage - 1) * PAGE_SIZE;

        // ── Flash message from a previous POST ──────────────────────────────────
        String flash = (String) session.getAttribute("flashMsg");
        if (flash != null) {
            req.setAttribute("msg", flash);
            session.removeAttribute("flashMsg");
        }

        // ── Fetch role-specific data ─────────────────────────────────────────────
        int totalCount = 0;
        int totalPages = 1;
        try {
            
            courseDAO.updateEndedCoursesStatus();
            
            if ("Admin".equals(loggedRole)) {
                totalCount = userDAO.countUsers(search);
                List<User> users = userDAO.getUsers(search, PAGE_SIZE, offset);
                req.setAttribute("users", users);

            } else if ("Instructor".equals(loggedRole)) {
                totalCount = courseDAO.countInstructorCourses(loggedId, search);
                List<Course> courses = courseDAO.getInstructorCourses(
                        loggedId, search, sortBy, PAGE_SIZE, offset);
                req.setAttribute("courses", courses);

            } else { // Student
                totalCount = courseDAO.countAllCourses(search);
                List<Course> courses = courseDAO.getAllCoursesForStudent(
                        loggedId, search, sortBy, PAGE_SIZE, offset);
                req.setAttribute("courses", courses);
            }

            totalPages = (totalCount == 0) ? 1
                    : (int) Math.ceil((double) totalCount / PAGE_SIZE);

        } catch (SQLException e) {
            req.setAttribute("msg", "Database error: " + e.getMessage());
        }

        req.setAttribute("search", search);
        req.setAttribute("sortBy", sortBy);
        req.setAttribute("currentPage", currentPage);
        req.setAttribute("totalPages", totalPages);

        req.getRequestDispatcher("/success.jsp").forward(req, resp);
    }

    // ── POST — handle actions and redirect (PRG pattern) ─────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);

        // ── Session guard ────────────────────────────────────────────────────────
        if (session == null
                || session.getAttribute("username") == null
                || session.getAttribute("role") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        String loggedRole = (String) session.getAttribute("role");
        Integer loggedId = (Integer) session.getAttribute("userId");
        String action = req.getParameter("action");
        String flashMsg = "";

        try {

            // ── ADMIN ACTIONS ────────────────────────────────────────────────────
            if ("Admin".equals(loggedRole)) {

                if ("createUser".equals(action)) {
                    String newUsername = req.getParameter("newUsername");
                    String newPassword = req.getParameter("newPassword");
                    String newRole = req.getParameter("newRole");
                    userDAO.createUser(newUsername, newPassword, newRole,loggedId);
                    flashMsg = "User \"" + newUsername + "\" created successfully.";

                } else if ("deleteUser".equals(action)) {
                    int userId = Integer.parseInt(req.getParameter("userId"));
                    userDAO.deleteUser(userId,loggedId);
                    
                    flashMsg = "User deleted.";
                } else if ("updateUser".equals(action)) {
                    int editUserId = Integer.parseInt(req.getParameter("editUserId"));
                    String editUsername = req.getParameter("editUsername");
                    String editPassword = req.getParameter("editPassword");
                    String editRole = req.getParameter("editRole");
                    
                    userDAO.updateUser(editUserId, editUsername, editPassword, editRole,loggedId);
                    flashMsg = "User \"" + editUsername + "\" updated successfully.";
                }

                // ── INSTRUCTOR ACTIONS ───────────────────────────────────────────────
            } else if ("Instructor".equals(loggedRole)) {

                if ("createCourse".equals(action)) {
                    String title = req.getParameter("courseTitle");
                    String courseDate = req.getParameter("courseDate");
                    courseDAO.createCourse(title, courseDate, loggedId);
                    flashMsg = "Course \"" + title + "\" created.";

                } else if ("deleteCourse".equals(action)) {
                    int courseId = Integer.parseInt(req.getParameter("courseId"));
                    courseDAO.toggleCourseStatus(courseId, loggedId);
                    flashMsg = "Course status toggled.";
                    
                } else if ("updateCourse".equals(action)) {
                    int editCourseId = Integer.parseInt(req.getParameter("editCourseId"));
                    String editTitle = req.getParameter("editCourseTitle");
                    String editDate = req.getParameter("editCourseDate");
                    
                    courseDAO.updateCourse(editCourseId, editTitle, editDate, loggedId);
                    flashMsg = "Course updated successfully.";
                }

                // ── STUDENT ACTIONS ──────────────────────────────────────────────────
            } else if ("Student".equals(loggedRole)) {

                if ("enroll".equals(action)) {
                    int courseId = Integer.parseInt(req.getParameter("courseId"));
                    boolean inserted = courseDAO.enroll(loggedId, courseId);
                    flashMsg = inserted ? "Enrolled successfully!" : "Already enrolled.";

                } else if ("unenroll".equals(action)) {
                    int courseId = Integer.parseInt(req.getParameter("courseId"));
                    courseDAO.unenroll(loggedId, courseId);
                    flashMsg = "Unenrolled.";
                }
            }

        } catch (NumberFormatException e) {
            flashMsg = "Invalid input: " + e.getMessage();
        } catch (SQLException e) {
            flashMsg = "Database error: " + e.getMessage();
        }

        // Store flash message in session so it survives the redirect
        session.setAttribute("flashMsg", flashMsg);

        // Preserve search/page state across the redirect
        String search = req.getParameter("search");
        if (search == null) {
            search = "";
        }
        String page = req.getParameter("page");
        if (page == null) {
            page = "1";
        }
        String sortBy = normalizeSortBy(loggedRole, req.getParameter("sortBy"));

        resp.sendRedirect(req.getContextPath() + "/success?search="
                + java.net.URLEncoder.encode(search, "UTF-8")
            + "&page=" + page
            + "&sortBy=" + java.net.URLEncoder.encode(sortBy, "UTF-8"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────
    /**
     * Parses the {@code page} query parameter, defaulting to 1 on
     * missing/invalid input.
     */
    private int parsePage(String pageStr) {
        if (pageStr != null && pageStr.matches("\\d+")) {
            int p = Integer.parseInt(pageStr);
            return p > 0 ? p : 1;
        }
        return 1;
    }

    private String normalizeSortBy(String role, String sortBy) {
        if ("Instructor".equals(role)) {
            if ("name".equals(sortBy) || "participants".equals(sortBy) || "status".equals(sortBy)) {
                return sortBy;
            }
            return "participants";
        }

        if ("Student".equals(role)) {
            if ("name".equals(sortBy) || "status".equals(sortBy)) {
                return sortBy;
            }
            return "name";
        }

        return sortBy == null ? "" : sortBy;
    }
}
