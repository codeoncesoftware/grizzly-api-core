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
package fr.codeonce.grizzly.core.service.datasource.sql;

import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@DependsOnDatabaseInitialization
@Service
public class SqlCacheService {

    @Autowired
    private DBSourceRepository dBSourceRepository;

    @Autowired
    private CryptoHelper encryption;

    @Cacheable(value = "dbSource", key = "#dbSource.id")
    public DataSource getClientDatasource(DBSource dbSource) {
        encryption.decrypt(dbSource);
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        String url = "jdbc:" + dbSource.getProvider().name().toLowerCase() + "://" + dbSource.getHost() + ":"
                + dbSource.getPort() + "/" + dbSource.getDatabase() + "&useSSL=false";
        dataSourceBuilder.driverClassName(getDriverClassName(dbSource.getProvider()));
        dataSourceBuilder.url(url);
        dataSourceBuilder.url(generateConnectionUrl(dbSource));
        dataSourceBuilder.username(dbSource.getUsername());
        String password = getSafeValue(dbSource.getPassword());
        dataSourceBuilder.password(password);
        return dataSourceBuilder.build();
    }

    private String getProviderName(Provider provider) {
        switch (provider) {
            case MYSQL:
                return "mysql";
            case MARIADB:
                return "mysql";
            case POSTGRESQL:
                return "postgresql";
            case SQLSERVER:
                return "sqlserver";
            default:
                return null;
        }
    }

    private String generateConnectionUrl(DBSource dto) {
        String url = "";
        if (dto.getProvider().name().equalsIgnoreCase("sqlserver")) {
            url = "jdbc:" + getProviderName(dto.getProvider()) + "://" + dto.getHost() + ":" + dto.getPort()
                    + ";databaseName=" + dto.getDatabase() + ";user=" + dto.getUsername() + ";password="
                    + getSafeValue(dto.getPassword()) + "&useSSL=false";
        } else {
            url = "jdbc:" + getProviderName(dto.getProvider()) + "://" + dto.getHost() + ":" + dto.getPort()
                    + "/" + dto.getDatabase() + "?user=" + dto.getUsername() + "&password=" + getSafeValue(dto.getPassword()) + "&useSSL=false";
        }
        return url;
    }


    @Cacheable(value = "jdbctemplate")
    public JdbcTemplate prepareDatasourceClient(String datasourceId) {
        JdbcTemplate jt = new JdbcTemplate();
        DBSource dbSource = dBSourceRepository.findById(datasourceId).orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, datasourceId));
        DataSource datasource = getClientDatasource(dbSource);
        jt.setDataSource(datasource);
        return jt;
    }

    private String getDriverClassName(Provider provider) {
        switch (provider) {
            case MYSQL:
                return "com.mysql.jdbc.Driver";
            case MARIADB:
                return "org.mariadb.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case SQLSERVER:
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default:
                return null;
        }
    }

    private String getSafeValue(char[] pwd) {
        return pwd == null ? "" : String.valueOf(pwd);
    }
}
