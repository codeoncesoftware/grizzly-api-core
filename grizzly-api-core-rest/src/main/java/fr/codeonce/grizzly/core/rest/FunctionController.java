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
package fr.codeonce.grizzly.core.rest;

import fr.codeonce.grizzly.core.service.function.FetchFonctionsRequest;
import fr.codeonce.grizzly.core.service.function.FunctionDto;
import fr.codeonce.grizzly.core.service.function.FunctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/function")
public class FunctionController {

    private static final Logger log = LoggerFactory.getLogger(FunctionController.class);

    @Autowired
    private FunctionService functionService;

    @PostMapping(path = "/create")
    public FunctionDto saveFunction(@RequestBody FunctionDto function) {
        return functionService.createFunction(function);
    }

    @GetMapping(path = "/get/all/project/{projectId}")
    public List<FunctionDto> getFunctionsByProject(@PathVariable String projectId) {
        return functionService.findFunctionByProject(projectId);
    }

    @GetMapping(path = "/get/{projectId}/{name}/{version}")
    public FunctionDto getFunctionByNameAndVersion(@PathVariable String projectId, @PathVariable String name,
                                                   @PathVariable String version) {
        return functionService.findFunctionByNameAndVersion(projectId, name, version);
    }

    @PostMapping(path = "/get")
    public FunctionDto getFunctionById(@RequestBody FetchFonctionsRequest fetchFonctionsRequest) {
        return functionService.findFunctionById(fetchFonctionsRequest.getFunctionId());
    }

    /*
     * @PostMapping(path = "/get/all/resource") public List<FunctionDto>
     * getFunctionsByResource(@RequestBody FetchFonctionsRequest
     * fetchFonctionsRequest) { return
     * functionService.findFunctionByResource(fetchFonctionsRequest.getFunctions());
     * }
     */

    /*
     * @PostMapping(path = "resource/add/") public void
     * addFunctionToResource(@RequestBody FuncionCreationRequest f) throws
     * CustomGitAPIException { functionService.addFunctionToResource(f); }
     */

    @DeleteMapping(path = "delete/{id}")
    public void deleteFunction(@PathVariable String id) {
        functionService.deleteFunction(id);
    }

    @DeleteMapping(path = "delete/{projectId}/{name}/{version}")
    public void deleteFunction(@PathVariable String projectId, @PathVariable String version,
                               @PathVariable String name) {

        functionService.deleteFunction(projectId, name, version);
    }

    @PutMapping(path = "/update/{id}")
    public FunctionDto updateFuction(@PathVariable String id, @RequestBody FunctionDto functionDto)
            throws NotFoundException {
        return functionService.updateFunction(id, functionDto);
    }

    @PutMapping(path = "/update/{projectId}/{name}/{version}")
    public FunctionDto updateFuctionByNameAndVersion(@PathVariable String projectId, @PathVariable String name,
                                                     @PathVariable String version, @RequestBody FunctionDto functionDto) throws NotFoundException {
        return functionService.updateFunction(projectId, name, version, functionDto);
    }

    @PostMapping("/clone/{projectId}/{name}/{oldVersion}")
    public FunctionDto cloneFunction(@PathVariable String projectId, @PathVariable String oldVersion,
                                     @PathVariable String name, @RequestBody String version) {

        return functionService.cloneFunction(projectId, name, oldVersion, version);

    }

}
