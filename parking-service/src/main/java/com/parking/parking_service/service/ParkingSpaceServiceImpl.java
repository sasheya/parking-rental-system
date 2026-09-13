package com.parking.parking_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.parking_service.dto.ParkingSearchRequest;
import com.parking.parking_service.dto.ParkingSpaceDTO;
import com.parking.parking_service.model.ParkingSpace;
import com.parking.parking_service.repository.ParkingSpaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpaceServiceImpl implements ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceDTO> getAllActiveSpaces() {
        return parkingSpaceRepository.findByActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSpaceDTO getSpaceById(Long id) {
        ParkingSpace space = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking space not found with ID: " + id));
        return mapToDTO(space);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceDTO> searchSpaces(ParkingSearchRequest request) {
        if (request.getLatitude() != null && request.getLongitude() != null) {
            double radius = request.getRadiusKm() != null ? request.getRadiusKm() : 10.0; // default 10km
            return parkingSpaceRepository.findWithinRadius(request.getLatitude(), request.getLongitude(), radius).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else if (request.getCity() != null && !request.getCity().isBlank()) {
            return parkingSpaceRepository.findByCityContainingIgnoreCaseAndActiveTrue(request.getCity().trim()).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else {
            return getAllActiveSpaces();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceDTO> getListingsByOwnerId(Long ownerId) {
        return parkingSpaceRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParkingSpaceDTO createSpace(Long ownerId, ParkingSpaceDTO dto) {
        ParkingSpace space = ParkingSpace.builder()
                .ownerId(ownerId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .city(dto.getCity())
                .zipCode(dto.getZipCode())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .pricePerHour(dto.getPricePerHour())
                .totalSlots(dto.getTotalSlots())
                .availableSlots(dto.getTotalSlots())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        space = parkingSpaceRepository.save(space);
        log.info("Created parking space ID: {} for owner ID: {}", space.getId(), ownerId);
        return mapToDTO(space);
    }

    @Override
    @Transactional
    public ParkingSpaceDTO updateSpace(Long ownerId, Long spaceId, ParkingSpaceDTO dto) {
        ParkingSpace space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Parking space not found with ID: " + spaceId));

        if (!space.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Unauthorized: Parking space does not belong to owner ID: " + ownerId);
        }

        if (dto.getTitle() != null) space.setTitle(dto.getTitle());
        if (dto.getDescription() != null) space.setDescription(dto.getDescription());
        if (dto.getAddress() != null) space.setAddress(dto.getAddress());
        if (dto.getCity() != null) space.setCity(dto.getCity());
        if (dto.getZipCode() != null) space.setZipCode(dto.getZipCode());
        if (dto.getLatitude() != null) space.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) space.setLongitude(dto.getLongitude());
        if (dto.getPricePerHour() != null) space.setPricePerHour(dto.getPricePerHour());
        if (dto.getTotalSlots() != null) {
            space.setTotalSlots(dto.getTotalSlots());
            if (space.getAvailableSlots() > dto.getTotalSlots()) {
                space.setAvailableSlots(dto.getTotalSlots());
            }
        }
        if (dto.getActive() != null) space.setActive(dto.getActive());

        space = parkingSpaceRepository.save(space);
        log.info("Updated parking space ID: {} for owner ID: {}", spaceId, ownerId);
        return mapToDTO(space);
    }

    @Override
    @Transactional
    public void deleteSpace(Long ownerId, Long spaceId) {
        ParkingSpace space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Parking space not found with ID: " + spaceId));

        if (!space.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Unauthorized: Parking space does not belong to owner ID: " + ownerId);
        }

        parkingSpaceRepository.delete(space);
        log.info("Deleted parking space ID: {} for owner ID: {}", spaceId, ownerId);
    }

    private ParkingSpaceDTO mapToDTO(ParkingSpace space) {
        return ParkingSpaceDTO.builder()
                .id(space.getId())
                .ownerId(space.getOwnerId())
                .title(space.getTitle())
                .description(space.getDescription())
                .address(space.getAddress())
                .city(space.getCity())
                .zipCode(space.getZipCode())
                .latitude(space.getLatitude())
                .longitude(space.getLongitude())
                .pricePerHour(space.getPricePerHour())
                .totalSlots(space.getTotalSlots())
                .availableSlots(space.getAvailableSlots())
                .active(space.getActive())
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .build();
    }
}
