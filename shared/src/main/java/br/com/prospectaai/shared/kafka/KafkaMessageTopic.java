package br.com.prospectaai.shared.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessageTopic<T> {
    private String applicationName;
    private String eventType;
    private Long timestamp;
    private T messageData;
}