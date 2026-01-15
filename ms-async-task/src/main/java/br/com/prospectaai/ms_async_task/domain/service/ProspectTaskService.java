package br.com.prospectaai.ms_async_task.domain.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import br.com.prospectaai.shared.dto.async.AsyncTaskPanelDto;
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
    private final UserAccountClient userAccountClient;
    @Value("${reachability.service.base-url:}")
    private String reachabilityBaseUrl;

    public List<AsyncTaskPanelDto> getAllProcessing(String userEmail) {
        List<ProspectTask> tasks = taskRepository.findByUserEmailAndStatus(userEmail, AsyncTaskStatus.PROCESSING);
        return tasks.stream()
            .<AsyncTaskPanelDto>map(task -> AsyncTaskPanelDto.builder()
                .taskId(task.getId())
                .query(task.getQuery())
                .platform(task.getPlatform())
                .status(br.com.prospectaai.shared.dto.async.AsyncTaskStatus.valueOf(task.getStatus().name()))
                .build())
            .collect(java.util.stream.Collectors.toList());
    }

    public List<AsyncTaskPanelDto> getAllProcessed(String userEmail) {
        List<ProspectTask> tasks = taskRepository.findByUserEmailAndStatus(userEmail, AsyncTaskStatus.PROCESSED);
        return tasks.stream()
            .<AsyncTaskPanelDto>map(task -> AsyncTaskPanelDto.builder()
                .taskId(task.getId())
                .query(task.getQuery())
                .platform(task.getPlatform())
                .status(br.com.prospectaai.shared.dto.async.AsyncTaskStatus.valueOf(task.getStatus().name()))
                .build())
            .collect(java.util.stream.Collectors.toList());
    }

    public List<br.com.prospectaai.ms_async_task.domain.dto.ProspectionSummaryDto> getAllResultsSummary(String userEmail) {
        List<ProspectTask> tasks = taskRepository.findByUserEmailAndStatuses(userEmail, java.util.List.of(AsyncTaskStatus.PROCESSING, AsyncTaskStatus.PROCESSED));
        return tasks.stream()
            .map(task -> br.com.prospectaai.ms_async_task.domain.dto.ProspectionSummaryDto.builder()
                .taskId(task.getId())
                .query(task.getQuery())
                .platform(task.getPlatform())
                .status(br.com.prospectaai.shared.dto.async.AsyncTaskStatus.valueOf(task.getStatus().name()))
                .createdAt(task.getCreatedAt().toString())
                .resultsCount(recordRepository.countByTask_Id(task.getId()))
                .build())
            .collect(java.util.stream.Collectors.toList());
    }

    public br.com.prospectaai.ms_async_task.domain.dto.ProspectionDetailDto getResultDetail(Long taskId, String userEmail) {
        var opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) return null;
        var task = opt.get();
        if (!userEmail.equals(task.getUserEmail())) return null;
        if (task.getStatus() != AsyncTaskStatus.PROCESSED) {
            return null;
        }
        var records = recordRepository.findByTask_Id(taskId);
        java.util.List<br.com.prospectaai.ms_async_task.domain.dto.ProspectionRecordDto> items = records.stream().map(r ->
            br.com.prospectaai.ms_async_task.domain.dto.ProspectionRecordDto.builder()
                .query(r.getQuery())
                .platform(r.getPlatform())
                .nomeEmpresa(r.getNomeEmpresa())
                .telefone(r.getTelefone())
                .endereco(r.getEndereco())
                .website(r.getWebsite())
                .rating(r.getRating())
                .reviews(r.getReviews())
                .especialidades(r.getEspecialidades())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build()
        ).collect(java.util.stream.Collectors.toList());
        return br.com.prospectaai.ms_async_task.domain.dto.ProspectionDetailDto.builder()
            .taskId(task.getId())
            .query(task.getQuery())
            .platform(task.getPlatform())
            .updatedAt(task.getUpdatedAt().toString())
            .resultsCount(items.size())
            .results(items)
            .build();
    }

    public boolean deleteProspection(Long taskId, String userEmail) {
        var opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) return false;
        var task = opt.get();
        if (!userEmail.equals(task.getUserEmail())) return false;
        var recs = recordRepository.findByTask_Id(taskId);
        if (recs != null && !recs.isEmpty()) {
            recordRepository.deleteAll(recs);
        }
        taskRepository.delete(task);
        return true;
    }
    
    public void call(String query, AsyncTaskPlatform platform, String userEmail, String location, String businessType, Integer radiusKm, String companySize) {
        AsyncTaskMessage message = AsyncTaskMessage.builder()
            .type(AsyncTaskMessageType.PROCESSING)
            .platform(platform)
            .query(query)
            .build();

        ProspectTask task = new ProspectTask();
        task.setUserEmail(userEmail);
        task.setQuery(query);
        task.setPlatform(platform);
        task.setLocation(location);
        task.setBusinessType(businessType);
        task.setRadiusKm(radiusKm);
        task.setCompanySize(companySize);
        task.setStatus(AsyncTaskStatus.PROCESSING);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task = taskRepository.save(task);

        sendProcessingEvent(task, userEmail);

        final Long taskId = task.getId();
        VirtualThread.callAsync(() -> prospect(message, taskId, userEmail));
    }

    private void prospect(AsyncTaskMessage message, Long taskId, String userEmail) {
        try {
            UUID userId = userAccountClient.resolveUserIdByEmail(userEmail);
            var taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return;
            var task = taskOpt.get();
            java.util.Map<String, String> config = new java.util.HashMap<>();
            config.put("serpapi.apiKey", serpApiKey);
            if (task.getLocation() != null) config.put("location", task.getLocation());
            if (task.getBusinessType() != null) config.put("businessType", task.getBusinessType());
            if (task.getCompanySize() != null) config.put("companySize", task.getCompanySize());
            if (task.getRadiusKm() != null) config.put("radiusKm", String.valueOf(task.getRadiusKm()));
            config.put("hl", "pt-BR");
            config.put("gl", "br");
            config.put("randomize", "true");
            if (reachabilityBaseUrl != null && !reachabilityBaseUrl.isBlank()) {
                config.put("reachability.baseUrl", reachabilityBaseUrl.trim());
            }
            Prospector prospector = ProspectorFactory.create(message.getPlatform(), config);
            var results = prospector.prospect(message.getQuery());

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
                    rec.setUserEmail(userEmail);
                    rec.setUserId(userId.toString());
                    toSave.add(rec);
                }
                recordRepository.saveAll(toSave);
            }

            task.setStatus(AsyncTaskStatus.PROCESSED);
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);

            Thread.sleep(15000);
            sendProcessedEvent(task, userEmail);
        } catch (Exception e) {
            System.err.println("[prospection-sdk] error -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Value("${serpapi.api-key}")
    private String serpApiKey;

    private void sendProcessingEvent(ProspectTask task, String userEmail) {
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
                .userEmail(userEmail)
                .title("Nova prospecção iniciada! Clique para mais detalhes. ")
                .description("Uma nova prospecção na plataforma " + task.getPlatform() + " buscando \"" + task.getQuery() + "\" foi iniciada! Acompanhe o progresso no painel de tarefas no canto inferior da tela.")
                .build();

        kafkaTemplate.send("notification", msg);
    }

    private void sendProcessedEvent(ProspectTask task, String userEmail) {
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
                .userEmail(userEmail)
                .title("Prossecção finalizada! Clique para mais detalhes. ")
                .description("A prospecção na plataforma " + task.getPlatform() + " buscando \"" + task.getQuery() + "\" foi finalizada! Clique para ver os resultados.")
                .build();
        kafkaTemplate.send("notification", msg);
    }
}
