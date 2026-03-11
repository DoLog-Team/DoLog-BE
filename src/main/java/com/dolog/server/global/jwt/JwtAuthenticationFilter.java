package com.dolog.server.global.jwt;

import com.dolog.server.global.exception.BaseException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JWT 인증 필터
 * - 요청 헤더의 Authorization에서 JWT 토큰 추출
 * - 유효성 검증 후 SecurityContext에 인증 저장
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            // 토큰이 없으면 그냥 다음 필터로 pass
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 유효한 토큰이면 인증 설정
            jwtTokenProvider.validateToken(token); // BaseException 가능

            String nickname = jwtTokenProvider.getNicknameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(nickname);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (BaseException e) { // JwtExpiredException, JwtInvalidException 등
            // 인증이 필요 없는 URL에서는 JWT 오류를 조용히 무시하고 통과시킴
            String uri = request.getRequestURI();
            if (uri.startsWith("/signup") || uri.startsWith("/login") || uri.startsWith("/refresh")) {
                // 로그 안 찍고 통과
                filterChain.doFilter(request, response);
                return;
            }

            // 나머지 URL은 기존처럼 EntryPoint 로 처리
            request.setAttribute("jwt_exception", e);
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(e.getMessage(), e)
            );
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
