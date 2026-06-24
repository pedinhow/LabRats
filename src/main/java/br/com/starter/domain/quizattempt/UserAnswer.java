package br.com.starter.domain.quizattempt;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_answers")
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "question_statement", nullable = false, columnDefinition = "TEXT")
    private String questionStatement;

    @Column(name = "selected_alternative_id")
    private UUID selectedAlternativeId;

    @Column(name = "selected_alternative_text", columnDefinition = "TEXT")
    private String selectedAlternativeText;

    @Column(name = "correct_alternative_id", nullable = false)
    private UUID correctAlternativeId;

    @Column(name = "correct_alternative_text", nullable = false, columnDefinition = "TEXT")
    private String correctAlternativeText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    public UserAnswer() {
    }

    public UserAnswer(
            UUID questionId,
            String questionStatement,
            UUID selectedAlternativeId,
            String selectedAlternativeText,
            UUID correctAlternativeId,
            String correctAlternativeText,
            boolean correct
    ) {
        this.questionId = questionId;
        this.questionStatement = questionStatement;
        this.selectedAlternativeId = selectedAlternativeId;
        this.selectedAlternativeText = selectedAlternativeText;
        this.correctAlternativeId = correctAlternativeId;
        this.correctAlternativeText = correctAlternativeText;
        this.correct = correct;
    }

    public void setAttempt(QuizAttempt attempt) {
        this.attempt = attempt;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public String getQuestionStatement() {
        return questionStatement;
    }

    public UUID getSelectedAlternativeId() {
        return selectedAlternativeId;
    }

    public String getSelectedAlternativeText() {
        return selectedAlternativeText;
    }

    public UUID getCorrectAlternativeId() {
        return correctAlternativeId;
    }

    public String getCorrectAlternativeText() {
        return correctAlternativeText;
    }

    public boolean isCorrect() {
        return correct;
    }
}
