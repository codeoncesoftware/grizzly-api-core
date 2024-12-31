package fr.codeonce.grizzly.core.service.datasource.bigquery;

import com.google.cloud.bigquery.BigQuery;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.bigquery.mapper.BigqueryDBSourceMapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BigquerySourceService {

    @Autowired
    private BigqueryDBSourceMapperService mapper;

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    private BigQueryCacheService cacheService;
    private static final Logger log = LoggerFactory.getLogger(BigquerySourceService.class);

    public DBSourceDto saveDBSource(DBSourceDto dto, byte[] file) throws IOException {
        try {
            DBSource source = mapper.mapToDomain(dto);
      /*  byte[] fileBytes = source.getPrivetkeyBigQuery();
        String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
        System.out.println(fileContent); */
            source = repository.save(source);
            cacheService.updateCache(file);
            return mapper.mapToDto(source, file);
        } catch (IOException e) {
            throw e;
        }
    }

    public Boolean checkTempConnectionBigquery(byte[] file) throws IOException {
        try {
            BigQuery bigQuery = cacheService.getClient(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
