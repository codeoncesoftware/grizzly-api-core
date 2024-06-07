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
package fr.codeonce.grizzly.core.service.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.model.GridFSFile;
import fr.codeonce.grizzly.common.runtime.IdentityProviders;
import fr.codeonce.grizzly.common.runtime.resource.CreateResourceRequest;
import fr.codeonce.grizzly.common.runtime.resource.RuntimeResource;
import fr.codeonce.grizzly.common.runtime.resource.RuntimeResourceFunction;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.container.hierarchy.ContainerHierarchy;
import fr.codeonce.grizzly.core.domain.container.hierarchy.ContainerHierarchyRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProvider;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.Resource;
import fr.codeonce.grizzly.core.function.util.FunctionRuntimeMapper;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.fs.FilesHandler;
import fr.codeonce.grizzly.core.service.fs.GitHandler;
import fr.codeonce.grizzly.core.service.fs.ZipHandler;
import fr.codeonce.grizzly.core.service.fs.model.CustomFile;
import fr.codeonce.grizzly.core.service.fs.model.CustomFolder;
import fr.codeonce.grizzly.core.service.function.FunctionService;
import fr.codeonce.grizzly.core.service.resource.utils.ResourceRuntimeMapper;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A service to Handle files with GridFs : Insert, Delete, Retrieve and Update
 *
 * @author rayen
 */

@Service
public class ResourceService {

    private static final String APPLICATION_JSON = "application/json";

    @Autowired
    private GitHandler gitHandler;

    @Autowired
    private ZipHandler zipHandler;

    @Autowired
    private FilesHandler filesHandler;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private MongoCacheService cacheService;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ContainerHierarchyRepository cHierarchyRepository;

    @Autowired
    private ResourceRuntimeMapper resourceRuntimeMapper;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private MappingJackson2HttpMessageConverter springMvcJacksonConverter;

    @Autowired
    private ResourceQueryService resourceQueryService;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private FunctionRuntimeMapper funtionRuntimeMapper;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    /**
     * Upload a file with GridFs
     *
     * @param file to insert in MongoDb
     * @return Id of the new inserted resource
     * @throws Exception
     * @throws IOException in case of missing file
     */
//	public String cloneGitRepository(String body) throws CustomGitAPIException {
//		try {
//
//			ObjectNode node = springMvcJacksonConverter.getObjectMapper().readValue(body, ObjectNode.class);
//			String gitRepoUrl = (node.get("gitRepoUrl") != null ? node.get("gitRepoUrl").textValue() : null);
//			String branch = (node.get("branch") != null ? node.get("branch").textValue() : null);
//			String containerId = (node.get("containerId") != null ? node.get("containerId").textValue() : null);
//			String projectId = (node.get("projectId") != null ? node.get("projectId").textValue() : null);
//			String dbsourceId = (node.get("dbsourceId") != null ? node.get("dbsourceId").textValue() : null);
//			String databaseName = (node.get("databaseName") != null ? node.get("databaseName").textValue() : null);
//			String gitUsername = (node.get("gitUsername") != null ? node.get("gitUsername").textValue() : null);
//			String gitPassword = (node.get("gitPassword") != null ? node.get("gitPassword").textValue() : null);
//
//			log.debug("Request to get git repository branchs list from : {}", gitRepoUrl);
//
//			return this.gitHandler.cloneGitRepository(gitRepoUrl, branch, projectId, containerId, dbsourceId,
//					databaseName, gitUsername, gitPassword);
//
//		} catch (IOException e) {
//			log.debug("Error while parsing GIT informations");
//			throw new BadCredentialsException("4011");
//		}
//	}
    public String gitSync(List<MultipartFile> files, String gitRepoUrl, String branch, String projectId,
                          String containerId, String swaggerUuid, String dbsourceId, String databaseName, String gitUsername,
                          String gitPassword) throws CustomGitAPIException {
        try {
            log.debug("Request to get git repository branchs list from : {}", gitRepoUrl);
//			return this.gitHandler.gitSync(files, gitRepoUrl, branch, projectId, containerId, swaggerUuid, dbsourceId,
//					databaseName, gitUsername, gitPassword);
            return null;
        } catch (Exception e) {
            log.error("error {}", e.getMessage());
            throw new BadCredentialsException("4011");
        }
    }

    public List<String> getRepoBranchsList(String body) {
        try {
            ObjectNode node = springMvcJacksonConverter.getObjectMapper().readValue(body, ObjectNode.class);
            String gitRepoUrl = (node.get("gitRepoUrl") != null ? node.get("gitRepoUrl").textValue() : null);
            String gitUsername = (node.get("gitUsername") != null ? node.get("gitUsername").textValue() : null);
            String gitPassword = (node.get("gitPassword") != null ? node.get("gitPassword").textValue() : null);
            String gitToken = (node.get("gitToken") != null ? node.get("gitToken").textValue() : null);
            log.debug("Request to get git repository branchs list from : {}", gitRepoUrl);
            return this.gitHandler.getRepoBranchsList(gitRepoUrl, gitUsername, gitPassword, gitToken);
        } catch (IOException e) {
            log.error("Error while parsing GIT informations {}", e);
            throw new BadCredentialsException("4011");
        }
    }

    public String importZipFile(MultipartFile file, String containerId, String dbsourceId, String databaseName)
            throws IOException {
        return this.zipHandler.importZipFile(file, containerId, dbsourceId, databaseName);
    }

    public GridFSFile getResourceFileWithId(String containerId, String fileId) {
        return this.filesHandler.getResourceFileWithId(containerId, fileId);
    }

    public GridFSFile getResourceFile(String containerId, String fileUri) {
        return this.filesHandler.getResource(containerId, fileUri);
    }

    public GridFSFile getResource(String containerId, String path) {
        return this.filesHandler.getResource(containerId, path);
    }

    public GridFsResource getGridFsResource(GridFSFile fsFile, String containerId) {
        return this.filesHandler.getGridFsResource(fsFile, containerId);
    }

    /**
     * Prepare the Runtime Resource, API, to be forwarder to the GateWay
     *
     * @param containerId
     * @param resourcePath
     * @return RuntimeResource
     */

    public Resource checkPathReource(String containerId, String resourcePath, String method) {

        List<Resource> resources = containerRepository.findById(containerId).orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId)).getResources();
        Resource resource = null;
        List<String> receivedPath = Arrays.asList(resourcePath.substring(1).split("/"));
        int i = 0;
        for (int j = 0; j < resources.size(); j++) {
            List<String> resspath = Arrays.asList(resources.get(j).getPath().substring(1).split("/"));
            if (i == 2) {
                return resource;
            }
            if (!findByQuery(resourcePath) && (resspath.size() == receivedPath.size()) && resources.get(j).getHttpMethod().equals(method)) {
                Boolean res = false;
                for (int k = 0; k < resspath.size(); k++) {
                    if (resspath.get(k).equalsIgnoreCase(receivedPath.get(k)))
                        res = true;
                }
                if (res) {
                    resource = resources.get(i);
                    i++;
                }
            }
        }
        return null;
    }

    public RuntimeResource getRuntimeResource(String containerId, String resourcePath, String method, String returnType) {
        return this.containerRepository.findById(containerId).map(container -> {
            StringBuilder secretKey = new StringBuilder();
            StringBuilder clientId = new StringBuilder();
            StringBuilder projectType = new StringBuilder();
            boolean defaultIdp = false;
            // Prepare the RuntimeResource
            Resource ress = container.getResources().stream().filter(res -> hasSamePath(resourcePath, method, res))
                    .findFirst().orElse(null);
            if (ress == null) {
                return null;
            }
            RuntimeResource resource = resourceRuntimeMapper.mapToRuntime(ress);
            resource.getCustomQuery().setQueryName(ress.getCustomQuery().getQueryName());
            Optional<Project> project = this.projectRepository.findById(container.getProjectId());
            if (project.isPresent()) {
                Project proj = project.get();
                if (proj.getSecurityConfig().getSecretKey() != null) {
                    secretKey.append(proj.getSecurityConfig().getSecretKey());
                }
                if (proj.getSecurityConfig().getClientId() != null) {
                    clientId.append(proj.getSecurityConfig().getClientId());
                }
                projectType.append(proj.getType());
                if (proj.getAuthMSRuntimeURL() != null) {
                    resource.setAuthMSRuntimeUrl(proj.getAuthMSRuntimeURL());
                }
                if (proj.getType().equals("authentication microservice")) {
                    resource.setAuthMSRuntimeUrl(proj.getRuntimeUrl());
                    resource.setAuthorizedApps(proj.getAuthorizedApps());
                }

                List<String> list = new ArrayList<>();
                if (!proj.getIdentityProviderIds().isEmpty()) {
                    if (proj.getIdentityProviderIds().get(0).equalsIgnoreCase(IdentityProviders.GRIZZLY.toString())) {
                        resource.setDefaultIdP(true);
                        defaultIdp = true;
                    } else {
                        proj.getIdentityProviderIds().stream().forEach(ip -> {
                            IdentityProvider idp = this.identityProviderRepository.findById(ip).get();
                            list.add(idp.getName().toString());
                        });
                    }
                }
                resource.setExistedIdentityProvidersName(list);
            }
            resource.setSecurityKey(secretKey.toString());
            resource.setClientId(clientId.toString());
            resource.setCurrentMicroservicetype(projectType.toString());
            resource.setReturnType(returnType);
            CompletableFuture.runAsync(() -> analyticsService.updateRequestCount(containerId));
            // don't call it if it XSL
            if (!projectType.toString().equals("authentication microservice") ||
                    (projectType.toString().equals("authentication microservice") && defaultIdp)) {
                this.dbSourceRepository.findById(resource.getCustomQuery().getDatasource()).ifPresent(db -> {
                    resource.setConnectionMode(db.getConnectionMode());
                    resource.setPhysicalDatabase(db.getPhysicalDatabase());
                    resource.setProvider(db.getProvider());
                    resource.setBucketName(db.getBucketName());
                    resource.setDatabaseType(db.getType());
                });
            }
            // fetch functions
            try {
                if (ress.getInFunctions() != null && !ress.getInFunctions().isEmpty()) {
                    resource.setInFunctions(new ArrayList<RuntimeResourceFunction>());
                    resource.getInFunctions().add(funtionRuntimeMapper
                            .dtoToRuntime(this.functionService.findFunctionById(ress.getInFunctions().get(0))));
                }
            } catch (Exception e) {
                resource.setInFunctions(new ArrayList<RuntimeResourceFunction>());
            }
            try {
                if (ress.getOutFunctions() != null && !ress.getOutFunctions().isEmpty()) {
                    resource.setOutFunctions(new ArrayList<RuntimeResourceFunction>());

                    resource.getOutFunctions().add(funtionRuntimeMapper
                            .dtoToRuntime(this.functionService.findFunctionById(ress.getOutFunctions().get(0))));
                }
            } catch (Exception e) {
                resource.setOutFunctions(new ArrayList<RuntimeResourceFunction>());
            }
            try {
                if (ress.getFunctions() != null && !ress.getFunctions().isEmpty()) {
                    resource.setFunctions(new ArrayList<RuntimeResourceFunction>());

                    resource.getFunctions().add(funtionRuntimeMapper
                            .dtoToRuntime(this.functionService.findFunctionById(ress.getFunctions().get(0))));
                }
            } catch (Exception e) {
                resource.setFunctions(new ArrayList<RuntimeResourceFunction>());
            }
            return resource;
        }).orElse(null);
    }

    /**
     * Test if the Received Path matches the Resource Path considering PathVariables
     *
     * @param resourcePath
     * @param res
     * @return boolean, True if paths match, false if not
     */
    private boolean hasSamePath(String resourcePath, String method, Resource res) {

        boolean result = false;
        List<String> receivedPath = Arrays.asList(resourcePath.substring(1).split("/"));
        List<String> resspath = Arrays.asList(res.getPath().substring(1).split("/"));
        if (receivedPath.size() != resspath.size()) {
            return false;
        }
        if (!res.getHttpMethod().equals(method)) {
            return false;
        } else {
            if (!res.getPath().contains("{")) {
                if (resourcePath.equalsIgnoreCase(res.getPath())) {
                    return true;
                }
            } else {
                String var1 = res.getPath().replace("{", ".");
                if (resourcePath.length() >= var1.split("\\.")[0].length()) {
                    if (resourcePath.substring(0, var1.split("\\.")[0].length())
                            .equalsIgnoreCase(var1.split("\\.")[0])) {
                        return true;
                    }
                }

            }
        }
        return result;
    }

    /**
     * Handle Uploading File to GridFs
     *
     * @param file
     * @param idContainer
     * @return
     * @throws IOException
     */
    public String uploadFile(MultipartFile file, String idContainer) throws IOException {
        Optional<Container> cont = this.containerRepository.findById(idContainer);
        if (cont.isPresent()) {
            Container container = cont.get();
            MongoClient mClient = this.cacheService.getMongoClient(container.getDbsourceId());
            if (mClient != null) {
                String databaseName = null;
                Optional<DBSource> dbsource = this.dbSourceRepository.findById(container.getDbsourceId());
                if (dbsource.isPresent()) {
                    databaseName = container.getDatabaseName();
                }
                Document metaData = new Document();
                metaData.put("containerId", idContainer);
                metaData.put("fileUri", file.getOriginalFilename());
                String fileId = this.cacheService.getGridFs(mClient, databaseName)
                        .store(file.getInputStream(), file.getOriginalFilename(), metaData).toHexString();
                Optional<ContainerHierarchy> hierarchyOp = this.cHierarchyRepository.findById(container.getHierarchyId());
                if (hierarchyOp.isPresent()) {
                    ContainerHierarchy hierarchy = hierarchyOp.get();
                    ObjectMapper mapper = new ObjectMapper();
                    CustomFolder folder = mapper.readValue(hierarchyOp.get().getHierarchy(), CustomFolder.class);
                    folder.getChildren().add(new CustomFile(file.getOriginalFilename(), fileId));
                    hierarchy.setHierarchy(mapper.writeValueAsString(folder));
                    this.cHierarchyRepository.save(hierarchy);
                }
            }
        }
        return null;
    }

    public void deleteFiles(String containerId) {

        this.filesHandler.deleteGridfsFiles(containerId);

    }

    /**
     * create resource request
     *
     * @param containerId
     * @param CreateResourceRequest
     * @return RuntimeResource
     * @throws Exception
     */
    public RuntimeResource createResource(String containerId, CreateResourceRequest createResourceRequest)
            throws Exception {
        StopWatch watch2 = new StopWatch();
        watch2.start("create runtime ressource");
        RuntimeResource runtimeResource = resourceQueryService.createResource(containerId, createResourceRequest);
        watch2.stop();
        log.info("timing create ressource :{}", watch2.getTotalTimeMillis());
        return runtimeResource;

    }

    public Boolean findByQuery(String path) {
        List<String> paths = new ArrayList<>(Arrays.asList(path.substring(1).split("/")));
        if (paths.size() > 1) {
            return paths.get(1).substring(0, "find".length()).equalsIgnoreCase("find");
        }
        return false;
    }

    public Resource getFindByPaths(String containerId) {
        List<Resource> resources = containerRepository.findById(containerId).get().getResources();
        Resource resource = null;
        int i = 0;
        for (int j = 0; j < resources.size(); j++) {
            if (i == 2) {
                return resource;
            }
            if (!findByQuery(resources.get(i).getPath()) && i < 2) {
                log.info("path {}", resources.get(i).getPath());
                resource = resources.get(i);
                i++;
            }
        }
        return null;
    }

    public Resource getFinalResource(String containerId, String reqPath, String mathod) {
        List<Resource> resources = containerRepository.findById(containerId).orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId)).getResources();
        List<String> receivedPath = Arrays.asList(reqPath.substring(1).split("/"));
        for (int i = 0; i < resources.size(); i++) {
            if (receivedPath.size() != Arrays.asList(resources.get(i).getPath().substring(1).split("/")).size()) {
                return resources.get(i);
            }
        }
        return null;
    }

}
