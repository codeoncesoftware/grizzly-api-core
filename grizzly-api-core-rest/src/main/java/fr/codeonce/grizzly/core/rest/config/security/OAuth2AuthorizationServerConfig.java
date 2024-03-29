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
package fr.codeonce.grizzly.core.rest.config.security;

import java.security.KeyPair;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.InMemoryApprovalStore;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import fr.codeonce.grizzly.core.domain.config.GrizzlyCoreProperties;
import fr.codeonce.grizzly.core.domain.config.GrizzlyDockerRuntimeProperties;
import fr.codeonce.grizzly.core.service.auth.MongoUserDetailsService;

@Configuration
@EnableAuthorizationServer
public class OAuth2AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	private GrizzlyCoreProperties properties;
	private KeyPair keyPair;

	private JwtProperties props;
	
	public OAuth2AuthorizationServerConfig(GrizzlyCoreProperties properties, JwtProperties props) {
		this.properties = properties;
		this.props = props;
		this.keyPair = props.getKeyPair(this.props.getPrivatekey(), this.props.getPublickey());
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		tokenConverter.setKeyPair(this.keyPair);
		return tokenConverter;
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	@Configuration
	@EnableAuthorizationServer
	protected static class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

		@Autowired
		private MongoUserDetailsService userDetailsService;

		private final AuthenticationManager authenticationManager;

		private final TokenStore tokenStore;

		@Autowired
		private final GrizzlyCoreProperties properties;

		@Autowired
		private final GrizzlyDockerRuntimeProperties dockerRuntimeProperties;

		private final AccessTokenConverter accessTokenConverter;

		@Autowired
		private PasswordEncoder passwordEncoder;

		private final JwtClamsEnhancer jwtClamsEnhancer;

		public AuthorizationServerConfiguration(
				@Qualifier("authenticationManagerBean") AuthenticationManager authenticationManager,
				TokenStore tokenStore, GrizzlyCoreProperties properties, AccessTokenConverter accessTokenConverter,
				JwtClamsEnhancer jwtClamsEnhancer,
				GrizzlyDockerRuntimeProperties dockerRuntimeProperties) {
			this.dockerRuntimeProperties = dockerRuntimeProperties;
			this.authenticationManager = authenticationManager;
			this.tokenStore = tokenStore;
			this.properties = properties;
			this.accessTokenConverter = accessTokenConverter;
			this.jwtClamsEnhancer = jwtClamsEnhancer;
		}

		@Bean
		protected AuthorizationCodeServices authorizationCodeServices() {
			return new InMemoryAuthorizationCodeServices();
		}

		@Bean
		public ApprovalStore approvalStore() {
			return new InMemoryApprovalStore();
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
			final TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
			tokenEnhancerChain
					.setTokenEnhancers(List.of(this.jwtClamsEnhancer, (JwtAccessTokenConverter) accessTokenConverter,
							new IdTokenEnhancer((JwtAccessTokenConverter) accessTokenConverter)));

			endpoints.authorizationCodeServices(authorizationCodeServices()).approvalStore(approvalStore())
					.tokenStore(tokenStore).tokenEnhancer(tokenEnhancerChain).accessTokenConverter(accessTokenConverter)
					.authenticationManager(authenticationManager).userDetailsService(userDetailsService);
		}

		@Override
		public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
			oauthServer.allowFormAuthenticationForClients();
		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			clients.inMemory().withClient(properties.getOauth2().getClientId())
					.secret(passwordEncoder.encode(properties.getOauth2().getClientSecret()))
					.accessTokenValiditySeconds(properties.getOauth2().getTokenValidity())
					.refreshTokenValiditySeconds(properties.getOauth2().getRefreshTokenValidity())
					.authorizedGrantTypes(properties.getOauth2().getGrantType().split(","))
					.scopes(properties.getOauth2().getScope().split(",")).resourceIds("code_once_rm");
		}

	}
}