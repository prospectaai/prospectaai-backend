package br.com.prospectaai.shared.dto.notification;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProspectionNotificationEvent {
    private String userEmail;
    private Long taskId;
    private String query;
    private String platform;
    private Instant createdAt;
}
