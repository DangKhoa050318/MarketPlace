package com.training.marketplace.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**").permitAll()
                        // Q&A and Content Moderation
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/products/*/questions").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/questions/*/answers").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/answers/*/official").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/content/vote").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/admin/moderation/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        // Storefront catalog reads are public
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/variants/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/recommendations/**").permitAll()
                        .requestMatchers("/api/v1/recently-viewed", "/api/v1/recently-viewed/**").permitAll()
                        .requestMatchers("/api/v1/analytics/events", "/api/v1/analytics/events/**").permitAll()
                        // Catalog writes: MANAGER/ADMIN (hard delete ADMIN only)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/categories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/categories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/variants/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/variants/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/variants/**").hasRole("ADMIN")
                        // Storefront (customer and back-office staff/manager/admin)
                        .requestMatchers("/api/v1/cart", "/api/v1/cart/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/orders", "/api/v1/orders/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/journey/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        // Coupons: shoppers can preview; management is ADMIN-only (preview matcher first).
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/coupons/preview").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/coupons/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/wishlist", "/api/v1/wishlist/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        // Admin
                        .requestMatchers("/api/v1/admin/dashboard/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/admin/analytics/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/admin/orders/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/admin/reviews/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/reviews/**").hasAnyRole("CUSTOMER", "STAFF", "MANAGER", "ADMIN")
                        // Warehouse back-office
                        .requestMatchers("/api/v1/warehouses/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/stock/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/movements/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/alerts/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/reorder-suggestions/**").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitingFilter, org.springframework.security.web.authentication.logout.LogoutFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
