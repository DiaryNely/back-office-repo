package com.example.model;

public class Vehicule {

    private Integer id;
    private String reference;
    private Integer nombrePlaces;
    private Integer typeCarburantId;
    private String typeCarburantCode;
    private String typeCarburantNom;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getNombrePlaces() {
        return nombrePlaces;
    }

    public void setNombrePlaces(Integer nombrePlaces) {
        this.nombrePlaces = nombrePlaces;
    }

    public Integer getTypeCarburantId() {
        return typeCarburantId;
    }

    public void setTypeCarburantId(Integer typeCarburantId) {
        this.typeCarburantId = typeCarburantId;
    }

    public String getTypeCarburantCode() {
        return typeCarburantCode;
    }

    public void setTypeCarburantCode(String typeCarburantCode) {
        this.typeCarburantCode = typeCarburantCode;
    }

    public String getTypeCarburantNom() {
        return typeCarburantNom;
    }

    public void setTypeCarburantNom(String typeCarburantNom) {
        this.typeCarburantNom = typeCarburantNom;
    }
}
