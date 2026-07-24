<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="myhelper.User" %>
<%@ page import="myhelper.Course" %>
<%@page import="java.time.LocalDate"%>

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
<%--
    success.jsp — Pure VIEW layer.
    All data is prepared by SuccessServlet and passed via request attributes:

      msg          – flash notification string (may be empty)
      users        – List<User>   (Admin only)
      courses      – List<Course> (Instructor / Student)
      search       – current search string
    sortBy       – current sort selection
      currentPage  – active page number
      totalPages   – total page count

    Session attributes read for display only:
      session.username  – logged-in username
      session.role      – "Admin" | "Instructor" | "Student"
--%>
<%
    /* ── Read session display values (no business logic here) ── */
    String loggedUser = (String) session.getAttribute("username");
    String loggedRole = (String) session.getAttribute("role");

    /* ── Read request attributes set by SuccessServlet ── */
    String msg = (String) request.getAttribute("msg");
    if (msg == null) {
        msg = "";
    }

    String search = (String) request.getAttribute("search");
    if (search == null) {
        search = "";
    }

    String sortBy = (String) request.getAttribute("sortBy");
    if (sortBy == null) {
        sortBy = "Instructor".equals(loggedRole) ? "participants" : "Student".equals(loggedRole) ? "name" : "";
    }

    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");

    @SuppressWarnings(  "unchecked")
    List<User> users = (List<User>) request.getAttribute("users");

    @SuppressWarnings(  "unchecked")
    List<Course> courses = (List<Course>) request.getAttribute("courses");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>Active Learning Inc.</title>
        <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet"/>
        <link rel="stylesheet" href="css/success.css"/>
    </head>
    <body>

        <%-- ═══════════════════════════════════════════
             NAVBAR
             ═══════════════════════════════════════════ --%>
        <nav>
            <a class="brand" href="${pageContext.request.contextPath}/success">Active Learning Inc.</a>

            <div class="nav-user">
                <span>Hi, <%=loggedUser%> (<%=loggedRole%>)</span>
                <a href="${pageContext.request.contextPath}/logout" class="logout-link" title="Logout">
                    <div class="badge">&#8594;</div>
                </a>
            </div>
        </nav>

        <%-- ═══════════════════════════════════════════
             MAIN
             ═══════════════════════════════════════════ --%>
        <main>

            <%-- Flash / error notification --%>
            <% if (!msg.isEmpty()) {%>
            <div class="toast <%= msg.startsWith("Database") || msg.startsWith("Invalid") ? "err" : ""%>">
                <%=msg%>
            </div>
            <% } %>

            <%-- ─────────────────────────────────────────
                 ADMIN VIEW — User management table
                 ───────────────────────────────────────── --%>
            <% if ("Admin".equals(loggedRole)) {%>

            <div class="page-header">
                <div class="page-title" style="color:#2d2fa3">Users</div>
                <div class="header-actions">
                    <form method="get" action="${pageContext.request.contextPath}/success" style="display:contents">
                        <div class="search-box">
                            <input type="text" name="search" placeholder="Search" value="<%=search%>"/>
                            <button type="submit">&#128269;</button>
                        </div>
                    </form>
                    <a class="btn btn-outline" href="${pageContext.request.contextPath}/logs">View Logs</a>
                    <button class="btn btn-primary" onclick="openModal('createUserModal')">+ Add User</button>
                </div>
            </div>

            <div class="dl-bar">
                <a href="${pageContext.request.contextPath}/downloadUsers" class="btn btn-outline btn-sm">
                    &#8659; Download PDF
                </a>
            </div>

            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>User</th>
                            <th>Role</th>
                            <th style="text-align:right">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (users == null || users.isEmpty()) { %>
                        <tr>
                            <td colspan="3" style="text-align:center;padding:2rem;color:var(--muted)">
                                No users found.
                            </td>
                        </tr>
                        <% } else {
                            for (User u : users) {
                                String uRole = u.getRole();
                                String roleClass = "Admin".equals(uRole) ? "role-admin"
                                        : "Instructor".equals(uRole) ? "role-instructor"
                                        : "role-student";
                        %>
                        <tr>
                            <td><%=u.getUsername()%></td>
                            <td><span class="role-badge <%=roleClass%>"><%=uRole%></span></td>
                            <td style="text-align:right">
                                <% if (!u.getUsername().equals(loggedUser)) {%>
                                <button class="btn btn-outline btn-sm" onclick="openEditUserModal('<%=u.getId()%>', '<%=u.getUsername()%>', '<%=uRole%>')">Edit</button>
                                <%-- added edit button for update in admin view --%>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/success"
                                      style="display:inline"
                                      onsubmit="return confirm('Delete this user?')">
                                    <input type="hidden" name="action"  value="deleteUser"/>
                                    <input type="hidden" name="userId"  value="<%=u.getId()%>"/>
                                    <input type="hidden" name="search"  value="<%=search%>"/>
                                    <input type="hidden" name="page"    value="<%=currentPage%>"/>
                                    <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                </form>
                                <% } else { %>
                                <span style="font-size:.75rem;color:var(--muted)">(you)</span>
                                <% } %>
                            </td>
                        </tr>
                        <% }
                            } %>
                    </tbody>
                </table>
            </div>

            <%-- ─────────────────────────────────────────
                 INSTRUCTOR VIEW — My courses grid
                 ───────────────────────────────────────── --%>
            <% } else if ("Instructor".equals(loggedRole)) {%>

            <div class="page-header">
                <div class="page-title">Seminars</div>
                <div class="header-actions">
                    <form method="get" action="${pageContext.request.contextPath}/success" style="display:flex; gap:0.7rem; align-items:center; flex-wrap:wrap;">
                        <input type="hidden" name="page" value="1"/>
                        <div class="search-box">
                            <input type="text" name="search" placeholder="Search" value="<%=search%>"/>
                            <button type="submit">&#128269;</button>
                        </div>
                        <select name="sortBy" class="sort-select" onchange="this.form.submit()">
                            <option value="name" <%= "name".equals(sortBy) ? "selected" : ""%>>Sort by Name</option>
                            <option value="participants" <%= "participants".equals(sortBy) ? "selected" : ""%>>Sort by Participants</option>
                            <option value="status" <%= "status".equals(sortBy) ? "selected" : ""%>>Sort by Status</option>
                        </select>
                    </form>
                    <button class="btn btn-primary" onclick="openModal('createCourseModal')">+ New Course</button>
                </div>
            </div>

            <div class="grid">
                <% if (courses == null || courses.isEmpty()) { %>
                <div style="grid-column:1/-1;text-align:center;padding:3rem;color:var(--muted)">
                    No courses yet. Create your first one!
                </div>
                <% } else {
                    for (Course c : courses) {%>
                <%
                    LocalDate dateToday = LocalDate.now();
                    String courseDateStr = c.getCourseDate();

                    LocalDate dateCourse = (courseDateStr != null && !courseDateStr.trim().isEmpty()) ? java.time.LocalDate.parse(courseDateStr) : null;

                    boolean isActive = "ACTIVE".equals(c.getStatus());
                    boolean isEnded = "ENDED".equals(c.getStatus());
                %>
                <div class="card">
                    <a class="card-title">
                        <%=c.getTitle()%>
                    </a>
                    <div class="card-meta"><%=c.getParticipantCount()%> Participants</div>
                    <div class="card-meta"><%=c.getCourseDate()%></div>
                    <% if (isActive) { %>
                    <div class="card-meta card-status-active">ACTIVE</div>
                    <% } else {%>
                    <div class="card-meta card-status-inactive">
                        <%= isEnded ? "SEMINAR ENDED" : c.getStatus()%>
                    </div>
                    <% }%>
                    <div class="card-footer">
                        <a href="${pageContext.request.contextPath}/participants?courseId=<%=c.getId()%>"
                           class="btn btn-primary btn-sm">
                            View Participants
                        </a>
                        <button class="btn btn-outline btn-sm" onclick="openEditCourseModal('<%=c.getId()%>', '<%=c.getTitle()%>', '<%=c.getCourseDate()%>')">Edit</button>
                        <%-- Toggle course status (activate/inactivate) --%>
                        <form method="post"
                              action="${pageContext.request.contextPath}/success"
                              style="display:inline">
                            <input type="hidden" name="action"   value="deleteCourse"/>
                            <input type="hidden" name="courseId" value="<%=c.getId()%>"/>
                            <input type="hidden" name="search"   value="<%=search%>"/>
                            <input type="hidden" name="page"     value="<%=currentPage%>"/>
                            <input type="hidden" name="sortBy"   value="<%=sortBy%>"/>

                            <% if (isEnded) { %>
                            <button type="button" class="btn btn-sm" disabled style="cursor: not-allowed;">ENDED</button>
                            <% } else if (isActive) { %>
                            <button type="submit" class="btn btn-warning btn-sm">Deactivate</button>
                            <% } else { %>
                            <button type="submit" class="btn btn-success btn-sm">Activate</button>
                            <% } %>
                        </form>
                    </div>
                </div>
                <% }
                    } %>
            </div>

            <%-- ─────────────────────────────────────────
                 STUDENT VIEW — All available seminars
                 ───────────────────────────────────────── --%>
            <% } else {%>

            <div class="page-header">
                <div class="page-title">Seminars</div>
                <form method="get" action="${pageContext.request.contextPath}/success" style="display:flex; gap:0.7rem; align-items:center; flex-wrap:wrap;">
                    <input type="hidden" name="page" value="1"/>
                    <div class="search-box">
                        <input type="text" name="search" placeholder="Search" value="<%=search%>"/>
                        <button type="submit">&#128269;</button>
                    </div>
                    <select name="sortBy" class="sort-select" onchange="this.form.submit()">
                        <option value="name" <%= "name".equals(sortBy) ? "selected" : ""%>>Sort by Name</option>
                        <option value="status" <%= "status".equals(sortBy) ? "selected" : ""%>>Sort by Status</option>
                    </select>
                </form>
            </div>

            <div class="grid">
                <% if (courses == null || courses.isEmpty()) { %>
                <div style="grid-column:1/-1;text-align:center;padding:3rem;color:var(--muted)">
                    No seminars available.
                </div>
                <% } else {
                    for (Course c : courses) {%>
                <div class="card">
                    <a class="card-title"
                       href="${pageContext.request.contextPath}/courseDetail?id=<%=c.getId()%>">
                        <%=c.getTitle()%>
                    </a>
                    <div class="card-meta">Prof. <%=c.getInstructorName()%></div>
                    <div class="card-meta"><%=c.getCourseDate()%></div>
                    <div class="card-footer">
                        <% if ("INACTIVE".equals(c.getStatus()) && c.isEnrolled()) { %>
                        <span class="btn btn-enrolled">Was Enrolled</span>
                        <% } else if (c.isEnrolled() && "ACTIVE".equals(c.getStatus())) {%>
                        <span class="btn btn-enrolled">Enrolled</span>
                        <form method="post"
                              action="${pageContext.request.contextPath}/success"
                              style="display:inline">
                            <input type="hidden" name="action"   value="unenroll"/>
                            <input type="hidden" name="courseId" value="<%=c.getId()%>"/>
                            <input type="hidden" name="search"   value="<%=search%>"/>
                            <input type="hidden" name="page"     value="<%=currentPage%>"/>
                            <input type="hidden" name="sortBy"   value="<%=sortBy%>"/>
                            <button type="submit" class="btn btn-danger btn-sm">Drop</button>
                        </form>
                        <% } else if ("ACTIVE".equals(c.getStatus())) {%>
                        <form method="post" action="${pageContext.request.contextPath}/success">
                            <input type="hidden" name="action"   value="enroll"/>
                            <input type="hidden" name="courseId" value="<%=c.getId()%>"/>
                            <input type="hidden" name="search"   value="<%=search%>"/>
                            <input type="hidden" name="page"     value="<%=currentPage%>"/>
                            <input type="hidden" name="sortBy"   value="<%=sortBy%>"/>
                            <button type="submit" class="btn btn-primary">Enroll</button>
                        </form>
                        <% } else if ("ENDED".equals(c.getStatus())) {%>    
                        <span class="btn btn-disabled" style="background: #ccc; cursor: not-allowed;">ENDED</span>
                        <% } else { %>
                        <span class="btn btn-disabled" style="background: #ccc; cursor: not-allowed;">Unavailable</span>
                        <% } %>
                    </div>
                </div>
                <% }
                    } %>
            </div>

            <% } %>

            <%-- ═══════════════════════════════════════════
                 PAGINATION
                 ═══════════════════════════════════════════ --%>
            <div class="pager">
                <%
                    String baseUrl = request.getContextPath() + "/success?search="
                            + java.net.URLEncoder.encode(search, "UTF-8")
                            + "&sortBy=" + java.net.URLEncoder.encode(sortBy, "UTF-8")
                            + "&page=";
                %>

                <a href="<%=baseUrl + Math.max(1, currentPage - 1)%>">&#8592;</a>

                <% for (int p = 1; p <= totalPages; p++) {
                        if (p == 1 || p == totalPages || (p >= currentPage - 1 && p <= currentPage + 1)) {%>
                <a href="<%=baseUrl + p%>" class="<%=p == currentPage ? "active" : ""%>"><%=p%></a>
                <% } else if (p == currentPage - 2 || p == currentPage + 2) { %>
                <span class="dots">&#8230;</span>
                <% }
                    }%>

                <a href="<%=baseUrl + Math.min(totalPages, currentPage + 1)%>">&#8594;</a>
            </div>

        </main>

        <%-- ═══════════════════════════════════════════
             FOOTER
             ═══════════════════════════════════════════ --%>
        <footer>
            <span>&copy; 2026 Active Learning Inc. All rights reserved.</span>
            <a href="${pageContext.request.contextPath}/tos.jsp">Terms of Service</a>
        </footer>

        <%-- ═══════════════════════════════════════════
             ADMIN — CREATE USER MODAL
             ═══════════════════════════════════════════ --%>
        <% if ("Admin".equals(loggedRole)) {%>
        <div class="modal-overlay" id="createUserModal">
            <div class="modal">
                <button class="close-modal" onclick="closeModal('createUserModal')">&#215;</button>
                <h2>Add New User</h2>
                <form method="post" action="${pageContext.request.contextPath}/success">
                    <input type="hidden" name="action" value="createUser"/>
                    <input type="hidden" name="search" value="<%=search%>"/>
                    <input type="hidden" name="page"   value="<%=currentPage%>"/>
                    <div class="form-group">
                        <label>Username</label>
                        <input type="text" name="newUsername" required placeholder="e.g. john_doe"/>
                    </div>
                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" name="newPassword" required placeholder="••••••••"/>
                    </div>
                    <div class="form-group">
                        <label>Role</label>
                        <select name="newRole" required>
                            <option value="">Select role</option>
                            <option value="Admin">Admin</option>
                            <option value="Instructor">Instructor</option>
                            <option value="Student">Student</option>
                        </select>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="closeModal('createUserModal')">
                            Cancel
                        </button>
                        <button type="submit" class="btn btn-primary">Create User</button>
                    </div>
                </form>
            </div>
        </div>
        <% } %>

        <%-- ═══════════════════════════════════════════
             ADMIN — EDIT USER MODAL
             ═══════════════════════════════════════════ --%>
        <% if ("Admin".equals(loggedRole)) {%>
        <div class="modal-overlay" id="editUserModal">
            <div class="modal">
                <button class="close-modal" onclick="closeModal('editUserModal')">&#215;</button>
                <h2>Edit User</h2>
                <form method="post" action="${pageContext.request.contextPath}/success">
                    <input type="hidden" name="action" value="updateUser"/>
                    <input type="hidden" name="search" value="<%=search%>"/>
                    <input type="hidden" name="page"   value="<%=currentPage%>"/>

                    <input type="hidden" name="editUserId" value=""/>

                    <div class="form-group">
                        <label>Username</label>
                        <input type="text" name="editUsername" required/>
                    </div>
                    <div class="form-group">
                        <label>New Password</label>
                        <input type="password" name="editPassword" placeholder="(Leave blank to keep current)"/>
                    </div>
                    <div class="form-group">
                        <label>Role</label>
                        <select name="editRole" required>
                            <option value="Admin">Admin</option>
                            <option value="Instructor">Instructor</option>
                            <option value="Student">Student</option>
                        </select>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="closeModal('editUserModal')">Cancel</button>
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
        <% } %>

        <%-- ═══════════════════════════════════════════
             INSTRUCTOR — CREATE COURSE MODAL
             ═══════════════════════════════════════════ --%>
        <%
            String dateToday = LocalDate.now().toString();
        %>
        <% if ("Instructor".equals(loggedRole)) {%>
        <div class="modal-overlay" id="createCourseModal">
            <div class="modal">
                <button class="close-modal" onclick="closeModal('createCourseModal')">&#215;</button>
                <h2>Create New Course</h2>
                <form method="post" action="${pageContext.request.contextPath}/success">
                    <input type="hidden" name="action" value="createCourse"/>
                    <input type="hidden" name="search" value="<%=search%>"/>
                    <input type="hidden" name="page"   value="<%=currentPage%>"/>
                    <input type="hidden" name="sortBy" value="<%=sortBy%>"/>
                    <div class="form-group">
                        <label>Course Title</label>
                        <input type="text" name="courseTitle" required placeholder="e.g. Data Structures"/>
                    </div>
                    <div class="form-group">
                        <label>Course Date</label>
                        <input type="date" name="courseDate" min="<%= dateToday%>" required/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="closeModal('createCourseModal')">
                            Cancel
                        </button>
                        <button type="submit" class="btn btn-success">Create Course</button>
                    </div>
                </form>
            </div>
        </div>
        <% }%>

        <%-- ═══════════════════════════════════════════
             INSTRUCTOR — EDIT COURSE MODAL
             ═══════════════════════════════════════════ --%>
        <% if ("Instructor".equals(loggedRole)) {%>
        <div class="modal-overlay" id="editCourseModal">
            <div class="modal">
                <button class="close-modal" onclick="closeModal('editCourseModal')">&#215;</button>
                <h2>Edit Course</h2>
                <form method="post" action="${pageContext.request.contextPath}/success">
                    <input type="hidden" name="action" value="updateCourse"/>
                    <input type="hidden" name="search" value="<%=search%>"/>
                    <input type="hidden" name="page"   value="<%=currentPage%>"/>
                    <input type="hidden" name="sortBy" value="<%=sortBy%>"/>

                    <input type="hidden" name="editCourseId" value=""/>

                    <div class="form-group">
                        <label>Course Title</label>
                        <input type="text" name="editCourseTitle" required/>
                    </div>
                    <div class="form-group">
                        <label>Course Date</label>
                        <input type="date" name="editCourseDate" min="<%= dateToday%>" required/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline" onclick="closeModal('editCourseModal')">Cancel</button>
                        <button type="submit" class="btn btn-success">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
        <% }%>

        <%-- External JS — at end of body for best load performance --%>
        <script src="js/success.js"></script>

    </body>
</html>
