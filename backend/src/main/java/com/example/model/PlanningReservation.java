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
    private String groupReference;
    private Integer ordrePassage;
    private String fractionReference;
    private Integer nombrePassagerOriginal;
    private Integer nombrePassagerAssigne;
    private String raisonNonAssignation;
    private LocalDateTime heureDepartReelle;
    private LocalDateTime heureArriveeAeroport;
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

    public String getGroupReference() {
        return groupReference;
    }

    public void setGroupReference(String groupReference) {
        this.groupReference = groupReference;
    }

    public Integer getOrdrePassage() {
        return ordrePassage;
    }

    public void setOrdrePassage(Integer ordrePassage) {
        this.ordrePassage = ordrePassage;
    }

    public String getFractionReference() {
        return fractionReference;
    }

    public void setFractionReference(String fractionReference) {
        this.fractionReference = fractionReference;
    }

    public Integer getNombrePassagerOriginal() {
        return nombrePassagerOriginal;
    }

    public void setNombrePassagerOriginal(Integer nombrePassagerOriginal) {
        this.nombrePassagerOriginal = nombrePassagerOriginal;
    }

    public Integer getNombrePassagerAssigne() {
        return nombrePassagerAssigne;
    }

    public void setNombrePassagerAssigne(Integer nombrePassagerAssigne) {
        this.nombrePassagerAssigne = nombrePassagerAssigne;
    }

    public String getRaisonNonAssignation() {
        return raisonNonAssignation;
    }

    public void setRaisonNonAssignation(String raisonNonAssignation) {
        this.raisonNonAssignation = raisonNonAssignation;
    }

    public LocalDateTime getHeureDepartReelle() {
        return heureDepartReelle;
    }

    public void setHeureDepartReelle(LocalDateTime heureDepartReelle) {
        this.heureDepartReelle = heureDepartReelle;
    }

    public LocalDateTime getHeureArriveeAeroport() {
        return heureArriveeAeroport;
    }

    public void setHeureArriveeAeroport(LocalDateTime heureArriveeAeroport) {
        this.heureArriveeAeroport = heureArriveeAeroport;
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