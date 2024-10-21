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

import fr.codeonce.grizzly.core.domain.Organization.Team;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.organization.ITeamService;
import fr.codeonce.grizzly.core.service.user.UserDto;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = {"*"}, allowedHeaders = {"*"})
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    ITeamService teamService;

    @GetMapping("/{email}")
    public UserDto getUser(@PathVariable String email) {
        log.info("Request to fetch user with email : {}", email);
        return userService.getUser(email);
    }

    @GetMapping("/")
    public UserDto getUserAndOrganisation() {
        String email = SecurityContextUtil.getCurrentUserEmail();
        if (!email.contains("@")) {
            email = userRepository
                    .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                    .get().getEmail();
        }
        return userService.getUserAndOrganisation(email);
    }

    @GetMapping("/apikey/{apikey}")
    public User getUserByApiKey(@PathVariable String apikey) {
        log.info("Request to fetch user with email : {}", apikey);
        return userService.getByApiKey(apikey);
    }

    @PutMapping("/update")
    public UserDto updateUser(@RequestBody UserDto userDto) {
        log.info("Request to update user with email : {}", userDto.getEmail());
        return userService.updateUser(userDto);
    }

    @PutMapping("/update/pwd")
    public boolean updateUserPwd(@RequestParam String oldPwd, @RequestParam String newPwd) {
        log.info("Request to update password");
        return userService.updateUserPwd(oldPwd, newPwd);
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/send/allUsers")
    public List<User> getAllUsersForJenkins() throws IOException {
        return userService.sendToAllUsers();
    }

    @GetMapping("/teams")
    public List<Team> getTeams() {
        return teamService.findAllTeams();
    }

    @PostMapping("/teams/{adminEmail}")
    public Team AddTeam(@RequestBody Team team, @PathVariable String adminEmail) {
        return teamService.addTeam(team, adminEmail);
    }

    @PutMapping("/updateTeam/{id}")
    public Team updateTeam(@RequestBody Team team, @PathVariable String id) {
        return teamService.updateTeam(team, id);
    }

    @DeleteMapping("/teams/delete/{id}")
    public void deleteTeam(@PathVariable String id) {
        teamService.deleteTeam(id);
    }

    @DeleteMapping("/deleteMemberFromTeam/{id}/{email}")
    public void deleteMemberFromTeam(@PathVariable String id, @PathVariable String email) {
        teamService.deleteMemberFromTeam(id, email);
    }

    @GetMapping("/team/{id}")
    public Team getTeam(@PathVariable String id) {
        return teamService.findById(id);
    }

    @GetMapping("/teamsByOrganisationId/{organisationId}")
    public List<Team> getTeamsByOrganisationId(@PathVariable String organisationId) {
        return teamService.findByOrganisationId(organisationId);
    }

    @PostMapping("/invite")
    public void invite(@RequestBody List<String> userEmails, @RequestParam String orgId, @RequestParam String orgName,
                       HttpServletRequest req) throws IOException {
        userService.sendInvitation(userEmails, req.getHeader("Accept-Language"), orgId, orgName);
    }

    @PostMapping("/mailOrganization")
    public void inviteOrganizationToExistedUser(@RequestBody String userEmail, @RequestParam String orgId, @RequestParam String orgName,
                                                HttpServletRequest req) throws IOException {
        userService.sendOrganizationInvitationToExistedUser(userEmail, req.getHeader("Accept-Language"), orgId, orgName);
    }

    @PostMapping("/sendNewsletters")
    public void sendNewsletters(HttpServletRequest req, @RequestParam String newsletterVersion,
                                @RequestParam String subject) {
        userService.sendNewsletter(userService.usersEmails(), "en", newsletterVersion,
                subject);
    }

    @GetMapping("/emails")
    public List<String> getAllUsersEmails() {
        return userService.usersEmails();
    }

    @GetMapping("/changeAccountType")
    public void changeAccountType(@RequestParam String userEmail, @RequestParam String accountType, @RequestParam String product) {
        this.userService.updateUserAccountType(userEmail, accountType, product);
    }

    // generate apiKey for existing users in prodDB for upgrade purposes
    @GetMapping("/generate/apiKey")
    public void generateApiKey() {
        userService.generateApiKey();
    }

    @GetMapping("/initializewelcomeEmailfield")
    public void initializeWelcomeEmailField() {
        userService.initializeWelcomeEmailField();
    }

    @GetMapping("/initializecheckNewsletterfield")
    public void initializecheckNewsletterfield() {
        userService.initializecheckNewsletterfield();
    }

}
