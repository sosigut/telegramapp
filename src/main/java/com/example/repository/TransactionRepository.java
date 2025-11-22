package com.example.repository;

import com.example.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;


public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    default List<Transaction> search(String category, LocalDateTime start, LocalDateTime end) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }

            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), start));
            }

            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), end));
            }

            query.orderBy(cb.desc(root.get("date")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}
