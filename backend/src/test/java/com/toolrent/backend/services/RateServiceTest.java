package com.toolrent.backend.services;

import com.toolrent.backend.entities.RateEntity;
import com.toolrent.backend.entities.RateEntity.RateType;
import com.toolrent.backend.repositories.RateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateServiceTest {

    @Mock
    private RateRepository rateRepository;

    @InjectMocks
    private RateService rateService;

    private RateEntity testRentalRate;
    private RateEntity testLateFeeRate;
    private RateEntity testRepairRate;

    @BeforeEach
    void setUp() {
        // Tarifa de arriendo
        testRentalRate = new RateEntity();
        testRentalRate.setId(1L);
        testRentalRate.setType(RateType.RENTAL_RATE);
        testRentalRate.setDailyAmount(new BigDecimal("5000"));
        testRentalRate.setActive(true);
        testRentalRate.setEffectiveFrom(LocalDate.now().minusDays(30));
        testRentalRate.setCreatedAt(LocalDateTime.now());
        testRentalRate.setCreatedBy("admin");

        // Tarifa de multa
        testLateFeeRate = new RateEntity();
        testLateFeeRate.setId(2L);
        testLateFeeRate.setType(RateType.LATE_FEE_RATE);
        testLateFeeRate.setDailyAmount(new BigDecimal("2000"));
        testLateFeeRate.setActive(true);
        testLateFeeRate.setEffectiveFrom(LocalDate.now().minusDays(30));
        testLateFeeRate.setCreatedAt(LocalDateTime.now());
        testLateFeeRate.setCreatedBy("admin");

        // Tarifa de reparación
        testRepairRate = new RateEntity();
        testRepairRate.setId(3L);
        testRepairRate.setType(RateType.REPAIR_RATE);
        testRepairRate.setDailyAmount(new BigDecimal("30.0"));
        testRepairRate.setActive(true);
        testRepairRate.setEffectiveFrom(LocalDate.now().minusDays(30));
        testRepairRate.setCreatedAt(LocalDateTime.now());
        testRepairRate.setCreatedBy("admin");
    }

    // ========== Tests for getCurrentRentalRate ==========
    @Test
    void getCurrentRentalRate_ShouldReturnRate_WhenRateExists() {
        when(rateRepository.findCurrentActiveRateByType(RateType.RENTAL_RATE))
                .thenReturn(Optional.of(testRentalRate));

        BigDecimal result = rateService.getCurrentRentalRate();

        assertNotNull(result);
        assertEquals(new BigDecimal("5000"), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.RENTAL_RATE);
    }

    @Test
    void getCurrentRentalRate_ShouldReturnDefaultValue_WhenRateNotFound() {
        when(rateRepository.findCurrentActiveRateByType(RateType.RENTAL_RATE))
                .thenReturn(Optional.empty());

        BigDecimal result = rateService.getCurrentRentalRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(5000.0), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.RENTAL_RATE);
    }

    @Test
    void getCurrentRentalRate_ShouldReturnDefaultValue_WhenExceptionOccurs() {
        when(rateRepository.findCurrentActiveRateByType(RateType.RENTAL_RATE))
                .thenThrow(new RuntimeException("Database error"));

        BigDecimal result = rateService.getCurrentRentalRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(5000.0), result);
    }

    // ========== Tests for getCurrentLateFeeRate ==========
    @Test
    void getCurrentLateFeeRate_ShouldReturnRate_WhenRateExists() {
        when(rateRepository.findCurrentActiveRateByType(RateType.LATE_FEE_RATE))
                .thenReturn(Optional.of(testLateFeeRate));

        BigDecimal result = rateService.getCurrentLateFeeRate();

        assertNotNull(result);
        assertEquals(new BigDecimal("2000"), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.LATE_FEE_RATE);
    }

    @Test
    void getCurrentLateFeeRate_ShouldReturnDefaultValue_WhenRateNotFound() {
        when(rateRepository.findCurrentActiveRateByType(RateType.LATE_FEE_RATE))
                .thenReturn(Optional.empty());

        BigDecimal result = rateService.getCurrentLateFeeRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000.0), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.LATE_FEE_RATE);
    }

    @Test
    void getCurrentLateFeeRate_ShouldReturnDefaultValue_WhenExceptionOccurs() {
        when(rateRepository.findCurrentActiveRateByType(RateType.LATE_FEE_RATE))
                .thenThrow(new RuntimeException("Database error"));

        BigDecimal result = rateService.getCurrentLateFeeRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(2000.0), result);
    }

    // ========== Tests for getCurrentRepairRate ==========
    @Test
    void getCurrentRepairRate_ShouldReturnRate_WhenRateExists() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.of(testRepairRate));

        BigDecimal result = rateService.getCurrentRepairRate();

        assertNotNull(result);
        assertEquals(new BigDecimal("30.0"), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.REPAIR_RATE);
    }

    @Test
    void getCurrentRepairRate_ShouldReturnDefaultValue_WhenRateNotFound() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.empty());

        BigDecimal result = rateService.getCurrentRepairRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(30.0), result);
        verify(rateRepository, times(1)).findCurrentActiveRateByType(RateType.REPAIR_RATE);
    }

    @Test
    void getCurrentRepairRate_ShouldReturnDefaultValue_WhenExceptionOccurs() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenThrow(new RuntimeException("Database error"));

        BigDecimal result = rateService.getCurrentRepairRate();

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(30.0), result);
    }

    // ========== Tests for getAllRates ==========
    @Test
    void getAllRates_ShouldReturnAllRates() {
        List<RateEntity> expectedRates = Arrays.asList(testRentalRate, testLateFeeRate, testRepairRate);
        when(rateRepository.findAll()).thenReturn(expectedRates);

        List<RateEntity> result = rateService.getAllRates();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(rateRepository, times(1)).findAll();
    }

    @Test
    void getAllRates_ShouldReturnEmptyList_WhenNoRates() {
        when(rateRepository.findAll()).thenReturn(List.of());

        List<RateEntity> result = rateService.getAllRates();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for getRateById ==========
    @Test
    void getRateById_ShouldReturnRate_WhenExists() {
        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));

        RateEntity result = rateService.getRateById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(RateType.RENTAL_RATE, result.getType());
        verify(rateRepository, times(1)).findById(1L);
    }

    @Test
    void getRateById_ShouldThrowException_WhenNotFound() {
        when(rateRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.getRateById(999L)
        );

        assertTrue(exception.getMessage().contains("Tarifa no encontrada"));
    }

    // ========== Tests for createRate ==========
    @Test
    void createRate_ShouldCreateSuccessfully_WithValidData() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(LocalDate.now());
        newRate.setCreatedBy("admin");

        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        RateEntity result = rateService.createRate(newRate);

        assertNotNull(result);
        assertEquals(new BigDecimal("6000"), result.getDailyAmount());
        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void createRate_ShouldSetDefaultValues_WhenNotProvided() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(LocalDate.now());

        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertNotNull(saved.getCreatedBy());
            assertEquals("system", saved.getCreatedBy());
            assertNotNull(saved.getCreatedAt());
            assertTrue(saved.getActive());
            return saved;
        });

        rateService.createRate(newRate);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void createRate_ShouldThrowException_WhenTypeIsNull() {
        RateEntity newRate = new RateEntity();
        newRate.setType(null);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(LocalDate.now());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.createRate(newRate)
        );

        assertTrue(exception.getMessage().contains("Tipo de tarifa es requerido"));
    }

    @Test
    void createRate_ShouldThrowException_WhenDailyAmountIsNull() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(null);
        newRate.setEffectiveFrom(LocalDate.now());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.createRate(newRate)
        );

        assertTrue(exception.getMessage().contains("Monto diario debe ser mayor a 0"));
    }

    @Test
    void createRate_ShouldThrowException_WhenDailyAmountIsZero() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(BigDecimal.ZERO);
        newRate.setEffectiveFrom(LocalDate.now());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.createRate(newRate)
        );

        assertTrue(exception.getMessage().contains("Monto diario debe ser mayor a 0"));
    }

    @Test
    void createRate_ShouldThrowException_WhenDailyAmountIsNegative() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("-100"));
        newRate.setEffectiveFrom(LocalDate.now());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.createRate(newRate)
        );

        assertTrue(exception.getMessage().contains("Monto diario debe ser mayor a 0"));
    }

    @Test
    void createRate_ShouldThrowException_WhenEffectiveFromIsNull() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.createRate(newRate)
        );

        assertTrue(exception.getMessage().contains("Fecha de inicio es requerida"));
    }

    @Test
    void createRate_ShouldHandleEmptyCreatedBy() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(LocalDate.now());
        newRate.setCreatedBy("   ");

        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertEquals("system", saved.getCreatedBy());
            return saved;
        });

        rateService.createRate(newRate);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    // ========== Tests for updateRate ==========
    @Test
    void updateRate_ShouldUpdateSuccessfully_WithValidData() {
        RateEntity updateDetails = new RateEntity();
        updateDetails.setDailyAmount(new BigDecimal("7000"));
        updateDetails.setEffectiveFrom(LocalDate.now().plusDays(1));

        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenReturn(testRentalRate);

        RateEntity result = rateService.updateRate(1L, updateDetails);

        assertNotNull(result);
        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldUpdateOnlyProvidedFields() {
        RateEntity updateDetails = new RateEntity();
        updateDetails.setDailyAmount(new BigDecimal("7000"));

        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertEquals(new BigDecimal("7000"), saved.getDailyAmount());
            assertEquals(testRentalRate.getEffectiveFrom(), saved.getEffectiveFrom());
            return saved;
        });

        rateService.updateRate(1L, updateDetails);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldUpdateEffectiveTo() {
        RateEntity updateDetails = new RateEntity();
        LocalDate newEndDate = LocalDate.now().plusDays(30);
        updateDetails.setEffectiveTo(newEndDate);

        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertEquals(newEndDate, saved.getEffectiveTo());
            return saved;
        });

        rateService.updateRate(1L, updateDetails);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldUpdateActiveStatus() {
        RateEntity updateDetails = new RateEntity();
        updateDetails.setActive(false);

        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertFalse(saved.getActive());
            return saved;
        });

        rateService.updateRate(1L, updateDetails);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldThrowException_WhenRateNotFound() {
        RateEntity updateDetails = new RateEntity();
        updateDetails.setDailyAmount(new BigDecimal("7000"));

        when(rateRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.updateRate(999L, updateDetails)
        );

        assertTrue(exception.getMessage().contains("Tarifa no encontrada"));
    }

    // ========== Tests for deactivateRate ==========
    @Test
    void deactivateRate_ShouldDeactivateSuccessfully() {
        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertFalse(saved.getActive());
            return saved;
        });

        RateEntity result = rateService.deactivateRate(1L);

        assertNotNull(result);
        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void deactivateRate_ShouldThrowException_WhenRateNotFound() {
        when(rateRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> rateService.deactivateRate(999L)
        );

        assertTrue(exception.getMessage().contains("Tarifa no encontrada"));
    }

    // ========== Tests for getRatesByType ==========
    @Test
    void getRatesByType_ShouldReturnRatesOfType() {
        List<RateEntity> rentalRates = Arrays.asList(testRentalRate);
        when(rateRepository.findByType(RateType.RENTAL_RATE)).thenReturn(rentalRates);

        List<RateEntity> result = rateService.getRatesByType(RateType.RENTAL_RATE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(RateType.RENTAL_RATE, result.get(0).getType());
        verify(rateRepository, times(1)).findByType(RateType.RENTAL_RATE);
    }

    @Test
    void getRatesByType_ShouldReturnEmptyList_WhenNoRatesOfType() {
        when(rateRepository.findByType(RateType.RENTAL_RATE)).thenReturn(List.of());

        List<RateEntity> result = rateService.getRatesByType(RateType.RENTAL_RATE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for hasActiveRate ==========
    @Test
    void hasActiveRate_ShouldReturnTrue_WhenActiveRateExists() {
        when(rateRepository.existsByTypeAndActiveTrue(RateType.RENTAL_RATE)).thenReturn(true);

        boolean result = rateService.hasActiveRate(RateType.RENTAL_RATE);

        assertTrue(result);
        verify(rateRepository, times(1)).existsByTypeAndActiveTrue(RateType.RENTAL_RATE);
    }

    @Test
    void hasActiveRate_ShouldReturnFalse_WhenNoActiveRate() {
        when(rateRepository.existsByTypeAndActiveTrue(RateType.RENTAL_RATE)).thenReturn(false);

        boolean result = rateService.hasActiveRate(RateType.RENTAL_RATE);

        assertFalse(result);
    }

    // ========== Tests for calculateRepairCost ==========
    @Test
    void calculateRepairCost_ShouldCalculateCorrectly() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.of(testRepairRate));

        BigDecimal replacementValue = new BigDecimal("10000");
        BigDecimal result = rateService.calculateRepairCost(replacementValue);

        assertNotNull(result);
        // 10000 * 30% = 3000
        assertEquals(new BigDecimal("3000.00"), result.setScale(2));
    }

    @Test
    void calculateRepairCost_ShouldUseDefaultRate_WhenRateNotFound() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.empty());

        BigDecimal replacementValue = new BigDecimal("10000");
        BigDecimal result = rateService.calculateRepairCost(replacementValue);

        assertNotNull(result);
        // 10000 * 30% = 3000 (using default rate)
        assertEquals(new BigDecimal("3000.00"), result.setScale(2));
    }

    @Test
    void calculateRepairCost_ShouldCalculateCorrectly_WithDifferentRate() {
        RateEntity customRepairRate = new RateEntity();
        customRepairRate.setType(RateType.REPAIR_RATE);
        customRepairRate.setDailyAmount(new BigDecimal("50.0")); // 50%
        customRepairRate.setActive(true);

        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.of(customRepairRate));

        BigDecimal replacementValue = new BigDecimal("10000");
        BigDecimal result = rateService.calculateRepairCost(replacementValue);

        assertNotNull(result);
        // 10000 * 50% = 5000
        assertEquals(new BigDecimal("5000.00"), result.setScale(2));
    }

    // ========== Tests for getRatesInDateRange ==========
    @Test
    void getRatesInDateRange_ShouldReturnRatesInRange() {
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(10);
        List<RateEntity> expectedRates = Arrays.asList(testRentalRate, testLateFeeRate);

        when(rateRepository.findRatesInDateRange(startDate, endDate)).thenReturn(expectedRates);

        List<RateEntity> result = rateService.getRatesInDateRange(startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(rateRepository, times(1)).findRatesInDateRange(startDate, endDate);
    }

    @Test
    void getRatesInDateRange_ShouldReturnEmptyList_WhenNoRatesInRange() {
        LocalDate startDate = LocalDate.now().minusDays(100);
        LocalDate endDate = LocalDate.now().minusDays(90);

        when(rateRepository.findRatesInDateRange(startDate, endDate)).thenReturn(List.of());

        List<RateEntity> result = rateService.getRatesInDateRange(startDate, endDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for getRateHistory ==========
    @Test
    void getRateHistory_ShouldReturnAllRatesOfType() {
        RateEntity historicalRate = new RateEntity();
        historicalRate.setId(4L);
        historicalRate.setType(RateType.RENTAL_RATE);
        historicalRate.setDailyAmount(new BigDecimal("4000"));
        historicalRate.setActive(false);
        historicalRate.setEffectiveFrom(LocalDate.now().minusDays(60));
        historicalRate.setEffectiveTo(LocalDate.now().minusDays(31));

        List<RateEntity> history = Arrays.asList(testRentalRate, historicalRate);
        when(rateRepository.findByType(RateType.RENTAL_RATE)).thenReturn(history);

        List<RateEntity> result = rateService.getRateHistory(RateType.RENTAL_RATE);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(rateRepository, times(1)).findByType(RateType.RENTAL_RATE);
    }

    @Test
    void getRateHistory_ShouldReturnEmptyList_WhenNoHistory() {
        when(rateRepository.findByType(RateType.RENTAL_RATE)).thenReturn(List.of());

        List<RateEntity> result = rateService.getRateHistory(RateType.RENTAL_RATE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Edge case tests ==========
    @Test
    void createRate_ShouldPreserveCreatedBy_WhenProvided() {
        RateEntity newRate = new RateEntity();
        newRate.setType(RateType.RENTAL_RATE);
        newRate.setDailyAmount(new BigDecimal("6000"));
        newRate.setEffectiveFrom(LocalDate.now());
        newRate.setCreatedBy("custom-user");

        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> {
            RateEntity saved = invocation.getArgument(0);
            assertEquals("custom-user", saved.getCreatedBy());
            return saved;
        });

        rateService.createRate(newRate);

        verify(rateRepository, times(1)).save(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldNotModifyUnspecifiedFields() {
        RateEntity updateDetails = new RateEntity();
        updateDetails.setDailyAmount(new BigDecimal("7000"));

        BigDecimal originalDailyAmount = testRentalRate.getDailyAmount();
        LocalDate originalEffectiveFrom = testRentalRate.getEffectiveFrom();

        when(rateRepository.findById(1L)).thenReturn(Optional.of(testRentalRate));
        when(rateRepository.save(any(RateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rateService.updateRate(1L, updateDetails);

        assertEquals(originalEffectiveFrom, testRentalRate.getEffectiveFrom());
    }

    @Test
    void calculateRepairCost_ShouldHandleZeroReplacementValue() {
        when(rateRepository.findCurrentActiveRateByType(RateType.REPAIR_RATE))
                .thenReturn(Optional.of(testRepairRate));

        BigDecimal replacementValue = BigDecimal.ZERO;
        BigDecimal result = rateService.calculateRepairCost(replacementValue);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO.setScale(2), result.setScale(2));
    }

    @Test
    void getAllRates_ShouldReturnMultipleTypes() {
        List<RateEntity> allRates = Arrays.asList(testRentalRate, testLateFeeRate, testRepairRate);
        when(rateRepository.findAll()).thenReturn(allRates);

        List<RateEntity> result = rateService.getAllRates();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getType() == RateType.RENTAL_RATE));
        assertTrue(result.stream().anyMatch(r -> r.getType() == RateType.LATE_FEE_RATE));
        assertTrue(result.stream().anyMatch(r -> r.getType() == RateType.REPAIR_RATE));
    }
}