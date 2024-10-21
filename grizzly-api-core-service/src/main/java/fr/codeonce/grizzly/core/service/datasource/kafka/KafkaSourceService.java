package fr.codeonce.grizzly.core.service.datasource.kafka;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.kafka.mapper.KafkaSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KafkaSourceService {
    @Autowired
    private KafkaSourceMapperService mapper;

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    private CryptoHelper encryption;

    @Autowired
    private kafkaCacheService cacheService;

    private static final Logger log = LoggerFactory.getLogger(KafkaSourceService.class);


    public DBSourceDto saveDBSource(DBSourceDto dto) throws ParseException {
        System.out.println(dto.getProvider());
        DBSource kafkaSource = mapper.mapToDomain(dto);
        encryption.encrypt(kafkaSource);
        kafkaSource = repository.save(kafkaSource);
        encryption.decrypt(kafkaSource);
        cacheService.updateCache(dto);
        return mapper.mapToDto(kafkaSource);
    }


    public boolean checkTempConnection(DBSourceDto dto) {
        try {
            KafkaProducer<String, String> kafkaProducer = cacheService.getProducer(dto);
            kafkaProducer.close();
            return true; // If the connection succeeded
        } catch (Exception e) {
            log.warn("Unable to establish Kafka connection: {}", e.getMessage());
            return false;
        }
    }


}
