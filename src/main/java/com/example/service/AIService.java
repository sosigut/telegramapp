package com.example.service;

import com.example.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${deepseek.apiKey:sk-o9vS81Woh0uCL73JjpKWLg}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeTransactions(List<Transaction> transactions) {
        try {
            // ДЕБАГ ЛОГИ
            System.out.println("=== AI SERVICE DEBUG ===");
            System.out.println("API Key present: " + (apiKey != null && !apiKey.isEmpty()));
            System.out.println("Transactions count: " + transactions.size());

            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("API Key is empty, using fallback");
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

            System.out.println("Sending request to DeepSeek API...");

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

            System.out.println("URL: " + url);
            System.out.println("Headers: " + headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + (response.getBody() != null ? response.getBody().substring(0, Math.min(200, response.getBody().length())) + "..." : "NULL"));

            if (response.getStatusCode() == HttpStatus.OK) {
                // Парсим ответ
                Map<String, Object> responseMap = mapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> messageResponse = (Map<String, String>) firstChoice.get("message");
                    String content = messageResponse.get("content");

                    System.out.println("AI Response successful: " + content.substring(0, Math.min(100, content.length())) + "...");
                    return content;
                } else {
                    System.out.println("No choices in response");
                }
            } else {
                System.out.println("HTTP Error: " + response.getStatusCode());
            }

            return generateFallbackAdvice(transactions);

        } catch (Exception e) {
            System.out.println("AI ERROR: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackAdvice(transactions);
        }
    }

    private String generateFallbackAdvice(List<Transaction> transactions) {
        System.out.println("Using fallback advice");

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

        double balance = income - expense;

        // Улучшенный анализ по категориям
        Map<String, Double> expenseByCategory = transactions.stream()
                .filter(t -> "Expense".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        StringBuilder analysis = new StringBuilder();
        analysis.append("🤖 *Финансовый анализ*\n\n");

        analysis.append("💰 **Баланс:**\n");
        analysis.append(String.format("• Доходы: %.2f ₽\n", income));
        analysis.append(String.format("• Расходы: %.2f ₽\n", expense));
        analysis.append(String.format("• Итого: %.2f ₽\n\n", balance));

        if (!expenseByCategory.isEmpty()) {
            analysis.append("📊 **Расходы по категориям:**\n");
            expenseByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        double percentage = expense > 0 ? (entry.getValue() / expense) * 100 : 0;
                        analysis.append(String.format("• %s: %.2f ₽ (%.1f%%)\n",
                                entry.getKey(), entry.getValue(), percentage));
                    });
            analysis.append("\n");
        }

        // Умные рекомендации
        analysis.append("💡 **Рекомендации:**\n");

        if (balance < 0) {
            analysis.append("⚠️  ВНИМАНИЕ: Расходы превышают доходы!\n");
            analysis.append("• Срочно сократите траты\n");
            analysis.append("• Пересмотрите бюджет\n");
        } else if (expense > income * 0.7) {
            analysis.append("📝 Высокий уровень расходов\n");
            analysis.append("• Оптимизируйте основные категории трат\n");
            analysis.append("• Создайте финансовую подушку\n");
        } else {
            analysis.append("✅ Отличный финансовый контроль!\n");
            analysis.append("• Продолжайте отслеживать расходы\n");
            analysis.append("• Рассмотрите возможность инвестиций\n");
        }

        return analysis.toString();
    }
}