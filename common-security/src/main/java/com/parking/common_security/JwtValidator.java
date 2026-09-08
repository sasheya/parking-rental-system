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
		byte[] keyBytes = Base64.getDecoder().decode(props.getPublicKey());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
	}
	
	public Claims validateAndParse(String token) {
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
