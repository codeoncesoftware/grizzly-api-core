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
package fr.codeonce.grizzly.core.service.analytics;

import fr.codeonce.grizzly.common.runtime.HealthCheck;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.analytics.Analytics;
import fr.codeonce.grizzly.core.domain.analytics.AnalyticsRepository;
import fr.codeonce.grizzly.core.domain.analytics.ApiCount;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.domain.user.AccountType;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.DBSourceMapper;
import fr.codeonce.grizzly.core.service.datasource.DBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceStatsService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private DBSourceService dbsourceService;

    @Autowired
    private UserService userService;

    @Autowired
    private DBSourceMapper dbSourceMapper;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private MongoDBSourceStatsService dbSourceStatsService;

    @Autowired
    private MongoCacheService cache;

    @Autowired
    private UserRepository userRepository;

    @Value("${offres.free.msCount : 10}")
    private long freeMsCount;

    @Value("${offres.limitless.msCount : 80}")
    private long limitlessMsCount;

    @Value("${offres.scalability.msCount : 10}")
    private long scalabilityMsCount;

    @Value("${offres.free.maxStorage : 500000}")
    private double freeMaxStorage;

    @Value("${offres.limitless.maxStorage : 50000000}")
    private double limitlessMaxStorage;

    @Value("${offres.scalability.maxStorage : 5000000}")
    private double scalabilityMaxStorage;

    @Autowired
    private CryptoHelper encryption;

    private static final String STORAGE = "storage";

    public AnalyticsDto getAnalytics() {
        AnalyticsDto analyticsDto = new AnalyticsDto();
        String currentUserEmail = userService.getConnectedUserEmail();

        // API COUNTS
        List<Analytics> allAnalytics = analyticsRepository.findByEmail(currentUserEmail);
        List<AnalyticsApiCountDTO> apiCounts = allAnalytics.stream()//
                .flatMap(a -> a.getApiCounts().stream())//
                .collect(Collectors.groupingBy(ApiCount::getName, Collectors.summingLong(ApiCount::getCount)))//
                .entrySet().stream()//
                .map(e -> new AnalyticsApiCountDTO(e.getKey(), e.getValue())).collect(Collectors.toList());

        analyticsDto.setApiCounts(apiCounts);

        // REQUEST COUNT
        Long requestCount = allAnalytics.stream()//
                .map(Analytics::getRequestCount)//
                .reduce(0L, Long::sum);
        analyticsDto.setRequestCount(requestCount);

        // MS COUNT
        Long msCount = projectRepository.countByUserEmail(currentUserEmail);
        analyticsDto.setMsCount(msCount);

        // DB COUNT
        Long dbCount = dbSourceRepository.countByUserEmail(currentUserEmail);
        analyticsDto.setDbCount(dbCount);

        // IDP COUNT
        Long idpCount = identityProviderRepository.countByUserEmail(currentUserEmail);
        analyticsDto.setIdpCount(idpCount);

        // COMPUTE DATA METRICS
        List<DBSource> dbSourceList = dbSourceRepository.findAllByUserEmail(currentUserEmail).stream().map(d -> {
            encryption.decrypt(d);
            return d;
        }).filter(db -> db.provider.equals(Provider.MONGO)).collect(Collectors.toList());
        AnalyticsDataDto data = analyticsDto.getData();

        data.setTotalStored(computeDataSize(dbSourceList));
        data.setStoredFile(computeStoredFile(dbSourceList));
        data.setStoredContent(Math.round(data.getTotalStored() - data.getStoredFile()));

        // RETURN RESULT
        return analyticsDto;

    }

    private double computeStoredFile(List<DBSource> dbSourceList) {
        double totalFiles = 0;
        double totalChunks = 0;

        totalFiles = dbSourceList.stream().filter(db -> cache.getMongoClient(db) != null).map(d -> {
            try {
                if (cache.getMongoClient(d).getDatabase(d.getDatabase()).listCollectionNames().into(new ArrayList<String>())
                        .contains("fs.files")) {
                    return dbSourceStatsService.getCollectionStats(d, d.getDatabase(), "fs.files");
                } else {
                    return new Document("size", 0);
                }
            } catch (RuntimeException e) {
                return new Document("size", 0);
            }

        }).map(d -> d.get("size")).filter(Objects::nonNull).map(d -> (int) d).reduce(0, Integer::sum);
        totalChunks = dbSourceList.stream().filter(db -> cache.getMongoClient(db) != null).map(d -> {
            try {
                if (cache.getMongoClient(d).getDatabase(d.getDatabase()).listCollectionNames().into(new ArrayList<String>())
                        .contains("fs.chunks")) {
                    return dbSourceStatsService.getCollectionStats(d, d.getDatabase(), "fs.chunks");
                } else {
                    return new Document("size", 0);
                }
            } catch (RuntimeException e) {
                return new Document("size", 0);
            }

        }).map(d -> d.get("size")).filter(Objects::nonNull).map(d -> (int) d).reduce(0, Integer::sum);
        return totalFiles + totalChunks;
    }

    private double computeDataSize(List<DBSource> dbSourceList) {
        return Math.round(dbSourceList.stream()//
                .map(d -> dbSourceStatsService.getDbStats(d, d.getDatabase()))//
                .filter(Objects::nonNull)
                .map(d -> Double.valueOf(String.valueOf(d.get("dataSize"))))//
                .reduce(0D, Double::sum));
    }

    public void updateContainerMetrics(Container container) {

        CompletableFuture.runAsync(() -> {
            Map<String, Long> apiCounts = container.getResources().stream()
                    .collect(Collectors.groupingBy(Resource::getExecutionType, Collectors.counting()));

            Optional<Analytics> analytics = analyticsRepository.findByContainerId(container.getId());

            if (analytics.isEmpty()) {
                analytics = Optional.of(new Analytics());
            }

            analytics.ifPresent(a -> {
                a.setContainerId(container.getId());
                String currentUserEmail = userService.getConnectedUserEmail();
                a.setEmail(currentUserEmail);
                a.setApiCounts(apiCounts.entrySet().stream().map(e -> new ApiCount(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()));
                analyticsRepository.save(a);
            });
        });
    }

    public void removeContainerAnalytics(String containerId) {
        analyticsRepository.deleteByContainerId(containerId);
    }

    public void updateRequestCount(String containerId) {
        Optional<Analytics> analytics = analyticsRepository.findByContainerId(containerId);

        if (analytics.isEmpty()) {
            analytics = Optional.of(new Analytics());
        }

        analytics.ifPresent(a -> {
            a.setContainerId(containerId);
            a.setRequestCount(a.getRequestCount() + 1);
            analyticsRepository.save(a);
        });
    }

    public Document checkUserLimits() {

        AnalyticsDto analytics = getAnalytics();
        long msCount = analytics.getMsCount();
        long dbCount = analytics.getDbCount();
        double storage = analytics.getData().getTotalStored();
        String currentUserEmail = userService.getConnectedUserEmail();
        Document metrics = new Document().append("ms", true).append(STORAGE, true).append("db", true);

        return this.userRepository.findByEmail(currentUserEmail).map(user -> {
            log.info(user.getEmail());
            if (user.getAccountType().equals(AccountType.FREE)) {
                if (msCount >= freeMsCount) {
                    metrics.put("ms", false);
                }
                if (storage > freeMaxStorage) {
                    metrics.put(STORAGE, false);
                }
                if (dbCount >= freeMsCount) {
                    metrics.put("db", false);
                }

            } else if (user.getAccountType().equals(AccountType.LIMITLESS)) {
                if (msCount >= limitlessMsCount) {
                    metrics.put("ms", false);
                }
                if (storage > limitlessMaxStorage) {
                    metrics.put(STORAGE, false);
                }
                if (dbCount >= limitlessMsCount) {
                    metrics.put("db", false);
                }
            } else if (user.getAccountType().equals(AccountType.SCALABILITY)) {
                if (msCount >= scalabilityMsCount) {
                    metrics.put("ms", false);
                }
                if (storage > scalabilityMaxStorage) {
                    metrics.put(STORAGE, false);
                }
                if (dbCount >= scalabilityMsCount) {
                    metrics.put("db", false);
                }
            }

            return metrics;

        }).orElseThrow(GlobalExceptionUtil.notFoundException(User.class, currentUserEmail));
    }

    public boolean storageLimitsOnUpload(long fileSize) {

        AnalyticsDto analytics = getAnalytics();
        double storage = analytics.getData().getTotalStored();
        String currentUserEmail = userService.getConnectedUserEmail();

        User user = this.userRepository.findByEmail(currentUserEmail)
                .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, currentUserEmail));

        double newStorage = storage + fileSize;

        return !((user.getAccountType().equals(AccountType.FREE) && (newStorage >= freeMaxStorage)) || (user
                .getAccountType().equals(AccountType.LIMITLESS) && (newStorage >= limitlessMaxStorage)
                || (user.getAccountType().equals(AccountType.SCALABILITY) && (newStorage >= scalabilityMaxStorage))));
    }

    public HealthCheck getHealthCheck(String containerId) throws SQLException {
        HealthCheck healthCheck = new HealthCheck();
        Container container = containerRepository.findById(containerId)//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        DBSource db = dbSourceRepository.findById(container.getDbsourceId())//
                .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, container.getDbsourceId()));
        encryption.decrypt(db);
        DBSourceDto dbSourceDto = dbSourceMapper.mapToDto(db);
        healthCheck.setVersion(container.getName());
        healthCheck.setDatabase(dbSourceDto.getName());
        Boolean status = dbsourceService.checkTempConnection(dbSourceDto);
        if (status) {
            healthCheck.setStatus("OK");
        } else {
            healthCheck.setStatus("KO");
        }
        Project p = projectRepository.findById(container.getProjectId()).orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, container.getProjectId()));
        healthCheck.setMicroservice(p.getName());
        return healthCheck;
    }
}
