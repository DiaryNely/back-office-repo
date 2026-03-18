package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Annotation pour mapper une methode a une requete HTTP POST
 * 
 * POST = Creation ou traitement de donnees (formulaires)
 * 
 * Exemple:
 * @PostMapping("/users")
 * public ModelView createUser(@Param("name") String name, @Param("email") String email) {
 *     // Creer un nouvel utilisateur
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostMapping {
    /**
     * L'URL de la route
     */
    String value();
}