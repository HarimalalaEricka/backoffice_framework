<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Planification des Réservations</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: white;
            padding: 40px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 30px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: bold;
            color: #555;
        }
        input[type="date"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 16px;
            box-sizing: border-box;
        }
        input[type="date"]:focus {
            outline: none;
            border-color: #007bff;
            box-shadow: 0 0 5px rgba(0,123,255,0.3);
        }
        .btn-submit {
            width: 100%;
            background-color: #007bff;
            color: white;
            padding: 14px;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        .btn-submit:hover {
            background-color: #0056b3;
        }
        .error {
            color: #dc3545;
            background-color: #f8d7da;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 20px;
            text-align: center;
        }
        .info-box {
            background-color: #e7f3ff;
            border: 1px solid #b6d4fe;
            border-radius: 4px;
            padding: 15px;
            margin-bottom: 25px;
        }
        .info-box h3 {
            margin: 0 0 10px 0;
            color: #084298;
            font-size: 14px;
        }
        .info-box ul {
            margin: 0;
            padding-left: 20px;
            color: #084298;
            font-size: 13px;
        }
        .info-box li {
            margin-bottom: 5px;
        }
        .icon {
            font-size: 48px;
            text-align: center;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">🚐</div>
        <h1>Planification Jour J</h1>
        
        <% if(request.getAttribute("error") != null) { %>
            <div class="error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>
        
        <%-- <div class="info-box">
            <h3> Comment ça marche ?</h3>
            <ul>
                <li>Sélectionnez une date de planification</li>
                <li>Le système récupère toutes les réservations du jour</li>
                <li>Les réservations sont groupées par vol (même heure d'arrivée)</li>
                <li>Un véhicule optimal est assigné à chaque groupe</li>
                <li>Priorité : capacité proche du besoin, puis Diesel</li>
            </ul>
        </div>
         --%>
        <form action="planification" method="POST">
            <div class="form-group">
                <label for="datePlanification">Date de planification</label>
                <input type="date" 
                       id="datePlanification" 
                       name="datePlanification" 
                       required 
                       min="<%= java.time.LocalDate.now() %>"
                       value="<%= java.time.LocalDate.now() %>">
            </div>
            
            <button type="submit" class="btn-submit">
                Lancer la planification
            </button>
        </form>
    </div>
</body>
</html>