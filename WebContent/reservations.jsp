<%@ page import="com.back.repository.HotelRepository,com.back.repository.ReservationRepository,com.back.service.ReservationService,com.back.models.Hotel,com.back.models.Reservation" %>
<%@ page import="java.util.List,java.time.LocalDate,java.time.LocalDateTime" %>
<%
// Configuration connexion (adapter si necessaire)
String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
String user = "postgres";
String password = "kanto";

HotelRepository hotelRepo = new HotelRepository(url, user, password);
ReservationRepository reservationRepo = new ReservationRepository(url, user, password);
ReservationService service = new ReservationService(reservationRepo, hotelRepo);

String message = null;
String error = null;

// Traitement POST pour ajout de reservation
if ("POST".equalsIgnoreCase(request.getMethod())) {
    String clientId = request.getParameter("client_id");
    String nbrPersStr = request.getParameter("nbr_pers");
    String dateHeureStr = request.getParameter("date_heure");
    String hotelIdStr = request.getParameter("hotel_id");

    try {
        int nbrPers = Integer.parseInt(nbrPersStr);
        int hotelId = Integer.parseInt(hotelIdStr);
        LocalDateTime dateHeure = null;
        try {
            dateHeure = LocalDateTime.parse(dateHeureStr);
        } catch (Exception e) {
            // ajouter secondes si necessaire
            if (!dateHeureStr.contains(":")) {
                throw e;
            }
            dateHeure = LocalDateTime.parse(dateHeureStr + ":00");
        }

        Reservation r = new Reservation(0, clientId, nbrPers, dateHeure, hotelId);
        service.insertReservation(r);
        message = "Reservation ajoutee avec succès.";
    } catch (Exception ex) {
        error = "Erreur lors de l'ajout : " + ex.getMessage();
    }
}

// Recuperation des hôtels pour le select
List<Hotel> hotels = hotelRepo.findAllHotels();

// Filtrage par date (GET)
String dateParam = request.getParameter("date");
List<Reservation> reservations;
if (dateParam != null && !dateParam.isEmpty()) {
    try {
        LocalDate d = LocalDate.parse(dateParam);
        reservations = reservationRepo.findByDate(d);
    } catch (Exception e) {
        reservations = reservationRepo.findAll();
        error = (error == null ? "" : error + " ") + "Format de date invalide pour le filtre.";
    }
} else {
    reservations = reservationRepo.findAll();
}
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

    <h2>Filtrer par date</h2>
    <form method="get" action="reservations.jsp">
        <label for="date">Date (YYYY-MM-DD) :</label>
        <input type="date" id="date" name="date" value="<%= (dateParam!=null?dateParam:"") %>">
        <div style="margin-top:8px">
            <input type="submit" value="Filtrer">
            <a href="reservations.jsp">Afficher tout</a>
        </div>
    </form>

    <h2>Ajouter une reservation</h2>
    <form method="post" action="reservations.jsp">
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

    <h2>Liste des reservations</h2>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Client</th>
                <th>Personnes</th>
                <th>Date/Heure</th>
                <th>Hôtel ID</th>
            </tr>
        </thead>
        <tbody>
            <% if (reservations == null || reservations.isEmpty()) { %>
                <tr><td colspan="5">Aucune reservation</td></tr>
            <% } else {
                for (Reservation r : reservations) { %>
                    <tr>
                        <td><%= r.getIdReservation() %></td>
                        <td><%= r.getClientId() %></td>
                        <td><%= r.getNbrPers() %></td>
                        <td><%= r.getDateHeureArrivee() %></td>
                        <td><%= r.getHotelId() %></td>
                    </tr>
            <%  }
            } %>
        </tbody>
    </table>

</body>
</html>