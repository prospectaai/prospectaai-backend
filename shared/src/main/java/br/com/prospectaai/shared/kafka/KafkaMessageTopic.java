package br.com.prospectaai.shared.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaMessageTopic<T> {
    private String applicationName;
    private String eventType;
    private Long timestamp;
    private String userEmail;
    private String title;
    private String description;
    private T messageData;
}