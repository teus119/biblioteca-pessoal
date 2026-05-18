package com.biblioteca.controller;

import com.biblioteca.dto.BookDTO;
import com.biblioteca.model.User;
import com.biblioteca.service.BookService;
import com.biblioteca.service.IsbnLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final IsbnLookupService isbnLookupService;

    @GetMapping
    public ResponseEntity<List<BookDTO.Response>> listBooks(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookService.findAllByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO.Response> getBook(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookService.findByIdAndUser(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<BookDTO.Response> createBook(
            @Valid @RequestBody BookDTO.CreateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookService.create(request, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO.Response> updateBook(
            @PathVariable String id,
            @RequestBody BookDTO.UpdateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookService.update(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        bookService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookDTO.Response>> searchBooks(
            @RequestParam String q,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookService.search(user.getId(), q));
    }

    @GetMapping("/stats")
    public ResponseEntity<BookDTO.StatsResponse> getStats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookService.getStats(user.getId()));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<Map<String, Object>> lookupByIsbn(@PathVariable String isbn) {
        return isbnLookupService.lookupByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
