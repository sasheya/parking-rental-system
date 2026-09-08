package com.parking.common_security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtValidator jwtValidator;
	
	public JwtAuthenticationFilter(JwtValidator jwtValidator) {
		this.jwtValidator = jwtValidator;
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,  FilterChain filterChain) throws IOException, ServletException {
		String header = request.getHeader("Authorization");
		if(header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			try {
				Long userId = jwtValidator.getUserId(token);
				String role = jwtValidator.getRole(token);
				
				var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE" + role)));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch(Exception e) {
				SecurityContextHolder.clearContext();
			}
		}
		
		filterChain.doFilter(request, response);
	}
	
}
