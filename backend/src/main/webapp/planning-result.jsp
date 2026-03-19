<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.example.model.Vehicule" %>
<%@ page import="com.example.model.PlanningResult" %>
<%@ page import="com.example.model.PlanningReservation" %>
<%@ page import="com.example.model.PlanningVehiculeSuivi" %>
<%@ page import="com.example.model.PlanningVehiculeTour" %>
<%
    PlanningResult planning = (PlanningResult) request.getAttribute("planning");
    List<Vehicule> vehicules = planning != null ? planning.getVehiculesDisponibles() : null;
    List<PlanningVehiculeTour> tours = planning != null ? planning.getToursAssignes() : null;
    List<PlanningReservation> nonAssignees = planning != null ? planning.getReservationsNonAssignees() : null;
    List<PlanningVehiculeSuivi> suivis = planning != null ? planning.getSuiviVehicules() : null;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planification - Résultat</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            background: #f5f6fa;
        }
        .layout {
            display: flex;
            min-height: 100vh;
        }
        .sidebar {
            width: 240px;
            background: #1f2937;
            color: #fff;
            padding: 24px 16px;
            box-sizing: border-box;
        }
        .sidebar h2 {
            margin-top: 0;
            font-size: 18px;
        }
        .menu-link {
            display: block;
            color: #dbeafe;
            text-decoration: none;
            padding: 10px 12px;
            border-radius: 6px;
            margin-bottom: 8px;
        }
        .menu-link.active {
            background: #3b82f6;
            color: #fff;
        }
        .menu-link:hover {
            background: #374151;
        }
        .content {
            flex: 1;
            padding: 24px;
            box-sizing: border-box;
        }
        .card {
            background: #fff;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            margin-bottom: 16px;
        }
        .meta {
            font-size: 14px;
            color: #374151;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
        }
        th, td {
            border: 1px solid #e5e7eb;
            padding: 8px;
            text-align: left;
        }
        th {
            background: #f3f4f6;
        }
        .tour {
            border: 1px solid #dbe3f0;
            border-radius: 6px;
            padding: 14px;
            margin-bottom: 12px;
            background: #f9fbff;
        }
        .top-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }
        .btn-link {
            text-decoration: none;
            background: #1366d6;
            color: #fff;
            padding: 8px 12px;
            border-radius: 4px;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <h2>Back-office</h2>
        <a class="menu-link" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
        <a class="menu-link active" href="<%= request.getContextPath() %>/planification">Planification</a>
    </aside>

    <main class="content">
        <section class="card">
            <div class="top-bar">
                <h1 style="margin:0;">Résultat de planification</h1>
                <a class="btn-link" href="<%= request.getContextPath() %>/planification">Nouvelle date</a>
            </div>

            <% if (planning != null) { %>
                <p class="meta">
                    Date : <strong><%= planning.getDate() %></strong> |
                    Réservations traitées : <strong><%= planning.getTotalReservations() %></strong> |
                    Non assignées : <strong><%= nonAssignees != null ? nonAssignees.size() : 0 %></strong>
                </p>
            <% } %>
        </section>

        <section class="card">
            <h2>Véhicules disponibles</h2>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Référence</th>
                    <th>Places</th>
                    <th>Carburant</th>
                </tr>
                </thead>
                <tbody>
                <% if (vehicules != null && !vehicules.isEmpty()) {
                    for (Vehicule vehicule : vehicules) { %>
                    <tr>
                        <td><%= vehicule.getId() %></td>
                        <td><%= vehicule.getReference() %></td>
                        <td><%= vehicule.getNombrePlaces() %></td>
                        <td><%= vehicule.getTypeCarburantCode() %> - <%= vehicule.getTypeCarburantNom() %></td>
                    </tr>
                <%  }
                   } else { %>
                    <tr><td colspan="4">Aucun véhicule disponible.</td></tr>
                <% } %>
                </tbody>
            </table>
        </section>

        <section class="card">
            <h2>Réservations assignées</h2>
            <% if (tours != null && !tours.isEmpty()) {
                for (PlanningVehiculeTour tour : tours) { %>
                <div class="tour">
                    <h3 style="margin-top:0;">
                        Groupe <%= tour.getGroupReference() != null ? tour.getGroupReference() : "-" %> - Véhicule <%= tour.getVehicule().getReference() %>
                        (places: <%= tour.getVehicule().getNombrePlaces() %>, carburant: <%= tour.getVehicule().getTypeCarburantCode() %>)
                    </h3>
                    <p class="meta">
                        Vols regroupés: <strong><%= tour.getVols() != null ? tour.getVols() : "-" %></strong> |
                        Passagers groupe: <strong><%= tour.getTotalPassagers() != null ? tour.getTotalPassagers() : 0 %></strong><br>
                        Route: <strong><%= tour.getRoute() %></strong><br>
                        Départ: <strong><%= tour.getHeureDepart() != null ? dtf.format(tour.getHeureDepart()) : "-" %></strong> |
                        Arrivée aéroport: <strong><%= tour.getHeureRetour() != null ? dtf.format(tour.getHeureRetour()) : "-" %></strong> |
                        Distance: <strong><%= tour.getDistanceTotaleKm() %> km</strong> |
                        Durée: <strong><%= tour.getDureeTotaleMinutes() %> min</strong>
                    </p>

                    <table>
                        <thead>
                        <tr>
                            <th>Réservation</th>
                            <th>Groupe</th>
                            <th>Vol</th>
                            <th>Client</th>
                            <th>Passagers</th>
                            <th>Ordre passage</th>
                            <th>Heure arrivée</th>
                            <th>Départ réel</th>
                            <th>Lieu</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (PlanningReservation reservation : tour.getReservations()) { %>
                            <tr>
                                <td>#<%= reservation.getId() %></td>
                                <td><%= reservation.getGroupReference() != null ? reservation.getGroupReference() : "-" %></td>
                                <td><%= reservation.getVolReference() != null ? reservation.getVolReference() : "-" %></td>
                                <td><%= reservation.getClientId() %></td>
                                <td><%= reservation.getNombrePassager() %></td>
                                <td><%= reservation.getOrdrePassage() != null ? reservation.getOrdrePassage() : "-" %></td>
                                <td><%= reservation.getDateHeureArrivee() != null ? dtf.format(reservation.getDateHeureArrivee()) : "-" %></td>
                                <td><%= reservation.getDateHeureDepartReelle() != null ? dtf.format(reservation.getDateHeureDepartReelle()) : "-" %></td>
                                <td><%= reservation.getLieuCode() %> - <%= reservation.getLieuLibelle() %></td>
                            </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            <%  }
               } else { %>
                <p>Aucune réservation n'a pu être assignée.</p>
            <% } %>
        </section>

        <section class="card">
            <h2>Suivi des véhicules</h2>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Référence</th>
                    <th>Trajets effectués</th>
                    <th>Historique (départ → retour)</th>
                </tr>
                </thead>
                <tbody>
                <% if (suivis != null && !suivis.isEmpty()) {
                    for (PlanningVehiculeSuivi suivi : suivis) {
                        Vehicule v = suivi.getVehicule(); %>
                    <tr>
                        <td><%= v != null ? v.getId() : "-" %></td>
                        <td><%= v != null && v.getReference() != null ? v.getReference() : "-" %></td>
                        <td><%= suivi.getNombreTrajets() != null ? suivi.getNombreTrajets() : 0 %></td>
                        <td>
                            <% if (suivi.getHistoriqueTrajets() != null && !suivi.getHistoriqueTrajets().isEmpty()) {
                                for (PlanningVehiculeTour t : suivi.getHistoriqueTrajets()) { %>
                                    Groupe <%= t.getGroupReference() != null ? t.getGroupReference() : "-" %> :
                                    <%= t.getHeureDepart() != null ? dtf.format(t.getHeureDepart()) : "-" %>
                                    →
                                    <%= t.getHeureRetour() != null ? dtf.format(t.getHeureRetour()) : "-" %>
                                    <br>
                            <%  }
                               } else { %>
                                -
                            <% } %>
                        </td>
                    </tr>
                <%  }
                   } else { %>
                    <tr><td colspan="4">Aucune donnée de suivi.</td></tr>
                <% } %>
                </tbody>
            </table>
        </section>

        <section class="card">
            <h2>Réservations non assignées</h2>
            <table>
                <thead>
                <tr>
                    <th>Réservation</th>
                    <th>Groupe</th>
                    <th>Vol</th>
                    <th>Client</th>
                    <th>Passagers</th>
                    <th>Heure arrivée</th>
                    <th>Lieu</th>
                    <th>Raison</th>
                </tr>
                </thead>
                <tbody>
                <% if (nonAssignees != null && !nonAssignees.isEmpty()) {
                    for (PlanningReservation reservation : nonAssignees) { %>
                    <tr>
                        <td>#<%= reservation.getId() %></td>
                        <td><%= reservation.getGroupReference() != null ? reservation.getGroupReference() : "-" %></td>
                        <td><%= reservation.getVolReference() != null ? reservation.getVolReference() : "-" %></td>
                        <td><%= reservation.getClientId() %></td>
                        <td><%= reservation.getNombrePassager() %></td>
                        <td><%= reservation.getDateHeureArrivee() != null ? dtf.format(reservation.getDateHeureArrivee()) : "-" %></td>
                        <td><%= reservation.getLieuCode() %> - <%= reservation.getLieuLibelle() %></td>
                        <td>Aucun véhicule avec capacité suffisante</td>
                    </tr>
                <%  }
                   } else { %>
                    <tr>
                        <td colspan="8">Aucune réservation non assignée.</td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </section>
    </main>
</div>
</body>
</html>