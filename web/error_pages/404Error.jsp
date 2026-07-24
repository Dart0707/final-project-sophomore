<%@ page contentType="text/html; charset=UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Login Error</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/index.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/error.css">
        <!-- Styles moved to css/error.css to keep markup clean -->
    </head>
    <body>
        <div class="login-wrapper" role="main">
            <div class="left-panel">
                <h1>Page Unreachable</h1>
                <p>We couldn't view page. The page you are trying to access does not exist.</p>
                <button class="login-btn" type="button" onclick="window.location.href='${pageContext.request.contextPath}/index.jsp'">Return to Login</button>
            </div>
            <div class="right-panel">
                <div class="brand-name">Active Learning Inc.</div>
                <img src="${pageContext.request.contextPath}/images/crying_girl.png" alt="crying" class="error-illustration"/>
            </div>
        </div>
    </body>
</html>
