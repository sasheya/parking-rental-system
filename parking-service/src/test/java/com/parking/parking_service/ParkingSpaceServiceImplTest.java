package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parking.parking_service.dto.ParkingSearchRequest;
import com.parking.parking_service.dto.ParkingSpaceDTO;
import com.parking.parking_service.model.ParkingSpace;
import com.parking.parking_service.repository.ParkingSpaceRepository;
import com.parking.parking_service.service.ParkingSpaceServiceImpl;

class ParkingSpaceServiceImplTest {

    private ParkingSpaceRepository repository;
    private ParkingSpaceServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ParkingSpaceRepository.class);
        service = new ParkingSpaceServiceImpl(repository);
    }

    private ParkingSpace space() {
        return ParkingSpace.builder()
                .id(1L)
                .ownerId(10L)
                .title("Central Parking")
                .description("Covered parking")
                .address("1 Main Street")
                .city("London")
                .zipCode("SW1")
                .latitude(51.50)
                .longitude(-0.12)
                .pricePerHour(new BigDecimal("5.00"))
                .totalSlots(10)
                .availableSlots(7)
                .active(true)
                .build();
    }

    private ParkingSpaceDTO dto() {
        return ParkingSpaceDTO.builder()
                .title("Central Parking")
                .description("Covered parking")
                .address("1 Main Street")
                .city("London")
                .zipCode("SW1")
                .latitude(51.50)
                .longitude(-0.12)
                .pricePerHour(new BigDecimal("5.00"))
                .totalSlots(10)
                .build();
    }

    // ---------------------------------------------------------
    // getAllActiveSpaces
    // ---------------------------------------------------------

    @Test
    void getAllActiveSpaces_returnsMappedSpaces() {
        when(repository.findByActiveTrue()).thenReturn(List.of(space()));

        List<ParkingSpaceDTO> result = service.getAllActiveSpaces();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Central Parking", result.get(0).getTitle());
        assertEquals(10L, result.get(0).getOwnerId());

        verify(repository).findByActiveTrue();
    }

    @Test
    void getAllActiveSpaces_returnsEmptyList() {
        when(repository.findByActiveTrue()).thenReturn(List.of());

        List<ParkingSpaceDTO> result = service.getAllActiveSpaces();

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------
    // getSpaceById
    // ---------------------------------------------------------

    @Test
    void getSpaceById_returnsSpace() {
        when(repository.findById(1L)).thenReturn(Optional.of(space()));

        ParkingSpaceDTO result = service.getSpaceById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Central Parking", result.getTitle());
    }

    @Test
    void getSpaceById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getSpaceById(99L));

        assertEquals(
                "Parking space not found with ID: 99",
                exception.getMessage());
    }

    // ---------------------------------------------------------
    // searchSpaces
    // ---------------------------------------------------------

    @Test
    void searchSpaces_byCoordinatesWithRadius() {
        when(repository.findWithinRadius(51.5, -0.12, 5.0))
                .thenReturn(List.of(space()));

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .latitude(51.5)
                .longitude(-0.12)
                .radiusKm(5.0)
                .build();

        List<ParkingSpaceDTO> result = service.searchSpaces(request);

        assertEquals(1, result.size());

        verify(repository).findWithinRadius(51.5, -0.12, 5.0);
        verify(repository, never()).findByCityContainingIgnoreCaseAndActiveTrue(any());
    }

    @Test
    void searchSpaces_byCoordinatesUsesDefaultRadius() {
        when(repository.findWithinRadius(51.5, -0.12, 10.0))
                .thenReturn(List.of(space()));

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .latitude(51.5)
                .longitude(-0.12)
                .build();

        List<ParkingSpaceDTO> result = service.searchSpaces(request);

        assertEquals(1, result.size());

        verify(repository).findWithinRadius(51.5, -0.12, 10.0);
    }

    @Test
    void searchSpaces_byCity_trimsCity() {
        when(repository.findByCityContainingIgnoreCaseAndActiveTrue("London"))
                .thenReturn(List.of(space()));

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .city("  London  ")
                .build();

        List<ParkingSpaceDTO> result = service.searchSpaces(request);

        assertEquals(1, result.size());

        verify(repository)
                .findByCityContainingIgnoreCaseAndActiveTrue("London");
    }

    @Test
    void searchSpaces_blankCity_returnsAllActiveSpaces() {
        when(repository.findByActiveTrue())
                .thenReturn(List.of(space()));

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .city("   ")
                .build();

        List<ParkingSpaceDTO> result = service.searchSpaces(request);

        assertEquals(1, result.size());

        verify(repository).findByActiveTrue();
        verify(repository, never())
                .findByCityContainingIgnoreCaseAndActiveTrue(any());
    }

    @Test
    void searchSpaces_noFilters_returnsAllActiveSpaces() {
        when(repository.findByActiveTrue())
                .thenReturn(List.of(space()));

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .build();

        List<ParkingSpaceDTO> result = service.searchSpaces(request);

        assertEquals(1, result.size());

        verify(repository).findByActiveTrue();
    }

    // ---------------------------------------------------------
    // getListingsByOwnerId
    // ---------------------------------------------------------

    @Test
    void getListingsByOwnerId_returnsOwnerListings() {
        when(repository.findByOwnerId(10L))
                .thenReturn(List.of(space()));

        List<ParkingSpaceDTO> result =
                service.getListingsByOwnerId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getOwnerId());

        verify(repository).findByOwnerId(10L);
    }

    // ---------------------------------------------------------
    // createSpace
    // ---------------------------------------------------------

    @Test
    void createSpaceInitializesAvailableSlotsFromTotalSlots() {
        when(repository.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> {
                    ParkingSpace saved = invocation.getArgument(0);
                    saved.setId(5L);
                    return saved;
                });

        ParkingSpaceDTO result =
                service.createSpace(4L, dto());

        assertEquals(5L, result.getId());
        assertEquals(4L, result.getOwnerId());
        assertEquals(10, result.getTotalSlots());
        assertEquals(10, result.getAvailableSlots());
        assertEquals(Boolean.TRUE, result.getActive());
    }

    @Test
    void createSpace_preservesExplicitActiveValue() {
        when(repository.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpaceDTO request = dto();
        request.setActive(false);

        ParkingSpaceDTO result =
                service.createSpace(4L, request);

        assertEquals(Boolean.FALSE, result.getActive());
        assertEquals(10, result.getAvailableSlots());
    }

    // ---------------------------------------------------------
    // updateSpace
    // ---------------------------------------------------------

    @Test
    void updateSpace_updatesAllProvidedFields() {
        ParkingSpace existing = space();

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(repository.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpaceDTO update = ParkingSpaceDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .address("New Address")
                .city("Manchester")
                .zipCode("M1")
                .latitude(53.48)
                .longitude(-2.24)
                .pricePerHour(new BigDecimal("8.00"))
                .totalSlots(5)
                .active(false)
                .build();

        ParkingSpaceDTO result =
                service.updateSpace(10L, 1L, update);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals("New Address", result.getAddress());
        assertEquals("Manchester", result.getCity());
        assertEquals("M1", result.getZipCode());
        assertEquals(53.48, result.getLatitude());
        assertEquals(-2.24, result.getLongitude());
        assertEquals(new BigDecimal("8.00"), result.getPricePerHour());
        assertEquals(5, result.getTotalSlots());
        assertEquals(5, result.getAvailableSlots());
        assertEquals(Boolean.FALSE, result.getActive());

        verify(repository).save(existing);
    }

    @Test
    void updateSpace_doesNotIncreaseAvailableSlotsWhenTotalSlotsIncreases() {
        ParkingSpace existing = space();
        existing.setAvailableSlots(7);
        existing.setTotalSlots(10);

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(repository.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpaceDTO update = ParkingSpaceDTO.builder()
                .totalSlots(15)
                .build();

        ParkingSpaceDTO result =
                service.updateSpace(10L, 1L, update);

        assertEquals(15, result.getTotalSlots());
        assertEquals(7, result.getAvailableSlots());
    }

    @Test
    void updateSpace_throwsWhenSpaceNotFound() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateSpace(
                        10L,
                        99L,
                        ParkingSpaceDTO.builder().title("Test").build()));

        assertEquals(
                "Parking space not found with ID: 99",
                exception.getMessage());
    }

    @Test
    void updateSpace_throwsWhenOwnerDoesNotMatch() {
        ParkingSpace existing = space();

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateSpace(
                        999L,
                        1L,
                        ParkingSpaceDTO.builder()
                                .title("Updated")
                                .build()));

        assertEquals(
                "Unauthorized: Parking space does not belong to owner ID: 999",
                exception.getMessage());

        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------
    // deleteSpace
    // ---------------------------------------------------------

    @Test
    void deleteSpace_deletesOwnedSpace() {
        ParkingSpace existing = space();

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        service.deleteSpace(10L, 1L);

        verify(repository).delete(existing);
    }

    @Test
    void deleteSpace_throwsWhenSpaceNotFound() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteSpace(10L, 99L));

        assertEquals(
                "Parking space not found with ID: 99",
                exception.getMessage());

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteSpace_throwsWhenOwnerDoesNotMatch() {
        ParkingSpace existing = space();

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteSpace(999L, 1L));

        assertEquals(
                "Unauthorized: Parking space does not belong to owner ID: 999",
                exception.getMessage());

        verify(repository, never()).delete(any());
    }
}