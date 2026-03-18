package com.myframework.annotations;

import java.lang.annotation.*;

/**
 * Sprint 11 bis - Annotation pour sécuriser l'accès à une méthode
 * 
 * Exemples:
 * 
 *   // Accessible uniquement si connecté (session contient "user")
 *   @Auth
 *   @GetMapping("/profile")
 *   public ModelView profile() { ... }
 * 
 *   // Accessible uniquement avec le rôle "admin"
 *   @Auth(roles = {"admin"})
 *   @GetMapping("/admin/dashboard")
 *   public ModelView adminDashboard() { ... }
 * 
 *   // Accessible avec rôle "admin" OU "moderator"
 *   @Auth(roles = {"admin", "moderator"})
 *   @PostMapping("/delete-post")
 *   public ModelView deletePost() { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auth {
    
    /**
     * Nom de l'attribut session contenant l'utilisateur connecté
     * Par défaut: "user"
     */
    String sessionKey() default "user";
    
    /**
     * Nom de l'attribut session contenant le rôle de l'utilisateur
     * Par défaut: "userRole"
     */
    String roleKey() default "userRole";
    
    /**
     * Rôles autorisés à accéder à cette méthode
     * Si vide, seule l'authentification est requise (pas de rôle spécifique)
     */
    String[] roles() default {};
}
