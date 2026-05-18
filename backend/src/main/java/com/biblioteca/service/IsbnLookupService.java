package com.biblioteca.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Serviço para buscar metadados de livros por ISBN via Open Library API.
 * Usado para satisfazer o requisito de VCR (WireMock) do projeto.
 * Endpoint: GET /api/books/isbn/{isbn}
 */
@Service
@RequiredArgsConstructor
public class IsbnLookupService {

    private final RestTemplate restTemplate;

    public static final String OPEN_LIBRARY_URL =
            "https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data";

    /**
     * Busca metadados de um livro pelo ISBN na Open Library API.
     * @param isbn ISBN do livro (10 ou 13 dígitos)
     * @return Map com dados do livro, ou Optional.empty() se não encontrado
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> lookupByIsbn(String isbn) {
        try {
            String url = OPEN_LIBRARY_URL.replace("{isbn}", isbn);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }

            String key = "ISBN:" + isbn;
            Object bookData = response.get(key);

            if (bookData instanceof Map<?, ?> map) {
                return Optional.of((Map<String, Object>) map);
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
