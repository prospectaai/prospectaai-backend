package br.com.prospectaai.ms_notification.domain.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.prospectaai.ms_notification.domain.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findByUserEmailOrderByDatetimeDesc(String userEmail, Pageable pageable);
    long countByUserEmailAndReadFalse(String userEmail);
    Optional<NotificationEntity> findByIdAndUserEmail(UUID id, String userEmail);
}
