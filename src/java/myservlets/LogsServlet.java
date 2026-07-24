package myservlets;

import myhelper.LogDAO;
import myhelper.LogEntry;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/logs")
public class LogsServlet extends HttpServlet {

    private LogDAO logDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        ServletContext ctx = getServletContext();
        String postgre_URL = ctx.getInitParameter("postgreURL");
        String postgre_pass = ctx.getInitParameter("postgrePass");
        String postgre_user = ctx.getInitParameter("postgreUser");
        logDAO = new LogDAO(postgre_URL, postgre_user, postgre_pass);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null
                || session.getAttribute("username") == null
                || session.getAttribute("role") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"Admin".equals(role)) {
            session.setAttribute("flashMsg", "Unauthorized.");
            resp.sendRedirect(req.getContextPath() + "/success");
            return;
        }
        Integer loggedId = (Integer) session.getAttribute("userId");
        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");

        try {
            List<LogEntry> logs = logDAO.getLogs(startDate, endDate, loggedId);
            req.setAttribute("logs", logs);
            req.setAttribute("startDate", startDate);
            req.setAttribute("endDate", endDate);
            req.getRequestDispatcher("/logs.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Unable to load logs.", e);
        }
    }
}