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
    private static final String PARTIAL_NON_ASSIGNED_REASON = "Capacité insuffisante après fractionnement";
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);

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

        PlanningParametre parametre = planningDAO.getPlanningParametre();
        int tempsAttenteMinutes = parametre.getTempsAttente() != null
                ? Math.max(0, parametre.getTempsAttente())
                : 30;

        List<ReservationGroup> groups = buildWaitingGroups(reservations, tempsAttenteMinutes, date);
        groups.sort(Comparator
                .comparing((ReservationGroup group) -> group.requestedDeparture)
                .thenComparing((ReservationGroup group) -> -group.totalPassagers));

        Map<String, BigDecimal> distances = planningDAO.getDistanceMap();

        Map<Integer, VehicleState> vehicleStates = new HashMap<>();
        LocalDateTime dayEnd = LocalDateTime.of(date, END_OF_DAY_TIME);
        for (Vehicule vehicule : vehicules) {
            VehicleState state = new VehicleState();
            state.vehicule = vehicule;
            state.tripsCount = 0;
            LocalTime heureDisponibilite = vehicule.getHeureDisponibilite() != null
                    ? vehicule.getHeureDisponibilite()
                    : LocalTime.MIDNIGHT;
            state.dayEnd = dayEnd;
            state.nextAvailableAt = LocalDateTime.of(date, heureDisponibilite);
            vehicleStates.put(vehicule.getId(), state);
        }

        Map<Integer, ReservationAssignmentTracker> assignmentTrackers = new HashMap<>();
        for (PlanningReservation reservation : reservations) {
            Integer totalPassengers = Objects.requireNonNullElse(reservation.getNombrePassager(), 0);
            ReservationAssignmentTracker tracker = new ReservationAssignmentTracker();
            tracker.originalPassengers = totalPassengers;
            tracker.assignedPassengers = 0;
            tracker.bestAssignedPassengers = 0;
            assignmentTrackers.put(reservation.getId(), tracker);
        }

        List<PlanningVehiculeTour> toursAssignes = new ArrayList<>();
        List<PlanningReservation> nonAssignees = new ArrayList<>();
        List<PlanningReservation> carryOver = new ArrayList<>();

        for (int index = 0; index < groups.size(); index++) {
            ReservationGroup group = groups.get(index);
            GroupAssignmentOutcome outcome = assignGroupWithSplit(group, carryOver, vehicleStates, parametre,
                    distances, date, assignmentTrackers);
            toursAssignes.addAll(outcome.tours);
            carryOver = outcome.nonAssigned;

            if (index == groups.size() - 1) {
                nonAssignees.addAll(carryOver);
            }
        }

        for (PlanningReservation reservation : reservations) {
            ReservationAssignmentTracker tracker = assignmentTrackers.get(reservation.getId());
            if (tracker == null) {
                continue;
            }

            reservation.setNombrePassagerOriginal(tracker.originalPassengers);
            reservation.setNombrePassagerAssigne(tracker.assignedPassengers);

            if (tracker.principalVehicleId != null) {
                reservation.setIdVehicule(tracker.principalVehicleId);
                reservation.setVehiculeReference(tracker.principalVehicleReference);
                planningDAO.assignVehicule(reservation.getId(), tracker.principalVehicleId);
            }
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

    private GroupAssignmentOutcome assignGroupWithSplit(ReservationGroup group,
            List<PlanningReservation> carryIn,
            Map<Integer, VehicleState> vehicleStates,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers) {

        GroupAssignmentOutcome outcome = new GroupAssignmentOutcome();

        int dispatchWindowMinutes = parametre.getTempsAttente() != null
                ? Math.max(0, parametre.getTempsAttente())
                : 30;

        LocalDateTime groupDepartTime = group.requestedDeparture;
        
        // Étape 1: Traiter les réservations non assignées du cycle précédent en priorité
        List<PlanningReservation> priorityReservations = new ArrayList<>();
        List<PlanningReservation> newReservations = new ArrayList<>();
        
        if (carryIn != null && !carryIn.isEmpty()) {
            priorityReservations.addAll(carryIn);
        }
        newReservations.addAll(group.reservations);
        
        // Essayer de retourner les véhicules avec les réservations prioritaires
        processDynamicAllocationForReturningVehicles(priorityReservations, vehicleStates, 
                groupDepartTime, dispatchWindowMinutes, outcome, assignmentTrackers, 
                parametre, distances, date, group);
        
        // Construire la charge de travail pour ce groupe
        List<PlanningReservation> workload = new ArrayList<>();
        // Ajouter les réservations non assignées restantes après le traitement dynamique
        workload.addAll(priorityReservations.stream()
                .filter(r -> r.getNombrePassagerAssigne() == null || r.getNombrePassagerAssigne() == 0)
                .collect(Collectors.toList()));
        workload.addAll(newReservations);

        List<ReservationUnit> pendingUnits = workload.stream()
                .map(ReservationUnit::new)
                .filter(unit -> unit.remainingPassengers > 0)
                .sorted(Comparator
                        .comparing((ReservationUnit unit) -> unit.remainingPassengers, Comparator.reverseOrder())
                        .thenComparing(unit -> unit.baseReservation.getDateHeureArrivee() != null
                                ? unit.baseReservation.getDateHeureArrivee()
                                : LocalDateTime.MIN))
                .collect(Collectors.toList());

        List<GroupTripSlot> tripSlots = new ArrayList<>();
        Set<Integer> vehiclesUsedForGroup = new LinkedHashSet<>();

        for (ReservationUnit unit : pendingUnits) {
            int remaining = unit.remainingPassengers;
            if (remaining <= 0) {
                continue;
            }

            GroupTripSlot directTrip = findTripWithCapacityAtLeast(tripSlots, remaining);
            if (directTrip != null) {
                assignPassengersToTrip(unit, directTrip, remaining, assignmentTrackers);
                continue;
            }

            VehicleState directVehicle = findUnusedVehicleWithCapacityAtLeast(vehicleStates, vehiclesUsedForGroup,
                    remaining, group.requestedDeparture, dispatchWindowMinutes);
            if (directVehicle != null) {
                GroupTripSlot newSlot = openTripSlot(directVehicle, group.requestedDeparture);
                if (newSlot != null) {
                    vehiclesUsedForGroup.add(directVehicle.vehicule.getId());
                    tripSlots.add(newSlot);
                    assignPassengersToTrip(unit, newSlot, remaining, assignmentTrackers);
                    continue;
                }
            }

            remaining = fillExistingTrips(unit, tripSlots, remaining, assignmentTrackers);

            if (remaining > 0) {
                List<VehicleState> availableVehicles = findUnusedVehiclesSorted(vehicleStates, vehiclesUsedForGroup,
                        group.requestedDeparture, dispatchWindowMinutes);

                for (VehicleState state : availableVehicles) {
                    if (remaining <= 0) {
                        break;
                    }

                    GroupTripSlot newSlot = openTripSlot(state, group.requestedDeparture);
                    if (newSlot == null) {
                        continue;
                    }
                    vehiclesUsedForGroup.add(state.vehicule.getId());
                    tripSlots.add(newSlot);

                    int assignedNow = Math.min(remaining, newSlot.capacityRemaining);
                    assignPassengersToTrip(unit, newSlot, assignedNow, assignmentTrackers);
                    remaining -= assignedNow;
                }
            }

            if (remaining > 0) {
                PlanningReservation notAssigned = buildReservationFragment(unit.baseReservation,
                        remaining,
                        unit.nextPartIndex(),
                        group.groupReference);
                notAssigned.setNombrePassagerAssigne(0);
                notAssigned.setRaisonNonAssignation(PARTIAL_NON_ASSIGNED_REASON);
                outcome.nonAssigned.add(notAssigned);
            }
        }

        for (GroupTripSlot slot : tripSlots) {
            if (slot.assignedPassengers <= 0 || slot.fragments.isEmpty()) {
                continue;
            }

            PlanningVehiculeTour tour = calculerTour(slot, group, parametre, distances, date);
            if (tour.getHeureRetour() != null && tour.getHeureRetour().isAfter(slot.vehicleState.dayEnd)) {
                slot.vehicleState.nextAvailableAt = slot.vehicleState.dayEnd.plusMinutes(1);
            } else {
                slot.vehicleState.nextAvailableAt = tour.getHeureRetour();
            }

            for (PlanningReservation fragment : tour.getReservations()) {
                fragment.setHeureDepartReelle(tour.getHeureDepart());
                fragment.setHeureArriveeAeroport(tour.getHeureRetour());
                fragment.setGroupReference(group.groupReference);
            }

            outcome.tours.add(tour);
        }

        outcome.tours.sort(Comparator
                .comparing((PlanningVehiculeTour tour) -> tour.getHeureDepart() != null
                        ? tour.getHeureDepart()
                        : LocalDateTime.MIN)
                .thenComparing(tour -> tour.getVehicule() != null ? tour.getVehicule().getReference() : ""));

        return outcome;
    }

    private int fillExistingTrips(ReservationUnit unit,
            List<GroupTripSlot> tripSlots,
            int remaining,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers) {
        List<GroupTripSlot> ordered = new ArrayList<>(tripSlots);
        ordered.sort(Comparator
                .comparing((GroupTripSlot slot) -> slot.capacityRemaining, Comparator.reverseOrder())
                .thenComparing(slot -> slot.tripNumber));

        for (GroupTripSlot slot : ordered) {
            if (remaining <= 0) {
                break;
            }

            if (slot.capacityRemaining <= 0) {
                continue;
            }

            int assignedNow = Math.min(remaining, slot.capacityRemaining);
            assignPassengersToTrip(unit, slot, assignedNow, assignmentTrackers);
            remaining -= assignedNow;
        }

        return remaining;
    }

    private GroupTripSlot findTripWithCapacityAtLeast(List<GroupTripSlot> tripSlots, int demand) {
        return tripSlots.stream()
                .filter(slot -> slot.capacityRemaining >= demand)
                .sorted(Comparator
                        .comparing((GroupTripSlot slot) -> slot.capacityRemaining)
                        .thenComparing(slot -> slot.tripNumber))
                .findFirst()
                .orElse(null);
    }

    private VehicleState findUnusedVehicleWithCapacityAtLeast(Map<Integer, VehicleState> vehicleStates,
            Set<Integer> vehiclesUsedForGroup,
            int demand,
            LocalDateTime requestedDeparture,
            int dispatchWindowMinutes) {
        return findUnusedVehiclesSorted(vehicleStates, vehiclesUsedForGroup, requestedDeparture, dispatchWindowMinutes)
                .stream()
                .filter(state -> Objects.requireNonNullElse(state.vehicule.getNombrePlaces(), 0) >= demand)
                .sorted(Comparator
                        .comparing((VehicleState state) -> Objects
                                .requireNonNullElse(state.vehicule.getNombrePlaces(), Integer.MAX_VALUE))
                        .thenComparing(state -> computeRealDeparture(state, requestedDeparture))
                        .thenComparing(state -> state.tripsCount)
                        .thenComparing(state -> "D".equalsIgnoreCase(state.vehicule.getTypeCarburantCode()) ? 0 : 1)
                        .thenComparing(state -> Objects.requireNonNullElse(state.vehicule.getId(), Integer.MAX_VALUE)))
                .findFirst()
                .orElse(null);
    }

    private List<VehicleState> findUnusedVehiclesSorted(Map<Integer, VehicleState> vehicleStates,
            Set<Integer> vehiclesUsedForGroup,
            LocalDateTime requestedDeparture,
            int dispatchWindowMinutes) {
        List<VehicleState> states = vehicleStates.values().stream()
                .filter(state -> state.vehicule != null && state.vehicule.getId() != null)
                .filter(state -> !vehiclesUsedForGroup.contains(state.vehicule.getId()))
                .filter(state -> Objects.requireNonNullElse(state.vehicule.getNombrePlaces(), 0) > 0)
                .filter(state -> !isVehicleUnavailableForDay(state, requestedDeparture))
                .filter(state -> isWithinDispatchWindow(state, requestedDeparture, dispatchWindowMinutes))
                .collect(Collectors.toList());

        states.sort(Comparator
                .comparing((VehicleState state) -> Objects.requireNonNullElse(state.vehicule.getNombrePlaces(), 0),
                        Comparator.reverseOrder())
                .thenComparing(state -> computeRealDeparture(state, requestedDeparture))
                .thenComparing(state -> state.tripsCount)
                .thenComparing(state -> "D".equalsIgnoreCase(state.vehicule.getTypeCarburantCode()) ? 0 : 1)
                .thenComparing(state -> Objects.requireNonNullElse(state.vehicule.getId(), Integer.MAX_VALUE)));

        return states;
    }

    private GroupTripSlot openTripSlot(VehicleState state, LocalDateTime requestedDeparture) {
        if (isVehicleUnavailableForDay(state, requestedDeparture)) {
            return null;
        }

        GroupTripSlot slot = new GroupTripSlot();
        slot.vehicleState = state;
        slot.vehicule = state.vehicule;
        slot.tripNumber = state.tripsCount + 1;
        slot.capacityTotal = Objects.requireNonNullElse(state.vehicule.getNombrePlaces(), 0);
        slot.capacityRemaining = slot.capacityTotal;
        slot.assignedPassengers = 0;
        slot.realDeparture = computeRealDeparture(state, requestedDeparture);

        if (slot.realDeparture.isAfter(state.dayEnd)) {
            return null;
        }

        state.tripsCount = slot.tripNumber;
        return slot;
    }

    private LocalDateTime computeRealDeparture(VehicleState state, LocalDateTime requestedDeparture) {
        if (state.nextAvailableAt != null && state.nextAvailableAt.isAfter(requestedDeparture)) {
            return state.nextAvailableAt;
        }
        return requestedDeparture;
    }

    private boolean isVehicleUnavailableForDay(VehicleState state, LocalDateTime requestedDeparture) {
        LocalDateTime realDeparture = computeRealDeparture(state, requestedDeparture);
        return realDeparture.isAfter(state.dayEnd);
    }

    private boolean isWithinDispatchWindow(VehicleState state,
            LocalDateTime requestedDeparture,
            int dispatchWindowMinutes) {
        LocalDateTime realDeparture = computeRealDeparture(state, requestedDeparture);
        LocalDateTime maxDeparture = requestedDeparture.plusMinutes(Math.max(0, dispatchWindowMinutes));
        return !realDeparture.isAfter(maxDeparture);
    }

    private void assignPassengersToTrip(ReservationUnit unit,
            GroupTripSlot slot,
            int passengers,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers) {
        if (passengers <= 0) {
            return;
        }

        PlanningReservation fragment = buildReservationFragment(unit.baseReservation,
                passengers,
                unit.nextPartIndex(),
                unit.baseReservation.getGroupReference());
        fragment.setIdVehicule(slot.vehicule.getId());
        fragment.setVehiculeReference(slot.vehicule.getReference());
        fragment.setHeureDepartReelle(slot.realDeparture);
        fragment.setNombrePassagerAssigne(passengers);

        slot.fragments.add(fragment);
        slot.assignedPassengers += passengers;
        slot.capacityRemaining -= passengers;
        unit.remainingPassengers -= passengers;

        ReservationAssignmentTracker tracker = assignmentTrackers.get(unit.baseReservation.getId());
        if (tracker != null) {
            tracker.assignedPassengers += passengers;
            if (passengers > tracker.bestAssignedPassengers) {
                tracker.bestAssignedPassengers = passengers;
                tracker.principalVehicleId = slot.vehicule.getId();
                tracker.principalVehicleReference = slot.vehicule.getReference();
            } else if (passengers == tracker.bestAssignedPassengers && tracker.principalVehicleId != null
                    && random.nextBoolean()) {
                tracker.principalVehicleId = slot.vehicule.getId();
                tracker.principalVehicleReference = slot.vehicule.getReference();
            }
        }
    }

    private PlanningReservation buildReservationFragment(PlanningReservation source,
            int passengers,
            int partIndex,
            String groupReference) {
        PlanningReservation fragment = new PlanningReservation();
        fragment.setId(source.getId());
        fragment.setClientId(source.getClientId());
        fragment.setNombrePassager(passengers);
        fragment.setDateHeureArrivee(source.getDateHeureArrivee());
        fragment.setIdHotel(source.getIdHotel());
        fragment.setHotelNom(source.getHotelNom());
        fragment.setLieuCode(source.getLieuCode());
        fragment.setLieuLibelle(source.getLieuLibelle());
        fragment.setVolReference(source.getVolReference());
        fragment.setGroupReference(groupReference != null ? groupReference : source.getGroupReference());
        fragment.setNombrePassagerOriginal(source.getNombrePassager());
        fragment.setNombrePassagerAssigne(passengers);
        fragment.setFractionReference("R" + source.getId() + "-P" + partIndex);
        return fragment;
    }

    private PlanningVehiculeTour calculerTour(GroupTripSlot slot,
            ReservationGroup group,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date) {

        List<PlanningReservation> sortedReservations = new ArrayList<>(slot.fragments);
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

        int capaciteTotal = Math.max(0, slot.capacityTotal);
        int capaciteUtilisee = Math.max(0, slot.assignedPassengers);
        int capaciteRestante = Math.max(0, slot.capacityRemaining);
        BigDecimal taux = BigDecimal.ZERO;
        if (capaciteTotal > 0) {
            taux = BigDecimal.valueOf(capaciteUtilisee)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(capaciteTotal), 2, RoundingMode.HALF_UP);
        }

        PlanningVehiculeTour tour = new PlanningVehiculeTour();
        tour.setGroupReference(group.groupReference);
        tour.setVols(group.volsLabel);
        tour.setTotalPassagers(capaciteUtilisee);
        tour.setNumeroTrajet(slot.tripNumber);
        tour.setHeureDepartTheorique(group.requestedDeparture);
        tour.setVehicule(slot.vehicule);
        tour.setReservations(sortedReservations);
        tour.setRoute(routeComputation.routeLabel);
        tour.setDistanceTotaleKm(routeComputation.totalKm.setScale(2, RoundingMode.HALF_UP));
        tour.setDureeTotaleMinutes(totalMinutes);
        tour.setCapaciteVehicule(capaciteTotal);
        tour.setCapaciteUtilisee(capaciteUtilisee);
        tour.setCapaciteRestante(capaciteRestante);
        tour.setTauxRemplissage(taux);
        tour.setHeureDepart(slot.realDeparture);
        tour.setHeureRetour(slot.realDeparture.plusMinutes(totalMinutes));

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

    private List<ReservationGroup> buildWaitingGroups(List<PlanningReservation> reservations,
            int waitingMinutes,
            LocalDate date) {
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
            current.requestedDeparture = current.lastArrival != null
                    ? current.lastArrival
                    : LocalDateTime.of(date, LocalTime.of(8, 0));

            for (PlanningReservation reservation : current.reservations) {
                reservation.setGroupReference(current.groupReference);
            }

            grouped.add(current);
        }

        return grouped;
    }

    /**
     * Traite l'allocation dynamique quand un véhicule revient et qu'il existe des réservations non assignées.
     * Logique :
     * - Si le véhicule revient avec capacité restante : partir immédiatement avec max passagers
     * - Si places dispo > passagers : comparer avec temps d'attente 
     *   - Attendre prochain regroupement OU partir selon temps d'attente estimé
     * - Nouveau regroupement : réservations non assignées en priorité
     */
    private void processDynamicAllocationForReturningVehicles(
            List<PlanningReservation> nonAssignedReservations,
            Map<Integer, VehicleState> vehicleStates,
            LocalDateTime groupDepartTime,
            int dispatchWindowMinutes,
            GroupAssignmentOutcome outcome,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date,
            ReservationGroup currentGroup) {

        if (nonAssignedReservations == null || nonAssignedReservations.isEmpty()) {
            return;
        }

        List<VehicleState> returningVehicles = vehicleStates.values().stream()
                .filter(state -> state.nextAvailableAt != null && 
                        state.nextAvailableAt.isBefore(groupDepartTime))
                .filter(state -> Objects.requireNonNullElse(state.vehicule.getNombrePlaces(), 0) > 0)
                .sorted(Comparator.comparing(state -> state.nextAvailableAt))
                .collect(Collectors.toList());

        for (VehicleState vehicleState : returningVehicles) {
            if (nonAssignedReservations.isEmpty()) {
                break;
            }

            VehicleReturnDecision decision = makeReturnVehicleDecision(
                    vehicleState,
                    nonAssignedReservations,
                    groupDepartTime,
                    dispatchWindowMinutes,
                    parametre);

            if (decision.shouldDepart) {
                // Sélectionner les passagers à embarquer
                List<PassengerSelectionResult> selections = selectPassengersForReturnVehicle(
                        nonAssignedReservations,
                        decision.availableCapacity,
                        assignmentTrackers);

                if (!selections.isEmpty()) {
                    // Créer une tournée de départ immédiat
                    PlanningVehiculeTour impromptuTour = buildImpromptuTour(
                            vehicleState,
                            selections,
                            currentGroup,
                            parametre,
                            distances,
                            date,
                            decision.departureTime,
                            assignmentTrackers);

                    if (impromptuTour != null) {
                        outcome.tours.add(impromptuTour);

                        // Mettre à jour les réservations et states
                        vehicleState.nextAvailableAt = impromptuTour.getHeureRetour();
                        vehicleState.tripsCount++;

                        // Retirer les réservations assignées de la liste des non assignées
                        for (PassengerSelectionResult selection : selections) {
                            nonAssignedReservations.removeIf(r ->
                                    r.getId().equals(selection.reservationId) &&
                                            r.getNombrePassager().equals(selection.passengerCount));
                        }
                    }
                }
            }
        }
    }

    /**
     * Décide si un véhicule qui revient doit partir immédiatement ou attendre.
     */
    private VehicleReturnDecision makeReturnVehicleDecision(
            VehicleState vehicleState,
            List<PlanningReservation> nonAssignedReservations,
            LocalDateTime nextGroupDepartTime,
            int dispatchWindowMinutes,
            PlanningParametre parametre) {

        VehicleReturnDecision decision = new VehicleReturnDecision();
        decision.availableCapacity = Objects.requireNonNullElse(vehicleState.vehicule.getNombrePlaces(), 0);

        // Il y a toujours des réservations non assignées : partir immédiatement par défaut
        decision.departureTime = vehicleState.nextAvailableAt;

        int totalNonAssignedPassengers = nonAssignedReservations.stream()
                .mapToInt(r -> Objects.requireNonNullElse(r.getNombrePassager(), 0))
                .sum();

        // Si places disponibles > passagers à transporter : comparer temps d'attente
        if (decision.availableCapacity > totalNonAssignedPassengers) {
            int estimatedWaitingMinutes = parametre.getTempsAttente() != null
                    ? Math.max(0, parametre.getTempsAttente())
                    : 30;

            // Si l'attente est inférieure au temps d'attente configuré : attendre
            if (estimatedWaitingMinutes <= 15) {  // Seuil de 15 minutes pour attendre
                decision.shouldDepart = false;
                return decision;
            }
        }

        // Partir immédiatement avec max passagers possibles
        decision.shouldDepart = true;
        return decision;
    }

    /**
     * Sélectionne les passagers à embarquer au retour du véhicule.
     * Stratégie : prendre le maximum de passagers selon la capacité, en priorité les plus grands groupes.
     */
    private List<PassengerSelectionResult> selectPassengersForReturnVehicle(
            List<PlanningReservation> nonAssignedReservations,
            int availableCapacity,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers) {

        List<PassengerSelectionResult> result = new ArrayList<>();
        int remainingCapacity = availableCapacity;

        // Trier par nombre de passagers décroissant (plus gros groupes d'abord)
        List<PlanningReservation> sorted = nonAssignedReservations.stream()
                .sorted(Comparator.comparing(
                        r -> Objects.requireNonNullElse(r.getNombrePassager(), 0),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        for (PlanningReservation reservation : sorted) {
            if (remainingCapacity <= 0) {
                break;
            }

            int passengers = Objects.requireNonNullElse(reservation.getNombrePassager(), 0);
            if (passengers <= 0) {
                continue;
            }

            int toAssign = Math.min(passengers, remainingCapacity);

            PassengerSelectionResult selection = new PassengerSelectionResult();
            selection.reservationId = reservation.getId();
            selection.passengerCount = toAssign;
            selection.reservation = reservation;

            result.add(selection);
            remainingCapacity -= toAssign;
        }

        return result;
    }

    /**
     * Construit une tournée impromptu pour un véhicule qui revient.
     */
    private PlanningVehiculeTour buildImpromptuTour(
            VehicleState vehicleState,
            List<PassengerSelectionResult> selections,
            ReservationGroup originalGroup,
            PlanningParametre parametre,
            Map<String, BigDecimal> distances,
            LocalDate date,
            LocalDateTime departureTime,
            Map<Integer, ReservationAssignmentTracker> assignmentTrackers) {

        if (selections.isEmpty()) {
            return null;
        }

        List<PlanningReservation> tourReservations = new ArrayList<>();
        int totalPassengers = 0;

        for (PassengerSelectionResult selection : selections) {
            PlanningReservation fragment = buildReservationFragment(
                    selection.reservation,
                    selection.passengerCount,
                    1,
                    originalGroup.groupReference);

            fragment.setIdVehicule(vehicleState.vehicule.getId());
            fragment.setVehiculeReference(vehicleState.vehicule.getReference());
            fragment.setHeureDepartReelle(departureTime);
            fragment.setNombrePassagerAssigne(selection.passengerCount);

            tourReservations.add(fragment);
            totalPassengers += selection.passengerCount;

            // Mise à jour du tracker
            ReservationAssignmentTracker tracker = assignmentTrackers.get(selection.reservationId);
            if (tracker != null) {
                tracker.assignedPassengers += selection.passengerCount;
                if (selection.passengerCount > tracker.bestAssignedPassengers) {
                    tracker.bestAssignedPassengers = selection.passengerCount;
                    tracker.principalVehicleId = vehicleState.vehicule.getId();
                    tracker.principalVehicleReference = vehicleState.vehicule.getReference();
                }
            }
        }

        // Calculer la route
        List<PlanningReservation> sortedReservations = new ArrayList<>(tourReservations);
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

        int capaciteTotal = Objects.requireNonNullElse(vehicleState.vehicule.getNombrePlaces(), 0);
        int capaciteUtilisee = totalPassengers;
        int capaciteRestante = capaciteTotal - capaciteUtilisee;
        BigDecimal taux = BigDecimal.ZERO;
        if (capaciteTotal > 0) {
            taux = BigDecimal.valueOf(capaciteUtilisee)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(capaciteTotal), 2, RoundingMode.HALF_UP);
        }

        PlanningVehiculeTour tour = new PlanningVehiculeTour();
        tour.setGroupReference(originalGroup.groupReference);
        tour.setVols(originalGroup.volsLabel);
        tour.setTotalPassagers(capaciteUtilisee);
        tour.setNumeroTrajet(vehicleState.tripsCount + 1);
        tour.setHeureDepartTheorique(originalGroup.requestedDeparture);
        tour.setVehicule(vehicleState.vehicule);
        tour.setReservations(sortedReservations);
        tour.setRoute(routeComputation.routeLabel);
        tour.setDistanceTotaleKm(routeComputation.totalKm.setScale(2, RoundingMode.HALF_UP));
        tour.setDureeTotaleMinutes(totalMinutes);
        tour.setCapaciteVehicule(capaciteTotal);
        tour.setCapaciteUtilisee(capaciteUtilisee);
        tour.setCapaciteRestante(capaciteRestante);
        tour.setTauxRemplissage(taux);
        tour.setHeureDepart(departureTime);
        tour.setHeureRetour(departureTime.plusMinutes(totalMinutes));

        applyVisitOrder(sortedReservations, routeComputation.orderedStops);

        return tour;
    }

    private static class VehicleReturnDecision {

        LocalDateTime departureTime;
        boolean shouldDepart = true;  // Partir par défaut
        int availableCapacity;
    }

    private static class PassengerSelectionResult {

        Integer reservationId;
        Integer passengerCount;
        PlanningReservation reservation;
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
        private LocalDateTime requestedDeparture;
    }

    private static class GroupAssignmentOutcome {

        private final List<PlanningVehiculeTour> tours = new ArrayList<>();
        private final List<PlanningReservation> nonAssigned = new ArrayList<>();
    }

    private static class ReservationUnit {

        private final PlanningReservation baseReservation;
        private int remainingPassengers;
        private int partCounter;

        private ReservationUnit(PlanningReservation baseReservation) {
            this.baseReservation = baseReservation;
            this.remainingPassengers = Objects.requireNonNullElse(baseReservation.getNombrePassager(), 0);
            this.partCounter = 1;
        }

        private int nextPartIndex() {
            return partCounter++;
        }
    }

    private static class GroupTripSlot {

        private VehicleState vehicleState;
        private Vehicule vehicule;
        private int tripNumber;
        private int capacityTotal;
        private int capacityRemaining;
        private int assignedPassengers;
        private LocalDateTime realDeparture;
        private final List<PlanningReservation> fragments = new ArrayList<>();
    }

    private static class ReservationAssignmentTracker {

        private int originalPassengers;
        private int assignedPassengers;
        private int bestAssignedPassengers;
        private Integer principalVehicleId;
        private String principalVehicleReference;
    }

    private static class VehicleState {

        private Vehicule vehicule;
        private int tripsCount;
        private LocalDateTime nextAvailableAt;
        private LocalDateTime dayEnd;
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
