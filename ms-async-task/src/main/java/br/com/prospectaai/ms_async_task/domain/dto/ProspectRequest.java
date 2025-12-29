package br.com.prospectaai.ms_async_task.domain.dto;

import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import lombok.Data;

@Data
public class ProspectRequest {
    private String query;
    private AsyncTaskPlatform platform;
}

