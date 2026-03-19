package com.app.repository;

import com.app.models.Assignation;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class AssignationRepository {

    private Connexion connexion;

    public AssignationRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
        * Vérifie si une réservation possède au moins une assignation
     */
    public boolean existsByReservationId(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM Assignation WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'assignation : " + e.getMessage());
        }
        return false;
    }

    /**
     * Somme des passagers assignés pour une réservation.
     */
    public int getTotalPassagersAssignesByReservationId(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return 0;
        }

        String sql = "SELECT COALESCE(SUM(nb_pers_assigne), 0) FROM Assignation WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul des passagers assignés : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Nombre d'assignations existantes pour une réservation.
     */
    public int countByReservationId(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM Assignation WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des assignations : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retourne true si la réservation est complètement assignée.
     */
    public boolean isReservationCompletementAssignee(int reservationId, int totalReservationPassagers) {
        return getTotalPassagersAssignesByReservationId(reservationId) >= totalReservationPassagers;
    }

    /**
     * Retourne true s'il reste des passagers à assigner pour cette réservation.
     */
    public boolean hasPassagersRestants(int reservationId, int totalReservationPassagers) {
        return getTotalPassagersAssignesByReservationId(reservationId) < totalReservationPassagers;
    }

    /**
     * Retourne le nombre de passagers restants à assigner pour une réservation
     * en utilisant la valeur source de Reservation.nbr_pers.
     */
    public int getPassagersRestantsByReservationId(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return 0;
        }

        String sql = "SELECT (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne), 0)) AS restants " +
                     "FROM Reservation r " +
                     "LEFT JOIN Assignation a ON a.reservation_id = r.idReservation " +
                     "WHERE r.idReservation = ? " +
                     "GROUP BY r.idReservation, r.nbr_pers";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("restants"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul des passagers restants : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retourne true s'il reste des passagers à assigner (calcul basé sur Reservation.nbr_pers).
     */
    public boolean hasPassagersRestants(int reservationId) {
        return getPassagersRestantsByReservationId(reservationId) > 0;
    }

    /**
     * Enregistre une nouvelle assignation
     */
    public void save(Assignation assignation) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignation.getReservationId());
            ps.setInt(2, assignation.getVehiculeId());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(assignation.getDateHeurePlanification()));
            int nbPersAssigne = assignation.getNbPersAssigne();
            if (nbPersAssigne <= 0) {
                nbPersAssigne = getNbrPersReservation(assignation.getReservationId());
            }
            ps.setInt(4, nbPersAssigne);
            ps.executeUpdate();
            System.out.println("Assignation enregistrée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de l'assignation : " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les assignations pour une date donnée
     */
    public List<Assignation> findByDate(LocalDate date) {
        List<Assignation> assignations = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return assignations;
        }

        String sql = "SELECT idAssignation, reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne " +
                     "FROM Assignation WHERE DATE(date_heure_planification) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idAssignation = rs.getInt("idAssignation");
                    int reservationId = rs.getInt("reservation_id");
                    int vehiculeId = rs.getInt("vehicule_id");
                    LocalDateTime dateHeurePlanification = rs.getTimestamp("date_heure_planification").toLocalDateTime();
                    int nbPersAssigne = rs.getInt("nb_pers_assigne");
                    assignations.add(new Assignation(idAssignation, reservationId, vehiculeId, dateHeurePlanification, nbPersAssigne));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par date : " + e.getMessage());
        }
        return assignations;
    }

    /**
     * Récupère les IDs des véhicules déjà utilisés pour une date donnée
     */
    public List<Integer> findVehiculeIdsByDate(LocalDate date) {
        List<Integer> vehiculeIds = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return vehiculeIds;
        }

        String sql = "SELECT DISTINCT vehicule_id FROM Assignation WHERE DATE(date_heure_planification) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehiculeIds.add(rs.getInt("vehicule_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des véhicules utilisés : " + e.getMessage());
        }
        return vehiculeIds;
    }

    /**
     * Supprime toutes les assignations pour une date donnée
     * Sprint 4 - Permet de réinitialiser les assignations avant recalcul
     */
    public void deleteByDate(LocalDate date) {
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "DELETE FROM Assignation WHERE DATE(date_heure_planification) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            int deletedRows = ps.executeUpdate();
            System.out.println("Assignations supprimées pour la date " + date + " : " + deletedRows + " ligne(s)");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des assignations : " + e.getMessage());
        }
    }

    private int getNbrPersReservation(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            return 0;
        }

        String sql = "SELECT nbr_pers FROM Reservation WHERE idReservation = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("nbr_pers");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la lecture de la réservation : " + e.getMessage());
        }
        return 0;
    }
}
