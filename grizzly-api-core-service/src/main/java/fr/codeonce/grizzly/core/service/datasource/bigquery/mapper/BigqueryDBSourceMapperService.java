package fr.codeonce.grizzly.core.service.datasource.bigquery.mapper;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.bigquery.BigquerySourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BigqueryDBSourceMapperService extends BigqueryDBSourceMapperImpl {

    @Autowired
    private BigquerySourceService bigqueryService;

    private static final Logger log = LoggerFactory.getLogger(BigquerySourceService.class);


    public DBSourceDto mapToDto(DBSource entity, byte[] file) throws IOException {
        DBSourceDto dto = super.mapToDto(entity);
        dto.setActive(bigqueryService.checkTempConnectionBigquery(file));
        dto.setConnectionSucceeded(bigqueryService.checkTempConnectionBigquery(file));
        return dto;
    }

}

