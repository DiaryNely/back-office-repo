package com.myframework.exceptions;

/**
 * Sprint 11 bis - Exception levée quand l'utilisateur n'a pas le bon rôle
 */
public class ForbiddenException extends SecurityException {
    
    private final String requiredRole;
    private final String userRole;
    
    public ForbiddenException(String message) {
        super(message);
        this.requiredRole = null;
        this.userRole = null;
    }
    
    public ForbiddenException(String requiredRole, String userRole) {
        super("Accès refusé. Rôle requis: " + requiredRole + ", votre rôle: " + userRole);
        this.requiredRole = requiredRole;
        this.userRole = userRole;
    }
    
    public String getRequiredRole() {
        return requiredRole;
    }
    
    public String getUserRole() {
        return userRole;
    }
}
