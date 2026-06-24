package br.com.starter.domain.quizattempt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.starter.domain.quiz.Quiz;
import br.com.starter.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAnswer> answers = new ArrayList<>();

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    public QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, User participant, Integer totalQuestions) {
        this.quiz = quiz;
        this.participant = participant;
        this.totalQuestions = totalQuestions;
    }

    public void complete(List<UserAnswer> answers, int score) {
        this.answers.clear();
        answers.forEach(this::addAnswer);
        this.score = score;
        this.completedAt = LocalDateTime.now();
    }

    public void addAnswer(UserAnswer answer) {
        answer.setAttempt(this);
        this.answers.add(answer);
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public User getParticipant() {
        return participant;
    }

    public List<UserAnswer> getAnswers() {
        return answers;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }
}
