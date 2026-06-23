package br.com.starter.domain.quiz;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlternativeRepository extends JpaRepository<Alternative, UUID> {
}
