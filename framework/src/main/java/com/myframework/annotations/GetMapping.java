package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Annotation pour mapper une methode a une requete HTTP GET
 * 
 * GET = Recuperation de donnees (affichage)
 * 
 * Exemple:
 * @GetMapping("/users")
 * public ModelView listUsers() {
 *     // Afficher la liste des utilisateurs
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetMapping {
    /**
     * L'URL de la route
     */
    String value();
}