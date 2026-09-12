package com.dolog.server.global.config;

import com.dolog.server.global.jwt.*;
import com.dolog.server.global.response.ErrorResponse;
import com.dolog.server.global.response.code.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보안 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json; charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(),
                                    ErrorResponse.of(GlobalErrorCode.ACCESS_DENIED_REQUEST));
                        }))
                // HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 헬스체크(관리 포트 9090)는 인증 없이 허용 — CD 폴링·외부 모니터링용
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        // HealthEndpoint 매처는 GET만 매칭 → HEAD(무료 모니터 기본 메서드) 별도 허용
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/exhibitions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/artworks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/artist-profiles", "/artist-profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/artists", "/artists/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh",
                                "/auth/social/login", "/auth/exhibition/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )

                // jwt 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, jwtAuthenticationEntryPoint),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
