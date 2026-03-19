package com.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlanningResult {

    private LocalDate date;
    private List<Vehicule> vehiculesDisponibles;
    private List<PlanningVehiculeTour> toursAssignes;
    private List<PlanningReservation> reservationsNonAssignees;
    private Integer totalReservations;

    public PlanningResult() {
        this.vehiculesDisponibles = new ArrayList<>();
        this.toursAssignes = new ArrayList<>();
        this.reservationsNonAssignees = new ArrayList<>();
        this.totalReservations = 0;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<Vehicule> getVehiculesDisponibles() {
        return vehiculesDisponibles;
    }

    public void setVehiculesDisponibles(List<Vehicule> vehiculesDisponibles) {
        this.vehiculesDisponibles = vehiculesDisponibles;
    }

    public List<PlanningVehiculeTour> getToursAssignes() {
        return toursAssignes;
    }

    public void setToursAssignes(List<PlanningVehiculeTour> toursAssignes) {
        this.toursAssignes = toursAssignes;
    }

    public List<PlanningReservation> getReservationsNonAssignees() {
        return reservationsNonAssignees;
    }

    public void setReservationsNonAssignees(List<PlanningReservation> reservationsNonAssignees) {
        this.reservationsNonAssignees = reservationsNonAssignees;
    }

    public Integer getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(Integer totalReservations) {
        this.totalReservations = totalReservations;
    }
}