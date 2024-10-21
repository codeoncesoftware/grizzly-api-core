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

import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.enums.*;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.project.SecurityApiConfig;
import fr.codeonce.grizzly.core.domain.resource.*;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.container.ContainerDto;
import fr.codeonce.grizzly.core.service.container.ContainerService;
import fr.codeonce.grizzly.core.service.container.ContainerSwaggerService;
import fr.codeonce.grizzly.core.service.datasource.DBSourceDto;
import fr.codeonce.grizzly.core.service.datasource.DBSourceMapper;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoDBSourceService;
import fr.codeonce.grizzly.core.service.fs.FilesHandler;
import fr.codeonce.grizzly.core.service.fs.MockMultipartFile;
import fr.codeonce.grizzly.core.service.fs.ZipHandler;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Service
public class ProjectExample {

    private static final String AUTHENTICATION_USER = "authentication_user";
    private static final String ALL_USERS = "/allusers";
    private static final String DELETE_USER = "/deleteuser";
    private static final String UPDATE_USER = "/updateuser";
    private static final String GRANT = "/grant";
    private static final String ACTIVATE = "/activate";
    private static final String ALL_ROLES = "/allroles";
    private static final String ME = "/me";
    private static final String SIGN_IN = "/signin";
    private static final String SIGN_UP = "/signup";
    private static final String OAUTH_AUTHORIZATION = "/authorization";
    private static final String OAUTH_USERINFO = "/userinfo";
    private static final String IDENTITY_PROVIDER = "identityProvider";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private MongoDBSourceService dbSourceService;

    @Autowired
    private FilesHandler filesHandler;

    @Autowired
    private ZipHandler zipHandler;

    @Autowired
    private MongoCacheService cacheService;

    @Value("${resource-url}")
    private String resourceUrl;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private DBSourceMapper dBSourceMapper;

    @Autowired
    private ContainerSwaggerService containerSwaggerService;

    public void createProjectExample() throws IOException {

        // create data-source
        String userEmail = getCurrentUserEmail();
        DBSourceDto dbSourceDto = new DBSourceDto();
        dbSourceDto.setName("Example");
        dbSourceDto.setDescription("This is a description for your first datasource");
        dbSourceDto.setAuthenticationDatabase(SecurityLevel.ADMIN.getValue());
        dbSourceDto.setDatabase("Example");
        dbSourceDto.setUserEmail(userEmail);
        dbSourceDto.setConnectionMode("FREE");
        dbSourceDto.setProvider(Provider.MONGO);
        DBSourceDto savedDataSource = dbSourceService.saveDBSource(dbSourceDto);
        DBSource dbSource = this.dbSourceService.getDbSourceById(savedDataSource.getId());

        // create project linked to a user

        Project project = new Project();
        project.setName("Demo");
        project.setType("microservice");
        project.setDescription("This is a description for your first project");
        project.setUserEmail(userEmail);
        project.getRoles().add(SecurityLevel.ADMIN.getValue());
        project.getRoles().add("user");
        SecurityApiConfig sec = new SecurityApiConfig();
        sec.setClientId("Demo");
        sec.setSecretKey(DigestUtils.sha256Hex(project.getName()) + DigestUtils.sha256Hex("co%de01/"));
        sec.setTokenExpiration(3600);
        project.setSecurityConfig(sec);
        project.setDbsourceId(savedDataSource.getId());
        project.setDatabaseName(savedDataSource.getDatabase());
        Project savedProject = projectRepository.save(project);

        // Create Default Version of Container V1

        Container containerTosave = new Container();
        containerTosave.setName("1.0.0");
        containerTosave.setDescription("This is a description for this project version");
        containerTosave.setProjectId(savedProject.getId());
        containerTosave.setUserEmail(userEmail);

        // create groups
        ResourceGroup security = new ResourceGroup();
        security.setName("Authentication Grizzly");
        security.setDescription("JWT token");
        ResourceGroup rg1 = new ResourceGroup();
        rg1.setName(Const.QUERY.getValue());
        ResourceGroup rg2 = new ResourceGroup();
        rg2.setName("XSL");
        ResourceGroup rg3 = new ResourceGroup();
        rg3.setName("ThymeLeaf");
        ResourceGroup rg4 = new ResourceGroup();
        rg4.setName("Freemarker");

        containerTosave.getResourceGroups().add(security);
        containerTosave.getResourceGroups().add(rg1);
        containerTosave.getResourceGroups().add(rg2);
        containerTosave.getResourceGroups().add(rg3);
        containerTosave.getResourceGroups().add(rg4);
        containerTosave.setDbsourceId(dbSource.getId());
        containerTosave.setDatabaseName(dbSource.getPhysicalDatabase());

        // Set the Swagger Salt
        if (StringUtils.isBlank(containerTosave.getSwaggerUuid())) {
            containerTosave.setSwaggerUuid(UUID.randomUUID().toString().substring(0, 8));
        }

        // Save Container
        Container savedContainer = containerRepository.save(containerTosave);

        this.dbSourceService.addFirstCollections(this.cacheService.getMongoClient(dbSource.getId()),
                containerTosave.getDatabaseName(), "demo");
        this.dbSourceService.addFirstCollections(this.cacheService.getMongoClient(dbSource.getId()),
                containerTosave.getDatabaseName(), AUTHENTICATION_USER);

        String containerId = savedContainer.getId();

        // upload files
        File zipFile = new File(resourceUrl);
        FileInputStream input = new FileInputStream(zipFile);
        MultipartFile multipartFile = new MockMultipartFile("transformation.zip", zipFile.getName(), "application/zip",
                IOUtils.toByteArray(input));
        zipHandler.importZipFile(multipartFile, containerId, dbSource.getId(), dbSource.getPhysicalDatabase());

        // set hierarchy
        containerTosave.setHierarchyId(containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId)).getHierarchyId());

        // create Query API
        Resource queryGetOne = createQueryApi(rg1.getName(), "/getone", HttpMethod.GET.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource queryGetAll = createQueryApi(rg1.getName(), "/getall", HttpMethod.GET.getValue(), savedDataSource,
                "{}", QueryType.INSERT.getValue(), true, null);
        Resource queryPost = createQueryApi(rg1.getName(), "/add", HttpMethod.POST.getValue(), savedDataSource, "{}",
                QueryType.INSERT.getValue(), true, null);
        Resource queryPut = createQueryApi(rg1.getName(), "/update", HttpMethod.PUT.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), true,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource queryDeleteOne = createQueryApi(rg1.getName(), "/deleteone", HttpMethod.DELETE.getValue(),
                savedDataSource, PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource queryDeleteAll = createQueryApi(rg1.getName(), "/deleteall", HttpMethod.DELETE.getValue(),
                savedDataSource, "{}", QueryType.INSERT.getValue(), true, null);

        // create XSL API
        Resource xsl = createXslApi(rg2.getName(), containerId);

        // create Thymeleaf API
        Resource thymeleaf = createThymeleafApi(rg3.getName(), containerId);

        // create Freemarker API
        Resource freemarker = createFreemarkerApi(rg4.getName(), containerId);

        // Security APIs
        containerTosave.setResources(createAuthGroup(false, savedDataSource, security.getName()));

        // Query APIs
        containerTosave.getResources().add(queryGetOne);
        containerTosave.getResources().add(queryGetAll);
        containerTosave.getResources().add(queryPost);
        containerTosave.getResources().add(queryPut);
        containerTosave.getResources().add(queryDeleteOne);
        containerTosave.getResources().add(queryDeleteAll);

        // Transformation APIs
        containerTosave.getResources().add(xsl);
        containerTosave.getResources().add(thymeleaf);
        containerTosave.getResources().add(freemarker);

        // save container finally
        containerRepository.save(containerTosave);

        // Analytics
        analyticsService.updateContainerMetrics(containerTosave);
    }

    private Resource createXslApi(String rg, String containerId) {

        Resource xsl = new Resource();
        xsl.setResourceGroup(rg);
        xsl.setHttpMethod(HttpMethod.POST.getValue());
        xsl.setPath("/xsl");
        xsl.setExecutionType(ResourceType.XSL.name());

        List<String> securityLevel = new ArrayList<>();
        securityLevel.add(SecurityLevel.PUBLIC.getValue());
        xsl.setSecurityLevel(securityLevel);

        // add body
        List<ResourceParameter> parameters = new ArrayList<>();
        ResourceParameter rp = new ResourceParameter();
        rp.setIn("Body");
        rp.setName("body");
        rp.setType(Const.STRING.getValue());
        rp.setValue(
                "<?xml version='1.0' encoding='ISO-8859-1'?> <catalog xmlns:foo='http://www.foo.org/' xmlns:bar='http://www.bar.org'> <foo:cd> <title>Empire Burlesque</title> <artist>Bob Dylan</artist> <country>USA</country> <company>Columbia</company> <price>10.90</price> <bar:year>1985</bar:year> <img>./assets/img/bob.jpg</img> </foo:cd> <foo:cd> <title>Hide your heart</title> <artist>Bonnie Tyler</artist> <country>UK</country> <company>CBS Records</company> <price>9.90</price> <bar:year>1988</bar:year> <img>./assets/img/bonnie.jpg</img> </foo:cd> <foo:cd> <title>Greatest Hits</title> <artist>Dolly Parton</artist> <country>USA</country> <company>RCA</company> <price>9.90</price> <bar:year>1982</bar:year> <img>./assets/img/dolly.jpg</img> </foo:cd> </catalog>");
        parameters.add(rp);
        xsl.setParameters(parameters);

        // primary file
        ResourceFile primaryResourceFile = new ResourceFile();
        String filePath = "transformation/transformation/xsl/catalog.xsl";
        String fileId = filesHandler.getResource(containerId, filePath).getObjectId().toString();
        primaryResourceFile.setFileUri(filePath);
        primaryResourceFile.setFileId(fileId);
        xsl.setResourceFile(primaryResourceFile);

        // secondary files
        // CSS
        ResourceFile secondaryFileCss = new ResourceFile();
        String filePathCss = "transformation/transformation/xsl/assets/css/mycss.css";
        String fileIdCss = filesHandler.getResource(containerId, filePathCss).getObjectId().toString();
        secondaryFileCss.setFileUri(filePathCss);
        secondaryFileCss.setFileId(fileIdCss);
        xsl.getSecondaryFilePaths().add(secondaryFileCss);

        // JS
        ResourceFile secondaryFileJs = new ResourceFile();
        String filePathJs = "transformation/transformation/xsl/assets/js/myjs.js";
        String fileIdJs = filesHandler.getResource(containerId, filePathJs).getObjectId().toString();
        secondaryFileJs.setFileUri(filePathJs);
        secondaryFileJs.setFileId(fileIdJs);
        xsl.getSecondaryFilePaths().add(secondaryFileJs);

        addCommonConfig(xsl);

        return xsl;
    }

    private void addCommonConfig(Resource res) {

        res.setConsumes(Collections.singletonList("application/json"));
        res.setProduces(Collections.singletonList("application/json"));

        List<APIResponse> responses = new ArrayList<>();
        responses.add(new APIResponse("200", "Ok"));
        responses.add(new APIResponse("401", "Unauthorized"));
        responses.add(new APIResponse("403", "Forbidden"));

        res.setResponses(responses);

    }

    private Resource createThymeleafApi(String rg, String containerId) {

        Resource thymeleaf = new Resource();
        thymeleaf.setResourceGroup(rg);
        thymeleaf.setHttpMethod(HttpMethod.POST.getValue());
        thymeleaf.setPath("/thymeleaf");
        thymeleaf.setExecutionType(ResourceType.Thymeleaf.name());

        List<String> securityLevel = new ArrayList<>();
        securityLevel.add(SecurityLevel.PUBLIC.getValue());
        thymeleaf.setSecurityLevel(securityLevel);

        // add body
        List<ResourceParameter> parameters = new ArrayList<>();
        ResourceParameter rp = new ResourceParameter();
        rp.setIn("Body");
        rp.setName("body");
        rp.setType(Const.STRING.getValue());
        rp.setValue("""
                {
                "message" : "Code Oncer",
                "subscriptionDate" : "until December 2021",
                "hobbies": 
                [ 
                "Swimming","Football", "Coding"				
                ]
                }\
                """);
        parameters.add(rp);
        thymeleaf.setParameters(parameters);

        // primary file
        ResourceFile primaryResourceFile = new ResourceFile();
        String filePath = "transformation/transformation/thyme/test.html";
        String fileId = filesHandler.getResource(containerId, filePath).getObjectId().toString();
        primaryResourceFile.setFileUri(filePath);
        primaryResourceFile.setFileId(fileId);
        thymeleaf.setResourceFile(primaryResourceFile);

        // secondary files
        // CSS
        ResourceFile secondaryFileCss = new ResourceFile();
        String filePathCss = "transformation/transformation/thyme/assets/css/myCss.css";
        String fileIdCss = filesHandler.getResource(containerId, filePathCss).getObjectId().toString();
        secondaryFileCss.setFileUri(filePathCss);
        secondaryFileCss.setFileId(fileIdCss);
        thymeleaf.getSecondaryFilePaths().add(secondaryFileCss);

        addCommonConfig(thymeleaf);

        return thymeleaf;
    }

    private Resource createFreemarkerApi(String rg, String containerId) {
        Resource freemarker = new Resource();
        freemarker.setResourceGroup(rg);
        freemarker.setHttpMethod(HttpMethod.POST.getValue());
        freemarker.setPath("/freemarker");
        freemarker.setExecutionType(ResourceType.FreeMarker.name());

        List<String> securityLevel = new ArrayList<>();
        securityLevel.add(SecurityLevel.PUBLIC.getValue());
        freemarker.setSecurityLevel(securityLevel);

        // add body
        List<ResourceParameter> parameters = new ArrayList<>();
        ResourceParameter rp = new ResourceParameter();
        rp.setIn("Body");
        rp.setName("body");
        rp.setType(Const.STRING.getValue());
        rp.setValue(
                "{ \"country\": \"France\", \"cities\": [ { \"id\":\"1\", \"name\":\"Lyon\", \"population\":\"100000\" }, { \"id\":\"2\", \"name\":\"Paris\", \"population\":\"130000\" } ,{ \"id\":\"2\", \"name\":\"Nice\", \"population\":\"30000\" } ,{ \"id\":\"4\", \"name\":\"Lille\", \"population\":\"90000\" } ,{ \"id\":\"5\", \"name\":\"Nantes\", \"population\":\"70000\" } ] }");
        parameters.add(rp);
        freemarker.setParameters(parameters);

        // primary file
        ResourceFile primaryResourceFile = new ResourceFile();
        String filePath = "transformation/transformation/freemarker/main.ftl";
        String fileId = filesHandler.getResource(containerId, filePath).getObjectId().toString();
        primaryResourceFile.setFileUri(filePath);
        primaryResourceFile.setFileId(fileId);
        freemarker.setResourceFile(primaryResourceFile);

        // secondary files
        // CSS
        ResourceFile secondaryFileCss = new ResourceFile();
        String filePathCss = "transformation/transformation/freemarker/assets/css/mycss.css";
        String fileIdCss = filesHandler.getResource(containerId, filePathCss).getObjectId().toString();
        secondaryFileCss.setFileUri(filePathCss);
        secondaryFileCss.setFileId(fileIdCss);
        freemarker.getSecondaryFilePaths().add(secondaryFileCss);

        // JS
        ResourceFile secondaryFileJs = new ResourceFile();
        String filePathJs = "transformation/transformation/freemarker/assets/js/myjs.js";
        String fileIdJs = filesHandler.getResource(containerId, filePathJs).getObjectId().toString();
        secondaryFileJs.setFileUri(filePathJs);
        secondaryFileJs.setFileId(fileIdJs);
        freemarker.getSecondaryFilePaths().add(secondaryFileJs);

        addCommonConfig(freemarker);

        return freemarker;
    }

    public Resource createQueryApi(String rgName, String path, String httpMethod, DBSourceDto savedDbSource,
                                   String queryContent, String type, boolean many, List<ResourceParameter> params) {
        Resource query = new Resource();
        query.setResourceGroup(rgName);
        query.setHttpMethod(httpMethod);
        query.setPath(path);
        query.setExecutionType(ResourceType.Query.name());

        List<String> securityLevel = new ArrayList<>();
        if (path.equals(ALL_USERS) || path.equals(GRANT) || path.equals(ALL_ROLES) || path.equals(ACTIVATE)
                || path.equals(DELETE_USER)) {
            securityLevel.add(SecurityLevel.ADMIN.getValue());
        } else if (path.equals(UPDATE_USER)) {
            securityLevel.add("all");
        } else if (path.equals(ME) || path.equals(OAUTH_USERINFO)) {
            securityLevel.add(SecurityLevel.ALL.name().toLowerCase());
        } else {
            securityLevel.add(SecurityLevel.PUBLIC.getValue());
        }
        query.setSecurityLevel(securityLevel);

        CustomQuery customQuery = new CustomQuery();
        if (savedDbSource != null) {
            customQuery.setDatasource(savedDbSource.getId());
            customQuery.setDatabase(savedDbSource.getDatabase());

            if (rgName.equals("Authentication Grizzly")) {
                customQuery.setCollectionName(AUTHENTICATION_USER);

            } else {
                customQuery.setCollectionName("demo");
            }
        }
        customQuery.setType(type);
        customQuery.setQuery(queryContent);
        customQuery.setMany(many);
        customQuery.setQueryName("executeQuery");
        query.setCustomQuery(customQuery);

        if (params != null) {
            query.setParameters(params);
        }

        addCommonConfig(query);

        return query;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public void setDefaultIdPResources(DBSourceDto savedDataSource, String groupName, List<Resource> resources) {
        List<ResourceParameter> params1 = new ArrayList<>();
        params1.add(new ResourceParameter("client_id", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
        params1.add(new ResourceParameter("client_secret", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));

        Resource jwk = createQueryApi(groupName, "/jwk", HttpMethod.GET.getValue(), savedDataSource,
                "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);

        Resource roles = createQueryApi(groupName, "/getRoles", HttpMethod.GET.getValue(), savedDataSource,
                "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);

        // create security APIs
        Resource signIn = createQueryApi(groupName, SIGN_IN, HttpMethod.POST.getValue(), savedDataSource, "{}",
                QueryType.INSERT.getValue(), false, null);
        Resource signUp = createQueryApi(groupName, SIGN_UP, HttpMethod.POST.getValue(), savedDataSource, "{}",
                QueryType.INSERT.getValue(), false, null);
        Resource getAllUsers = createQueryApi(groupName, ALL_USERS, HttpMethod.GET.getValue(), savedDataSource,
                "{}",
                QueryType.INSERT.getValue(), true, null);
        Resource grantRole = createQueryApi(groupName, GRANT, HttpMethod.PUT.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource activateUser = createQueryApi(groupName, ACTIVATE, HttpMethod.POST.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource updateUser = createQueryApi(groupName, UPDATE_USER, HttpMethod.PUT.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));

        Resource deleteUser = createQueryApi(groupName, DELETE_USER, HttpMethod.DELETE.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));

        Resource meUser = createQueryApi(groupName, ME, HttpMethod.GET.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_SESSION_USERNAME.getValue(), QueryType.INSERT.getValue(), false, null);
        resources.add(signIn);
        resources.add(signUp);
        resources.add(getAllUsers);
        resources.add(activateUser);
        resources.add(grantRole);
        resources.add(updateUser);
        resources.add(deleteUser);
        resources.add(meUser);
        resources.add(roles);
        resources.add(jwk);

    }

    public void setSQLDefaultIdPResources(DBSourceDto savedDataSource, String groupName, List<Resource> resources) {
        List<ResourceParameter> params1 = new ArrayList<>();
        params1.add(new ResourceParameter("client_id", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
        params1.add(new ResourceParameter("client_secret", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));

        Resource jwk = createQueryApi(groupName, "/jwk", HttpMethod.GET.getValue(), savedDataSource,
                "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);

        Resource roles = createQueryApi(groupName, "/getRoles", HttpMethod.GET.getValue(), savedDataSource,
                "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);

        // create security APIs
        Resource signIn = createQueryApi(groupName, SIGN_IN, HttpMethod.POST.getValue(), savedDataSource, "{}",
                QueryType.INSERT.getValue(), false, null);
        Resource signUp = createQueryApi(groupName, SIGN_UP, HttpMethod.POST.getValue(), savedDataSource, "{}",
                QueryType.INSERT.getValue(), false, null);
        Resource getAllUsers = createQueryApi(groupName, ALL_USERS, HttpMethod.GET.getValue(), savedDataSource,
                "{}",
                QueryType.INSERT.getValue(), true, null);

        Resource meUser = createQueryApi(groupName, "/me", HttpMethod.GET.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_SESSION_USERNAME.getValue(), QueryType.INSERT.getValue(), false, null);

        Resource deleteUser = createQueryApi(groupName, DELETE_USER, HttpMethod.DELETE.getValue(), savedDataSource,
                PredefinedQuery.SQL_DELETE_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource activateUser = createQueryApi(groupName, ACTIVATE, HttpMethod.POST.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource updateUser = createQueryApi(groupName, UPDATE_USER, HttpMethod.PUT.getValue(), savedDataSource,
                PredefinedQuery.SQL_UPDATE_USER.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        Resource grantRole = createQueryApi(groupName, GRANT, HttpMethod.PUT.getValue(), savedDataSource,
                PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                        null, Const.QUERY.getValue())));
        resources.add(signIn);
        resources.add(signUp);
        resources.add(roles);
        resources.add(getAllUsers);
        resources.add(activateUser);
        resources.add(grantRole);
        resources.add(updateUser);
        resources.add(deleteUser);
        resources.add(meUser);
        resources.add(jwk);
    }

    public void setResources(DBSourceDto savedDataSource, String groupName, List<Resource> resources) {
        if (savedDataSource == null) {
            List<ResourceParameter> params = new ArrayList<>();
            List<ResourceParameter> params1 = new ArrayList<>();
            params.add(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(), null, Const.QUERY.getValue()));
            params.add(new ResourceParameter("password", Const.STRING.getValue(), null, Const.QUERY.getValue()));
            params.add(new ResourceParameter(IDENTITY_PROVIDER, Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
            params1.add(new ResourceParameter("client_id", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
            params1.add(new ResourceParameter("client_secret", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));

            Resource auth = createQueryApi(groupName, OAUTH_AUTHORIZATION,
                    HttpMethod.GET.getValue(), null,
                    "{\"username\":\"%username\",\"password\":\"%password\",\"identityProvider\":\"%identityProvider\"}",
                    null, false, params);
            Resource userinfo = createQueryApi(groupName, OAUTH_USERINFO,
                    HttpMethod.GET.getValue(), null, "{\"identityProvider\":\"%identityProvider\"}",
                    null, false,
                    Collections.singletonList(new ResourceParameter(IDENTITY_PROVIDER, Const.STRING.getValue(), null, Const.QUERY.getValue(), true)));
            Resource jwk = createQueryApi(groupName, "/jwk", HttpMethod.GET.getValue(), savedDataSource,
                    "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);
            Resource roles = createQueryApi(groupName, "/getRoles", HttpMethod.GET.getValue(), savedDataSource,
                    "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);
            resources.add(auth);
            resources.add(userinfo);
            resources.add(jwk);
            resources.add(roles);
        } else {
            // create security APIs
            Resource signIn = createQueryApi(groupName, SIGN_IN, HttpMethod.POST.getValue(), savedDataSource, "{}",
                    QueryType.INSERT.getValue(), false, null);
            Resource signUp = createQueryApi(groupName, SIGN_UP, HttpMethod.POST.getValue(), savedDataSource, "{}",
                    QueryType.INSERT.getValue(), false, null);
            Resource getAllUsers = createQueryApi(groupName, ALL_USERS, HttpMethod.GET.getValue(), savedDataSource,
                    "{}",
                    QueryType.INSERT.getValue(), true, null);
            Resource getAllRoles = createQueryApi(groupName, ALL_ROLES, HttpMethod.GET.getValue(), savedDataSource,
                    "{}",
                    QueryType.INSERT.getValue(), true, null);
            Resource grantRole = createQueryApi(groupName, GRANT, HttpMethod.PUT.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            Resource activateUser = createQueryApi(groupName, ACTIVATE, HttpMethod.POST.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            Resource updateUser = createQueryApi(groupName, UPDATE_USER, HttpMethod.PUT.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));

            Resource deleteUser = createQueryApi(groupName, DELETE_USER, HttpMethod.DELETE.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));

            Resource meUser = createQueryApi(groupName, ME, HttpMethod.GET.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_SESSION_USERNAME.getValue(), QueryType.INSERT.getValue(), false, null);
            resources.add(signIn);
            resources.add(signUp);
            resources.add(getAllRoles);
            resources.add(getAllUsers);
            resources.add(activateUser);
            resources.add(grantRole);
            resources.add(updateUser);
            resources.add(deleteUser);
            resources.add(meUser);

        }
    }

    public void setSQLResources(DBSourceDto savedDataSource, String groupName, List<Resource> resources) {
        if (savedDataSource == null) {
            List<ResourceParameter> params = new ArrayList<>();
            List<ResourceParameter> params1 = new ArrayList<>();
            params.add(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(), null, Const.QUERY.getValue()));
            params.add(new ResourceParameter("password", Const.STRING.getValue(), null, Const.QUERY.getValue()));
            params.add(new ResourceParameter(IDENTITY_PROVIDER, Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
            params1.add(new ResourceParameter("client_id", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));
            params1.add(new ResourceParameter("client_secret", Const.STRING.getValue(), null, Const.QUERY.getValue(), true));

            Resource auth = createQueryApi(groupName, OAUTH_AUTHORIZATION,
                    HttpMethod.GET.getValue(), null,
                    "{\"username\":\"%username\",\"password\":\"%password\",\"identityProvider\":\"%identityProvider\"}",
                    null, false, params);
            Resource userinfo = createQueryApi(groupName, OAUTH_USERINFO,
                    HttpMethod.GET.getValue(), null, "{\"identityProvider\":\"%identityProvider\"}",
                    null, false,
                    Collections.singletonList(new ResourceParameter(IDENTITY_PROVIDER, Const.STRING.getValue(), null, Const.QUERY.getValue(), true)));
            Resource jwk = createQueryApi(groupName, "/jwk", HttpMethod.GET.getValue(), savedDataSource,
                    "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);
            Resource roles = createQueryApi(groupName, "/getRoles", HttpMethod.GET.getValue(), savedDataSource,
                    "{\"client_id\":\"%client_id\",\"client_secret\":\"%client_secret\"}", null, false, params1);
            resources.add(auth);
            resources.add(userinfo);
            resources.add(jwk);
            resources.add(roles);
        } else {
            // create security APIs
            Resource signIn = createQueryApi(groupName, SIGN_IN, HttpMethod.POST.getValue(), savedDataSource, "{}",
                    QueryType.INSERT.getValue(), false, null);
            Resource signUp = createQueryApi(groupName, SIGN_UP, HttpMethod.POST.getValue(), savedDataSource, "{}",
                    QueryType.INSERT.getValue(), false, null);
            Resource getAllUsers = createQueryApi(groupName, ALL_USERS, HttpMethod.GET.getValue(), savedDataSource,
                    "{}",
                    QueryType.INSERT.getValue(), true, null);

            Resource getAllRoles = createQueryApi(groupName, ALL_ROLES, HttpMethod.GET.getValue(), savedDataSource,
                    "{}",
                    QueryType.INSERT.getValue(), true, null);

            Resource meUser = createQueryApi(groupName, "/me", HttpMethod.GET.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_SESSION_USERNAME.getValue(), QueryType.INSERT.getValue(), false, null);

            Resource deleteUser = createQueryApi(groupName, DELETE_USER, HttpMethod.DELETE.getValue(), savedDataSource,
                    PredefinedQuery.SQL_DELETE_BY_USERNAME.getValue(), QueryType.INSERT.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            Resource activateUser = createQueryApi(groupName, ACTIVATE, HttpMethod.POST.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            Resource updateUser = createQueryApi(groupName, UPDATE_USER, HttpMethod.PUT.getValue(), savedDataSource,
                    PredefinedQuery.SQL_UPDATE_USER.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            Resource grantRole = createQueryApi(groupName, GRANT, HttpMethod.PUT.getValue(), savedDataSource,
                    PredefinedQuery.FIND_BY_USERNAME.getValue(), QueryType.UPDATE.getValue(), false,
                    Collections.singletonList(new ResourceParameter(Const.USERNAME.getValue(), Const.STRING.getValue(),
                            null, Const.QUERY.getValue())));
            resources.add(signIn);
            resources.add(signUp);
            resources.add(getAllRoles);
            resources.add(getAllUsers);
            resources.add(activateUser);
            resources.add(grantRole);
            resources.add(updateUser);
            resources.add(deleteUser);
            resources.add(meUser);

        }
    }

    public List<Resource> createSQLAuthGroup(boolean defaultIdP, DBSourceDto savedDataSource, String groupName) {

        List<Resource> resources = new ArrayList<>();
        setSQLResources(savedDataSource, groupName, resources);
        resources.forEach(resource -> convertSwaggerResponseToOpenApiResponse(resource.getResponses(), resource));

        return resources;

    }

    public List<Resource> createAuthGroup(boolean defaultIdP, DBSourceDto savedDataSource, String groupName) {

        List<Resource> resources = new ArrayList<>();
        if (defaultIdP) {
            setDefaultIdPResources(savedDataSource, groupName, resources);
        } else {
            setResources(savedDataSource, groupName, resources);
        }

        if (savedDataSource != null && savedDataSource.getId() != null && !savedDataSource.getType().equals("sql") && savedDataSource.getProvider().equals(Provider.MONGO)) {
            dbSourceService.addFirstCollections(
                    dbSourceService.getOnPremiseMClient(dBSourceMapper.mapToDomain(savedDataSource)),
                    savedDataSource.getDatabase(), AUTHENTICATION_USER);
        }

        resources.forEach(resource -> convertSwaggerResponseToOpenApiResponse(resource.getResponses(), resource));

        return resources;

    }

    public ContainerDto editorProjectExample() throws Exception {
        Container containerTosave = containerService.createProjectExampleForContainer();
        Container savedContainer = containerRepository.save(containerTosave);
        InputStream is = new URL("https://petstore.swagger.io/v2/swagger.json").openConnection().getInputStream();

        File file = new File("petstore.json");
        FileUtils.copyInputStreamToFile(is, file);

        FileInputStream input = new FileInputStream(file);

        MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain",
                IOUtils.toByteArray(input));

        return containerSwaggerService.importSwaggerOnExistingContainer(multipartFile, savedContainer.getId(), "true",
                false);

    }

    private void convertSwaggerResponseToOpenApiResponse(List<APIResponse> apiResponses, Resource resource) {
        List<OpenAPIResponse> openApiResponses = new ArrayList<>();
        apiResponses.forEach(apiResponse -> {
            OpenAPIResponse openAPIResponse = new OpenAPIResponse();
            openAPIResponse.setCode(apiResponse.getCode());
            openAPIResponse.setDescription(apiResponse.getDescription());
            if (apiResponse.getHeaders() != null) {
                openAPIResponse.setHeaders(apiResponse.getHeaders());
            }
            if (apiResponse.getSchema() != null && !resource.getProduces().isEmpty()) {
                Map<String, ContentEelement> content = new HashMap<>();
                resource.getProduces().forEach(produce -> {
                    ContentEelement contentElement = new ContentEelement();
                    contentElement.setSchema(apiResponse.getSchema());
                    content.put(produce, contentElement);
                });
                openAPIResponse.setContent(content);
            }
            openApiResponses.add(openAPIResponse);
        });
        resource.setOpenAPIResponses(openApiResponses);
    }

}
