package br.com.starter.domain.quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findAllByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    Optional<Quiz> findByIdAndCreatorId(UUID id, UUID creatorId);
}
