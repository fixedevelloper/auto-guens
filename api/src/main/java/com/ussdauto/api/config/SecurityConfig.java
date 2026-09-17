package com.ussdauto.api.config;

import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.security.ApiKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DeviceRepository deviceRepository;

    @Value("${ussd-automation.security.merchant-api-key:}")
    private String merchantApiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/transactions/*/status").hasRole("DEVICE")
                        .requestMatchers(HttpMethod.PATCH, "/api/devices/*").hasRole("DEVICE")
                        .requestMatchers(HttpMethod.GET, "/api/devices/*/sim-slots").hasAnyRole("MERCHANT", "DEVICE")
                        .requestMatchers("/api/**").hasRole("MERCHANT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new ApiKeyAuthFilter(deviceRepository, merchantApiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
