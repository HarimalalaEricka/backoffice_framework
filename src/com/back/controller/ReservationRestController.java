package com.back.controller;

import com.back.models.Hotel;
import com.back.models.Reservation;
import com.back.service.ReservationService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationRestController {

    private final ReservationService service;

    public ReservationRestController(ReservationService service) { this.service = service; }

    @GetMapping("/hotels")
    public List<Hotel> hotels() { return service.getHotels(); }

    @GetMapping("/reservations")
    public List<Reservation> reservations(@RequestParam(required=false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : null;
        return service.getReservations(d);
    }

    @PostMapping("/reservations")
    public void insert(@RequestBody Reservation reservation) { service.insertReservation(reservation); }
}
