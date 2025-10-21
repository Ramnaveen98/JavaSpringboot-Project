package com.autobridge_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // --- Stateless JWT API & CORS/CSRF ---
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // --- 401 vs 403 handling ---
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 401
                        .accessDeniedHandler((req, res, e) -> res.setStatus(HttpStatus.FORBIDDEN.value())) // 403
                )

                // --- Authorization: specific → general ---
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger / API docs
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Root/health/error
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/v1/health/**"
                        ).permitAll()

                        // Auth (signup/login)
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Static uploads (images saved by admin)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // Public catalog
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/services/public",
                                "/api/v1/services/public/**"
                        ).permitAll()

                        // Public vehicles list
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/vehicles/public",
                                "/api/v1/vehicles/public/**"
                        ).permitAll()

                        // New public endpoints under /api/v1/public (incl. image proxy)
                        .requestMatchers("/api/v1/public/image-proxy").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()

                        // ---------- Agent requests (dashboard + actions) ----------
                        // IMPORTANT: allow BOTH /agent/requests and /agent/requests/mine
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/agent/requests",
                                "/api/v1/agent/requests/mine"
                        ).hasAnyRole("AGENT","ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/agent/requests/*/start").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/agent/requests/*/complete").hasRole("AGENT")

                        // ---------- Requests lifecycle (user) ----------
                        .requestMatchers(HttpMethod.POST, "/api/v1/requests").hasAnyRole("USER","ADMIN")
                        // Let AGENT hit this too so the User dashboard doesn't 403 when an agent opens it.
                        .requestMatchers(HttpMethod.GET, "/api/v1/requests/mine").hasAnyRole("USER","ADMIN","AGENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/requests/*").authenticated()

                        // Feedback
                        .requestMatchers(HttpMethod.POST,  "/api/v1/requests/*/feedback").hasRole("USER")
                        .requestMatchers(HttpMethod.GET,   "/api/v1/feedback").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/feedback/*/acknowledge").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/v1/agent/feedback").hasRole("AGENT")

                        // --- Admin analytics/search/export (OLD + NEW paths) ---
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/requests/admin",
                                "/api/v1/requests/admin/export"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/admin/requests",
                                "/api/v1/admin/requests/export"
                        ).hasRole("ADMIN")

                        // --- Staff actions on requests (assign/start/complete) (OLD + NEW paths) ---
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/requests/*/assign",
                                "/api/v1/requests/*/start",
                                "/api/v1/requests/*/complete"
                        ).hasAnyRole("ADMIN","AGENT")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/admin/requests/*/assign",
                                "/api/v1/admin/requests/*/start",
                                "/api/v1/admin/requests/*/complete"
                        ).hasAnyRole("ADMIN","AGENT")

                      //  .requestMatchers("/api/v1/auth/password/forgot", "/api/v1/auth/password/verify", "/api/v1/auth/password/reset")
                      //  .permitAll()


                        // Cancel allowed for admin/agent/user
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/requests/*/cancel",
                                "/api/v1/admin/requests/*/cancel"
                        ).hasAnyRole("ADMIN","AGENT","USER")

                        // Admin directory (agents + users CRUD)
                        .requestMatchers("/api/v1/admin/directory/**").hasRole("ADMIN")

                        // Admin VEHICLES CRUD
                        .requestMatchers("/api/v1/admin/vehicles/**").hasRole("ADMIN")

                        // Legacy agents management (if still present)
                        .requestMatchers("/api/v1/agents/**").hasRole("ADMIN")

                        // Catch-all
                        .anyRequest().authenticated()
                )

                // --- JWT filter ---
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://localhost:4200"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","X-Requested-With"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
