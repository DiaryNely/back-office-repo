package com.example.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.model.TypeCarburant;
import com.example.model.Vehicule;
import com.example.service.VehiculeService;
import com.myframework.annotations.Controller;
import com.myframework.annotations.GetMapping;
import com.myframework.annotations.PostMapping;
import com.myframework.annotations.RequestParam;
import com.myframework.core.ModelView;

@Controller
public class VehiculeController {

    private final VehiculeService vehiculeService;

    public VehiculeController() {
        this.vehiculeService = new VehiculeService();
    }

    @GetMapping("/vehicules")
    public ModelView listVehicules() throws SQLException {
        return buildVehiculePage(null, null, null, null, null, null);
    }

    @GetMapping("/vehicules/edit")
    public ModelView editVehicule(@RequestParam("id") Integer id) throws SQLException {
        Vehicule vehicule = vehiculeService.getVehicule(id);

        ModelView mv = buildVehiculePage(null, null, null, null, null, null);

        if (vehicule == null) {
            mv.addData("errorMessage", "Le véhicule demandé n'existe pas.");
            return mv;
        }

        Map<String, Object> editData = new HashMap<>();
        editData.put("id", vehicule.getId());
        editData.put("reference", vehicule.getReference());
        editData.put("nombrePlaces", vehicule.getNombrePlaces());
        editData.put("typeCarburantId", vehicule.getTypeCarburantId());
        mv.addData("editData", editData);

        return mv;
    }

    @PostMapping("/vehicules/create")
    public ModelView createVehicule(
            @RequestParam("reference") String reference,
            @RequestParam("nombrePlaces") Integer nombrePlaces,
            @RequestParam("typeCarburantId") Integer typeCarburantId) throws SQLException {

        try {
            int id = vehiculeService.creerVehicule(reference, nombrePlaces, typeCarburantId);
            return buildVehiculePage("Véhicule créé avec succès (ID " + id + ").", null,
                    reference, nombrePlaces, typeCarburantId, null);
        } catch (IllegalArgumentException e) {
            return buildVehiculePage(null, e.getMessage(), reference, nombrePlaces, typeCarburantId, null);
        }
    }

    @PostMapping("/vehicules/update")
    public ModelView updateVehicule(
            @RequestParam("id") Integer id,
            @RequestParam("reference") String reference,
            @RequestParam("nombrePlaces") Integer nombrePlaces,
            @RequestParam("typeCarburantId") Integer typeCarburantId) throws SQLException {

        try {
            vehiculeService.modifierVehicule(id, reference, nombrePlaces, typeCarburantId);
            return buildVehiculePage("Véhicule modifié avec succès.", null, null, null, null, null);
        } catch (IllegalArgumentException e) {
            Map<String, Object> editData = new HashMap<>();
            editData.put("id", id);
            editData.put("reference", reference);
            editData.put("nombrePlaces", nombrePlaces);
            editData.put("typeCarburantId", typeCarburantId);
            return buildVehiculePage(null, e.getMessage(), null, null, null, editData);
        }
    }

    @PostMapping("/vehicules/delete")
    public ModelView deleteVehicule(@RequestParam("id") Integer id) throws SQLException {
        try {
            vehiculeService.supprimerVehicule(id);
            return buildVehiculePage("Véhicule supprimé avec succès.", null, null, null, null, null);
        } catch (IllegalArgumentException e) {
            return buildVehiculePage(null, e.getMessage(), null, null, null, null);
        }
    }

    private ModelView buildVehiculePage(String successMessage,
            String errorMessage,
            String formReference,
            Integer formNombrePlaces,
            Integer formTypeCarburantId,
            Map<String, Object> editData) throws SQLException {

        List<Vehicule> vehicules = vehiculeService.getVehicules();
        List<TypeCarburant> typesCarburant = vehiculeService.getTypesCarburant();

        ModelView mv = new ModelView("/vehicule-crud.jsp");
        mv.addData("vehicules", vehicules);
        mv.addData("typesCarburant", typesCarburant);

        if (successMessage != null) {
            mv.addData("successMessage", successMessage);
        }

        if (errorMessage != null) {
            mv.addData("errorMessage", errorMessage);
        }

        if (formReference != null || formNombrePlaces != null || formTypeCarburantId != null) {
            Map<String, Object> formData = new HashMap<>();
            formData.put("reference", formReference);
            formData.put("nombrePlaces", formNombrePlaces);
            formData.put("typeCarburantId", formTypeCarburantId);
            mv.addData("formData", formData);
        }

        if (editData != null) {
            mv.addData("editData", editData);
        }

        return mv;
    }
}
