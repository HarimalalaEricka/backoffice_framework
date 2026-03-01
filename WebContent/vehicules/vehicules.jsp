<%@ page import="com.app.models.Vehicule" %>
<%@ page import="java.util.List" %>
<%
String message = (String) request.getAttribute("message");
String error = (String) request.getAttribute("error");
List<Vehicule> vehicules = (List<Vehicule>) request.getAttribute("vehicules");
if (vehicules == null) vehicules = new java.util.ArrayList<>();
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8" />
    <title>Gestion des vehicules</title>
    <style>
        body { font-family: Arial, sans-serif; margin:20px; }
        .msg { padding:10px; border-radius:4px; margin-bottom:12px; }
        .msg-ok { background:#e6ffed; border:1px solid #b6f0c8; }
        .msg-err { background:#ffe6e6; border:1px solid #f0b6b6; }
        form { margin-bottom:20px; }
        label { display:block; margin:6px 0 2px; }
        input, select { padding:6px; width:300px; }
        table { border-collapse: collapse; width:100%; }
        th, td { border:1px solid #ddd; padding:8px; text-align:left; }
        th { background:#f4f4f4; }
        .btn-delete { padding:4px 8px; background:#ff6b6b; color:white; border:none; border-radius:3px; cursor:pointer; }
        .btn-delete:hover { background:#ff5252; }
    </style>
</head>
<body>
    <h1>Gestion des vehicules</h1>

    <% if (message != null) { %>
        <div class="msg msg-ok"><%= message %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="msg msg-err"><%= error %></div>
    <% } %>

    <h2>Ajouter un vehicule</h2>
    <form method="post" action="<%= request.getContextPath() %>/vehicules/insert">
        <label for="reference">Reference :</label>
        <input type="text" id="reference" name="reference" required placeholder="ex: VH-001">

        <label for="nbr_places">Nombre de places :</label>
        <input type="number" id="nbr_places" name="nbr_places" min="1" required placeholder="ex: 5">

        <label for="type_carburant">Type de carburant :</label>
        <select id="type_carburant" name="type_carburant" required>
            <option value="">-- Selectionner --</option>
            <option value="D">Diesel (D)</option>
            <option value="ES">Essence (ES)</option>
            <option value="EL">electrique (EL)</option>
            <option value="H">Hybride (H)</option>
        </select>

        <div style="margin-top:10px">
            <input type="submit" value="Ajouter vehicule">
        </div>
    </form>

    <h2>Liste des vehicules</h2>
    <% if (vehicules.isEmpty()) { %>
        <p>Aucun vehicule enregistre.</p>
    <% } else { %>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Reference</th>
                    <th>Nombre de places</th>
                    <th>Type de carburant</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <% for (Vehicule v : vehicules) { %>
                    <tr>
                        <td><%= v.getIdVehicule() %></td>
                        <td><%= v.getReference() %></td>
                        <td><%= v.getNbrPlaces() %></td>
                        <td><%= v.getTypeCarburant() %></td>
                        <td>
                            <form method="post" action="<%= request.getContextPath() %>/vehicules/delete" style="display:inline;">
                                <input type="hidden" name="id_vehicule" value="<%= v.getIdVehicule() %>">
                                <button type="submit" class="btn-delete" onclick="return confirm('Êtes-vous sûr ?');">Supprimer</button>
                            </form>
                        </td>
                    </tr>
                <% } %>
            </tbody>
        </table>
    <% } %>
</body>
</html>
