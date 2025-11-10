package br.com.prospectaai.ms_auto_kafka_topic.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.prospectaai.shared.kafka.KafkaTopic;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic userCreatedTopic() {
        return new NewTopic(KafkaTopic.USER_CREATED.getTopic(), 1,  (short) 1);
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return new NewTopic(KafkaTopic.USER_UPDATED.getTopic(), 1,  (short) 1);
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return new NewTopic(KafkaTopic.USER_DELETED.getTopic(), 1,  (short) 1);
    }

    @Bean
    public NewTopic emailSenderTopic() {
        return new NewTopic(KafkaTopic.EMAIL_SENDER.getTopic(), 1,  (short) 1);
    }
}
