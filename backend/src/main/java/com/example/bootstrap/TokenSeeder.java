package com.example.bootstrap;

import java.sql.SQLException;
import java.time.LocalDateTime;

import com.example.service.TokenService;

public class TokenSeeder {

    public static void main(String[] args) {
        TokenService tokenService = new TokenService();

        try {
            String validToken = tokenService.generateRandomToken();
            String expiredToken = tokenService.generateRandomToken();

            LocalDateTime validExpiration = LocalDateTime.now().plusDays(7);
            LocalDateTime expiredExpiration = LocalDateTime.now().minusDays(1);

            int validTokenId = tokenService.createToken(validToken, validExpiration);
            int expiredTokenId = tokenService.createToken(expiredToken, expiredExpiration);

            System.out.println("=== TOKENS INSERES ===");
            System.out.println(
                    "Token valide  (id=" + validTokenId + ") : " + validToken + " (expire le " + validExpiration + ")");
            System.out.println("Token expire  (id=" + expiredTokenId + ") : " + expiredToken + " (expire le "
                    + expiredExpiration + ")");
        } catch (IllegalArgumentException | SQLException e) {
            System.err.println("Erreur lors de l'insertion des tokens: " + e.getMessage());
            System.exit(1);
        }
    }
}