package myservlets;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import myhelper.Course;
import myhelper.CourseDAO;
import myhelper.User;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import java.util.logging.Logger;
import utils.LoggerUtil;

public class ParticipantsServlet extends HttpServlet {

    private String URL;
    private String dbUser;
    private String dbPass;
    private String mySQL_URL;
    private String mySQL_pass;
    private String mySQL_user;
    private String postgre_URL;
    private String postgre_pass;
    private String postgre_user;
    private CourseDAO courseDAO;
    private static final Logger logger = utils.SystemLogger.setupLogger(ParticipantsServlet.class.getName(), "");
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        URL = config.getInitParameter("URL");
        if (URL == null) URL = config.getServletContext().getInitParameter("URL");
        dbUser = config.getInitParameter("dbUser");
        if (dbUser == null) dbUser = config.getServletContext().getInitParameter("dbUser");
        dbPass = config.getInitParameter("dbPass");
        if (dbPass == null) dbPass = config.getServletContext().getInitParameter("dbPass");
        mySQL_URL = getServletContext().getInitParameter("mySQLURL");
        mySQL_pass = getServletContext().getInitParameter("mySQLPass");
        mySQL_user = getServletContext().getInitParameter("mySQLUser");
        postgre_URL = getServletContext().getInitParameter("postgreURL");
        postgre_pass = getServletContext().getInitParameter("postgrePass");
        postgre_user = getServletContext().getInitParameter("postgreUser");
        courseDAO = new CourseDAO(URL, dbUser, dbPass,mySQL_URL,mySQL_user,mySQL_pass,postgre_URL,postgre_user,postgre_pass);
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

        String loggedRole = (String) session.getAttribute("role");
        Integer loggedId = (Integer) session.getAttribute("userId");

        if (!"Admin".equals(loggedRole) && !"Instructor".equals(loggedRole)) {
            resp.sendRedirect(req.getContextPath() + "/success");
            return;
        }

        int courseId;
        try {
            courseId = Integer.parseInt(req.getParameter("courseId"));
        } catch (Exception e) {
            session.setAttribute("flashMsg", "Invalid course selection.");
            resp.sendRedirect(req.getContextPath() + "/success");
            return;
        }
        
        try {
            Course course = courseDAO.getCourseById(courseId);
            if (course == null) {
                session.setAttribute("flashMsg", "Course not found.");
                resp.sendRedirect(req.getContextPath() + "/success");
                return;
            }

            if ("Instructor".equals(loggedRole) && course.getInstructorId() != loggedId) {
                session.setAttribute("flashMsg", "You can only view your own course participants.");
                resp.sendRedirect(req.getContextPath() + "/success");
                return;
            }

            // Get date filter parameters for filtering
            String startDate = req.getParameter("startDate");
            String endDate = req.getParameter("endDate");

            List<User> participants = loadParticipants(courseId, startDate, endDate);

            req.setAttribute("course", course);
            req.setAttribute("participants", participants);
            req.setAttribute("participantCount", participants.size());
            req.setAttribute("startDate", startDate);
            req.setAttribute("endDate", endDate);

            String downloadPdf = req.getParameter("downloadPdf");
            if ("true".equalsIgnoreCase(downloadPdf) || "1".equals(downloadPdf)) {
                writePdf(resp, course, participants, (String) session.getAttribute("username"), startDate, endDate);
                logger.info("Event: Download Participants ——— Description: Instructor"+loggedId+" Downloaded participants from course code "+courseId+".");
                LoggerUtil.log("INFO","Download Participants",loggedId,"ParticipantServlet.java","Instructor"+loggedId+" Downloaded participants from course code "+courseId+".",postgre_URL,postgre_user,postgre_pass);
                return;
            }

            req.getRequestDispatcher("/participants.jsp").forward(req, resp);

        } catch (SQLException e) {
            logger.severe("Event: Download Participants ——— Description: Unable to load participants.");
            LoggerUtil.log("SEVERE","Download Participants",null,"ParticipantServlet.java","Unable to load participants.",postgre_URL,postgre_user,postgre_pass);
            throw new ServletException("Unable to load participants.", e);     
        }
    }

    private void writePdf(HttpServletResponse resp, Course course, List<User> participants, String username, String startDate, String endDate)
            throws ServletException, IOException {
        ServletContext context = getServletContext();
        String headerText = getContextParam(context, "pdf.header.text", "Participants List");
        String footerText = getContextParam(context, "pdf.footer.text", "Generated by the system");
        Date now = new Date();
        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
        String fileName = "COURSELIST_" + new SimpleDateFormat("yyyyMMddHHmmss").format(now) + ".pdf";

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 54);
        try (OutputStream output = resp.getOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new ReportPageEvent(headerText, footerText));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

            document.add(new Paragraph("Participants List", titleFont));
            document.add(new Paragraph("Course: " + course.getTitle(), bodyFont));
            document.add(new Paragraph("Date of Seminar: " + course.getCourseDate(), bodyFont));
            document.add(new Paragraph("Generated by: " + (username == null || username.isEmpty() ? "Unknown" : username), bodyFont));
            document.add(new Paragraph("Generated at: " + generatedAt, bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.1f, 3.2f, 1.4f});

            boolean hasStartDate = startDate != null && !startDate.isEmpty();
            boolean hasEndDate = endDate != null && !endDate.isEmpty();
            String filterMessage;

            if (hasStartDate && hasEndDate) {
                filterMessage = "PARTICIPANTS ENROLLED FROM " + startDate + " TO " + endDate;
            } else {
                filterMessage = "ALL ENROLLED PARTICIPANTS";
            }

            PdfPCell filterCell = new PdfPCell(new Phrase(filterMessage, headerFont));
            filterCell.setColspan(3);
            filterCell.setBackgroundColor(new BaseColor(45, 47, 163));
            filterCell.setPadding(8f);
            filterCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(filterCell);

            addHeaderCell(table, "ID", headerFont);
            addHeaderCell(table, "Username", headerFont);
            addHeaderCell(table, "Role", headerFont);

            if (participants.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No participants enrolled yet.", bodyFont));
                emptyCell.setColspan(3);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10f);
                table.addCell(emptyCell);
            } else {
                for (User participant : participants) {
                    table.addCell(new Phrase(String.valueOf(participant.getId()), bodyFont));
                    table.addCell(new Phrase(participant.getUsername(), bodyFont));
                    table.addCell(new Phrase(participant.getRole(), bodyFont));
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            logger.severe("Event: Download Participants ——— Description: Unable to generate PDF.");
            LoggerUtil.log("SEVERE","Download Participants",null,"ParticipantServlet.java","Unable to generate PDF.",postgre_URL,postgre_user,postgre_pass);
            throw new ServletException("Unable to generate PDF.", e);
            
        }
    }

    private String getContextParam(ServletContext context, String name, String defaultValue) {
        String value = context.getInitParameter(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private List<User> loadParticipants(int courseId, String startDate, String endDate)
            throws SQLException {
        boolean hasStartDate = startDate != null && !startDate.isEmpty();
        boolean hasEndDate = endDate != null && !endDate.isEmpty();

        if (hasStartDate && hasEndDate) {
            return courseDAO.getCourseParticipantsByDateRange(courseId, startDate, endDate);
        }

        if (hasStartDate) {
            return courseDAO.getCourseParticipantsByEnrollmentDay(courseId, startDate);
        }

        if (hasEndDate) {
            return courseDAO.getCourseParticipantsByEnrollmentDay(courseId, endDate);
        }

        return courseDAO.getCourseParticipants(courseId);
    }

    private void addHeaderCell(PdfPTable table, String label, Font headerFont) {
        PdfPCell header = new PdfPCell(new Phrase(label, headerFont));
        header.setBackgroundColor(new BaseColor(45, 47, 163));
        header.setPadding(8f);
        table.addCell(header);
    }

    private static class ReportPageEvent extends PdfPageEventHelper {

        private final String headerText;
        private final String footerText;
        private PdfTemplate totalPagesTemplate;
        private com.itextpdf.text.Font headerFont;
        private com.itextpdf.text.Font footerFont;

        private ReportPageEvent(String headerText, String footerText) {
            this.headerText = headerText;
            this.footerText = footerText;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(40, 16);
            headerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, BaseColor.BLACK);
            footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            float headerY = document.top() + 28;
            float footerY = document.bottom() - 24;

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(headerText, headerFont),
                    left, headerY, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(footerText, footerFont),
                    left, footerY, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber() + " of ", footerFont),
                    right - 42, footerY, 0);
            // place the template slightly lower to match the text baseline
            canvas.addTemplate(totalPagesTemplate, right - 40, footerY);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(totalPagesTemplate, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber()), footerFont),
                    0, 0, 0);
        }
    }
}
