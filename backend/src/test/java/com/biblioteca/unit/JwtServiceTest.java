package com.biblioteca.unit;

import com.biblioteca.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtService — Testes Unitários (Caixa Branca)")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("generateToken — deve gerar token não vazio com 3 partes (header.payload.signature)")
    void generateToken_structure_hasThreeParts() {
        String token = jwtService.generateToken("test@email.com", "user-123");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractEmail — deve extrair o email correto do token")
    void extractEmail_returnsCorrectEmail() {
        String email = "extract@email.com";
        String token = jwtService.generateToken(email, "uid-1");

        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("extractUserId — deve extrair o userId correto do token")
    void extractUserId_returnsCorrectUserId() {
        String userId = "uid-xyz-999";
        String token = jwtService.generateToken("user@email.com", userId);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("isTokenValid — deve retornar true para token válido")
    void isTokenValid_validToken_returnsTrue() {
        String email = "valid@email.com";
        String token = jwtService.generateToken(email, "uid-2");

        assertThat(jwtService.isTokenValid(token, email)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid — deve retornar false para email diferente do token")
    void isTokenValid_wrongEmail_returnsFalse() {
        String token = jwtService.generateToken("real@email.com", "uid-3");

        assertThat(jwtService.isTokenValid(token, "fake@email.com")).isFalse();
    }

    @ParameterizedTest
    @DisplayName("generateToken — deve gerar tokens únicos para usuários diferentes")
    @CsvSource({
            "user1@test.com, uid-1",
            "user2@test.com, uid-2",
            "user3@test.com, uid-3"
    })
    void generateToken_differentUsers_uniqueTokens(String email, String userId) {
        String token = jwtService.generateToken(email, userId);

        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }
}
