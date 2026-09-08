package com.parking.user_service.service;

import java.util.List;

import com.parking.user_service.dto.VehicleDTO;

public interface VehicleService {
    List<VehicleDTO> getVehiclesByUserId(Long userId);
    VehicleDTO addVehicle(Long userId, VehicleDTO dto);
    VehicleDTO updateVehicle(Long userId, Long vehicleId, VehicleDTO dto);
    void deleteVehicle(Long userId, Long vehicleId);
}
