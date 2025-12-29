package br.com.prospectaai.shared.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AsyncTaskMessage {
    private String query;
    private AsyncTaskMessageType type;
    private AsyncTaskPlatform platform;
}

