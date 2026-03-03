<%@ page import="com.app.models.Vehicule" %>
<%@ page import="java.util.List" %>
<%
String error = (String) request.getAttribute("error");
List<Vehicule> vehicules = (List<Vehicule>) request.getAttribute("vehicules");
if (vehicules == null) vehicules = new java.util.ArrayList<>();
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8" />
    <title>Liste des Véhicules</title>
    <style>
        body { font-family: Arial, sans-serif; margin:20px; }
        .msg-err { background:#ffe6e6; border:1px solid #f0b6b6; padding:10px; border-radius:4px; margin-bottom:12px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width:100%; margin-top:20px; }
        th, td { border:1px solid #ddd; padding:10px; text-align:left; }
        th { background:#4CAF50; color:white; font-weight:bold; }
        tr:nth-child(even) { background:#f9f9f9; }
        tr:hover { background:#f1f1f1; }
    </style>
</head>
<body>
    <h1>Liste des Véhicules</h1>

    <% if (error != null) { %>
        <div class="msg-err"><%= error %></div>
    <% } %>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Référence</th>
                <th>Nombre de Places</th>
                <th>Type de Carburant</th>
            </tr>
        </thead>
        <tbody>
            <% if (vehicules.isEmpty()) { %>
                <tr>
                    <td colspan="4" style="text-align:center; color:#999;">Aucun véhicule trouvé</td>
                </tr>
            <% } else { %>
                <% for (Vehicule v : vehicules) { %>
                    <tr>
                        <td><%= v.getIdVehicule() %></td>
                        <td><%= v.getReference() %></td>
                        <td><%= v.getNbrPlaces() %></td>
                        <td><%= v.getTypeCarburant() %></td>
                    </tr>
                <% } %>
            <% } %>
        </tbody>
    </table>
</body>
</html>
