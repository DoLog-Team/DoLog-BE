package com.dolog.server.global.jwt;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/social/login",
            "/auth/exhibition/login");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod())
                || ("POST".equals(request.getMethod()) && PUBLIC_AUTH_PATHS.contains(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String bearer = request.getHeader("Authorization");
            // 토큰이 없으면 공개/보호 경로 판정은 SecurityConfig에 맡긴다.
            if (bearer != null) {
                if (!bearer.startsWith("Bearer ")) throw new JwtInvalidException();
                var identity = jwtTokenProvider.parseAccessToken(bearer.substring(7));
                var user = userDetailsService.loadUser(identity.accountId(), identity.sessionId());
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (BaseException e) {
            request.setAttribute("jwt_exception", e);
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException(e.getErrorCode().getMessage(), e));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
