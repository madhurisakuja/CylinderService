package com.cylindertrack.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.allowed-emails}")
    private String allowedEmailsRaw;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Force HTTPS in production
            .requiresChannel(channel -> channel
                .requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                .requiresSecure()
            )
            // Security headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;")
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .frameOptions(frame -> frame.deny())
            )
            // CSRF enabled by default — only exclude stateless endpoints if needed
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/images/**", "/login**", "/error**", "/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .defaultSuccessUrl("/newhome", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                )
                .failureUrl("/login?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return request -> {
            OidcUser user = delegate.loadUser(request);
            String email = user.getEmail();

            List<String> allowed = Arrays.stream(allowedEmailsRaw.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isBlank())
                    .toList();

            if (email == null || !allowed.contains(email.toLowerCase())) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "Access denied for: " + email
                );
            }

            return user;
        };
    }
}
