package com.biblioteca.service;

import com.biblioteca.dto.BookDTO;
import com.biblioteca.exception.ResourceNotFoundException;
import com.biblioteca.model.Book;
import com.biblioteca.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<BookDTO.Response> findAllByUser(String userId) {
        return bookRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookDTO.Response findByIdAndUser(String id, String userId) {
        Book book = bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com id: " + id));
        return toResponse(book);
    }

    public BookDTO.Response create(BookDTO.CreateRequest request, String userId) {
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .genre(request.getGenre())
                .status(request.getStatus() != null ? request.getStatus() : Book.ReadingStatus.WISHLIST)
                .rating(request.getRating())
                .notes(request.getNotes())
                .coverUrl(request.getCoverUrl())
                .userId(userId)
                .build();

        return toResponse(bookRepository.save(book));
    }

    public BookDTO.Response update(String id, BookDTO.UpdateRequest request, String userId) {
        Book book = bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com id: " + id));

        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getAuthor() != null) book.setAuthor(request.getAuthor());
        if (request.getIsbn() != null) book.setIsbn(request.getIsbn());
        if (request.getGenre() != null) book.setGenre(request.getGenre());
        if (request.getStatus() != null) book.setStatus(request.getStatus());
        if (request.getRating() != null) book.setRating(request.getRating());
        if (request.getNotes() != null) book.setNotes(request.getNotes());
        if (request.getCoverUrl() != null) book.setCoverUrl(request.getCoverUrl());

        return toResponse(bookRepository.save(book));
    }

    public void delete(String id, String userId) {
        if (!bookRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Livro não encontrado com id: " + id);
        }
        bookRepository.deleteById(id);
    }

    public List<BookDTO.Response> search(String userId, String query) {
        return bookRepository.searchByUserIdAndQuery(userId, query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookDTO.StatsResponse getStats(String userId) {
        List<Book> books = bookRepository.findByUserId(userId);
        long total = books.size();
        long reading = bookRepository.countByUserIdAndStatus(userId, Book.ReadingStatus.READING);
        long read = bookRepository.countByUserIdAndStatus(userId, Book.ReadingStatus.READ);
        long wishlist = bookRepository.countByUserIdAndStatus(userId, Book.ReadingStatus.WISHLIST);
        double avgRating = books.stream()
                .filter(b -> b.getRating() != null)
                .mapToInt(Book::getRating)
                .average()
                .orElse(0.0);

        return BookDTO.StatsResponse.builder()
                .total(total)
                .reading(reading)
                .read(read)
                .wishlist(wishlist)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .build();
    }

    private BookDTO.Response toResponse(Book book) {
        return BookDTO.Response.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .genre(book.getGenre())
                .status(book.getStatus())
                .rating(book.getRating())
                .notes(book.getNotes())
                .coverUrl(book.getCoverUrl())
                .userId(book.getUserId())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
