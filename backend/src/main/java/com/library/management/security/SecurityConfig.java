package com.library.management.security;

import com.library.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/librarian").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/activate", "/api/v1/users/*/deactivate").hasRole("ADMIN")
                        // Book & Category reads
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**", "/api/v1/categories/**").permitAll()
                        // Reservations
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservations/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/my-reservations", "/api/v1/reservations/pending").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reservations/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/book/**").hasAnyRole("LIBRARIAN", "ADMIN")
                        // Loans
                        .requestMatchers(HttpMethod.POST, "/api/v1/loans/checkout").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/loans/my-loans", "/api/v1/loans/my-active-loans").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/loans/active", "/api/v1/loans/overdue", "/api/v1/loans/reservation/**").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/loans/return").hasAnyRole("LIBRARIAN", "ADMIN")
                        // Fines
                        .requestMatchers(HttpMethod.GET, "/api/v1/fines/my-fines", "/api/v1/fines/my-fines/pending").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/fines/pending").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/fines/pay").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/fines/waive").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/fines/**").authenticated()
                        .requestMatchers("/api/v1/users/profile").authenticated()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value(),
                "You don't have permission to access this resource");
    }
}

