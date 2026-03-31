package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlanningVehiculeTour {

    private String groupReference;
    private String vols;
    private Integer totalPassagers;
    private Vehicule vehicule;
    private List<PlanningReservation> reservations;
    private String route;
    private BigDecimal distanceTotaleKm;
    private Integer dureeTotaleMinutes;
    private LocalDateTime heureDepart;
    private LocalDateTime heureRetour;
    private String decisionDepart;
    private Integer tempsAttenteUtiliseMinutes;
    private BigDecimal tauxRemplissage;

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

    public String getDecisionDepart() {
        return decisionDepart;
    }

    public void setDecisionDepart(String decisionDepart) {
        this.decisionDepart = decisionDepart;
    }

    public Integer getTempsAttenteUtiliseMinutes() {
        return tempsAttenteUtiliseMinutes;
    }

    public void setTempsAttenteUtiliseMinutes(Integer tempsAttenteUtiliseMinutes) {
        this.tempsAttenteUtiliseMinutes = tempsAttenteUtiliseMinutes;
    }

    public BigDecimal getTauxRemplissage() {
        return tauxRemplissage;
    }

    public void setTauxRemplissage(BigDecimal tauxRemplissage) {
        this.tauxRemplissage = tauxRemplissage;
    }
}