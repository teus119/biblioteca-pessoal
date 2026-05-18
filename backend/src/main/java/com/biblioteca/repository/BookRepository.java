package com.biblioteca.repository;

import com.biblioteca.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    List<Book> findByUserId(String userId);

    Optional<Book> findByIdAndUserId(String id, String userId);

    List<Book> findByUserIdAndStatus(String userId, Book.ReadingStatus status);

    @Query("{ 'userId': ?0, $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { 'author': { $regex: ?1, $options: 'i' } } ] }")
    List<Book> searchByUserIdAndQuery(String userId, String query);

    long countByUserIdAndStatus(String userId, Book.ReadingStatus status);

    boolean existsByIdAndUserId(String id, String userId);
}
