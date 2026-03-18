package com.myframework.exceptions;

/**
 * Sprint 11 bis - Exception levée quand l'utilisateur n'est pas authentifié
 */
public class UnauthorizedException extends SecurityException {
    
    private final String redirectUrl;
    
    public UnauthorizedException(String message) {
        super(message);
        this.redirectUrl = "/login";
    }
    
    public UnauthorizedException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
}
