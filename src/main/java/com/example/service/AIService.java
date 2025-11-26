package com.example.service;

import com.example.model.DeepSeekMessage;
import com.example.model.DeepSeekRequest;
import com.example.model.DeepSeekResponse;
import com.example.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${deepseek.apiKey:}")
    private String apiKey;

    public String analyzeTransactions(List<Transaction> transactions) {
        try {
            // Проверка API ключа
            if (apiKey == null || apiKey.isEmpty()) {
                return generateFallbackAdvice(transactions);
            }

            System.out.println("API KEY LOADED: " + apiKey);

            if (transactions.isEmpty()) {
                return "У вас пока что нет никаких транзакций.";
            }

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

                Задача:
                1) Найти категории с наибольшими расходами.
                2) Определить, где пользователь тратит больше нормы.
                3) Оценить регулярные и нерегулярные траты.
                4) Посчитать примерные перерасходы.
                5) Дать 5–8 конкретных советов, где можно экономить.
                6) Объяснять просто и по делу.

                Дай итог в формате:
                - краткий обзор трат
                - проблемные зоны
                - рекомендации
                """.formatted(summary);

            RestClient client = RestClient.builder()
                    .baseUrl("https://api.deepseek.com/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)   // ← ДОБАВИТЬ СЮДА
                    .build();

            DeepSeekRequest req = new DeepSeekRequest(
                    "deepseek-chat",
                    new DeepSeekMessage[]{
                            new DeepSeekMessage("user", prompt)
                    }
            );

            DeepSeekResponse response = client.post()
                    .body(req)
                    .retrieve()
                    .body(DeepSeekResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return generateFallbackAdvice(transactions);
            }

            System.out.println("AI RAW RESPONSE: " + response);

            return response.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            System.out.println("AI Service error: " + e.getMessage());
            e.printStackTrace();
            return generateFallbackAdvice(transactions);
        }
    }

    private String generateFallbackAdvice(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "🤖 **Финансовый анализ**\n\n" +
                    "У вас пока нет транзакций для анализа.\n" +
                    "Добавьте несколько доходов и расходов!";
        }

        // Простой анализ без AI
        double totalIncome = transactions.stream()
                .filter(t -> "Income".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = transactions.stream()
                .filter(t -> "Expense".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = totalIncome - totalExpense;

        StringBuilder advice = new StringBuilder();
        advice.append("🤖 **Анализ ваших финансов**\n\n");
        advice.append(String.format("📈 Доходы: %.2f ₽\n", totalIncome));
        advice.append(String.format("📉 Расходы: %.2f ₽\n", totalExpense));
        advice.append(String.format("⚖️ Баланс: %.2f ₽\n\n", balance));

        if (balance > 0) {
            advice.append("✅ Вы живете по средствам!\n");
        } else {
            advice.append("⚠️ Внимание: расходы превышают доходы\n");
        }

        advice.append("\n💡 **Общие советы:**\n");
        advice.append("• Отслеживайте все траты\n");
        advice.append("• Создайте бюджет на месяц\n");
        advice.append("• Откладывайте 10-20% доходов\n");

        return advice.toString();
    }
}
