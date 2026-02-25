<%@ page import="com.app.models.Hotel,com.app.models.Reservation" %>
<%@ page import="java.util.List" %>
<%
String message = (String) request.getAttribute("message");
String error = (String) request.getAttribute("error");
List<Hotel> hotels = (List<Hotel>) request.getAttribute("hotels");
List<Reservation> reservations = (List<Reservation>) request.getAttribute("reservations");
if (hotels == null) hotels = new java.util.ArrayList<>();
if (reservations == null) reservations = new java.util.ArrayList<>();
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8" />
    <title>Gestion des reservations</title>
    <style>
        body { font-family: Arial, sans-serif; margin:20px; }
        .msg { padding:10px; border-radius:4px; margin-bottom:12px; }
        .msg-ok { background:#e6ffed; border:1px solid #b6f0c8; }
        .msg-err { background:#ffe6e6; border:1px solid #f0b6b6; }
        form { margin-bottom:20px; }
        label { display:block; margin:6px 0 2px; }
        input, select { padding:6px; width:300px; }
        table { border-collapse: collapse; width:100%; }
        th, td { border:1px solid #ddd; padding:8px; }
        th { background:#f4f4f4; }
    </style>
</head>
<body>
    <h1>Gestion des reservations </h1>

    <% if (message != null) { %>
        <div class="msg msg-ok"><%= message %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="msg msg-err"><%= error %></div>
    <% } %>
    <h2>Ajouter une reservation</h2>
    <form method="post" action="<%= request.getContextPath() %>/reservations/insert">
        <label for="client_id">ID Client :</label>
        <input type="text" id="client_id" name="client_id" required>

        <label for="nbr_pers">Nombre de personnes :</label>
        <input type="number" id="nbr_pers" name="nbr_pers" min="1" required>

        <label for="date_heure">Date et heure d'arrivee :</label>
        <input type="datetime-local" id="date_heure" name="date_heure" required>

        <label for="hotel_id">Hôtel :</label>
        <select id="hotel_id" name="hotel_id" required>
            <option value="">-- Selectionner --</option>
            <% for (Hotel h : hotels) { %>
                <option value="<%= h.getIdHotel() %>"><%= h.getNom() %></option>
            <% } %>
        </select>

        <div style="margin-top:10px">
            <input type="submit" value="Ajouter">
        </div>
    </form>
</body>
</html>