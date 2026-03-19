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
import com.example.model.PlanningVehiculeSuivi;
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

        Map<String, BigDecimal> distances = planningDAO.getDistanceMap();

        List<ComputedGroup> computedGroups = new ArrayList<>();
        for (ReservationGroup group : groups) {
            computedGroups.add(computeGroupData(group, parametre, distances, date));
        }

        computedGroups.sort(Comparator
                .comparing((ComputedGroup group) -> group.plannedDeparture != null ? group.plannedDeparture
                        : LocalDateTime.MIN)
                .thenComparing((ComputedGroup group) -> Objects.requireNonNullElse(group.totalPassagers, 0),
                        Comparator.reverseOrder())
                .thenComparing(group -> group.groupReference != null ? group.groupReference : ""));

        Map<Integer, LocalDateTime> nextAvailableByVehiculeId = new HashMap<>();
        Map<Integer, Integer> tripsCountByVehiculeId = new HashMap<>();

        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        for (Vehicule vehicule : vehicules) {
            if (vehicule.getId() != null) {
                nextAvailableByVehiculeId.put(vehicule.getId(), startOfDay);
                tripsCountByVehiculeId.put(vehicule.getId(), 0);
            }
        }

        List<PlanningVehiculeTour> toursAssignes = new ArrayList<>();
        List<PlanningReservation> nonAssignees = new ArrayList<>();

        for (ComputedGroup group : computedGroups) {
            VehiculeSelection selection = selectVehiculeForGroup(
                    vehicules,
                    group.totalPassagers,
                    group.plannedDeparture,
                    nextAvailableByVehiculeId,
                    tripsCountByVehiculeId);

            if (selection == null || selection.vehicule == null) {
                if (group.reservationsOrdered != null) {
                    nonAssignees.addAll(group.reservationsOrdered);
                }
                continue;
            }

            Vehicule vehiculeChoisi = selection.vehicule;
            LocalDateTime heureDepart = selection.startTime;
            LocalDateTime heureRetour = heureDepart.plusMinutes(Objects.requireNonNullElse(group.dureeTotaleMinutes, 0));

            int currentTrips = tripsCountByVehiculeId.getOrDefault(vehiculeChoisi.getId(), 0);
            tripsCountByVehiculeId.put(vehiculeChoisi.getId(), currentTrips + 1);
            nextAvailableByVehiculeId.put(vehiculeChoisi.getId(), heureRetour);

            if (group.reservationsOrdered != null) {
                for (PlanningReservation reservation : group.reservationsOrdered) {
                    reservation.setIdVehicule(vehiculeChoisi.getId());
                    reservation.setVehiculeReference(vehiculeChoisi.getReference());
                    reservation.setGroupReference(group.groupReference);
                    reservation.setDateHeureDepartReelle(heureDepart);

                    if (reservation.getId() != null) {
                        planningDAO.assignVehicule(reservation.getId(), vehiculeChoisi.getId());
                    }
                }
            }

            PlanningVehiculeTour tour = new PlanningVehiculeTour();
            tour.setGroupReference(group.groupReference);
            tour.setVols(group.vols);
            tour.setTotalPassagers(group.totalPassagers);
            tour.setVehicule(vehiculeChoisi);
            tour.setReservations(group.reservationsOrdered != null ? group.reservationsOrdered : new ArrayList<>());
            tour.setRoute(group.route);
            tour.setDistanceTotaleKm(group.distanceTotaleKm != null
                    ? group.distanceTotaleKm.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            tour.setDureeTotaleMinutes(group.dureeTotaleMinutes);
            tour.setHeureDepart(heureDepart);
            tour.setHeureRetour(heureRetour);

            toursAssignes.add(tour);
        }

        toursAssignes.sort(Comparator
                .comparing((PlanningVehiculeTour tour) -> tour.getHeureDepart() != null ? tour.getHeureDepart()
                        : LocalDateTime.MIN)
                .thenComparing(tour -> tour.getVehicule() != null && tour.getVehicule().getReference() != null
                        ? tour.getVehicule().getReference()
                        : ""));

        List<PlanningVehiculeSuivi> suiviVehicules = buildSuiviVehicules(vehicules, toursAssignes);

        PlanningResult result = new PlanningResult();
        result.setDate(date);
        result.setVehiculesDisponibles(vehicules);
        result.setToursAssignes(toursAssignes);
        result.setReservationsNonAssignees(nonAssignees);
        result.setSuiviVehicules(suiviVehicules);
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

    private VehiculeSelection selectVehiculeForGroup(List<Vehicule> vehicules,
            Integer nombrePassager,
            LocalDateTime plannedDeparture,
            Map<Integer, LocalDateTime> nextAvailableByVehiculeId,
            Map<Integer, Integer> tripsCountByVehiculeId) {
        if (nombrePassager == null || nombrePassager <= 0 || plannedDeparture == null) {
            return null;
        }

        VehiculeSelection best = null;

        for (Vehicule vehicule : vehicules) {
            if (vehicule.getId() == null) {
                continue;
            }

            Integer places = vehicule.getNombrePlaces();
            if (places == null || places < nombrePassager) {
                continue;
            }

            LocalDateTime availableAt = nextAvailableByVehiculeId.getOrDefault(vehicule.getId(), plannedDeparture);
            LocalDateTime startTime = availableAt != null && availableAt.isAfter(plannedDeparture)
                    ? availableAt
                    : plannedDeparture;
            int tripsCount = tripsCountByVehiculeId.getOrDefault(vehicule.getId(), 0);

            VehiculeSelection candidate = new VehiculeSelection(vehicule, startTime, tripsCount);
            if (candidate.isBetterThan(best)) {
                best = candidate;
            }
        }

        return best;
    }

    private boolean isDiesel(Vehicule vehicule) {
        return vehicule != null && "D".equalsIgnoreCase(vehicule.getTypeCarburantCode());
    }

    private ComputedGroup computeGroupData(ReservationGroup group,
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

        LocalDateTime plannedDeparture = sortedReservations.stream()
                .map(PlanningReservation::getDateHeureArrivee)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.of(date, LocalTime.of(8, 0)));

        applyVisitOrder(sortedReservations, routeComputation.orderedStops);

        ComputedGroup computed = new ComputedGroup();
        computed.groupReference = group.groupReference;
        computed.vols = group.volsLabel;
        computed.totalPassagers = group.totalPassagers;
        computed.reservationsOrdered = sortedReservations;
        computed.route = routeComputation.routeLabel;
        computed.distanceTotaleKm = routeComputation.totalKm;
        computed.dureeTotaleMinutes = totalMinutes;
        computed.plannedDeparture = plannedDeparture;
        return computed;
    }

    private List<PlanningVehiculeSuivi> buildSuiviVehicules(List<Vehicule> vehicules,
            List<PlanningVehiculeTour> toursAssignes) {
        Map<Integer, PlanningVehiculeSuivi> suiviByVehiculeId = new HashMap<>();

        for (Vehicule vehicule : vehicules) {
            if (vehicule.getId() == null) {
                continue;
            }
            PlanningVehiculeSuivi suivi = new PlanningVehiculeSuivi();
            suivi.setVehicule(vehicule);
            suiviByVehiculeId.put(vehicule.getId(), suivi);
        }

        for (PlanningVehiculeTour tour : toursAssignes) {
            if (tour.getVehicule() == null || tour.getVehicule().getId() == null) {
                continue;
            }
            PlanningVehiculeSuivi suivi = suiviByVehiculeId.get(tour.getVehicule().getId());
            if (suivi == null) {
                continue;
            }
            suivi.getHistoriqueTrajets().add(tour);
        }

        List<PlanningVehiculeSuivi> suivis = new ArrayList<>(suiviByVehiculeId.values());
        for (PlanningVehiculeSuivi suivi : suivis) {
            int count = suivi.getHistoriqueTrajets() != null ? suivi.getHistoriqueTrajets().size() : 0;
            suivi.setNombreTrajets(count);
            if (suivi.getHistoriqueTrajets() != null) {
                suivi.getHistoriqueTrajets().sort(Comparator
                        .comparing((PlanningVehiculeTour t) -> t.getHeureDepart() != null ? t.getHeureDepart()
                                : LocalDateTime.MIN));
            }
        }

        suivis.sort(Comparator
                .comparing((PlanningVehiculeSuivi suivi) -> Objects.requireNonNullElse(suivi.getNombreTrajets(), 0))
                .thenComparing(suivi -> suivi.getVehicule() != null && suivi.getVehicule().getReference() != null
                        ? suivi.getVehicule().getReference()
                        : ""));

        return suivis;
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
        if (from == null || to == null) {
            return BigDecimal.ZERO;
        }

        if (from.equals(to)) {
            return BigDecimal.ZERO;
        }

        BigDecimal direct = distances.get(distanceKey(from, to));
        if (direct != null) {
            return direct;
        }

        BigDecimal reverse = distances.get(distanceKey(to, from));
        if (reverse != null) {
            return reverse;
        }

        // Fallback: if the matrix is incomplete (missing hotel-to-hotel distances),
        // approximate via the airport so multi-stop routes still get non-zero km.
        // This requires that (from <-> AER) and (AER <-> to) exist.
        if (!AEROPORT_CODE.equals(from) && !AEROPORT_CODE.equals(to)) {
            BigDecimal fromToAirport = distances.get(distanceKey(from, AEROPORT_CODE));
            if (fromToAirport == null) {
                fromToAirport = distances.get(distanceKey(AEROPORT_CODE, from));
            }

            BigDecimal airportToTo = distances.get(distanceKey(AEROPORT_CODE, to));
            if (airportToTo == null) {
                airportToTo = distances.get(distanceKey(to, AEROPORT_CODE));
            }

            if (fromToAirport != null && airportToTo != null) {
                return fromToAirport.add(airportToTo);
            }
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
        private LocalDateTime lastArrival;
    }

    private class VehiculeSelection {

        private final Vehicule vehicule;
        private final LocalDateTime startTime;
        private final int tripsCount;

        private VehiculeSelection(Vehicule vehicule, LocalDateTime startTime, int tripsCount) {
            this.vehicule = vehicule;
            this.startTime = startTime;
            this.tripsCount = tripsCount;
        }

        private boolean isBetterThan(VehiculeSelection other) {
            if (other == null) {
                return true;
            }

            if (startTime != null && other.startTime != null) {
                int cmpStart = startTime.compareTo(other.startTime);
                if (cmpStart != 0) {
                    return cmpStart < 0;
                }
            }

            if (tripsCount != other.tripsCount) {
                return tripsCount < other.tripsCount;
            }

            boolean diesel = isDiesel(vehicule);
            boolean otherDiesel = isDiesel(other.vehicule);
            if (diesel != otherDiesel) {
                return diesel;
            }

            Integer places = vehicule != null ? vehicule.getNombrePlaces() : null;
            Integer otherPlaces = other.vehicule != null ? other.vehicule.getNombrePlaces() : null;
            int cmpPlaces = Integer.compare(
                    Objects.requireNonNullElse(places, Integer.MAX_VALUE),
                    Objects.requireNonNullElse(otherPlaces, Integer.MAX_VALUE));
            if (cmpPlaces != 0) {
                return cmpPlaces < 0;
            }

            return Integer.compare(
                    Objects.requireNonNullElse(vehicule.getId(), Integer.MAX_VALUE),
                    Objects.requireNonNullElse(other.vehicule.getId(), Integer.MAX_VALUE)) < 0;
        }
    }

    private static class ComputedGroup {
        private String groupReference;
        private String vols;
        private Integer totalPassagers;
        private List<PlanningReservation> reservationsOrdered;
        private String route;
        private BigDecimal distanceTotaleKm;
        private Integer dureeTotaleMinutes;
        private LocalDateTime plannedDeparture;
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