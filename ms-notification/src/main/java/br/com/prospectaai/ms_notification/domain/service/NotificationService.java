package br.com.prospectaai.ms_notification.domain.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import br.com.prospectaai.ms_notification.domain.entity.NotificationEntity;
import br.com.prospectaai.ms_notification.domain.repository.NotificationRepository;
import br.com.prospectaai.shared.dto.notification.GenericNotification;
import br.com.prospectaai.shared.kafka.KafkaMessageTopic;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    
    public NotificationEntity persist(KafkaMessageTopic<?> message, String link, boolean isRead) {
        return persist(
            GenericNotification.builder()
                .userEmail(message.getUserEmail())
                .title(message.getTitle())
                .content(message.getDescription())
                .datetime(message.getTimestamp() != null ? Instant.ofEpochMilli(message.getTimestamp()) : Instant.now())
                .link(link)
                .read(isRead)
                .build()
        );
    }
    public NotificationEntity persist(GenericNotification dto) {
        NotificationEntity n = new NotificationEntity();
        n.setUserEmail(dto.getUserEmail());
        n.setTitle(dto.getTitle());
        n.setDatetime(dto.getDatetime() != null ? dto.getDatetime() : Instant.now());
        n.setIcon(dto.getIcon());
        n.setContent(dto.getContent());
        n.setLink(dto.getLink());
        n.setRead(dto.isRead());
        return repository.save(n);
    }

    public Page<NotificationEntity> listByUser(String userEmail, Pageable pageable) {
        return repository.findByUserEmailOrderByDatetimeDesc(userEmail, pageable);
    }

    public long unreadCount(String userEmail) {
        return repository.countByUserEmailAndReadFalse(userEmail);
    }

    public Optional<NotificationEntity> getByIdForUser(UUID id, String userEmail) {
        if (id == null || userEmail == null || userEmail.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdAndUserEmail(id, userEmail);
    }

    public boolean markAsReadForUser(UUID id, String userEmail) {
        System.out.println("markAsReadForUser - id: " + id + " - userEmail: " + userEmail);
        var opt = getByIdForUser(id, userEmail);
        if (opt.isEmpty()) return false;
        var n = opt.get();
        if (!n.isRead()) {
            n.setRead(true);
            repository.save(n);
        }
        return true;
    }
}
