package com.example.controller;

import com.example.model.Transaction;
import com.example.repository.TransactionRepository;
import com.example.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AIService aiService;

    @PostMapping
    public Transaction add(@RequestBody Transaction t, @RequestHeader("X-User-Id") Long userId){
        t.setUserId(userId);
        t.setDate(LocalDateTime.now());
        return transactionRepository.save(t);
    }

    @GetMapping
    public List<Transaction> getAll(@RequestHeader("X-User-Id") Long userId){
        return transactionRepository.findByUserId(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        transactionRepository.deleteById(id);
    }

    @GetMapping("/search")
    public List<Transaction> search(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        System.out.println("Raw search params - category: '" + category + "', start: " + start + ", end: " + end);

        LocalDateTime startDate = (start != null) ? start.atStartOfDay() : null;
        LocalDateTime endDate = (end != null) ? end.atTime(23, 59, 59) : null;

        // "Все" → null
        if (category != null && category.trim().equalsIgnoreCase("Все")) {
            category = null;
        }

        List<Transaction> result = transactionRepository.search(userId ,category, startDate, endDate);
        System.out.println("Found transactions: " + result.size());
        return result;
    }

    @GetMapping("/ai-advice")
    public String getAIAdvice(@RequestHeader("X-User-Id") Long userId) {
        List<Transaction> list = transactionRepository.findByUserId(userId);
        return aiService.analyzeTransactions(list);
    }

}
