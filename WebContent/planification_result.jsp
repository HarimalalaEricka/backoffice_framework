<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.app.planification.VehiculePlanDTO" %>
<%@ page import="com.app.planification.VoyageDTO" %>
<%@ page import="com.app.planification.TrajetDetailDTO" %>
<%@ page import="com.app.models.Reservation" %>
<%@ page import="com.app.models.Vehicule" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Résultat Planification</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1, h2 {
            color: #333;
        }
        .stats {
            display: flex;
            gap: 20px;
            margin-bottom: 30px;
            flex-wrap: wrap;
        }
        .stat-box {
            background-color: #007bff;
            color: white;
            padding: 15px 25px;
            border-radius: 8px;
            text-align: center;
        }
        .stat-box.warning {
            background-color: #ffc107;
            color: #333;
        }
        .stat-box.success {
            background-color: #28a745;
        }
        .stat-number {
            font-size: 24px;
            font-weight: bold;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background-color: #007bff;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        .vehicule-card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            background-color: #fafafa;
        }
        .vehicule-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
            margin-bottom: 15px;
        }
        .vehicule-ref {
            font-size: 18px;
            font-weight: bold;
            color: #007bff;
        }
        .vehicule-info {
            color: #666;
        }
        .carburant-D { color: #28a745; font-weight: bold; }
        .carburant-ES { color: #ffc107; font-weight: bold; }
        .carburant-EL { color: #17a2b8; font-weight: bold; }
        .carburant-H { color: #6f42c1; font-weight: bold; }
        .btn-retour {
            background-color: #6c757d;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            margin-top: 20px;
        }
        .btn-retour:hover {
            background-color: #5a6268;
        }
        .error {
            color: #dc3545;
            background-color: #f8d7da;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .success-msg {
            color: #155724;
            background-color: #d4edda;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .non-assignees {
            background-color: #fff3cd;
            border: 1px solid #ffc107;
            border-radius: 8px;
            padding: 20px;
            margin-top: 30px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Résultat de la Planification</h1>
        <h1>ETU003350</h1>
        <h1>ETU003366</h1>
        <h1>ETU003324</h1>
        
        <% if(request.getAttribute("error") != null) { %>
            <div class="error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>
        
        <% if(request.getAttribute("success") != null) { %>
            <div class="success-msg">
                <%= request.getAttribute("success") %>
            </div>
        <% } %>
        
        <p><strong>Date de planification :</strong> <%= request.getAttribute("datePlanification") %></p>
        
        <!-- Statistiques -->
        <div class="stats">
            <div class="stat-box success">
                <div class="stat-number"><%= request.getAttribute("nombreVehiculesUtilises") %></div>
                <div>Véhicules utilisés</div>
            </div>
            <div class="stat-box">
                <div class="stat-number"><%= request.getAttribute("nombreReservationsAssignees") %></div>
                <div>Réservations assignées</div>
            </div>
            <div class="stat-box">
                <div class="stat-number"><%= request.getAttribute("totalPersonnesAssignees") %></div>
                <div>Personnes transportées</div>
            </div>
            <div class="stat-box warning">
                <div class="stat-number"><%= request.getAttribute("nombreReservationsNonAssignees") %></div>
                <div>Non assignées</div>
            </div>
        </div>
        
        <!-- Liste des véhicules assignés -->
        <h2>🚗 Véhicules Assignés</h2>
        
        <% 
        List<VehiculePlanDTO> vehiculesAssignes = (List<VehiculePlanDTO>) request.getAttribute("vehiculesAssignes");
        if (vehiculesAssignes != null && !vehiculesAssignes.isEmpty()) {
            for (VehiculePlanDTO vp : vehiculesAssignes) {
                Vehicule v = vp.getVehicule();
        %>
            <div class="vehicule-card">
                <div class="vehicule-header">
                    <span class="vehicule-ref"><%= v.getReference() %></span>
                    <span class="vehicule-info">
                        Capacité: <%= v.getNbrPlaces() %> places | 
                        Carburant: <span class="carburant-<%= v.getTypeCarburant() %>"><%= v.getTypeCarburant() %></span>
                    </span>
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th>ID Réservation</th>
                            <th>Client</th>
                            <th>Nb Personnes</th>
                            <th>Heure Arrivée</th>
                            <th>Hôtel</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Reservation r : vp.getReservations()) { %>
                        <%
                            int nbPersonnesAffiche = r.getNbrPers();
                            boolean splitTrouve = false;
                            int sommeAssigneeVehicule = 0;

                            if (vp.getVoyages() != null && !vp.getVoyages().isEmpty()) {
                                for (VoyageDTO vdg : vp.getVoyages()) {
                                    if (vdg.getPassagersAssignesParReservation() != null
                                            && vdg.getPassagersAssignesParReservation().containsKey(r.getIdReservation())) {
                                        sommeAssigneeVehicule += vdg.getPassagersAssignesParReservation().get(r.getIdReservation());
                                        splitTrouve = true;
                                    }
                                }
                            }

                            if (splitTrouve) {
                                nbPersonnesAffiche = sommeAssigneeVehicule;
                            }
                        %>
                        <tr>
                            <td><%= r.getIdReservation() %></td>
                            <td><%= r.getClientId() %></td>
                            <td><%= nbPersonnesAffiche %></td>
                            <td><%= r.getDateHeureArrivee() %></td>
                            <td><%= r.getHotelId() %></td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
                <p><strong>Total personnes dans ce véhicule :</strong> <%= vp.getTotalPersonnes() %></p>
                
                <!-- Sprint 3 : Affichage des voyages multiples -->
                <% 
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                if (vp.getVoyages() != null && !vp.getVoyages().isEmpty()) { 
                %>
                    <div style="background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin-top: 15px;">
                        <h3 style="margin-top: 0; color: #2e7d32;">
                            🚐 <%= vp.getNombreVoyages() %> Voyage<%= vp.getNombreVoyages() > 1 ? "s" : "" %> effectué<%= vp.getNombreVoyages() > 1 ? "s" : "" %>
                            <span style="font-size: 14px; font-weight: normal; color: #666;">
                                (Distance totale cumulée: <%= vp.getDistanceTotaleTousVoyages() %> km)
                            </span>
                        </h3>
                        
                        <% 
                        int voyageNum = 0;
                        for (VoyageDTO voyage : vp.getVoyages()) { 
                            voyageNum++;
                            String bgColor = voyageNum % 2 == 1 ? "#e3f2fd" : "#fff3e0";
                            String titleColor = voyageNum % 2 == 1 ? "#1976d2" : "#f57c00";
                        %>
                        <div style="background-color: <%= bgColor %>; padding: 15px; border-radius: 5px; margin-bottom: 15px; border-left: 4px solid <%= titleColor %>;">
                            <h4 style="margin-top: 0; color: <%= titleColor %>;">
                                ✈️ Voyage <%= voyage.getNumeroVoyage() %> 
                                <span style="font-size: 13px; font-weight: normal; color: #666;">
                                    (<%= voyage.getTotalPersonnes() %> personne<%= voyage.getTotalPersonnes() > 1 ? "s" : "" %>, 
                                    <%= voyage.getDureeVoyage() %> minutes)
                                </span>
                            </h4>
                            
                            <div style="display: flex; gap: 30px; margin-bottom: 15px; flex-wrap: wrap;">
                                <div>
                                    <strong>🛫 Départ :</strong> <%= voyage.getHeureDepart().format(formatter) %>
                                </div>
                                <div>
                                    <strong>🛬 Retour :</strong> <%= voyage.getHeureRetour().format(formatter) %>
                                </div>
                                <div>
                                    <strong>📏 Distance :</strong> <%= voyage.getDistanceTotale() %> km
                                </div>
                            </div>
                            
                            <!-- Réservations de ce voyage -->
                            <div style="margin-bottom: 10px;">
                                <strong>👥 Réservations :</strong>
                                <% for (Reservation r : voyage.getReservations()) { %>
                                    <% int nbAssigne = voyage.getPassagersAssignesPourReservation(r); %>
                                    <span style="background-color: white; padding: 3px 8px; border-radius: 3px; margin-right: 5px; display: inline-block; margin-bottom: 3px;">
                                        <%= r.getClientId() %> (<%= nbAssigne %> pers. assignée<%= nbAssigne > 1 ? "s" : "" %><%= nbAssigne < r.getNbrPers() ? " / split" : "" %>)
                                    </span>
                                <% } %>
                            </div>
                            
                            <!-- Itinéraire détaillé du voyage -->
                            <% if (voyage.getDetailsTrajet() != null && !voyage.getDetailsTrajet().isEmpty()) { %>
                            <h5 style="margin-bottom: 10px; color: #424242;">🗺️ Itinéraire</h5>
                            <table style="background-color: white; font-size: 13px;">
                                <thead>
                                    <tr>
                                        <th style="width: 50px;">Ordre</th>
                                        <th>Hôtel</th>
                                        <th>Heure d'arrivée</th>
                                        <th style="width: 100px;">Segment (km)</th>
                                        <th style="width: 100px;">Cumulée (km)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% for (TrajetDetailDTO detail : voyage.getDetailsTrajet()) { %>
                                    <tr>
                                        <td style="text-align: center;"><%= detail.getOrdre() %></td>
                                        <td><strong><%= detail.getNomHotel() %></strong></td>
                                        <td><%= detail.getHeureArrivee().format(formatter) %></td>
                                        <td style="text-align: right;"><%= detail.getDistanceSegment() %> km</td>
                                        <td style="text-align: right;"><strong><%= detail.getDistanceCumulee() %> km</strong></td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                <% } %>
            </div>
        <% 
            }
        } else { 
        %>
            <p>Aucun véhicule assigné.</p>
        <% } %>
        
        <!-- Liste des réservations non assignées -->
        <% 
        List<Reservation> nonAssignees = (List<Reservation>) request.getAttribute("reservationsNonAssignees");
        if (nonAssignees != null && !nonAssignees.isEmpty()) { 
        %>
        <div class="non-assignees">
            <h2>⚠️ Réservations Non Assignées</h2>
            <table>
                <thead>
                    <tr>
                        <th>ID Réservation</th>
                        <th>Client</th>
                        <th>Nb Personnes</th>
                        <th>Heure Arrivée</th>
                        <th>Hôtel</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Reservation r : nonAssignees) { %>
                    <tr>
                        <td><%= r.getIdReservation() %></td>
                        <td><%= r.getClientId() %></td>
                        <td><%= r.getNbrPers() %></td>
                        <td><%= r.getDateHeureArrivee() %></td>
                        <td><%= r.getHotelId() %></td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
        <% } %>
        
        <a href="planification/form" class="btn-retour">← Retour au formulaire</a>
    </div>
</body>
</html>