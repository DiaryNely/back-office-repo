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
    private LocalDateTime dateHeureDepartReelle;
    private Integer idVehicule;
    private String vehiculeReference;
    private Integer passagersInitiaux;
    private Integer passagersAssignes;
    private Integer passagersRestants;
    private String statutAssignation;
    private Integer passagersAssignesSurTour;

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

    public LocalDateTime getDateHeureDepartReelle() {
        return dateHeureDepartReelle;
    }

    public void setDateHeureDepartReelle(LocalDateTime dateHeureDepartReelle) {
        this.dateHeureDepartReelle = dateHeureDepartReelle;
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

    public Integer getPassagersInitiaux() {
        return passagersInitiaux;
    }

    public void setPassagersInitiaux(Integer passagersInitiaux) {
        this.passagersInitiaux = passagersInitiaux;
    }

    public Integer getPassagersAssignes() {
        return passagersAssignes;
    }

    public void setPassagersAssignes(Integer passagersAssignes) {
        this.passagersAssignes = passagersAssignes;
    }

    public Integer getPassagersRestants() {
        return passagersRestants;
    }

    public void setPassagersRestants(Integer passagersRestants) {
        this.passagersRestants = passagersRestants;
    }

    public String getStatutAssignation() {
        return statutAssignation;
    }

    public void setStatutAssignation(String statutAssignation) {
        this.statutAssignation = statutAssignation;
    }

    public Integer getPassagersAssignesSurTour() {
        return passagersAssignesSurTour;
    }

    public void setPassagersAssignesSurTour(Integer passagersAssignesSurTour) {
        this.passagersAssignesSurTour = passagersAssignesSurTour;
    }
}