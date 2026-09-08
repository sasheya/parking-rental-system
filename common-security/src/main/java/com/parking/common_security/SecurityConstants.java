package com.parking.common_security;

public class SecurityConstants {
    public static final String HEADER_STRING = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "userId";

    private SecurityConstants() {
        // Utility class
    }
}
