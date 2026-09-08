package com.parking.auth_service.service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.parking.auth_service.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.private-key:#{null}}")
    private String privateKeyStr;

    @Value("${jwt.public-key:#{null}}")
    private String publicKeyStr;

    @Value("${jwt.access-expiration-ms:86400000}") // 24 hours default
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}") // 7 days default
    private long refreshExpirationMs;

    @Getter
    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    @PostConstruct
    public void initKeys() {
        try {
            if (privateKeyStr != null && !privateKeyStr.isBlank() && publicKeyStr != null && !publicKeyStr.isBlank()) {
                byte[] privBytes = Base64.getDecoder().decode(privateKeyStr.trim());
                byte[] pubBytes = Base64.getDecoder().decode(publicKeyStr.trim());

                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
                log.info("Successfully loaded RS256 key pair from configuration.");
            } else {
                log.info("No RS256 key pair configured. Generating dynamic RSA key pair...");
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                this.privateKey = keyPair.getPrivate();
                this.publicKey = keyPair.getPublic();
            }
        } catch (Exception e) {
            log.error("Failed to initialize RS256 keys, generating fallback RSA key pair.", e);
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                this.privateKey = keyPair.getPrivate();
                this.publicKey = keyPair.getPublic();
            } catch (Exception ex) {
                throw new RuntimeException("Could not initialize RSA KeyPair", ex);
            }
        }
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateAndParseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(validateAndParseToken(token).getSubject());
    }

    public String getRoleFromToken(String token) {
        return validateAndParseToken(token).get("role", String.class);
    }
}
