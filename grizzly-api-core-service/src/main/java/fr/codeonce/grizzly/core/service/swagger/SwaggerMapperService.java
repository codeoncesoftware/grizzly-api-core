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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.codeonce.grizzly.common.runtime.AWSCredentials;
import fr.codeonce.grizzly.core.domain.container.Contact;
import fr.codeonce.grizzly.core.domain.container.License;
import fr.codeonce.grizzly.core.domain.container.*;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.endpointModel.*;
import fr.codeonce.grizzly.core.domain.enums.SecurityLevel;
import fr.codeonce.grizzly.core.domain.function.Function;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.resource.*;
import fr.codeonce.grizzly.core.service.swagger.utils.IDefinitionGenerator;
import fr.codeonce.grizzly.core.service.swagger.utils.ParameterFactory;
import fr.codeonce.grizzly.core.service.swagger.utils.SwaggerParserException;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SwaggerMapperService extends SwaggerMapperImpl {

    @Autowired
    Environment environment;

    @Autowired
    private IDefinitionGenerator definitionGenerator;

    @Autowired
    private InvalidSwaggerRepository invalidSwaggerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private DBSourceRepository dbRepository;

    private ParameterFactory factory = ParameterFactory.getInstance();

    private static final String XNAME = "x-name";
    private static final String AUTH_GRIZZLY = "Authentication Grizzly";

    private static final Logger log = LoggerFactory.getLogger(SwaggerMapperService.class);

    /**
     * Map a Container To a Swagger Object
     */
    public Swagger mapToSwagger(Container container, String type) {
        Project project = projectRepository.findById(container.getProjectId())
                .orElseThrow(GlobalExceptionUtil.notFoundException(Project.class, container.getProjectId()));

        Swagger swagger = super.mapToSwagger(container);
        swagger.getInfo().setDescription(container.getDescription());
        swagger.getInfo().setTitle(project.getName());
        swagger.getInfo().setVersion(container.getName());

        // setting contact
        io.swagger.models.Contact contact = new io.swagger.models.Contact();
        if (container.getContact() != null) {
            contact.setEmail(container.getContact().getEmail());
            contact.setName(container.getContact().getName());
            contact.setUrl(container.getContact().getUrl());
            swagger.getInfo().setContact(contact);

        }

        // setting license
        io.swagger.models.License license = new io.swagger.models.License();
        if (container.getLicense() != null) {
            license.setUrl(container.getLicense().getUrl());
            license.setName(container.getLicense().getName());
            swagger.getInfo().setLicense(license);
        }

        swagger.getInfo().setTermsOfService(container.getTermsOfService());
        swagger.setTags(container.getResourceGroups().stream().map(super::mapToTag).collect(Collectors.toList()));

        Map<String, Path> paths = fetchPathsFromContainer(container, type);
        if (paths.size() != 0) {
            swagger.setPaths(paths);
        } else {
            // Collections.emptyMap does not work in that case
            Map<String, Path> emPaths = new HashMap<>();
            Path p = new Path();
            emPaths.put("/", p);
            swagger.setPaths(emPaths);
        }

        swagger.setResponses(fetchResponsesFromContainer(container));

        swagger.setHost(container.getHost());
        swagger.setBasePath(container.getBasePath());
        swagger.setSchemes(convertStringSchemesToEnum(container.getSchemes()));

        // Set Definitions
        Map<String, Model> map = new HashMap<>();
        definitionGenerator.modelDefintion(container).forEach(model -> {
            map.put(model.getTitle(), model);
        });
        swagger.setDefinitions(map);


        // Set Security Definitions
        Map<String, SecuritySchemeDefinition> security = new HashMap<>();
        ApiKeyAuthDefinition apiKeyDef = new ApiKeyAuthDefinition().in(In.HEADER).name("Authorization");
        if (project.isSecurityEnabled()) {
            if (project.getAuthMSRuntimeURL() != null) {
                apiKeyDef.setDescription(
                        "The security of this microservice is delegated to this IAM microservice: " + project.getAuthMSRuntimeURL() +
                                "/\n\n Standard Authorization header using the Bearer scheme. \n\n Value example: \"Bearer {token}\"");
            } else if (project.getType().equals("authentication microservice")) {
                apiKeyDef.setDescription(
                        "IAM API: " + project.getRuntimeUrl() +
                                "/\n\n Standard Authorization header using the Bearer scheme. \n\n Value example: \"Bearer {token}\"");
            } else {
                apiKeyDef.setDescription(
                        "Standard Authorization header using the Bearer scheme. \n\n Value example: \"Bearer {token}\"");
            }
            security.put("token", apiKeyDef);
        }

        swagger.setSecurityDefinitions(security);
        return swagger;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Resource mapToResource(Operation operation) {
        List<APIResponse> responsesList = new ArrayList<>();
        Resource resource = super.mapToResource(operation);
        try {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {

                    APIResponse apiResponse = new APIResponse();
                    apiResponse.setCode(code);

                    apiResponse.setDescription(response.getDescription());
                    if (response.getHeaders() != null) {
                        Map<String, Header> headers = new HashMap<String, Header>();
                        response.getHeaders().forEach((title, value) -> {
                            Header header = new Header(value.getType(), value.getDescription(), value.getFormat());

                            headers.put(title, header);

                        });
                        Headers allHeaders = new Headers();
                        allHeaders.setHeaders(headers);
                        apiResponse.setHeaders(allHeaders);

                    }

                    if (response.getExamples() != null) {
                        Map<String, Object> exemple = new HashMap<String, Object>();
                        List<Object> values = new ArrayList<Object>();
                        response.getExamples().forEach((k, v) -> {
                            values.add(v);
                            exemple.put(k, v);
                            apiResponse.setExemples(exemple);
                        });
                    }

                    if (response.getSchema() != null) {
                        APISchema schema = new APISchema();
                        if (response.getSchema().getType().equals("ref")) {
                            schema = new APISchema(false, false, false);
                            schema.setRef(response.getResponseSchema().getReference().substring(
                                    "#/definitions/".length(), response.getResponseSchema().getReference().length()));
                            apiResponse.setSchema(schema);
                            responsesList.add(apiResponse);
                        } else if (response.getSchema().getType().equals("array")) {
                            ArrayModel arrayModel = (ArrayModel) response.getResponseSchema();
                            schema = new APISchema(true, false, false);

                            if (!arrayModel.getItems().getClass().equals(StringProperty.class)) {
                                schema.setRef(((RefProperty) arrayModel.getItems()).get$ref().substring(
                                        "#/definitions/".length(),
                                        ((RefProperty) arrayModel.getItems()).get$ref().length()));
                            } else {
                                schema = new APISchema(false, false, true);
                            }

                            apiResponse.setSchema(schema);
                            responsesList.add(apiResponse);
                        } else if (response.getSchema().getClass().equals(MapProperty.class)) {
                            MapProperty mp = (MapProperty) response.getSchema();
                            AdditionalProperties ap = new AdditionalProperties(mp.getAdditionalProperties().getType(),
                                    mp.getAdditionalProperties().getFormat());
                            schema = new APISchema(false, true, false);
                            schema.setAdditionalProperties(ap);
                            apiResponse.setSchema(schema);
                            responsesList.add(apiResponse);
                        } else if (response.getSchema().getClass().equals(StringProperty.class)) {
                            schema = new APISchema(false, false, true);
                            apiResponse.setSchema(schema);
                            responsesList.add(apiResponse);

                        }

                    } else {
                        responsesList.add(apiResponse);
                    }
                });
            }
        } catch (Exception e) {
            log.debug("an error has occured {}", e);
        }

        resource.setResponses(responsesList);
        convertSwaggerResponseToOpenApiResponse(responsesList, resource);
        return resource;
    }

    private void convertSwaggerResponseToOpenApiResponse(List<APIResponse> apiResponses, Resource resource) {
        List<OpenAPIResponse> openApiResponses = new ArrayList<OpenAPIResponse>();
        apiResponses.forEach(apiResponse -> {
            OpenAPIResponse openAPIResponse = new OpenAPIResponse();
            openAPIResponse.setCode(apiResponse.getCode());
            openAPIResponse.setDescription(apiResponse.getDescription());
            if (apiResponse.getExemples() != null) {
                Map<String, ExampleObject> exemples = new HashMap<String, ExampleObject>();
                apiResponse.getExemples().forEach((k, v) -> {
                    ExampleObject ea = new ExampleObject();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        ea.setValue(mapper.writeValueAsString(v));
                    } catch (JsonProcessingException e) {
                        log.error("error in json parsing", e);
                    }
                    exemples.put(k, ea);
                });
                openAPIResponse.setExemples(exemples);
            }
            if (apiResponse.getHeaders() != null) {
                openAPIResponse.setHeaders(apiResponse.getHeaders());
            }

            if (apiResponse.getSchema() != null) {
                Map<String, ContentEelement> content = new HashMap<String, ContentEelement>();
                if (!resource.getProduces().isEmpty()) {

                    resource.getProduces().forEach(produce -> {
                        ContentEelement contentElement = new ContentEelement();
                        contentElement.setSchema(apiResponse.getSchema());
                        content.put(produce, contentElement);
                    });

                } else {

                    ContentEelement contentElement = new ContentEelement();
                    contentElement.setSchema(apiResponse.getSchema());
                    content.put("application/json", contentElement);
                }
                openAPIResponse.setContent(content);
            }
            openApiResponses.add(openAPIResponse);
        });
        resource.setOpenAPIResponses(openApiResponses);
    }

    private void convertSwaggerParamsToOpenApiRequestBody(Resource resource) {
        BodyMap bodyMap = new BodyMap();
        resource.getParameters().stream().filter(el -> el.getIn().equalsIgnoreCase("body")).collect(Collectors.toList())
                .forEach(param -> {
                    BodySchema bodySchema = new BodySchema();
                    bodySchema.setRef(param.getModelName());
                    bodySchema.setDescription(param.getDescription());
                    Map<String, RequestBody> content = new HashMap<String, RequestBody>();
                    if (!resource.getConsumes().isEmpty()) {
                        resource.getConsumes().forEach(consume -> {
                            RequestBody requestBody = new RequestBody();
                            requestBody.setSchema(bodySchema);
                            requestBody.setRequired(param.getRequired());
                            content.put(consume, requestBody);
                        });
                    } else {
                        RequestBody requestBody = new RequestBody();
                        requestBody.setSchema(bodySchema);
                        requestBody.setRequired(param.getRequired());
                        content.put("application/json", requestBody);
                    }
                    bodyMap.setContent(content);
                });
        resource.setRequestBody(bodyMap);
    }

    /**
     * Add the Responses List to the Operation Object after Mapping
     */
    @SuppressWarnings("deprecation")
    @Override
    public Operation mapToOperation(Resource resource) {
        Operation operation = new Operation();
        Map<String, Response> responsesList = new HashMap<>();
        resource.getResponses().forEach(response -> {
            RefModel model = new RefModel();
            if (!response.getSchema().getRef().equals(null)) {
                model.set$ref("#/definitions/" + response.getSchema().getRef());
            }
            Response responseToReturn = new Response();
            responseToReturn.setResponseSchema(model);
            responseToReturn.setDescription(response.getDescription());

            Map<String, Property> map = new HashMap<String, Property>();
            response.getHeaders().getHeaders().forEach((c, v) -> {
                Property p = PropertyBuilder.build(v.getType(), null, null);
                p.setName(v.getName());
                p.setDescription(v.getDescription());
                map.put(c, p);
            });
            responseToReturn.setHeaders(map);
            responsesList.put(response.getCode(), responseToReturn);
        });
        operation.setResponses(responsesList);
        return operation;
    }

    private Map<String, Response> fetchResponsesFromContainer(Container container) {
        Map<String, Response> responses = new HashMap<>();
        container.getResources().forEach(resource -> {
            Operation operation = super.mapToOperation(resource);
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((k, v) -> responses.put(k, v));
            }
        });
        return responses;
    }

    /**
     * Fetch Swagger Paths from a Container
     *
     * @param container
     * @return
     */
    private Map<String, Path> fetchPathsFromContainer(Container container, String type) {

        Map<String, Path> paths = new HashMap<String, Path>();
        container.getResources().forEach(resource -> {
            Path path = new Path();
            Operation operation = setOperationFields(resource, type);
            if (operation.getResponses() == null || operation.getResponses().size() == 0) {
                Response res = new Response();
                res.description("OK");
                operation.setResponses(Collections.singletonMap("200", res));
            }
            if (ObjectUtils.isEmpty(paths.get(resource.getPath()))) {
                path.set(resource.getHttpMethod().toLowerCase(), operation);
                paths.put(resource.getPath(), path);
            } else {
                paths.get(resource.getPath()).set(resource.getHttpMethod().toLowerCase(), operation);
            }
        });
        return paths;
    }

    /**
     * Set Operation Object Fields from the Given Resource Object
     *
     * @param resource
     * @return
     */
    private Operation setOperationFields(Resource resource, String type) {

        Operation operation = super.mapToOperation(resource);
        // operation.
        operation.setTags(Collections.singletonList(resource.getResourceGroup()));
        /*
         * Calling Factory to Get the suitable Parameter depending on parameter In field
         */
        String httpMethod = resource.getHttpMethod();
        operation.setParameters(
                resource.getParameters().stream().map(factory::makeParameter).collect(Collectors.toList()));
        if (resource.getCustomQuery() != null
                && (resource.getExecutionType().equalsIgnoreCase("query")
                && StringUtils.equalsAnyIgnoreCase(httpMethod, "post", "put"))
                && (resource.getCustomQuery().getType() != null
                && StringUtils.equalsAnyIgnoreCase(resource.getCustomQuery().getType(), "insert", "update"))
                && !containsBodyParam(operation.getParameters(), "Body")
                && resource.getParameters().stream().noneMatch(param -> param.getType().equalsIgnoreCase("file"))) {

            // Attach Sign-In and Sign-Up models to AUTH APIs
            if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/signup")) {
                operation.getParameters().add(factory.getModelDef("signup"));
            } else if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/signin")) {
                operation.getParameters().add(factory.getModelDef("signin"));
            } else if (resource.getResourceGroup().equals("Grizzly-Demo-Pet") && resource.getPath().equals("/add")
                    || resource.getPath().equals("/update")) {
                // operation.getParameters().add(factory.getModelDef("pet"));

            } else if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/grant")) {
                operation.getParameters().add(factory.getModelDef("roles"));
            } else if (resource.getResourceGroup().equals(AUTH_GRIZZLY) && resource.getPath().equals("/updateuser")) {
                operation.getParameters().add(factory.getModelDef("authUser"));
            }

        }
        // Setting Responses
        Map<String, Response> responses = new HashMap<>();
        resource.getResponses().forEach(resp -> {
            Response response = new Response();
            response.setDescription(resp.getDescription());
            if (resp.getExemples() != null) {
                Map<String, Object> exemples = new HashMap<String, Object>();
                resp.getExemples().forEach((k, v) -> {
                    exemples.put(k, v);
                });

                response.setExamples(exemples);
            }
            if (resp.getHeaders() != null) {
                resp.getHeaders().getHeaders().forEach((c, v) -> {
                    Property prop = PropertyBuilder.build(v.getType(), v.getFormat(), null);
                    prop.setDescription(v.getDescription());
                    prop.setName(v.getName());
                    response.addHeader(c, prop);
                });
            }
            if (resp.getSchema() != null && resp.getSchema().getArray()) {
                if (resp.getSchema().getRef() != null && !resp.getSchema().getRef().equals("")) {
                    RefProperty mo = new RefProperty("#/definitions/" + resp.getSchema().getRef());
                    ArrayModel arrModel = new ArrayModel();
                    arrModel.setItems(mo);
                    response.setResponseSchema(arrModel);
                } else {
                    if (resp.getSchema().getRef() != null && !resp.getSchema().getRef().equals("")) {
                        RefModel model = new RefModel();
                        model.set$ref("#/definitions/" + resp.getSchema().getRef());
                        response.setResponseSchema(model);
                    }
                }
                if (resp.getSchema().getObject() != null && resp.getSchema().getObject()) {
                    ModelImpl model = new ModelImpl();
                    if (resp.getSchema().getAdditionalProperties() != null) {
                        model.setAdditionalProperties(
                                PropertyBuilder.build(resp.getSchema().getAdditionalProperties().getType(),
                                        resp.getSchema().getAdditionalProperties().getFormat(), null));
                    }
                    model.setType("object");
                    response.setResponseSchema(model);
                }
                if (resp.getSchema().getString() != null && resp.getSchema().getString()) {
                    ModelImpl model = new ModelImpl();
                    model.setType("string");
                    response.setResponseSchema(model);

                }
            }

            responses.put(resp.getCode(), response);
        });
        operation.setResponses(responses);

        // Set Security Attribute
        if (!resource.getSecurityLevel().isEmpty() && !resource.getSecurityLevel().get(0).equals("public")) {
            Map<String, List<String>> map = new HashMap<>();
            map.put("token", new ArrayList<String>());
            List<Map<String, List<String>>> list = new ArrayList<>();
            list.add(map);
            operation.setSecurity(list);
        } else {
            operation.setSecurity(null);
        }
        // Add Custom Fields If Swagger Is For Dev Mode
        if (type.equalsIgnoreCase("dev")) {
            if (resource.getName() != null) {
                operation.setVendorExtension(XNAME, resource.getName());
            }
            operation.setVendorExtension("x-executionType", resource.getExecutionType());
            /* Fetch associated fileID from files collection */
            if (resource.getResourceFile() != null && resource.getResourceFile().getFileId() != null
                    && !resource.getResourceFile().getFileId().isEmpty()) {

                /* Add Custom fields to Swagger for both ContainerId and FileUri */
                operation.getVendorExtensions().put("x-fileId", resource.getResourceFile().getFileId());
                operation.getVendorExtensions().put("x-fileUri", resource.getResourceFile().getFileUri());
            }
            if (resource.getExecutionType() != null && resource.getExecutionType().equals("Query")) {
                operation.getVendorExtensions().put("x-type", resource.getCustomQuery().getType());
                operation.getVendorExtensions().put("x-collectionName", resource.getCustomQuery().getCollectionName());
                operation.getVendorExtensions().put("x-queryType", resource.getCustomQuery().getQueryName());
                operation.getVendorExtensions().put("x-query", resource.getCustomQuery().getQuery());
                operation.getVendorExtensions().put("x-query-many", resource.getCustomQuery().isMany());
                operation.getVendorExtensions().put("x-query-request-many", resource.getCustomQuery().isRequestMany());
                if (!resource.getOutFunctions().isEmpty() && functionRepository.findById(resource.getOutFunctions().get(0)).isPresent()) {
                    operation.getVendorExtensions().put("x-out-functions", functionRepository.findById(resource.getOutFunctions().get(0)).get());
                }
                if (!resource.getFunctions().isEmpty() && functionRepository.findById(resource.getFunctions().get(0)).isPresent()) {
                    operation.getVendorExtensions().put("x-functions", functionRepository.findById(resource.getFunctions().get(0)).get());
                }
                if (!resource.getInFunctions().isEmpty() && functionRepository.findById(resource.getInFunctions().get(0)).isPresent()) {
                    operation.getVendorExtensions().put("x-in-functions", functionRepository.findById(resource.getInFunctions().get(0)).get());
                }
                // Set Projection Fields
                if (resource.getFields() != null) {
                    StringBuilder fields = new StringBuilder();
                    resource.getFields().stream().forEach(field -> fields.append(field + ','));
                    operation.getVendorExtensions().put("x-fields", fields.toString());
                }
            }
        }

        return operation;
    }

    /**
     * Check if an Operation Contains Parameter with the same given Name
     *
     * @param list
     * @param name
     * @return Found or Not
     */
    private boolean containsBodyParam(final List<Parameter> list, final String name) {
        return list.stream().map(Parameter::getName).filter(name::equalsIgnoreCase).findFirst().isPresent();
    }

    public Container mapToContainer(Swagger swagger, String projectId, String content, String editor, boolean docker)
            throws Exception {
        Container container = mapToContainer(swagger, content);
        this.projectRepository.findById(projectId).ifPresent(project -> {
            if (editor.equals("true") || docker == true) {
                project.setName(swagger.getInfo().getTitle().replaceAll("[^a-zA-Z0-9]", "_"));
            }
            if (swagger.getHost() != null && (swagger.getHost().equals("dwk5c1lsnem8l.cloudfront.net")
                    || swagger.getHost().equals("localhost:4900") || swagger.getHost().equals("grizzly-api.com"))) {
                if (swagger.getSecurityDefinitions() == null) {
                    project.setSecurityEnabled(false);
                } else {
                    project.setSecurityEnabled(true);
                    if (swagger.getSecurityDefinitions().get("token") != null) {
                        String description = swagger.getSecurityDefinitions().get("token").getDescription();
                        if (description.contains("The security of this microservice is delegated to this IAM microservice:")) {
                            String authmsRuntimeUrl = description.substring(
                                    StringUtils.ordinalIndexOf(description, ":", 1) + 2,
                                    StringUtils.ordinalIndexOf(description, "/", 5));
                            project.setAuthMSRuntimeURL(authmsRuntimeUrl);
                        } else if (description.contains("IAM API:")) {
                            project.setAuthMSRuntimeURL(null);
                            project.setType("authentication microservice");
                        } else {
                            project.setAuthMSRuntimeURL(null);
                            project.setType("microservice");
                            project.getSecurityConfig().setClientId(project.getName());
                            project.getSecurityConfig()
                                    .setSecretKey(DigestUtils.sha256Hex(project.getName()) + DigestUtils.sha256Hex("co%de01/"));
                            if (project.getRoles().isEmpty()) {
                                project.getRoles().add(SecurityLevel.ADMIN.getValue());
                                project.getRoles().add("user");
                            }
                        }

                    }
                }
            }

            this.projectRepository.save(project);

            container.setProjectId(projectId);
            if (project.getDbsourceId() != null) {
                this.dbRepository.findById(project.getDbsourceId()).ifPresent(db -> {
                    String databaseName;
                    if (db.getConnectionMode().equalsIgnoreCase("FREE")) {
                        databaseName = db.getPhysicalDatabase();
                    } else {
                        databaseName = project.getDatabaseName();
                    }
                    fetchContainerResources(swagger, container, project.getDbsourceId(), databaseName, projectId);
                });
            } else {
                // fetch resources without database
                fetchContainerResources(swagger, container, project.getDbsourceId(), "", projectId);
            }

        });

        return container;
    }

    @Override
    public Container mapToContainer(Swagger swagger, String content) throws Exception {
        try {
            Container container = super.mapToContainer(swagger, content);
            List<ResourceGroup> lrg = new ArrayList<>();
            if (swagger.getPaths() != null) {
                swagger.getPaths().forEach((k, v) -> {
                    v.getOperations().forEach(operation -> {
                        if (operation.getTags() != null) {
                            operation.getTags().forEach(tag -> {
                                Boolean result = false;
                                for (int i = 0; i < lrg.size(); i++) {
                                    if (lrg.get(i).getName().equals(tag)) {
                                        result = true;
                                        break;
                                    }
                                }
                                if (result == false) {
                                    ResourceGroup rg = new ResourceGroup();
                                    rg.setName(tag);
                                    rg.setDescription("");
                                    if (!lrg.stream().map(el -> el.getName()).anyMatch(el -> el.equals(rg.getName())))
                                        lrg.add(rg);
                                }
                            });
                        }

                    });
                });
            }

            if (lrg.size() == 0) {
                ResourceGroup rg = new ResourceGroup();
                rg.setName("Default");
                rg.setDescription("");

                if (!lrg.stream().map(el -> el.getName()).anyMatch(el -> el.equals(rg.getName())))
                    lrg.add(rg);
            }

            if (swagger.getTags() != null) {
                swagger.getTags().stream().map(x -> {
                    ResourceGroup rg = new ResourceGroup();
                    rg.setName(x.getName());
                    rg.setDescription(x.getDescription());
                    if (!lrg.stream().map(el -> el.getName()).anyMatch(el -> el.equals(rg.getName())))
                        lrg.add(rg);
                    return x;
                }).collect(Collectors.toList());
            }

            container.setResourceGroups(lrg);
            if (swagger.getInfo().getDescription() != null) {
                container.setDescription(swagger.getInfo().getDescription());
            }
            if (swagger.getInfo().getContact() != null) {
                Contact contact = new Contact(swagger.getInfo().getContact().getName(),
                        swagger.getInfo().getContact().getUrl(), swagger.getInfo().getContact().getEmail());
                container.setContact(contact);
            }

            if (swagger.getInfo().getLicense() != null) {
                License license = new License(swagger.getInfo().getLicense().getName(),
                        swagger.getInfo().getLicense().getUrl());
                container.setLicense(license);
            }

            // Delete Existing "Authentication Grizzly" Resource Group
            container.setResourceGroups(container.getResourceGroups().stream()
                    .filter(rg -> !rg.getName().equalsIgnoreCase("Authentication Grizzly"))
                    .collect(Collectors.toList()));

            return container;
        } catch (Exception e) {
            InvalidSwagger invalidSwagger = new InvalidSwagger();
            invalidSwagger.setContent(content);
            invalidSwaggerRepository.save(invalidSwagger);
            throw new SwaggerParserException(
                    "Swagger format is invalid for more details contact us on support@codeonce.fr");
        }

    }

    public void fetchContainerResources(Swagger swagger, Container container, String dbSourceID, String dbName,
                                        String projectId) {
        List<Resource> resources = new ArrayList<>();
        if (swagger.getPaths() != null) {
            swagger.getPaths().forEach((pathName, pathDetails) -> {

                if (pathDetails.getOperationMap() != null) {
                    pathDetails.getOperationMap().forEach((httpMethod, operation) -> {
                        if (operation.getTags() != null) {
                            if (!operation.getTags().contains("Authentication Grizzly")) {
                                Resource resource = setResourceFields(operation, pathName, httpMethod, dbSourceID,
                                        dbName, projectId);
                                if (pathDetails.getParameters() != null) {
                                    resource.setParameters(pathDetails.getParameters().stream()
                                            .map(factory::makeResourceParameter).collect(Collectors.toList()));
                                }
                                convertSwaggerParamsToOpenApiRequestBody(resource);
                                convertSwaggerResponseToOpenApiResponse(resource.getResponses(), resource);
                                resources.add(resource);
                            }
                        } else {

                            Resource resource = setResourceFields(operation, pathName, httpMethod, dbSourceID, dbName,
                                    projectId);
                            if (pathDetails.getParameters() != null) {
                                resource.setParameters(pathDetails.getParameters().stream()
                                        .map(factory::makeResourceParameter).collect(Collectors.toList()));
                            }
                            convertSwaggerParamsToOpenApiRequestBody(resource);
                            convertSwaggerResponseToOpenApiResponse(resource.getResponses(), resource);
                            resources.add(resource);
                        }

                    });
                }
            });
        }
        List<ResourceParameter> parameters = new ArrayList<ResourceParameter>();
        if (swagger.getParameters() != null) {
            swagger.getParameters().forEach((title, value) -> {
                ParameterFactory pf = new ParameterFactory();
                parameters.add(pf.makeResourceParameter(value));

            });
        }

        List<EndpointModel> models = new ArrayList<EndpointModel>();
        if (swagger.getDefinitions() != null) {
            swagger.getDefinitions().forEach((title, value) -> {
                EndpointModel model = new EndpointModel();

                model.setTitle(title);

                if (value.getDescription() != null) {
                    model.setDescription(value.getDescription().trim());
                }
                if (value.getClass().equals(ModelImpl.class) && ((ModelImpl) value).getFormat() != null) {
                    model.setFormat(((ModelImpl) value).getFormat());

                }
                if (value.getExample() != null) {
                    model.setExample(value.getExample());
                }
                if (!value.getClass().equals(ComposedModel.class) && !value.getClass().equals(ArrayModel.class)
                        && ((ModelImpl) value).getEnum() != null) {
                    model.setEnums(((ModelImpl) value).getEnum());
                }

                if (value.getClass().equals(ModelImpl.class)) {
                    ModelImpl modelImpl = (ModelImpl) value;
                    if (modelImpl.getEnum() != null) {
                        model.setEnums(modelImpl.getEnum());
                    }
                    if (modelImpl.getType() != null) {
                        model.setType(modelImpl.getType());
                    }
                    if (modelImpl.getRequired() != null) {
                        model.setRequired(modelImpl.getRequired());
                    }
                }

                ArrayList<ModelProperty> props = new ArrayList<ModelProperty>();
                if (value.getClass().equals(ComposedModel.class)) {
                    ComposedModel cm = (ComposedModel) value;
                    manageComposedModels(swagger, cm, props, models);

                }
                if (fetchProperties(value, models).size() == 0 && value.getClass().equals(ArrayModel.class)) {
                    ArrayModel arrayModel = (ArrayModel) value;
                    model.setArray(true);
                    if (value.getDescription() != null) {
                        model.setDescription(value.getDescription().trim());
                    }

                    if (arrayModel.getItems().getClass().equals(RefProperty.class)) {
                        RefProperty refProperty = (RefProperty) arrayModel.getItems();
                        model.setRef(refProperty.get$ref().split("/")[2]);

                    } else if (arrayModel.getItems().getClass().equals(BooleanProperty.class)) {
                        model.setType("boolean");

                    } else if (arrayModel.getItems().getClass().equals(StringProperty.class)) {
                        model.setType("string");
                    }

                }

                List<ModelProperty> newList = Stream.concat(props.stream(), fetchProperties(value, models).stream())
                        .collect(Collectors.toList());
                model.setProperties(newList);
                models.add(model);
            });
        }

        container.setEndpointModels(models);
        container.setParameters(parameters);
        container.setResources(resources);
        container.setDescription(swagger.getInfo().getDescription());

    }

    private void manageComposedModels(Swagger swagger, ComposedModel cm, ArrayList<ModelProperty> props,
                                      List<EndpointModel> models) {
        cm.getAllOf().forEach(mod -> {
            if (mod.getClass().equals(ModelImpl.class)) {
                ModelImpl modelImpl = (ModelImpl) mod;
                fetchProperties(modelImpl, models).forEach(modelProperty -> {
                    props.add(modelProperty);
                });

            }
            if (mod.getClass().equals(RefModel.class)) {
                RefModel refModel = (RefModel) mod;
                String refName = refModel.get$ref().split("/")[2];
                if (swagger.getDefinitions().get(refName).getClass().equals(ComposedModel.class)) {
                    ComposedModel composedModel = (ComposedModel) swagger.getDefinitions().get(refName);
                    manageComposedModels(swagger, composedModel, props, models);
                } else {
                    fetchProperties(swagger.getDefinitions().get(refName), models).forEach(modelProperty -> {
                        props.add(modelProperty);
                    });
                }

            }

        });
    }

    private List<ModelProperty> fetchProperties(Model value, List<EndpointModel> models) {
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        if (value.getProperties() != null) {
            value.getProperties().forEach((k, v) -> {
                ModelProperty prop = new ModelProperty();

                if (v.getExample() != null) {
                    prop.setExample(v.getExample().toString());
                }
                if (v.getDescription() != null) {
                    prop.setDescription(v.getDescription().trim());
                }
                if (v.getClass().equals(StringProperty.class)) {
                    StringProperty stringProperty = (StringProperty) v;
                    prop.setEnums(stringProperty.getEnum());
                    prop.setName(k);
                    prop.setType("string");
                } else if (v.getClass().equals(ArrayProperty.class)) {
                    ArrayProperty arrayProperty = (ArrayProperty) v;

                    Property p = arrayProperty.getItems();
                    prop.setName(k);
                    prop.setArray(true);
                    if (arrayProperty.getItems().getClass().equals(RefProperty.class)) {
                        RefProperty refProperty = (RefProperty) arrayProperty.getItems();
                        prop.setType(refProperty.get$ref().split("/")[2]);
                        prop.setRef(refProperty.get$ref().split("/")[2]);
                    } else {
                        prop.setType(arrayProperty.getItems().getType());
                        prop.setRef("");
                    }
                } else if (v.getClass().equals(ObjectProperty.class)) {
                    ObjectProperty objectProperty = (ObjectProperty) v;

                    prop.setRef(k);
                    prop.setName(k);
                    prop.setArray(false);
                    prop.setType("object");
                    EndpointModel em = new EndpointModel();
                    List<ModelProperty> mp = new ArrayList<ModelProperty>();
                    objectProperty.getProperties().forEach((ke, va) -> {
                        ModelProperty p = new ModelProperty();
                        p.setName(ke);
                        p.setType(va.getType());
                        p.setFormat(va.getFormat());
                        mp.add(p);

                    });
                    em.setTitle(k);
                    em.setProperties(mp);
                    models.add(em);
                } else {
                    if (v.getClass().equals(RefProperty.class)) {
                        prop.setName(k);
                        RefProperty refProperty = (RefProperty) v;
                        prop.setType("object");
                        prop.setRef(refProperty.get$ref().split("/")[2]);
                        prop.setArray(false);
                    } else {
                        prop.setName(k);
                        prop.setType(v.getType());
                        prop.setArray(false);
                        prop.setRef("");
                    }
                }
                properties.add(prop);
            });

        }
        return properties;
    }

    /**
     * Set Resource Object Fields From the Given Operation Object
     *
     * @param operation
     * @param pathName
     * @param httpMethod
     * @return
     */
    private Resource setResourceFields(Operation operation, String pathName, HttpMethod httpMethod, String dbSourceID,
                                       String dbName, String projectId) {
        Resource resource = mapToResource(operation);
        resource.setPath(pathName);
        resource.setHttpMethod(httpMethod.toString());

        if (operation.getTags() != null) {
            resource.setResourceGroup(operation.getTags().get(0));
        } else {

            resource.setResourceGroup("Default");
        }
        resource.setName(extractVendorField(operation, XNAME));

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
        if (operation.getParameters() != null) {
            resource.setParameters(operation.getParameters().stream().map(factory::makeResourceParameter)
                    .collect(Collectors.toList()));
        }

        resource.setSecurityLevel(new ArrayList<String>(Arrays.asList("public")));
        return resource;
    }

    /**
     * Extract Fields From Operation Object in a Safe Mode
     *
     * @param operation
     * @param key
     * @return the Value or null
     */
    private String extractVendorField(Operation operation, String key) {
        Map<String, Object> vendorExtensions = operation.getVendorExtensions();
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
        Map<String, Object> vendorExtensions = operation.getVendorExtensions();
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

    private List<Scheme> convertStringSchemesToEnum(List<String> schemes) {
        List<Scheme> enumSchemes = new ArrayList<Scheme>();
        if (schemes != null) {
            schemes.forEach(scheme -> {
                if (scheme.equalsIgnoreCase("http")) {
                    enumSchemes.add(Scheme.HTTP);
                }
                if (scheme.equalsIgnoreCase("https")) {
                    enumSchemes.add(Scheme.HTTPS);
                }
                if (scheme.equalsIgnoreCase("ws")) {
                    enumSchemes.add(Scheme.WS);
                }
                if (scheme.equalsIgnoreCase("wss")) {
                    enumSchemes.add(Scheme.WSS);
                }
            });
        }

        return enumSchemes;
    }

}