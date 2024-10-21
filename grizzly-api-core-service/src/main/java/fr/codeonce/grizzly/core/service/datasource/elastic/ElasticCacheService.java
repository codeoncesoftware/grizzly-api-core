/*
 * Copyright © 2020 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.codeonce.grizzly.core.service.datasource.elastic;

import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ElasticCacheService {

    private static final String DEFAULT_ELASTIC_USERNAME = "elastic";

    private static final String LOCALHOST_IP = "127.0.0.1";

    private static final String LOCALHOST = "localhost";

    @Autowired
    private DBSourceRepository repository;

    @Autowired
    @Qualifier("coreCryptoHelper")
    private CryptoHelper encryption;

    @Cacheable(value = "elasticClients", key = "#db.id")
    public RestHighLevelClient getClient(DBSource db) {

        return buildRestHighLevelClient(db);
    }

    public RestHighLevelClient buildRestHighLevelClient(DBSource db) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (db.isSecured()) {
            credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(
                    StringUtils.defaultIfEmpty(db.getUsername(), DEFAULT_ELASTIC_USERNAME),
                    getSafeValue(db.getPassword())));
        }

        HttpClientConfigCallback clientConfigCallBack = (HttpAsyncClientBuilder httpClientBuilder) -> httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider);

        RequestConfigCallback requestConfigCallBack = (
                RequestConfig.Builder requestConfigBuilder) -> requestConfigBuilder
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(60000);

        RestClientBuilder builder = RestClient
                .builder(new HttpHost(db.getHost(), db.getPort(), guessConnectionProtocol(db)))
                .setHttpClientConfigCallback(clientConfigCallBack).setRequestConfigCallback(requestConfigCallBack);

        return new RestHighLevelClient(builder);

    }


    public RestClient getRestClient(String dbsourceId) {

        return buildRestClient(dbsourceId);

    }

    private RestClient buildRestClient(String dbsourceId) {

        return this.repository.findById(dbsourceId).map(db -> {
            encryption.decrypt(db);
            Header[] headers = {new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                    new BasicHeader("Role", "Read")};
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            if (db.isSecured()) {
                credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(

                        StringUtils.defaultIfEmpty(db.getUsername(), DEFAULT_ELASTIC_USERNAME),
                        getSafeValue(db.getPassword())));
            }

            HttpClientConfigCallback clientConfigCallBack = (
                    HttpAsyncClientBuilder httpClientBuilder) -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider);

            RequestConfigCallback requestConfigCallBack = (
                    RequestConfig.Builder requestConfigBuilder) -> requestConfigBuilder.setConnectTimeout(5000)
                    .setConnectionRequestTimeout(60000);

            return RestClient.builder(new HttpHost(db.getHost(), db.getPort(), guessConnectionProtocol(db)))
                    .setDefaultHeaders(headers).setHttpClientConfigCallback(clientConfigCallBack)
                    .setRequestConfigCallback(requestConfigCallBack).build();
        }).orElseThrow();

    }

    /**
     * Guess the used protocol, the Default is "HTTPS " if Elastic is hosted on the
     * Cloud For Local instance, we use "HTTP"
     *
     * @param db
     * @return
     */
    private String guessConnectionProtocol(DBSource db) {

        return (db.getHost().toLowerCase().contains(LOCALHOST) || db.getHost().toLowerCase().contains(LOCALHOST_IP))
                ? "http"
                : "https";

    }

    private String getSafeValue(char[] pwd) {
        return pwd == null ? "" : String.valueOf(pwd);
    }

    @CachePut(value = "elasticClients", key = "#db.id")
    public RestHighLevelClient updateCache(DBSource db) {
        return buildRestHighLevelClient(db);
    }

}
