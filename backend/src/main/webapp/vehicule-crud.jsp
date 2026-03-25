<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.example.model.Vehicule" %>
<%@ page import="com.example.model.TypeCarburant" %>
<%
    List<Vehicule> vehicules = (List<Vehicule>) request.getAttribute("vehicules");
    List<TypeCarburant> typesCarburant = (List<TypeCarburant>) request.getAttribute("typesCarburant");
    Map<String, Object> formData = (Map<String, Object>) request.getAttribute("formData");
    Map<String, Object> editData = (Map<String, Object>) request.getAttribute("editData");
    String errorMessage = (String) request.getAttribute("errorMessage");
    String successMessage = (String) request.getAttribute("successMessage");

    String reference = "";
    String nombrePlaces = "";
    String typeCarburantId = "";

    if (formData != null) {
        if (formData.get("reference") != null) {
            reference = formData.get("reference").toString();
        }
        if (formData.get("nombrePlaces") != null) {
            nombrePlaces = formData.get("nombrePlaces").toString();
        }
        if (formData.get("typeCarburantId") != null) {
            typeCarburantId = formData.get("typeCarburantId").toString();
        }
    }

    String editId = "";
    String editReference = "";
    String editNombrePlaces = "";
    String editTypeCarburantId = "";

    if (editData != null) {
        if (editData.get("id") != null) {
            editId = editData.get("id").toString();
        }
        if (editData.get("reference") != null) {
            editReference = editData.get("reference").toString();
        }
        if (editData.get("nombrePlaces") != null) {
            editNombrePlaces = editData.get("nombrePlaces").toString();
        }
        if (editData.get("typeCarburantId") != null) {
            editTypeCarburantId = editData.get("typeCarburantId").toString();
        }
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Gestion des véhicules</title>
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
        .grid {
            display: grid;
            grid-template-columns: repeat(3, minmax(180px, 1fr));
            gap: 12px;
        }
        .form-group {
            margin-bottom: 12px;
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
            padding: 10px 14px;
            border: none;
            border-radius: 4px;
            color: #fff;
            background: #1366d6;
            cursor: pointer;
        }
        button.delete {
            background: #c62828;
        }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            border: 1px solid #e5e7eb;
            padding: 8px;
            text-align: left;
        }
        th {
            background: #f3f4f6;
        }
        .actions {
            display: flex;
            gap: 8px;
            align-items: center;
        }
        form.inline {
            display: inline;
        }
    </style>
</head>
<body>
<div class="layout">
    <aside class="sidebar">
        <h2>ETU003123 - ETU003367 - ETU003142</h2>
        <a class="menu-link" href="<%= request.getContextPath() %>/reservations/new">Réservations</a>
        <a class="menu-link active" href="<%= request.getContextPath() %>/vehicules">Véhicules</a>
        <a class="menu-link" href="<%= request.getContextPath() %>/planification">Planification</a>
    </aside>

    <main class="content">
        <h1>CRUD Véhicules</h1>

        <% if (errorMessage != null && !errorMessage.isBlank()) { %>
            <div class="message error"><%= errorMessage %></div>
        <% } %>

        <% if (successMessage != null && !successMessage.isBlank()) { %>
            <div class="message success"><%= successMessage %></div>
        <% } %>

        <section class="card">
            <h2>Ajouter un véhicule</h2>
            <form method="post" action="<%= request.getContextPath() %>/vehicules/create">
                <div class="grid">
                    <div class="form-group">
                        <label for="reference">Référence</label>
                        <input id="reference" type="text" name="reference" value="<%= reference %>" required>
                    </div>
                    <div class="form-group">
                        <label for="nombrePlaces">Nombre de places</label>
                        <input id="nombrePlaces" type="number" name="nombrePlaces" min="1" value="<%= nombrePlaces %>" required>
                    </div>
                    <div class="form-group">
                        <label for="typeCarburantId">Type carburant</label>
                        <select id="typeCarburantId" name="typeCarburantId" required>
                            <option value="">-- Sélectionner --</option>
                            <% if (typesCarburant != null) {
                                for (TypeCarburant type : typesCarburant) {
                                    String selected = String.valueOf(type.getId()).equals(typeCarburantId) ? "selected" : "";
                            %>
                                <option value="<%= type.getId() %>" <%= selected %>><%= type.getCode() %> - <%= type.getNom() %></option>
                            <%  }
                               } %>
                        </select>
                    </div>
                </div>
                <button type="submit">Créer</button>
            </form>
        </section>

        <% if (!editId.isBlank()) { %>
        <section class="card">
            <h2>Modifier le véhicule #<%= editId %></h2>
            <form method="post" action="<%= request.getContextPath() %>/vehicules/update">
                <input type="hidden" name="id" value="<%= editId %>">
                <div class="grid">
                    <div class="form-group">
                        <label for="editReference">Référence</label>
                        <input id="editReference" type="text" name="reference" value="<%= editReference %>" required>
                    </div>
                    <div class="form-group">
                        <label for="editNombrePlaces">Nombre de places</label>
                        <input id="editNombrePlaces" type="number" name="nombrePlaces" min="1" value="<%= editNombrePlaces %>" required>
                    </div>
                    <div class="form-group">
                        <label for="editTypeCarburantId">Type carburant</label>
                        <select id="editTypeCarburantId" name="typeCarburantId" required>
                            <% if (typesCarburant != null) {
                                for (TypeCarburant type : typesCarburant) {
                                    String selected = String.valueOf(type.getId()).equals(editTypeCarburantId) ? "selected" : "";
                            %>
                                <option value="<%= type.getId() %>" <%= selected %>><%= type.getCode() %> - <%= type.getNom() %></option>
                            <%  }
                               } %>
                        </select>
                    </div>
                </div>
                <button type="submit">Enregistrer modifications</button>
            </form>
        </section>
        <% } %>

        <section class="card">
            <h2>Liste des véhicules</h2>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Référence</th>
                    <th>Places</th>
                    <th>Carburant</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <% if (vehicules != null && !vehicules.isEmpty()) {
                    for (Vehicule vehicule : vehicules) {
                %>
                    <tr>
                        <td><%= vehicule.getId() %></td>
                        <td><%= vehicule.getReference() %></td>
                        <td><%= vehicule.getNombrePlaces() %></td>
                        <td><%= vehicule.getTypeCarburantCode() %> - <%= vehicule.getTypeCarburantNom() %></td>
                        <td>
                            <div class="actions">
                                <a href="<%= request.getContextPath() %>/vehicules/edit?id=<%= vehicule.getId() %>">Modifier</a>
                                <form class="inline" method="post" action="<%= request.getContextPath() %>/vehicules/delete">
                                    <input type="hidden" name="id" value="<%= vehicule.getId() %>">
                                    <button class="delete" type="submit">Supprimer</button>
                                </form>
                            </div>
                        </td>
                    </tr>
                <%  }
                   } else { %>
                    <tr>
                        <td colspan="5">Aucun véhicule enregistré.</td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </section>
    </main>
</div>
</body>
</html>