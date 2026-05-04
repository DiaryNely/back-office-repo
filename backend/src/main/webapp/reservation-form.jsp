<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.example.model.Hotel" %>
<%
    List<Hotel> hotels = (List<Hotel>) request.getAttribute("hotels");
    Map<String, Object> formData = (Map<String, Object>) request.getAttribute("formData");
    String errorMessage = (String) request.getAttribute("errorMessage");
    String successMessage = (String) request.getAttribute("successMessage");

    String clientId = "";
    String nombrePassager = "";
    String dateHeureArrivee = "";
    String idHotel = "";

    if (formData != null) {
        if (formData.get("clientId") != null) {
            clientId = formData.get("clientId").toString();
        }
        if (formData.get("nombrePassager") != null) {
            nombrePassager = formData.get("nombrePassager").toString();
        }
        if (formData.get("dateHeureArrivee") != null) {
            dateHeureArrivee = formData.get("dateHeureArrivee").toString();
        }
        if (formData.get("idHotel") != null) {
            idHotel = formData.get("idHotel").toString();
        }
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Nouvelle réservation</title>
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
            padding: 32px 28px;
            box-sizing: border-box;
        }
        .container {
            max-width: 560px;
            background: #fff;
            padding: 32px 36px;
            border-radius: 12px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.10);
            border-top: 4px solid #3b82f6;
        }
        h1 {
            margin-top: 0;
            margin-bottom: 6px;
            font-size: 22px;
            color: #1f2937;
        }
        .form-subtitle {
            color: #6b7280;
            font-size: 14px;
            margin-top: 0;
            margin-bottom: 24px;
        }
        .divider {
            border: none;
            border-top: 1px solid #e5e7eb;
            margin: 0 0 24px 0;
        }
        .message {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 12px 14px;
            border-radius: 6px;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .error {
            background: #fef2f2;
            color: #991b1b;
            border: 1px solid #fecaca;
        }
        .success {
            background: #f0fdf4;
            color: #166534;
            border: 1px solid #bbf7d0;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 6px;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            letter-spacing: 0.01em;
        }
        input, select {
            width: 100%;
            padding: 10px 12px;
            box-sizing: border-box;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            color: #1f2937;
            background: #f9fafb;
            transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
            outline: none;
        }
        input:focus, select:focus {
            border-color: #3b82f6;
            background: #fff;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
        }
        select {
            appearance: none;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%236b7280' d='M0 0l6 8 6-8z'/%3E%3C/svg%3E");
            background-repeat: no-repeat;
            background-position: right 12px center;
            padding-right: 32px;
        }
        .form-actions {
            margin-top: 28px;
        }
        button[type="submit"] {
            padding: 11px 24px;
            background: #2563eb;
            border: none;
            color: #fff;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s, box-shadow 0.2s;
            box-shadow: 0 1px 3px rgba(37, 99, 235, 0.30);
        }
        button[type="submit"]:hover {
            background: #1d4ed8;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.35);
        }
        button[type="submit"]:active {
            background: #1e40af;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <h2>Back-office</h2>
        <a class="menu-link active" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/planification">Planification</a>
    </aside>
    <main class="content">
        <div class="container">
            <h1>Nouvelle réservation</h1>
            <p class="form-subtitle">Remplissez les informations ci-dessous pour créer une réservation.</p>
            <hr class="divider">

    <% if (errorMessage != null && !errorMessage.isBlank()) { %>
        <div class="message error"><%= errorMessage %></div>
    <% } %>

    <% if (successMessage != null && !successMessage.isBlank()) { %>
        <div class="message success"><%= successMessage %></div>
    <% } %>

            <form method="post" action="<%= request.getContextPath() %>/reservations/new">
                <div class="form-group">
                    <label for="clientId">ID Client (4 chiffres)</label>
                    <input id="clientId" type="text" name="clientId" maxlength="4" placeholder="ex : 1042" value="<%= clientId %>" required>
                </div>

                <div class="form-group">
                    <label for="nombrePassager">Nombre de passagers</label>
                    <input id="nombrePassager" type="number" name="nombrePassager" min="1" placeholder="ex : 3" value="<%= nombrePassager %>" required>
                </div>

                <div class="form-group">
                    <label for="dateHeureArrivee">Date et heure d'arrivée</label>
                    <input id="dateHeureArrivee" type="datetime-local" name="dateHeureArrivee" value="<%= dateHeureArrivee %>" required>
                </div>

                <div class="form-group">
                    <label for="idHotel">Hôtel</label>
                    <select id="idHotel" name="idHotel" required>
                        <option value="">-- Sélectionner un hôtel --</option>
                        <% if (hotels != null) {
                            for (Hotel hotel : hotels) {
                                String selected = String.valueOf(hotel.getId()).equals(idHotel) ? "selected" : "";
                        %>
                            <option value="<%= hotel.getId() %>" <%= selected %>>
                                <%= hotel.getNom() %> - <%= hotel.getAdresse() %>
                            </option>
                        <%  }
                           } %>
                    </select>
                </div>

                <div class="form-actions">
                    <button type="submit">Enregistrer la réservation</button>
                </div>
            </form>
        </div>
    </main>
</div>
</body>
</html>