package com.biblioteca.controller;

import com.biblioteca.MongoTestBase;
import com.biblioteca.dto.AuthDTO;
import com.biblioteca.dto.BookDTO;
import com.biblioteca.model.Book;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController — Testes E2E (Caixa Preta)")
class AuthControllerTest extends MongoTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // =========================================================
    // POST /api/auth/register
    // =========================================================

    @Test
    @DisplayName("POST /api/auth/register — 201 com dados válidos")
    void register_validData_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthDTO.RegisterRequest.builder()
                                        .username("pedro")
                                        .email("pedro@email.com")
                                        .password("senha123")
                                        .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("pedro"))
                .andExpect(jsonPath("$.email").value("pedro@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register — 409 para email duplicado")
    void register_duplicateEmail_returns409() throws Exception {
        var body = objectMapper.writeValueAsString(
                AuthDTO.RegisterRequest.builder()
                        .username("user1").email("dup@email.com").password("senha123").build());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));

        var body2 = objectMapper.writeValueAsString(
                AuthDTO.RegisterRequest.builder()
                        .username("user2").email("dup@email.com").password("senha456").build());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email já cadastrado")));
    }

    @ParameterizedTest
    @DisplayName("POST /api/auth/register — 422 para campos inválidos")
    @ValueSource(strings = {
            "{\"username\":\"\",\"email\":\"a@b.com\",\"password\":\"123456\"}",
            "{\"username\":\"ok\",\"email\":\"invalido\",\"password\":\"123456\"}",
            "{\"username\":\"ok\",\"email\":\"ok@ok.com\",\"password\":\"123\"}"
    })
    void register_invalidFields_returns422(String body) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================
    // POST /api/auth/login
    // =========================================================

    @Test
    @DisplayName("POST /api/auth/login — 200 com credenciais válidas")
    void login_validCredentials_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AuthDTO.RegisterRequest.builder()
                                .username("ana").email("ana@email.com").password("senha123").build())));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthDTO.LoginRequest.builder()
                                        .email("ana@email.com").password("senha123").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/login — 401 com senha incorreta")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AuthDTO.RegisterRequest.builder()
                                .username("lucas").email("lucas@email.com").password("correta123").build())));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthDTO.LoginRequest.builder()
                                        .email("lucas@email.com").password("errada999").build())))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/auth/me (requer JWT)
    // =========================================================

    @Test
    @DisplayName("GET /api/auth/me — 200 com JWT válido")
    void getMe_validJwt_returns200() throws Exception {
        String token = registerAndGetToken("sofia", "sofia@email.com", "senha123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sofia@email.com"));
    }

    @Test
    @DisplayName("GET /api/auth/me — 403 sem JWT")
    void getMe_noJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    // Helper
    String registerAndGetToken(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthDTO.RegisterRequest.builder()
                                        .username(username).email(email).password(password).build())))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                AuthDTO.AuthResponse.class).getToken();
    }
}
