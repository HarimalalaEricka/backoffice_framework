package com.app.controllers;

import com.framework.annotation.*;
import com.framework.model.ModelView;
import com.app.util.Connexion;
import com.app.models.Reservation;

import java.sql.Connection;
import com.app.models.Hotel;
import com.app.repository.ReservationRepository;
import com.app.repository.HotelRepository;
import com.app.service.ReservationService;
import com.framework.annotation.RequestParam;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class ReservationController {
    private Connexion connexion;

    public ReservationController() {
        // Initialiser la connexion à la base de données
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres"; // À adapter si un mot de passe est défini
        this.connexion = new Connexion(url, username, password);
    }


    @HandleGet("/reservations/insert")
    public ModelView insertForm() {
        ModelView mv = new ModelView();
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres"; // adapter si nécessaire

        HotelRepository hotelRepo = new HotelRepository(url, username, password);
        ReservationRepository reservationRepo = new ReservationRepository(url, username, password);
        mv.addAttribute("hotels", hotelRepo.findAllHotels());
        mv.addAttribute("reservations", reservationRepo.findAll());
        mv.setView("/reservations.jsp");
        return mv;
    }

    @HandlePost("/reservations/insert")
    public ModelView handleInsert(@RequestParam("client_id") String clientId,
                                  @RequestParam("nbr_pers") int nbrPers,
                                  @RequestParam("date_heure") String dateHeure,
                                  @RequestParam("hotel_id") int hotelId) {
        ModelView mv = new ModelView();
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres";

        try {
            java.time.LocalDateTime dt;
            try {
                dt = java.time.LocalDateTime.parse(dateHeure);
            } catch (Exception ex) {
                if (dateHeure != null && dateHeure.length() == 16) {
                    dt = java.time.LocalDateTime.parse(dateHeure + ":00");
                } else {
                    throw ex;
                }
            }

            ReservationRepository reservationRepo = new ReservationRepository(url, username, password);
            HotelRepository hotelRepo = new HotelRepository(url, username, password);
            ReservationService service = new ReservationService(reservationRepo, hotelRepo);

            Reservation r = new Reservation(0, clientId, nbrPers, dt, hotelId);
            service.insertReservation(r);
            mv.addAttribute("message", "Réservation ajoutée avec succès.");
            // refresh lists for view
            mv.addAttribute("hotels", hotelRepo.findAllHotels());
            mv.addAttribute("reservations", reservationRepo.findAll());
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de l'ajout : " + e.getMessage());
        }

        mv.setView("/reservations.jsp");
        return mv;
    }
}