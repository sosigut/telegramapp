package com.example.service;

import com.example.model.DeepSeekMessage;
import com.example.model.DeepSeekRequest;
import com.example.model.DeepSeekResponse;
import com.example.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${deepseek.apiKey:sk-o9vS81Woh0uCL73JjpKMLg}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeTransactions(List<Transaction> transactions) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return generateFallbackAdvice(transactions);
            }

            if (transactions.isEmpty()) {
                return "У вас пока что нет никаких транзакций.";
            }

            // Генерация сводки
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
                """.formatted(summary);

            // ИСПРАВЛЕННЫЙ URL - используем правильный эндпоинт LiteLLM
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.artemox.com/v1/chat/completions") // Полный URL до эндпоинта
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            DeepSeekRequest req = new DeepSeekRequest(
                    "deepseek-chat", // Модель должна совпадать с той, что настроена в LiteLLM
                    new DeepSeekMessage[]{
                            new DeepSeekMessage("user", prompt)
                    }
            );

            // Получаем сырой JSON
            String raw = client.post()
                    .body(req)
                    .retrieve()
                    .body(String.class);

            System.out.println("=== RAW DEEPSEEK RESPONSE ===");
            System.out.println(raw);

            DeepSeekResponse response = mapper.readValue(raw, DeepSeekResponse.class);

            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                return generateFallbackAdvice(transactions);
            }

            return response.getChoices().get(0).getMessage().getContent();

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