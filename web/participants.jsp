<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="myhelper.Course" %>
<%@ page import="myhelper.User" %>

<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    if (session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/error_pages/sessionError.jsp");
        return;
    }

    String username = (String) session.getAttribute("username");
    String role = (String) session.getAttribute("role");
%>
<%
    String loggedUser = (String) session.getAttribute("username");
    String loggedRole = (String) session.getAttribute("role");
    Course course = (Course) request.getAttribute("course");
    @SuppressWarnings("unchecked")
    List<User> participants = (List<User>) request.getAttribute("participants");
    Integer participantCount = (Integer) request.getAttribute("participantCount");
    String startDate = (String) request.getAttribute("startDate");
    String endDate = (String) request.getAttribute("endDate");
    if (participantCount == null) {
        participantCount = participants == null ? 0 : participants.size();
    }

    StringBuilder downloadUrl = new StringBuilder(request.getContextPath())
            .append("/participants?courseId=").append(course.getId())
            .append("&downloadPdf=true");
    if (startDate != null && !startDate.isEmpty()) {
        downloadUrl.append("&startDate=").append(startDate);
    }
    if (endDate != null && !endDate.isEmpty()) {
        downloadUrl.append("&endDate=").append(endDate);
    }
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>Participants</title>
        <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet"/>
        <link rel="stylesheet" href="css/success.css"/>

        <script type="text/javascript" src="${pageContext.request.contextPath}/js/filter.js"></script>
    </head>
    <body>
        <nav>
            <a class="brand" href="${pageContext.request.contextPath}/success">Active Learning Inc.</a>
            <div class="nav-user">
                <span>Hi, <%=loggedUser%> (<%=loggedRole%>)</span>
                <a href="index.jsp" class="logout-link" title="Logout">
                    <div class="badge">&#8594;</div>
                </a>
            </div>
        </nav>

        <main>
            <% if (course == null) { %>
            <div class="toast err">Participants list is unavailable.</div>
            <div class="page-header">
                <div class="page-title">Participants</div>
                <a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/success">Back</a>
            </div>
            <% } else {%>
            <div class="page-header">
                <div>
                    <div class="page-title"><%=course.getTitle()%></div>
                    <div class="card-meta"><%=course.getCourseDate()%> · <%=participantCount%> enrolled</div>
                </div>
                <div class="header-actions">
                    <a class="btn btn-outline btn-sm"
                       href="<%=downloadUrl.toString()%>">
                        Download PDF
                    </a>
                    <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/success">
                        Back to Courses
                    </a>
                </div>
            </div>

            <!-- Date Range Filter -->
            <div id="inlineDateError" style="display: none; width: 100%; background-color: #fff5f5; color: #e53e3e; border-left: 4px solid #e53e3e; padding: 1rem; border-radius: 4px; margin-bottom: 1rem; font-size: 0.95rem; font-weight: 500;"></div>
            <div style="background: #f8f9fa; padding: 1.5rem; border-radius: 8px; margin-bottom: 2rem;">     
                <form method="get" action="${pageContext.request.contextPath}/participants" onsubmit="return validateDateFilters();" style="display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap; position: relative;">
                    <input type="hidden" name="courseId" value="<%=course.getId()%>" />

                    <div style="flex: 1; min-width: 200px;">
                        <label for="startDate" style="display: block; margin-bottom: 0.5rem; font-weight: 500; color: #333;">Start Date:</label>
                        <input type="date" id="startDate" name="startDate" value="<%=startDate != null ? startDate : ""%>" required style="width: 100%; padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.95rem;" />
                    </div>

                    <div style="flex: 1; min-width: 200px;">
                        <label for="endDate" style="display: block; margin-bottom: 0.5rem; font-weight: 500; color: #333;">End Date:</label>
                        <input type="date" id="endDate" name="endDate" value="<%=endDate != null ? endDate : ""%>" required style="width: 100%; padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.95rem;" />
                    </div>

                    <button type="submit" style="padding: 0.5rem 1.5rem; background: #2d2fa3; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: 500;">
                        Filter
                    </button>

                    <% if ((startDate != null && !startDate.isEmpty()) || (endDate != null && !endDate.isEmpty())) {%>
                    <a href="${pageContext.request.contextPath}/participants?courseId=<%=course.getId()%>" style="padding: 0.5rem 1.5rem; background: #6c757d; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; text-decoration: none; display: inline-block;">
                        Clear Filter
                    </a>
                    <% } %>
                </form>
            </div>

            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th style="width:120px">User ID</th>
                            <th>Username</th>
                            <th style="width:160px">Role</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (participants == null || participants.isEmpty()) { %>
                        <tr>
                            <td colspan="3" style="text-align:center;padding:2rem;color:var(--muted)">
                                No participants enrolled yet.
                            </td>
                        </tr>
                        <% } else {
                            for (User participant : participants) {
                                String participantRole = participant.getRole();
                                String roleClass = "Admin".equals(participantRole) ? "role-admin"
                                    : "Instructor".equals(participantRole) ? "role-instructor"
                                        : "role-student";
                        %>
                        <tr>
                            <td><%=participant.getId()%></td>
                            <td><%=participant.getUsername()%></td>
                            <td><span class="role-badge <%=roleClass%>"><%=participantRole%></span></td>
                        </tr>
                        <%  }
                            } %>
                    </tbody>
                </table>
            </div>
            <% }%>
        </main>
    </body>
</html>