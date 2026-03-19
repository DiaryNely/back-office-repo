package com.example.model;

import java.math.BigDecimal;

public class PlanningParametre {

    private BigDecimal vitesseMoyenne;
    private Integer tempsAttente;

    public BigDecimal getVitesseMoyenne() {
        return vitesseMoyenne;
    }

    public void setVitesseMoyenne(BigDecimal vitesseMoyenne) {
        this.vitesseMoyenne = vitesseMoyenne;
    }

    public Integer getTempsAttente() {
        return tempsAttente;
    }

    public void setTempsAttente(Integer tempsAttente) {
        this.tempsAttente = tempsAttente;
    }
}