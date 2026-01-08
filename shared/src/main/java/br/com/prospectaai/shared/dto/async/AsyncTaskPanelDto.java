package br.com.prospectaai.shared.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncTaskPanelDto {
    private Long taskId;
    private String query;
    private AsyncTaskPlatform platform;
    private AsyncTaskStatus status;
}
