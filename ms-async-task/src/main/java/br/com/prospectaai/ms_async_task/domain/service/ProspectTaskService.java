package br.com.prospectaai.ms_async_task.domain.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.prospectaai.ms_async_task.domain.dto.ProspectRequest;
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
    @Value("${google.places.api-key:}")
    private String googlePlacesApiKey;
    @Value("${serper.api-key:}")
    private String serperApiKey;

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
                .imageUrl(r.getImageUrl())
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

    public void deleteAllProspection(String userEmail) {
        List<ProspectTask> tasks = taskRepository.findByUserEmailAndStatuses(userEmail, java.util.List.of(AsyncTaskStatus.PROCESSING, AsyncTaskStatus.PROCESSED));
        if (tasks.isEmpty()) return;
        
        for (ProspectTask task : tasks) {
            var recs = recordRepository.findByTask_Id(task.getId());
            if (recs != null && !recs.isEmpty()) {
                recordRepository.deleteAll(recs);
            }
            taskRepository.delete(task);
        }
    }
    
    public void call(ProspectRequest request, String userEmail) {
        long existing = taskRepository.countByUserEmailAndQueryAndPlatformAndStatus(
            userEmail,
            request.getQuery(),
            request.getPlatform(),
            AsyncTaskStatus.PROCESSING
        );
        if (existing > 0) {
            return;
        }
        AsyncTaskMessage message = AsyncTaskMessage.builder()
            .type(AsyncTaskMessageType.PROCESSING)
            .platform(request.getPlatform())
            .query(request.getQuery())
            .build();

        ProspectTask task = new ProspectTask();
        task.setUserEmail(userEmail);
        task.setQuery(request.getQuery());
        task.setPlatform(request.getPlatform());
        task.setLocation(request.getLocation());
        task.setBusinessType(request.getBusinessType());
        task.setRadiusKm(request.getRadiusKm());
        task.setCompanySize(request.getCompanySize());
        task.setUseAddress(request.getUseAddress());
        task.setAddressStreet(request.getAddressStreet());
        task.setAddressNumber(request.getAddressNumber());
        task.setAddressCity(request.getAddressCity());
        task.setAddressNeighborhood(request.getAddressNeighborhood());
        task.setAddressState(request.getAddressState());
        task.setAddressZip(request.getAddressZip());
        task.setLatitude(request.getLatitude());
        task.setLongitude(request.getLongitude());
        task.setStateId(request.getStateId());
        task.setStateSigla(request.getStateSigla());
        task.setCityName(request.getCityName());
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
            config.put("serpapi.apiKey", serpApiKey != null ? serpApiKey.trim() : "");
            config.put("serper.apiKey", serperApiKey != null ? serperApiKey.trim() : "");
            String computedLoc = computeLocation(task);
            if (computedLoc != null && !computedLoc.isBlank()) {
                config.put("location", computedLoc);
            } else if (task.getLocation() != null) {
                config.put("location", task.getLocation());
            }
            if (task.getBusinessType() != null) config.put("businessType", task.getBusinessType());
            if (task.getRadiusKm() != null) config.put("radiusKm", String.valueOf(task.getRadiusKm()));
            if (task.getLatitude() != null && task.getLongitude() != null) {
                config.put("lat", String.valueOf(task.getLatitude()));
                config.put("lon", String.valueOf(task.getLongitude()));
            }
            config.put("hl", "pt-BR");
            config.put("gl", "br");
            config.put("randomize", "true");
            if (googlePlacesApiKey != null && !googlePlacesApiKey.isBlank()) {
                config.put("google.places.apiKey", googlePlacesApiKey.trim());
            }
            Prospector prospector = ProspectorFactory.create(message.getPlatform(), config);
            var results = prospector.prospect(message.getQuery());

            System.out.println("[ProspectTaskService] Received " + (results != null ? results.size() : 0) + " results from SDK for task " + taskId);

            List<ProspectionRecord> toSave = new ArrayList<>();
            if (results != null && !results.isEmpty()) {
                for (var r : results) {
                    try {
                        ProspectionRecord rec = new ProspectionRecord();
                        rec.setTask(task);
                        rec.setQuery(truncate(r.getQuery(), 512));
                        rec.setPlatform(r.getPlatform() != null ? truncate(r.getPlatform().name(), 64) : null);
                        rec.setNomeEmpresa(truncate(r.getNomeEmpresa(), 256));
                        rec.setTelefone(truncate(r.getTelefone(), 64));
                        rec.setEndereco(truncate(r.getEndereco(), 512));
                        rec.setWebsite(truncate(r.getWebsite(), 256));
                        rec.setImageUrl(truncate(r.getImageUrl(), 512));
                        
                        if (r.getRating() != null && !r.getRating().isBlank()) {
                            try {
                                rec.setRating(Double.valueOf(r.getRating().replace(",", ".")));
                            } catch (NumberFormatException nfe) {
                                rec.setRating(null);
                            }
                        }
                        
                        if (r.getReviews() != null && !r.getReviews().isBlank()) {
                            try {
                                rec.setReviews(Integer.valueOf(r.getReviews().replaceAll("[^0-9]", "")));
                            } catch (NumberFormatException nfe) {
                                rec.setReviews(null);
                            }
                        }

                        rec.setEspecialidades(truncate(r.getEspecialidades(), 1024));
                        rec.setCreatedAt(Instant.now());
                        rec.setUserEmail(userEmail);
                        rec.setUserId(userId.toString());
                        toSave.add(rec);
                    } catch (Exception ex) {
                        System.err.println("[ProspectTaskService] Error mapping record: " + ex.getMessage());
                    }
                }
                
                if (!toSave.isEmpty()) {
                    System.out.println("[ProspectTaskService] Saving " + toSave.size() + " records to database for task " + taskId);
                    recordRepository.saveAll(toSave);
                    System.out.println("[ProspectTaskService] Successfully saved records for task " + taskId);
                } else {
                    System.out.println("[ProspectTaskService] No records to save for task " + taskId);
                }
            } else {
                System.out.println("[ProspectTaskService] Results list is empty or null for task " + taskId);
            }

            task.setStatus(AsyncTaskStatus.PROCESSED);
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
            sendProcessedEvent(task, userEmail);
        } catch (Exception e) {
            System.err.println("[prospection-sdk] error -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
    
    private String computeLocation(ProspectTask task) {
        if (task.getUseAddress() == null || !task.getUseAddress()) return null;
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (task.getAddressStreet() != null && !task.getAddressStreet().isBlank()) {
            if (task.getAddressNumber() != null && !task.getAddressNumber().isBlank()) {
                parts.add(task.getAddressStreet().trim() + " " + task.getAddressNumber().trim());
            } else {
                parts.add(task.getAddressStreet().trim());
            }
        }
        if (task.getAddressNeighborhood() != null && !task.getAddressNeighborhood().isBlank()) {
            parts.add(task.getAddressNeighborhood().trim());
        }
        if (task.getAddressCity() != null && !task.getAddressCity().isBlank()) {
            parts.add(task.getAddressCity().trim());
        }
        if (task.getAddressState() != null && !task.getAddressState().isBlank()) {
            parts.add(task.getAddressState().trim());
        }
        if (task.getAddressZip() != null && !task.getAddressZip().isBlank()) {
            parts.add(task.getAddressZip().trim());
        }
        if (parts.isEmpty()) return null;
        return String.join(", ", parts);
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
