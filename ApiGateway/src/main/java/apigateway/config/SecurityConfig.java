package apigateway.config;

import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private final CookieConfig cookieConfig;

    public SecurityConfig(CookieConfig cookieConfig) {
        this.cookieConfig = cookieConfig;
    }



    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/admin/**"
                        )
                        .access((authentication, context) -> new AuthorizationDecision(
                                hasRole(authentication.get(), Roles.ADMIN) &&
                                        isActive(authentication.get())
                        ))

                        .requestMatchers(
                                "/auth/manager/**",
                                "/user/manager/**",
                                "/account/manager/**"
                        )
                        .access((authentication, context) -> new AuthorizationDecision(
                                hasRole(authentication.get(), Roles.ADMIN) || (hasRole(authentication.get(), Roles.MANAGER) && isActive(authentication.get()))
                        ))

                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/auth/verify-user",
                                "/*/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        )
                        .permitAll()

                        .anyRequest()
                        .access((authentication, context) -> {
                            return new AuthorizationDecision(isActive(authentication.get()) || hasRole(authentication.get(), Roles.ADMIN));
                        })
                )
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(request ->cookieConfig.getCookieByKey(request, "at"))
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }

    private boolean isActive(Authentication authentication) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        String status = jwt.getClaimAsString("status");

        return AuthUserStatus.ACTIVE.name().equals(status);
    }

    private boolean hasAnyRole(Authentication authentication, Roles... roles) {
        for (Roles role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRole(Authentication authentication, Roles role) {
        String roleAuthority = "ROLE_" + role.name();

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> roleAuthority.equals(authority.getAuthority()));
    }
}
