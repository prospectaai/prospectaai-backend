package br.com.prospectaai.ms_useraccount.domain.service;

import java.time.Instant;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import br.com.prospectaai.ms_useraccount.domain.entity.PreRegisterAccountEntity;
import br.com.prospectaai.shared.email.EmailEnvelope;
import br.com.prospectaai.shared.kafka.KafkaMessageTopic;
import br.com.prospectaai.shared.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final KafkaTemplate<String, KafkaMessageTopic<?>> kafkaTemplate;

    public void sendPreRegisterConfirmationEmail(PreRegisterAccountEntity entity) {
        // Lógica para enviar email de confirmação com código de 6 dígitos
        String emailContent = "Olá, " + entity.getDisplayName() + "!\n\n" +
                "Seu código de confirmação é: " + entity.getConfirmationCode() + "\n" +
                "Ele é válido por 30 minutos.\n\n" +
                "Se você não solicitou este código, ignore este email.\n\n" +
                "Atenciosamente,\n" +
                "Equipe Prospecta AI 💙";

        KafkaMessageTopic<?> messageTopic = KafkaMessageTopic.builder()
            .applicationName("")
            .eventType("sender")
            .timestamp(Instant.now().toEpochMilli())
            .messageData(
                EmailEnvelope.builder()
                    .recipient(entity.getEmail())
                    .title("Código de Verificação - Prospecta AI")
                    .emailContent(emailContent)
                    .emailType("verification_code")
                    .metadata(null)
                    .build()
            )
            .build();

        System.out.println("Sending email to: " + entity.getEmail());
        var future = kafkaTemplate.send(KafkaTopic.EMAIL_SENDER.getTopic(), messageTopic);
        future.whenComplete((result, ex) -> {
            if (ex == null && result != null) {
                var meta = result.getRecordMetadata();
                System.out.println("Kafka SEND OK -> topic=" + meta.topic() +
                        ", partition=" + meta.partition() + ", offset=" + meta.offset());
            } else if (ex != null) {
                System.err.println("Kafka SEND FAIL -> " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public void sendResetPasswordEmail(String recipient, String displayName, String link) {
        String content = "Olá, " + displayName + "!\n\n" +
                "Recebemos uma solicitação para redefinir sua senha.\n" +
                "Clique no link abaixo para continuar:\n" +
                link + "\n\n" +
                "Se você não solicitou a alteração, ignore este email.\n\n" +
                "Atenciosamente,\nEquipe Prospecta AI 💙";

        KafkaMessageTopic<?> messageTopic = KafkaMessageTopic.builder()
            .applicationName("")
            .eventType("sender")
            .timestamp(Instant.now().toEpochMilli())
            .messageData(
                EmailEnvelope.builder()
                    .recipient(recipient)
                    .title("Redefinição de Senha - Prospecta AI")
                    .emailContent(content)
                    .emailType("reset_password")
                    .metadata(null)
                    .build()
            )
            .build();

        var future = kafkaTemplate.send(KafkaTopic.EMAIL_SENDER.getTopic(), messageTopic);
        future.whenComplete((result, ex) -> {
            if (ex == null && result != null) {
                var meta = result.getRecordMetadata();
                System.out.println("Kafka SEND OK -> topic=" + meta.topic() +
                        ", partition=" + meta.partition() + ", offset=" + meta.offset());
            } else if (ex != null) {
                System.err.println("Kafka SEND FAIL -> " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}
