package br.com.starter.domain.quizattempt;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    Optional<QuizAttempt> findByIdAndParticipantId(UUID id, UUID participantId);
}
