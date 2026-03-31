package com.example.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PlanningVehiculeSuivi {

    private Vehicule vehicule;
    private Integer nombreTrajets;
    private List<PlanningVehiculeTour> historiqueTrajets;
    private BigDecimal tauxRemplissageMoyen;
    private Integer tempsAttenteUtiliseMinutes;

    public PlanningVehiculeSuivi() {
        this.historiqueTrajets = new ArrayList<>();
        this.nombreTrajets = 0;
        this.tauxRemplissageMoyen = BigDecimal.ZERO;
        this.tempsAttenteUtiliseMinutes = 0;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public Integer getNombreTrajets() {
        return nombreTrajets;
    }

    public void setNombreTrajets(Integer nombreTrajets) {
        this.nombreTrajets = nombreTrajets;
    }

    public List<PlanningVehiculeTour> getHistoriqueTrajets() {
        return historiqueTrajets;
    }

    public void setHistoriqueTrajets(List<PlanningVehiculeTour> historiqueTrajets) {
        this.historiqueTrajets = historiqueTrajets;
    }

    public BigDecimal getTauxRemplissageMoyen() {
        return tauxRemplissageMoyen;
    }

    public void setTauxRemplissageMoyen(BigDecimal tauxRemplissageMoyen) {
        this.tauxRemplissageMoyen = tauxRemplissageMoyen;
    }

    public Integer getTempsAttenteUtiliseMinutes() {
        return tempsAttenteUtiliseMinutes;
    }

    public void setTempsAttenteUtiliseMinutes(Integer tempsAttenteUtiliseMinutes) {
        this.tempsAttenteUtiliseMinutes = tempsAttenteUtiliseMinutes;
    }
}
