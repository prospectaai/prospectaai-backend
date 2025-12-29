package br.com.prospectaai.ms_notification.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.prospectaai.shared.kafka.KafkaMessageTopic;
import br.com.prospectaai.shared.dto.notification.NotificationType;
import br.com.prospectaai.shared.dto.notification.AsyncTaskNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import br.com.prospectaai.ms_notification.sse.NotificationSseService;

@Component
@RequiredArgsConstructor
public class KafkaNotificationListener {

    private final ObjectMapper objectMapper;
    private final NotificationSseService sseService;

    @KafkaListener(topics = "notification", groupId = "ms-notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(KafkaMessageTopic<?> message) {
        try {
            String eventType = message.getEventType();
            if (NotificationType.ASYNC_TASK_PROCESSING_STARTED.name().equals(eventType)
                    || NotificationType.ASYNC_TASK_PROCESSED.name().equals(eventType)) {
                AsyncTaskNotification payload = objectMapper.convertValue(message.getMessageData(), AsyncTaskNotification.class);
                sseService.broadcast(eventType, payload);
            }
        } catch (Exception e) {
            System.out.println("Erro ao enviar notificação: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
