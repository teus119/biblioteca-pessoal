package com.biblioteca.unit;

import com.biblioteca.MongoTestBase;
import com.biblioteca.dto.BookDTO;
import com.biblioteca.exception.ResourceNotFoundException;
import com.biblioteca.model.Book;
import com.biblioteca.repository.BookRepository;
import com.biblioteca.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BookService — Testes Unitários (Caixa Branca)")
class BookServiceTest extends MongoTestBase {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    private static final String USER_ID = "user-001";

    @BeforeEach
    void cleanUp() {
        bookRepository.deleteAll();
    }

    // =========================================================
    // CAIXA BRANCA: Lógica interna de create
    // =========================================================

    @Test
    @DisplayName("create — deve salvar livro com status WISHLIST por padrão")
    void create_defaultStatus_isWishlist() {
        BookDTO.CreateRequest req = BookDTO.CreateRequest.builder()
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .build();

        BookDTO.Response result = bookService.create(req, USER_ID);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Book.ReadingStatus.WISHLIST);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @ParameterizedTest
    @DisplayName("create — deve aceitar todos os status de leitura")
    @EnumSource(Book.ReadingStatus.class)
    void create_allStatuses(Book.ReadingStatus status) {
        BookDTO.CreateRequest req = BookDTO.CreateRequest.builder()
                .title("Livro Teste")
                .author("Autor")
                .status(status)
                .build();

        BookDTO.Response result = bookService.create(req, USER_ID);

        assertThat(result.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @DisplayName("create — deve aceitar ratings válidos de 1 a 5")
    @CsvSource({"1", "2", "3", "4", "5"})
    void create_validRatings(int rating) {
        BookDTO.CreateRequest req = BookDTO.CreateRequest.builder()
                .title("Livro " + rating)
                .author("Autor")
                .rating(rating)
                .build();

        BookDTO.Response result = bookService.create(req, USER_ID);

        assertThat(result.getRating()).isEqualTo(rating);
    }

    // =========================================================
    // CAIXA BRANCA: Lógica interna de update (patch parcial)
    // =========================================================

    @Test
    @DisplayName("update — deve atualizar somente campos não-nulos (patch parcial)")
    void update_partialPatch_onlyNonNullFields() {
        BookDTO.Response created = bookService.create(
                BookDTO.CreateRequest.builder()
                        .title("Título Original")
                        .author("Autor Original")
                        .status(Book.ReadingStatus.WISHLIST)
                        .build(), USER_ID);

        BookDTO.UpdateRequest patch = BookDTO.UpdateRequest.builder()
                .status(Book.ReadingStatus.READING)
                .build();

        BookDTO.Response updated = bookService.update(created.getId(), patch, USER_ID);

        assertThat(updated.getTitle()).isEqualTo("Título Original");
        assertThat(updated.getAuthor()).isEqualTo("Autor Original");
        assertThat(updated.getStatus()).isEqualTo(Book.ReadingStatus.READING);
    }

    @Test
    @DisplayName("update — deve atualizar todos os campos quando fornecidos")
    void update_allFields() {
        BookDTO.Response created = bookService.create(
                BookDTO.CreateRequest.builder()
                        .title("Original")
                        .author("Original")
                        .isbn("123")
                        .genre("Original")
                        .status(Book.ReadingStatus.WISHLIST)
                        .rating(3)
                        .notes("Original")
                        .coverUrl("Original")
                        .build(), USER_ID);

        BookDTO.UpdateRequest patch = BookDTO.UpdateRequest.builder()
                .title("Novo Título")
                .author("Novo Autor")
                .isbn("456")
                .genre("Novo Gênero")
                .status(Book.ReadingStatus.READ)
                .rating(5)
                .notes("Novas Notas")
                .coverUrl("Nova Capa")
                .build();

        BookDTO.Response updated = bookService.update(created.getId(), patch, USER_ID);

        assertThat(updated.getTitle()).isEqualTo("Novo Título");
        assertThat(updated.getAuthor()).isEqualTo("Novo Autor");
        assertThat(updated.getIsbn()).isEqualTo("456");
        assertThat(updated.getGenre()).isEqualTo("Novo Gênero");
        assertThat(updated.getStatus()).isEqualTo(Book.ReadingStatus.READ);
        assertThat(updated.getRating()).isEqualTo(5);
        assertThat(updated.getNotes()).isEqualTo("Novas Notas");
        assertThat(updated.getCoverUrl()).isEqualTo("Nova Capa");
    }

    // =========================================================
    // CAIXA BRANCA: Lógica de getStats
    // =========================================================

    @Test
    @DisplayName("getStats — deve calcular média de rating corretamente")
    void getStats_averageRating_correct() {
        bookRepository.saveAll(List.of(
                Book.builder().title("A").author("X").userId(USER_ID).rating(4).status(Book.ReadingStatus.READ).build(),
                Book.builder().title("B").author("X").userId(USER_ID).rating(2).status(Book.ReadingStatus.READ).build(),
                Book.builder().title("C").author("X").userId(USER_ID).status(Book.ReadingStatus.WISHLIST).build()
        ));

        BookDTO.StatsResponse stats = bookService.getStats(USER_ID);

        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getRead()).isEqualTo(2);
        assertThat(stats.getWishlist()).isEqualTo(1);
        assertThat(stats.getAverageRating()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("getStats — deve retornar média 0.0 quando nenhum livro tem rating")
    void getStats_noRatings_averageIsZero() {
        bookRepository.save(Book.builder()
                .title("Sem Rating").author("X").userId(USER_ID)
                .status(Book.ReadingStatus.WISHLIST).build());

        BookDTO.StatsResponse stats = bookService.getStats(USER_ID);

        assertThat(stats.getAverageRating()).isEqualTo(0.0);
    }

    // =========================================================
    // CAIXA BRANCA: Isolamento de usuários
    // =========================================================

    @Test
    @DisplayName("findAllByUser — deve retornar somente livros do usuário correto")
    void findAllByUser_userIsolation() {
        bookService.create(BookDTO.CreateRequest.builder().title("Meu Livro").author("A").build(), USER_ID);
        bookService.create(BookDTO.CreateRequest.builder().title("Livro Outro").author("B").build(), "outro-user");

        List<BookDTO.Response> books = bookService.findAllByUser(USER_ID);

        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Meu Livro");
    }

    @Test
    @DisplayName("delete — deve lançar ResourceNotFoundException para livro de outro usuário")
    void delete_otherUserBook_throwsException() {
        BookDTO.Response book = bookService.create(
                BookDTO.CreateRequest.builder().title("Livro").author("A").build(), USER_ID);

        assertThatThrownBy(() -> bookService.delete(book.getId(), "intruso"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(book.getId());
    }

    @Test
    @DisplayName("search — deve buscar por título e autor (case-insensitive)")
    void search_titleAndAuthor_caseInsensitive() {
        bookService.create(BookDTO.CreateRequest.builder().title("Harry Potter").author("J.K. Rowling").build(), USER_ID);
        bookService.create(BookDTO.CreateRequest.builder().title("Senhor dos Anéis").author("Tolkien").build(), USER_ID);

        List<BookDTO.Response> resultTitle = bookService.search(USER_ID, "harry");
        List<BookDTO.Response> resultAuthor = bookService.search(USER_ID, "tolkien");

        assertThat(resultTitle).hasSize(1);
        assertThat(resultTitle.get(0).getTitle()).contains("Harry");
        assertThat(resultAuthor).hasSize(1);
        assertThat(resultAuthor.get(0).getAuthor()).contains("Tolkien");
    }
}
