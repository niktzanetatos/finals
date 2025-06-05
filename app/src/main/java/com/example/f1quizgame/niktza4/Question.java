package com.example.f1quizgame.niktza4;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    private String questionText;
    private String correctAnswer;
    private List<String> options;
    private String category;

    public Question() {} // Required for Firebase

    public String getQuestionText() {
        return questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCategory() {
        return category;
    }

    // Setters also if needed
}
