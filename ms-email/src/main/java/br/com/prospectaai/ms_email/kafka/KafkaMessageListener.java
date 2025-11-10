package br.com.prospectaai.ms_email.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.prospectaai.shared.kafka.KafkaMessageTopic;

@Component
public class KafkaMessageListener {
    @KafkaListener(topics = "email.sender", groupId = "ms-email-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(KafkaMessageTopic<?> message) {
        System.out.println("Mensagem recebida: " + message);
    }

}
