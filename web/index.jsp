<%-- 
    Document   : index
    Created on : 05 4, 26, 8:56:12 PM
    Author     : D'Artagnan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<% 
    String flashMessage = (String) session.getAttribute("flashMessage");
    if(flashMessage != null) {
        session.removeAttribute("flashMessage");
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Login Page</title>
        <link rel="stylesheet" href="css/index.css">
        <script src="https://www.google.com/recaptcha/api.js" async defer></script>
    </head>
    <body>
    <div class="login-wrapper"> <!-- The container that provides the centered layout -->
        
        <div class="left-panel">
            <h1>Hey <br>Learners!</h1>
            <p>Learn directly from the pros who build the industry. Gain practical expertise through hands-on workshops and lead the next generation of tech!</p>
        </div>

        <div class="right-panel">
            <h1 class="brand-name">Active Learning Inc.</h1>
            
            <% if (flashMessage != null) { %>
                <div class="toast err">
                    <%= flashMessage %>
                </div>
            <% } %>
            
            <h2 class="welcome-text">Welcome!</h2>
            
            <form action="LoginServlet" method="POST">
                <div class="input-group">
                    <input type="text" placeholder="Email" name="email">
                </div>
                <div class="input-group">
                    <input type="password" placeholder="Password" name="password">
                </div>
                
                <input type="submit" value="Login" class="login-btn">
                
                <div class="captcha-box">
                    <div class="g-recaptcha" data-sitekey="6LdmmZgsAAAAAHrOdR7KOxtP7aiAsZ7GCKSY8shJ"></div>
                </div>
            </form>
        </div>

    </div>
</body>
</html>
