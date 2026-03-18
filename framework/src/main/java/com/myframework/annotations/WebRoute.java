package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Annotation pour associer une URL à une méthode de contrôleur
 * 
 * Exemple d'utilisation:
 * @WebRoute("/hello")
 * public void sayHello(HttpServletRequest req, HttpServletResponse resp) { ... }
 */
@Retention(RetentionPolicy.RUNTIME)  // L'annotation sera disponible à l'exécution
@Target(ElementType.METHOD)          // On annote uniquement des méthodes
public @interface WebRoute {
    /**
     * L'URL associée à la méthode
     * Exemple: "/hello", "/users/list", "/api/products"
     */
    String value();
}