package fr.codeonce.grizzly.core.service.datasource.bigquery;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class BigQueryCacheService {

    @Autowired
    private DBSourceRepository repository;


    @Cacheable(value = "bigQueryClients", key = "#file.hashCode()")
    public BigQuery getClient(byte[] file) throws IOException {
        return buildBigQueryClient(file);
    }

    // Method to create a BigQuery client and cache it
    private BigQuery buildBigQueryClient(byte[] file) throws IOException {
        try (ByteArrayInputStream credentialsStream = new ByteArrayInputStream(file)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            Credentials scopedCredentials = credentials.createScoped("https://www.googleapis.com/auth/bigquery");
            return BigQueryOptions.newBuilder().setCredentials(scopedCredentials).build().getService();
        } catch (IOException e) {
            throw new IOException("Error building BigQuery client: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error building BigQuery client: " + e.getMessage(), e);
        }
    }

    @CachePut(value = "bigQueryClients", key = "#file.hashCode()")
    public BigQuery updateCache(byte[] file) {
        try {
            return buildBigQueryClient(file);
        } catch (Exception e) {
            System.err.println("Error updating cache: " + e.getMessage());
            return null;
        }
    }
}