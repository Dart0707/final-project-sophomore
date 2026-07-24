<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>System Error</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/index.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/error.css">
    </head>
    <body>
        <div class="login-wrapper" role="main">
            <div class="left-panel">
                <h1>Unexpected Error</h1>
                <p>
                    <%
                        if (exception != null && exception.getMessage() != null) {
                            out.print(exception.getMessage());
                        } else {
                            out.print("An unexpected system error occurred.");
                        }
                    %>
                </p>

                <button class="login-btn" type="button" onclick="window.history.back();">Return</button>
            </div>
            <div class="right-panel">
                <div class="brand-name">Active Learning Inc.</div>
                <img src="${pageContext.request.contextPath}/images/crying_girl.png" alt="crying" class="error-illustration"/>
            </div>
        </div>
    </body>
</html>