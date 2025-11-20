package com.example.repository;

import com.example.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepositury extends JpaRepository<Transaction, Long> {
}
