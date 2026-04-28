package com.dolog.server.global.jwt;

import com.dolog.server.global.exception.BaseException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JWT 인증 필터
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    // 인증 제외 경로 (exact match - POST나 다른 메서드임에도 제외해야 하는 경우)
    private static final Set<String> EXCLUDE_URLS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/signup"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. 모든 GET 요청은 필터를 거치지 않음
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. POST 등 다른 메서드 중 예외 경로 체크
        return EXCLUDE_URLS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // shouldNotFilter에서 true를 반환하면 이미 이 메서드 자체가 실행되지 않지만,
        // 안전을 위해 내부 로직도 간소화합니다.
        try {
            String token = resolveToken(request);

            if (token == null) {
                throw new RuntimeException("JWT 토큰이 존재하지 않습니다.");
            }

            jwtTokenProvider.validateToken(token);
            String email = jwtTokenProvider.getNicknameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (BaseException e) {
            request.setAttribute("jwt_exception", e);
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(e.getMessage(), e)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}