package com.parking.user_service.service;

import com.parking.user_service.dto.UserProfileDTO;

public interface UserService {
    UserProfileDTO getProfileByUserId(Long userId);
    UserProfileDTO createOrUpdateProfile(Long userId, UserProfileDTO dto);
}
