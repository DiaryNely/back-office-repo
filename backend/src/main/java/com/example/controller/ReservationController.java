package com.example.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.model.Hotel;
import com.example.model.ReservationListItem;
import com.example.service.ReservationService;
import com.myframework.annotations.Controller;
import com.myframework.annotations.GetMapping;
import com.myframework.annotations.Json;
import com.myframework.annotations.PostMapping;
import com.myframework.annotations.RequestParam;
import com.myframework.core.ModelView;

@Controller
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController() {
        this.reservationService = new ReservationService();
    }

    @GetMapping("/")
    public ModelView home() throws SQLException {
        return showCreationForm();
    }

    @GetMapping("/reservations/new")
    public ModelView showCreationForm() throws SQLException {
        List<Hotel> hotels = reservationService.getHotelsDisponibles();

        ModelView mv = new ModelView("/reservation-form.jsp");
        mv.addData("hotels", hotels);
        return mv;
    }

    @Json
    @GetMapping("/api/reservations")
    public List<ReservationListItem> listReservations(
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin) throws SQLException {
        return reservationService.getReservations(dateDebut, dateFin);
    }

    @PostMapping("/reservations/new")
    public ModelView createReservation(
            @RequestParam("clientId") String clientId,
            @RequestParam("nombrePassager") Integer nombrePassager,
            @RequestParam("dateHeureArrivee") String dateHeureArrivee,
            @RequestParam("idHotel") Integer idHotel) throws SQLException {

        ModelView mv = new ModelView("/reservation-form.jsp");
        List<Hotel> hotels = reservationService.getHotelsDisponibles();
        mv.addData("hotels", hotels);

        Map<String, Object> formData = new HashMap<>();
        formData.put("clientId", clientId);
        formData.put("nombrePassager", nombrePassager);
        formData.put("dateHeureArrivee", dateHeureArrivee);
        formData.put("idHotel", idHotel);
        mv.addData("formData", formData);

        try {
            int newReservationId = reservationService.creerReservation(
                    clientId,
                    nombrePassager,
                    dateHeureArrivee,
                    idHotel);

            mv.addData("successMessage", "Réservation enregistrée avec succès (ID " + newReservationId + ").");
        } catch (IllegalArgumentException e) {
            mv.addData("errorMessage", e.getMessage());
        }

        return mv;
    }
}
