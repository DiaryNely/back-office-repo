package com.example.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public PlanningService() {
        this.planningDAO = new PlanningDAO();
        this.vehiculeDAO = new VehiculeDAO();
    }

    public PlanningResult planifier(String dateValue) throws SQLException {
        LocalDate date = parseDate(dateValue);

        List<Vehicule> vehicules = vehiculeDAO.findAll();
        List<PlanningReservation> reservations = planningDAO.findReservationsByDate(date);

        planningDAO.clearAssignmentsByDate(date);

        PlanningParametre parametre = planningDAO.getPlanningParametre();
        int tempsAttenteMinutes = parametre.getTempsAttente() != null
                ? Math.max(0, parametre.getTempsAttente())
                : 30;

        List<ReservationGroup> groups = buildWaitingGroups(reservations, tempsAttenteMinutes);
        groups.sort((a, b) -> Integer.compare(b.totalPassagers, a.totalPassagers));

        List<PlanningReservation> nonAssignees = new ArrayList<>();
        Set<Integer> vehiculesDejaUtilises = new LinkedHashSet<>();

        for (ReservationGroup group : groups) {
            Vehicule vehiculeChoisi = choisirVehicule(vehicules, group.totalPassagers, vehiculesDejaUtilises);

            if (vehiculeChoisi == null) {
                nonAssignees.addAll(group.reservations);
                continue;
            }

            vehiculesDejaUtilises.add(vehiculeChoisi.getId());
            group.assignedVehiculeId = vehiculeChoisi.getId();

            for (PlanningReservation reservation : group.reservations) {
                reservation.setIdVehicule(vehiculeChoisi.getId());
                reservation.setVehiculeReference(vehiculeChoisi.getReference());
                reservation.setGroupReference(group.groupReference);
                planningDAO.assignVehicule(reservation.getId(), vehiculeChoisi.getId());
            }
        }

        Map<String, BigDecimal> distances = planningDAO.getDistanceMap();
        List<PlanningVehiculeTour> toursAssignes = new ArrayList<>();

        for (ReservationGroup group : groups) {
            if (group.assignedVehiculeId == null) {
                continue;
            }

            Vehicule vehicule = vehicules.stream()
                    .filter(v -> Objects.equals(v.getId(), group.assignedVehiculeId))
                    .findFirst()
                    .orElse(null);

            if (vehicule == null) {
                continue;
            }

            PlanningVehiculeTour tour = calculerTour(vehicule, group, parametre, distances, date);
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

    private Vehicule choisirVehicule(List<Vehicule> vehicules,
            Integer nombrePassager,
            Set<Integer> vehiculesDejaUtilises) {
        if (nombrePassager == null || nombrePassager <= 0) {
            return null;
        }

        List<Vehicule> candidats = vehicules.stream()
                .filter(v -> !vehiculesDejaUtilises.contains(v.getId()))
                .filter(v -> v.getNombrePlaces() != null && v.getNombrePlaces() >= nombrePassager)
                .collect(Collectors.toList());

        if (candidats.isEmpty()) {
            return null;
        }

        int capaciteProche = candidats.stream()
                .map(Vehicule::getNombrePlaces)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);

        List<Vehicule> capaciteOptimale = candidats.stream()
                .filter(v -> Objects.equals(v.getNombrePlaces(), capaciteProche))
                .collect(Collectors.toList());

        List<Vehicule> diesel = capaciteOptimale.stream()
                .filter(v -> "D".equalsIgnoreCase(v.getTypeCarburantCode()))
                .collect(Collectors.toList());

        List<Vehicule> pool = diesel.isEmpty() ? capaciteOptimale : diesel;
        if (pool.size() == 1) {
            return pool.get(0);
        }

        int randomIndex = (int) (Math.random() * pool.size());
        return pool.get(randomIndex);
    }

    private PlanningVehiculeTour calculerTour(Vehicule vehicule,
            ReservationGroup group,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date) {

        List<PlanningReservation> sortedReservations = new ArrayList<>(group.reservations);
        sortedReservations.sort(Comparator.comparing(
                reservation -> reservation.getDateHeureArrivee() != null
                        ? reservation.getDateHeureArrivee()
                        : LocalDateTime.of(date, LocalTime.MIDNIGHT)));

        RouteComputation routeComputation = computeNearestNeighborRoute(sortedReservations, distances);

        BigDecimal vitesse = parametre.getVitesseMoyenne();
        if (vitesse == null || vitesse.compareTo(BigDecimal.ZERO) <= 0) {
            vitesse = BigDecimal.valueOf(50);
        }

        int totalMinutes = routeComputation.totalKm
                .divide(vitesse, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        LocalDateTime heureDepart = sortedReservations.stream()
                .map(PlanningReservation::getDateHeureArrivee)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.of(date, LocalTime.of(8, 0)));

        PlanningVehiculeTour tour = new PlanningVehiculeTour();
        tour.setGroupReference(group.groupReference);
        tour.setVols(group.volsLabel);
        tour.setTotalPassagers(group.totalPassagers);
        tour.setVehicule(vehicule);
        tour.setReservations(sortedReservations);
        tour.setRoute(routeComputation.routeLabel);
        tour.setDistanceTotaleKm(routeComputation.totalKm.setScale(2, RoundingMode.HALF_UP));
        tour.setDureeTotaleMinutes(totalMinutes);
        tour.setHeureDepart(heureDepart);
        tour.setHeureRetour(heureDepart.plusMinutes(totalMinutes));

        applyVisitOrder(sortedReservations, routeComputation.orderedStops);

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
            return new RouteComputation("AER", BigDecimal.ZERO, new ArrayList<>());
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

        List<String> orderedStops = route.stream()
                .filter(stop -> !AEROPORT_CODE.equals(stop))
                .collect(Collectors.toList());

        return new RouteComputation(String.join(" -> ", route), totalKm, orderedStops);
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

    private List<ReservationGroup> buildWaitingGroups(List<PlanningReservation> reservations, int waitingMinutes) {
        Map<String, ReservationGroup> flights = buildFlightUnits(reservations);
        List<ReservationGroup> sortedFlights = new ArrayList<>(flights.values());

        sortedFlights
                .sort(Comparator.comparing(group -> group.lastArrival != null ? group.lastArrival : LocalDateTime.MIN));

        List<ReservationGroup> grouped = new ArrayList<>();
        int index = 1;
        int cursor = 0;

        while (cursor < sortedFlights.size()) {
            ReservationGroup first = sortedFlights.get(cursor);
            ReservationGroup current = new ReservationGroup();
            mergeGroup(current, first);

            LocalDateTime start = first.lastArrival;
            cursor++;

            while (cursor < sortedFlights.size()) {
                ReservationGroup candidate = sortedFlights.get(cursor);
                if (start == null || candidate.lastArrival == null) {
                    break;
                }

                long diff = ChronoUnit.MINUTES.between(start, candidate.lastArrival);
                if (diff <= waitingMinutes) {
                    mergeGroup(current, candidate);
                    cursor++;
                } else {
                    break;
                }
            }

            current.groupReference = "G" + String.format("%03d", index++);
            current.volsLabel = current.volReferences.isEmpty()
                    ? "-"
                    : current.volReferences.stream().sorted().collect(Collectors.joining(", "));

            for (PlanningReservation reservation : current.reservations) {
                reservation.setGroupReference(current.groupReference);
            }

            grouped.add(current);
        }

        return grouped;
    }

    private Map<String, ReservationGroup> buildFlightUnits(List<PlanningReservation> reservations) {
        Map<String, ReservationGroup> groups = new HashMap<>();

        for (PlanningReservation reservation : reservations) {
            String flightKey = reservation.getVolReference() != null && !reservation.getVolReference().isBlank()
                    ? reservation.getVolReference().trim()
                    : "RES-" + reservation.getId();

            ReservationGroup group = groups.computeIfAbsent(flightKey, key -> new ReservationGroup());
            group.reservations.add(reservation);
            group.totalPassagers += Objects.requireNonNullElse(reservation.getNombrePassager(), 0);
            group.volReferences.add(flightKey);

            if (reservation.getDateHeureArrivee() != null
                    && (group.lastArrival == null || reservation.getDateHeureArrivee().isAfter(group.lastArrival))) {
                group.lastArrival = reservation.getDateHeureArrivee();
            }
        }

        return groups;
    }

    private void mergeGroup(ReservationGroup target, ReservationGroup source) {
        target.reservations.addAll(source.reservations);
        target.totalPassagers += source.totalPassagers;
        target.volReferences.addAll(source.volReferences);

        if (source.lastArrival != null
                && (target.lastArrival == null || source.lastArrival.isAfter(target.lastArrival))) {
            target.lastArrival = source.lastArrival;
        }
    }

    private void applyVisitOrder(List<PlanningReservation> reservations, List<String> orderedStops) {
        if (orderedStops == null || orderedStops.isEmpty()) {
            for (PlanningReservation reservation : reservations) {
                reservation.setOrdrePassage(1);
            }
            return;
        }

        Map<String, Integer> orderByStop = new HashMap<>();
        for (int index = 0; index < orderedStops.size(); index++) {
            orderByStop.putIfAbsent(orderedStops.get(index), index + 1);
        }

        for (PlanningReservation reservation : reservations) {
            Integer order = orderByStop.getOrDefault(reservation.getLieuCode(), orderedStops.size());
            reservation.setOrdrePassage(order);
        }

        reservations.sort(Comparator
                .comparing((PlanningReservation reservation) -> Objects
                        .requireNonNullElse(reservation.getOrdrePassage(), Integer.MAX_VALUE))
                .thenComparing(reservation -> reservation.getDateHeureArrivee() != null
                        ? reservation.getDateHeureArrivee()
                        : LocalDateTime.MIN));
    }

    private static class ReservationGroup {

        private String groupReference;
        private String volsLabel;
        private final List<PlanningReservation> reservations = new ArrayList<>();
        private final Set<String> volReferences = new LinkedHashSet<>();
        private int totalPassagers;
        private Integer assignedVehiculeId;
        private LocalDateTime lastArrival;
    }

    private static class RouteComputation {

        private final String routeLabel;
        private final BigDecimal totalKm;
        private final List<String> orderedStops;

        private RouteComputation(String routeLabel,
                BigDecimal totalKm,
                List<String> orderedStops) {
            this.routeLabel = routeLabel;
            this.totalKm = totalKm;
            this.orderedStops = orderedStops;
        }
    }
}