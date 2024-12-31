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
package fr.codeonce.grizzly.core.service.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.common.runtime.SecurityApiConfig;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.analytics.ProjectUse;
import fr.codeonce.grizzly.core.domain.analytics.ProjectUseRepository;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.enums.SecurityLevel;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.ResourceGroup;
import fr.codeonce.grizzly.core.domain.user.AccountType;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.container.ContainerDto;
import fr.codeonce.grizzly.core.service.container.ContainerMapperService;
import fr.codeonce.grizzly.core.service.container.ContainerService;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.datasource.sql.SqlDBSourceService;
import fr.codeonce.grizzly.core.service.datasource.sql.mapper.SqlDBSourceMapperService;
import fr.codeonce.grizzly.core.service.fs.GitHandler;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.CryptoHelper;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final String AUTHENTICATION_MICROSERVICE = "authentication microservice";

    @Autowired
    private Environment environment;

    @Autowired
    private CryptoHelper encryption;

    @Autowired
    private ProjectExample projectExample;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectUseRepository projectUseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private MongoDBSourceMapperService dbSourceMapperService;

    @Autowired
    private GitHandler gitHandler;

    @Autowired
    private MongoCacheService cacheService;

    @Autowired
    private MongoDBSourceService dbSourceService;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ProjectMapper mapper;

    @Autowired
    private ContainerMapperService containerMapper;

    @Autowired
    private SqlDBSourceService sqlDBSourceService;

    @Autowired
    private SqlDBSourceMapperService sqlmapper;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;

    @Value("${frontUrl}")
    private String url;

    @Autowired
    private AnalyticsService analyticsService;

    public static KeyPair generateRSAKkeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Creates a new project in the database with the given project DTO
     *
     * @param projectDto
     * @return ProjectDto
     * @throws NoSuchAlgorithmException
     * @throws CustomGitAPIException
     * @throws Exception
     */
    public ProjectDto createProject(ProjectDto projectDto) throws NoSuchAlgorithmException, CustomGitAPIException {
        String currentUserEmail = userService.getConnectedUserEmail();
        if (currentUserEmail == null) {
            currentUserEmail = projectDto.getUserEmail();
        }
        projectDto.setUserEmail(currentUserEmail);
        DBSourceDto savedDataSource = new DBSourceDto();
        // MAP DTO
        Project project = mapper.mapToDomain(projectDto);
        project.setSecurityEnabled(projectDto.isSecurityEnabled());
        KeyPair keypair = generateRSAKkeyPair();
        setProjectSecurityCredentials(projectDto, project);
        if (projectDto.getType().equals(AUTHENTICATION_MICROSERVICE)) {
            project.setType(AUTHENTICATION_MICROSERVICE);
            //project.setUserManagementEnabled(projectDto.isUserManagementEnabled());
            project.getSecurityConfig().setPrivateKey(Base64.getEncoder().encodeToString(keypair.getPrivate().getEncoded()));
            project.getSecurityConfig().setPublicKey(Base64.getEncoder().encodeToString(keypair.getPublic().getEncoded()));
            project.setIdentityProviderIds(projectDto.getIdentityProviderIds());
            if (project.getRoles().isEmpty()) {
                if (project.getIdentityProviderIds() != null && projectDto.getIdentityProviderIds().get(0).equalsIgnoreCase(IdentityProviders.GRIZZLY.toString())) {
                    project.getRoles().add(SecurityLevel.ADMIN.getValue());
                    project.getRoles().add("user");
                } else {
                    project.getRoles().add("authenticated user");
                }
            }
        }

        project.setAuthMSRuntimeURL(projectDto.getAuthMSRuntimeURL());
        project.setIamDelegatedSecurity(projectDto.getIamDelegatedSecurity());
        Project savedProject = projectRepository.save(project);

        // Create Default Version of Container V1
        Container containerTosave = new Container();
        containerTosave.setName("1.0.0");
        containerTosave.setDescription(projectDto.getDescription());
        containerTosave.setProjectId(savedProject.getId());

        // Set the Swagger Salt
        if (StringUtils.isBlank(containerTosave.getSwaggerUuid())) {
            containerTosave.setSwaggerUuid(UUID.randomUUID().toString().substring(0, 8));
        }
        ResourceGroup security = new ResourceGroup();
        containerTosave.setDbsourceId(savedProject.getDbsourceId());
        if ((!projectDto.getType().equals(AUTHENTICATION_MICROSERVICE)) || (projectDto.getType().equals(AUTHENTICATION_MICROSERVICE) && projectDto.getIdentityProviderIds() != null && projectDto.getIdentityProviderIds().get(0).equalsIgnoreCase(IdentityProviders.GRIZZLY.toString()))) {
            DBSource db = dbSourceRepository.findById(savedProject.getDbsourceId())
                    .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, savedProject.getDbsourceId()));
            if (db.getType().equals("nosql")) {
                // Set Database Name
                if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                    containerTosave.setDatabaseName(db.getPhysicalDatabase());
                } else {
                    containerTosave.setDatabaseName(savedProject.getDatabaseName());
                    if (!dbSourceMapperService.mapToDto(db).isDockerMode()
                            && savedProject.getIamDelegatedSecurity() == null) {
                        if (db.getProvider().equals(Provider.MONGO)) {
                            this.dbSourceService.addFirstCollections(this.cacheService.getMongoClient(db.getId()),
                                    containerTosave.getDatabaseName(), "authentication_user");
                        }
                    }
                }
            } else {
                if (!sqlmapper.mapToDto(db).isDockerMode() && savedProject.getIamDelegatedSecurity() == null
                        && db.getDatabases() != null &&
                        db.getDatabases().stream().filter(
                                        d -> d.getTables().stream().anyMatch(t -> t.getName().equals("authentication_user")))
                                .findFirst().isEmpty()) {
                    encryption.decrypt(db);
                    DBSourceDto dto = sqlmapper.mapToDto(db);
                    dto.setAuthDBEnabled(true);
                    try {
                        sqlDBSourceService.createFirstTable(db);
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                    }
                    dto = sqlDBSourceService.getTables(dto, "all");
                    DBSource sqlSource = sqlmapper.mapToDomain(dto);
                    db = dbSourceRepository.save(sqlSource);
                }

            }

            savedDataSource = dbSourceMapperService.mapToDto(db);

        }

        containerService.saveResources(containerTosave, projectDto, savedDataSource, security, currentUserEmail);
        // Save Container
        Container container = containerRepository.save(containerTosave);

        // SET HOST + BasePath
        container.setHost(
                environment.getProperty("frontUrl").substring(environment.getProperty("frontUrl").indexOf("//") + 2));
        container.setBasePath("/runtime/" + container.getId());
        container = containerRepository.save(container);
        savedProject.setRuntimeUrl(url + "/runtime/" + container.getId());
        Project finalProject = projectRepository.save(savedProject);

        // UPDATE METRICS
        analyticsService.updateContainerMetrics(containerTosave);

        // Synchronize GIT REPO
        if (savedProject.getGitUrl() != null) {
            gitHandler.gitFirstSync(projectDto, Collections.emptyList(), project.getId(), container.getId(),
                    savedProject.getDbsourceId(), savedProject.getDatabaseName());
        }
        // RETURN DTO
        return mapper.mapToDto(finalProject);

    }

    private Project setProjectSecurityCredentials(ProjectDto projectDto, Project project) {
        project.getSecurityConfig().setTokenExpiration(3600);
        if (projectDto.isSecurityEnabled() && !projectDto.getType().equals(AUTHENTICATION_MICROSERVICE)) {
            if (projectDto.getAuthMSRuntimeURL() == null) {
                // SAVE NEW
                project.getSecurityConfig().setClientId(project.getName());
                project.getSecurityConfig()
                        .setSecretKey(DigestUtils.sha256Hex(project.getName()) + DigestUtils.sha256Hex("co%de01/"));
                if (project.getRoles().isEmpty()) {
                    project.getRoles().add(SecurityLevel.ADMIN.getValue());
                    project.getRoles().add("user");
                }
            } else {
                project.getSecurityConfig().setClientId(projectDto.getSecurityConfig().getClientId());
                project.getSecurityConfig().setSecretKey(projectDto.getSecurityConfig().getSecretKey());
            }

        }
        return project;
    }

    /**
     * Returns a single project with its given id
     *
     * @param id
     * @return ProjectDto
     */
    public ProjectDto get(String id) {
        return projectRepository.findById(id)//
                .map(p -> mapper.mapToDto(p))//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, id));
    }

    /**
     * return a list of all the projects in the database
     *
     * @return List<ProjectDto>
     */
    public List<ProjectDto> getAll() {
        return projectRepository.findAll().stream()// GET ALL
                .map(p -> mapper.mapToDto(p))// MAP ALL TO DTOs
                .collect(Collectors.toList());
    }

    /**
     * updates the old project with its given id and the new project DTO
     *
     * @param newProjectDto
     * @param projectId
     * @return ProjectDto
     */
    public ProjectDto update(ProjectDto newProjectDto, String projectId) {
        return projectRepository.findById(newProjectDto.getId()).map(p -> {
            if (p.getDbsourceId() != null && !p.getDbsourceId().equals(newProjectDto.getDbsourceId())) {
                this.containerService.updateResourcesDbsourceId(newProjectDto.getId(), newProjectDto.getDbsourceId());
            }
            this.containerService.updateResources(newProjectDto, projectId);
            mapper.mapToDomainUpdate(newProjectDto, p);// MAP TO DOMAIN
            setProjectSecurityCredentials(newProjectDto, p);
            p.getSecurityConfig().setTokenExpiration(newProjectDto.getSecurityConfig().getTokenExpiration());
            projectRepository.save(p);// UPDATE ENTITY
            if (p.getDbsourceId() != null) {
                updateRelatedContainers(p.getId(), p.getDbsourceId(), p.getDatabaseName());
            }
            if ((p.getGitBranch() != null && !p.getGitBranch().equals(newProjectDto.getGitBranch()))
                    || (p.getGitToken() != null && !p.getGitToken().equals(newProjectDto.getGitToken()))
                    || (p.getGitLocalpath() != null && !p.getGitLocalpath().equals(newProjectDto.getGitLocalpath()))
                    || (p.getGitUsername() != null && !p.getGitUsername().equals(newProjectDto.getGitUsername()))
                    || (p.getGitPassword() != null && !p.getGitPassword().equals(newProjectDto.getGitPassword()))
                    || (p.getGitUrl() != null && !p.getGitUrl().equals(newProjectDto.getGitUrl()))) {
                gitFirstSyncProject(newProjectDto);
            }

            return mapper.mapToDto(p);// RETURN DTO
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, projectId));

    }

    /**
     * return a list of the projects with a given microservice type
     *
     * @param type
     * @return ProjectDto
     */
    public List<ProjectDto> getByType(String type) {
        String currentUserEmail = userService.getConnectedUserEmail();
        return projectRepository.findByTypeIgnoreCaseAndUserEmail(type, currentUserEmail).stream()// GET ALL
                .map(p -> mapper.mapToDto(p))// MAP ALL TO DTOs
                .collect(Collectors.toList());
    }

    /**
     * Update the Database Name for the Related Containers to a Project
     *
     * @param id of the Project
     */
    private void updateRelatedContainers(String projectId, String dbsourceId, String databaseName) {
        String name = "dbName";
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, mongoDatabase);
        Map<String, String> dbName = new HashMap<>();
        dbName.put(name, databaseName);
        this.dbSourceRepository.findById(dbsourceId).ifPresent(db -> {
            if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                dbName.put(name, db.getPhysicalDatabase());
            }
            Update update = new Update();
            update.set("enabled", true);
            update.set("dbsourceId", dbsourceId);
            update.set("databaseName", dbName.get(name));
            mongoTemplate.updateMulti(Query.query(Criteria.where("projectId").is(projectId)), update, Container.class);

        });

    }

    /**
     * deletes a single project with its given id
     *
     * @param id
     * @return
     */

    @Transactional
    public void delete(String projectId) {
        projectRepository.findById(projectId).ifPresentOrElse(p -> {
            projectRepository.delete(p);

            // delete all containers
            containerService.deleteContainersUnderProject(projectId);

        }, () -> {
            throw GlobalExceptionUtil.notFoundException(Project.class, projectId).get();
        });

    }

    /**
     * Deletes all the projects in the database
     *
     * @return
     */
    public void deleteAll() {
        // Const.DELETE.getValue() ALL
        projectRepository.deleteAll();

    }

    /**
     * Check if the given project name is taken
     *
     * @param projectName
     * @return true if a project with the given name already exists, false if not
     */
    public boolean existsProjectName(String projectName, String projectId) {
        // CHECK PROJECT NAME UNICITY
        boolean[] exists = new boolean[]{false};
        String currentUserEmail = userService.getConnectedUserEmail();
        this.projectRepository.findByNameIgnoreCaseAndUserEmail(projectName, currentUserEmail).ifPresent(proj -> {
            if (projectId != null && proj.getId().equals(projectId)) {
                exists[0] = false;
            } else {
                exists[0] = true;
            }
        });
        return exists[0];
    }

    /**
     * Export project in JSON File
     *
     * @param projectId
     * @return JSON Configuration File
     * @throws IOException
     */
    public String exportProject(String projectId) throws IOException {
        StringBuilder projectJson = new StringBuilder();
        ProjectDto projectDto = projectRepository.findById(projectId)//
                .map(p -> mapper.mapToDto(p))//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, projectId));
        ObjectMapper jsonMapper = new ObjectMapper();
        projectJson.append(jsonMapper.writeValueAsString(projectDto));
        projectJson.append(',');
        List<ContainerDto> containersList = containerRepository.findAll().stream()//
                .map(c -> containerMapper.mapToDto(c))//
                .collect(Collectors.toList());
        containersList.forEach(container -> {
            try {
                projectJson.append(jsonMapper.writeValueAsString(container));
                FileWriter file = new FileWriter("testApi.json");
                file.write(projectJson.toString());
                file.close();

            } catch (Exception e) {
                log.debug("{}", e);
            }
        });
        return projectJson.toString();
    }

    private boolean containsName(final List<Project> list, final String name) {
        return list.stream().map(Project::getName).filter(name::equalsIgnoreCase).findFirst().isPresent();
    }

    /**
     * return a list of all the projects of a given user
     *
     * @param userEmail
     * @return List<ProjectDto>
     */
    public List<ProjectDto> getAllByUser() {
        String currentUserEmail = userService.getConnectedUserEmail();
        List<Project> allProjects = projectRepository.findAll();
        List<Project> projects = new ArrayList<Project>();
        List<Project> personnalProjects = projectRepository.findAllByUserEmail(currentUserEmail);

        if (memberRepository.findByEmail(currentUserEmail) != null) {
            List<String> memberTeams = memberRepository.findByEmail(currentUserEmail).getTeamIds();
            for (int i = 0; i < memberTeams.size(); i++) {
                String TeamId = memberTeams.get(i);
                for (int j = 0; j < allProjects.size(); j++) {
                    Project project = allProjects.get(j);
                    if (project.getTeamIds().contains(TeamId)) {
                        projects.add(project);
                    }
                }
            }

            personnalProjects.forEach(proj -> {
                if (!containsName(projects, proj.getName())) {
                    projects.add(proj);
                }
            });
            for (int i = 0; i < projects.size(); i++) {
                Project p = projects.get(i);
                p.setOrganizationId(memberRepository.findByEmail(currentUserEmail).getOrganisationId());
                if (p.getIdentityProviderIds() == null) { // In case of projects implemeted on previous versions
                    p.setSecurityEnabled(true);
                    p.setIdentityProviderIds(Collections.emptyList());
                }
                projectRepository.save(p);
            }

            return projects.stream()// GET ALL BY USER
                    .map(p -> mapper.mapToDto(p))// MAP ALL TO DTOs
                    .collect(Collectors.toList());
        } else {
            for (int i = 0; i < personnalProjects.size(); i++) {
                Project p = personnalProjects.get(i);
                p.setOrganizationId(null);
                if (p.getIdentityProviderIds() == null) { // In case of projects implemeted on previous versions
                    p.setSecurityEnabled(true);
                    p.setIdentityProviderIds(Collections.emptyList());
                }
                projectRepository.save(p);
            }
            return personnalProjects.stream()// GET ALL BY USER
                    .map(p -> mapper.mapToDto(p))// MAP ALL TO DTOs
                    .collect(Collectors.toList());
        }

    }

    public Boolean existsProjectByDbSourceName(String databaseName) {
        return projectRepository.existsByDatabaseName(databaseName);
    }

    public String deleteByNameAndUserEmail(String name, String userEmail) {
        projectRepository.deleteByNameAndUserEmail(name, userEmail);
        return name;
    }

    public void addProjectType() {
        projectRepository.findAll().forEach(el -> {
            if (el.getType() == null) {
                el.setType("markup microservice");
                projectRepository.save(el);
            }
        });

    }

    public Container cloneProjectAndContainer(String projectId, String containerId) {
        Project oldProject = projectRepository.findById(projectId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, projectId));
        oldProject.setId(null);
        oldProject.setName("Copy of " + oldProject.getName());
        Project newProject = projectRepository.save(oldProject);
        Container currentContainer = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        currentContainer.setId(null);
        currentContainer.setProjectId(newProject.getId());
        return containerRepository.save(currentContainer);
    }

    public int countUsedPojectByUserEmail(String userEmail) {
        return this.projectRepository.findByUserEmailAndUsed(userEmail, true).size();
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void getUsedProject() {
        this.projectRepository.findAll().forEach(project -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastUpdated = LocalDateTime.ofInstant(project.getLastUpdate().toInstant(),
                    ZoneId.systemDefault());
            long unit = ChronoUnit.SECONDS.between(lastUpdated, now);
            if (unit < 86400) {
                Optional<User> userOp = userRepository.findByEmail(project.getUserEmail());
                if (userOp.isPresent()) {
                    User user = userOp.get();
                    if (user.getAccountType().equals(AccountType.SCALABILITY)) {
                        if (!projectUseRepository.existsByProjectId(project.getId())) {
                            ProjectUse projectUse = new ProjectUse(project.getId(), user.getEmail(),
                                    project.getLastUpdate());
                            projectUseRepository.save(projectUse);
                        } else {
                            ProjectUse projectUse = projectUseRepository.findByProjectId(project.getId());
                            List<Container> containers = containerRepository.findAllByProjectId(project.getId())
                                    .stream().sorted((o1, o2) -> o1.getLastUpdate().compareTo(o2.getLastUpdate()))
                                    .collect(Collectors.toList());
                            projectUse.setUseDate(containers.get(containers.size() - 1).getLastUpdate());
                            projectUseRepository.save(projectUse);

                        }
                    }
                }
            }
        });
    }

    public List<ProjectUse> getAllProjectUsed() {
        return this.projectUseRepository.findAll();
    }

    public EstimationDto getUsedProjectsInCurrentMonth(User user) {
        EstimationDto estimation = new EstimationDto();
        List<String> ids = new ArrayList<>();
        if (user.getNextInvoiceDate() != null) {
            this.projectUseRepository.findAll().forEach(pr -> {
                LocalDateTime localDateTime = user.getNextInvoiceDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                localDateTime = localDateTime.minusDays(30);
                Date currentDateMinus30Days = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                if (pr.getUserEmail().equals(user.getEmail()) && currentDateMinus30Days.compareTo(pr.getUseDate()) * pr.getUseDate().compareTo(user.getNextInvoiceDate()) >= 0) {
                    ids.add(pr.getId());
                }
            });
        }

        return estimation;
    }

    public boolean checkGitcredentials(ProjectDto projectDto) {
        return gitHandler.checkCredentials(projectDto);
    }

    public ProjectDto gitSyncProject(ProjectDto projectDto) {
        containerRepository.findAllByProjectId(projectDto.getId()).forEach(container -> {
            try {
                gitHandler.gitSync(projectDto, Collections.emptyList(), projectDto.getId(), container.getId(),
                        projectDto.getDbsourceId(), projectDto.getDatabaseName());
            } catch (CustomGitAPIException e) {
                e.printStackTrace();
            }
        });

        return projectDto;
    }

    public ProjectDto gitFirstSyncProject(ProjectDto projectDto) {
        containerRepository.findAllByProjectId(projectDto.getId()).forEach(container -> {
            try {
                gitHandler.gitFirstSync(projectDto, Collections.emptyList(), projectDto.getId(), container.getId(),
                        projectDto.getDbsourceId(), projectDto.getDatabaseName());
            } catch (CustomGitAPIException e) {
                e.printStackTrace();
            }
        });

        return projectDto;
    }

    public ProjectDto addApp(String projectId, String appName) {
        return projectRepository.findById(projectId).map(p -> {
            SecurityApiConfig security = new SecurityApiConfig();
            security.setClientId(appName);
            security.setSecretKey(DigestUtils.sha256Hex(appName).substring(0, 16));
            if (p.getAuthorizedApps() != null) {
                p.getAuthorizedApps().add(security);
            } else { // in case of adding the first app
                List<SecurityApiConfig> securityList = new ArrayList<>();
                securityList.add(security);
                p.setAuthorizedApps(securityList);
            }
            projectRepository.save(p);// UPDATE ENTITY
            return mapper.mapToDto(p);// RETURN DTO
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, projectId));

    }

    public ProjectDto deleteApp(String projectId, String clientId) {
        return projectRepository.findById(projectId).map(p -> {
            p.getAuthorizedApps().removeIf(f -> f.getClientId().equals(clientId));
            projectRepository.save(p);// UPDATE ENTITY
            return mapper.mapToDto(p);// RETURN DTO
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, projectId));
    }
}
