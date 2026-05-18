package com.biblioteca.integration;

import com.biblioteca.MongoTestBase;
import com.biblioteca.model.Book;
import com.biblioteca.model.User;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BookRepository — Testes de Integração")
class BookRepositoryIntegrationTest extends MongoTestBase {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String USER_A = "userA";
    private static final String USER_B = "userB";

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("findByUserId — deve retornar apenas os livros do usuário especificado")
    void findByUserId_returnsOnlyUserBooks() {
        bookRepository.saveAll(List.of(
                buildBook("Livro A1", USER_A, Book.ReadingStatus.READING),
                buildBook("Livro A2", USER_A, Book.ReadingStatus.READ),
                buildBook("Livro B1", USER_B, Book.ReadingStatus.WISHLIST)
        ));

        List<Book> booksA = bookRepository.findByUserId(USER_A);
        List<Book> booksB = bookRepository.findByUserId(USER_B);

        assertThat(booksA).hasSize(2).allMatch(b -> b.getUserId().equals(USER_A));
        assertThat(booksB).hasSize(1).allMatch(b -> b.getUserId().equals(USER_B));
    }

    @Test
    @DisplayName("findByIdAndUserId — deve retornar livro quando id e userId coincidem")
    void findByIdAndUserId_matchingOwner_returnsBook() {
        Book saved = bookRepository.save(buildBook("Meu Livro", USER_A, Book.ReadingStatus.READ));

        Optional<Book> result = bookRepository.findByIdAndUserId(saved.getId(), USER_A);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Meu Livro");
    }

    @Test
    @DisplayName("findByIdAndUserId — deve retornar vazio quando userId não corresponde")
    void findByIdAndUserId_wrongOwner_returnsEmpty() {
        Book saved = bookRepository.save(buildBook("Livro Alheio", USER_A, Book.ReadingStatus.WISHLIST));

        Optional<Book> result = bookRepository.findByIdAndUserId(saved.getId(), USER_B);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("findByUserIdAndStatus — deve filtrar livros por status")
    @MethodSource("statusProvider")
    void findByUserIdAndStatus_filters(Book.ReadingStatus status, int expected) {
        bookRepository.saveAll(List.of(
                buildBook("L1", USER_A, Book.ReadingStatus.READING),
                buildBook("L2", USER_A, Book.ReadingStatus.READING),
                buildBook("L3", USER_A, Book.ReadingStatus.READ),
                buildBook("L4", USER_A, Book.ReadingStatus.WISHLIST)
        ));

        List<Book> books = bookRepository.findByUserIdAndStatus(USER_A, status);

        assertThat(books).hasSize(expected);
        assertThat(books).allMatch(b -> b.getStatus() == status);
    }

    static Stream<Arguments> statusProvider() {
        return Stream.of(
                Arguments.of(Book.ReadingStatus.READING, 2),
                Arguments.of(Book.ReadingStatus.READ, 1),
                Arguments.of(Book.ReadingStatus.WISHLIST, 1)
        );
    }

    @ParameterizedTest
    @DisplayName("searchByUserIdAndQuery — deve buscar por título e autor (case-insensitive)")
    @MethodSource("searchProvider")
    void searchByUserIdAndQuery_findsResults(String query, int expectedCount) {
        bookRepository.saveAll(List.of(
                buildBook("Harry Potter e a Pedra Filosofal", USER_A, Book.ReadingStatus.READ),
                buildBook("Harry Potter e a Câmara Secreta", USER_A, Book.ReadingStatus.READ),
                buildBook("O Senhor dos Anéis", USER_A, Book.ReadingStatus.WISHLIST)
        ));

        List<Book> results = bookRepository.searchByUserIdAndQuery(USER_A, query);

        assertThat(results).hasSize(expectedCount);
    }

    static Stream<Arguments> searchProvider() {
        return Stream.of(
                Arguments.of("harry", 2),
                Arguments.of("POTTER", 2),
                Arguments.of("senhor", 1),
                Arguments.of("xyz_nao_existe", 0)
        );
    }

    @Test
    @DisplayName("countByUserIdAndStatus — deve contar corretamente por status")
    void countByUserIdAndStatus_correctCounts() {
        bookRepository.saveAll(List.of(
                buildBook("A", USER_A, Book.ReadingStatus.READING),
                buildBook("B", USER_A, Book.ReadingStatus.READING),
                buildBook("C", USER_A, Book.ReadingStatus.READ)
        ));

        assertThat(bookRepository.countByUserIdAndStatus(USER_A, Book.ReadingStatus.READING)).isEqualTo(2);
        assertThat(bookRepository.countByUserIdAndStatus(USER_A, Book.ReadingStatus.READ)).isEqualTo(1);
        assertThat(bookRepository.countByUserIdAndStatus(USER_A, Book.ReadingStatus.WISHLIST)).isEqualTo(0);
    }

    private Book buildBook(String title, String userId, Book.ReadingStatus status) {
        return Book.builder()
                .title(title)
                .author("Autor Teste")
                .userId(userId)
                .status(status)
                .build();
    }
}
