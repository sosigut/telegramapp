package com.example.controller;

import com.example.model.Transaction;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @PostMapping
    public Transaction add(@RequestBody Transaction t){
        t.setDate(LocalDateTime.now());
        return transactionRepository.save(t);
    }

    @GetMapping
    public List<Transaction> getAll(){
        return transactionRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        transactionRepository.deleteById(id);
    }

    @GetMapping("/search")
    public List<Transaction> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {

        LocalDateTime startDate = (start != null) ? start.atStartOfDay() : null;
        LocalDateTime endDT = (end != null) ? end.atTime(23, 59, 59) : null;

        // "Все" → null
        if (category != null && category.trim().equalsIgnoreCase("Все")) {
            category = null;
        }

        return transactionRepository.search(category, startDate, endDT);
    }
}
