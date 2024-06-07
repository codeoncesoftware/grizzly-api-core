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
package fr.codeonce.grizzly.core.service.function;

import fr.codeonce.grizzly.core.domain.function.Function;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.service.container.ContainerService;
import fr.codeonce.grizzly.core.service.resource.ResourceService;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
public class FunctionService {

    private static final Logger log = LoggerFactory.getLogger(FunctionService.class);

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private FunctionFunctionDtoMapperImpl functionDtoMapper;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ContainerService containerService;

    public FunctionDto createFunction(FunctionDto f) {
        log.info("Creating a function ");
        Function function = functionRepository.save(functionDtoMapper.dtoToFunction(f));
        log.info("Function created with ID : {} ", function.getId());
        return functionDtoMapper.functionToDto(function);
    }

    public List<FunctionDto> findAllFunction() {
        log.info("Request to get all the functions ");
        return functionRepository.findAll().stream().map(f -> {
            return functionDtoMapper.functionToDto(f);
        }).collect(Collectors.toList());
    }

    public List<FunctionDto> findFunctionByProject(String projectId) {
        log.info("Request to find the function within project with Id: ", projectId);
        return functionRepository.findByProjectId(projectId).stream().map(f -> {
            return functionDtoMapper.functionToDto(f);
        }).collect(Collectors.toList());
    }

    public FunctionDto findFunctionByNameAndVersion(String projectId, String name, String version) {
        log.info("Request to find the function with Name : {} and version : {} ", name, version);
        return functionRepository.findByProjectIdAndNameAndVersion(projectId, name, version)
                .map(f -> functionDtoMapper.functionToDto(f))
                .orElseThrow(GlobalExceptionUtil.notFoundException(Function.class, name));

    }
    /*
     * public List<FunctionDto> findFunctionByResource(List<String> ids) { Query q =
     * Query.query(Criteria.where("id").in(ids)); return mongoOperations.find(q,
     * Function.class).stream().map(e -> functionDtoMapper.functionToDto(e))
     * .collect(Collectors.toList()); }
     */

    public FunctionDto findFunctionById(String id) {

        return functionRepository.findById(id).map(f -> functionDtoMapper.functionToDto(f))
                .orElseThrow(GlobalExceptionUtil.notFoundException(Function.class, id));
    }

    public void deleteFunction(String id) {
        log.info("Request to delete the function with Id : {} ", id);
        functionRepository.deleteById(id);
    }

    public void deleteFunction(String projectId, String name, String version) {
        log.info("Request to delete the function within porject :{}  with name : {}  and version :{}", projectId, name,
                version);
        functionRepository.deleteByProjectIdAndNameAndVersion(projectId, name, version);
    }

    public FunctionDto updateFunction(String id, FunctionDto function) throws NotFoundException {
        log.info("Request to update the function with Id : {} ", id);
        Optional<Function> oldFunction = functionRepository.findById(id);
        oldFunction.map((p) -> {
            p.setVersion(function.getVersion());
            p.setLanguage(function.getLanguage());
            p.setLogs(function.getLogs());
            p.setName(function.getName());
            p.setFunction(function.getFunction());
            p.setClassName(function.getClassName());
            p.setMethodName(function.getMethodName());
            p.setModel(function.getModel());
            p.setModelName(function.getModelName());

            return p;
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Function.class, id));

        return functionDtoMapper.functionToDto(functionRepository.save(oldFunction.get()));

    }

    public FunctionDto updateFunction(String projectId, String name, String version, FunctionDto function)
            throws NotFoundException {
        log.info("Request to update the function withing project {} with name : {} and version : {}", projectId, name,
                version);
        Optional<Function> oldFunction = functionRepository.findByProjectIdAndNameAndVersion(projectId, name, version);
        Function newFunction = oldFunction.map((p) -> {
            p.setLanguage(function.getLanguage());
            p.setVersion(function.getVersion());
            p.setLogs(function.getLogs());
            p.setName(function.getName());
            p.setFunction(function.getFunction());
            p.setClassName(function.getClassName());
            p.setMethodName(function.getMethodName());
            p.setModel(function.getModel());
            p.setModelName(function.getModelName());
            if (p.getLanguage().equalsIgnoreCase("AWS Lambda")) {
                p.setAwsCredentials(function.getAwsCredentials());
                p.setAwsFunctionName(function.getAwsFunctionName());
            }
            return p;
        }).orElseThrow(GlobalExceptionUtil.notFoundException(Function.class, name));

        return functionDtoMapper.functionToDto(functionRepository.save(newFunction));

    }

    public FunctionDto cloneFunction(String projectId, String name, String oldVersion, String version) {
        Function clonedFunction = functionRepository.findByProjectIdAndNameAndVersion(projectId, name, oldVersion)
                .orElseThrow(null);
        clonedFunction.setVersion(version);
        clonedFunction.setId(null);

        clonedFunction = functionRepository.save(clonedFunction);
        return functionDtoMapper.functionToDto(clonedFunction);

    }

}
