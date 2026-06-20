package br.com.starter.domain.quiz;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {
    @JsonAlias({ "statement", "question", "pergunta", "enunciado" })
    private String statement;

    @JsonAlias({ "options", "alternativas" })
    private List<Alternative> alternatives;

    public Question() {
    }

    public Question(String statement, List<Alternative> alternatives) {
        this.statement = statement;
        this.alternatives = alternatives;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<Alternative> alternatives) {
        this.alternatives = alternatives;
    }
}
