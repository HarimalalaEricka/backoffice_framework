<%@ page import="com.app.models.Vehicule" %>
<%
String message = (String) request.getAttribute("message");
String error = (String) request.getAttribute("error");
Vehicule vehicule = (Vehicule) request.getAttribute("vehicule");
if (vehicule == null) {
    vehicule = new Vehicule();
}
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8" />
    <title>Modifier un vehicule</title>
    <style>
        body { font-family: Arial, sans-serif; margin:20px; }
        .msg { padding:10px; border-radius:4px; margin-bottom:12px; }
        .msg-ok { background:#e6ffed; border:1px solid #b6f0c8; }
        .msg-err { background:#ffe6e6; border:1px solid #f0b6b6; }
        form { margin-bottom:20px; }
        label { display:block; margin:6px 0 2px; }
        input, select { padding:6px; width:300px; }
        .btn { padding:8px 16px; margin-right:10px; border-radius:3px; border:none; cursor:pointer; }
        .btn-primary { background:#4CAF50; color:white; }
        .btn-primary:hover { background:#45a049; }
        .btn-secondary { background:#888; color:white; }
        .btn-secondary:hover { background:#666; }
        .button-group { margin-top:20px; }
    </style>
</head>
<body>
    <h1>Modifier un vehicule</h1>

    <% if (message != null) { %>
        <div class="msg msg-ok"><%= message %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="msg msg-err"><%= error %></div>
    <% } %>

    <form method="post" action="<%= request.getContextPath() %>/vehicules/update">
        <input type="hidden" name="id_vehicule" value="<%= vehicule.getIdVehicule() %>">

        <label for="reference">Reference :</label>
        <input type="text" id="reference" name="reference" value="<%= vehicule.getReference() %>" required placeholder="ex: VH-001">

        <label for="nbr_places">Nombre de places :</label>
        <input type="number" id="nbr_places" name="nbr_places" value="<%= vehicule.getNbrPlaces() %>" min="1" required placeholder="ex: 5">

        <label for="type_carburant">Type de carburant :</label>
        <select id="type_carburant" name="type_carburant" required>
            <option value="">-- Selectionner --</option>
            <option value="D" <%= vehicule.getTypeCarburant() != null && vehicule.getTypeCarburant().equals("D") ? "selected" : "" %>>Diesel (D)</option>
            <option value="ES" <%= vehicule.getTypeCarburant() != null && vehicule.getTypeCarburant().equals("ES") ? "selected" : "" %>>Essence (ES)</option>
            <option value="EL" <%= vehicule.getTypeCarburant() != null && vehicule.getTypeCarburant().equals("EL") ? "selected" : "" %>>electrique (EL)</option>
            <option value="H" <%= vehicule.getTypeCarburant() != null && vehicule.getTypeCarburant().equals("H") ? "selected" : "" %>>Hybride (H)</option>
        </select>

        <div class="button-group">
            <input type="submit" value="Enregistrer" class="btn btn-primary">
            <a href="<%= request.getContextPath() %>/vehicules/insert" class="btn btn-secondary">Annuler</a>
        </div>
    </form>
</body>
</html>
