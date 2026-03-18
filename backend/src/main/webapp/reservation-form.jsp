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
            padding: 24px;
            box-sizing: border-box;
        }
        .container {
            max-width: 700px;
            margin: 0;
            background: #fff;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }
        h1 {
            margin-top: 0;
        }
        .message {
            padding: 10px;
            border-radius: 4px;
            margin-bottom: 14px;
        }
        .error {
            background: #ffe8e8;
            color: #a40000;
        }
        .success {
            background: #e8ffe8;
            color: #126300;
        }
        .form-group {
            margin-bottom: 14px;
        }
        label {
            display: block;
            margin-bottom: 6px;
            font-weight: bold;
        }
        input, select {
            width: 100%;
            padding: 8px;
            box-sizing: border-box;
        }
        button {
            padding: 10px 16px;
            background: #1366d6;
            border: none;
            color: white;
            border-radius: 4px;
            cursor: pointer;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <h2>Back-office</h2>
        <a class="menu-link active" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
    </aside>
    <main class="content">
        <div class="container">
            <h1>Saisie d'une nouvelle réservation</h1>

    <% if (errorMessage != null && !errorMessage.isBlank()) { %>
        <div class="message error"><%= errorMessage %></div>
    <% } %>

    <% if (successMessage != null && !successMessage.isBlank()) { %>
        <div class="message success"><%= successMessage %></div>
    <% } %>

            <form method="post" action="<%= request.getContextPath() %>/reservations/new">
                <div class="form-group">
                    <label for="clientId">Client (ID sur 4 chiffres)</label>
                    <input id="clientId" type="text" name="clientId" maxlength="4" value="<%= clientId %>" required>
                </div>

                <div class="form-group">
                    <label for="nombrePassager">Nombre de passagers</label>
                    <input id="nombrePassager" type="number" name="nombrePassager" min="1" value="<%= nombrePassager %>" required>
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

                <button type="submit">Enregistrer la réservation</button>
            </form>
        </div>
    </main>
</div>
</body>
</html>