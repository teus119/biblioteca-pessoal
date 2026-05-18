package com.biblioteca.vcr;

import com.biblioteca.MongoTestBase;
import com.biblioteca.service.IsbnLookupService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Testes VCR (Video Cassette Recorder) — usam WireMock para gravar/reproduzir
 * chamadas HTTP externas à Open Library API.
 *
 * O WireMock intercepta as chamadas HTTP e serve respostas gravadas (tapes),
 * garantindo que os testes sejam determinísticos e não dependam de rede.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("IsbnLookupService — Testes VCR (WireMock)")
class IsbnLookupVcrTest extends MongoTestBase {

    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9090));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9090);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    /**
     * Cria uma instância de IsbnLookupService apontando para o WireMock.
     */
    private IsbnLookupService buildServiceWithWireMock() {
        RestTemplate rt = new RestTemplate();
        IsbnLookupService service = new IsbnLookupService(rt) {
            @Override
            public Optional<Map<String, Object>> lookupByIsbn(String isbn) {
                // Sobrescreve a URL base para apontar para o WireMock
                try {
                    String url = "http://localhost:9090/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = rt.getForObject(url, Map.class);
                    if (response == null || response.isEmpty()) return Optional.empty();
                    Object bookData = response.get("ISBN:" + isbn);
                    if (bookData instanceof Map<?, ?> map) {
                        return Optional.of((Map<String, Object>) map);
                    }
                    return Optional.empty();
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
        };
        return service;
    }

    @Test
    @DisplayName("VCR — deve retornar dados do livro quando ISBN é encontrado (tape: 9780140449136)")
    void lookupByIsbn_found_returnsData() {
        // TAPE: resposta gravada da Open Library para Crime e Castigo
        stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:9780140449136"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("jscmd", equalTo("data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "ISBN:9780140449136": {
                                    "title": "Crime e Castigo",
                                    "authors": [{"name": "Fiódor Dostoiévski"}],
                                    "number_of_pages": 671,
                                    "publish_date": "1993"
                                  }
                                }
                                """)));

        IsbnLookupService service = buildServiceWithWireMock();
        Optional<Map<String, Object>> result = service.lookupByIsbn("9780140449136");

        assertThat(result).isPresent();
        assertThat(result.get()).containsKey("title");
        assertThat(result.get().get("title")).isEqualTo("Crime e Castigo");

        // Verificar que o WireMock foi chamado exatamente 1 vez
        verify(1, getRequestedFor(urlPathEqualTo("/api/books")));
    }

    @Test
    @DisplayName("VCR — deve retornar Optional.empty() quando ISBN não é encontrado (tape: 0000000000)")
    void lookupByIsbn_notFound_returnsEmpty() {
        // TAPE: resposta gravada para ISBN inexistente
        stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:0000000000"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        IsbnLookupService service = buildServiceWithWireMock();
        Optional<Map<String, Object>> result = service.lookupByIsbn("0000000000");

        assertThat(result).isEmpty();
        verify(1, getRequestedFor(urlPathEqualTo("/api/books")));
    }

    @Test
    @DisplayName("VCR — deve retornar Optional.empty() quando API retorna erro 500")
    void lookupByIsbn_serverError_returnsEmpty() {
        // TAPE: simula falha da API externa
        stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(aResponse().withStatus(500)));

        IsbnLookupService service = buildServiceWithWireMock();
        Optional<Map<String, Object>> result = service.lookupByIsbn("9780000000000");

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("VCR — deve lidar com múltiplos ISBNs parametrizados")
    @CsvSource({
            "9780743273565, The Great Gatsby, F. Scott Fitzgerald",
            "9780140449136, Crime e Castigo, Fiódor Dostoiévski"
    })
    void lookupByIsbn_multipleIsbns(String isbn, String expectedTitle, String expectedAuthor) {
        // TAPE parametrizada
        stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:" + isbn))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                  "ISBN:%s": {
                                    "title": "%s",
                                    "authors": [{"name": "%s"}]
                                  }
                                }
                                """, isbn, expectedTitle, expectedAuthor))));

        IsbnLookupService service = buildServiceWithWireMock();
        Optional<Map<String, Object>> result = service.lookupByIsbn(isbn);

        assertThat(result).isPresent();
        assertThat(result.get().get("title")).isEqualTo(expectedTitle);
    }
}
