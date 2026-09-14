package com.parking.user_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.parking.user_service.dto.UserProfileDTO;
import com.parking.user_service.model.UserProfile;
import com.parking.user_service.repository.UserProfileRepository;
import com.parking.user_service.service.UserServiceImpl;

class UserServiceImplTest {

    @Test
    void createOrUpdateProfileCreatesProfileWhenNoneExists() {
        UserProfileRepository repository = mock(UserProfileRepository.class);
        when(repository.findByUserId(12L)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(repository);
        UserProfileDTO result = service.createOrUpdateProfile(12L,
                UserProfileDTO.builder().email("user@example.com").fullName("Test User").build());

        assertEquals(12L, result.getUserId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("Test User", result.getFullName());
    }
}