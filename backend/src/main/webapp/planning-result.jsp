<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.example.model.Vehicule" %>
<%@ page import="com.example.model.PlanningResult" %>
<%@ page import="com.example.model.PlanningReservation" %>
<%@ page import="com.example.model.PlanningVehiculeTour" %>
<%
    PlanningResult planning = (PlanningResult) request.getAttribute("planning");
    List<Vehicule> vehicules = planning != null ? planning.getVehiculesDisponibles() : null;
    List<PlanningVehiculeTour> tours = planning != null ? planning.getToursAssignes() : null;
    List<PlanningReservation> nonAssignees = planning != null ? planning.getReservationsNonAssignees() : null;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");

    int vehiculesCount = vehicules != null ? vehicules.size() : 0;
    int toursCount = tours != null ? tours.size() : 0;
    int nonAssigneesCount = nonAssignees != null ? nonAssignees.size() : 0;
    int totalReservations = planning != null ? planning.getTotalReservations() : 0;
    int assignedReservations = Math.max(0, totalReservations - nonAssigneesCount);
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planification - Résultat</title>
    <style>
        /*
         * Planification des Transferts - Styles
         * Gestion de la planification et attribution des véhicules
         */

        body {
            font-family: "Segoe UI", Arial, sans-serif;
            margin: 0;
            background: #f3f4f6;
            color: #1f2933;
        }

        a { color: inherit; }

        .navbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 22px;
            background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
            color: #fff;
            position: sticky;
            top: 0;
            z-index: 10;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
        }

        .nav-brand {
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        .nav-links {
            display: flex;
            gap: 12px;
            align-items: center;
        }

        .nav-link {
            text-decoration: none;
            padding: 8px 12px;
            border-radius: 6px;
            color: #cdd7f6;
            font-weight: 600;
        }

        .nav-link:hover { background: rgba(255, 255, 255, 0.08); }

        .nav-link.active {
            background: #2563eb;
            color: #fff;
            box-shadow: 0 4px 10px rgba(37, 99, 235, 0.35);
        }

        .page-shell {
            max-width: 1240px;
            margin: 0 auto;
            padding: 22px 18px 40px 18px;
        }

        .planification-page .page-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            flex-wrap: wrap;
            gap: 10px;
        }

        .planification-page .page-title {
            display: flex;
            align-items: center;
            gap: 15px;
        }

        .planification-page .page-title h1 {
            margin: 0;
            font-size: 18px;
            color: #2c3e50;
        }

        .page-subtitle {
            font-size: 13px;
            color: #4b5563;
            background: #e5e7eb;
            padding: 6px 10px;
            border-radius: 10px;
        }

        .planification-page .btn-change-date {
            padding: 10px 14px;
            font-size: 13px;
            background: #2563eb;
            color: #fff;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 600;
            border: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
        }

        .planification-page .btn-change-date:hover { background: #1e4fcc; }

        /* ============================================
           STATISTIQUES (LIGNE COMPACTE)
           ============================================ */

        .stats-grid {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            flex-wrap: wrap;
        }

        .stat-card {
            background: #fff;
            padding: 12px 20px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
            display: flex;
            align-items: center;
            gap: 12px;
            flex: 1;
            min-width: 160px;
        }

        .stat-icon {
            width: 34px;
            height: 34px;
            display: grid;
            place-items: center;
            border-radius: 8px;
            background: #eef2ff;
            color: #1e3a8a;
            font-weight: 700;
            font-size: 14px;
        }

        .stat-value {
            font-size: 24px;
            font-weight: bold;
            line-height: 1;
        }

        .stat-value.blue { color: #3498db; }
        .stat-value.green { color: #27ae60; }
        .stat-value.purple { color: #9b59b6; }
        .stat-value.teal { color: #2ecc71; }
        .stat-value.red { color: #e74c3c; }

        .stat-label {
            font-size: 11px;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 0.3px;
        }

        /* ============================================
           SECTIONS
           ============================================ */

        .section-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin: 20px 0 12px 0;
        }

        .section-icon { font-size: 20px; }

        .section-title {
            font-size: 16px;
            font-weight: 600;
            color: #2c3e50;
            margin: 0;
        }

        .section-count {
            background: #e9ecef;
            padding: 3px 10px;
            border-radius: 10px;
            font-size: 12px;
            color: #666;
        }

        .section-count.danger {
            background: #fde8e8;
            color: #c0392b;
        }

        .panel {
            background: #fff;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
            padding: 16px;
            margin-bottom: 18px;
        }

        /* ============================================
           CARTE VÉHICULE
           ============================================ */

        .vehicles-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 14px;
        }

        .vehicle-card {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            overflow: hidden;
        }

        .vehicle-header {
            background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
            color: white;
            padding: 12px 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
        }

        .vehicle-id-section {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .vehicle-icon { font-size: 22px; }

        .vehicle-info .vehicle-id {
            font-size: 16px;
            font-weight: bold;
        }

        .badge-carburant {
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 10px;
            font-weight: bold;
            text-transform: uppercase;
        }

        .badge-electrique { background: #3498db; color: white; }
        .badge-essence { background: #e74c3c; color: white; }
        .badge-diesel { background: #7f8c8d; color: white; }
        .badge-default { background: #6366f1; color: white; }

        .vehicle-specs {
            display: flex;
            gap: 16px;
            flex-wrap: wrap;
            font-size: 12px;
            opacity: 0.9;
        }

        .spec-item {
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .spec-item strong { font-weight: 700; }

        .vehicle-content { padding: 12px; }

        /* ============================================
           CARTE TRAJET
           ============================================ */

        .trajet-card {
            background: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 12px;
            border: 1px solid #e5e9ed;
            overflow: hidden;
        }

        .trajet-header {
            background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
            color: white;
            padding: 10px 14px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        .trajet-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
        }

        .trajet-number {
            background: rgba(255, 255, 255, 0.2);
            padding: 3px 10px;
            border-radius: 10px;
            font-weight: bold;
            font-size: 12px;
        }

        .trajet-horaires {
            display: flex;
            gap: 12px;
            font-size: 12px;
            flex-wrap: wrap;
        }

        .trajet-content { padding: 12px; }

        .trajet-stats {
            display: flex;
            gap: 8px;
            margin-bottom: 12px;
            flex-wrap: wrap;
        }

        .trajet-stat {
            background: white;
            padding: 8px 12px;
            border-radius: 6px;
            border: 1px solid #e5e9ed;
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
        }

        .trajet-stat-value { font-weight: 700; color: #2c3e50; }
        .trajet-stat-label { font-size: 10px; color: #888; text-transform: uppercase; }

        .assign-pills {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
            margin-bottom: 12px;
        }

        .assign-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 10px;
            background: #eef2ff;
            border: 1px solid #dce3f7;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }

        .assign-pill .pax { color: #2563eb; }
        .assign-pill .loc { color: #4b5563; font-weight: 500; }
        .assign-pill.partial { background: #fff7e6; border-color: #ffe2a8; }
        .assign-pill.partial .pax { color: #c27803; }

        .trajet-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
        }

        @media (max-width: 1100px) {
            .trajet-grid { grid-template-columns: 1fr; }
        }

        /* ============================================
           ITINÉRAIRE
           ============================================ */

        .itinerary-title {
            font-weight: 600;
            font-size: 12px;
            color: #555;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .itinerary-steps {
            background: white;
            border-radius: 6px;
            border: 1px solid #e5e9ed;
            overflow: hidden;
        }

        .itinerary-step {
            display: grid;
            grid-template-columns: 30px 1fr auto;
            align-items: center;
            padding: 8px 10px;
            border-bottom: 1px solid #f0f3f6;
            font-size: 12px;
            transition: background 0.15s ease;
        }

        .itinerary-step:last-child { border-bottom: none; }
        .itinerary-step:hover { background: #fafbfc; }

        .step-indicator {
            display: flex;
            flex-direction: column;
            align-items: center;
            position: relative;
        }

        .step-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            z-index: 2;
            border: 2px solid white;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
        }

        .step-dot.depart { background: #27ae60; }
        .step-dot.arret { background: #3498db; }
        .step-dot.retour { background: #e74c3c; }

        .step-line {
            position: absolute;
            width: 2px;
            background: #3498db;
            top: 16px;
            height: calc(100% + 8px);
            left: 50%;
            transform: translateX(-50%);
            z-index: 1;
        }

        .itinerary-step:last-child .step-line { display: none; }

        .step-content { padding-left: 8px; }

        .step-name {
            font-size: 13px;
            font-weight: 600;
            color: #2c3e50;
        }

        .step-type { font-size: 10px; color: #95a5a6; text-transform: uppercase; }

        .step-metrics {
            display: flex;
            gap: 6px;
            align-items: center;
        }

        .metric-badge {
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 600;
        }

        .metric-badge.distance { background: #ebf5fb; color: #2980b9; }
        .metric-badge.time { background: #eafaf1; color: #1e8449; }
        .metric-badge.return-distance { background: #fef9e7; color: #b7950b; }

        /* ============================================
           TABLE RÉSERVATIONS
           ============================================ */

        .table-responsive {
            background: #ffffff;
            border: 1px solid #ccd6e2;
            border-radius: 10px;
            overflow: auto;
            box-shadow: 0 1px 3px rgba(44, 62, 80, 0.08);
        }

        .table-responsive .reservations-table {
            width: 100%;
            min-width: 780px;
            border-collapse: separate;
            border-spacing: 0;
            font-size: 14px;
            font-variant-numeric: tabular-nums;
        }

        .table-responsive .reservations-table th {
            position: sticky;
            top: 0;
            z-index: 2;
            background: #edf2f7;
            color: #1f2d3d;
            padding: 11px 12px;
            text-align: left;
            font-weight: 700;
            font-size: 12px;
            letter-spacing: 0.2px;
            border-right: 1px solid #d6dee7;
            border-bottom: 2px solid #bec9d4;
            white-space: nowrap;
        }

        .table-responsive .reservations-table td {
            padding: 10px 12px;
            color: #243447;
            border-right: 1px solid #e0e7ef;
            border-bottom: 1px solid #e0e7ef;
            white-space: nowrap;
            background: #ffffff;
        }

        .table-responsive .reservations-table th:last-child,
        .table-responsive .reservations-table td:last-child { border-right: none; }

        .table-responsive .reservations-table tbody tr:nth-child(even) td { background: #f8fbff; }
        .table-responsive .reservations-table tbody tr:hover td { background: #edf5ff; }
        .table-responsive .reservations-table td:nth-child(1) { font-weight: 600; color: #1f2d3d; }

        .reservations-section { background: white; border-radius: 6px; border: 1px solid #e5e9ed; overflow: hidden; }

        .reservations-header {
            background: #f8f9fa;
            padding: 8px 12px;
            font-weight: 600;
            font-size: 12px;
            color: #555;
            border-bottom: 1px solid #e5e9ed;
            display: flex;
            align-items: center;
            gap: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .reservations-table th {
            background: #f8f9fa;
            padding: 6px 10px;
            text-align: left;
            font-weight: 600;
            color: #666;
            font-size: 10px;
            text-transform: uppercase;
            border-bottom: 1px solid #e5e9ed;
        }

        .reservations-table td {
            padding: 8px 10px;
            border-bottom: 1px solid #f0f3f6;
            vertical-align: middle;
        }

        .reservations-table .num { text-align: right; }
        .reservations-table .time { font-variant-numeric: tabular-nums; }

        .reservations-table tr:last-child td { border-bottom: none; }
        .reservations-table tr:hover { background: #fafbfc; }

        .resa-id { font-weight: 700; color: #3498db; }

        .resa-badge {
            background: #e8f4fd;
            color: #2980b9;
            padding: 3px 8px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 600;
            white-space: nowrap;
        }

        .resa-badge.partial {
            background: #fff3cd;
            color: #856404;
            border: 1px dashed #e0a800;
        }

        .reservations-table tr.partial-assignment { background: #fffbf0; }
        .reservations-table tr.partial-assignment:hover { background: #fff8e6; }

        /* ============================================
           RÉSERVATIONS NON ASSIGNÉES
           ============================================ */

        .non-assignee-section { margin-top: 20px; }

        .non-assignee-card {
            background: white;
            border-radius: 8px;
            margin-bottom: 10px;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
            border-left: 3px solid #e74c3c;
            overflow: hidden;
        }

        .non-assignee-header {
            background: #fdf2f2;
            padding: 8px 14px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .non-assignee-id { font-weight: bold; color: #c0392b; font-size: 13px; }

        .non-assignee-motif {
            font-size: 11px;
            color: #e74c3c;
            background: #fff5f5;
            padding: 3px 8px;
            border-radius: 8px;
        }

        .non-assignee-content {
            padding: 12px 14px;
            display: flex;
            gap: 20px;
            align-items: center;
            flex-wrap: wrap;
        }

        .non-assignee-field {
            display: flex;
            flex-direction: column;
            font-size: 12px;
            min-width: 130px;
        }

        .non-assignee-field .label { font-size: 10px; color: #888; text-transform: uppercase; }
        .non-assignee-field .value { font-weight: 600; color: #333; }

        .btn-planifier {
            background: linear-gradient(135deg, #27ae60 0%, #219a52 100%);
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            font-size: 12px;
            margin-left: auto;
            transition: transform 0.2s ease;
        }

        .btn-planifier:hover { transform: translateY(-1px); }

        /* ============================================
           ÉTAT VIDE
           ============================================ */

        .empty-state {
            text-align: center;
            padding: 40px 20px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
        }

        .empty-state .empty-icon { font-size: 48px; margin-bottom: 12px; }
        .empty-state h3 { margin: 0 0 8px 0; color: #2c3e50; }
        .empty-state p { color: #777; margin: 0; }

        /* ============================================
           UTILITAIRES
           ============================================ */

        .tag {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 4px 10px;
            background: #e5e7eb;
            border-radius: 12px;
            font-size: 11px;
        }

        .footer {
            padding: 16px 18px 26px 18px;
            background: #111827;
            color: #d1d5db;
            text-align: center;
            font-size: 13px;
            margin-top: 30px;
        }
    </style>
</head>
<body class="planification-page">
<header class="navbar">
    <div class="nav-brand">
        <span>🚐</span>
        <span>Back-Office Transferts</span>
    </div>
    <nav class="nav-links">
        <a class="nav-link" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="nav-link" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
        <a class="nav-link active" href="<%= request.getContextPath() %>/planification">Planification</a>
    </nav>
</header>

<main class="page-shell">
    <div class="page-header">
        <div class="page-title">
            <h1>Planification des transferts</h1>
            <span class="page-subtitle">Date: <%= planning != null ? planning.getDate() : "-" %></span>
        </div>
        <a class="btn-change-date" href="<%= request.getContextPath() %>/planification">Changer de date</a>
    </div>

    <% if (planning == null) { %>
    <div class="empty-state">
        <div class="empty-icon">📅</div>
        <h3>Aucun résultat à afficher</h3>
        <p>Sélectionnez une date pour lancer la planification.</p>
        <div style="margin-top: 12px;">
            <a class="btn-change-date" href="<%= request.getContextPath() %>/planification">Choisir une date</a>
        </div>
    </div>
    <% } else { %>

    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-icon">🚐</div>
            <div>
                <div class="stat-value blue"><%= vehiculesCount %></div>
                <div class="stat-label">Véhicules disponibles</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon">🧭</div>
            <div>
                <div class="stat-value purple"><%= toursCount %></div>
                <div class="stat-label">Trajets planifiés</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon">✅</div>
            <div>
                <div class="stat-value green"><%= assignedReservations %></div>
                <div class="stat-label">Réservations assignées</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon">📄</div>
            <div>
                <div class="stat-value teal"><%= totalReservations %></div>
                <div class="stat-label">Total réservations</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon">⚠️</div>
            <div>
                <div class="stat-value red"><%= nonAssigneesCount %></div>
                <div class="stat-label">Non assignées</div>
            </div>
        </div>
    </div>

    <div class="section-header">
        <div class="section-icon">📊</div>
        <h2 class="section-title">Vue regroupée</h2>
        <span class="section-count <%= toursCount == 0 ? "danger" : "" %>"><%= toursCount %></span>
    </div>
    <div class="panel">
        <% if (tours != null && !tours.isEmpty()) { %>
            <div class="table-responsive">
                <table class="reservations-table" style="min-width: 720px;">
                    <thead>
                    <tr>
                        <th>Véhicule</th>
                        <th>Clients</th>
                        <th>Nb pers</th>
                        <th>Heure départ</th>
                        <th>Heure retour</th>
                        <th>Durée (min)</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (PlanningVehiculeTour tour : tours) {
                        java.util.LinkedHashSet<String> clients = new java.util.LinkedHashSet<>();
                        for (PlanningReservation r : tour.getReservations()) {
                            clients.add("Client" + (r.getClientId() != null ? r.getClientId() : ""));
                        }
                    %>
                    <tr>
                        <td><strong><%= tour.getVehicule().getReference() %></strong></td>
                        <td><%= String.join(", ", clients) %></td>
                        <td><%= tour.getTotalPassagers() != null ? tour.getTotalPassagers() : 0 %></td>
                        <td><%= tour.getHeureDepart() != null ? tf.format(tour.getHeureDepart()) : "-" %></td>
                        <td><%= tour.getHeureRetour() != null ? tf.format(tour.getHeureRetour()) : "-" %></td>
                        <td><%= tour.getDureeTotaleMinutes() %></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        <% } else { %>
            <div class="empty-state" style="box-shadow:none;">
                <div class="empty-icon">📊</div>
                <p>Aucun trajet planifié.</p>
            </div>
        <% } %>
    </div>

    <div class="section-header">
        <div class="section-icon">🚐</div>
        <h2 class="section-title">Véhicules disponibles</h2>
        <span class="section-count"><%= vehiculesCount %></span>
    </div>
    <div class="panel">
        <% if (vehicules != null && !vehicules.isEmpty()) { %>
            <div class="vehicles-grid">
                <% for (Vehicule vehicule : vehicules) { %>
                <div class="vehicle-card">
                    <div class="vehicle-header">
                        <div class="vehicle-id-section">
                            <div class="vehicle-icon">🚐</div>
                            <div class="vehicle-info">
                                <div class="vehicle-id"><%= vehicule.getReference() %></div>
                                <div class="tag">ID <%= vehicule.getId() %></div>
                            </div>
                        </div>
                        <div class="badge-carburant <%= "ELEC".equalsIgnoreCase(vehicule.getTypeCarburantCode()) ? "badge-electrique" : ("ESS".equalsIgnoreCase(vehicule.getTypeCarburantCode()) ? "badge-essence" : ("DSL".equalsIgnoreCase(vehicule.getTypeCarburantCode()) ? "badge-diesel" : "badge-default")) %>">
                            <%= vehicule.getTypeCarburantCode() %> - <%= vehicule.getTypeCarburantNom() %>
                        </div>
                    </div>
                    <div class="vehicle-content">
                        <div class="vehicle-specs">
                            <div class="spec-item"><strong>Places:</strong> <%= vehicule.getNombrePlaces() %></div>
                            <div class="spec-item"><strong>Carburant:</strong> <%= vehicule.getTypeCarburantNom() %></div>
                        </div>
                    </div>
                </div>
                <% } %>
            </div>
        <% } else { %>
            <div class="empty-state" style="box-shadow:none;">
                <div class="empty-icon">ℹ️</div>
                <p>Aucun véhicule disponible.</p>
            </div>
        <% } %>
    </div>

    <div class="section-header">
        <div class="section-icon">🧭</div>
        <h2 class="section-title">Trajets et assignations</h2>
        <span class="section-count <%= toursCount == 0 ? "danger" : "" %>"><%= toursCount %></span>
    </div>
    <% if (tours != null && !tours.isEmpty()) { %>
        <% for (PlanningVehiculeTour tour : tours) { %>
            <div class="vehicle-card">
                <div class="vehicle-header">
                    <div class="vehicle-id-section">
                        <div class="vehicle-icon">🧭</div>
                        <div class="vehicle-info">
                            <div class="vehicle-id">Véhicule <%= tour.getVehicule().getReference() %></div>
                            <div class="vehicle-specs">
                                <div class="spec-item">Places: <strong><%= tour.getVehicule().getNombrePlaces() %></strong></div>
                                <div class="spec-item">Capacité utilisée: <strong><%= tour.getCapaciteUtilisee() != null ? tour.getCapaciteUtilisee() : "-" %></strong></div>
                                <div class="spec-item">Restant: <strong><%= tour.getCapaciteRestante() != null ? tour.getCapaciteRestante() : "-" %></strong></div>
                            </div>
                        </div>
                    </div>
                    <div class="badge-carburant <%= "ELEC".equalsIgnoreCase(tour.getVehicule().getTypeCarburantCode()) ? "badge-electrique" : ("ESS".equalsIgnoreCase(tour.getVehicule().getTypeCarburantCode()) ? "badge-essence" : ("DSL".equalsIgnoreCase(tour.getVehicule().getTypeCarburantCode()) ? "badge-diesel" : "badge-default")) %>">
                        <%= tour.getVehicule().getTypeCarburantCode() %> - <%= tour.getVehicule().getTypeCarburantNom() %>
                    </div>
                </div>

                <div class="trajet-card">
                    <div class="trajet-header">
                        <div class="trajet-title">
                            <span class="trajet-number">Trajet <%= tour.getNumeroTrajet() != null ? tour.getNumeroTrajet() : "-" %></span>
                            <span>Groupe <%= tour.getGroupReference() != null ? tour.getGroupReference() : "-" %></span>
                            <span class="tag">Vols: <%= tour.getVols() != null ? tour.getVols() : "-" %></span>
                        </div>
                        <div class="trajet-horaires">
                            <span>Départ théorique: <strong><%= tour.getHeureDepartTheorique() != null ? dtf.format(tour.getHeureDepartTheorique()) : "-" %></strong></span>
                            <span>Départ réel: <strong><%= tour.getHeureDepart() != null ? dtf.format(tour.getHeureDepart()) : "-" %></strong></span>
                            <span>Retour: <strong><%= tour.getHeureRetour() != null ? dtf.format(tour.getHeureRetour()) : "-" %></strong></span>
                        </div>
                    </div>
                    <div class="trajet-content">
                        <div class="trajet-stats">
                            <div class="trajet-stat">
                                <div class="trajet-stat-value"><%= tour.getTotalPassagers() != null ? tour.getTotalPassagers() : 0 %></div>
                                <div class="trajet-stat-label">Passagers groupe</div>
                            </div>
                            <div class="trajet-stat">
                                <div class="trajet-stat-value"><%= tour.getDistanceTotaleKm() %> km</div>
                                <div class="trajet-stat-label">Distance</div>
                            </div>
                            <div class="trajet-stat">
                                <div class="trajet-stat-value"><%= tour.getDureeTotaleMinutes() %> min</div>
                                <div class="trajet-stat-label">Durée</div>
                            </div>
                            <div class="trajet-stat">
                                <div class="trajet-stat-value"><%= tour.getTauxRemplissage() != null ? tour.getTauxRemplissage() : "0" %>%</div>
                                <div class="trajet-stat-label">Remplissage</div>
                            </div>
                            <div class="trajet-stat">
                                <div class="trajet-stat-value"><%= tour.getRoute() %></div>
                                <div class="trajet-stat-label">Route</div>
                            </div>
                        </div>

                        <div class="assign-pills">
                            <% for (PlanningReservation reservation : tour.getReservations()) {
                                boolean partial = reservation.getNombrePassagerAssigne() != null
                                        && reservation.getNombrePassagerOriginal() != null
                                        && !reservation.getNombrePassagerAssigne().equals(reservation.getNombrePassagerOriginal());
                            %>
                            <span class="assign-pill <%= partial ? "partial" : "" %>">
                                <span>#<%= reservation.getId() %></span>
                                <span class="pax"><%= reservation.getNombrePassagerAssigne() != null ? reservation.getNombrePassagerAssigne() : reservation.getNombrePassager() %> pax</span>
                                <span class="loc"><%= reservation.getLieuLibelle() %></span>
                            </span>
                            <% } %>
                        </div>

                        <div class="trajet-grid">
                            <div class="itinerary-section">
                                <div class="itinerary-title">Itinéraire</div>
                                <div class="itinerary-steps">
                                    <%
                                        List<PlanningReservation> tourReservations = tour.getReservations();
                                        int idx = 0;
                                        for (PlanningReservation reservation : tourReservations) {
                                            String stepClass;
                                            if (idx == 0) {
                                                stepClass = "depart";
                                            } else if (idx == tourReservations.size() - 1) {
                                                stepClass = "retour";
                                            } else {
                                                stepClass = "arret";
                                            }
                                    %>
                                    <div class="itinerary-step">
                                        <div class="step-indicator">
                                            <div class="step-dot <%= stepClass %>"></div>
                                            <div class="step-line"></div>
                                        </div>
                                        <div class="step-content">
                                            <div class="step-name">Réservation #<%= reservation.getId() %> - <%= reservation.getLieuLibelle() %></div>
                                            <div class="step-type">
                                                <%= reservation.getOrdrePassage() != null ? "Ordre " + reservation.getOrdrePassage() : "Ordre -" %> |
                                                Arrivée: <%= reservation.getDateHeureArrivee() != null ? dtf.format(reservation.getDateHeureArrivee()) : "-" %>
                                            </div>
                                            <div class="step-metrics">
                                                <span class="metric-badge distance">Passagers <%= reservation.getNombrePassagerAssigne() != null ? reservation.getNombrePassagerAssigne() : reservation.getNombrePassager() %></span>
                                                <span class="metric-badge time">Départ réel <%= reservation.getHeureDepartReelle() != null ? dtf.format(reservation.getHeureDepartReelle()) : "-" %></span>
                                            </div>
                                        </div>
                                        <div class="step-badge <%= stepClass %>"><%= stepClass %></div>
                                    </div>
                                    <%
                                            idx++;
                                        }
                                    %>
                                </div>
                            </div>

                            <div>
                                <div class="reservations-section">
                                    <div class="reservations-header">Détail des réservations</div>
                                    <div class="table-responsive">
                                        <table class="reservations-table">
                                            <colgroup>
                                                <col span="1" style="width: 9%">
                                                <col span="1" style="width: 8%">
                                                <col span="1" style="width: 8%">
                                                <col span="1" style="width: 9%">
                                                <col span="1" style="width: 9%">
                                                <col span="1" style="width: 12%">
                                                <col span="1" style="width: 12%">
                                                <col span="1" style="width: 8%">
                                                <col span="1" style="width: 10%">
                                                <col span="1" style="width: 10%">
                                                <col span="1" style="width: 10%">
                                            </colgroup>
                                            <thead>
                                            <tr>
                                                <th>Réservation</th>
                                                <th>Fraction</th>
                                                <th>Groupe</th>
                                                <th>Vol</th>
                                                <th>Client</th>
                                                <th>Passagers assignés</th>
                                                <th>Passagers origine</th>
                                                <th>Ordre</th>
                                                <th>Arrivée</th>
                                                <th>Départ</th>
                                                <th>Lieu</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            <% for (PlanningReservation reservation : tour.getReservations()) {
                                                boolean partial = reservation.getNombrePassagerAssigne() != null
                                                        && reservation.getNombrePassagerOriginal() != null
                                                        && !reservation.getNombrePassagerAssigne().equals(reservation.getNombrePassagerOriginal());
                                            %>
                                            <tr class="<%= partial ? "partial-assignment" : "" %>">
                                                <td class="resa-id">#<%= reservation.getId() %></td>
                                                <td><%= reservation.getFractionReference() != null ? reservation.getFractionReference() : "-" %></td>
                                                <td><%= reservation.getGroupReference() != null ? reservation.getGroupReference() : "-" %></td>
                                                <td><%= reservation.getVolReference() != null ? reservation.getVolReference() : "-" %></td>
                                                <td><%= reservation.getClientId() %></td>
                                                <td class="num">
                                                    <span class="resa-badge <%= partial ? "partial" : "" %>"><%= reservation.getNombrePassagerAssigne() != null ? reservation.getNombrePassagerAssigne() : reservation.getNombrePassager() %></span>
                                                </td>
                                                <td class="num"><%= reservation.getNombrePassagerOriginal() != null ? reservation.getNombrePassagerOriginal() : reservation.getNombrePassager() %></td>
                                                <td class="num"><%= reservation.getOrdrePassage() != null ? reservation.getOrdrePassage() : "-" %></td>
                                                <td class="time"><%= reservation.getDateHeureArrivee() != null ? dtf.format(reservation.getDateHeureArrivee()) : "-" %></td>
                                                <td class="time"><%= reservation.getHeureDepartReelle() != null ? dtf.format(reservation.getHeureDepartReelle()) : "-" %></td>
                                                <td><%= reservation.getLieuCode() %> - <%= reservation.getLieuLibelle() %></td>
                                            </tr>
                                            <% } %>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        <% } %>
    <% } else { %>
        <div class="empty-state">
            <div class="empty-icon">🧭</div>
            <h3>Aucun trajet planifié</h3>
            <p>Ajoutez des réservations ou sélectionnez une autre date.</p>
        </div>
    <% } %>

    <div class="section-header">
        <div class="section-icon">⏳</div>
        <h2 class="section-title">Réservations non assignées</h2>
        <span class="section-count danger"><%= nonAssigneesCount %></span>
    </div>
    <div class="panel non-assignee-section">
        <% if (nonAssignees != null && !nonAssignees.isEmpty()) { %>
            <% for (PlanningReservation reservation : nonAssignees) { %>
            <div class="non-assignee-card">
                <div class="non-assignee-header">
                    <div class="non-assignee-id">Réservation #<%= reservation.getId() %></div>
                    <div class="non-assignee-motif"><%= reservation.getRaisonNonAssignation() != null ? reservation.getRaisonNonAssignation() : "Aucun véhicule disponible" %></div>
                </div>
                <div class="non-assignee-content">
                    <div class="non-assignee-field">
                        <span class="label">Fraction</span>
                        <span class="value"><%= reservation.getFractionReference() != null ? reservation.getFractionReference() : "-" %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Groupe</span>
                        <span class="value"><%= reservation.getGroupReference() != null ? reservation.getGroupReference() : "-" %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Vol</span>
                        <span class="value"><%= reservation.getVolReference() != null ? reservation.getVolReference() : "-" %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Client</span>
                        <span class="value"><%= reservation.getClientId() %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Passagers restants</span>
                        <span class="value"><%= reservation.getNombrePassager() %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Arrivée</span>
                        <span class="value"><%= reservation.getDateHeureArrivee() != null ? dtf.format(reservation.getDateHeureArrivee()) : "-" %></span>
                    </div>
                    <div class="non-assignee-field">
                        <span class="label">Lieu</span>
                        <span class="value"><%= reservation.getLieuCode() %> - <%= reservation.getLieuLibelle() %></span>
                    </div>
                    <button type="button" class="btn-planifier">Tenter une réaffectation</button>
                </div>
            </div>
            <% } %>
        <% } else { %>
            <div class="empty-state" style="box-shadow:none;">
                <div class="empty-icon">✅</div>
                <p>Aucune réservation non assignée.</p>
            </div>
        <% } %>
    </div>
    <% } %>
</main>

<footer class="footer">
    © 2026 Back-Office Transferts — Planification des véhicules et réservations
</footer>
</body>
</html>