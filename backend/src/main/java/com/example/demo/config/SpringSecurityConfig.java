package com.example.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.demo.model.User;
import com.example.demo.service.OAuth2UserService;
import com.example.demo.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserService oAuth2UserService;
    private final JwtUtil jwtUtil;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5500"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/public/**", "/login/**", "/oauth2/**", "/error",
                            "/ws/**", "/api/test/**").permitAll();
                    auth.requestMatchers("/api/user/**").authenticated();
                    auth.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))

                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication
                                    .getPrincipal();

                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");
                            String picture = oAuth2User.getAttribute("picture"); // 獲取 Google 提供的真實大頭貼

                            User loggedInUser = oAuth2UserService.processPostLogin(email, name, picture);
                            if (!Boolean.TRUE.equals(loggedInUser.getIsActive())) {
                            response.sendRedirect("http://localhost:5173/verify?email=" + email);
                            return;
}
                            String token = jwtUtil.generateToken(loggedInUser.getUserId(), loggedInUser.getRole());

                            ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
                                    .httpOnly(true)
                                    .secure(false)
                                    .path("/")
                                    .maxAge(24 * 60 * 60)
                                    .sameSite("Lax")
                                    .build();

                            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
                            response.sendRedirect("http://localhost:5173/loginView?oauth2=success");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}