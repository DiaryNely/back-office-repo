package com.example.model;

import java.time.LocalDateTime;

public class Reservation {

    private Integer id;
    private String clientId;
    private Integer nombrePassager;
    private LocalDateTime dateHeureArrivee;
    private Integer idHotel;

    public Reservation() {
    }

    public Reservation(String clientId, Integer nombrePassager, LocalDateTime dateHeureArrivee, Integer idHotel) {
        this.clientId = clientId;
        this.nombrePassager = nombrePassager;
        this.dateHeureArrivee = dateHeureArrivee;
        this.idHotel = idHotel;
    }

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
}
