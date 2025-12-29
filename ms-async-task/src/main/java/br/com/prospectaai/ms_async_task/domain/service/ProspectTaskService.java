package br.com.prospectaai.ms_async_task.domain.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.com.prospectaai.ms_async_task.domain.entity.ProspectTask;
import br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord;
import br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus;
import br.com.prospectaai.ms_async_task.domain.repository.ProspectTaskRepository;
import br.com.prospectaai.ms_async_task.domain.repository.ProspectionRecordRepository;
import br.com.prospectaai.ms_async_task.domain.util.VirtualThread;
import br.com.prospectaai.sdk.prospection.Prospector;
import br.com.prospectaai.sdk.prospection.ProspectorFactory;
import br.com.prospectaai.shared.dto.async.AsyncTaskMessage;
import br.com.prospectaai.shared.dto.async.AsyncTaskMessageType;
import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import br.com.prospectaai.shared.dto.analytics.AnalyticsOverview;
import br.com.prospectaai.shared.dto.notification.AsyncTaskNotification;
import br.com.prospectaai.shared.dto.notification.NotificationType;
import br.com.prospectaai.shared.kafka.KafkaMessageTopic;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProspectTaskService {
    private final ProspectTaskRepository taskRepository;
    private final ProspectionRecordRepository recordRepository;
    private final KafkaTemplate<String, KafkaMessageTopic<?>> kafkaTemplate;
    private final AnalyticsService analyticsService;

    public void call(String query, AsyncTaskPlatform platform) {
        AsyncTaskMessage message = AsyncTaskMessage.builder()
            .type(AsyncTaskMessageType.PROCESSING)
            .platform(platform)
            .query(query)
            .build();

        ProspectTask task = new ProspectTask();
        task.setQuery(query);
        task.setPlatform(platform);
        task.setStatus(AsyncTaskStatus.PROCESSING);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task = taskRepository.save(task);

        sendProcessingEvent(task);

        final Long taskId = task.getId();
        VirtualThread.callAsync(() -> prospect(message, taskId));
    }

    private void prospect(AsyncTaskMessage message, Long taskId) {
        try {
            Prospector prospector = ProspectorFactory.create(message.getPlatform(),
                    java.util.Map.of("serpapi.apiKey", serpApiKey));
            var results = prospector.prospect(message.getQuery());
            var taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return;
            var task = taskOpt.get();

            List<ProspectionRecord> toSave = new ArrayList<>();
            if (results != null && !results.isEmpty()) {
                for (var r : results) {
                    ProspectionRecord rec = new ProspectionRecord();
                    rec.setTask(task);
                    rec.setQuery(r.getQuery());
                    rec.setPlatform(r.getPlatform() != null ? r.getPlatform().name() : null);
                    rec.setNomeEmpresa(r.getNomeEmpresa());
                    rec.setTelefone(r.getTelefone());
                    rec.setEndereco(r.getEndereco());
                    rec.setWebsite(r.getWebsite());
                    rec.setRating(r.getRating() != null && !r.getRating().isBlank() ? Double.valueOf(r.getRating()) : null);
                    rec.setReviews(r.getReviews() != null && !r.getReviews().isBlank() ? Integer.valueOf(r.getReviews()) : null);
                    rec.setEspecialidades(r.getEspecialidades());
                    rec.setCreatedAt(Instant.now());
                    toSave.add(rec);
                }
                recordRepository.saveAll(toSave);
            }

            task.setStatus(AsyncTaskStatus.PROCESSED);
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);

            sendProcessedEvent(task);
        } catch (Exception e) {
            System.err.println("[prospection-sdk] error -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Value("${serpapi.api-key}")
    private String serpApiKey;

    private void sendProcessingEvent(ProspectTask task) {
        AsyncTaskNotification payload = new AsyncTaskNotification(
                task.getId(),
                task.getQuery(),
                task.getPlatform(),
                br.com.prospectaai.shared.dto.async.AsyncTaskStatus.PROCESSING,
                null
        );
        KafkaMessageTopic<AsyncTaskNotification> msg = KafkaMessageTopic.<AsyncTaskNotification>builder()
                .applicationName("ms-async-task")
                .eventType(NotificationType.ASYNC_TASK_PROCESSING_STARTED.name())
                .timestamp(System.currentTimeMillis())
                .messageData(payload)
                .build();
        kafkaTemplate.send("notification", msg);
    }

    private void sendProcessedEvent(ProspectTask task) {
        AnalyticsOverview overview = analyticsService.getOverview();
        AsyncTaskNotification payload = new AsyncTaskNotification(
                task.getId(),
                task.getQuery(),
                task.getPlatform(),
                br.com.prospectaai.shared.dto.async.AsyncTaskStatus.PROCESSED,
                overview
        );
        KafkaMessageTopic<AsyncTaskNotification> msg = KafkaMessageTopic.<AsyncTaskNotification>builder()
                .applicationName("ms-async-task")
                .eventType(NotificationType.ASYNC_TASK_PROCESSED.name())
                .timestamp(System.currentTimeMillis())
                .messageData(payload)
                .build();
        kafkaTemplate.send("notification", msg);
    }
}
