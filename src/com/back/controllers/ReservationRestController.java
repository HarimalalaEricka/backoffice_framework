package com.app.controller;

import com.app.models.Hotel;
import com.app.models.Reservation;
import com.app.service.ReservationService;
import com.app.repository.HotelRepository;
import com.app.repository.ReservationRepository;
import com.framework.annotation.Controller;
import com.framework.annotation.HandleGet;
import com.framework.annotation.HandlePost;
import com.framework.annotation.JsonResponse;
import com.framework.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur des réservations utilisant le Framework.
 * Les annotations du framework assurent le routage automatique.
 */
@Controller
public class ReservationRestController {

    private final ReservationService service;

    /**
     * Constructeur par défaut : initialise les repositories avec des valeurs
     * par défaut pour la base `gestion_ticket`. Ajustez le mot de passe si besoin.
     */
    public ReservationRestController() {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String user = "postgres";
        String password = "postgres"; // adapter si nécessaire
        ReservationRepository reservationRepo = new ReservationRepository(url, user, password);
        HotelRepository hotelRepo = new HotelRepository(url, user, password);
        this.service = new ReservationService(reservationRepo, hotelRepo);
    }

    /** Constructeur alternatif pour injection explicite. */
    public ReservationRestController(ReservationService service) {
        this.service = service;
    }

    @HandleGet("/hotels")
    @JsonResponse
    public List<Hotel> hotels() { 
        return service.getHotels(); 
    }

    @HandleGet("/reservations")
    @JsonResponse
    public List<Reservation> reservations(@RequestParam("date") String date) {
        LocalDate d = date != null && !date.isEmpty() ? LocalDate.parse(date) : null;
        return service.getReservations(d);
    }

    @HandlePost("/reservations/insert")
    @JsonResponse
    public String insert(@RequestParam("client_id") String clientId,
                         @RequestParam("nbr_pers") int nbrPers,
                         @RequestParam("date_heure") String dateHeure,
                         @RequestParam("hotel_id") int hotelId) {
        Reservation reservation = new Reservation(0, clientId, nbrPers, 
                java.time.LocalDateTime.parse(dateHeure), hotelId);
        service.insertReservation(reservation);
        return "{\"status\": \"success\"}";
    }
}
