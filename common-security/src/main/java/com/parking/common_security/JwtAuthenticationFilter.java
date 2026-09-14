package com.parking.common_security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || (path.startsWith("/api/auth/") && !path.equals("/api/auth/me"))
                || path.equals("/api/auth")            
                // Swagger/OpenAPI
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                // actuator               
                || path.startsWith("/actuator/")
                || path.equals("/actuator")
                || isPublicParkingRead(request);
    }

    private boolean isPublicParkingRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.equals("/api/parking")
                || path.equals("/api/parking/search")
                || path.matches("/api/parking/\\d+")
                || path.matches("/api/parking/\\d+/(slots|availability)");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.HEADER_STRING);

        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
            try {
                Claims claims = jwtValidator.validateAndParse(token);
                if ("refresh".equals(claims.get("type", String.class))) {
                    throw new IllegalArgumentException("Refresh tokens cannot authenticate API requests");
                }

                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String authority = (role != null && !role.startsWith("ROLE_")) ? "ROLE_" + role : role;
                    SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority != null ? authority : "ROLE_USER");

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.singletonList(grantedAuthority)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
