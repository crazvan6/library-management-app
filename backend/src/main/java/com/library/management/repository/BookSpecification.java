package com.library.management.repository;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.entity.Book;
import com.library.management.entity.Category;
import com.library.management.enums.BookStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> withFilters(BookSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getTitle())) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + criteria.getTitle().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getAuthor())) {
                predicates.add(cb.like(cb.lower(root.get("author")), "%" + criteria.getAuthor().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getIsbn())) {
                predicates.add(cb.equal(root.get("isbn"), criteria.getIsbn()));
            }
            if (criteria.getCategoryId() != null) {
                Join<Book, Category> join = root.join("categories");
                predicates.add(cb.equal(join.get("categoryId"), criteria.getCategoryId()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (Boolean.TRUE.equals(criteria.getAvailableOnly())) {
                predicates.add(cb.greaterThan(root.get("availableQuantity"), 0));
                predicates.add(cb.equal(root.get("status"), BookStatus.AVAILABLE));
            }
            if (criteria.getMinYear() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publicationYear"), criteria.getMinYear()));
            }
            if (criteria.getMaxYear() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publicationYear"), criteria.getMaxYear()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}


