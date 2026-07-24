package myservlets;

import utils.LoggerUtil;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import myexceptions.NullValueException;
import utils.EncryptionUtil;

/**
 *
 * @author D'Artagnan
 */
public class LoginServlet extends HttpServlet {

    private String URL;
    private String dbUser;
    private String dbPass;
    private String dbDriver;
    private String cipherAlgorithm;
    private String secretKey;
    private String recaptchaSecretKey;
    private String postgre_URL;
    private String postgre_user;
    private String postgre_pass;
    private static Logger logger;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Prefer servlet init params, fall back to context params
        URL = config.getInitParameter("URL");
        if (URL == null) URL = config.getServletContext().getInitParameter("URL");
        dbUser = config.getInitParameter("dbUser");
        if (dbUser == null) dbUser = config.getServletContext().getInitParameter("dbUser");
        dbPass = config.getInitParameter("dbPass");
        if (dbPass == null) dbPass = config.getServletContext().getInitParameter("dbPass");
        dbDriver = config.getInitParameter("dbDriver");
        if (dbDriver == null) dbDriver = config.getServletContext().getInitParameter("dbDriver");
        cipherAlgorithm = config.getInitParameter("cipherAlgorithm");
        if (cipherAlgorithm == null) cipherAlgorithm = config.getServletContext().getInitParameter("cipherAlgorithm");
        secretKey = config.getInitParameter("secretKey");
        if (secretKey == null) secretKey = config.getServletContext().getInitParameter("secretKey");
        recaptchaSecretKey = config.getInitParameter("recaptchaSecretKey");
        if (recaptchaSecretKey == null) recaptchaSecretKey = config.getServletContext().getInitParameter("recaptchaSecretKey");
        String appRootPath = config.getServletContext().getRealPath("/");
        postgre_URL = getServletContext().getInitParameter("postgreURL");
        postgre_user = getServletContext().getInitParameter("postgreUser");
        postgre_pass = getServletContext().getInitParameter("postgrePass");
        logger = utils.SystemLogger.setupLogger(LoginServlet.class.getName(), appRootPath);

        if (dbDriver != null && !dbDriver.trim().isEmpty()) {
            try {
                Class.forName(dbDriver);
            } catch (ClassNotFoundException e) {
                throw new ServletException("DB Driver not found.", e);
            }
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        HttpSession session = request.getSession(false);

        
        email = (email != null) ? email.trim() : "";
        password = (password != null) ? password.trim() : "";

        logger.info("Event: Start of login ——— Description: User ' " + email + "' initialized login.");

        try {
            if (email.isEmpty() || password.isEmpty()) {
                throw new NullValueException("Email or password is blank.");
            }
            // CAPTCHA CHECK
            session = request.getSession(true);
            Integer captchaAttempts = (Integer) session.getAttribute("captchaAttempts");
            if (captchaAttempts == null) {
                captchaAttempts = 0;
            }

            if (captchaAttempts >= 3) {
                session.setAttribute("flashMessage", "Error: Maximum CAPTCHA attempts reached. Access denied.");
                response.sendRedirect(request.getContextPath() + "/error_pages/captchaError.jsp");
                return;
            }

            String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

            boolean isCaptchaValid = utils.CaptchaUtil.verify(gRecaptchaResponse, recaptchaSecretKey);

            if (!isCaptchaValid) {
                logger.warning("Event: CAPTCHA validation result ——— Description: Failed for user '" + email + "'.");
                LoggerUtil.log("WARNING","CAPTCHA",null,"LoginServlet.java","Captcha vaildation failed.",postgre_URL,postgre_user,postgre_pass);
                captchaAttempts++;
                session.setAttribute("captchaAttempts", captchaAttempts);

                int attemptsLeft = 3 - captchaAttempts;
                session.setAttribute("flashMessage", "Error: CAPTCHA validation failed. " + attemptsLeft + " attempt(s) remaining.");
                response.sendRedirect("index.jsp");
                return;
            }

            logger.info("Event: CAPTCHA validation result ——— Description: Passed for user '" + email + "'.");
            LoggerUtil.log("INFO","CAPTCHA",null,"LoginServlet.java","Captcha passed for user '"+email+"'.",postgre_URL,postgre_user,postgre_pass);
            session.removeAttribute("captchaAttempts");

            // break line for separating different checking method ————————————————————————    
            // DATABASE CHECK
            try (Connection conn = DriverManager.getConnection(URL, dbUser, dbPass)) {
                String sql = "SELECT ID, PASSWORD, USERROLE FROM USERS WHERE EMAIL = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, email);

                ResultSet rs = ps.executeQuery();
                boolean usernameExists = rs.next();

                if (!usernameExists) {
                    logger.warning("Event: Username lookup ——— Description: Email '" + email + "' not found.");
                    LoggerUtil.log("WARNING","Login",null,"LoginServlet.java","Failed login email '" + email + "' not found.",postgre_URL,postgre_user,postgre_pass);
                    
                    logger.warning("Event: Failed login ——— Description: Invalid username for '" + email + "'.");

                    if (password.isEmpty()) {
                        request.getRequestDispatcher("error_pages/loginError.jsp")
                                .forward(request, response);
                    } else {
                        request.getRequestDispatcher("error_pages/loginError.jsp")
                                .forward(request, response);
                    }
                    return;
                }

                logger.info("Event: Username lookup ——— Description: Email '" + email + "' found.");
                LoggerUtil.log("INFO","CAPTCHA",null,"LoginServlet.java","Email '"+email+"' found.",postgre_URL,postgre_user,postgre_pass);
                
                String dbPassword = rs.getString("PASSWORD");
                String decryptedPassword = null;
                try {
                    decryptedPassword = EncryptionUtil.decrypt(dbPassword, secretKey, cipherAlgorithm);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.log(Level.SEVERE, "Event: Exception or system error ——— Description: Password decryption failed for user '" + email + "'.", e);
                    LoggerUtil.log("SEVERE","Login",null,"LoginServlet.java","Password decryption failed for user",postgre_URL,postgre_user,postgre_pass);

                    session.setAttribute("flashMessage", "System Error: Decryption failed. Check logs.");
                    response.sendRedirect("index.jsp");
                    return;
                }
                String role = rs.getString("USERROLE");

                if (decryptedPassword == null || !decryptedPassword.equals(password)) {
                    logger.warning("Event: Password validation result ——— Description: Password mismatch for '" + email + "'.");
                    logger.warning("Event: Failed Login ——— Description: Incorrect password entered for '" + email + "'.");
                    LoggerUtil.log("WARNING","Login",null,"LoginServlet.java","Incorrect password entered for '" + email + "'.",postgre_URL,postgre_user,postgre_pass);
                    
                    request.getRequestDispatcher("error_pages/loginError.jsp")
                            .forward(request, response);
                    return;
                }

                logger.info("Event: Password validation result ——— Description: Password verified successfully for '" + email + "'.");
                
                logger.info("Event: Successful login ——— Description: Session created and authenticated for '" + email + "'.");
                
                int id = rs.getInt("ID");
                LoggerUtil.log("INFO","CAPTCHA",id,"LoginServlet.java","Password succesfully verified for '"+email+"'.",postgre_URL,postgre_user,postgre_pass);
                LoggerUtil.log("INFO","CAPTCHA",id,"LoginServlet.java","Session created and authenticated for '"+email+"'.",postgre_URL,postgre_user,postgre_pass);
                session.setAttribute("username", email);
                session.setAttribute("role", role);
                session.setAttribute("userId", id);

                response.sendRedirect(request.getContextPath() + "/success");
            }

        } catch (NullValueException e) {
            logger.warning("Event: Failed login ——— Description: Blank credentials provided.");
            LoggerUtil.log("WARNING","Login",null,"LoginServlet.java","No value entered.",postgre_URL,postgre_user,postgre_pass);
            request.getRequestDispatcher("error_pages/loginError.jsp")
                    .forward(request, response);
        } catch (IOException | SQLException | ServletException e) {
            logger.log(Level.SEVERE, "Event: Exception or system error ——— Description: System crashed during login for '" + email + "'.", e);
            LoggerUtil.log("SEVERE","Login",null,"LoginServlet.java","System crashed during login for '" + email + "'.",postgre_URL,postgre_user,postgre_pass);
            throw new ServletException(e);
        } finally {
            logger.info("Event: End of login process ——— Description: Login process terminated for '" + email + "'.\n");
            LoggerUtil.log("INFO","CAPTCHA",null,"LoginServlet.java","Login process terminated for '" + email + "'.\n",postgre_URL,postgre_user,postgre_pass);
        }
    }
}
