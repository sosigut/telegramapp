package com.example.repository;

import com.example.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t " +
            "WHERE (:category IS NULL OR LOWER(t.category) = LOWER(:category)) " +
            "AND (:start IS NULL OR t.date >= :start) " +
            "AND (:end IS NULL OR t.date <= :end)")
    List<Transaction> search(
            @Param("category") String category,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
