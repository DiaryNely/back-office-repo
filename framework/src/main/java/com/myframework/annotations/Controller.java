package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Annotation pour marquer une classe comme contrôleur
 * Seules les classes annotées avec @Controller seront scannées
 * 
 * Exemple d'utilisation:
 * @Controller
 * public class UserController {
 *     @WebRoute("/users")
 *     public void listUsers(...) { }
 * }
 */
@Retention(RetentionPolicy.RUNTIME)  // Disponible à l'exécution
@Target(ElementType.TYPE)            // On annote uniquement des classes (TYPE)
public @interface Controller {
    // Pas de paramètre pour le moment
}