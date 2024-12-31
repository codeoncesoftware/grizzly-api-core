package fr.codeonce.grizzly.core.rest.config.security;

import fr.codeonce.grizzly.core.domain.user.UserRepository;
import fr.codeonce.grizzly.core.service.auth.MongoUserDetailsService;
import fr.codeonce.grizzly.core.service.security.APIKeyAuthFilter;
import fr.codeonce.grizzly.core.service.security.JWTAuthenticationFilter;
import fr.codeonce.grizzly.core.service.security.JwtAuthEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JwtAuthEntryPoint authEntryPoint;
    private String principalRequestHeader = "apiKey";
    private MongoUserDetailsService userDetailsService;
    @Autowired
    private UserRepository userRepository;

    public SecurityConfig(MongoUserDetailsService userDetailsService, JwtAuthEntryPoint authEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        APIKeyAuthFilter filter = new APIKeyAuthFilter(principalRequestHeader);
        filter.setAuthenticationManager(new AuthenticationManager() {

            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String principal = (String) authentication.getPrincipal();

                if (!userRepository.existsByApiKeyIgnoreCase(principal)) {
                    throw new BadCredentialsException("The API key was not found or not the expected value.");
                }
                authentication.setAuthenticated(true);
                return authentication;
            }
        });

        http.csrf(csrf -> csrf.disable()).exceptionHandling(handling -> handling.authenticationEntryPoint(authEntryPoint)).sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(requests -> requests
                .requestMatchers("/swagger-ui/**").permitAll().requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll().requestMatchers("/api/auth/**").permitAll()//
                .requestMatchers("/actuator/**").permitAll()//
                .requestMatchers("/api/auth/logout").permitAll()//
                .requestMatchers("/api/auth/checknewsletter/{userEmail}").permitAll()
                .requestMatchers("/api/auth/send/reset/password/{userEmail}").permitAll()
                .requestMatchers("/api/check/user/{email}").permitAll()//
                .requestMatchers("/api/resource/public/**").permitAll()//
                .requestMatchers("/api/resource/exists").permitAll()//
                .requestMatchers("/api/dbsource/public/**").permitAll()//
                .requestMatchers("/api/identityprovider/public/**").permitAll()//
                .requestMatchers("/api/container/public/**").permitAll()//
                .requestMatchers("/api/project/public/**").permitAll()//
                .requestMatchers("/api/invoice/test").permitAll()//
                .requestMatchers("/api/swagger/**").permitAll()//
                .requestMatchers("/github/login").permitAll()
                .requestMatchers("/google/login").permitAll().requestMatchers("/api/analytics/healthCheck").permitAll()
                .requestMatchers("/api/**").authenticated()).httpBasic(withDefaults());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilter(filter);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter();
    }
}
