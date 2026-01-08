package br.com.prospectaai.ms_notification.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationItem {
    private String id;
    private String title;
    private String datetime;
    private String sentLabel;
    private String icon;
    private String content;
    private String link;
    private boolean read;
}
