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
package fr.codeonce.grizzly.core.service.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.common.runtime.AWSCredentials;
import fr.codeonce.grizzly.core.domain.container.*;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.endpointModel.*;
import fr.codeonce.grizzly.core.domain.enums.HttpMethod;
import fr.codeonce.grizzly.core.domain.enums.SecurityLevel;
import fr.codeonce.grizzly.core.domain.function.Function;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.*;
import fr.codeonce.grizzly.core.service.swagger.utils.IDefinitionGenerator;
import fr.codeonce.grizzly.core.service.swagger.utils.ParamOpenApiFactory;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenAPIMapperService extends OpenAPIMapperImpl {

    private static final String AUTH_GRIZZLY = "Authentication Grizzly";
    private static final String AUTHMSRUNTIMEURL = "authMSRuntimeUrl";

    @Autowired
    Environment environment;

    @Autowired
    private IDefinitionGenerator definitionGenerator;

    @Autowired
    private InvalidSwaggerRepository invalidSwaggerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DBSourceRepository dbRepository;

    @Autowired
    private FunctionRepository functionRepository;

    private ParamOpenApiFactory factory = ParamOpenApiFactory.getInstance();

    private static final Logger log = LoggerFactory.getLogger(OpenAPIMapperService.class);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public OpenAPI mapToOpenAPI(Container container, String type) {
        Project project = projectRepository.findById(container.getProjectId())
                .orElseThrow(GlobalExceptionUtil.notFoundException(DBSource.class, container.getProjectId()));

        OpenAPI swagger = super.mapToOpenAPI(container);
        if (container.getDescription() != null) {
            swagger.getInfo().setDescription(container.getDescription());
        }
        swagger.getInfo().setTitle(project.getName());
        swagger.getInfo().setVersion(container.getName());

        // setting contact
        if (container.getContact() != null) {
            io.swagger.v3.oas.models.info.Contact contact = new io.swagger.v3.oas.models.info.Contact();
            contact.setEmail(container.getContact().getEmail());
            contact.setName(container.getContact().getName());
            contact.setUrl(container.getContact().getUrl());
            swagger.getInfo().setContact(contact);

        }

        // setting license
        if (container.getLicense() != null) {
            io.swagger.v3.oas.models.info.License license = new io.swagger.v3.oas.models.info.License();
            license.setUrl(container.getLicense().getUrl());
            license.setName(container.getLicense().getName());
            swagger.getInfo().setLicense(license);
        }

        if (container.getTermsOfService() != null) {
            swagger.getInfo().setTermsOfService(container.getTermsOfService());
        }
        swagger.setTags(container.getResourceGroups().stream().map(super::mapToTag).collect(Collectors.toList()));

        Paths paths = fetchOpenApiPathsFromContainer(container, type);

        if (container.getServers() != null) {
            List<Server> servers = new ArrayList<Server>();
            container.getServers().forEach(ser -> {
                Server server = new Server();
                server.setDescription(ser.getDescription());
                server.setUrl(ser.getUrl());
                servers.add(server);
            });
            // Convert Host + BasePath -> Server and add it to the existing list
            if (container.getHost() != null && container.getBasePath() != null) {
                Server server = new Server();
                if (container.getHost().substring(container.getHost().length() - 1).equals("/")
                        || container.getBasePath().substring(0).equals("/")) {
                    server.setUrl(container.getHost() + container.getBasePath());
                } else {
                    server.setUrl(container.getHost() + "/" + container.getBasePath());
                }
                if (!containsServers(servers, server.getUrl())) {
                    servers.add(server);
                }
            }
            swagger.setServers(servers);

        }

        if (paths.size() != 0) {
            swagger.setPaths(paths);
        } else {
            Paths emPaths = new Paths();
            swagger.setPaths(emPaths);
        }
        swagger.externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation().url(container.getHost()));
        Components components = new Components();

        components.setSchemas(definitionGenerator.generateSchemaDefiniton(container));
        Map<String, ApiResponse> responses = new HashMap<String, ApiResponse>();
        if (container.getResponses() != null) {
            container.getResponses().forEach(el -> {
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setDescription(el.getDescription());
                Content content = new Content();
                el.getContent().forEach((key, value) -> {
                    MediaType media = new MediaType();
                    Schema sch = new Schema();
                    sch.set$ref(value.getSchema().getRef());
                    if (value.getSchema().getArray() != null && value.getSchema().getArray()) {
                        sch.setType("array");
                    }
                    if (value.getSchema().getObject() != null && value.getSchema().getObject()) {
                        sch.setType("object");
                    }
                    if (value.getSchema().getString() != null && value.getSchema().getString()) {
                        sch.setType("string");
                    }
                    media.setSchema(sch);
                    content.addMediaType(key, media);
                });

                apiResponse.setContent(content);
                responses.put(el.getCode(), apiResponse);
            });
        }

        if (container.getRequestBodies() != null) {
            Map<String, io.swagger.v3.oas.models.parameters.RequestBody> requestBodies = new HashMap<String, io.swagger.v3.oas.models.parameters.RequestBody>();
            container.getRequestBodies().forEach(rb -> {
                io.swagger.v3.oas.models.parameters.RequestBody requestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
                Content content = new Content();
                rb.getContent().forEach((key, value) -> {
                    MediaType media = new MediaType();
                    Schema sch = new Schema();
                    sch.set$ref(value.getSchema().getRef());
                    sch.setDescription(value.getSchema().getDescription());
                    media.setSchema(sch);
                    content.addMediaType(key, media);
                });

                requestBody.setContent(content);
                requestBody.setRequired(rb.getRequired());
                requestBody.setDescription(rb.getDescription());
                requestBodies.put(rb.getName(), requestBody);

            });

            components.setRequestBodies(requestBodies);
        }
        SecurityScheme ss = new SecurityScheme();
        if (project.getAuthMSRuntimeURL() != null) {
            ss.setDescription(project.getAuthMSRuntimeURL());
            ss.setType(SecurityScheme.Type.APIKEY);
            ss.setIn(SecurityScheme.In.HEADER);
            ss.setName("Authorization");
            components.addSecuritySchemes(AUTHMSRUNTIMEURL, ss);
        }
        if (project.isSecurityEnabled() && project.getAuthMSRuntimeURL() == null) {
            ss.setType(SecurityScheme.Type.APIKEY);
            ss.setIn(SecurityScheme.In.HEADER);
            if (!project.getType().equals("authentication microservice")) {
                ss.setDescription("classic auth");
                ss.setName("Authorization");
            } else {
                ss.setDescription("iam api");
                ss.setName("Authorization");
            }
            components.addSecuritySchemes("auth", ss);
        }
        components.setResponses(responses);
        swagger.setComponents(components);
        return swagger;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Resource mapToResource(Operation operation, OpenAPI swagger) {
        List<OpenAPIResponse> responsesList = new ArrayList<OpenAPIResponse>();
        Resource resource = super.mapToResource(operation, swagger);
        try {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {
                    OpenAPIResponse apiResponse = new OpenAPIResponse();
                    apiResponse.setCode(code);
                    apiResponse.setDescription(response.getDescription());
                    if (response.getHeaders() != null) {
                        Map<String, Header> headers = new HashMap<String, Header>();
                        response.getHeaders().forEach((title, value) -> {
                            if (value.getSchema() != null) {
                                Header header = new Header(value.getSchema().getType(), value.getDescription(),
                                        value.getSchema().getFormat());
                                headers.put(title, header);
                            }

                        });
                        Headers allHeaders = new Headers();
                        allHeaders.setHeaders(headers);
                        apiResponse.setHeaders(allHeaders);

                    }
                    if (response.getContent() != null) {
                        Map<String, ContentEelement> content = new HashMap<String, ContentEelement>();
                        response.getContent().forEach((k, val) -> {

                            ContentEelement element = new ContentEelement();
                            Map<String, ExampleObject> examples = new HashMap<String, ExampleObject>();
                            if (val.getExample() != null) {
                                Object v = val.getExample();
                                ExampleObject example = new ExampleObject();
                                String value = null;
                                if (value == null) {
                                    ObjectMapper mapper = new ObjectMapper();
                                    try {
                                        example.setValue(mapper.writeValueAsString(v));
                                    } catch (JsonProcessingException e) {
                                        log.error("error in json parsing", e);
                                    }
                                }

                                examples.put("Example", example);

                            }
                            if (val.getExamples() != null) {

                                val.getExamples().forEach((key, v) -> {
                                    ExampleObject example = new ExampleObject();
                                    String value = null;
                                    if (v.get$ref() != null) {
                                        if (v.get$ref().contains("#/components/examples/")) {
                                            swagger.getComponents().getExamples().get(v.get$ref()
                                                            .substring("#/components/examples/".length(), v.get$ref().length()))
                                                    .getValue().toString();
                                            value = String.valueOf(swagger.getComponents().getExamples().get(v.get$ref()
                                                            .substring("#/components/examples/".length(), v.get$ref().length()))
                                                    .getValue());
                                            example.setValue(value);
                                        }

                                    }
                                    example.setDescription(v.getDescription());
                                    example.setExternalValue(v.getExternalValue());
                                    example.setSummary(v.getSummary());
                                    if (value == null) {
                                        ObjectMapper mapper = new ObjectMapper();
                                        try {
                                            example.setValue(mapper.writeValueAsString(v.getValue()));
                                        } catch (JsonProcessingException e) {
                                            log.error("error in json parsing", e);
                                        }
                                    }

                                    examples.put(key, example);
                                });
                            }
                            element.setExamples(examples);
                            if (val.getSchema() != null) {
                                APISchema schema = new APISchema();
                                if (val.getSchema().get$ref() != null) {
                                    schema = new APISchema(false, false, false);
                                    schema.setRef(val.getSchema().get$ref().substring("#/components/schemas/".length(),
                                            val.getSchema().get$ref().length()));

                                    element.setSchema(schema);
                                    content.put(k, element);

                                } else {
                                    if (val.getSchema().getType().equalsIgnoreCase("array")) {
                                        ArraySchema arraySchema = (ArraySchema) val.getSchema();
                                        schema = new APISchema(true, false, false);
                                        schema.setRef(arraySchema.getItems().get$ref()
                                                .substring("#/components/schemas/".length()));
                                        element.setSchema(schema);
                                        content.put(k, element);
                                    }
                                }
                                apiResponse.setContent(content);
                            }
                        });
                    }
                    responsesList.add(apiResponse);

                });
            }
        } catch (Exception e) {
            log.debug("an error has occured {}", e);
        }

        resource.setOpenAPIResponses(responsesList);
        // support the transformation between swagger V3 -> V2 in responses
        convertOpenApiResponseToSwaggerResponse(responsesList, resource);
        return resource;
    }

    private void convertOpenApiResponseToSwaggerResponse(List<OpenAPIResponse> openApiResponses, Resource resource) {
        List<APIResponse> responses = new ArrayList<APIResponse>();
        openApiResponses.forEach(openApiResponse -> {
            APIResponse apiResponse = new APIResponse();
            apiResponse.setCode(openApiResponse.getCode());
            apiResponse.setDescription(openApiResponse.getDescription());
            if (openApiResponse.getHeaders() != null) {
                apiResponse.setHeaders(openApiResponse.getHeaders());
            }

            if (openApiResponse.getContent() != null) {
                if (!openApiResponse.getContent().values().isEmpty()) {
                    APISchema schema = openApiResponse.getContent().values().stream().collect(Collectors.toList())
                            .get(0).getSchema();
                    apiResponse.setSchema(schema);
                    if (openApiResponse.getContent().values().stream().collect(Collectors.toList()).get(0)
                            .getExamples() != null) {
                        Map<String, Object> exemples = new HashMap<String, Object>();
                        openApiResponse.getContent().values().stream().collect(Collectors.toList()).get(0).getExamples()
                                .forEach((key, value) -> {
                                    if (value.getValue() != null) {
                                        exemples.put(key, value.getValue());
                                    }
                                });
                        apiResponse.setExemples(exemples);
                    }

                }
                openApiResponse.getContent().forEach((k, v) -> {
                    if (!resource.getProduces().contains(k)) {
                        resource.getProduces().add(k);
                    }
                });
            }
            responses.add(apiResponse);
        });
        resource.setResponses(responses);
    }

    public Container mapToContainer(OpenAPI swagger, String projectId, String content, String editor, boolean docker) throws Exception {
        Container container = mapToContainer(swagger, content);
        this.projectRepository.findById(projectId).ifPresent(project -> {
            container.setDescription(swagger.getInfo().getDescription());
            if (editor.equals("true") || docker == true) {
                project.setName(swagger.getInfo().getTitle().replaceAll("[^a-zA-Z0-9]", "_"));
            }
            // project.setName(swagger.getInfo().getTitle());
            if (swagger.getExternalDocs() != null
                    && (swagger.getExternalDocs().getUrl().equals("dwk5c1lsnem8l.cloudfront.net")
                    || swagger.getExternalDocs().getUrl().equals("localhost:4900")
                    || swagger.getExternalDocs().getUrl().equals("grizzly-api.com"))) {
                if (swagger.getComponents().getSecuritySchemes() != null) {
                    project.setSecurityEnabled(true);
                    if (swagger.getComponents().getSecuritySchemes().get(AUTHMSRUNTIMEURL) != null) {
                        project.setAuthMSRuntimeURL(swagger.getComponents().getSecuritySchemes().get(AUTHMSRUNTIMEURL).getDescription());
                    }
                    if (swagger.getComponents().getSecuritySchemes().get("auth") != null) {
                        project.setAuthMSRuntimeURL(null);
                        if (swagger.getComponents().getSecuritySchemes().get("auth").getDescription().equals("classic auth")) {
                            project.setType("microservice");
                            project.getSecurityConfig().setClientId(project.getName());
                            project.getSecurityConfig()
                                    .setSecretKey(DigestUtils.sha256Hex(project.getName()) + DigestUtils.sha256Hex("co%de01/"));
                            if (project.getRoles().isEmpty()) {
                                project.getRoles().add(SecurityLevel.ADMIN.getValue());
                                project.getRoles().add("user");
                            }
                        }
                        if (swagger.getComponents().getSecuritySchemes().get("auth").getDescription().equals("iam api")) {
                            project.setType("authentication microservice");
                        }

                    }
                } else {
                    project.setSecurityEnabled(false);
                }

            }

            Project savedProject = this.projectRepository.save(project);
            container.setProjectId(savedProject.getId());
            if (project.getDbsourceId() != null) {
                this.dbRepository.findById(project.getDbsourceId()).ifPresent(db -> {
                    String databaseName;
                    if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                        databaseName = db.getPhysicalDatabase();
                    } else {
                        databaseName = project.getDatabaseName();
                    }
                    fetchContainerResources(swagger, container, project.getDbsourceId(), databaseName);
                });
            } else {
                // fetch resources without database
                fetchContainerResources(swagger, container, project.getDbsourceId(), "");
            }

        });
        return container;
    }

    private void getTags(Operation operation, List<ResourceGroup> lrg) {

        if (operation != null) {
            if (operation.getTags() != null) {
                operation.getTags().forEach(tag -> {
                    ResourceGroup rg = new ResourceGroup();
                    rg.setName(tag);
                    rg.setDescription("");
                    if (!lrg.stream().map(el -> el.getName()).anyMatch(el -> el.equals(rg.getName())))
                        lrg.add(rg);
                });
            }
        }
    }

    @Override
    public Container mapToContainer(OpenAPI swagger, String fileContent) throws Exception {
        try {
            if (swagger != null) {
                Container container = super.mapToContainer(swagger, fileContent);
                List<ResourceGroup> lrg = new ArrayList<>();
                swagger.getPaths().forEach((k, v) -> {
                    getTags(v.getGet(), lrg);
                    getTags(v.getPost(), lrg);
                    getTags(v.getPut(), lrg);
                    getTags(v.getDelete(), lrg);
                });
                if (lrg.isEmpty()) {
                    ResourceGroup rg = new ResourceGroup();
                    rg.setName("Default");
                    rg.setDescription("");
                    if (lrg.stream().map(el -> el.getName()).noneMatch(el -> el.equals(rg.getName())))
                        lrg.add(rg);
                }

                container.setResourceGroups(lrg);
                if (swagger.getInfo().getContact() != null) {
                    Contact contact = new Contact(swagger.getInfo().getContact().getName(),
                            swagger.getInfo().getContact().getUrl(), swagger.getInfo().getContact().getEmail());
                    container.setContact(contact);
                }
                List<fr.codeonce.grizzly.core.domain.container.Server> servers = new ArrayList<>();
                swagger.getServers().forEach(ser -> {
                    fr.codeonce.grizzly.core.domain.container.Server server = new fr.codeonce.grizzly.core.domain.container.Server();
                    server.setDescription(ser.getDescription());
                    server.setUrl(ser.getUrl());
                    servers.add(server);
                });
                container.setServers(servers);
                if (swagger.getInfo().getLicense() != null) {
                    License license = new License(swagger.getInfo().getLicense().getName(),
                            swagger.getInfo().getLicense().getUrl());
                    container.setLicense(license);
                }
                List<OpenAPIResponse> responses = new ArrayList<>();
                if (swagger.getComponents().getResponses() != null) {
                    swagger.getComponents().getResponses().forEach((k, v) -> {
                        OpenAPIResponse apiresponse = new OpenAPIResponse();
                        apiresponse.setCode(k);
                        apiresponse.setDescription(v.getDescription());
                        Map<String, ContentEelement> content = new HashMap<>();
                        if (v.getContent() != null) {
                            v.getContent().forEach((ke, va) -> {
                                ContentEelement contentElement = new ContentEelement();
                                APISchema schema = new APISchema();
                                schema.setRef(va.getSchema().get$ref());
                                if (va.getSchema().getType() != null) {
                                    schema.setObject(va.getSchema().getType().equalsIgnoreCase("object"));
                                    schema.setArray(va.getSchema().getType().equalsIgnoreCase("array"));
                                    schema.setString(va.getSchema().getType().equalsIgnoreCase("string"));
                                }
                                contentElement.setSchema(schema);
                                content.put(ke, contentElement);
                            });
                        }
                        apiresponse.setContent(content);
                        responses.add(apiresponse);
                    });
                    container.setResponses(responses);
                }

                // Delete Existing "Authentication Grizzly" Resource Group
                container.setResourceGroups(container.getResourceGroups().stream()
                        .filter(rg -> !rg.getName().equalsIgnoreCase("Authentication Grizzly"))
                        .collect(Collectors.toList()));
                return container;
            } else {
                InvalidSwagger invalidSwagger = new InvalidSwagger();
                invalidSwagger.setContent(fileContent);
                invalidSwaggerRepository.save(invalidSwagger);
                throw new Exception("Swagger format is invalid for more details contact us on support@codeonce.fr");
            }
        } catch (Exception e) {
            log.error("import error : {}", e);
            InvalidSwagger invalidSwagger = new InvalidSwagger();
            invalidSwagger.setContent(fileContent);
            invalidSwaggerRepository.save(invalidSwagger);
            throw new Exception("Swagger format is invalid for more details contact us on support@codeonce.fr");
        }

    }

    private void makeOperation(OpenAPI swagger, Operation operation, HttpMethod httpMethod, List<Resource> resources,
                               String pathName, String dbSourceID, String dbName, String projectId) {
        if (operation.getTags() != null) {
            if (!operation.getTags().contains("Authentication Grizzly")) {
                Resource resource = setResourceFields(swagger, operation, pathName, httpMethod, dbSourceID, dbName,
                        projectId);
                if (operation.getParameters() != null) {
                    resource.setParameters(operation.getParameters().stream().map(factory::makeResourceParameter)
                            .collect(Collectors.toList()));
                }
                resources.add(resource);
            }
        } else {
            Resource resource = setResourceFields(swagger, operation, pathName, httpMethod, dbSourceID, dbName,
                    projectId);
            if (operation.getParameters() != null) {
                resource.setParameters(operation.getParameters().stream().map(factory::makeResourceParameter)
                        .collect(Collectors.toList()));
            }
            resources.add(resource);
        }
    }

    private boolean containsBodyParam(final List<io.swagger.v3.oas.models.parameters.Parameter> list,
                                      final String name) {
        return list.stream().map(io.swagger.v3.oas.models.parameters.Parameter::getName).filter(name::equalsIgnoreCase)
                .findFirst().isPresent();
    }

    @SuppressWarnings("rawtypes")
    private Operation setOperationFields(Resource resource, String type) {

        Operation operation = super.mapToOperation(resource);
        operation.setTags(Collections.singletonList(resource.getResourceGroup()));
        /*
         * Calling Factory to Get the suitable Parameter depending on parameter In field
         */
        operation.setParameters(resource.getParameters().stream().filter(el -> !el.getIn().equalsIgnoreCase("body"))
                .map(factory::makeParameter).collect(Collectors.toList()));
        if (resource.getCustomQuery() != null && (resource.getExecutionType().equalsIgnoreCase("query")
                && (resource.getHttpMethod().equalsIgnoreCase("post")
                || resource.getHttpMethod().equalsIgnoreCase("put")))) {
            if ((resource.getCustomQuery().getType() != null
                    && (resource.getCustomQuery().getType().equalsIgnoreCase("Insert")
                    || resource.getCustomQuery().getType().equalsIgnoreCase("Update")))
                    && !containsBodyParam(operation.getParameters(), "body")
                    && resource.getParameters().stream().noneMatch(param -> param.getType().equalsIgnoreCase("file"))) {

                // Attach Sign-In and Sign-Up models to AUTH APIs
                if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/signup")) {
                    // operation.getParameters().add(factory.getModelDef("singnup"));
                    operation.setRequestBody(factory.getModelDef("singnup", "SignUp Model"));
                } else if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/signin")) {
                    operation.setRequestBody(factory.getModelDef("signin", "SignIn Model"));
                }
            }

        }

        if (resource.getRequestBody() != null) {

            io.swagger.v3.oas.models.parameters.RequestBody rb = new io.swagger.v3.oas.models.parameters.RequestBody();
            rb.setDescription(resource.getRequestBody().getDescription());
            rb.setRequired(resource.getRequestBody().getRequired());
            if (resource.getRequestBody().getContent() != null) {
                Content c = new Content();
                resource.getRequestBody().getContent().forEach((k, v) -> {
                    Schema sc = new Schema();
                    sc.set$ref(v.getSchema().getRef());
                    sc.setFormat(v.getSchema().getFormat());
                    sc.setDescription(v.getSchema().getDescription());
                    sc.setRequired(v.getSchema().getRequired());
                    sc.setType(v.getSchema().getType());
                    MediaType mt = new MediaType();
                    if (v.getSchema().getType() != null) {
                        if (v.getSchema().getType().equals("array")) {
                            ArraySchema as = new ArraySchema();
                            as.setDescription(v.getSchema().getDescription());
                            as.setRequired(v.getSchema().getRequired());
                            as.setFormat(v.getSchema().getFormat());
                            as.setType("array");
                            Schema items = new Schema();
                            items.$ref("#/components/schemas/" + v.getSchema().getRef());
                            as.setItems(items);
                            mt.setSchema(as);
                        } else {
                            mt.setSchema(sc);
                        }
                    } else {
                        mt.setSchema(sc);
                    }

                    c.addMediaType(k, mt);

                });
                rb.setContent(c);
            }
            if (rb.getContent() != null) {
                operation.setRequestBody(rb);
            }

        }
        if (resource.getOpenAPIResponses() != null) {
            ApiResponses responses = new ApiResponses();
            resource.getOpenAPIResponses().forEach(resp -> {
                ApiResponse response = new ApiResponse();
                Content content = new Content();
                MediaType mediaType = new MediaType();
                response.setDescription(resp.getDescription());
                if (resp.getHeaders() != null) {
                    Map<String, io.swagger.v3.oas.models.headers.Header> headers = new HashMap<>();
                    resp.getHeaders().getHeaders().forEach((c, v) -> {

                        io.swagger.v3.oas.models.headers.Header header = new io.swagger.v3.oas.models.headers.Header();
                        Schema schema = new Schema();
                        schema.setType(v.getType());
                        schema.setFormat(v.getFormat());
                        header.setDescription(v.getDescription());
                        header.setSchema(schema);
                        headers.put(c, header);

                    });
                    response.setHeaders(headers);
                }
                if (resp.getContent() != null) {
                    resp.getContent().forEach((k, v) -> {
                        String contentKey = "";
                        if (k.equals("*/*")) {
                            contentKey = "'" + k + "'";
                        } else {
                            contentKey = k;
                        }
                        if (v.getExamples() != null) {
                            Map<String, Example> examples = new HashMap<>();
                            v.getExamples().forEach((key, value) -> {
                                Example example = new Example();
                                example.set$ref(value.get$ref());
                                example.setDescription(value.getDescription());
                                example.setExternalValue(value.getExternalValue());
                                example.setSummary(value.getSummary());
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    if (value.getValue() != null) {
                                        example.setValue(convertToObject(mapper.readTree(value.getValue())));
                                    }

                                } catch (JsonProcessingException e) {
                                    log.error("error in json parsing", e);
                                }
                                examples.put(key, example);
                            });
                            mediaType.setExamples(examples);
                        }
                        if (v.getSchema() != null) {
                            if (v.getSchema().getArray()) {
                                if (v.getSchema().getRef() != null) {
                                    if (!v.getSchema().getRef().equals("")) {
                                        Schema schema = new Schema();
                                        schema.set$ref(v.getSchema().getRef());
                                        ArraySchema arraySchema = new ArraySchema();
                                        arraySchema.setItems(schema);
                                        mediaType.setSchema(arraySchema);
                                        content.put(contentKey, mediaType);
                                        response.setContent(content);
                                    }
                                }
                            } else {
                                if (!(v.getSchema().getRef() == null)) {
                                    if (!v.getSchema().getRef().equals("")) {
                                        Schema schema = new Schema();
                                        schema.set$ref(v.getSchema().getRef());
                                        mediaType.setSchema(schema);
                                        content.put(contentKey, mediaType);
                                        response.setContent(content);
                                    }
                                }

                            }
                            if (v.getSchema().getObject() != null) {
                                if (v.getSchema().getObject()) {
                                    ModelImpl model = new ModelImpl();
                                    Schema schema = new Schema();
                                    Schema embeddedSchema = new Schema();
                                    embeddedSchema.setType(v.getSchema().getAdditionalProperties().getType());
                                    embeddedSchema.setFormat(v.getSchema().getAdditionalProperties().getFormat());
                                    if (v.getSchema().getAdditionalProperties() != null) {
                                        model.setAdditionalProperties(
                                                PropertyBuilder.build(v.getSchema().getAdditionalProperties().getType(),
                                                        v.getSchema().getAdditionalProperties().getFormat(), null));
                                    }

                                    schema.setType("object");
                                    mediaType.setSchema(schema);
                                    content.put(contentKey, mediaType);
                                    response.setContent(content);
                                }

                            }
                            if (v.getSchema().getString() != null && v.getSchema().getString()) {
                                Schema schema = new Schema();
                                schema.setType("string");
                                mediaType.setSchema(schema);
                                content.put(contentKey, mediaType);
                                response.setContent(content);
                            }
                        }
                    });
                }
                responses.put(resp.getCode(), response);
            });
            if (!responses.isEmpty()) {
                operation.setResponses(responses);
            }
        }
        // Add Custom Fields If Swagger Is For Dev Mode
        if (type.equalsIgnoreCase("dev")) {
            if (resource.getName() != null) {
                operation.addExtension("x-name", resource.getName());
            }
            operation.addExtension("x-executionType", resource.getExecutionType());
            /* Fetch associated fileID from files collection */
            if (resource.getResourceFile() != null && resource.getResourceFile().getFileId() != null
                    && !resource.getResourceFile().getFileId().isEmpty()) {

                /* Add Custom fields to Swagger for both ContainerId and FileUri */
                operation.addExtension("x-fileId", resource.getResourceFile().getFileId());
                operation.addExtension("x-fileUri", resource.getResourceFile().getFileUri());
            }
            if (resource.getExecutionType() != null && resource.getExecutionType().equals("Query")) {
                operation.addExtension("x-type", resource.getCustomQuery().getType());
                operation.addExtension("x-collectionName", resource.getCustomQuery().getCollectionName());
                operation.addExtension("x-queryType", resource.getCustomQuery().getQueryName());
                operation.addExtension("x-query", resource.getCustomQuery().getQuery());
                operation.addExtension("x-query-many", resource.getCustomQuery().isMany());
                operation.addExtension("x-query-request-many", resource.getCustomQuery().isRequestMany());
                if (!resource.getOutFunctions().isEmpty()
                        && functionRepository.findById(resource.getOutFunctions().get(0)).isPresent()) {
                    operation.addExtension("x-out-functions",
                            functionRepository.findById(resource.getOutFunctions().get(0)).get());
                }
                if (!resource.getFunctions().isEmpty()
                        && functionRepository.findById(resource.getFunctions().get(0)).isPresent()) {
                    operation.addExtension("x-functions",
                            functionRepository.findById(resource.getFunctions().get(0)).get());
                }
                if (!resource.getInFunctions().isEmpty()
                        && functionRepository.findById(resource.getInFunctions().get(0)).isPresent()) {
                    operation.addExtension("x-in-functions",
                            functionRepository.findById(resource.getInFunctions().get(0)).get());
                }
                // Set Projection Fields
                if (resource.getFields() != null) {
                    StringBuilder fields = new StringBuilder();
                    resource.getFields().stream().forEach(field -> fields.append(field + ','));
                    operation.addExtension("x-fields", fields.toString());
                }
            }
        }

        return operation;
    }

    private Paths fetchOpenApiPathsFromContainer(Container container, String type) {

        Paths paths = new Paths();
        container.getResources().forEach(resource -> {
            Operation operation = setOperationFields(resource, type);

            if (resource.getHttpMethod().equalsIgnoreCase("get")) {

                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setGet(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setGet(operation);
                }

            }
            if (resource.getHttpMethod().equalsIgnoreCase("post")) {
                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setPost(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setPost(operation);
                }

            }
            if (resource.getHttpMethod().equalsIgnoreCase("put")) {

                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setPut(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setPut(operation);
                }

            }
            if (resource.getHttpMethod().equalsIgnoreCase("delete")) {
                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setDelete(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setDelete(operation);
                }

            }
            if (resource.getHttpMethod().equalsIgnoreCase("options")) {
                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setOptions(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setOptions(operation);
                }
            }
            if (resource.getHttpMethod().equalsIgnoreCase("patch")) {
                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setPatch(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setPatch(operation);
                }

            }
            if (resource.getHttpMethod().equalsIgnoreCase("head")) {
                PathItem path = new PathItem();
                if (ObjectUtils.isEmpty(paths.get(resource.getPath().toLowerCase()))) {
                    path.setHead(operation);
                    paths.put(resource.getPath(), path);
                } else {
                    paths.get(resource.getPath()).setHead(operation);
                }

            }
        });

        return paths;

    }

    @SuppressWarnings("unchecked")
    private void fetchContainerResources(OpenAPI swagger, Container container, String dbSourceID, String dbName) {
        List<Resource> resources = new ArrayList<>();
        if (swagger.getPaths() != null) {
            swagger.getPaths().forEach((pathName, pathDetails) -> {
                if (pathDetails.getPost() != null) {
                    makeOperation(swagger, pathDetails.getPost(), HttpMethod.POST, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
                if (pathDetails.getGet() != null) {
                    makeOperation(swagger, pathDetails.getGet(), HttpMethod.GET, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
                if (pathDetails.getPut() != null) {
                    makeOperation(swagger, pathDetails.getPut(), HttpMethod.PUT, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
                if (pathDetails.getDelete() != null) {
                    makeOperation(swagger, pathDetails.getDelete(), HttpMethod.DELETE, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
                if (pathDetails.getPatch() != null) {
                    makeOperation(swagger, pathDetails.getPatch(), HttpMethod.PATCH, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
                if (pathDetails.getOptions() != null) {
                    makeOperation(swagger, pathDetails.getOptions(), HttpMethod.OPTIONS, resources, pathName,
                            dbSourceID, dbName, container.getProjectId());
                }
                if (pathDetails.getTrace() != null) {
                    makeOperation(swagger, pathDetails.getTrace(), HttpMethod.TRACE, resources, pathName, dbSourceID,
                            dbName, container.getProjectId());
                }
            });

            List<EndpointModel> models = fetchEndpointModels(swagger);
            container.setEndpointModels(models);
            container.setResources(resources);
            List<ResourceParameter> parameters = new ArrayList<ResourceParameter>();
            if (swagger.getComponents().getParameters() != null) {
                swagger.getComponents().getParameters().forEach((title, value) -> {
                    ParamOpenApiFactory pf = new ParamOpenApiFactory();
                    parameters.add(pf.makeResourceParameter(value));

                });
            }
            container.setParameters(parameters);
            List<BodyMap> requestBodies = new ArrayList<BodyMap>();
            if (swagger.getComponents().getRequestBodies() != null) {
                swagger.getComponents().getRequestBodies().forEach((title, value) -> {
                    BodyMap bm = new BodyMap();
                    bm.setName(title);
                    bm.setRequired(value.getRequired());
                    Map<String, RequestBody> content = new HashMap<String, RequestBody>();
                    value.getContent().forEach((k, v) -> {
                        BodySchema bs;
                        if (v.getSchema().get$ref() != null) {
                            bs = new BodySchema(v.getSchema().getDescription(), v.getSchema().getFormat(), null,
                                    v.getSchema().get$ref().substring("#/components/schemas/".length(),
                                            v.getSchema().get$ref().length()),
                                    null, v.getSchema().getAdditionalProperties());
                        } else {
                            bs = new BodySchema(v.getSchema().getDescription(), v.getSchema().getFormat(), null, null,
                                    null, v.getSchema().getAdditionalProperties());
                        }

                        RequestBody rb = new RequestBody(bs, null, null, value.getRequired());
                        content.put(k, rb);
                    });
                    bm.setContent(content);
                    bm.setRequired(value.getRequired());
                    bm.setDescription(value.getDescription());
                    requestBodies.add(bm);

                });
            }
            container.setRequestBodies(requestBodies);

        }

    }

    private void manageComposedModels(OpenAPI swagger, ComposedSchema cm, EndpointModel model,
                                      List<ModelProperty> props, List<EndpointModel> models) {
        cm.getAllOf().forEach(sch -> {
            Map<String, Schema> p = sch.getProperties();
            if (p != null) {
                fetchEmbeddedModels(sch, model, models);
            }

            if (sch.get$ref() != null) {
                String refName = sch.get$ref().split("/")[3];
                if (!swagger.getComponents().getSchemas().get(refName).getClass().equals(ComposedSchema.class)) {

                    fetchEmbeddedModels(swagger.getComponents().getSchemas().get(refName), model, models);
                } else {
                    ComposedSchema composedModel = (ComposedSchema) swagger.getComponents().getSchemas().get(refName);
                    manageComposedModels(swagger, composedModel, model, props, models);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<EndpointModel> fetchEndpointModels(OpenAPI swagger) {
        List<EndpointModel> models = new ArrayList<EndpointModel>();
        if (swagger.getComponents().getSchemas() != null) {
            swagger.getComponents().getSchemas().forEach((title, value) -> {
                ArrayList<ModelProperty> props = new ArrayList<ModelProperty>();
                if (!value.getClass().equals(ComposedSchema.class)) {
                    EndpointModel model = new EndpointModel();
                    model.setTitle(title);
                    model.setType(value.getType());

                    if (value.getRequired() != null) {
                        model.setRequired(value.getRequired());
                    }
                    if (value.getDescription() != null) {
                        model.setDescription(value.getDescription().trim());
                    }

                    if (value.getEnum() != null) {
                        List<String> enums = new ArrayList<String>();
                        value.getEnum().forEach(e -> {
                            enums.add(e.toString().trim());
                        });
                        model.setEnums(enums);
                    }

                    fetchEmbeddedModels(value, model, models);
                } else {

                    ComposedSchema cs = (ComposedSchema) value;
                    EndpointModel model = new EndpointModel();
                    model.setTitle(title);

                    if (value.getRequired() != null) {
                        model.setRequired(value.getRequired());
                    }
                    if (value.getDescription() != null) {
                        model.setDescription(value.getDescription());
                    }

                    if (value.getEnum() != null) {
                        List<String> enums = new ArrayList<String>();
                        value.getEnum().forEach(e -> {
                            enums.add(e.toString().trim());
                        });
                        model.setEnums(enums);
                    }

                    manageComposedModels(swagger, cs, model, props, models);

                }
            });
        }
        return models;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<ModelProperty> fetchEmbeddedModels(Schema value, EndpointModel model, List<EndpointModel> models) {
        ArrayList<ModelProperty> properts = new ArrayList<ModelProperty>();
        if (value.getProperties() != null) {

            Map<String, Schema> props = value.getProperties();
            props.forEach((k, v) -> {
                ModelProperty prop = new ModelProperty();
                prop.setName(k);
                prop.setArray(false);
                if (v.getDescription() != null) {
                    prop.setDescription(v.getDescription().trim());
                }
                if (v.getExample() != null) {
                    prop.setExample(v.getExample().toString());
                }

                if (v.getEnum() != null) {
                    List<String> enums = new ArrayList<String>();
                    v.getEnum().forEach(e -> {
                        enums.add(e.toString().trim());
                    });
                    prop.setEnums(enums);
                }
                if (v.get$ref() != null) {
                    prop.setRef(v.get$ref().split("/")[3]);
                }

                if (v.getType() != null) {
                    prop.setType(v.getType());
                    if (v.getType().equalsIgnoreCase("array")) {
                        ArraySchema arr = (ArraySchema) v;
                        if (arr.getItems() != null) {
                            if (arr.getItems().get$ref() != null) {
                                if (arr.getItems().getProperties() != null) {
                                    EndpointModel embeddedModel = new EndpointModel();
                                    embeddedModel.setTitle(k);
                                    prop.setRef(k);
                                    fetchEmbeddedModels(arr.getItems(), embeddedModel, models);
                                } else {
                                    prop.setRef(arr.getItems().get$ref().split("/")[3]);
                                }

                            } else {
                                if (arr.getItems().getProperties() != null) {
                                    EndpointModel embeddedModel = new EndpointModel();
                                    embeddedModel.setTitle(k);

                                    fetchEmbeddedModels(arr.getItems(), embeddedModel, models);
                                    prop.setRef(k);
                                } else {
                                    prop.setRef("");
                                }

                                prop.setType(arr.getItems().getType());

                            }
                        }
                        prop.setArray(true);
                    }
                }

                if (v.getProperties() != null) {
                    EndpointModel embeddedModel = new EndpointModel();
                    embeddedModel.setTitle(k);
                    prop.setRef(k);
                    fetchEmbeddedModels(v, embeddedModel, models);
                }

                properts.add(prop);

            });

        }
        // List<ModelProperty> newList = Stream.concat(properts.stream(),
        // properties.stream()).collect(Collectors.toList());
        model.setProperties(properts);
        if (!containsName(models, model.getTitle())) {
            models.add(model);
        }

        return properts;
    }

    private Resource setResourceFields(OpenAPI swagger, Operation operation, String pathName, HttpMethod httpMethod,
                                       String dbSourceID, String dbName, String projectId) {
        Resource resource = mapToResource(operation, swagger);
        resource.setPath(pathName);
        resource.setHttpMethod(httpMethod.toString());

        if (operation.getTags() != null) {
            resource.setResourceGroup(operation.getTags().get(0));
        } else {
            resource.setResourceGroup("Default");
        }

        if (dbSourceID != null)
            resource.getCustomQuery().setDatasource(dbSourceID);
        if (dbName != null)
            resource.getCustomQuery().setDatabase(dbName);
        resource.setName(extractVendorField(operation, "x-name"));

        String executionType = extractVendorField(operation, "x-executionType");
        if (executionType == null) {
            resource.setExecutionType("");
        } else {
            resource.setExecutionType(executionType);
        }

        if (extractVendorField(operation, "x-fileID") != null) {
            resource.setResourceFile(new ResourceFile(extractVendorField(operation, "x-fileID"),
                    extractVendorField(operation, "x-fileUri")));
        }
        resource.getCustomQuery().setType(extractVendorField(operation, "x-type"));
        if (dbSourceID != null)
            resource.getCustomQuery().setDatasource(dbSourceID);
        if (dbName != null)
            resource.getCustomQuery().setDatabase(dbName);
        // For Query API
        if (executionType != null && executionType.equals(ResourceType.Query.name())) {
            resource.getCustomQuery().setType(extractVendorField(operation, "x-type"));
            resource.getCustomQuery().setCollectionName(extractVendorField(operation, "x-collectionName"));
            resource.getCustomQuery().setQueryName(extractVendorField(operation, "x-queryType"));
            resource.getCustomQuery().setQuery(extractVendorField(operation, "x-query"));
            resource.getCustomQuery().setMany(Boolean.parseBoolean(extractVendorField(operation, "x-query-many")));
            resource.getCustomQuery()
                    .setRequestMany(Boolean.parseBoolean(extractVendorField(operation, "x-query-request-many")));
            try {
                resource.setInFunctions(extractVendorFieldFunction(operation, "x-in-functions", projectId));
                resource.setOutFunctions(extractVendorFieldFunction(operation, "x-out-functions", projectId));
                resource.setFunctions(extractVendorFieldFunction(operation, "x-functions", projectId));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // Set Projection Fields
            String fields = extractVendorField(operation, "x-fields");
            if (fields != null) {
                resource.setFields(Arrays.asList(fields.split("\\,")));
                if (resource.getFields().get(resource.getFields().size() - 1).equals(",")) {
                    resource.getFields().remove(resource.getFields().size() - 1);
                }
            }
        }
        BodyMap requestBody = new BodyMap();

        if (operation.getRequestBody() != null) {
            if (operation.getRequestBody().get$ref() != null) {
                if (operation.getRequestBody().get$ref().contains("#/components/requestBodies/")) {
                    String rbKey = operation.getRequestBody().get$ref().substring(
                            "#/components/requestBodies/".length(), operation.getRequestBody().get$ref().length());
                    io.swagger.v3.oas.models.parameters.RequestBody request = swagger.getComponents().getRequestBodies()
                            .get(rbKey);
                    requestBody.setDescription(request.getDescription());
                    requestBody.setRequired(request.getRequired());

                    Map<String, RequestBody> content = new HashMap<String, RequestBody>();
                    if (request.getContent() != null) {
                        request.getContent().forEach((k, v) -> {
                            if (v.getSchema() != null) {
                                if (v.getSchema().get$ref() != null) {
                                    RequestBody rb = new RequestBody();
                                    BodySchema bs = new BodySchema();
                                    bs.setRef(v.getSchema().get$ref().substring("#/components/schemas/".length(),
                                            v.getSchema().get$ref().length()));
                                    rb.setSchema(bs);
                                    content.put(k, rb);
                                }
                            }
                        });
                    }
                    requestBody.setContent(content);
                }
            } else {
                requestBody.setDescription(operation.getRequestBody().getDescription());
                requestBody.setRequired(operation.getRequestBody().getRequired());

                Map<String, RequestBody> content = new HashMap<String, RequestBody>();
                if (operation.getRequestBody().getContent() != null) {
                    operation.getRequestBody().getContent().forEach((k, v) -> {
                        if (v.getSchema() != null) {
                            if (v.getSchema().get$ref() != null) {
                                RequestBody rb = new RequestBody();
                                BodySchema bs = new BodySchema();
                                bs.setRef(v.getSchema().get$ref().substring("#/components/schemas/".length(),
                                        v.getSchema().get$ref().length()));
                                rb.setSchema(bs);
                                content.put(k, rb);
                            } else {
                                if (v.getSchema().getType() != null) {
                                    if (v.getSchema().getType().equals("string")) {
                                        RequestBody rb = new RequestBody();
                                        BodySchema bs = new BodySchema();
                                        bs.setFormat(v.getSchema().getFormat());
                                        bs.setType("string");
                                        rb.setSchema(bs);
                                        content.put(k, rb);
                                    }
                                    if (v.getSchema().getType().equals("array")) {
                                        ArraySchema arrSch = (ArraySchema) v.getSchema();
                                        RequestBody rb = new RequestBody();
                                        BodySchema bs = new BodySchema();
                                        bs.setFormat(v.getSchema().getFormat());
                                        bs.setType("array");
                                        if (arrSch.getItems() != null) {
                                            if (arrSch.getItems().get$ref() != null) {
                                                bs.setRef(arrSch.getItems().get$ref().substring(
                                                        "#/components/schemas/".length(),
                                                        arrSch.getItems().get$ref().length()));
                                            }

                                        }
                                        rb.setSchema(bs);
                                        content.put(k, rb);
                                    }
                                }

                            }
                        }
                    });
                }
                requestBody.setContent(content);
            }

            resource.setRequestBody(requestBody);

            // convert request Body to body parameter
            convertRequestBodyToBodyParam(requestBody, resource);

        }
        resource.setSecurityLevel(new ArrayList<>(Arrays.asList("public")));
        return resource;
    }

    private void convertRequestBodyToBodyParam(BodyMap requestBody, Resource resource) {
        ResourceParameter resourceParameter = new ResourceParameter();
        resourceParameter.setIn("Body");
        resourceParameter.setName("body");
        resourceParameter.setType("object");
        if (requestBody.getContent() != null) {
            if (!requestBody.getContent().values().stream().collect(Collectors.toList()).isEmpty()) {
                BodySchema reqBody = requestBody.getContent().values().stream().collect(Collectors.toList()).get(0)
                        .getSchema();
                if (reqBody.getRef() != null) {
                    resourceParameter.setModelName(reqBody.getRef());
                }
                if (reqBody.getType() != null && reqBody.getType().equals("string")) {
                    resourceParameter.setType("string");
                }

            }

            requestBody.getContent().forEach((k, v) -> {
                if (!resource.getConsumes().contains(k)) {
                    resource.getConsumes().add(k);
                }
            });
        }
        resourceParameter.setDescription(requestBody.getDescription());
        resourceParameter.setRequired(requestBody.getRequired());
        resource.getParameters().add(resourceParameter);
    }

    private boolean containsName(final List<EndpointModel> list, final String name) {
        return list.stream().anyMatch(o -> o.getTitle().equals(name));
    }

    private Object convertToObject(JsonNode value) {
        return value.deepCopy();
    }

    private boolean containsServers(final List<Server> servers, final String url) {
        return servers.stream().anyMatch(o -> o.getUrl().equals(url));
    }

    /**
     * Extract Fields From Operation Object in a Safe Mode
     *
     * @param operation
     * @param key
     * @return the Value or null
     */
    private String extractVendorField(Operation operation, String key) {
        Map<String, Object> vendorExtensions = operation.getExtensions();
        if (vendorExtensions != null) {
            Object value = vendorExtensions.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private List<String> extractVendorFieldFunction(Operation operation, String key, String projectId)
            throws JsonProcessingException {
        Map<String, Object> vendorExtensions = operation.getExtensions();
        List<String> functions = new ArrayList<String>();
        Function function = new Function();
        if (vendorExtensions != null) {
            Object value = vendorExtensions.get(key);
            ObjectMapper mapper = new ObjectMapper();
            if (value != null) {
                String json = mapper.writeValueAsString(value);
                JsonNode node = mapper.readValue(json, JsonNode.class);
                if (node.has("id")) {
                    AWSCredentials awsCredentials = new AWSCredentials();
                    if (node.has("awsCredentials") && node.has("awsAccessKeyId")) {
                        awsCredentials.setAwsAccessKeyId(node.get("awsCredentials").get("awsAccessKeyId").asText());
                        awsCredentials.setAwsSecretAccess(node.get("awsCredentials").get("awsSecretAccess").asText());
                        awsCredentials.setAwsSecretAccess(node.get("awsCredentials").get("region").asText());
                        function.setAwsCredentials(awsCredentials);
                        function.setAwsFunctionName(node.get("awsFunctionName").asText());
                    }
                    if (node.has("function")) {
                        function.setFunction(node.get("function").asText());
                    }
                    function.setLanguage(node.get("language").asText());
                    function.setProjectId(projectId);
                    function.setVersion(node.get("version").asText());
                    function.setName(node.get("name").asText());
                    if (functionRepository.findByProjectIdAndNameAndVersion(projectId, node.get("name").asText(),
                            node.get("version").asText()).isEmpty()) {
                        function = functionRepository.save(function);
                    }
                    functions.add(function.getId());
                }
            }
        }
        return functions;
    }
}
