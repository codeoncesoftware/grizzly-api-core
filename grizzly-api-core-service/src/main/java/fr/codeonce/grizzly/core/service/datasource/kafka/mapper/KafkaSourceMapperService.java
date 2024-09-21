package fr.codeonce.grizzly.core.service.datasource.kafka.mapper;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.kafka.KafkaSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KafkaSourceMapperService extends KafkaSourceMapperImpl {

    @Autowired
    private KafkaSourceService kafkaService;

    @Override
    public DBSourceDto mapToDto(DBSource entity) {
        DBSourceDto dto = super.mapToDto(entity);
        dto.setActive(kafkaService.checkTempConnection(dto));

        return dto;
    }
}
