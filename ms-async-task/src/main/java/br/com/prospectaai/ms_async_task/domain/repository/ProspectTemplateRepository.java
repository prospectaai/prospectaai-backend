package br.com.prospectaai.ms_async_task.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_async_task.domain.entity.ProspectTemplate;

public interface ProspectTemplateRepository extends JpaRepository<ProspectTemplate, UUID> {
    List<ProspectTemplate> findByUserId(UUID userId);
    java.util.Optional<ProspectTemplate> findByIdAndUserId(UUID id, UUID userId);
}
