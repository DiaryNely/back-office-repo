package com.example.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.dao.PlanningDAO;
import com.example.dao.VehiculeDAO;
import com.example.model.PlanningParametre;
import com.example.model.PlanningReservation;
import com.example.model.PlanningResult;
import com.example.model.PlanningVehiculeTour;
import com.example.model.Vehicule;

public class PlanningService {

    private static final String AEROPORT_CODE = "AER";

    private final PlanningDAO planningDAO;
    private final VehiculeDAO vehiculeDAO;
    private final Random random;

    public PlanningService() {
        this.planningDAO = new PlanningDAO();
        this.vehiculeDAO = new VehiculeDAO();
        this.random = new Random();
    }

    public PlanningResult planifier(String dateValue) throws SQLException {
        LocalDate date = parseDate(dateValue);

        List<Vehicule> vehicules = vehiculeDAO.findAll();
        List<PlanningReservation> reservations = planningDAO.findReservationsByDate(date);

        planningDAO.clearAssignmentsByDate(date);

        Map<Integer, List<PlanningReservation>> reservationsParVehicule = new HashMap<>();
        List<PlanningReservation> nonAssignees = new ArrayList<>();

        for (PlanningReservation reservation : reservations) {
            Vehicule vehiculeChoisi = choisirVehicule(vehicules, reservation.getNombrePassager());

            if (vehiculeChoisi == null) {
                nonAssignees.add(reservation);
                continue;
            }

            reservation.setIdVehicule(vehiculeChoisi.getId());
            reservation.setVehiculeReference(vehiculeChoisi.getReference());
            planningDAO.assignVehicule(reservation.getId(), vehiculeChoisi.getId());

            reservationsParVehicule.computeIfAbsent(vehiculeChoisi.getId(), key -> new ArrayList<>())
                    .add(reservation);
        }

        PlanningParametre parametre = planningDAO.getPlanningParametre();
        Map<String, BigDecimal> distances = planningDAO.getDistanceMap();

        List<PlanningVehiculeTour> toursAssignes = new ArrayList<>();

        for (Vehicule vehicule : vehicules) {
            List<PlanningReservation> assignees = reservationsParVehicule.getOrDefault(vehicule.getId(),
                    Collections.emptyList());

            if (assignees.isEmpty()) {
                continue;
            }

            PlanningVehiculeTour tour = calculerTour(vehicule, assignees, parametre, distances, date);
            toursAssignes.add(tour);
        }

        PlanningResult result = new PlanningResult();
        result.setDate(date);
        result.setVehiculesDisponibles(vehicules);
        result.setToursAssignes(toursAssignes);
        result.setReservationsNonAssignees(nonAssignees);
        result.setTotalReservations(reservations.size());
        return result;
    }

    private LocalDate parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            throw new IllegalArgumentException("La date de planification est obligatoire.");
        }

        try {
            return LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format date invalide. Utilisez yyyy-MM-dd.");
        }
    }

    private Vehicule choisirVehicule(List<Vehicule> vehicules, Integer nombrePassager) {
        if (nombrePassager == null || nombrePassager <= 0) {
            return null;
        }

        List<Vehicule> candidats = vehicules.stream()
                .filter(v -> v.getNombrePlaces() != null && v.getNombrePlaces() >= nombrePassager)
                .collect(Collectors.toList());

        if (candidats.isEmpty()) {
            return null;
        }

        int capaciteMinimale = candidats.stream()
                .map(Vehicule::getNombrePlaces)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);

        List<Vehicule> capaciteOptimale = candidats.stream()
                .filter(v -> v.getNombrePlaces() != null && v.getNombrePlaces() == capaciteMinimale)
                .collect(Collectors.toList());

        List<Vehicule> diesel = capaciteOptimale.stream()
                .filter(v -> "D".equalsIgnoreCase(v.getTypeCarburantCode()))
                .collect(Collectors.toList());

        List<Vehicule> pool = !diesel.isEmpty() ? diesel : capaciteOptimale;

        if (pool.size() == 1) {
            return pool.get(0);
        }

        return pool.get(random.nextInt(pool.size()));
    }

    private PlanningVehiculeTour calculerTour(Vehicule vehicule,
            List<PlanningReservation> reservations,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date) {

        List<PlanningReservation> sortedReservations = new ArrayList<>(reservations);
        sortedReservations.sort(Comparator.comparing(
                reservation -> reservation.getDateHeureArrivee() != null
                        ? reservation.getDateHeureArrivee()
                        : LocalDateTime.of(date, LocalTime.MIDNIGHT)));

        RouteComputation routeComputation = computeNearestNeighborRoute(sortedReservations, distances);

        BigDecimal vitesse = parametre.getVitesseMoyenne();
        if (vitesse == null || vitesse.compareTo(BigDecimal.ZERO) <= 0) {
            vitesse = BigDecimal.valueOf(50);
        }

        int waitingMinutes = Math.max(0, routeComputation.destinationCount - 1)
                * Math.max(0, parametre.getTempsAttente());

        int travelMinutes = routeComputation.totalKm
                .divide(vitesse, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        int totalMinutes = travelMinutes + waitingMinutes;

        LocalDateTime heureDepart = sortedReservations.stream()
                .map(PlanningReservation::getDateHeureArrivee)
                .filter(value -> value != null)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.of(date, LocalTime.of(8, 0)));

        PlanningVehiculeTour tour = new PlanningVehiculeTour();
        tour.setVehicule(vehicule);
        tour.setReservations(sortedReservations);
        tour.setRoute(routeComputation.routeLabel);
        tour.setDistanceTotaleKm(routeComputation.totalKm.setScale(2, RoundingMode.HALF_UP));
        tour.setDureeTotaleMinutes(totalMinutes);
        tour.setHeureDepart(heureDepart);
        tour.setHeureRetour(heureDepart.plusMinutes(totalMinutes));
        return tour;
    }

    private RouteComputation computeNearestNeighborRoute(List<PlanningReservation> reservations,
            Map<String, BigDecimal> distances) {
        Set<String> remainingDestinations = new LinkedHashSet<>();

        for (PlanningReservation reservation : reservations) {
            if (reservation.getLieuCode() != null && !reservation.getLieuCode().isBlank()) {
                remainingDestinations.add(reservation.getLieuCode().trim());
            }
        }

        if (remainingDestinations.isEmpty()) {
            return new RouteComputation("AER", BigDecimal.ZERO, 0);
        }

        String current = AEROPORT_CODE;
        List<String> route = new ArrayList<>();
        route.add(AEROPORT_CODE);

        BigDecimal totalKm = BigDecimal.ZERO;

        while (!remainingDestinations.isEmpty()) {
            String next = findNearest(current, remainingDestinations, distances);
            totalKm = totalKm.add(getDistance(current, next, distances));
            route.add(next);
            remainingDestinations.remove(next);
            current = next;
        }

        totalKm = totalKm.add(getDistance(current, AEROPORT_CODE, distances));
        route.add(AEROPORT_CODE);

        return new RouteComputation(String.join(" -> ", route), totalKm, Math.max(0, route.size() - 2));
    }

    private String findNearest(String from,
            Set<String> destinations,
            Map<String, BigDecimal> distances) {
        String nearest = null;
        BigDecimal bestKm = null;

        for (String destination : destinations) {
            BigDecimal km = getDistance(from, destination, distances);

            if (bestKm == null || km.compareTo(bestKm) < 0
                    || (km.compareTo(bestKm) == 0 && destination.compareTo(nearest) < 0)) {
                nearest = destination;
                bestKm = km;
            }
        }

        if (nearest == null) {
            return destinations.iterator().next();
        }

        return nearest;
    }

    private BigDecimal getDistance(String from,
            String to,
            Map<String, BigDecimal> distances) {
        BigDecimal direct = distances.get(distanceKey(from, to));
        if (direct != null) {
            return direct;
        }

        BigDecimal reverse = distances.get(distanceKey(to, from));
        if (reverse != null) {
            return reverse;
        }

        return BigDecimal.ZERO;
    }

    private String distanceKey(String from, String to) {
        return from + "->" + to;
    }

    private static class RouteComputation {

        private final String routeLabel;
        private final BigDecimal totalKm;
        private final int destinationCount;

        private RouteComputation(String routeLabel, BigDecimal totalKm, int destinationCount) {
            this.routeLabel = routeLabel;
            this.totalKm = totalKm;
            this.destinationCount = destinationCount;
        }
    }
}