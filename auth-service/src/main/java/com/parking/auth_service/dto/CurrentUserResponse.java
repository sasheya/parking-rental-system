package com.parking.auth_service.dto;

import com.parking.auth_service.model.Role;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrentUserResponse {
    Long userId;
    String email;
    Role role;
}