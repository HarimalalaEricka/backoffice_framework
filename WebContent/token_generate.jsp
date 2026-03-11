<%@ page import="java.time.LocalDateTime" %>
<%
Boolean success = (Boolean) request.getAttribute("success");
String error = (String) request.getAttribute("error");
String token = (String) request.getAttribute("token");
LocalDateTime expiration = (LocalDateTime) request.getAttribute("expiration");
Integer duree = (Integer) request.getAttribute("duree");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8" />
    <title>Generation de Token</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .msg { padding: 10px; border-radius: 4px; margin-bottom: 12px; }
        .msg-ok { background: #e6ffed; border: 1px solid #b6f0c8; }
        .msg-err { background: #ffe6e6; border: 1px solid #f0b6b6; }
        label { display: block; margin: 6px 0 2px; }
        select { padding: 6px; width: 300px; }
        table { border-collapse: collapse; margin-top: 10px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #f4f4f4; }
    </style>
</head>
<body>
    <h1>Generation de Token</h1>

    <% if (success != null && success) { %>
        <div class="msg msg-ok">Token genere avec succès - Stocké en session ✓</div>
        <table>
            <tr>
                <th>Token</th>
                <td><%= token %></td>
            </tr>
            <tr>
                <th>Date d'expiration</th>
                <td><%= expiration %></td>
            </tr>
            <tr>
                <th>Duree</th>
                <td><%= duree %> heure(s)</td>
            </tr>
        </table>
    <% } %>

    <% if (error != null) { %>
        <div class="msg msg-err"><%= error %></div>
    <% } %>

    <h2>Generer un nouveau token</h2>
    <form method="post" action="<%= request.getContextPath() %>/token/generate">
        <label for="duree">Duree de validite :</label>
        <input type="number" name="duree" required>
        <label for="unite"></label>
        <select name="unite">
            <option value="minutes">Minutes</option>
            <option value="heures">Heures</option>
            <option value="jours">Jours</option>
        </select>
        <div style="margin-top: 10px;">
            <input type="submit" value="Generer">
        </div>
    </form>
</body>
</html>
