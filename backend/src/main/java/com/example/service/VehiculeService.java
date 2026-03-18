package com.example.service;

import java.sql.SQLException;
import java.util.List;

import com.example.dao.VehiculeDAO;
import com.example.model.TypeCarburant;
import com.example.model.Vehicule;

public class VehiculeService {

    private final VehiculeDAO vehiculeDAO;

    public VehiculeService() {
        this.vehiculeDAO = new VehiculeDAO();
    }

    public List<Vehicule> getVehicules() throws SQLException {
        return vehiculeDAO.findAll();
    }

    public List<TypeCarburant> getTypesCarburant() throws SQLException {
        return vehiculeDAO.findTypesCarburant();
    }

    public Vehicule getVehicule(Integer id) throws SQLException {
        return vehiculeDAO.findById(id);
    }

    public int creerVehicule(String reference, Integer nombrePlaces, Integer typeCarburantId) throws SQLException {
        validateForCreate(reference, nombrePlaces, typeCarburantId);

        Vehicule vehicule = new Vehicule();
        vehicule.setReference(reference.trim());
        vehicule.setNombrePlaces(nombrePlaces);
        vehicule.setTypeCarburantId(typeCarburantId);

        return vehiculeDAO.insert(vehicule);
    }

    public void modifierVehicule(Integer id, String reference, Integer nombrePlaces, Integer typeCarburantId)
            throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant du véhicule est obligatoire.");
        }

        Vehicule existing = vehiculeDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Le véhicule à modifier n'existe pas.");
        }

        validateForUpdate(id, reference, nombrePlaces, typeCarburantId);

        Vehicule vehicule = new Vehicule();
        vehicule.setId(id);
        vehicule.setReference(reference.trim());
        vehicule.setNombrePlaces(nombrePlaces);
        vehicule.setTypeCarburantId(typeCarburantId);

        boolean updated = vehiculeDAO.update(vehicule);
        if (!updated) {
            throw new IllegalArgumentException("Impossible de modifier le véhicule.");
        }
    }

    public void supprimerVehicule(Integer id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant du véhicule est obligatoire.");
        }

        Vehicule existing = vehiculeDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Le véhicule à supprimer n'existe pas.");
        }

        boolean deleted = vehiculeDAO.delete(id);
        if (!deleted) {
            throw new IllegalArgumentException("Impossible de supprimer le véhicule.");
        }
    }

    private void validateForCreate(String reference, Integer nombrePlaces, Integer typeCarburantId)
            throws SQLException {
        String normalizedReference = validateReference(reference);

        if (vehiculeDAO.existsByReference(normalizedReference)) {
            throw new IllegalArgumentException("La référence du véhicule existe déjà.");
        }

        validateNombrePlaces(nombrePlaces);
        validateTypeCarburant(typeCarburantId);
    }

    private void validateForUpdate(Integer id, String reference, Integer nombrePlaces, Integer typeCarburantId)
            throws SQLException {
        String normalizedReference = validateReference(reference);

        if (vehiculeDAO.existsByReferenceExcludingId(normalizedReference, id)) {
            throw new IllegalArgumentException("La référence du véhicule existe déjà.");
        }

        validateNombrePlaces(nombrePlaces);
        validateTypeCarburant(typeCarburantId);
    }

    private String validateReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("La référence du véhicule est obligatoire.");
        }

        String normalized = reference.trim();
        if (normalized.length() < 2 || normalized.length() > 50) {
            throw new IllegalArgumentException("La référence doit contenir entre 2 et 50 caractères.");
        }

        return normalized;
    }

    private void validateNombrePlaces(Integer nombrePlaces) {
        if (nombrePlaces == null) {
            throw new IllegalArgumentException("Le nombre de places est obligatoire.");
        }

        if (nombrePlaces <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être supérieur à 0.");
        }
    }

    private void validateTypeCarburant(Integer typeCarburantId) throws SQLException {
        if (typeCarburantId == null) {
            throw new IllegalArgumentException("Le type de carburant est obligatoire.");
        }

        if (!vehiculeDAO.existsTypeCarburant(typeCarburantId)) {
            throw new IllegalArgumentException("Le type de carburant sélectionné n'existe pas.");
        }
    }
}
