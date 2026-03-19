package com.example.model;

import java.time.LocalDateTime;

public class PlanningReservation {

    private Integer id;
    private String clientId;
    private Integer nombrePassager;
    private LocalDateTime dateHeureArrivee;
    private Integer idHotel;
    private String hotelNom;
    private String lieuCode;
    private String lieuLibelle;
    private String volReference;
    private Integer ordrePassage;
    private Integer idVehicule;
    private String vehiculeReference;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getNombrePassager() {
        return nombrePassager;
    }

    public void setNombrePassager(Integer nombrePassager) {
        this.nombrePassager = nombrePassager;
    }

    public LocalDateTime getDateHeureArrivee() {
        return dateHeureArrivee;
    }

    public void setDateHeureArrivee(LocalDateTime dateHeureArrivee) {
        this.dateHeureArrivee = dateHeureArrivee;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }

    public String getHotelNom() {
        return hotelNom;
    }

    public void setHotelNom(String hotelNom) {
        this.hotelNom = hotelNom;
    }

    public String getLieuCode() {
        return lieuCode;
    }

    public void setLieuCode(String lieuCode) {
        this.lieuCode = lieuCode;
    }

    public String getLieuLibelle() {
        return lieuLibelle;
    }

    public void setLieuLibelle(String lieuLibelle) {
        this.lieuLibelle = lieuLibelle;
    }

    public String getVolReference() {
        return volReference;
    }

    public void setVolReference(String volReference) {
        this.volReference = volReference;
    }

    public Integer getOrdrePassage() {
        return ordrePassage;
    }

    public void setOrdrePassage(Integer ordrePassage) {
        this.ordrePassage = ordrePassage;
    }

    public Integer getIdVehicule() {
        return idVehicule;
    }

    public void setIdVehicule(Integer idVehicule) {
        this.idVehicule = idVehicule;
    }

    public String getVehiculeReference() {
        return vehiculeReference;
    }

    public void setVehiculeReference(String vehiculeReference) {
        this.vehiculeReference = vehiculeReference;
    }
}