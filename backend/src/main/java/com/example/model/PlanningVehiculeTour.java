package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlanningVehiculeTour {

    private String groupReference;
    private String vols;
    private Integer totalPassagers;
    private Integer numeroTrajet;
    private LocalDateTime heureDepartTheorique;
    private Vehicule vehicule;
    private List<PlanningReservation> reservations;
    private String route;
    private BigDecimal distanceTotaleKm;
    private Integer dureeTotaleMinutes;
    private LocalDateTime heureDepart;
    private LocalDateTime heureRetour;

    public PlanningVehiculeTour() {
        this.reservations = new ArrayList<>();
    }

    public String getGroupReference() {
        return groupReference;
    }

    public void setGroupReference(String groupReference) {
        this.groupReference = groupReference;
    }

    public String getVols() {
        return vols;
    }

    public void setVols(String vols) {
        this.vols = vols;
    }

    public Integer getTotalPassagers() {
        return totalPassagers;
    }

    public void setTotalPassagers(Integer totalPassagers) {
        this.totalPassagers = totalPassagers;
    }

    public Integer getNumeroTrajet() {
        return numeroTrajet;
    }

    public void setNumeroTrajet(Integer numeroTrajet) {
        this.numeroTrajet = numeroTrajet;
    }

    public LocalDateTime getHeureDepartTheorique() {
        return heureDepartTheorique;
    }

    public void setHeureDepartTheorique(LocalDateTime heureDepartTheorique) {
        this.heureDepartTheorique = heureDepartTheorique;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public List<PlanningReservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<PlanningReservation> reservations) {
        this.reservations = reservations;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public BigDecimal getDistanceTotaleKm() {
        return distanceTotaleKm;
    }

    public void setDistanceTotaleKm(BigDecimal distanceTotaleKm) {
        this.distanceTotaleKm = distanceTotaleKm;
    }

    public Integer getDureeTotaleMinutes() {
        return dureeTotaleMinutes;
    }

    public void setDureeTotaleMinutes(Integer dureeTotaleMinutes) {
        this.dureeTotaleMinutes = dureeTotaleMinutes;
    }

    public LocalDateTime getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(LocalDateTime heureDepart) {
        this.heureDepart = heureDepart;
    }

    public LocalDateTime getHeureRetour() {
        return heureRetour;
    }

    public void setHeureRetour(LocalDateTime heureRetour) {
        this.heureRetour = heureRetour;
    }
}