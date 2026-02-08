package com.back.service;

import com.back.models.Hotel;
import com.back.models.Reservation;
import com.back.repository.HotelRepository;
import com.back.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service métier pour la gestion des réservations.
 * Utilise le framework pour les logs et la gestion.
 */
public class ReservationService {

    private static final Logger logger = Logger.getLogger(ReservationService.class.getName());
    private final ReservationRepository reservationRepo;
    private final HotelRepository hotelRepo;

    public ReservationService(ReservationRepository reservationRepo, HotelRepository hotelRepo) {
        this.reservationRepo = reservationRepo;
        this.hotelRepo = hotelRepo;
    }

    public void insertReservation(Reservation r) {
        logger.info("Insertion d'une réservation pour le client : " + r.getClientId());
        reservationRepo.insertReservation(r);
    }

    public List<Hotel> getHotels() {
        logger.info("Récupération de la liste des hôtels");
        return hotelRepo.findAllHotels();
    }

    public List<Reservation> getReservations(LocalDate date) {
        if (date == null) {
            logger.info("Récupération de toutes les réservations");
            return reservationRepo.findAll();
        } else {
            logger.info("Récupération des réservations pour la date : " + date);
            return reservationRepo.findByDate(date);
        }
    }
}
