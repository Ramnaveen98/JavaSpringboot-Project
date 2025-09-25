package com.autobridge_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/", "/api/v1/health/**").permitAll()


                        .requestMatchers("/api/v1/auth/**").permitAll()


                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/services/**",
                                "/api/v1/inventory/**",
                                "/api/v1/slots/**").permitAll()


                        .requestMatchers(HttpMethod.GET, "/api/v1/requests/*").permitAll()


                        .requestMatchers(HttpMethod.POST, "/api/v1/requests").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/feedback").authenticated()


                        .requestMatchers("/api/v1/requests/export.csv").hasRole("ADMIN")
                        .requestMatchers("/api/v1/agents/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/requests/**").hasAnyRole("ADMIN","AGENT")
                        .requestMatchers("/api/v1/feedback/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
