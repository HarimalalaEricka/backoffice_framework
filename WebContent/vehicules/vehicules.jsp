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
        <meta charset="utf-8"/>
        <title>Gestion des vehicules</title>

        <style>
            
            body{
                font-family: Arial, sans-serif;
                margin:40px;
                background-color:#f5f5f5;
            }
            
            .container{
                max-width:1000px;
                margin:0 auto;
                background:white;
                padding:30px;
                border-radius:8px;
                box-shadow:0 2px 10px rgba(0,0,0,0.1);
            }
            
            h1,h2{
                color:#333;
            }
            
            label{
                display:block;
                margin-top:10px;
                font-weight:bold;
            }
            
            input,select{
                width:300px;
                padding:8px;
                margin-top:5px;
                border:1px solid #ccc;
                border-radius:4px;
            }
            
            .btn-submit{
                margin-top:15px;
                background-color:#007bff;
                color:white;
                border:none;
                padding:10px 18px;
                border-radius:4px;
                cursor:pointer;
            }
            
            .btn-submit:hover{
                background-color:#0056b3;
            }
            
            table{
                width:100%;
                border-collapse:collapse;
                margin-top:20px;
            }
            
            th,td{
                border:1px solid #ddd;
                padding:12px;
            }
            
            th{
                background-color:#007bff;
                color:white;
            }
            
            tr:nth-child(even){
                background-color:#f9f9f9;
            }
            
            .btn-delete{
                background-color:#dc3545;
                color:white;
                border:none;
                padding:6px 10px;
                border-radius:4px;
                cursor:pointer;
            }
            
            .btn-delete:hover{
                background-color:#b02a37;
            }
            
            .btn-edit{
                background-color:#ffc107;
                padding:6px 10px;
                border-radius:4px;
                text-decoration:none;
                color:black;
                margin-right:8px;
            }
            
            .btn-edit:hover{
                background-color:#e0a800;
            }
            
            .success-msg{
                color:#155724;
                background-color:#d4edda;
                padding:12px;
                border-radius:4px;
                margin-bottom:20px;
            }
            
            .error-msg{
                color:#721c24;
                background-color:#f8d7da;
                padding:12px;
                border-radius:4px;
                margin-bottom:20px;
            }
            
            .form-box{
                background:#fafafa;
                padding:20px;
                border-radius:8px;
                border:1px solid #ddd;
                margin-bottom:30px;
            }
            
        </style>

    </head>

    <body>

        <div class="container">

            <h1>Gestion des véhicules</h1>

            <% if (message != null) { %>
            <div class="success-msg">
                <%= message %>
            </div>
            <% } %>

            <% if (error != null) { %>
            <div class="error-msg">
                <%= error %>
            </div>
            <% } %>


            <div class="form-box">

                <h2>Ajouter un véhicule</h2>

                <form method="post" action="<%= request.getContextPath() %>/vehicules/insert">

                    <label>Reference</label>
                    <input type="text" name="reference" required placeholder="ex: VH-001">

                    <label>Nombre de places</label>
                    <input type="number" name="nbr_places" min="1" required placeholder="ex: 5">

                    <label>Type de carburant</label>
                    <select name="type_carburant" required>
                        <option value="">-- Selectionner --</option>
                        <option value="D">Diesel (D)</option>
                        <option value="ES">Essence (ES)</option>
                        <option value="EL">Electrique (EL)</option>
                        <option value="H">Hybride (H)</option>
                    </select>

                    <br>

                    <button class="btn-submit">Ajouter véhicule</button>

                </form>

            </div>


            <h2>Liste des véhicules</h2>

            <% if (vehicules.isEmpty()) { %>

            <p>Aucun véhicule enregistré.</p>

            <% } else { %>

            <table>

                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Reference</th>
                        <th>Nombre de places</th>
                        <th>Type carburant</th>
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

                            <a class="btn-edit"
                            href="<%= request.getContextPath() %>/vehicules/edit?id=<%= v.getIdVehicule() %>">
                            Editer
                        </a>

                        <form method="post"
                        action="<%= request.getContextPath() %>/vehicules/delete"
                        style="display:inline;">

                        <input type="hidden"
                        name="id_vehicule"
                        value="<%= v.getIdVehicule() %>">

                        <button class="btn-delete"
                        onclick="return confirm('Êtes-vous sûr ?');">
                        Supprimer
                    </button>

                </form>

            </td>

        </tr>

        <% } %>

    </tbody>

</table>

<% } %>

</div>

</body>
</html>