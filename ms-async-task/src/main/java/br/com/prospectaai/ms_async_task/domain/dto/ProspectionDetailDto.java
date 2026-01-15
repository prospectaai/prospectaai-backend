package br.com.prospectaai.ms_async_task.domain.dto;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProspectionDetailDto {
    private Long taskId;
    private String query;
    private AsyncTaskPlatform platform;
    private String updatedAt;
    private long resultsCount;
    private List<ProspectionRecordDto> results;
}

