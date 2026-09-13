package com.parking.user_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.user_service.dto.VehicleDTO;
import com.parking.user_service.model.Vehicle;
import com.parking.user_service.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByUserId(Long userId) {
        return vehicleRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleDTO addVehicle(Long userId, VehicleDTO dto) {
        if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate())) {
            throw new IllegalArgumentException("Vehicle with license plate already exists: " + dto.getLicensePlate());
        }

        Vehicle vehicle = Vehicle.builder()
                .userId(userId)
                .licensePlate(dto.getLicensePlate().toUpperCase().trim())
                .make(dto.getMake())
                .model(dto.getModel())
                .color(dto.getColor())
                .vehicleType(dto.getVehicleType())
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Added vehicle ID: {} for user ID: {}", vehicle.getId(), userId);
        return mapToDTO(vehicle);
    }

    @Override
    @Transactional
    public VehicleDTO updateVehicle(Long userId, Long vehicleId, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (!vehicle.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: Vehicle does not belong to user ID: " + userId);
        }

        if (dto.getLicensePlate() != null && !dto.getLicensePlate().equalsIgnoreCase(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate())) {
                throw new IllegalArgumentException("Vehicle with license plate already exists: " + dto.getLicensePlate());
            }
            vehicle.setLicensePlate(dto.getLicensePlate().toUpperCase().trim());
        }

        if (dto.getMake() != null) vehicle.setMake(dto.getMake());
        if (dto.getModel() != null) vehicle.setModel(dto.getModel());
        if (dto.getColor() != null) vehicle.setColor(dto.getColor());
        if (dto.getVehicleType() != null) vehicle.setVehicleType(dto.getVehicleType());

        vehicle = vehicleRepository.save(vehicle);
        log.info("Updated vehicle ID: {} for user ID: {}", vehicle.getId(), userId);
        return mapToDTO(vehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (!vehicle.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: Vehicle does not belong to user ID: " + userId);
        }

        vehicleRepository.delete(vehicle);
        log.info("Deleted vehicle ID: {} for user ID: {}", vehicleId, userId);
    }

    private VehicleDTO mapToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
                .id(vehicle.getId())
                .userId(vehicle.getUserId())
                .licensePlate(vehicle.getLicensePlate())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .vehicleType(vehicle.getVehicleType())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
