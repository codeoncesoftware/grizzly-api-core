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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

import fr.codeonce.grizzly.core.domain.user.UserRepository;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {
	
    private String principalRequestHeader="apiKey";
    
    @Autowired
    private UserRepository userRepository;


	public OAuth2ResourceServerConfig(TokenStore tokenStore) {
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		  APIKeyAuthFilter filter = new APIKeyAuthFilter(principalRequestHeader);
	        filter.setAuthenticationManager(new AuthenticationManager() {

	            @Override
	            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	                String principal = (String) authentication.getPrincipal();
	                
	                if (!userRepository.existsByApiKeyIgnoreCase(principal))
	                {
	                    throw new BadCredentialsException("The API key was not found or not the expected value.");
	                }
	                authentication.setAuthenticated(true);
	                return authentication;
	            }
	        });
		http.exceptionHandling().and().csrf().disable().headers().frameOptions().disable().and().httpBasic().disable()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)//
				.and().authorizeRequests()
				// PUBLIC API
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()//
				.antMatchers("/api/auth/checknewsletter/{userEmail}").permitAll()
				.antMatchers("/api/auth/send/reset/password/{userEmail}").permitAll()
				.antMatchers("/api/auth/**").permitAll()//
				.antMatchers("/api/check/user/{email}").permitAll()//
				.antMatchers("/swagger-ui.html").permitAll()//
				.antMatchers("/api/resource/public/**").permitAll()//
				.antMatchers("/api/resource/exists").permitAll()//
				.antMatchers("/api/dbsource/public/**").permitAll()//
				.antMatchers("/api/identityprovider/public/**").permitAll()//
				.antMatchers("/api/container/public/**").permitAll()//
				.antMatchers("/api/project/public/**").permitAll()//
				.antMatchers("/api/invoice/test").permitAll()//
				.antMatchers("/api/swagger/**").permitAll()//
				.antMatchers("/github/login").permitAll()
				.antMatchers("/api/analytics/healthCheck").permitAll()
	            .antMatchers("/api/**").authenticated().and().
				addFilter(filter).authorizeRequests().
	            antMatchers("/api/**").authenticated();
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId("code_once_rm");
	}
}