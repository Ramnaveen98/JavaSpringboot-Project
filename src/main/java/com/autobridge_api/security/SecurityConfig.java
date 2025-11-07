package com.autobridge_api.security;

import org.springframework.beans.factory.annotation.Value;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // ==== CORS inputs (wired from application*.properties or Cloud Run env) ====
    // Exact origins (comma-separated). Example:
    // APP_CORS_ALLOWED_ORIGINS="https://autobridge-frontend-XXXX.us-central1.run.app"
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174}")
    private String allowedOriginsProp;

    // Wildcard patterns (comma-separated). Defaults include Cloud Run *.a.run.app
    // Example: APP_CORS_ALLOWED_ORIGIN_PATTERNS="https://*.a.run.app"
    @Value("${app.cors.allowed-origin-patterns:https://*.a.run.app}")
    private String allowedOriginPatternsProp;

    // Cookies across origins? Keep false unless you truly need them.
    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ---- Stateless JWT API ----
                .csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ---- 401/403 behavior ----
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 401 for unauthenticated
                        .accessDeniedHandler((req, res, e) -> res.setStatus(HttpStatus.FORBIDDEN.value())) // 403 for forbidden
                )

                // ---- Authorization rules (specific → general) ----
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger / API docs
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Health/ping/root (your custom health; Actuator remains protected)
                        .requestMatchers("/", "/error", "/api/v1/ping", "/api/v1/health/**").permitAll()

                        // Auth (login/signup + reset)
                        .requestMatchers("/api/v1/auth/**",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/reset-password").permitAll()

                        // Static uploads (vehicle images served by Spring from /uploads/**)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // Public services
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/services/public",
                                "/api/v1/services/public/**"
                        ).permitAll()

                        // Public vehicles (current + legacy paths)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/vehicles/public",
                                "/api/v1/vehicles/public/**",
                                "/api/v1/public/vehicles",
                                "/api/v1/public/vehicles/**"
                        ).permitAll()

                        // Other public helpers
                        .requestMatchers("/api/v1/public/**").permitAll()

                        // ---------------- Agent ----------------
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/agent/requests",
                                "/api/v1/agent/requests/mine"
                        ).hasAnyRole("AGENT","ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agent/requests/*/start").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agent/requests/*/complete").hasRole("AGENT")

                        // -------- Requests lifecycle (user) --------
                        .requestMatchers(HttpMethod.POST, "/api/v1/requests").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/requests/mine").hasAnyRole("USER","ADMIN","AGENT")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/requests/*").authenticated()

                        // Feedback
                        .requestMatchers(HttpMethod.POST,  "/api/v1/requests/*/feedback").hasRole("USER")
                        .requestMatchers(HttpMethod.GET,   "/api/v1/feedback").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/feedback/*/acknowledge").hasRole("ADMIN")

                        // ----- Admin analytics/search/export (old + new) -----
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/requests/admin",
                                "/api/v1/requests/admin/export"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/admin/requests",
                                "/api/v1/admin/requests/export"
                        ).hasRole("ADMIN")

                        // ----- Staff actions on requests (assign/start/complete) (old + new) -----
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

                        // Cancel (admin/agent/user) (old + new)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/requests/*/cancel",
                                "/api/v1/admin/requests/*/cancel"
                        ).hasAnyRole("ADMIN","AGENT","USER")

                        // Admin directory & vehicles CRUD
                        .requestMatchers("/api/v1/admin/directory/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/vehicles/**").hasRole("ADMIN")

                        // Legacy agents mgmt (if still present)
                        .requestMatchers("/api/v1/agents/**").hasRole("ADMIN")

                        // Catch-all
                        .anyRequest().authenticated()
                )

                // ---- JWT filter ----
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ==== Beans: encoder, auth manager, CORS source ====

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * Central CORS configuration (used by Spring Security via http.cors()).
     * Values come from:
     *   - app.cors.allowed-origins           (exact origins)
     *   - app.cors.allowed-origin-patterns   (wildcards like https://*.a.run.app)
     *   - app.cors.allow-credentials         (default false)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Exact origins
        List<String> exactOrigins = splitCsv(allowedOriginsProp);
        if (!exactOrigins.isEmpty()) {
            cfg.setAllowedOrigins(exactOrigins);
        }

        // Patterns (add localhost wildcards for dev convenience)
        List<String> patterns = new ArrayList<>(splitCsv(allowedOriginPatternsProp));
        if (!patterns.contains("http://localhost:*")) patterns.add("http://localhost:*");
        if (!patterns.contains("http://127.0.0.1:*")) patterns.add("http://127.0.0.1:*");
        cfg.setAllowedOriginPatterns(patterns);

        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization","Content-Type","Accept","Origin","X-Requested-With",
                "Cache-Control","Pragma"
        ));
        cfg.setExposedHeaders(List.of("Authorization","Content-Disposition","Location"));
        cfg.setAllowCredentials(allowCredentials);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
