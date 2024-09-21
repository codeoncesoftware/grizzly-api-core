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
package fr.codeonce.grizzly.core.service.test;

import fr.codeonce.grizzly.core.domain.Organization.InvitationRepository;
import fr.codeonce.grizzly.core.domain.Organization.MemberRepository;
import fr.codeonce.grizzly.core.domain.Organization.OrganisationRepository;
import fr.codeonce.grizzly.core.domain.Organization.TeamRepository;
import fr.codeonce.grizzly.core.domain.analytics.AnalyticsRepository;
import fr.codeonce.grizzly.core.domain.analytics.ProjectUseRepository;
import fr.codeonce.grizzly.core.domain.config.AppProperties;
import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import fr.codeonce.grizzly.core.domain.config.GrizzlyHubProperties;
import fr.codeonce.grizzly.core.domain.container.ContainerRepository;
import fr.codeonce.grizzly.core.domain.container.ImportedSwaggerRepository;
import fr.codeonce.grizzly.core.domain.container.InvalidSwaggerRepository;
import fr.codeonce.grizzly.core.domain.container.hierarchy.ContainerHierarchyRepository;
import fr.codeonce.grizzly.core.domain.datasource.DBSourceRepository;
import fr.codeonce.grizzly.core.domain.docker.DockerExportRepository;
import fr.codeonce.grizzly.core.domain.function.FunctionRepository;
import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProviderRepository;
import fr.codeonce.grizzly.core.domain.log.LogRepository;
import fr.codeonce.grizzly.core.domain.project.ProjectRepository;
import fr.codeonce.grizzly.core.domain.user.InvoiceRepository;
import fr.codeonce.grizzly.core.domain.user.PaymentRepository;
import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.domain.user.token.TokenRepository;
import fr.codeonce.grizzly.core.function.util.FunctionRuntimeMapper;
import fr.codeonce.grizzly.core.service.identityprovider.IdentityProviderMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest()

public abstract class AbstractServiceTest {

    @MockBean
    protected FunctionRuntimeMapper functionRuntimeMapper;

    @MockBean
    protected FunctionRepository functionRepository;

    @MockBean
    protected ContainerRepository containerRepository;

    @MockBean
    protected DockerExportRepository dockerExportRepository;

    @MockBean
    protected InvalidSwaggerRepository invalidSwaggerRepository;

    @MockBean
    protected ProjectUseRepository projectUseRepository;

    @MockBean
    protected LogRepository logRepository;

    @MockBean
    protected ImportedSwaggerRepository importedSwaggerRepository;

    @MockBean
    protected ProjectRepository projectRepository;

    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected GrizzlyCoreProperties resourceManagerProperties;

    @MockBean
    protected GrizzlyHubProperties hubProperties;

    @MockBean
    protected AppProperties appProperties;

    @MockBean
    protected GridFsTemplate gridFsTemplate;

    @MockBean
    protected ContainerHierarchyRepository containerHierarchyRepository;

    @MockBean
    protected DBSourceRepository dbSourceRepository;

    @MockBean
    protected TokenRepository tokenRepository;

    @MockBean
    protected CacheManager cacheManager;

    @MockBean
    protected JavaMailSender javaMailSender;

    @MockBean
    protected AnalyticsRepository analyticsRepository;

    @MockBean
    protected InvitationRepository invitationRepository;

    @MockBean
    protected MemberRepository memberRepository;

    @MockBean
    protected OrganisationRepository organisationRepository;

    @MockBean
    protected TeamRepository teamRepository;

    @MockBean
    protected InvoiceRepository invoiceRepository;

    @MockBean
    protected PaymentRepository paymentRepository;

    @MockBean
    protected IdentityProviderRepository identityProviderRepository;

    @MockBean
    protected IdentityProviderMapper idpMapper;
}
