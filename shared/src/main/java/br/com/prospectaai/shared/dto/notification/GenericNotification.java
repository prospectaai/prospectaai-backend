package br.com.prospectaai.shared.dto.notification;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenericNotification {
    private String userEmail;
    private String title;
    private Instant datetime;
    private String icon;
    private String content;
    private String link;
    private boolean read;
}
