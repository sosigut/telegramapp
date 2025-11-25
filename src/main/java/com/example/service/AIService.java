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

    @Value("${deepseek.apiKey}")
    private String apiKey;

    public String analyzeTransactions(List<Transaction> transactions){

        if(transactions.isEmpty()){
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
                .baseUrl("https://api.deepseek.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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

        return response.getChoices().get(0).getMessage().getContent();

    }
}
