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
import fr.codeonce.grizzly.core.domain.datasource.*;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.query.CustomQueryDto;
import fr.codeonce.grizzly.core.service.datasource.sql.mapper.SqlDBSourceMapperService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class SqlDBSourceService {

    @Autowired
    private CryptoHelper encryption;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SqlDBSourceMapperService mapper;

    @Autowired
    private SqlCacheService sqlCacheService;

    private static final String SQL_SERVER = "sqlserver";
    private static final String MY_SQL = "mysql";
    private static final String POSTGRESQL = "postgresql";
    private static final String MARIA_DB = "mariadb";
    private static final String COLUMN_NAME = "COLUMN_NAME";

    private static final Logger log = LoggerFactory.getLogger(SqlDBSourceService.class);

    public DBSourceDto saveDBSource(DBSourceDto dto) throws SQLException {
        String currentUserEmail = "";
        if (SecurityContextUtil.getCurrentUserEmail() != null) {
            currentUserEmail = SecurityContextUtil.getCurrentUserEmail();
            if (!currentUserEmail.contains("@")) {
                User user = userRepository
                        .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                        .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, currentUserEmail));
                currentUserEmail = user.getEmail();
            }
        } else if (dto.getUserEmail() != null) {
            currentUserEmail = dto.getUserEmail();
        } else {
            currentUserEmail = "editor@codeonce.fr";
        }
        dto.setUserEmail(currentUserEmail);
        CustomDatabase db = new CustomDatabase();
        db.setName(dto.getName());

        dto.setDatabases(List.of(db));
        if (!dto.isDockerMode()) {
            getTables(dto, "all");
            dto.setActive(checkTempConnection(dto));
        }
        DBSource sqlSource = mapper.mapToDomain(dto);
        sqlSource.setDatabases(dto.getDatabases());
        encryption.encrypt(sqlSource);
        sqlSource = dbSourceRepository.save(sqlSource);
        encryption.decrypt(sqlSource);
        if (dto.isAuthDBEnabled()) {
            this.createFirstTable(sqlSource);
        }
        return mapper.mapToDto(sqlSource);
    }

    public boolean checkTempConnection(DBSourceDto dto) throws SQLException {
        Connection conn = null;
        try {
            String url = generateConnectionUrl(dto);
            conn = DriverManager.getConnection(url);
            return conn.isValid(1000);
        } catch (Exception e) {
            log.warn("Can't get status : {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private String getTableName(DBSourceDto dto, ResultSet resultSet) throws SQLException {
        String tableName = "";
        if (dto.getProvider().name().equalsIgnoreCase(SQL_SERVER)) {
            tableName = resultSet.getString(2) + "." + resultSet.getString(3);
        } else {
            tableName = resultSet.getString(3);
        }
        return tableName;
    }

    private Set<String> getPrimaryKeys(String tableName, DatabaseMetaData metadata) throws SQLException {
        ResultSet pkColumns = metadata.getPrimaryKeys(null, null, tableName);
        Set<String> pkColumnSet = new TreeSet<>();
        while (pkColumns.next()) {
            pkColumnSet.add(pkColumns.getString(COLUMN_NAME));
        }
        return pkColumnSet;
    }

    private List<Column> getColumns(String tableName, DatabaseMetaData metadata) throws SQLException {
        List<Column> columns = new ArrayList<>();
        ResultSet rsCols = metadata.getColumns(null, null, tableName, null);
        while (rsCols.next()) {
            Column column = new Column();
            column.setUnique(false);
            column.setName(rsCols.getString(COLUMN_NAME));
            column.setType((rsCols.getString("TYPE_NAME")));
            boolean isNullable = rsCols.getString("IS_NULLABLE").equalsIgnoreCase("yes");
            column.setNullable(isNullable);
            boolean autoIncrement = rsCols.getString("IS_AUTOINCREMENT").equalsIgnoreCase("yes");
            column.setAutoIncrement(autoIncrement);
            columns.add(column);
        }
        return columns;
    }

    private void getUniqueColumns(DBSourceDto dto, DatabaseMetaData metadata, List<Column> columns, String tableName)
            throws SQLException {
        ResultSet uniqueVaules = null;
        if (dto.getProvider().equals(Provider.SQLSERVER) || dto.getProvider().equals(Provider.POSTGRESQL)) {
            String schema = tableName.split("\\.")[0];
            String tabName = tableName.split("\\.")[1];
            uniqueVaules = metadata.getIndexInfo(null, schema, tabName, true, false);
        } else {
            uniqueVaules = metadata.getIndexInfo(null, null, tableName, true, false);
        }

        for (Column col : columns) {
            while (uniqueVaules.next()) {
                if (col.getName().equals(uniqueVaules.getString(COLUMN_NAME))) {
                    col.setUnique(true);
                }
            }
        }
    }

    private List<Index> getIndexes(DBSourceDto dto, DatabaseMetaData metadata, String tableName) throws SQLException {
        try {
            List<Index> indexes = new ArrayList<>();
            ResultSet indexValues = null;
            if (dto.getProvider().equals(Provider.SQLSERVER) || dto.getProvider().equals(Provider.POSTGRESQL)) {
                String schema = tableName.split("\\.")[0];
                String tabName = tableName.split("\\.")[1];
                indexValues = metadata.getIndexInfo(null, schema, tabName, false, false);
            } else {
                indexValues = metadata.getIndexInfo(null, null, tableName, false, false);
            }
            while (indexValues.next()) {
                Index index = new Index();
                String dbIndexName = indexValues.getString("INDEX_NAME");
                String dbColumnName = indexValues.getString(COLUMN_NAME);
                index.setName(dbIndexName);
                index.setColumn(dbColumnName);
                if (dbIndexName != null) {
                    indexes.add(index);
                }
            }
            return indexes;
        } catch (SQLException exception) {
            return new ArrayList<Index>();
        }
    }

    private List<Constraint> getConstraints(DatabaseMetaData metadata, DBSourceDto dto, String tableName)
            throws SQLException {
        ResultSet forignColumns = null;
        List<Constraint> constraints = new ArrayList<>();
        if (dto.getProvider().equals(Provider.SQLSERVER)) {
            String schema = tableName.split("\\.")[0];
            String tabName = tableName.split("\\.")[1];
            forignColumns = metadata.getImportedKeys(null, schema, tabName);

        } else {
            forignColumns = metadata.getImportedKeys(null, null, tableName);

        }
        while (forignColumns.next()) {
            int onDelete = Integer.parseInt(forignColumns.getString("DELETE_RULE"));
            int onUpdate = Integer.parseInt(forignColumns.getString("UPDATE_RULE"));
            Constraint constraint = new Constraint();
            constraint.setColumnName(forignColumns.getString("FKCOLUMN_NAME"));
            constraint.setRefColumn(forignColumns.getString("PKCOLUMN_NAME"));
            constraint.setRefTable(forignColumns.getString("PKTABLE_NAME"));
            constraint.setName(forignColumns.getString("FK_NAME"));
            constraint.setOnUpdate(setConstraintRule(onUpdate));
            constraint.setOnDelete(setConstraintRule(onDelete));
            constraints.add(constraint);
        }
        return constraints;
    }

    public DatabaseMetaData getMetadata(DBSourceDto dto) throws SQLException {
        String url = generateConnectionUrl(dto);
        Connection conn = DriverManager.getConnection(url);
        return conn.getMetaData();

    }

    public DBSourceDto getTables(DBSourceDto dto, String mode) {
        List<Table> tables = new ArrayList<>();
        try {
            DatabaseMetaData metadata = getMetadata(dto);
            ResultSet rs = metadata.getTables(dto.getDatabase(), null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                Table table = new Table();
                String tableName = getTableName(dto, rs);
                table.setName(tableName);
                switch (mode) {
                    // get tables names + columns
                    case "columns":
                        try {
                            table.setPrimaryKeys(getPrimaryKeys(tableName, metadata));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        try {
                            table.setColumns(getColumns(tableName, metadata));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        break;
                    // get tables names + columns + constraints
                    case "constraints":
                        try {
                            table.setPrimaryKeys(getPrimaryKeys(tableName, metadata));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        try {
                            table.setConstraints(getConstraints(metadata, dto, tableName));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        try {
                            table.setColumns(getColumns(tableName, metadata));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }

                        break;
                    // get tables + columns + constraints
                    case "all":
                        try {
                            table.setPrimaryKeys(getPrimaryKeys(tableName, metadata));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        try {
                            table.setConstraints(getConstraints(metadata, dto, tableName));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        try {
                            table.setIndexes(getIndexes(dto, metadata, tableName));
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        List<Column> columns = getColumns(tableName, metadata);
                        try {
                            getUniqueColumns(dto, metadata, columns, tableName);
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                        }
                        table.setColumns(getColumns(tableName, metadata));
                        break;
                    default:
                        break;
                }
                tables.add(table);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        CustomDatabase customDatabase = new CustomDatabase();
        customDatabase.setTables(tables);
        customDatabase.setName(dto.getDatabase());
        dto.setDatabases(Collections.singletonList(customDatabase));
        return dto;
    }

    private String insertUserQuery(String provider) {
        switch (provider) {

            case MY_SQL:
                return "INSERT IGNORE INTO `authentication_user` ( `firstname`, `lastname`, `username`, `password`, `enabled`) VALUES ('Administrator', 'Grizzly', 'admin', 'admin', 1); ";
            case MARIA_DB:
                return "INSERT IGNORE INTO `authentication_user` ( `firstname`, `lastname`, `username`, `password`, `enabled`) VALUES ('Administrator', 'Grizzly', 'admin', 'admin', 1); ";
            case "postgresql":
                return "INSERT INTO authentication_user ( firstname, lastname, username, password, enabled) VALUES ('Administrator', 'Grizzly', 'admin', 'admin', true) ON CONFLICT DO NOTHING;";
            case SQL_SERVER:
                return "IF NOT EXISTS (SELECT * from authentication_user) INSERT INTO authentication_user ( firstname, lastname, username, password, enabled) VALUES ('Administrator', 'Grizzly', 'admin', 'admin', 1); ";
            default:
                return null;
        }

    }

    private String insertRolesQuery(String provider) {
        switch (provider) {

            case MY_SQL:
                return "INSERT IGNORE INTO `authentication_roles` (`_id`, `name`) VALUES (1, 'admin');";
            case MARIA_DB:
                return "INSERT IGNORE INTO `authentication_roles` (`_id`, `name`) VALUES (1, 'admin');";
            case POSTGRESQL:
                return "INSERT INTO authentication_roles (_id, name) VALUES (1, 'admin') ON CONFLICT DO NOTHING ;";
            case SQL_SERVER:
                return "IF NOT EXISTS (SELECT * from authentication_roles)  INSERT INTO authentication_roles (name) VALUES ('admin');";
            default:
                return null;

        }

    }

    private String insertUserRolesQuery(String provider) {
        switch (provider) {

            case "mysql":
                return "INSERT IGNORE INTO `authentication_user_roles` (`user_id`, `role_name`) VALUES (1, 'admin');";
            case "mariadb":
                return "INSERT IGNORE INTO `authentication_user_roles` (`user_id`, `role_name`) VALUES (1, 'admin');";
            case POSTGRESQL:
                return "INSERT INTO authentication_user_roles (user_id, role_name) VALUES (1, 'admin') ON CONFLICT DO NOTHING;";
            case SQL_SERVER:
                return "IF NOT EXISTS (SELECT * from authentication_roles) INSERT INTO authentication_user_roles (user_id, role_name) VALUES (1, 'admin');";
            default:
                return null;
        }
    }

    public void insertAdmin(DBSource dbSource) throws SQLException {
        String insertUserQuery = this.insertUserQuery(dbSource.getProvider().name().toLowerCase());
        CustomQueryDto customQuery = new CustomQueryDto();
        customQuery.setQuery(insertUserQuery);
        this.executeQuery(dbSource.getId(), customQuery);

        String insertRolesQuery = this.insertRolesQuery(dbSource.getProvider().name().toLowerCase());
        customQuery.setQuery(insertRolesQuery);
        this.executeQuery(dbSource.getId(), customQuery);

        String insertUserRolesQuery = this.insertUserRolesQuery(dbSource.getProvider().name().toLowerCase());
        customQuery.setQuery(insertUserRolesQuery);
        this.executeQuery(dbSource.getId(), customQuery);

    }

    public void createFirstTable(DBSource dbSource) throws SQLException {
        String queryUser = this.generateUserQuery(dbSource.getProvider().name().toLowerCase());
        String queryRoles = this.generateRolesQuery(dbSource.getProvider().name().toLowerCase());
        String queryUserRoles = this.generateUserRolesQuery(dbSource.getProvider().name().toLowerCase());

        CustomQueryDto customQuery = new CustomQueryDto();
        customQuery.setQuery(queryUser);
        this.executeQuery(dbSource.getId(), customQuery);
        customQuery.setQuery(queryRoles);
        this.executeQuery(dbSource.getId(), customQuery);
        customQuery.setQuery(queryUserRoles);
        this.executeQuery(dbSource.getId(), customQuery);

        this.insertAdmin(dbSource);

    }

    public String generateUserQuery(String provider) {
        switch (provider) {
            case MY_SQL:
                return "CREATE TABLE IF NOT EXISTS `authentication_user` (`_id`  INT NOT NULL AUTO_INCREMENT , `firstname` varchar(20) , `lastname`  varchar(20) , `username` varchar(20) NOT NULL UNIQUE, `password`  varchar(20) NOT NULL ,`email` varchar(20) ,`phone` varchar(20) ,  `enabled`  boolean DEFAULT false , PRIMARY KEY ( `_id` ));";
            case MARIA_DB:
                return "CREATE TABLE IF NOT EXISTS `authentication_user` (`_id`  INT NOT NULL AUTO_INCREMENT , `firstname` varchar(20) , `lastname`  varchar(20) , `username` varchar(20) NOT NULL UNIQUE, `password`  varchar(20) NOT NULL ,`email` varchar(20) ,`phone` varchar(20) ,  `enabled`  boolean DEFAULT false , PRIMARY KEY ( `_id` ));";
            case POSTGRESQL:
                return "CREATE TABLE IF NOT EXISTS authentication_user (_id  SERIAL   , firstname varchar(20) , lastname  varchar(20) , username varchar(20) NOT NULL UNIQUE, password  varchar(20) NOT NULL , email varchar(20) , phone varchar(20) ,  enabled  boolean DEFAULT false , PRIMARY KEY( _id));";
            case SQL_SERVER:
                return "IF NOT EXISTS (select * from sys.tables where name='authentication_user') CREATE TABLE  authentication_user (_id  INT IDENTITY(1,1) PRIMARY KEY , firstname varchar(20) , lastname  varchar(20) , username varchar(20) NOT NULL UNIQUE, password  varchar(20) NOT NULL , email varchar(20) , phone varchar(20) ,  enabled  BIT  DEFAULT 0 ); ";
            default:
                return null;
        }
    }

    public String generateRolesQuery(String provider) {
        switch (provider) {
            case MY_SQL:
                return "CREATE TABLE IF NOT EXISTS `authentication_roles` (`_id`  INT NOT NULL AUTO_INCREMENT , `name` varchar(20) , PRIMARY KEY ( `_id` ));";
            case MARIA_DB:
                return "CREATE TABLE IF NOT EXISTS `authentication_roles` (`_id`  INT NOT NULL AUTO_INCREMENT , `name` varchar(20) , PRIMARY KEY ( `_id` ));";
            case POSTGRESQL:
                return "CREATE TABLE IF NOT EXISTS authentication_roles (_id SERIAL  , name varchar(20) , PRIMARY KEY (_id));";
            case SQL_SERVER:
                return "IF NOT EXISTS (select * from sys.tables where name='authentication_roles')CREATE TABLE authentication_roles (_id  INT IDENTITY(1,1) PRIMARY KEY  , name varchar(20));";
            default:
                return null;
        }
    }

    public String generateUserRolesQuery(String provider) {
        switch (provider) {
            case MY_SQL:
                return "CREATE TABLE IF NOT EXISTS `authentication_user_roles` (`user_id` INT NOT NULL , `role_name` varchar(20) NOT NULL ,  PRIMARY KEY ( `user_id` ,  `role_name`));";
            case MARIA_DB:
                return "CREATE TABLE IF NOT EXISTS `authentication_user_roles` (`user_id` INT NOT NULL , `role_name` varchar(20) NOT NULL ,  PRIMARY KEY ( `user_id` ,  `role_name`));";
            case POSTGRESQL:
                return "CREATE TABLE IF NOT EXISTS authentication_user_roles (user_id INT NOT NULL , role_name varchar(20) NOT NULL ,  PRIMARY KEY ( user_id ,  role_name));";
            case SQL_SERVER:
                return "IF NOT EXISTS (select * from sys.tables where name='authentication_user_roles') CREATE TABLE  authentication_user_roles (user_id INT NOT NULL , role_name varchar(20) NOT NULL ,  PRIMARY KEY ( user_id ,  role_name));";
            default:
                return null;
        }
    }

    private String setConstraintRule(int code) {
        switch (code) {
            case 0:
                return "CASCADE";
            case 1:
                return "RESTRICT";
            case 2:
                return "SET NULL";
            case 3:
                return "NO ACTION";
            default:
                return "NO ACTION";
        }
    }

    private String generateConnectionUrl(DBSourceDto dto) {
        String url = "";
        if (dto.getProvider().name().equalsIgnoreCase(SQL_SERVER)) {
            url = "jdbc:" + getProviderName(dto.getProvider()) + "://" + dto.getHost() + ":" + dto.getPort()
                    + ";databaseName=" + dto.getDatabase() + ";user=" + dto.getUsername() + ";password="
                    + getSafeValue(dto.getPassword()) + "&useSSL=false";
        } else {
            url = "jdbc:" + getProviderName(dto.getProvider()) + "://" + dto.getHost() + ":" + dto.getPort() + "/"
                    + dto.getDatabase() + "?user=" + dto.getUsername() + "&password=" + getSafeValue(dto.getPassword()) + "&useSSL=false";
        }
        return url;
    }

    private String getProviderName(Provider provider) {
        switch (provider) {
            case MYSQL:
                return MY_SQL;
            case MARIADB:
                return MY_SQL;
            case POSTGRESQL:
                return POSTGRESQL;
            case SQLSERVER:
                return "sqlserver";
            case AS400:
                return "as400";
            case DB2:
                return "db2:test";
            default:
                return null;
        }
    }

    public void executeQuery(String dbSourceId, CustomQueryDto customQueryDto) {
        JdbcTemplate jt = sqlCacheService.prepareDatasourceClient(dbSourceId);
        jt.execute(customQueryDto.getQuery());
    }

    public String getSafeValue(char[] pwd) {
        return pwd == null ? "" : String.valueOf(pwd);
    }

}
