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
package fr.codeonce.grizzly.core.service.log;

import fr.codeonce.grizzly.core.domain.log.Log;
import fr.codeonce.grizzly.core.domain.log.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private LogDtoMapper logDtoMapper;

    public LogDto createLog(LogDto r) {
        log.info("Creating a Log ");
        Log savedLog = logRepository.save(logDtoMapper.dtoTolog(r));
        log.info("Log created with ID : {} ", savedLog.getId());
        return logDtoMapper.logToDto(savedLog);
    }

    public void deleteLog(String id) {
        log.info("Request to delete the log with Id : {} ", id);
        logRepository.deleteById(id);
    }

    public void deleteByProject(String projectId) {
        log.info("Request to delete all the log within project with Id : {} ", projectId);
        logRepository.deleteByProjectId(projectId);
    }

    public void deleteByContainer(String containerId) {
        log.info("Request to delete all the log within container with Id : {} ", containerId);
        logRepository.deleteByContainerId(containerId);
    }

    public String aggregateLogger(String projectId) {
        SimpleDateFormat dateFor = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        return logRepository
                .getByProjectId(projectId).stream().map(l -> "<p>" + dateFor.format(l.getTime()) + " " + l.getLogLevel()
                        + " [" + l.getSource() + "]  " + l.getMessage() + "</p>")
                .collect(Collectors.joining("", "<h3>LOGS</h3>", ""));

    }

}
