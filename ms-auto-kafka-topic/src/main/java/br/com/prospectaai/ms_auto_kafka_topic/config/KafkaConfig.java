package br.com.prospectaai.ms_auto_kafka_topic.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic userCreatedTopic() {
        return new NewTopic("user.created", 1,  (short) 1);
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return new NewTopic("user.updated", 1,  (short) 1);
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return new NewTopic("user.deleted", 1,  (short) 1);
    }
}
