package com.example.controller;

import com.example.model.Transaction;
import com.example.repository.TransactionRepositury;
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

    private final TransactionRepositury transactionRepositury;

    @PostMapping
    public Transaction add(@RequestBody Transaction t){
        return transactionRepositury.save(t);
    }

    @GetMapping
    public List<Transaction> getAll(){
        return transactionRepositury.findAll();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        transactionRepositury.deleteById(id);
    }

    @GetMapping("/search")
    public List<Transaction> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
            ) {
        LocalDateTime startDate = start == null ? start.atStartOfDay() : null;
        LocalDateTime endDT = (end != null) ? end.atTime(23, 59, 59) : null;

        return transactionRepositury.search(category, startDate, endDT);
    }
}
