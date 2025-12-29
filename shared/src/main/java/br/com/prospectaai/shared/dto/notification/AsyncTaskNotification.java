package br.com.prospectaai.shared.dto.notification;

import br.com.prospectaai.shared.dto.analytics.AnalyticsOverview;
import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import br.com.prospectaai.shared.dto.async.AsyncTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskNotification {
    private Long taskId;
    private String query;
    private AsyncTaskPlatform platform;
    private AsyncTaskStatus status;
    private AnalyticsOverview analyticsOverview;
}
