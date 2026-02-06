package com.back.service;

import com.back.models.Hotel;
import com.back.models.Reservation;
import com.back.repository.HotelRepository;
import com.back.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final HotelRepository hotelRepo;

    public ReservationService(ReservationRepository reservationRepo, HotelRepository hotelRepo) {
        this.reservationRepo = reservationRepo;
        this.hotelRepo = hotelRepo;
    }

    public void insertReservation(Reservation r) { reservationRepo.insertReservation(r); }
    public List<Hotel> getHotels() { return hotelRepo.findAllHotels(); }
    public List<Reservation> getReservations(LocalDate date) { 
        return date == null ? reservationRepo.findAll() : reservationRepo.findByDate(date); 
    }
}
