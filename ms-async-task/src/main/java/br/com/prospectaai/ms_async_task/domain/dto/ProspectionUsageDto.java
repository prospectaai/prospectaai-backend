package br.com.prospectaai.ms_async_task.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProspectionUsageDto {
    private long used;
    private long limit;
}
