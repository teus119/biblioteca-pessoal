package com.biblioteca.unit;

import com.biblioteca.MongoTestBase;
import com.biblioteca.dto.AuthDTO;
import com.biblioteca.exception.DuplicateResourceException;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthService — Testes Unitários (Caixa Branca)")
class AuthServiceTest extends MongoTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("register — deve criar usuário e retornar JWT válido")
    void register_validRequest_returnsJwt() {
        AuthDTO.RegisterRequest req = AuthDTO.RegisterRequest.builder()
                .username("joao")
                .email("joao@email.com")
                .password("senha123")
                .build();

        AuthDTO.AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("joao");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");
        assertThat(response.getUserId()).isNotBlank();
        assertThat(userRepository.existsByEmail("joao@email.com")).isTrue();
    }

    @Test
    @DisplayName("register — deve lançar DuplicateResourceException para email duplicado")
    void register_duplicateEmail_throwsException() {
        AuthDTO.RegisterRequest req = AuthDTO.RegisterRequest.builder()
                .username("maria1")
                .email("dup@email.com")
                .password("senha123")
                .build();
        authService.register(req);

        AuthDTO.RegisterRequest dup = AuthDTO.RegisterRequest.builder()
                .username("maria2")
                .email("dup@email.com")
                .password("outrasenha")
                .build();

        assertThatThrownBy(() -> authService.register(dup))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email já cadastrado");
    }

    @Test
    @DisplayName("register — deve lançar DuplicateResourceException para username duplicado")
    void register_duplicateUsername_throwsException() {
        authService.register(AuthDTO.RegisterRequest.builder()
                .username("mesmo_user")
                .email("a@email.com")
                .password("senha123")
                .build());

        assertThatThrownBy(() -> authService.register(
                AuthDTO.RegisterRequest.builder()
                        .username("mesmo_user")
                        .email("b@email.com")
                        .password("senha123")
                        .build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username já em uso");
    }

    @Test
    @DisplayName("login — deve autenticar com credenciais corretas e retornar JWT")
    void login_validCredentials_returnsJwt() {
        authService.register(AuthDTO.RegisterRequest.builder()
                .username("ana")
                .email("ana@email.com")
                .password("senha123")
                .build());

        AuthDTO.AuthResponse response = authService.login(
                AuthDTO.LoginRequest.builder()
                        .email("ana@email.com")
                        .password("senha123")
                        .build());

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("ana@email.com");
    }

    @Test
    @DisplayName("register — senha deve ser armazenada como hash BCrypt")
    void register_password_isStoredAsHash() {
        authService.register(AuthDTO.RegisterRequest.builder()
                .username("carlos")
                .email("carlos@email.com")
                .password("minhasenha")
                .build());

        String stored = userRepository.findByEmail("carlos@email.com")
                .orElseThrow()
                .getPassword();

        assertThat(stored).startsWith("$2a$");
        assertThat(stored).isNotEqualTo("minhasenha");
    }

    @ParameterizedTest
    @DisplayName("register — deve atribuir role USER por padrão")
    @ValueSource(strings = {"user1@x.com", "user2@x.com", "user3@x.com"})
    void register_defaultRole_isUser(String email) {
        String username = email.replace("@x.com", "");
        authService.register(AuthDTO.RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("senha123")
                .build());

        String role = userRepository.findByEmail(email).orElseThrow().getRole();
        assertThat(role).isEqualTo("USER");
    }
}
