package br.com.prospectaai.ms_async_task.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import br.com.prospectaai.shared.kafka.KafkaMessageTopic;

@Configuration
@EnableKafka
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapKafkaServer;

    @Bean
    public ProducerFactory<String, KafkaMessageTopic<?>> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        String resolved = resolveBootstrapServers(bootstrapKafkaServer);
        System.out.println("[ms-async-task][KafkaProducerConfig] bootstrap.servers=" + resolved);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, resolved);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 5);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 30000);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, KafkaMessageTopic<?>> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private String resolveBootstrapServers(String configured) {
        if (configured == null || configured.isBlank()) {
            return "localhost:9092";
        }
        String[] entries = configured.split(",");
        List<String> valid = new ArrayList<>();
        for (String e : entries) {
            String entry = e.trim();
            if (entry.isEmpty()) continue;
            String host = entry.contains(":") ? entry.substring(0, entry.indexOf(':')) : entry;
            String port = entry.contains(":") ? entry.substring(entry.indexOf(':') + 1) : "9092";
            try {
                InetAddress.getAllByName(host);
                valid.add(host + ":" + port);
            } catch (UnknownHostException ignored) {}
        }
        return valid.isEmpty() ? "localhost:9092" : String.join(",", valid);
    }
}
