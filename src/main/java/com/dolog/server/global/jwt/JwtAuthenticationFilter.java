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

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 인증 제외 경로 (exact match)
    private static final Set<String> EXCLUDE_URLS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/signup"
    );

    // 인증 제외 경로 (패턴 match: method -> patterns)
    private static final Map<String, List<String>> EXCLUDE_PATTERNS = Map.of(
            "GET", List.of(
                    "/api/exhibitions/*/zones",
                    "/api/exhibitions/*/partners"
            )
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        try {
            // 인증 제외 경로는 패스 (exact)
            if (EXCLUDE_URLS.contains(uri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 인증 제외 경로는 패스 (pattern)
            String method = request.getMethod();
            List<String> patterns = EXCLUDE_PATTERNS.getOrDefault(method, List.of());
            if (patterns.stream().anyMatch(p -> pathMatcher.match(p, uri))) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);

            // 토큰 없으면 401 처리
            if (token == null) {
                throw new RuntimeException("JWT 토큰이 존재하지 않습니다.");
            }

            // 토큰 유효성 검증
            jwtTokenProvider.validateToken(token);

            // 토큰에서 이메일 추출
            String email = jwtTokenProvider.getNicknameFromToken(token); // nickname 대신 email 사용 가능

            // UserDetails 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // SecurityContext 세팅
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
            return; // 필터 체인 종료
        }

        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}