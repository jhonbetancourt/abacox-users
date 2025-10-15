package com.infomedia.abacox.users.config;

import com.infomedia.abacox.users.component.legacy.Md5PasswordEncoder;
import com.infomedia.abacox.users.constants.PasswordEncodingAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth ->
                        auth.requestMatchers(publicPaths()).permitAll()
                .anyRequest().authenticated())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new UsernameAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public AuthenticationManager noopAuthenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("Authentication is disabled");
        };
    }

    private String[] publicPaths() {
        return new String[] {"/v3/api-docs/**"
                , "/swagger-ui/**"
                , "/swagger-ui.html"
                , "/error"
                , "/api/auth/**"
                , "/api/module/**"
                , "/api/health/**"
                , "/websocket/module"};
    }

    public boolean isPublicPath(String path) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (String url : publicPaths()) {
            if (matcher.match(url, path)) {
                return true;
            }
        }
        return false;
    }

    public class UsernameAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            String headerUsername = request.getHeader("X-Username");
            String username = "anonymousUser";

            if(headerUsername != null){
                username = headerUsername;
            }

            if (!username.equals("anonymousUser")) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken authToken
                        = new UsernamePasswordAuthenticationToken(username, null, null);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }

            filterChain.doFilter(request, response);
        }
    }

    /**
     * The primary, modern password encoder for creating new hashes.
     * This bean is named "bcryptPasswordEncoder" by default because of the method name.
     */
    @Bean
    @Primary
    public PasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * A password encoder for validating legacy MD5 hashes during migration.
     * This bean is named "md5PasswordEncoder" by default because of the method name.
     */
    @Bean
    public PasswordEncoder md5PasswordEncoder() {
        return new Md5PasswordEncoder();
    }

    /**
     * A map of all available password encoders, keyed by our enum.
     * This allows for a clean, delegating strategy in the authentication service.
     * We use @Qualifier to tell Spring exactly which bean to inject for each parameter.
     */
    @Bean
    public Map<PasswordEncodingAlgorithm, PasswordEncoder> passwordEncoders(
            @Qualifier("bcryptPasswordEncoder") PasswordEncoder bcryptEncoder,
            @Qualifier("md5PasswordEncoder") PasswordEncoder md5Encoder
    ) {
        return Map.of(
                PasswordEncodingAlgorithm.BCRYPT, bcryptEncoder,
                PasswordEncodingAlgorithm.MD5, md5Encoder
        );
    }
}