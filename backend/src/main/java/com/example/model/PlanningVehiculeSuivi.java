package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class PlanningVehiculeSuivi {

    private Vehicule vehicule;
    private Integer nombreTrajets;
    private List<PlanningVehiculeTour> historiqueTrajets;

    public PlanningVehiculeSuivi() {
        this.historiqueTrajets = new ArrayList<>();
        this.nombreTrajets = 0;
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
}
