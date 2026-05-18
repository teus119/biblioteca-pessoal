package com.biblioteca.dto;

import com.biblioteca.model.Book;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class BookDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Título é obrigatório")
        private String title;

        @NotBlank(message = "Autor é obrigatório")
        private String author;

        private String isbn;
        private String genre;

        @Builder.Default
        private Book.ReadingStatus status = Book.ReadingStatus.WISHLIST;

        @Min(value = 1, message = "Avaliação mínima é 1")
        @Max(value = 5, message = "Avaliação máxima é 5")
        private Integer rating;

        private String notes;
        private String coverUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String author;
        private String isbn;
        private String genre;
        private Book.ReadingStatus status;

        @Min(value = 1, message = "Avaliação mínima é 1")
        @Max(value = 5, message = "Avaliação máxima é 5")
        private Integer rating;

        private String notes;
        private String coverUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String title;
        private String author;
        private String isbn;
        private String genre;
        private Book.ReadingStatus status;
        private Integer rating;
        private String notes;
        private String coverUrl;
        private String userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsResponse {
        private long total;
        private long reading;
        private long read;
        private long wishlist;
        private double averageRating;
    }
}
