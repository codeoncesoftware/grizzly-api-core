package fr.codeonce.grizzly.core.service.datasource.bigquery.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;

@Mapper
public interface BigqueryDBSourceMapper {

    DBSource mapToDomain(DBSourceDto dto);

    DBSourceDto mapToDto(DBSource entity);

}
