package br.com.prospectaai.ms_email.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.prospectaai.ms_email.domain.service.EmailService;
import br.com.prospectaai.shared.email.EmailEnvelope;
import br.com.prospectaai.shared.kafka.KafkaMessageTopic;

@Component
public class KafkaMessageListener {
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public KafkaMessageListener(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "email.sender", groupId = "ms-email-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(KafkaMessageTopic<?> message) {
        try {
            EmailEnvelope emailEnvelope = objectMapper.convertValue(message.getMessageData(), EmailEnvelope.class);
            System.out.println("[ms-email] Consumido: recipient=" + emailEnvelope.getRecipient() + 
                ", title=" + emailEnvelope.getTitle());
            emailService.sendEmail(emailEnvelope);
        } catch (Exception e) {
            System.out.println("Erro ao enviar email: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
