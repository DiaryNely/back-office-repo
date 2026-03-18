package com.example.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.example.dao.HotelDAO;
import com.example.dao.ReservationDAO;
import com.example.model.Hotel;
import com.example.model.Reservation;
import com.example.model.ReservationListItem;

public class ReservationService {

    private final ReservationDAO reservationDAO;
    private final HotelDAO hotelDAO;

    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
        this.hotelDAO = new HotelDAO();
    }

    public List<Hotel> getHotelsDisponibles() throws SQLException {
        return hotelDAO.findAll();
    }

    public List<ReservationListItem> getReservations(String dateDebut, String dateFin) throws SQLException {
        LocalDate start = parseDate(dateDebut, "dateDebut");
        LocalDate end = parseDate(dateFin, "dateFin");

        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin doit être supérieure ou égale à la date de début.");
        }

        return reservationDAO.findByDateRange(start, end);
    }

    public int creerReservation(String clientId,
            Integer nombrePassager,
            String dateHeureArrivee,
            Integer idHotel) throws SQLException {

        validateClientId(clientId);
        validateNombrePassager(nombrePassager);
        validateHotel(idHotel);
        LocalDateTime dateArrivee = parseAndValidateDateHeureArrivee(dateHeureArrivee);

        Reservation reservation = new Reservation(clientId.trim(), nombrePassager, dateArrivee, idHotel);
        return reservationDAO.insert(reservation);
    }

    private void validateClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Le client est obligatoire.");
        }

        String normalizedClientId = clientId.trim();

        if (!normalizedClientId.matches("\\d{4}")) {
            throw new IllegalArgumentException("Le client doit être un identifiant sur 4 chiffres.");
        }
    }

    private void validateNombrePassager(Integer nombrePassager) {
        if (nombrePassager == null) {
            throw new IllegalArgumentException("Le nombre de passagers est obligatoire.");
        }

        if (nombrePassager <= 0) {
            throw new IllegalArgumentException("Le nombre de passagers doit être supérieur à 0.");
        }
    }

    private void validateHotel(Integer idHotel) throws SQLException {
        if (idHotel == null) {
            throw new IllegalArgumentException("L'hôtel est obligatoire.");
        }

        if (!hotelDAO.existsById(idHotel)) {
            throw new IllegalArgumentException("L'hôtel sélectionné n'existe pas.");
        }
    }

    private LocalDateTime parseAndValidateDateHeureArrivee(String dateHeureArrivee) {
        if (dateHeureArrivee == null || dateHeureArrivee.isBlank()) {
            throw new IllegalArgumentException("La date et l'heure d'arrivée sont obligatoires.");
        }

        String normalized = dateHeureArrivee.trim();

        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException first) {
            try {
                return LocalDateTime.parse(normalized + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException second) {
                throw new IllegalArgumentException("Format date/heure invalide. Format attendu : yyyy-MM-ddTHH:mm");
            }
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format invalide pour " + fieldName + ". Format attendu: yyyy-MM-dd");
        }
    }
}
