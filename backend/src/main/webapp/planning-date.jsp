<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String errorMessage = (String) request.getAttribute("errorMessage");
    String planningDate = (String) request.getAttribute("planningDate");
    if (planningDate == null) {
        planningDate = "";
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planification - Saisie date</title>
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
            max-width: 620px;
            background: #fff;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }
        .message {
            padding: 10px;
            border-radius: 4px;
            margin-bottom: 14px;
            background: #ffe8e8;
            color: #a40000;
        }
        .form-group {
            margin-bottom: 14px;
        }
        label {
            display: block;
            margin-bottom: 6px;
            font-weight: bold;
        }
        input {
            width: 100%;
            padding: 8px;
            box-sizing: border-box;
        }
        button {
            padding: 10px 14px;
            border: none;
            border-radius: 4px;
            color: #fff;
            background: #1366d6;
            cursor: pointer;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <h2>ETU003123 - ETU003367 - ETU003142</h2>
        <a class="menu-link" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
        <a class="menu-link active" href="<%= request.getContextPath() %>/planification">Planification</a>
    </aside>

    <main class="content">
        <section class="card">
            <h1>Planification des réservations</h1>
            <p>Saisissez une date pour lancer l'affectation automatique des véhicules.</p>

            <% if (errorMessage != null && !errorMessage.isBlank()) { %>
                <div class="message"><%= errorMessage %></div>
            <% } %>

            <form method="get" action="<%= request.getContextPath() %>/planification/result">
                <div class="form-group">
                    <label for="date">Date de planification</label>
                    <input id="date" type="date" name="date" value="<%= planningDate %>" required>
                </div>

                <button type="submit">Lancer la planification</button>
            </form>
        </section>
    </main>
</div>
</body>
</html>