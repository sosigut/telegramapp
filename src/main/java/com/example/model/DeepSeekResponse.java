package com.example.model;

import lombok.Getter;

import java.util.List;

public class DeepSeekResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public static class Choice {
        private Message message;

        public Message getMessage() {
            return message;
        }
    }

    public static class Message {
        private String role;
        @Getter
        private String content;

    }
}

