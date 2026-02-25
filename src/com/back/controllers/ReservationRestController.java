package com.app.controllers;

import com.app.models.Hotel;
import com.app.models.Reservation;
import com.app.service.ReservationService;
import com.app.repository.HotelRepository;
import com.app.repository.ReservationRepository;
import com.framework.annotation.Controller;
import com.framework.annotation.HandleGet;
import com.framework.annotation.JsonResponse;
import com.framework.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ReservationRestController {

    private final ReservationService service;

    public ReservationRestController() {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String user = "postgres";
        String password = "postgres";
        ReservationRepository reservationRepo = new ReservationRepository(url, user, password);
        HotelRepository hotelRepo = new HotelRepository(url, user, password);
        this.service = new ReservationService(reservationRepo, hotelRepo);
    }

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
}
