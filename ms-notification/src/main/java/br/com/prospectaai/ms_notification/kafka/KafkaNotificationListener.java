package br.com.prospectaai.ms_notification.kafka;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.prospectaai.shared.kafka.KafkaMessageTopic;
import br.com.prospectaai.shared.dto.notification.NotificationType;
import br.com.prospectaai.shared.dto.notification.AsyncTaskNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import br.com.prospectaai.ms_notification.domain.service.NotificationService;
import br.com.prospectaai.ms_notification.sse.NotificationSseService;

@Component
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final ObjectMapper objectMapper;
    private final NotificationSseService sseService;
    private final NotificationService notificationService;

    @KafkaListener(topics = "notification", groupId = "ms-notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(KafkaMessageTopic<?> message) {
        try {            
            processAsyncTaskNotifications(message);
        } catch (Exception e) {
            System.out.println("Erro ao enviar notificação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processAsyncTaskNotifications(KafkaMessageTopic<?> message) {
        String eventType = message.getEventType();
        AsyncTaskNotification payload = objectMapper.convertValue(message.getMessageData(), AsyncTaskNotification.class);
        
        if (NotificationType.ASYNC_TASK_PROCESSING_STARTED.name().equals(eventType) |
            NotificationType.ASYNC_TASK_PROCESSED.name().equals(eventType)) {
            notificationService.persist(message, null, false);

            // emitir evento de notificação geérica para o usuário
            sseService.sendTo(message.getUserEmail(), "USER_NOTIFICATION", java.util.Map.of(
                "id", payload.getTaskId().toString(),
                "title", message.getTitle(),
                "datetime", LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getTimestamp()), ZoneId.of("America/Sao_Paulo")).toString(),
                "sentLabel", "",
                "icon", "bell",
                "content", message.getDescription(),
                "link", NotificationType.ASYNC_TASK_PROCESSED.name().equals(eventType) ? "/saas/results" : "",
                "read", false
            ));

            if (NotificationType.ASYNC_TASK_PROCESSED.name().equals(eventType)) {
                // emitir evento para task panel do usuário.
                sseService.sendTo(message.getUserEmail(), "COMPLETE_TASK_ON_PANEL", java.util.Map.of(
                    "taskId", payload.getTaskId().toString(),
                    "platform", payload.getPlatform().name(),
                    "status", payload.getStatus().name(),
                    "query", payload.getQuery()
                ));
            }
        } 
    }
}
