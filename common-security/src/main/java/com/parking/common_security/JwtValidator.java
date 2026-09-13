package com.parking.common_security;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class JwtValidator {
	private final PublicKey publicKey;
	
	public JwtValidator(JwtProperties props) throws Exception {
		if (props.getPublicKey() == null || props.getPublicKey().isBlank()) {
			this.publicKey = null;
			return;
		}

		byte[] keyBytes = Base64.getDecoder().decode(props.getPublicKey().trim());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
	}
	
	public Claims validateAndParse(String token) {
		if (publicKey == null) {
			throw new IllegalStateException("JWT_PUBLIC_KEY is not configured");
		}

		return Jwts.parser()
				.verifyWith(publicKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();		
	}
	
	public Long getUserId(String token) {
		return Long.parseLong(validateAndParse(token).getSubject());
	}
	
	public String getRole(String token) {
		return validateAndParse(token).get("role", String.class);
	}
}
