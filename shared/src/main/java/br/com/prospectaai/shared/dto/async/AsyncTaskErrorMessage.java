package br.com.prospectaai.shared.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AsyncTaskErrorMessage {
    private String workflowId;
    private String nodeName;
    private String errorMessage;
    private String stackTrace;
    private String timestamp;
    private String query;
    private AsyncTaskPlatform platform;
}

