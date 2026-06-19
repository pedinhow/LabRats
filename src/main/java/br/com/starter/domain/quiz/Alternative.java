package br.com.starter.domain.quiz;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alternative {
    @JsonAlias({ "texto", "option", "alternative" })
    private String text;

    @JsonAlias({ "correct", "correta", "is_correct" })
    private Boolean isCorrect;

    public Alternative() {
    }

    public Alternative(String text, Boolean isCorrect) {
        this.text = text;
        this.isCorrect = isCorrect;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
