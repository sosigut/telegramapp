package com.example.service;

import com.example.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${deepseek.apiKey:sk-o9vS81Woh0uCL73JjpKWLg}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeTransactions(List<Transaction> transactions) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return generateFallbackAdvice(transactions);
            }

            if (transactions.isEmpty()) {
                return "У вас пока что нет никаких транзакций.";
            }

            // Генерация сводки транзакций
            StringBuilder summary = new StringBuilder();
            transactions.forEach(transaction -> summary.append(
                    String.format("%s - %s: %.2f (%s)\n",
                            transaction.getDate().toLocalDate(),
                            transaction.getCategory(),
                            transaction.getAmount(),
                            transaction.getType())
            ));

            String prompt = """
                Ты — профессиональный финансовый консультант.
                
                Вот операции пользователя:

                %s

                Требуется:
                - краткий обзор трат
                - проблемные зоны
                - рекомендации
                Дай коротко, структурированно, без воды.
                """.formatted(summary.toString());

            // Создаем запрос вручную
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            // Создаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Отправляем запрос
            String url = "https://api.artemox.com/v1/chat/completions";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // Парсим ответ
                Map<String, Object> responseMap = mapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> messageResponse = (Map<String, String>) firstChoice.get("message");
                    return messageResponse.get("content");
                }
            }

            return generateFallbackAdvice(transactions);

        } catch (Exception e) {
            System.out.println("AI ERROR: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackAdvice(transactions);
        }
    }

    private String generateFallbackAdvice(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "🤖 У вас пока нет транзакций.";
        }

        double income = transactions.stream()
                .filter(t -> "Income".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = transactions.stream()
                .filter(t -> "Expense".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        return """
                🤖 *Анализ ваших финансов (без AI)*

                📈 Доходы: %.2f ₽
                📉 Расходы: %.2f ₽
                ⚖️ Баланс: %.2f ₽

                💡 Базовые советы:
                • Ведите бюджет
                • Контролируйте категории расходов
                • Храните подушку безопасности
                """.formatted(income, expense, income - expense);
    }
}