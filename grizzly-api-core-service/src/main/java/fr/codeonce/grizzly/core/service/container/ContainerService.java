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
package fr.codeonce.grizzly.core.service.container;

import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.container.Server;
import fr.codeonce.grizzly.core.domain.container.hierarchy.ContainerHierarchy;
import fr.codeonce.grizzly.core.domain.container.hierarchy.ContainerHierarchyRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import fr.codeonce.grizzly.core.domain.endpointModel.ModelProperty;
import fr.codeonce.grizzly.core.domain.enums.SecurityLevel;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.project.SecurityApiConfig;
import fr.codeonce.grizzly.core.domain.resource.CustomQuery;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.domain.resource.ResourceGroup;
import fr.codeonce.grizzly.core.domain.resource.ResourceRequestModel;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.DBSourceMapper;
import fr.codeonce.grizzly.core.service.datasource.DBSourceService;
import fr.codeonce.grizzly.core.service.datasource.mongo.mapper.MongoDBSourceMapperService;
import fr.codeonce.grizzly.core.service.datasource.sql.SqlDBSourceService;
import fr.codeonce.grizzly.core.service.fs.FilesHandler;
import fr.codeonce.grizzly.core.service.fs.GitHandler;
import fr.codeonce.grizzly.core.service.identityprovider.IdentityProviderDto;
import fr.codeonce.grizzly.core.service.log.LogService;
import fr.codeonce.grizzly.core.service.project.ProjectDto;
import fr.codeonce.grizzly.core.service.project.ProjectExample;
import fr.codeonce.grizzly.core.service.project.ProjectMapper;
import fr.codeonce.grizzly.core.service.project.ProjectService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class ContainerService {
    private static final Logger log = LoggerFactory.getLogger(ContainerService.class);

    @Autowired
    Environment environment;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private ProjectExample projectExample;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ContainerMapperService containerMapper;

    @Autowired
    private ContainerExportService containerExportService;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private ContainerHierarchyRepository hierarchyRepository;

    @Autowired
    private FilesHandler filesHandler;

    @Autowired
    private GitHandler gitHandler;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private DBSourceMapper dbSourceMapper;

    @Autowired
    private SqlDBSourceService sqlDBSourceService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DBSourceService dbSourceService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LogService logService;

    @Autowired
    private MongoDBSourceMapperService dbSourceMapperService;

    IdentityProviderDto savedIdentityProvider = new IdentityProviderDto();


    @Value("${frontUrl}")
    private String frontUrl;

    /**
     * Creates a new container or updates the existing one given a container DTO
     *
     * @param containerDto
     * @return ContainerDto
     * @throws CustomGitAPIException
     */
    public ContainerDto saveContainer(ContainerDto containerDto, boolean integrate) throws CustomGitAPIException {
        String projectId = containerDto.getProjectId();
        Optional<Project> p = projectRepository.findById(projectId);

        // MAP DTO
        if (p.isPresent()) {

            // set Container to save
            Container containerToSave = containerMapper.mapToDomain(containerDto);

            // set log if there is no log assigned to the container

            // If a DB is linked
            if (p.get().getDbsourceId() != null) {
                this.dbSourceRepository.findById(p.get().getDbsourceId()).ifPresent(db -> {
                    containerToSave.setDbsourceId(db.getId());
                    if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                        containerToSave.setDatabaseName(db.getPhysicalDatabase());
                    } else {
                        containerToSave.setDatabaseName(p.get().getDatabaseName());
                    }
                });
            }

            if (containerDto.getId() == null) { // Container creation only
                Optional<DBSource> db = dbSourceRepository.findById(p.get().getDbsourceId());
                // Security Group
                if (db.isPresent()) {
                    ResourceGroup security = new ResourceGroup();
                    security.setName("Authentication Grizzly");
                    security.setDescription("JWT token");
                    containerToSave.getResourceGroups().add(0, security);
                    DBSourceDto savedDataSource = dbSourceMapper.mapToDto(db.get());
                    containerToSave.getResources()
                            .addAll(projectExample.createAuthGroup(false, savedDataSource, security.getName()));
                }

                if (!integrate) {
                    // Default Group
                    ResourceGroup rg = new ResourceGroup();
                    rg.setName("Untitled");
                    rg.setDescription("This is a description for this group of APIs");
                    List<ResourceGroup> lrg = new ArrayList<>();
                    lrg.add(rg);
                    containerToSave.getResourceGroups().add(rg);
                }

            }

            // SET EMAIL
            if (containerDto.getUserEmail() == null) {
                String currentUserEmail = userService.getConnectedUserEmail();
                containerToSave.setUserEmail(currentUserEmail);

            }
            // SET MODELS
            List<EndpointModel> modelsToAdd = new ArrayList<EndpointModel>();
            if (!containerToSave.getUserEmail().equals("editor@codeonce.fr") && p.get().isSecurityEnabled() &&
                    !p.get().getType().equals("authentication microservice") && p.get().getAuthMSRuntimeURL() == null) {
                modelsToAdd = setModels();
            }

            modelsToAdd.forEach(el -> {
                int elementIndex = findIndex(containerToSave.getEndpointModels(), el);
                if (elementIndex < 0) {
                    containerToSave.getEndpointModels().add(el);
                }
            });

            containerToSave.getResources().forEach((r) -> {

                if (r.getHttpMethod().equals("GET") && (r.getRequestBody() == null || (r.getRequestBody() != null &&
                        r.getRequestBody().getContent() != null && r.getRequestBody().getContent().size() == 0 && r.getRequestBody().getName() == null))) {
                    r.setRequestBody(null);
                }
                if ((r.getOutFunctions() != null && r.getOutFunctions().size() > 0
                        && !r.getOutFunctions().get(0).isEmpty())
                        || (r.getInFunctions() != null && r.getInFunctions().size() > 0
                        && !r.getInFunctions().get(0).isEmpty())) {
                    if (r.getRequestBody() != null && r.getRequestBody().getContent() != null
                            && r.getRequestBody().getContent().get("application/json") != null
                            && r.getRequestBody().getContent().get("application/json").getSchema().getRef() != null) {
                        String q = RequestModelUtils.collectApiParameters(
                                RequestModelUtils.parseEndpointModelsToBodyRequest(containerToSave.getEndpointModels(),
                                        r.getRequestBody().getContent().get("application/json").getSchema().getRef(),
                                        null, true),
                                r.getParameters());
                        q = Json2Pojo.jsonSchema2Pojo(q, "codeonce");
                        ResourceRequestModel requestModel = new ResourceRequestModel();
                        requestModel.setName(r.getRequestBody().getName());
                        float f = (float) containerDto.getLastUpdate().getTime() / 1000;
                        requestModel.setLastUpdate(f);
                        requestModel.setRequestModel(q);
                        r.setRequestModels(new ArrayList<ResourceRequestModel>());
                        r.getRequestModels().add(requestModel);
                    }

                }

            });
            // Set the Swagger Salt
            if (StringUtils.isBlank(containerToSave.getSwaggerUuid())) {
                containerToSave.setSwaggerUuid(UUID.randomUUID().toString().substring(0, 8));
            }

            // CHECK MISSING ATTRIBUTES
            checkMissingAttributes(containerToSave);
            // SAVE
            Container container = containerRepository.save(containerToSave);
            /// converTablesToEndpoitModels(container.getDbsourceId());


            if (!container.getUserEmail().equals("editor@codeonce.fr")) {
                container.setHost(environment.getProperty("frontUrl")
                        .substring(environment.getProperty("frontUrl").indexOf("//") + 2));
                container.setBasePath("/runtime/" + container.getId());
            }

            Container savedContainer = containerRepository.save(container);


            // Save Analytics
            analyticsService.updateContainerMetrics(savedContainer);

            // GIT SYNC
            if (containerDto.getFirstCreation() != null && p.get().getGitUrl() != null) {
//				gitHandler.gitSync(Collections.emptyList(), p.get().getGitUrl(), p.get().getGitBranch(),
//						p.get().getId(), savedContainer.getId(), savedContainer.getSwaggerUuid(),
//						p.get().getDbsourceId(), p.get().getDatabaseName(), p.get().getGitUsername(),
//						p.get().getGitPassword());

            }
            return containerMapper.mapToDto(savedContainer);

        } else {
            throw GlobalExceptionUtil.notFoundException(Project.class, projectId).get();
        }

    }

    public static int findIndex(List<EndpointModel> arr, EndpointModel t) {
        int len = arr.size();
        return IntStream.range(0, len).filter(i -> t.getTitle().equals(arr.get(i).getTitle())).findFirst().orElse(-1);
    }

    /**
     * Returns a Container with its given Id
     *
     * @param containerId
     * @return ContainerDto
     */
    public ContainerDto get(String containerId) {

        return containerRepository.findById(containerId)//
                .map(c -> containerMapper.mapToDto(c))//
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
    }

    /**
     * Checks if the new container's name is unique
     *
     * @param containerDto
     * @return boolean
     */
    public boolean existsContainerName(ContainerDto containerDto) {
        log.debug(containerDto.getProjectId());
        List<ContainerDto> list = containersByProject(containerDto.getProjectId());
        log.debug(list.get(0).getName());
        list.stream().filter(dto -> dto.getName().equalsIgnoreCase(containerDto.getName()))
                .collect(Collectors.toList());
        return !(list.isEmpty());
    }

    /**
     * Returns a list of all the containers in the database
     *
     * @return List<ContainerDto>
     */
    public List<ContainerDto> getAll() {
        return containerRepository.findAll().stream()//
                .map(c -> containerMapper.mapToDto(c))//
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all the containers that belong to a specified project id
     *
     * @param projectId
     * @return List<ContainerDto>
     */
    public List<ContainerDto> containersByProject(String projectId) {
        return containerRepository.findAllByProjectId(projectId).stream()//
                .map(c -> containerMapper.mapToDto(c))//
                .collect(Collectors.toList());
    }

    public void deleteContainersUnderProject(String projectId) {
        containersByProject(projectId).stream().map(c -> {
            delete(c.getId());
            return c;
        }).collect(Collectors.toList());
    }

    /**
     * Deletes a container given its containerId
     *
     * @param containerId
     * @return
     */
    public void delete(String containerId) {
        containerRepository.findById(containerId).map(c -> {
            deleteHierarchy(c);
            logService.deleteByContainer(c.getId());
            filesHandler.deleteGridfsFiles(containerId);
            containerRepository.delete(c);
            analyticsService.removeContainerAnalytics(c.getId());
            return true;
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
    }

    /**
     * Delete a container's hierarchy
     *
     * @param c, the given container to delete
     */
    private void deleteHierarchy(Container c) {
        if (c.getHierarchyId().isEmpty())
            return;
        hierarchyRepository.deleteById(c.getHierarchyId());
    }

    /**
     * Deletes all the containers in the database
     *
     * @return
     */
    public void deleteAll() {
        containerRepository.deleteAll();
    }

    /**
     * Deletes all the containers under a project in the database
     */
    public void deleteAllByProject(String projectId) {
        logService.deleteByProject(projectId);
        containerRepository.deleteContainerByProjectId(projectId);

    }

    /**
     * Enable or Disable a Container
     *
     * @param containerId
     */
    public void enableDisableContainer(String containerId) {
        Optional<Container> container = containerRepository.findById(containerId);
        if (container.isPresent()) {
            containerRepository.findAllByProjectId(container.get().getProjectId()).stream().filter(Container::isEnabled)
                    .forEach(cont -> {
                        cont.setEnabled(false);
                        containerRepository.save(cont);
                    });
            container.get().setEnabled(!container.get().isEnabled());
            containerRepository.save(container.get());
        }
    }

    /**
     * Update dbsourceId field in all resources on Project Database Change
     *
     * @param id of the project
     */
    public void updateResourcesDbsourceId(String projectId, String dbsourceId) {

        containerRepository.findAllByProjectId(projectId).stream().forEach(cont -> {
            cont.getResources().parallelStream().forEach(ress -> ress.getCustomQuery().setDatasource(dbsourceId));
            containerRepository.save(cont);
        });
    }

    /**
     * Update all resources on Project Database Change
     */
    public void updateResources(ProjectDto newProjectDto, String projectId) {

        if (!(newProjectDto.getType().equals("authentication microservice") && newProjectDto.getIdentityProviderIds() != null && newProjectDto.getIdentityProviderIds().get(0).equalsIgnoreCase(IdentityProviders.GRIZZLY.toString()))) {
            containerRepository.findAllByProjectId(projectId).stream().forEach(cont -> {
                cont.getResources().removeIf(f -> f.getResourceGroup().equals("Authentication Grizzly"));
                cont.getResourceGroups().removeIf(ff -> ff.getName().equals("Authentication Grizzly"));
                saveResourcesForUpdate(newProjectDto, cont);
            });
        }

    }

    public void saveResourcesForUpdate(ProjectDto newProjectDto, Container cont) {
        ResourceGroup security = new ResourceGroup();
        if (newProjectDto.isSecurityEnabled()) {

            if ((!newProjectDto.getType().equals("authentication microservice")) && newProjectDto.getAuthMSRuntimeURL() == null) {
                security.setName("Authentication Grizzly");
                security.setDescription("JWT token");
                cont.getResourceGroups().add(0, security);

                DBSource db = dbSourceRepository.findById(newProjectDto.getDbsourceId())
                        .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, newProjectDto.getDbsourceId()));

                DBSourceDto savedDS = dbSourceMapperService.mapToDto(db);
                if (savedDS.getType() != null && savedDS.getType().equalsIgnoreCase("sql")) {
                    cont.getResources().addAll(0, projectExample.createSQLAuthGroup(false, savedDS, security.getName()));

                } else {
                    cont.getResources().addAll(0, projectExample.createAuthGroup(false, savedDS, security.getName()));
                }
            }

            if (cont.getResourceGroups().stream().filter(rg -> rg.getName().equals("Untitled")).collect(Collectors.toList()).isEmpty()
                    && !newProjectDto.getType().equals("authentication microservice")) {
                // Default Group
                ResourceGroup rg = new ResourceGroup();
                rg.setName("Untitled");
                rg.setDescription("This is a description for this group of APIs");
                cont.getResourceGroups().add(rg);
            }
        }

        containerRepository.save(cont);
    }

    public void saveResources(Container containerTosave, ProjectDto projectDto, DBSourceDto savedDataSource, ResourceGroup security, String currentUserEmail) {
        if (projectDto.isSecurityEnabled()) {
            containerTosave.setUserEmail(currentUserEmail);

            if (projectDto.getType().equals("authentication microservice")) {
                security.setDescription("Oauth identity provider");

                if (projectDto.getIdentityProviderIds() != null && projectDto.getIdentityProviderIds().get(0).equalsIgnoreCase(IdentityProviders.GRIZZLY.toString())) {
                    security.setName("Authentication Grizzly");
                    // SET MODELS
                    List<EndpointModel> modelsToAdd = setModels();
                    containerTosave.setEndpointModels(modelsToAdd);
                    if (savedDataSource.getType() != null && savedDataSource.getType().equalsIgnoreCase("sql")) {
                        containerTosave.setResources(
                                projectExample.createSQLAuthGroup(true, savedDataSource, security.getName()));

                    } else {
                        containerTosave.setResources(
                                projectExample.createAuthGroup(true, savedDataSource, security.getName()));
                    }
                } else {
                    security.setName("Authentication Oauth");
                    containerTosave.setResources(projectExample.createAuthGroup(false, null, security.getName()));
                }
                containerTosave.getResourceGroups().add(security);

            }
            if (!projectDto.getType().equals("authentication microservice") && projectDto.getAuthMSRuntimeURL() == null) {
                security.setName("Authentication Grizzly");
                security.setDescription("JWT token");
                containerTosave.getResourceGroups().add(security);
                // SET MODELS
                List<EndpointModel> modelsToAdd = setModels();
                containerTosave.setEndpointModels(modelsToAdd);
                if (savedDataSource.getType() != null && savedDataSource.getType().equalsIgnoreCase("sql")) {
                    containerTosave.setResources(
                            projectExample.createSQLAuthGroup(false, savedDataSource, security.getName()));

                } else {
                    containerTosave.setResources(
                            projectExample.createAuthGroup(false, savedDataSource, security.getName()));
                }
            }

            if (projectDto.getType().equals("microservice") || projectDto.getType().equals("markup microservice")) {
                // Default Group
                ResourceGroup rg = new ResourceGroup();
                rg.setName("Untitled");
                rg.setDescription("This is a description for this group of APIs");
                containerTosave.getResourceGroups().add(rg);
            }
        } else {
            // Default Group
            ResourceGroup rg = new ResourceGroup();
            rg.setName("Untitled");
            rg.setDescription("This is a description for this group of APIs");
            containerTosave.getResourceGroups().add(rg);

        }
    }

    /**
     * Returns the size of a container given its Id
     *
     * @param containerId
     * @return
     * @throws JSONException
     * @throws IOException
     */

    public long sizeOfContainer(String containerId) throws IOException {

        Container c = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        String hierarchyId = c.getHierarchyId();
        if (hierarchyId.equals(""))
            return 0;
        ContainerHierarchy hierarachy = hierarchyRepository.findById(hierarchyId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(ContainerHierarchy.class, hierarchyId));
        if (hierarachy.getHierarchy() != null) {
            File container = containerExportService.export(containerId);
            if (container == null)
                return 0;
            return FileUtils.sizeOfDirectory(container);

        }
        return 0;

    }

    public List<EndpointModel> setModels() {
        List<EndpointModel> modelsToAdd = new ArrayList<EndpointModel>();
        EndpointModel model1 = new EndpointModel();
        EndpointModel model2 = new EndpointModel();
        EndpointModel user = new EndpointModel();
        EndpointModel roles = new EndpointModel();

        List<ModelProperty> properties1 = new ArrayList<ModelProperty>();
        properties1.add(new ModelProperty("username", "string", false, ""));
        properties1.add(new ModelProperty("password", "string", false, ""));
        model1.setTitle("signIn");
        model1.setProperties(properties1);
        modelsToAdd.add(model1);
        List<ModelProperty> properties2 = new ArrayList<ModelProperty>();
        properties2.add(new ModelProperty("username", "string", false, ""));
        properties2.add(new ModelProperty("password", "string", false, ""));
        properties2.add(new ModelProperty("firstname", "string", false, ""));
        properties2.add(new ModelProperty("lastname", "string", false, ""));
        properties2.add(new ModelProperty("email", "string", false, ""));
        properties2.add(new ModelProperty("phone", "number", false, ""));
        model2.setTitle("signUp");
        model2.setProperties(properties2);
        modelsToAdd.add(model2);
        user.setTitle("authUser");
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        properties.add(new ModelProperty("_id", "string", false, ""));
        properties.add(new ModelProperty("username", "string", false, ""));
        properties.add(new ModelProperty("password", "string", false, ""));
        properties.add(new ModelProperty("firstname", "string", false, ""));
        properties.add(new ModelProperty("lastname", "string", false, ""));
        properties.add(new ModelProperty("phone", "string", false, ""));
        properties.add(new ModelProperty("email", "string", false, ""));
        user.setProperties(properties);
        modelsToAdd.add(user);
        roles.setTitle("roles");
        List<ModelProperty> rolesProps = new ArrayList<ModelProperty>();
        rolesProps.add(new ModelProperty("roles", "string", true, ""));
        roles.setProperties(rolesProps);
        modelsToAdd.add(roles);

        return modelsToAdd;
    }

    public ContainerDto clearContainer(String containerId) {
        Container c = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        c.getResourceGroups().clear();
        c.getResources().clear();
        c.getEndpointModels().clear();
        if (c.getRequestBodies() != null) {
            c.getRequestBodies().clear();
        }
        if (c.getResponses() != null) {
            c.getResponses().clear();
        }
        if (c.getServers() != null) {
            c.getServers().clear();
        }
        if (c.getHost() != null) {
            c.setHost(null);
        }
        if (c.getBasePath() != null) {
            c.setBasePath(null);
        }
        if (c.getDescription() != null) {
            c.setDescription(null);
        }
        if (c.getContact() != null) {
            c.setContact(null);
        }
        if (c.getLicense() != null) {
            c.setLicense(null);
        }
        if (c.getTermsOfService() != null) {
            c.setTermsOfService(null);
        }
        ResourceGroup defaultGroup = new ResourceGroup();
        defaultGroup.setName("Default");
        c.getResourceGroups().add(defaultGroup);
        Container clearedContainer = containerRepository.save(c);
        return containerMapper.mapToDto(clearedContainer);
    }

    public boolean integrate(String email, String containerId) throws ParseException, CustomGitAPIException, SQLException {

        // create a default datasource and link it
        String projectId = get(containerId).getProjectId();
        ProjectDto oldProject = projectService.get(projectId);
        String projectName = oldProject.getName();

        if (!projectService.existsProjectName(projectName, null)) {

            DBSourceDto dbSourceDto = new DBSourceDto();
            String dbname = projectName.replaceAll("\\s", "_").replaceAll("\\-", "_").replaceAll("\\.", "_");
            dbSourceDto.setName(dbname);
            dbSourceDto.setDescription("This is a description for your first datasource");
            dbSourceDto.setAuthenticationDatabase(SecurityLevel.ADMIN.getValue());
            if (dbname.length() > 9) {
                dbSourceDto.setDatabase(dbname.substring(0, 10));
            } else {
                dbSourceDto.setDatabase(dbname);
            }
            dbSourceDto.setUserEmail(email);
            dbSourceDto.setConnectionMode("FREE");
            dbSourceDto.setProvider(Provider.MONGO);
            dbSourceDto.setType("nosql");

            DBSourceDto savedDataSource = dbSourceService.saveDBSource(dbSourceDto);

            Project newProject = projectMapper.mapToDomain(oldProject);
            newProject.setId(null);
            newProject.setUserEmail(email);
            newProject.setDbsourceId(savedDataSource.getId());
            newProject.setDatabaseName(savedDataSource.getDatabase());

            Project savedProject = projectRepository.save(newProject);

            ContainerDto container = this.get(containerId);

            container.setHost(environment.getProperty("frontUrl")
                    .substring(environment.getProperty("frontUrl").indexOf("//") + 2));
            container.setBasePath("/runtime/" + container.getId());

            // Add Grizzly API Runtime URL
            List<Server> servers = container.getServers();
            Server server = new Server(
                    environment.getProperty("frontUrl").substring(environment.getProperty("frontUrl").indexOf("//") + 2)
                            + "/runtime/" + container.getId(),
                    "Grizzly API Runtime Url");
            if (servers == null) {
                servers = new ArrayList<Server>();
            }
            if (!containsName(servers, server.getUrl())) {
                servers.add(server);
            }
            container.setServers(servers);

            container.setUserEmail(email);
            container.setProjectId(savedProject.getId());
            updateResourcesQueries(container, savedDataSource);

            // create runtime server and baseUrl

            container.setId(null);
            this.saveContainer(container, true);

            return true;
        }
        return false;

    }

    private void updateResourcesQueries(ContainerDto container, DBSourceDto dataSourceDto) {
        container.getResourceGroups().forEach(rg -> {
            rg.getResources().forEach(r -> {
                r.getCustomQuery().setDatasource(dataSourceDto.getId());
                r.getCustomQuery().setDatabase(dataSourceDto.getName());
            });
        });
    }

    public void generateAuthModels() {
        containerRepository.findAll().forEach(el -> {
            for (int i = 0; i < el.getResourceGroups().size(); i++) {
                if (el.getResourceGroups().get(i).getName().equalsIgnoreCase("Authentication Grizzly")) {
                    el.setEndpointModels(setModels());
                    containerRepository.save(el);
                    break;
                }
            }
        });
    }


    public void generateExportPreference() {
        containerRepository.findAll().forEach(el -> {
            el.setExportPreference("V2");
            containerRepository.save(el);
        });
    }

    public Container createProjectExampleForContainer() {
        String userEmail = "editor@codeonce.fr";

        // create project linked to a user

        Project project = new Project();
        project.setName("Untitled");
        project.setDescription("This is a description for your first project");
        project.setType("microservice");
        project.setUserEmail(userEmail);
        project.getRoles().add(SecurityLevel.ADMIN.getValue());
        project.getRoles().add("user");

        SecurityApiConfig sec = new SecurityApiConfig();
        sec.setClientId("Untitled");
        sec.setSecretKey(DigestUtils.sha256Hex(project.getName()) + DigestUtils.sha256Hex("co%de01/"));
        sec.setTokenExpiration(3600);
        project.setSecurityConfig(sec);
        Project savedProject = projectRepository.save(project);

        // Create Default Version of Container V1

        Container containerTosave = new Container();
        containerTosave.setName("1.0.0");
        containerTosave.setDescription("This is a description for this project version");
        containerTosave.setProjectId(savedProject.getId());
        containerTosave.setUserEmail(userEmail);

        containerTosave.setSwaggerUuid(UUID.randomUUID().toString().substring(0, 8));
        return containerTosave;
    }

    public ContainerDto generateNewContainer() {
        ResourceGroup r = new ResourceGroup();
        r.setName("Default");
        Container containerTosave = createProjectExampleForContainer();
        containerTosave.getResourceGroups().add(r);
        Container savedContainer = containerRepository.save(containerTosave);

        return containerMapper.mapToDto(savedContainer);
    }

    private void checkMissingAttributes(Container container) {

        container.getResources().forEach(resource -> {
            resource.setMissingAttributes(checkMissingResource(resource));
        });
    }

    private List<String> checkMissingResource(Resource resource) {
        List<String> missingAttributes = new ArrayList<String>();
        if (resource.getCustomQuery().getDatasource() != null) {
            if (dbSourceRepository.findById(resource.getCustomQuery().getDatasource()).isPresent()) {
                DBSource dbSource = dbSourceRepository.findById(resource.getCustomQuery().getDatasource()).orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, resource.getCustomQuery().getDatasource()));
                if (dbSource.getProvider().equals(Provider.MONGO)) {
                    if (resource.getCustomQuery().getQuery() == null) {
                        missingAttributes.add("query");
                    }
                    if (resource.getCustomQuery().getCollectionName() == null) {
                        missingAttributes.add("collectionName");
                    }
                }
            }
        }
        return missingAttributes;
    }

    public void shareContainer(ShareContainerDto shareContainerDto, String containerId, String lang)
            throws IOException {
        String subject = "";
        for (String userEmail : shareContainerDto.getEmails()) {
            String content = emailService.getOutputContent("share-microservice.html", "templates/share-microservice",
                    lang, shareContainerDto);
            if (lang.contains("en")) {
                subject = shareContainerDto.getProjectName() + " is shared with you";
            } else {
                subject = shareContainerDto.getProjectName() + " est partagé avec vous";
            }
            emailService.send(content, subject, userEmail, "editor");
        }
    }

    private boolean containsName(final List<Server> servers, final String url) {
        return servers.stream().filter(o -> o.getUrl().equals(url)).findFirst().isPresent();
    }

    public ContainerDto saveContainerWithAbsentProject(ContainerDto containerDto, String dbSourceID, String physicalDatabase) {
        Container containerToSave = containerMapper.mapToDomain(containerDto);
        containerToSave.setDbsourceId(dbSourceID);
        containerToSave.setDatabaseName(physicalDatabase);
        // Set the Swagger Salt
        if (StringUtils.isBlank(containerToSave.getSwaggerUuid())) {
            containerToSave.setSwaggerUuid(UUID.randomUUID().toString().substring(0, 8));
        }
        // SAVE
        Container container = containerRepository.save(containerToSave);

        // Save Analytics
        analyticsService.updateContainerMetrics(container);
        return containerMapper.mapToDto(container);
    }

    public List<ContainerDto> containersByDbsource(String dbsourceId) {
        return containerRepository.findByDbsourceId(dbsourceId).stream()//
                .map(c -> containerMapper.mapToDto(c))//
                .collect(Collectors.toList());
    }

    public void updateCollectionName(String currentCollectionName, String newCollectionName) {
        // Récupère tous les conteneurs
        List<Container> containers = containerRepository.findAll();

        for (Container container : containers) {
            for (Resource resource : container.getResources()) {
                CustomQuery customQuery = resource.getCustomQuery();
                if (customQuery != null && currentCollectionName.equals(customQuery.getCollectionName())) {
                    customQuery.setCollectionName(newCollectionName); // Mise à jour de la collectionName
                }
            }
            containerRepository.save(container); // Enregistre le conteneur mis à jour
        }
    }

    public boolean containersByCollection(String collectionName) {
        boolean collectionUsed=false;
        // Récupère tous les conteneurs
        List<Container> containers = containerRepository.findAll();

        for (Container container : containers) {
            for (Resource resource : container.getResources()) {
                CustomQuery customQuery = resource.getCustomQuery();
                if (customQuery != null && collectionName.equals(customQuery.getCollectionName())) {
                    collectionUsed=true;
                    return collectionUsed;
                }
            }
        }
        return collectionUsed;
    }
}
