package com.ecommerce.ecommerce_backend.config;

import com.ecommerce.ecommerce_backend.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/public/**",
            "/api/health",
            "/api/webhook/**",
            "/api/payway/**",

            "/uploads/**",
            "/static/**",
            "/webjars/**",

            // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",

            // Spring error endpoint
            "/error"
    };

    private static final String[] ADMIN_URLS = {
            "/api/admin/**",
            "/api/users/**",
            "/api/orders/admin/**",
            "/api/payments/refund/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // CORS is loaded from CorsConfig.corsConfigurationSource()
                .cors(cors -> cors.disable())

                // JWT APIs normally do not use CSRF tokens
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                                // Browser preflight requests
                                .requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()

                                // Public endpoints
                                .requestMatchers(PUBLIC_URLS)
                                .permitAll()

                                // Public product/category GET requests
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/products/**",
                                        "/api/products",
                                        "/api/categories"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/categories/**"
                                )
                                .permitAll()

                                // Admin routes must come before general /api/orders/** rules
//                                .requestMatchers(ADMIN_URLS)
//                                .hasRole("ADMIN")
                                // 👈 ALLOW USERS TO ACCESS THEIR OWN PROFILE STATS
                                .requestMatchers("/api/users/me/**").authenticated()
                                .requestMatchers("/api/users/me").authenticated()

                                // 👈 Admin routes must come AFTER /me routes
                                .requestMatchers(ADMIN_URLS).hasRole("ADMIN")

                                // Admin product operations
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/products/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/products/**"
                                )
                                .hasRole("ADMIN")

                                // Admin category operations
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/categories/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/categories/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/categories/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/categories/**"
                                )
                                .hasRole("ADMIN")

                                // Authenticated customer routes
                                .requestMatchers("/api/orders/**")
                                .authenticated()

                                .requestMatchers("/api/cart/**")
                                .authenticated()

                                .requestMatchers("/api/payments/**")
                                .authenticated()

                                .requestMatchers("/api/users/me")
                                .authenticated()

                                // Any remaining endpoint requires login
                                .anyRequest()
                                .permitAll()
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"success\":false," +
                                                    "\"message\":\"Unauthorized\"," +
                                                    "\"statusCode\":401}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.setCharacterEncoding("UTF-8");

                                    response.getWriter().write(
                                            "{\"success\":false," +
                                                    "\"message\":\"Forbidden\"," +
                                                    "\"statusCode\":403}"
                                    );
                                }
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .addLogoutHandler(logoutHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler handler =
                new SimpleUrlLogoutSuccessHandler();

        handler.setDefaultTargetUrl("/api/auth/logout");
        handler.setAlwaysUseDefaultTargetUrl(true);

        return handler;
    }

    @Bean
    public LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            // Add custom JWT invalidation logic here if required.
        };
    }
}
