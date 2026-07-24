<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="myhelper.LogEntry" %>

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
    @SuppressWarnings("unchecked")
    List<LogEntry> logs = (List<LogEntry>) request.getAttribute("logs");
    String startDate = (String) request.getAttribute("startDate");
    String endDate = (String) request.getAttribute("endDate");
    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>Action Logs</title>
        <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet"/>
        <link rel="stylesheet" href="css/success.css"/>
        
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/filter.js"></script>
    </head>
    <body>
        <nav>
            <a class="brand" href="${pageContext.request.contextPath}/success">Active Learning Inc.</a>
            <div class="nav-links">
            </div>
            <div class="nav-user">
                <span>Hi, <%=loggedUser%> (<%=loggedRole%>)</span>
                <a href="${pageContext.request.contextPath}/logout" class="logout-link" title="Logout">
                    <div class="badge">&#8594;</div>
                </a>
            </div>
        </nav>

        <main>
            <div class="page-header">
                <div>
                    <div class="page-title">Action Logs</div>
                </div>
                <a class="btn btn-outline btn-sm" href="${pageContext.request.contextPath}/success">Back to Dashboard</a>
            </div>

            <div id="inlineDateError" style="display: none; width: 100%; background-color: #fff5f5; color: #e53e3e; border-left: 4px solid #e53e3e; padding: 1rem; border-radius: 4px; margin-bottom: 1rem; font-size: 0.95rem; font-weight: 500;"></div>
            <div style="background:#fff;border:1px solid var(--border);border-radius:14px;padding:1rem 1.1rem;margin-bottom:1rem;box-shadow:var(--shadow)">
                <form method="get" action="${pageContext.request.contextPath}/logs" onsubmit="return validateDateFilters();" style="display:flex;gap:1rem;align-items:flex-end;flex-wrap:wrap">
                    <div class="form-group" style="margin-bottom:0;min-width:220px;flex:1">
                        <label for="startDate">Start Date</label>
                        <input type="date" id="startDate" name="startDate" value="<%=startDate != null ? startDate : ""%>"/>
                    </div>
                    <div class="form-group" style="margin-bottom:0;min-width:220px;flex:1">
                        <label for="endDate">End Date</label>
                        <input type="date" id="endDate" name="endDate" value="<%=endDate != null ? endDate : ""%>"/>
                    </div>
                    <button type="submit" class="btn btn-primary">Filter</button>
                    <a class="btn btn-outline" href="${pageContext.request.contextPath}/logs">Clear</a>
                </form>
            </div>

            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th style="width:100px">ID</th>
                            <th style="width:180px">Timestamp</th>
                            <th style="width:110px">Level</th>
                            <th style="width:140px">Category</th>
                            <th style="width:110px">User ID</th>
                            <th style="width:180px">Source</th>
                            <th>Message</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (logs == null || logs.isEmpty()) { %>
                        <tr>
                            <td colspan="7" style="text-align:center;padding:2rem;color:var(--muted)">No logs found for the selected date range.</td>
                        </tr>
                        <% } else {
                            for (LogEntry log : logs) {
                                String levelClass = "ERROR".equalsIgnoreCase(log.getLogLevel()) ? "role-admin"
                                        : "WARNING".equalsIgnoreCase(log.getLogLevel()) ? "role-instructor"
                                        : "role-student";
                        %>
                        <tr>
                            <td><%=log.getLogId()%></td>
                            <td><%=log.getLogTimestamp() == null ? "" : displayFormat.format(log.getLogTimestamp())%></td>
                            <td><span class="role-badge <%=levelClass%>"><%=log.getLogLevel()%></span></td>
                            <td><%=log.getCategory()%></td>
                            <td><%=log.getUserId() == null ? "-" : log.getUserId()%></td>
                            <td><%=log.getSource()%></td>
                            <td><%=log.getMessage()%></td>
                        </tr>
                        <%  }
                        } %>
                    </tbody>
                </table>
            </div>
        </main>
    </body>
</html>