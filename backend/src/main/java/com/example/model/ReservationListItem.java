package com.example.model;

public class ReservationListItem {

    private Integer id;
    private String clientId;
    private Integer nombrePassager;
    private String dateHeureArrivee;
    private Integer idHotel;

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

    public String getDateHeureArrivee() {
        return dateHeureArrivee;
    }

    public void setDateHeureArrivee(String dateHeureArrivee) {
        this.dateHeureArrivee = dateHeureArrivee;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }
}
