package com.example.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.example.dao.TokenDAO;
import com.example.model.Token;
import com.myframework.exceptions.ForbiddenException;

public class TokenService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenDAO tokenDAO;

    public TokenService() {
        this.tokenDAO = new TokenDAO();
    }

    public void verifyAccessToken(String tokenValue) throws SQLException {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new ForbiddenException("Token manquant.");
        }

        Token token = tokenDAO.findByToken(tokenValue);

        if (token == null) {
            throw new ForbiddenException("Token invalide.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (token.getDateExpiration() == null || token.getDateExpiration().isBefore(now)) {
            throw new ForbiddenException("Token expiré.");
        }
    }

    public String generateRandomToken() {
        int length = 14 + RANDOM.nextInt(3);
        StringBuilder token = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(TOKEN_ALPHABET.length());
            token.append(TOKEN_ALPHABET.charAt(index));
        }

        return token.toString();
    }

    public int createToken(String tokenValue, LocalDateTime expirationDateTime) throws SQLException {
        String normalizedToken = validateToken(tokenValue);

        if (expirationDateTime == null) {
            throw new IllegalArgumentException("La date d'expiration est obligatoire.");
        }

        return tokenDAO.insert(normalizedToken, expirationDateTime);
    }

    private String validateToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Le token est obligatoire.");
        }

        String normalized = tokenValue.trim();
        if (normalized.length() < 14 || normalized.length() > 16) {
            throw new IllegalArgumentException("Le token doit contenir entre 14 et 16 caractères.");
        }

        return normalized;
    }
}