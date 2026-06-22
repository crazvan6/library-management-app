package com.library.management.repository;

import com.library.management.entity.Book;
import com.library.management.enums.BookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<Book> findByStatus(BookStatus status);

    List<Book> findByAvailableQuantityGreaterThan(Integer quantity);

    @Query("SELECT b FROM Book b WHERE b.status = 'AVAILABLE' AND b.availableQuantity > 0")
    List<Book> findAllAvailableBooks();

    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c.categoryId = :categoryId")
    List<Book> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.status = :status")
    long countByStatus(@Param("status") BookStatus status);
}


