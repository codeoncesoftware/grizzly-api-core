package fr.codeonce.grizzly.core.service.datasource.kafka;

import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class kafkaCacheService {
    @Cacheable(value = "kafkaProducers", key = "#dto.hashCode()")
    public KafkaProducer<String, String> getProducer(DBSourceDto dto) {
        return buildKafkaProducer(dto);
    }

    private KafkaProducer<String, String> buildKafkaProducer(DBSourceDto dto) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", dto.getHost() + ":" + dto.getPort());
        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + dto.getUsername() + "\" password=\"" + dto.getPassword() + "\";");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<>(properties);
    }

    @CachePut(value = "kafkaProducers", key = "#dto.hashCode()")
    public KafkaProducer<String, String> updateCache(DBSourceDto dto) {
        return buildKafkaProducer(dto);
    }
}