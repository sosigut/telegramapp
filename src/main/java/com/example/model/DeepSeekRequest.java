package com.example.model;

public record DeepSeekRequest(
        String model,
        DeepSeekMessage[] messages
) {}
