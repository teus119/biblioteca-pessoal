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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BookController — Testes E2E (Caixa Preta)")
class BookControllerTest extends MongoTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        bookRepository.deleteAll();
        userRepository.deleteAll();
        token = registerAndGetToken("testuser", "test@email.com", "senha123");
    }

    // =========================================================
    // POST /api/books
    // =========================================================

    @Test
    @DisplayName("POST /api/books — 201 ao criar livro válido")
    void createBook_valid_returns201() throws Exception {
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookDTO.CreateRequest.builder()
                                        .title("1984")
                                        .author("George Orwell")
                                        .status(Book.ReadingStatus.WISHLIST)
                                        .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("1984"))
                .andExpect(jsonPath("$.author").value("George Orwell"))
                .andExpect(jsonPath("$.status").value("WISHLIST"));
    }

    @Test
    @DisplayName("POST /api/books — 401 sem token de autenticação")
    void createBook_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Livro\",\"author\":\"Autor\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @DisplayName("POST /api/books — 422 para campos obrigatórios ausentes")
    @MethodSource("invalidBookProvider")
    void createBook_missingFields_returns422(String body) throws Exception {
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    static Stream<Arguments> invalidBookProvider() {
        return Stream.of(
                Arguments.of("{\"author\":\"Autor\"}"),      // título ausente
                Arguments.of("{\"title\":\"Livro\"}"),       // autor ausente
                Arguments.of("{}")                            // tudo ausente
        );
    }

    // =========================================================
    // GET /api/books
    // =========================================================

    @Test
    @DisplayName("GET /api/books — 200 e lista vazia para novo usuário")
    void listBooks_newUser_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/books — deve retornar somente livros do usuário autenticado")
    void listBooks_returnsOnlyUserBooks() throws Exception {
        // Criar livro para user principal
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        BookDTO.CreateRequest.builder().title("Meu Livro").author("A").build())));

        // Criar livro para outro usuário
        String otherToken = registerAndGetToken("outro", "outro@email.com", "senha123");
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        BookDTO.CreateRequest.builder().title("Livro Outro").author("B").build())));

        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Meu Livro"));
    }

    // =========================================================
    // GET /api/books/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/books/{id} — 404 para ID inexistente")
    void getBook_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/books/000000000000000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // PUT /api/books/{id}
    // =========================================================

    @Test
    @DisplayName("PUT /api/books/{id} — 200 ao atualizar livro existente")
    void updateBook_valid_returns200() throws Exception {
        String bookId = createBookAndGetId("Livro Original", "Autor");

        mockMvc.perform(put("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Livro Atualizado\",\"status\":\"READING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Livro Atualizado"))
                .andExpect(jsonPath("$.status").value("READING"));
    }

    // =========================================================
    // DELETE /api/books/{id}
    // =========================================================

    @Test
    @DisplayName("DELETE /api/books/{id} — 204 ao deletar livro existente")
    void deleteBook_existing_returns204() throws Exception {
        String bookId = createBookAndGetId("Para Deletar", "Autor");

        mockMvc.perform(delete("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verificar que foi removido
        mockMvc.perform(get("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} — 404 para livro de outro usuário")
    void deleteBook_otherUser_returns404() throws Exception {
        String otherToken = registerAndGetToken("hacker", "hacker@email.com", "senha123");
        String bookId = createBookAndGetId("Livro Seguro", "Autor");

        mockMvc.perform(delete("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // GET /api/books/stats
    // =========================================================

    @Test
    @DisplayName("GET /api/books/stats — deve retornar estatísticas corretas")
    void getStats_returnsCorrectData() throws Exception {
        createBookWithStatus(Book.ReadingStatus.READ);
        createBookWithStatus(Book.ReadingStatus.READING);
        createBookWithStatus(Book.ReadingStatus.WISHLIST);

        mockMvc.perform(get("/api/books/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.read").value(1))
                .andExpect(jsonPath("$.reading").value(1))
                .andExpect(jsonPath("$.wishlist").value(1));
    }

    // =========================================================
    // GET /api/books/search
    // =========================================================

    @Test
    @DisplayName("GET /api/books/search — deve retornar resultados filtrados")
    void search_returnsFilteredBooks() throws Exception {
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        BookDTO.CreateRequest.builder().title("Dom Casmurro").author("Machado").build())));

        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        BookDTO.CreateRequest.builder().title("Grande Sertão").author("Guimarães Rosa").build())));

        mockMvc.perform(get("/api/books/search?q=dom")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Dom Casmurro"));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String registerAndGetToken(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AuthDTO.RegisterRequest.builder()
                                        .username(username).email(email).password(password).build())))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                AuthDTO.AuthResponse.class).getToken();
    }

    private String createBookAndGetId(String title, String author) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookDTO.CreateRequest.builder().title(title).author(author).build())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createBookWithStatus(Book.ReadingStatus status) throws Exception {
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        BookDTO.CreateRequest.builder()
                                .title("Livro " + status)
                                .author("Autor")
                                .status(status)
                                .build())));
    }
}
