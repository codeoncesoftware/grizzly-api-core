package fr.codeonce.grizzly.core.service.datasource.kafka.mapper;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import org.mapstruct.Mapper;

@Mapper
public interface KafkaSourceMapper {
    DBSource mapToDomain(DBSourceDto dto);

    DBSourceDto mapToDto(DBSource entity);
}

