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
package fr.codeonce.grizzly.core.service.user;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import fr.codeonce.grizzly.common.runtime.Provider;
import fr.codeonce.grizzly.core.domain.Organization.*;
import fr.codeonce.grizzly.core.domain.container.Container;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSource;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.project.Project;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.user.AccountType;
import fr.codeonce.grizzly.core.domain.user.User;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.domain.user.token.TokenRepository;
import fr.codeonce.grizzly.core.domain.user.token.VerificationToken;
import fr.codeonce.grizzly.core.service.datasource.mongo.MongoCacheService;
import fr.codeonce.grizzly.core.service.util.EmailService;
import fr.codeonce.grizzly.core.service.util.GlobalExceptionUtil;
import fr.codeonce.grizzly.core.service.util.SecurityContextUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@PropertySource("classpath:application.yml")
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private DBSourceRepository dbSourceRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MongoCacheService cacheService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ResourceLoader resourceloader;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private ContainerRepository ContainerRepository;

    @Autowired
    private ProjectRepository ProjectRepository;

    @Value("${frontUrl}")
    private String url;

    @Value("${ssoUrl}")
    private String ssoUrl;

    @Value("${editor.password:atjR3uNN98KWB3qZ}")
    private String editorPwd;

    @Value("${mailingEnabled: false}")
    private Boolean mailingEnabled;

    @Autowired
    private PasswordEncoder passwordEncoder;


    /**
     * Adds a user to the database using a given userDto
     *
     * @param userDto
     * @return UserDto
     * @throws IOException
     */
    public UserDto addUser(UserDto userDto) throws IOException {

        User userToSave = userMapper.mapToDomain(userDto);
        userToSave.setFirstTime(true);
        String email = userToSave.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw GlobalExceptionUtil.duplicateNameFound(User.class, email).get();
        } else {
            userToSave.setPassword(DigestUtils.sha256Hex(userToSave.getPassword() + DigestUtils.sha256Hex("co%de01/")));
            String generatedString = RandomStringUtils.randomAlphabetic(32);
            userToSave.setApiKey(generatedString);
            User savedUser = userRepository.save(userToSave);
            return userMapper.mapToDto(savedUser);
        }
    }

    /**
     * Update users if not exist Apikey
     */

    public UserDto updateUserApiKey(UserDto userDto) {

        String generatedString = RandomStringUtils.randomAlphabetic(32);
        return this.userRepository.findById(userDto.getId()).map(user -> {
            user.setApiKey(generatedString);
            return this.userMapper.mapToDto(this.userRepository.save(user));
        }).orElseThrow();
    }

    /**
     * Update users if not exist Apikey
     */

    public void updateUserAccountType(String userEmail, String accountType, String product) {
        if (product.equals("grizzly_api")) {
            User user = this.userRepository.findByEmail(userEmail).get();
            user.setAccountType(AccountType.valueOf(accountType));
            Date currentDate = new Date();
            user.setBuyOfferDate(currentDate);
        }
    }

    public UserDto updateUser(UserDto userDto) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        if (userDto.getEmail().equals(currentEmail)) {
            return this.userRepository.findByEmail(userDto.getEmail()).map(user -> {
                user.setLastConnection(new Date());
                user.setFirstName(userDto.getFirstName());
                user.setLastName(userDto.getLastName());
                user.setOrganization(userDto.getOrganization());
                user.setPhone(userDto.getPhone());
                user.setFirstTime(userDto.isFirstTime());
                user.setTechContact(userDto.getTechContact());
                user.setBillingContact(userDto.getBillingContact());
                user.setPaymentMethod(userDto.getPaymentMethod());
                user.setHasPaymentMethod(userDto.getHasPaymentMethod());
                user.setNextInvoiceDate(userDto.getNextInvoiceDate());
                user.setBuyOfferDate(userDto.getBuyOfferDate());
                return this.userMapper.mapToDto(this.userRepository.save(user));
            }).orElseThrow();
        } else {
            throw new NoSuchElementException("User is not registered. Please contact us to resolve this issue.");
        }

    }

    /**
     * Returns a specific user using his email
     *
     * @param email
     * @return
     */
    public UserDto getUser(String email) {
        return userMapper.mapToDto(userRepository.findByEmail(email)
                .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, email)));

    }

    public boolean getUserByApiKey(String apikey, String containerId) {
        log.info("Request to fetch user with email : {}", apikey);
        log.info(containerId);
        if (!userRepository.existsByApiKeyIgnoreCase(apikey))
            return false;
        Container container = new Container();
        if (ContainerRepository.existsById(containerId)) {
            container = ContainerRepository.findById(containerId).get();
        } else {
            return false;
        }
        Project project = ProjectRepository.findById(container.getProjectId()).get();
        User user = userRepository.findByEmail(project.getUserEmail()).get();
        boolean x = apikey.equals(user.getApiKey());

        if (userRepository.existsByApiKeyIgnoreCase(apikey) && x)
            return true;
        if (ContainerRepository.findById(containerId).isEmpty())
            return false;
        else
            return false;


    }

    public User getByApiKey(String apiKey) {
        return (userRepository.findByApiKey(apiKey)).get();

    }

    /**
     * Returns all the users in the database
     *
     * @return List<UserDto>
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(u -> userMapper.mapToDto(u)).collect(Collectors.toList());
    }

    /**
     * Verifies the UNICITY of the user email
     *
     * @param email
     * @return boolean
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Confirm a Given Email Address
     *
     * @param
     * @return
     */
    public void confirmEmail(String token) {
        Optional<VerificationToken> verifToken = this.tokenRepository.findByToken(token);
        if (verifToken.isPresent()) {
            this.tokenRepository.delete(verifToken.get());
            // Verify if Token Not Expired
            Date date = new Date();
            Date tokenDate = verifToken.get().getCreatedDate();
            if (TimeUnit.MILLISECONDS.toMinutes((date.getTime() - tokenDate.getTime())) > 3600) {
                return; // Send Expired Token Exception
            }
            Optional<User> userOp = this.userRepository.findByEmail(verifToken.get().getUserEmail());
            if (userOp.isPresent()) {
                // Activate User
                User user = userOp.get();
                user.setEnabled(true);
                this.userRepository.save(user);
            }
        }
    }

    public void checkNewsletter(String email, Boolean firstTime) {
        Optional<User> userOp = this.userRepository.findByEmail(email);
        if (userOp.isPresent()) {
            // Activate User
            User user = userOp.get();
            user.setCheckNewsletter(firstTime);
            this.userRepository.save(user);
        }
    }


    public void confirmRegistration(String userEmail, String lang) throws IOException {
        VerificationToken token = new VerificationToken(userEmail);
        this.tokenRepository.save(token);

        String subject = "";
        if (lang.contains("en")) {
            subject = "Email Confirmation";
        } else {
            subject = "Confirmation de votre email";
        }
        String confirmUrl = ssoUrl + "/confirm/email/" + token.getToken();
        Context context = new Context();
        // Get HTML After Processing the THYMELEAF File
        String content = getOutputContent("confirm-registration.html", "templates/confirm-registration", lang,
                confirmUrl, context);

        emailService.send(content, subject, userEmail, "api");

    }

    /**
     * Send an Email with Token for Password Reset
     *
     * @param email
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public void sendResetPassword(String email, String lang) {

        this.userRepository.findByEmail(email).ifPresentOrElse(user -> {
            StringBuilder tokenValue = new StringBuilder();
            this.tokenRepository.findByUserEmail(email).ifPresentOrElse(tok -> {
                tok.setToken(tokenValue.append(VerificationToken.generateToken(email)).toString());
                log.info("email {} lang {}", tok.getUserEmail(), lang);
                this.tokenRepository.save(tok);
            }, () -> {
                VerificationToken token = new VerificationToken(email);
                tokenValue.append(token.getToken());
                this.tokenRepository.save(token);
            });
            // Send Exception if a Token with That Email Already Exists
            String subject = "";
            if (lang.contains("en")) {
                subject = "Password Reset";
            } else {
                subject = "Réinitialisation du Mot de passe";
            }

            String resetUrl = ssoUrl + "/reset/" + tokenValue.toString();
            try {
                Context context = new Context();
                String content = getOutputContent("reset-password.html", "templates/reset-password", lang, resetUrl, context);
                emailService.send(content, subject, email, "api");
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }, () -> {
            throw new BadCredentialsException("4013"); // Code 3: Email not Registered
        });

    }

    /**
     * Parse the THYMELEAF template, inject the given data, URL for the Button
     *
     * @param templateFileName
     * @param variablesPath
     * @param url
     * @return the output HTML as String
     * @throws IOException
     */
    public String getOutputContent(String templateFileName, String variablesPath, String lang, String url, Context context)
            throws IOException {
        // Choose Language
        if (lang.contains("fr")) {
            variablesPath += "-fr.json";
        } else {
            variablesPath += "-en.json";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = new ObjectMapper()
                .readValue(resourceloader.getResource("classpath:" + variablesPath).getInputStream(), HashMap.class);

        // Set THYMELEAF Variables from JSON File
        context.setVariables(variables);
        context.setVariable("url", url);
        return templateEngine.process(templateFileName, context);
    }

    public String getOutputContent(String templateFileName, String variablesPath, String lang, String url, String orgName)
            throws IOException {
        Context context = new Context();
        context.setVariable("orgName", orgName);
        return getOutputContent(templateFileName, variablesPath, lang, url, context);
    }

    public String getOutputContentWithAtt(String templateFileName, String lang, String last4Digits, Long amount) {
        Context context = new Context();
        context.setVariable("amount", amount);
        context.setVariable("last4Digits", last4Digits);

        return templateEngine.process(templateFileName, context);
    }

    // NEEDS TO BE REFACTORED
    public void logout(HttpServletRequest req) {
        this.dbSourceRepository.findAllByUserEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .parallelStream().forEach(db -> {
                    evictAndLogout(db);
                });
        try {
            req.logout();
        } catch (ServletException e) {
            log.error("an error occurred!", e);
        }
    }

    private Object evictAndLogout(DBSource db) {
        if (db.getProvider().equals(Provider.MONGO) && !db.getConnectionMode().equalsIgnoreCase("FREE")) {
            MongoClient mongoClient = this.cacheService.getMongoClient(db.getId());
            if (mongoClient != null) {
                mongoClient.close();
                this.cacheService.evictMongoClient(db.getId());
            }
        }
        return null;
    }

    /**
     * Verifies if the password matches the email
     *
     * @param email
     * @param oldPassword
     * @return
     */
    public boolean verifyOldPassword(String email, String oldPassword) {
        return this.userRepository.existsByEmailAndPassword(email,
                DigestUtils.sha256Hex(oldPassword + DigestUtils.sha256Hex("co%de01/")));
    }

    public boolean checkToken(String token) {
        return this.tokenRepository.findByToken(token).isPresent();
    }

    public void resetPassword(String token, String password) {
        tokenRepository.findByToken(token).ifPresent(verifToken -> {
            if (isValidToken(verifToken)) {
                userRepository.findByEmail(verifToken.getUserEmail()).ifPresent(user -> {
                    setUserPwd(user, password);
                    tokenRepository.deleteById(verifToken.getId());
                });
            }
        });
    }

    public boolean updateUserPwd(String oldPwd, String newPwd) {
        return this.userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .map(user -> {
                    if (verifyOldPassword(user.getEmail(), oldPwd)) {
                        setUserPwd(user, newPwd);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

    private boolean isValidToken(VerificationToken verifToken) {
        Date date = new Date();
        Date tokenDate = verifToken.getCreatedDate();
        return TimeUnit.MILLISECONDS.toMinutes((date.getTime() - tokenDate.getTime())) <= 3600;
    }

    private void setUserPwd(User user, String newPwd) {
        user.setPassword(DigestUtils.sha256Hex(newPwd + DigestUtils.sha256Hex("co%de01/")));
        userRepository.save(user);
    }

    public String getName(String email) {
        return this.userRepository.findByEmail(email).map(User::getEmail).orElseThrow();
    }

    public UserDto getUserAndOrganisation(String email) {
        UserDto userDto = userMapper.mapToDto(userRepository.findByEmail(email).get());
        userDto.setPassword(null);
        Member member = memberRepository.findByEmail(email);
        if (member != null) {
            userDto.setOrganisationName(organisationRepository.findById(member.getOrganisationId()).get().getName());
            userDto.setOrganisationId(organisationRepository.findById(member.getOrganisationId()).get().getId());
            if (member.getRole().equals("admin")) {
                userDto.setIsAdmin(true);

            } else
                userDto.setIsAdmin(false);
        }
        return userDto;
    }

    public List<String> usersEmails() {
        List<String> emails = new ArrayList<>();
        userRepository.findAll().forEach(user -> {
            emails.add(user.getEmail());
        });
        return emails;
    }

    public UserDto addEditor() throws IOException {
        String email = "editor@codeonce.fr";
        UserDto editor = new UserDto();
        editor.setFirstName("editor");
        editor.setLastName("grizzly");
        editor.setEmail(email);
        editor.setFirstTime(false);
        editor.setPassword(editorPwd);
        return addUser(editor);
    }

    public void sendInvitation(List<String> userEmails, String lang, String orgId, String orgName) throws IOException {
        String subject = "";
        if (lang.contains("en")) {
            subject = "Invitation to join " + orgName + " on Grizzly API";
        } else {
            subject = "Invitation pour joindre " + orgName + " sur Grizzly API";
        }
        String confirmUrl = url + "/sign-up";

        Context context = new Context();
        // Get HTML After Processing the THYMELEAF File
        String content = getOutputContent("invitation.html", "templates/invitation", lang, confirmUrl, context);
        for (String userEmail : userEmails) {
            emailService.send(content, subject, userEmail, "api");
            createInvitation(orgId, orgName, userEmail);

        }
    }

    public void sendOrganizationInvitationToExistedUser(String userEmail, String lang, String orgId, String orgName) throws IOException {
        String subject = "";
        if (lang.contains("en")) {
            subject = "Invitation to join " + orgName + " on Grizzly API";
        } else {
            subject = "Invitation pour joindre " + orgName + " sur Grizzly API";
        }
        String confirmUrl = url;

        // Get HTML After Processing the THYMELEAF File
        String content = getOutputContent("memberInvitation.html", "templates/memberInvitation",
                lang, confirmUrl, orgName);
        emailService.send(content, subject, userEmail, "api");
    }

    public void sendNewsletter(List<String> userEmails, String lang, String newsletterVersion, String subject) {
        // Get HTML After Processing the THYMELEAF File
        userEmails.stream().forEach(userEmail -> {
            Optional<User> userOp = this.userRepository.findByEmail(userEmail);
            if (userOp.get().getCheckNewsletter() == null || userOp.get().getCheckNewsletter()) {
                String confirmUrl = url;
                Context context = new Context();
                String path = newsletterVersion.substring(0, newsletterVersion.indexOf(".html"));
                //String content = "";
                try {
                    String content = getOutputContent("newsletters/" + newsletterVersion, "templates/newsletters/" + path, lang, confirmUrl, context);
                    emailService.send(content, subject, userEmail, "api");
                    checkNewsletter(userEmail, false);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
    }

    private void createInvitation(String orgId, String orgName, String userEmail) {
        Invitation i = new Invitation();
        i.setOrganizationId(orgId);
        i.setOrgnizationName(orgName);
        i.setUserEmail(userEmail);
        this.invitationRepository.save(i);
    }

    public List<User> sendToAllUsers() throws IOException {

        String subject = "New version of Grizzly API CLI is available";
        List<User> users = userRepository.findAll();
        // Get HTML After Processing the THYMELEAF File
        String content = "Check this link for more details <a href=\'https://www.npmjs.com/package/grizzly_api_cli\'>Grizzly API CLI Repository</a>";
        for (User userEmail : users) {
            emailService.send(content, subject, userEmail.getEmail(), "api");
        }
        return users;
    }

    public void generateApiKey() {
        List<User> users = userRepository.findAll();
        users.forEach(el -> {
            if (el.getApiKey() == null) {
                el.setApiKey(RandomStringUtils.randomAlphabetic(32));
                userRepository.save(el);
            }
        });
    }

    public String getConnectedUserEmail() {
        String currentUserEmail = SecurityContextUtil.getCurrentUserEmail();
        if (currentUserEmail == null) {
            return null;
        }
        if (!currentUserEmail.contains("@")) {
            currentUserEmail = userRepository
                    .findByApiKey(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())
                    .get().getEmail();
        }
        return currentUserEmail;
    }

    public User getConnectedUserByApiKey() {
        String key = SecurityContextUtil.getCurrentUserEmail();
        User user = null;
        if (key == null) {
            return null;
        }
        if (key.contains("@")) {
            user = userRepository
                    .findByEmail(key)
                    .orElseThrow(GlobalExceptionUtil.notFoundException(User.class, key));
        }
        return user;
    }

    //@Scheduled(fixedDelay = 10000)
    //every day at 12pm
    @Scheduled(cron = "0 0 12 * * ?")
    public void sendWelcomeMail() {
        if (mailingEnabled) {
            String subject = "Welcome to Grizzly API";

            String confirmUrl = url;
            Context context = new Context();
            // Get HTML After Processing the THYMELEAF File
            List<User> users = userRepository.findAll();
            users.stream().filter(u -> !u.getEmail().equals("editor@codeonce.fr")).forEach(user -> {
                Date date = new Date();
                int diffInDays = (int) ((date.getTime() - user.getRegistrationDate().getTime()) / (1000 * 60 * 60 * 24));

                if (diffInDays >= 3 && (!user.isEnabled() || (user.isEnabled() && (user.getLastConnection() == null ||
                        user.getLastConnection() == user.getRegistrationDate())))) {
                    if (user.getWelcomeEmailDay() == 0) {
                        try {
                            context.setVariable("firstname", user.getFirstName());
                            String content = getOutputContent("/welcome/welcome1.html", "templates/welcome/welcome1", "en", confirmUrl, context);
                            emailService.send(content, subject, user.getEmail(), "api");
                        } catch (IOException e) {
                        }
                    }
                    if (user.getWelcomeEmailDay() == 5) {
                        try {
                            context.setVariable("firstname", user.getFirstName());
                            String content = getOutputContent("/welcome/welcome3.html", "templates/welcome/welcome3", "en", confirmUrl, context);
                            emailService.send(content, subject, user.getEmail(), "api");
                        } catch (IOException e) {
                        }
                    }
                    user.setWelcomeEmailDay(user.getWelcomeEmailDay() + 1);
                    userRepository.save(user);


                }
            });
        }
    }

    public void initializeWelcomeEmailField() {
        userRepository.findAll().parallelStream().forEach(user -> {
            user.setWelcomeEmailDay(0);
            userRepository.save(user);
        });
    }

    public void initializecheckNewsletterfield() {
        userRepository.findAll().parallelStream().forEach(user -> {
            user.setCheckNewsletter(true);
            userRepository.save(user);
        });
    }
}
