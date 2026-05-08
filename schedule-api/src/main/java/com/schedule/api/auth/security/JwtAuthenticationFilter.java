package com.schedule.api.auth.security;


import com.schedule.api.auth.exception.AuthErrorCode;
import com.schedule.api.auth.config.AuthProperties;
import com.schedule.api.common.exception.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthProperties authProperties;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthProperties authProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateWithJwt(request);
            authenticateWithDevHeaders(request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateWithJwt(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            AuthenticatedUser user = jwtTokenProvider.parseAccessToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, java.util.List.of());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_EXPIRED, "액세스 토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(AuthErrorCode.AUTH_INVALID_TOKEN, "유효하지 않은 액세스 토큰입니다.");
        }
    }

    private void authenticateWithDevHeaders(HttpServletRequest request) {
        if (!authProperties.isDevHeaderEnabled()) {
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String groupId = request.getHeader("X-Group-Id");

        if (userId == null || userId.isBlank() || groupId == null || groupId.isBlank()) {
            return;
        }

        AuthenticatedUser user = new AuthenticatedUser(userId, groupId, userId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
