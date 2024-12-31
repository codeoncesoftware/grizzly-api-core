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

import fr.codeonce.grizzly.common.runtime.resource.CreateResourceRequest;
import fr.codeonce.grizzly.common.runtime.resource.RuntimeResource;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.endpointModel.EndpointModel;
import fr.codeonce.grizzly.core.domain.endpointModel.ModelProperty;
import fr.codeonce.grizzly.core.domain.resource.*;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.resource.utils.ResourceRuntimeMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceQueryService {
    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ResourceRuntimeMapper resourceRuntimeMapper;
    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;


    private List<String> keyWords = new ArrayList<>(
            List.of("findBy", "And", "Or", "Not", "Is", "Equals", "Between", "LessThan", "LessThanEqual", "GreaterThan",
                    "GreaterThanEqual", "After", "Before", "IsNull", "IsNotNull", "NotIn", "In"));

    public List<String> getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(List<String> keyWords) {
        this.keyWords = keyWords;
    }

    private static final Logger log = LoggerFactory.getLogger(ResourceQueryService.class);

    /**
     * Query create resource request
     *
     * @param containerId
     * @param CreateResourceRequest
     * @return RuntimeResource
     * @throws Exception
     */
    public Boolean isCollection(String collectionName) throws Exception {
        if (keyWords.contains(collectionName))
            throw new Exception(collectionName + " is a reserved try a collection name");
        else
            return true;
    }

    public String queryType(CreateResourceRequest createResourceRequest) throws Exception {
        String type = "";
        List<String> paths = new ArrayList<String>(Arrays.asList(createResourceRequest.getPath().split("/")));
        if (paths.size() == 0 || paths.size() == 1)
            throw new Exception("Invalid Query");
        else {
            if (paths.size() == 2 && isCollection(paths.get(1)))
                type = "findAll";
            else if (paths.size() > 2) {
                if (paths.get(2).length() >= "find".length()) {
                    if (paths.get(2).toLowerCase().substring(0, "find".length()).equals("find")) {
                        if (paths.get(2).length() > 5
                                && paths.get(2).substring(0, "findBy".length()).equals("findBy")) {
                            keyWords.remove("findBy");
                            log.info("find {}", notContains(
                                    paths.get(2).substring("findBy".length(), paths.get(2).length()), keyWords));
                            if (!keyWords.contains(paths.get(2).substring("findBy".length(), paths.get(2).length()))
                                    && notContains(paths.get(2).substring("findBy".length(), paths.get(2).length()),
                                    keyWords)) {
                                type = "findByWithOneParam";
                            } else if (paths.get(2).substring("findBy".length(), paths.get(2).length()).toLowerCase()
                                    .contains("and")
                                    || paths.get(2).substring("findBy".length(), paths.get(2).length()).toLowerCase()
                                    .contains("or")) {
                                type = "findByWithTwoParams";
                            } else {
                                type = "findByWithComparaison";
                            }
                        }
                    }
                } else {
                    type = "pathQuery";
                }

            } else
                throw new Exception("unknownen query 2");

        }
        return type;
    }

    private ResourceParameter createResourceParameter(CreateResourceRequest createResourceRequest, String parametre,
                                                      String type, String in) {
        ResourceParameter resourceParameter = new ResourceParameter();
        resourceParameter.setIn(in);
        resourceParameter.setName(parametre);
        resourceParameter.setType(type);
        return resourceParameter;
    }

    private CustomQuery createCustomQuery(String containerId, CreateResourceRequest createResourceRequest,
                                          String query) {
        String parsedBody = createResourceRequest.getParsedBody();
        String collectionName = createResourceRequest.getPath().split("/")[1];
        Container container = containerRepository.findById(containerId).get();
        String idDb = container.getDbsourceId();
        Optional<DBSource> dataBase = dbSourceRepository.findById(idDb);
        DBSource dbToSave = dataBase.get();
        CustomQuery runtimeCustomQuery = new CustomQuery();
        runtimeCustomQuery.setDatasource(idDb);
        runtimeCustomQuery.setDatabase(dbToSave.getName());
        runtimeCustomQuery.setCollectionName(collectionName);
        runtimeCustomQuery.setQuery(query);
        if (createResourceRequest.getHttpMethod().equalsIgnoreCase("put")) {
            runtimeCustomQuery.setType("Update");
            runtimeCustomQuery.setMany(false);
        } else if (createResourceRequest.getHttpMethod().equalsIgnoreCase("post")) {
            runtimeCustomQuery.setType("Insert");
            runtimeCustomQuery.setMany(false);
        } else {
            runtimeCustomQuery.setType("Insert");
            runtimeCustomQuery.setMany(true);
        }

        return runtimeCustomQuery;
    }

    public Resource createQueryAndResourceParameter(String containerId, CreateResourceRequest createResourceRequest,
                                                    Resource resource) throws Exception {
        String query = "";
        String fullRequestName = createResourceRequest.getPath().split("/")[2];
        List<ResourceParameter> parameters = new ArrayList<>();
        log.info("queryType {}", queryType(createResourceRequest));
        if (queryType(createResourceRequest).equals("pathQuery")) {
            String firstHeader = createResourceRequest.getRequestparam().get(0);
            if (readQueryPath(firstHeader).equals("one")) {
                query = "{attribute1:path1}";
                parameters.add(createResourceParameter(createResourceRequest, "path1", "String", "path"));

            } else if (readQueryPath(firstHeader).equals("and") || readQueryPath(firstHeader).equals("or")) {
                query = "{'$operation':[{'attribute1':'path1'},{'attribute2':'path2'}]}";
                parameters.add(createResourceParameter(createResourceRequest, "path1", "String", "path"));
                parameters.add(createResourceParameter(createResourceRequest, "path2", "String", "path"));
            }
            resource.setCustomQuery(createCustomQuery(containerId, createResourceRequest, query));
            resource.setParameters(parameters);
            return resource;

        }
        if (queryType(createResourceRequest).equals("findByWithOneParam")) {
            String firstParam = createResourceRequest.getRequestparam().get(0);
            query = "{\"" + firstParam + "\":\"%" + firstParam + "\"}";
            parameters.add(createResourceParameter(createResourceRequest, firstParam, "String", "Query"));
        } else {
            if (queryType(createResourceRequest).equals("findByWithTwoParams")) {
                if (createResourceRequest.getRequestparam().get(1) != null) {
                    String firstParam = createResourceRequest.getRequestparam().get(0);
                    String secondParam = createResourceRequest.getRequestparam().get(1);
                    if (fullRequestName.contains("And")) {
                        query = "{\"$and\":[" + "{\"" + firstParam + "\":\"%" + firstParam + "\"}" + "," + "{\""
                                + secondParam + "\":\"%" + secondParam + "\"}" + "]}";
                        parameters.add(createResourceParameter(createResourceRequest, firstParam, "String", "Query"));
                        parameters.add(createResourceParameter(createResourceRequest, secondParam, "String", "Query"));

                    } else if (fullRequestName.contains("Or")) {
                        query = "{\"$or\":[" + "{\"" + firstParam + "\":\"%" + firstParam + "\"}" + "," + "{\""
                                + secondParam + "\":\"%" + secondParam + "\"}" + "]}";
                        parameters.add(createResourceParameter(createResourceRequest, firstParam, "String", "Query"));
                        parameters.add(createResourceParameter(createResourceRequest, secondParam, "String", "Query"));
                    }
                }
            } else if (queryType(createResourceRequest).equals("findByWithComparaison")) {
                String firstParam = createResourceRequest.getRequestparam().get(0);
                if (fullRequestName.contains("LessThan")) {
                    query = "{\"" + firstParam + "\":{\"$lt\":\"%" + firstParam + "\"}}";
                    parameters.add(createResourceParameter(createResourceRequest, firstParam, "Int32", "Query"));

                } else if (fullRequestName.contains("GreaterThan")) {
                    query = "{\"" + firstParam + "\":{\"$gt\":\"%" + firstParam + "\"}}";
                    parameters.add(createResourceParameter(createResourceRequest, firstParam, "Int32", "Query"));
                } else if (fullRequestName.contains("Is") || fullRequestName.contains("Equals")) {
                    query = "{\"" + firstParam + "\":\"%" + firstParam + "\"}";
                    parameters.add(createResourceParameter(createResourceRequest, firstParam, "Int32", "Query"));

                } else {
                    query = "{\"" + firstParam + "\":\"%" + firstParam + "\"}";
                }
            }

        }
        resource.setCustomQuery(createCustomQuery(containerId, createResourceRequest, query));
        resource.setParameters(parameters);
        return resource;

    }

    public RuntimeResource createResource(String containerId, CreateResourceRequest createResourceRequest)
            throws Exception {
        // Prepare the final resource to add
        Container containerToSave = containerRepository.findById(containerId).get();
        Resource resource = resourceRuntimeMapper.mapToResource(createResourceRequest);
        resource.setCustomQuery(createCustomQuery(containerId, createResourceRequest, "{}"));
        log.info("query type {}", queryType(createResourceRequest));
        if (!queryType(createResourceRequest).equals("findAll")) {
            resource = createQueryAndResourceParameter(containerId, createResourceRequest, resource);
        }
        if (queryType(createResourceRequest).equals("pathQuery")) {
            String firstHeader = createResourceRequest.getRequestparam().get(0);
            List<String> headers = fetchQueryPath(firstHeader);
            if (headers.size() == 1) {
                resource.setPath("/" + resource.getCustomQuery().getCollectionName() + "/{path1}");
            } else {
                resource.setPath("/" + resource.getCustomQuery().getCollectionName() + "/{path1}/{path2}");
            }
        }

        List<APIResponse> apiResponses = new ArrayList<APIResponse>(List.of(new APIResponse("200", "Ok"),
                new APIResponse("401", "Unauthorized"), new APIResponse("403", "Forbidden")));

        // resource.set
        resource.setResourceGroup("Untitled");
        resource.setExecutionType("Query");
        // resource.setResponses(apiResponses);
        resource.getProduces().add("application/json");
        resource.getConsumes().add("application/json");
        resource.getSecurityLevel().add("public");
        List<ResourceParameter> params = resource.getParameters();
        if (createResourceRequest.getHttpMethod().equalsIgnoreCase("post")) {
            ResourceParameter resourceParameter = new ResourceParameter();
            resourceParameter.setIn("Body");
            resourceParameter.setType("object");
            resourceParameter.setName("body");
            resourceParameter.setModelName(resource.getCustomQuery().getCollectionName());
            params.add(resourceParameter);
        }
        resource.setParameters(params);
        // resource.getParameters()
        List<Resource> lresource = containerToSave.getResources();
        lresource.add(resource);
        containerToSave.setResources(lresource);
        List<EndpointModel> endpointModels = containerToSave.getEndpointModels();

        if (createResourceRequest.getHttpMethod().equalsIgnoreCase("post")) {
            endpointModels.add(getEndpointModel(createResourceRequest.getParsedBody(),
                    resource.getCustomQuery().getCollectionName()));
            APISchema apiSchema = new APISchema();
            apiSchema.setRef(resource.getCustomQuery().getCollectionName());
            apiSchema.setArray(false);
            apiSchema.setObject(false);
            APIResponse apiResponse = new APIResponse();
            apiResponse.setCode("200");
            apiResponse.setDescription("ok");
            apiResponse.setSchema(apiSchema);
            apiResponses.set(0, apiResponse);
        }
        resource.setResponses(apiResponses);

        containerToSave.setEndpointModels(endpointModels);

        Container savedContainer = containerRepository.save(containerToSave);
        analyticsService.updateContainerMetrics(savedContainer);
        RuntimeResource runtimeResource = resourceRuntimeMapper.mapToRuntime(resource);
        this.dbSourceRepository.findById(resource.getCustomQuery().getDatasource()).ifPresent(db -> {
            runtimeResource.setConnectionMode(db.getConnectionMode());
            runtimeResource.setPhysicalDatabase(db.getPhysicalDatabase());
            runtimeResource.setProvider(db.getProvider());
            runtimeResource.setBucketName(db.getBucketName());
        });
        runtimeResource.setServiceURL(resource.getServiceURL());
        return runtimeResource;
    }

    public boolean notContains(String key, List<String> keywords) {
        for (int i = 0; i < keywords.size(); i++) {
            if (key.contains(keywords.get(i)))
                return false;
        }
        return true;
    }

    private List<String> fetchQueryPath(String query) {
        log.info("query {}", query);
        List<String> params = new ArrayList<String>();
        List<String> parameters = Arrays.asList(query.split(","));
        log.info("size 1 {}", parameters.size());
        if (parameters.size() == 1) {
            for (int i = 0; i < parameters.size(); i++) {
                params.add(parameters.get(i).substring(1).split(":")[0]);
            }
        } else {
            List<String> headers = Arrays.asList(query.split(",")[0].split(":"));
            params.add(parameters.get(0).split(":")[1].substring(2));
            params.add(parameters.get(1).split(":")[0].substring(1));

        }

        return params;
    }

    private String readQueryPath(String query) {
        String type = "";
        List<String> parameters = Arrays.asList(query.split(","));
        if (parameters.size() == 1) {
            type = "one";
        } else {
            List<String> headers = Arrays.asList(query.split(",")[0].split(":"));
            String operation = headers.get(0).substring(1);
            log.info("operation {}", operation);
            switch (operation) {
                case "$and":
                    log.info("and");
                    type = "and";
                    break;
                case "$or":
                    type = "or";
                    break;
                case "$gt":
                    type = "gt";
                    break;
                case "$lt":
                    type = "lt";
                    break;
                default:
                    type = "Unknown";
            }
        }
        return type;
    }

    public EndpointModel getEndpointModel(String parsedBody, String title) {
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        List<String> attributes = Arrays
                .asList(parsedBody.replace(" ", "").replace("{", "").replace("}", "").split(","));
        attributes.forEach(el -> {
            properties.add(new ModelProperty(el.split(":")[0].substring(1, el.split(":")[0].length() - 1),
                    getType(el.split(":")[1]), false, ""));
        });
        EndpointModel endpointModel = new EndpointModel();
        endpointModel.setProperties(properties);
        endpointModel.setTitle(title);
        return endpointModel;
    }

    public String getType(String value) {
        String result = "string";
        if (BooleanUtils.toBooleanObject(value) != null) {
            result = "boolean";
        } else if (NumberUtils.isNumber(value)) {
            result = "number";
        }
        return result;
    }


}
