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
package fr.codeonce.grizzly.core.service.fs;

import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.function.Function;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.util.FileSystemUtil;
import fr.codeonce.grizzly.core.service.analytics.AnalyticsService;
import fr.codeonce.grizzly.core.service.container.ContainerSwaggerService;
import fr.codeonce.grizzly.core.service.project.ProjectDto;
import fr.codeonce.grizzly.core.service.util.CustomGitAPIException;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class GitHandler {

    static final String TMPDIR = FileSystemUtil.getTempFolder();

    private static final Logger log = LoggerFactory.getLogger(GitHandler.class);

    @Autowired
    private FilesHandler filesHandler;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ContainerSwaggerService containerSwaggerService;

    /**
     * Checks validity of provided git credentials
     *
     * @param projectDto
     * @return boolean value indicating validity
     */
    public boolean checkCredentials(ProjectDto projectDto) {
        File fileDir = null;
        try {
            String dir = TMPDIR + UUID.randomUUID().toString().substring(0, 8) + File.separator;
            fileDir = makeGitDirectory(dir);
            Git git = null;
            CloneCommand gitCommand = Git.cloneRepository()//
                    .setURI(projectDto.getGitUrl())//
                    .setDirectory(fileDir);
            if (StringUtils.isNoneEmpty(projectDto.getGitUsername(), projectDto.getGitPassword())) {
                git = gitCommand
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(projectDto.getGitUsername(),
                                projectDto.getGitPassword()))
                        .call();
            } else {
                if (StringUtils.isNotEmpty(projectDto.getGitToken())) {
                    git = gitCommand.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(projectDto.getGitToken(), "")).call();
                } else {
                    git = gitCommand.call();
                }
            }
        } catch (Exception e) {
            log.error("Exception occured {}", e);
            return false;
        } finally {
            deleteDir(fileDir);
        }
        return true;
    }

    /***
     * For a given GIT Repository URL return Branch List
     *
     * @param gitRepoUrl
     * @return list of BRANCHS
     * @throws CustomGitAPIException
     */
    public List<String> getRepoBranchsList(String gitRepoUrl, String gitUsername, String gitPassword, String gitToken) {
        log.debug("Fetching Branchs for Repo : {}", log);
        Collection<Ref> refs;
        List<String> branches = new ArrayList<>();
        try {

            if (StringUtils.isNoneEmpty(gitUsername, gitPassword)) {
                refs = Git.lsRemoteRepository().setHeads(true).setRemote(gitRepoUrl)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitPassword))
                        .call();
            } else {
                if (StringUtils.isNotEmpty(gitToken)) {
                    refs = Git.lsRemoteRepository().setHeads(true).setRemote(gitRepoUrl)
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitToken, "")).call();
                } else {
                    refs = Git.lsRemoteRepository().setHeads(true).setRemote(gitRepoUrl).call();
                }
            }
            for (Ref ref : refs) {
                branches.add(ref.getName().substring(ref.getName().lastIndexOf('/') + 1, ref.getName().length()));
            }
            Collections.sort(branches);

        } catch (Exception e) {
            throw new BadCredentialsException("4011", e);
        }
        return branches;
    }

    private Git cloneRepo(ProjectDto projectDto, String dir, Git git) throws GitAPIException, IOException {
        boolean exitingBranch = getRepoBranchsList(projectDto.getGitUrl(), projectDto.getGitUsername(),
                projectDto.getGitPassword(), projectDto.getGitToken()).contains(projectDto.getGitBranch());
        try {
            if (exitingBranch) {
                CloneCommand gitCommand = Git.cloneRepository()//
                        .setURI(projectDto.getGitUrl())//
                        .setDirectory(makeGitDirectory(dir))//
                        .setBranch(projectDto.getGitBranch());//
                if (StringUtils.isNoneEmpty(projectDto.getGitUsername(), projectDto.getGitPassword())) {
                    git = gitCommand.setRemote("origin")
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(projectDto.getGitUsername(),
                                    projectDto.getGitPassword()))
                            .call();
                } else {
                    if (StringUtils.isNotEmpty(projectDto.getGitToken())) {
                        git = gitCommand.setCredentialsProvider(
                                new UsernamePasswordCredentialsProvider(projectDto.getGitToken(), "")).call();
                    } else {
                        git = gitCommand.call();
                    }
                }
            } else {
                CloneCommand gitCommand = Git.cloneRepository()//
                        .setURI(projectDto.getGitUrl())//
                        .setDirectory(makeGitDirectory(dir));//
                if (StringUtils.isNoneEmpty(projectDto.getGitUsername(), projectDto.getGitPassword())) {
                    git = gitCommand
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(projectDto.getGitUsername(),
                                    projectDto.getGitPassword()))
                            .call();
                } else {
                    if (StringUtils.isNotEmpty(projectDto.getGitToken())) {
                        git = gitCommand.setCredentialsProvider(
                                new UsernamePasswordCredentialsProvider(projectDto.getGitToken(), "")).call();
                    } else {
                        git = gitCommand.call();
                    }
                }
                git.branchCreate().setName(projectDto.getGitBranch()).call();
                git.checkout().setName(projectDto.getGitBranch()).call();
            }
        } catch (GitAPIException e) {
            throw new CustomGitAPIException("Invalid repository", e);
        } catch (IOException e) {
            throw new CustomGitAPIException("Invalid directory", e);
        }
        return git;
    }

    @SuppressWarnings("resource")
    public String gitFirstSync(ProjectDto projectDto, List<MultipartFile> modelFiles, String projectId,
                               String containerId, String dbsourceId, String databaseName) throws CustomGitAPIException {
        Container container = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        Git git = null;
        String dir = TMPDIR + projectId + File.separator;
        try {
            git = cloneRepo(projectDto, dir, git);
            // create directory
            createSwaggerFile(git, projectDto, containerId, container.getSwaggerUuid(), dir, container);
            // create directory
            createTypeScriptModels(git, projectDto, modelFiles, container, dir);
            // save functions to files
            createFunctions(git, projectDto, projectId, dir);
            // commiting
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Update swagger").call();
            // pushing
            pushOperation(git, projectDto);
        } catch (IOException e) {
            throw new CustomGitAPIException("Unable to open directory", e);
        } catch (GitAPIException e) {
            throw new CustomGitAPIException(dir, e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
        File repo = new File(dir);
        long fileSize = FileUtils.sizeOfDirectory(repo) / 1000; // in KiloBytes
        if (!analyticsService.storageLimitsOnUpload(fileSize)) {
            return "-1"; // limit reached!
        }
        return filesHandler.getJsonHierarchy(dir, containerId, dbsourceId, databaseName);
    }

    @SuppressWarnings("resource")
    public String gitSync(ProjectDto projectDto, List<MultipartFile> modelFiles, String projectId, String containerId,
                          String dbsourceId, String databaseName) throws CustomGitAPIException {
        Container container = containerRepository.findById(containerId)
                .orElseThrow(GlobalExceptionUtil.notFoundException(Container.class, containerId));
        String dir = TMPDIR + projectId + File.separator;
        Git git = null;
        try (Repository localRepo = new FileRepository(dir + "/.git")) {
            git = new Git(localRepo);
            git = gitOperations(git, projectDto, dir, projectId, containerId, container.getSwaggerUuid(), container,
                    modelFiles);
        } catch (IOException | JGitInternalException e) {
            throw new CustomGitAPIException("Cant open directory " + dir, e);
        } catch (GitAPIException e) {
            throw new CustomGitAPIException("Error when operating with the repository", e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
        File repo = new File(dir);

        long fileSize = FileUtils.sizeOfDirectory(repo) / 1000; // in KiloBytes
        if (!analyticsService.storageLimitsOnUpload(fileSize)) {
            return "-1"; // limit reached!
        }
        return filesHandler.getJsonHierarchy(dir, containerId, dbsourceId, databaseName);
    }

    private void createSwaggerFile(Git git, ProjectDto projectDto, String containerId, String swaggerUuid, String dir,
                                   Container container) throws GitAPIException, IOException {
        makeGitDirectory(dir + File.separator + projectDto.getName() + File.separator + container.getName());

        File swagger = new File(dir + File.separator + projectDto.getName() + File.separator + container.getName(),
                "swagger.json");
        File swaggerDev = new File(dir + File.separator + projectDto.getName() + File.separator + container.getName(),
                "swagger-dev.json");
        try {
            swagger.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(swagger));
            writer.write(containerSwaggerService.getSwaggerJson("prod", containerId, swaggerUuid));
            swaggerDev.createNewFile();
            writer.close();
            writer = new BufferedWriter(new FileWriter(swaggerDev));
            writer.write(containerSwaggerService.getSwaggerJson("dev", containerId, swaggerUuid));
            writer.close();
        } catch (IOException e) {
            throw new CustomGitAPIException("Cant open directory " + dir, e);
        }
    }

    private void createTypeScriptModels(Git git, ProjectDto projectDto, List<MultipartFile> modelFiles,
                                        Container container, String repoDir) throws GitAPIException {
        if (!modelFiles.equals(Collections.emptyList())) {
            String modelsDir = repoDir + File.separator + projectDto.getName() + File.separator + container.getName()
                    + File.separator + "models";
            File gitDirectory = new File(modelsDir);
            gitDirectory.mkdir();
            modelFiles.forEach(file -> {
                saveModel(file, modelsDir);
            });
        }
    }

    private void createFunctions(Git git, ProjectDto projectDto, String projectId, String dir) throws GitAPIException {
        List<Function> functions = functionRepository.findByProjectId(projectId);
        if (!functions.isEmpty()) {
            functions.forEach(function -> {
                try {
                    if (!Files.exists(
                            Path.of(dir + File.separator + projectDto.getName() + File.separator + "functions"))) {
                        Files.createDirectories(
                                Path.of(dir + File.separator + projectDto.getName() + File.separator + "functions"));
                    }
                    Files.writeString(
                            Path.of(dir + File.separator + projectDto.getName() + File.separator + "functions"
                                    + File.separator + function.getName() + "-" + function.getVersion() + ".js"),
                            function.getFunction());

                } catch (IOException e) {
                    log.error("can't parse function {}", e);
                }
            });
        }
    }

    private void pullOperation(Git git, ProjectDto projectDto, String dir) throws CustomGitAPIException {
        try {
            PullResult pullResult = null;
            PullCommand pullCommand = git.pull().setRemote("origin").setRemoteBranchName(projectDto.getGitBranch());
            if (StringUtils.isNoneEmpty(projectDto.getGitUsername(), projectDto.getGitPassword())) {
                pullResult = pullCommand
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(projectDto.getGitUsername(),
                                projectDto.getGitPassword()))
                        .call();
            } else {
                if (StringUtils.isNotEmpty(projectDto.getGitToken())) {
                    pullResult = pullCommand.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(projectDto.getGitToken(), "")).call();
                } else {
                    pullResult = pullCommand.call();
                }
            }
            if (pullResult.isSuccessful() && !functionRepository.findByProjectId(projectDto.getId()).isEmpty()) {
                Path funcPath = Path.of(dir + File.separator + projectDto.getName() + File.separator + "functions");
                try (var functionFiles = Files.list(funcPath)) {
                    functionFiles.forEach(file -> {
                        String fileName = file.getFileName().toString().replace(".js", "");
                        functionRepository.findByProjectIdAndNameAndVersion(projectDto.getId(), fileName.split("-")[0],
                                fileName.split("-")[1]).ifPresent(function -> {
                            try {
                                function.setFunction(Files.readString(file).replace("\r", ""));
                                functionRepository.save(function);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    });
                } catch (IOException e1) {
                    throw new CustomGitAPIException("Can't open directory " + funcPath, e1);
                }
            }
        } catch (GitAPIException e) {
            throw new CustomGitAPIException("Error while pulling from repostiroy", e);
        }
    }

    private void pushOperation(Git git, ProjectDto projectDto) throws CustomGitAPIException {
        try {
            PushCommand pushCommand = git.push();
            pushCommand.setRemote(projectDto.getGitUrl());
            if (StringUtils.isNoneEmpty(projectDto.getGitUsername(), projectDto.getGitPassword())) {
                pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(projectDto.getGitUsername(),
                        projectDto.getGitPassword())).call();
            } else {
                if (StringUtils.isNotEmpty(projectDto.getGitToken())) {
                    pushCommand.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(projectDto.getGitToken(), "")).call();
                } else {
                    pushCommand.call();
                }
            }
        } catch (GitAPIException e) {
            throw new CustomGitAPIException("Error when pushing latest changes to remote repository", e);
        }
    }

    public Git gitOperations(Git git, ProjectDto projectDto, String dir, String projectId, String containerId,
                             String swaggerUuid, Container container, List<MultipartFile> modelFiles) throws GitAPIException, IOException {
        // create directory
        createSwaggerFile(git, projectDto, containerId, swaggerUuid, dir, container);
        // create directory
        createTypeScriptModels(git, projectDto, modelFiles, container, dir);
        // save functions to files
        createFunctions(git, projectDto, projectId, dir);

        git.add().addFilepattern(".").call();
        git.commit().setMessage("Update swagger").call();

        // pull
        pullOperation(git, projectDto, dir);
        // pushing
        pushOperation(git, projectDto);

        git.close();
        return git;
    }

    private static File makeGitDirectory(String fullRepoName) throws IOException {
        deleteDir(new File(fullRepoName));
        File gitDirectory = new File(fullRepoName);
        gitDirectory.mkdirs();
        return gitDirectory;
    }

    private static boolean deleteDir(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(file, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public static void saveModel(MultipartFile file, String pathToSave) {
        try {
            if (!Files.exists(Path.of(TMPDIR + pathToSave))) {
                Files.createDirectories(Path.of(TMPDIR + pathToSave));
                log.debug("folder to save zip in : {} {}", TMPDIR, pathToSave);

            }
            log.debug("file to save : {}", file.getOriginalFilename());
            Files.write(Path.of(TMPDIR + pathToSave + File.separator + file.getOriginalFilename()), file.getBytes());
        } catch (IOException e1) {
            GlobalExceptionUtil.fileNotFoundException(file.getOriginalFilename());
        }
    }

}
