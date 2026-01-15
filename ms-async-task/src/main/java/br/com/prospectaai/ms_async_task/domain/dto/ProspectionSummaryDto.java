package br.com.prospectaai.ms_async_task.domain.dto;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import br.com.prospectaai.shared.dto.async.AsyncTaskStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProspectionSummaryDto {
    private Long taskId;
    private String query;
    private AsyncTaskPlatform platform;
    private AsyncTaskStatus status;
    private String createdAt;
    private long resultsCount;
}

