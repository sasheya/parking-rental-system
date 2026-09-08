package com.parking.user_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.user_service.dto.UserProfileDTO;
import com.parking.user_service.model.UserProfile;
import com.parking.user_service.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfileByUserId(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for user ID: " + userId));
        return mapToDTO(profile);
    }

    @Override
    @Transactional
    public UserProfileDTO createOrUpdateProfile(Long userId, UserProfileDTO dto) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .email(dto.getEmail())
                        .build());

        if (dto.getEmail() != null) {
            profile.setEmail(dto.getEmail());
        }
        if (dto.getFullName() != null) {
            profile.setFullName(dto.getFullName());
        }
        if (dto.getPhoneNumber() != null) {
            profile.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAddress() != null) {
            profile.setAddress(dto.getAddress());
        }
        if (dto.getAvatarUrl() != null) {
            profile.setAvatarUrl(dto.getAvatarUrl());
        }

        profile = userProfileRepository.save(profile);
        log.info("Saved user profile for user ID: {}", userId);
        return mapToDTO(profile);
    }

    private UserProfileDTO mapToDTO(UserProfile profile) {
        return UserProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .address(profile.getAddress())
                .avatarUrl(profile.getAvatarUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
