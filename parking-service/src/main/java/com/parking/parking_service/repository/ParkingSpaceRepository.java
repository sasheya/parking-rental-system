package com.parking.parking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parking.parking_service.model.ParkingSpace;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {
    List<ParkingSpace> findByOwnerId(Long ownerId);
    List<ParkingSpace> findByActiveTrue();
    List<ParkingSpace> findByCityContainingIgnoreCaseAndActiveTrue(String city);

    @Query("SELECT p FROM ParkingSpace p WHERE p.active = true AND " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
           "cos(radians(p.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radiusKm")
    List<ParkingSpace> findWithinRadius(@Param("lat") Double lat,
                                        @Param("lng") Double lng,
                                        @Param("radiusKm") Double radiusKm);
}
