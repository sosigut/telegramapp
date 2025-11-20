package com.example.controller;

import com.example.model.Transaction;
import com.example.repository.TransactionRepositury;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
