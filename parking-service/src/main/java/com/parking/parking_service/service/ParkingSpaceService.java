package com.parking.parking_service.service;

import java.util.List;

import com.parking.parking_service.dto.ParkingSearchRequest;
import com.parking.parking_service.dto.ParkingSpaceDTO;

public interface ParkingSpaceService {
    List<ParkingSpaceDTO> getAllActiveSpaces();
    ParkingSpaceDTO getSpaceById(Long id);
    List<ParkingSpaceDTO> searchSpaces(ParkingSearchRequest request);
    List<ParkingSpaceDTO> getListingsByOwnerId(Long ownerId);
    ParkingSpaceDTO createSpace(Long ownerId, ParkingSpaceDTO dto);
    ParkingSpaceDTO updateSpace(Long ownerId, Long spaceId, ParkingSpaceDTO dto);
    void deleteSpace(Long ownerId, Long spaceId);
}
