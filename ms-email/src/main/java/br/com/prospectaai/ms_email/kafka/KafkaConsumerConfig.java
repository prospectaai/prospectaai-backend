package br.com.prospectaai.ms_email.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import br.com.prospectaai.shared.kafka.KafkaMessageTopic;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapKafkaServer;

    @Bean
    public ConsumerFactory<String, KafkaMessageTopic<?>> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapKafkaServer);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // ou defina explicitamente seu pacote
        // O produtor não envia type headers; garantir que o consumidor não dependa deles
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, KafkaMessageTopic.class.getName());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "ms-email-group"); // defina seu grupo de consumidores
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // ou "latest" conforme necessário

        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(KafkaMessageTopic.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KafkaMessageTopic<?>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaMessageTopic<?>> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}