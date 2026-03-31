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

        initializeReservationState(reservations);
        planningDAO.clearAssignmentsByDate(date);

        PlanningParametre parametre = planningDAO.getPlanningParametre();
        int maxWaitMinutes = parametre.getTempsAttente() != null
                ? Math.max(0, parametre.getTempsAttente())
                : 10;

        Map<String, BigDecimal> distances = planningDAO.getDistanceMap();
        BigDecimal vitesseMoyenne = parametre.getVitesseMoyenne() != null
                ? parametre.getVitesseMoyenne()
                : BigDecimal.valueOf(50);

        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        Map<Integer, VehiculeRuntimeState> vehiculeStates = new HashMap<>();

        for (Vehicule vehicule : vehicules) {
            if (vehicule.getId() == null) {
                continue;
            }
            vehiculeStates.put(vehicule.getId(), new VehiculeRuntimeState(vehicule, startOfDay));
        }

        List<PlanningVehiculeTour> toursAssignes = new ArrayList<>();

        while (hasPendingReservations(reservations)) {
            VehiculeRuntimeState nextVehicule = selectNextVehicule(vehiculeStates);
            if (nextVehicule == null) {
                break;
            }

            LocalDateTime referenceTime = nextVehicule.availableAt;
            List<ReservationGroup> candidateGroups = buildCandidateGroups(
                    reservations,
                    referenceTime,
                    maxWaitMinutes);

            if (candidateGroups.isEmpty()) {
                LocalDateTime nextArrival = findNextArrival(reservations, referenceTime);
                if (nextArrival == null) {
                    break;
                }

                // Vehicle stays idle until the next arrival if no candidate can be served now.
                nextVehicule.availableAt = nextArrival;
                continue;
            }

            candidateGroups.sort(groupComparator(referenceTime));
            ReservationGroup primaryGroup = candidateGroups.get(0);

            long waitBeforeDeparture = Math.max(0,
                    ChronoUnit.MINUTES.between(referenceTime, primaryGroup.referenceArrival));
            LocalDateTime departureTime = referenceTime.plusMinutes(waitBeforeDeparture);

            int capacity = Math.max(0, Objects.requireNonNullElse(nextVehicule.vehicule.getNombrePlaces(), 0));
            if (capacity <= 0) {
                nextVehicule.availableAt = departureTime;
                continue;
            }

            List<ReservationGroup> groupsForFill = groupsArrivedAt(candidateGroups, departureTime);
            int assignableNow = countAssignablePassengers(groupsForFill);

            if (assignableNow < capacity) {
                LocalDateTime nextFlightArrival = findNearestFutureArrival(candidateGroups, departureTime);
                if (nextFlightArrival != null) {
                    long extraWait = ChronoUnit.MINUTES.between(departureTime, nextFlightArrival);
                    if (extraWait > 0 && extraWait <= maxWaitMinutes) {
                        departureTime = nextFlightArrival;
                        waitBeforeDeparture += extraWait;
                        groupsForFill = groupsArrivedAt(candidateGroups, departureTime);
                    }
                }
            }

            List<GroupAllocation> allocations = allocatePassengers(groupsForFill, capacity);
            int assignedPassengers = allocations.stream().mapToInt(allocation -> allocation.assignedPassengers).sum();

            if (assignedPassengers <= 0) {
                LocalDateTime nextArrival = findNextArrival(reservations, referenceTime);
                if (nextArrival == null) {
                    break;
                }
                nextVehicule.availableAt = nextArrival;
                continue;
            }

            applyAllocations(allocations, nextVehicule.vehicule);

            RouteComputation route = computeNearestNeighborRouteFromAllocations(allocations, distances);
            int durationMinutes = computeDurationMinutes(route.totalKm, vitesseMoyenne);
            LocalDateTime returnTime = departureTime.plusMinutes(durationMinutes);

            for (GroupAllocation allocation : allocations) {
                allocation.reservation.setDateHeureDepartReelle(departureTime);
            }

            PlanningVehiculeTour tour = new PlanningVehiculeTour();
            tour.setGroupReference(buildGroupReferenceLabel(allocations));
            tour.setVols(buildVolsLabel(allocations));
            tour.setTotalPassagers(assignedPassengers);
            tour.setVehicule(nextVehicule.vehicule);
            tour.setReservations(buildTripReservationSnapshots(allocations));
            tour.setRoute(route.routeLabel);
            tour.setDistanceTotaleKm(route.totalKm.setScale(2, RoundingMode.HALF_UP));
            tour.setDureeTotaleMinutes(durationMinutes);
            tour.setHeureDepart(departureTime);
            tour.setHeureRetour(returnTime);
            tour.setDecisionDepart(waitBeforeDeparture > 0 ? "ATTENTE" : "IMMEDIAT");
            tour.setTempsAttenteUtiliseMinutes((int) waitBeforeDeparture);
            tour.setTauxRemplissage(computeFillRate(assignedPassengers, capacity));

            toursAssignes.add(tour);

            nextVehicule.tripsCount++;
            nextVehicule.availableAt = returnTime;
        }

        toursAssignes.sort(Comparator
                .comparing((PlanningVehiculeTour tour) -> tour.getHeureDepart() != null
                        ? tour.getHeureDepart()
                        : LocalDateTime.MIN)
                .thenComparing(tour -> tour.getVehicule() != null && tour.getVehicule().getReference() != null
                        ? tour.getVehicule().getReference()
                        : ""));

        updateReservationStatuses(reservations);

        List<PlanningReservation> reservationsTotalementAssignees = reservations.stream()
                .filter(reservation -> "TOTAL".equals(reservation.getStatutAssignation()))
                .collect(Collectors.toList());

        List<PlanningReservation> reservationsPartiellementAssignees = reservations.stream()
                .filter(reservation -> "PARTIEL".equals(reservation.getStatutAssignation()))
                .collect(Collectors.toList());

        List<PlanningReservation> reservationsNonAssignees = reservations.stream()
                .filter(reservation -> "NON_ASSIGNE".equals(reservation.getStatutAssignation()))
                .collect(Collectors.toList());

        List<PlanningVehiculeSuivi> suiviVehicules = buildSuiviVehicules(vehicules, toursAssignes);

        PlanningResult result = new PlanningResult();
        result.setDate(date);
        result.setVehiculesDisponibles(vehicules);
        result.setToursAssignes(toursAssignes);
        result.setReservationsTotalementAssignees(reservationsTotalementAssignees);
        result.setReservationsPartiellementAssignees(reservationsPartiellementAssignees);
        result.setReservationsNonAssignees(reservationsNonAssignees);
        result.setSuiviVehicules(suiviVehicules);
        result.setTotalReservations(reservations.size());
        return result;
    }

    private void initializeReservationState(List<PlanningReservation> reservations) {
        for (PlanningReservation reservation : reservations) {
            int total = Math.max(0, Objects.requireNonNullElse(reservation.getNombrePassager(), 0));
            reservation.setPassagersInitiaux(total);
            reservation.setPassagersAssignes(0);
            reservation.setPassagersRestants(total);
            reservation.setStatutAssignation("NON_ASSIGNE");
            reservation.setPassagersAssignesSurTour(0);
            reservation.setIdVehicule(null);
            reservation.setVehiculeReference(null);
        }
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

    private boolean hasPendingReservations(List<PlanningReservation> reservations) {
        for (PlanningReservation reservation : reservations) {
            Integer restants = reservation.getPassagersRestants();
            if (restants != null && restants > 0) {
                return true;
            }
        }
        return false;
    }

    private VehiculeRuntimeState selectNextVehicule(Map<Integer, VehiculeRuntimeState> vehiculeStates) {
        return vehiculeStates.values().stream()
                .min(Comparator
                        .comparing((VehiculeRuntimeState state) -> state.availableAt)
                        .thenComparingInt(state -> state.tripsCount)
                        .thenComparing(state -> Objects.requireNonNullElse(state.vehicule.getId(), Integer.MAX_VALUE)))
                .orElse(null);
    }

    private List<ReservationGroup> buildCandidateGroups(List<PlanningReservation> reservations,
            LocalDateTime referenceTime,
            int maxWaitMinutes) {
        Map<String, ReservationGroup> groupsByKey = new HashMap<>();
        LocalDateTime maxArrival = referenceTime.plusMinutes(maxWaitMinutes);

        for (PlanningReservation reservation : reservations) {
            if (Objects.requireNonNullElse(reservation.getPassagersRestants(), 0) <= 0) {
                continue;
            }

            LocalDateTime arrival = reservation.getDateHeureArrivee();
            if (arrival == null || arrival.isAfter(maxArrival)) {
                continue;
            }

            String key = reservation.getVolReference() != null && !reservation.getVolReference().isBlank()
                    ? reservation.getVolReference().trim()
                    : "RES-" + reservation.getId();

            if (reservation.getGroupReference() == null || reservation.getGroupReference().isBlank()) {
                reservation.setGroupReference("GRP-" + key);
            }

            ReservationGroup group = groupsByKey.computeIfAbsent(key, value -> new ReservationGroup());
            group.volReference = key;
            group.reservations.add(reservation);
            group.totalRemainingPassengers += Objects.requireNonNullElse(reservation.getPassagersRestants(), 0);

            if (group.referenceArrival == null || arrival.isBefore(group.referenceArrival)) {
                group.referenceArrival = arrival;
            }

            int seq = Objects.requireNonNullElse(reservation.getId(), Integer.MAX_VALUE);
            group.sequence = Math.min(group.sequence, seq);
        }

        for (ReservationGroup group : groupsByKey.values()) {
            group.isNewGroup = group.referenceArrival != null && group.referenceArrival.isAfter(referenceTime);
            group.reservations.sort(Comparator
                    .comparing((PlanningReservation reservation) -> reservation.getDateHeureArrivee() != null
                            ? reservation.getDateHeureArrivee()
                            : LocalDateTime.MIN)
                    .thenComparing(reservation -> Objects.requireNonNullElse(reservation.getId(), Integer.MAX_VALUE)));
        }

        return new ArrayList<>(groupsByKey.values());
    }

    private Comparator<ReservationGroup> groupComparator(LocalDateTime referenceTime) {
        return Comparator
                .comparing((ReservationGroup group) -> !group.isNewGroup)
                .thenComparing(group -> group.referenceArrival != null ? group.referenceArrival : LocalDateTime.MAX)
                .thenComparingInt(group -> group.sequence)
                .thenComparing(group -> group.volReference != null ? group.volReference : "");
    }

    private List<ReservationGroup> groupsArrivedAt(List<ReservationGroup> groups, LocalDateTime moment) {
        return groups.stream()
                .filter(group -> group.referenceArrival != null && !group.referenceArrival.isAfter(moment))
                .collect(Collectors.toList());
    }

    private int countAssignablePassengers(List<ReservationGroup> groups) {
        int total = 0;
        for (ReservationGroup group : groups) {
            total += Math.max(0, group.totalRemainingPassengers);
        }
        return total;
    }

    private LocalDateTime findNearestFutureArrival(List<ReservationGroup> groups, LocalDateTime moment) {
        return groups.stream()
                .map(group -> group.referenceArrival)
                .filter(Objects::nonNull)
                .filter(arrival -> arrival.isAfter(moment))
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime findNextArrival(List<PlanningReservation> reservations, LocalDateTime afterTime) {
        return reservations.stream()
                .filter(reservation -> Objects.requireNonNullElse(reservation.getPassagersRestants(), 0) > 0)
                .map(PlanningReservation::getDateHeureArrivee)
                .filter(Objects::nonNull)
                .filter(arrival -> arrival.isAfter(afterTime))
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private List<GroupAllocation> allocatePassengers(List<ReservationGroup> groups, int capacity) {
        List<GroupAllocation> allocations = new ArrayList<>();
        int capacityLeft = capacity;

        for (ReservationGroup group : groups) {
            if (capacityLeft <= 0) {
                break;
            }

            for (PlanningReservation reservation : group.reservations) {
                if (capacityLeft <= 0) {
                    break;
                }

                int remaining = Math.max(0, Objects.requireNonNullElse(reservation.getPassagersRestants(), 0));
                if (remaining <= 0) {
                    continue;
                }

                int assigned = Math.min(remaining, capacityLeft);
                if (assigned <= 0) {
                    continue;
                }

                GroupAllocation allocation = new GroupAllocation();
                allocation.groupReference = group.volReference;
                allocation.reservation = reservation;
                allocation.assignedPassengers = assigned;
                allocations.add(allocation);

                capacityLeft -= assigned;
            }
        }

        return allocations;
    }

    private void applyAllocations(List<GroupAllocation> allocations, Vehicule vehicule) throws SQLException {
        for (GroupAllocation allocation : allocations) {
            PlanningReservation reservation = allocation.reservation;

            int alreadyAssigned = Math.max(0, Objects.requireNonNullElse(reservation.getPassagersAssignes(), 0));
            int currentRemaining = Math.max(0, Objects.requireNonNullElse(reservation.getPassagersRestants(), 0));

            reservation.setPassagersAssignes(alreadyAssigned + allocation.assignedPassengers);
            reservation.setPassagersRestants(Math.max(0, currentRemaining - allocation.assignedPassengers));
            reservation.setPassagersAssignesSurTour(allocation.assignedPassengers);

            if (reservation.getIdVehicule() == null && vehicule != null && vehicule.getId() != null) {
                reservation.setIdVehicule(vehicule.getId());
                reservation.setVehiculeReference(vehicule.getReference());

                if (reservation.getId() != null) {
                    planningDAO.assignVehicule(reservation.getId(), vehicule.getId());
                }
            }
        }
    }

    private RouteComputation computeNearestNeighborRouteFromAllocations(List<GroupAllocation> allocations,
            Map<String, BigDecimal> distances) {
        Set<String> stops = new LinkedHashSet<>();

        for (GroupAllocation allocation : allocations) {
            PlanningReservation reservation = allocation.reservation;
            if (reservation.getLieuCode() != null && !reservation.getLieuCode().isBlank()) {
                stops.add(reservation.getLieuCode().trim());
            }
        }

        if (stops.isEmpty()) {
            return new RouteComputation("AER", BigDecimal.ZERO);
        }

        String current = AEROPORT_CODE;
        List<String> route = new ArrayList<>();
        route.add(AEROPORT_CODE);

        BigDecimal totalKm = BigDecimal.ZERO;
        Set<String> remaining = new LinkedHashSet<>(stops);

        while (!remaining.isEmpty()) {
            String next = findNearest(current, remaining, distances);
            totalKm = totalKm.add(getDistance(current, next, distances));
            route.add(next);
            remaining.remove(next);
            current = next;
        }

        totalKm = totalKm.add(getDistance(current, AEROPORT_CODE, distances));
        route.add(AEROPORT_CODE);

        return new RouteComputation(String.join(" -> ", route), totalKm);
    }

    private String findNearest(String from,
            Set<String> destinations,
            Map<String, BigDecimal> distances) {
        String nearest = null;
        BigDecimal bestKm = null;

        for (String destination : destinations) {
            BigDecimal km = getDistance(from, destination, distances);

            if (bestKm == null || km.compareTo(bestKm) < 0
                    || (km.compareTo(bestKm) == 0 && destination.compareTo(Objects.requireNonNullElse(nearest, "")) < 0)) {
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
        if (from == null || to == null || from.equals(to)) {
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

    private int computeDurationMinutes(BigDecimal distanceKm, BigDecimal vitesseMoyenne) {
        BigDecimal speed = vitesseMoyenne != null && vitesseMoyenne.compareTo(BigDecimal.ZERO) > 0
                ? vitesseMoyenne
                : BigDecimal.valueOf(50);

        return distanceKm
                .divide(speed, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal computeFillRate(int assignedPassengers, int capacity) {
        if (capacity <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(assignedPassengers)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
    }

    private String buildGroupReferenceLabel(List<GroupAllocation> allocations) {
        Set<String> refs = new LinkedHashSet<>();
        for (GroupAllocation allocation : allocations) {
            if (allocation.reservation != null && allocation.reservation.getGroupReference() != null
                    && !allocation.reservation.getGroupReference().isBlank()) {
                refs.add(allocation.reservation.getGroupReference());
            }
        }
        if (refs.isEmpty()) {
            return "-";
        }
        return String.join(", ", refs);
    }

    private String buildVolsLabel(List<GroupAllocation> allocations) {
        Set<String> vols = new LinkedHashSet<>();
        for (GroupAllocation allocation : allocations) {
            PlanningReservation reservation = allocation.reservation;
            if (reservation != null && reservation.getVolReference() != null && !reservation.getVolReference().isBlank()) {
                vols.add(reservation.getVolReference());
            }
        }

        if (vols.isEmpty()) {
            return "-";
        }

        return vols.stream().sorted().collect(Collectors.joining(", "));
    }

    private List<PlanningReservation> buildTripReservationSnapshots(List<GroupAllocation> allocations) {
        List<PlanningReservation> snapshots = new ArrayList<>();

        for (GroupAllocation allocation : allocations) {
            PlanningReservation reservation = allocation.reservation;

            PlanningReservation snapshot = new PlanningReservation();
            snapshot.setId(reservation.getId());
            snapshot.setClientId(reservation.getClientId());
            snapshot.setNombrePassager(reservation.getNombrePassager());
            snapshot.setDateHeureArrivee(reservation.getDateHeureArrivee());
            snapshot.setIdHotel(reservation.getIdHotel());
            snapshot.setHotelNom(reservation.getHotelNom());
            snapshot.setLieuCode(reservation.getLieuCode());
            snapshot.setLieuLibelle(reservation.getLieuLibelle());
            snapshot.setVolReference(reservation.getVolReference());
            snapshot.setGroupReference(reservation.getGroupReference());
            snapshot.setOrdrePassage(reservation.getOrdrePassage());
            snapshot.setDateHeureDepartReelle(reservation.getDateHeureDepartReelle());
            snapshot.setIdVehicule(reservation.getIdVehicule());
            snapshot.setVehiculeReference(reservation.getVehiculeReference());
            snapshot.setPassagersInitiaux(reservation.getPassagersInitiaux());
            snapshot.setPassagersAssignes(reservation.getPassagersAssignes());
            snapshot.setPassagersRestants(reservation.getPassagersRestants());
            snapshot.setStatutAssignation(reservation.getStatutAssignation());
            snapshot.setPassagersAssignesSurTour(allocation.assignedPassengers);
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private void updateReservationStatuses(List<PlanningReservation> reservations) {
        int groupIndex = 1;

        for (PlanningReservation reservation : reservations) {
            int assigned = Math.max(0, Objects.requireNonNullElse(reservation.getPassagersAssignes(), 0));
            int remaining = Math.max(0, Objects.requireNonNullElse(reservation.getPassagersRestants(), 0));

            if (assigned > 0 && remaining == 0) {
                reservation.setStatutAssignation("TOTAL");
            } else if (assigned > 0) {
                reservation.setStatutAssignation("PARTIEL");
            } else {
                reservation.setStatutAssignation("NON_ASSIGNE");
            }

            if (reservation.getGroupReference() == null || reservation.getGroupReference().isBlank()) {
                reservation.setGroupReference("G" + String.format("%03d", groupIndex++));
            }
        }
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
            List<PlanningVehiculeTour> history = suivi.getHistoriqueTrajets();
            int trips = history != null ? history.size() : 0;
            suivi.setNombreTrajets(trips);

            int totalWait = 0;
            BigDecimal totalFill = BigDecimal.ZERO;

            if (history != null) {
                history.sort(Comparator.comparing(
                        tour -> tour.getHeureDepart() != null ? tour.getHeureDepart() : LocalDateTime.MIN));

                for (PlanningVehiculeTour tour : history) {
                    totalWait += Math.max(0, Objects.requireNonNullElse(tour.getTempsAttenteUtiliseMinutes(), 0));
                    totalFill = totalFill.add(Objects.requireNonNullElse(tour.getTauxRemplissage(), BigDecimal.ZERO));
                }
            }

            suivi.setTempsAttenteUtiliseMinutes(totalWait);

            if (trips > 0) {
                suivi.setTauxRemplissageMoyen(totalFill.divide(BigDecimal.valueOf(trips), 2, RoundingMode.HALF_UP));
            } else {
                suivi.setTauxRemplissageMoyen(BigDecimal.ZERO);
            }
        }

        suivis.sort(Comparator
                .comparing((PlanningVehiculeSuivi suivi) -> Objects.requireNonNullElse(suivi.getNombreTrajets(), 0))
                .thenComparing(suivi -> suivi.getVehicule() != null && suivi.getVehicule().getReference() != null
                        ? suivi.getVehicule().getReference()
                        : ""));

        return suivis;
    }

    private static class VehiculeRuntimeState {
        private final Vehicule vehicule;
        private LocalDateTime availableAt;
        private int tripsCount;

        private VehiculeRuntimeState(Vehicule vehicule, LocalDateTime availableAt) {
            this.vehicule = vehicule;
            this.availableAt = availableAt;
            this.tripsCount = 0;
        }
    }

    private static class ReservationGroup {
        private String volReference;
        private final List<PlanningReservation> reservations = new ArrayList<>();
        private int totalRemainingPassengers;
        private LocalDateTime referenceArrival;
        private boolean isNewGroup;
        private int sequence = Integer.MAX_VALUE;
    }

    private static class GroupAllocation {
        private String groupReference;
        private PlanningReservation reservation;
        private int assignedPassengers;
    }

    private static class RouteComputation {
        private final String routeLabel;
        private final BigDecimal totalKm;

        private RouteComputation(String routeLabel, BigDecimal totalKm) {
            this.routeLabel = routeLabel;
            this.totalKm = totalKm;
        }
    }
}
