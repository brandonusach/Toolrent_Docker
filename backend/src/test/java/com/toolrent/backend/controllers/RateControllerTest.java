package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toolrent.backend.entities.RateEntity;
import com.toolrent.backend.services.RateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RateControllerTest {

    @Mock
    private RateService rateService;

    @InjectMocks
    private RateController rateController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RateEntity testRate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rateController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testRate = new RateEntity();
        testRate.setId(1L);
        testRate.setType(RateEntity.RateType.RENTAL_RATE);
        testRate.setDailyAmount(BigDecimal.valueOf(1000));
        testRate.setEffectiveFrom(LocalDate.now());
        testRate.setActive(true);
    }

    // Utility method to create a RateEntity
    private RateEntity createRate(Long id, RateEntity.RateType type, double amount) {
        RateEntity rate = new RateEntity();
        rate.setId(id);
        rate.setType(type);
        rate.setDailyAmount(BigDecimal.valueOf(amount));
        rate.setEffectiveFrom(LocalDate.now());
        rate.setActive(true);
        return rate;
    }

    // ========== Tests for GET /api/v1/rates/current/* (Current Rates) ==========

    @Test
    void getCurrentRentalRate_ShouldReturnRentalRate() throws Exception {
        when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.valueOf(1000));
        mockMvc.perform(get("/api/v1/rates/current/rental"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(1000))
                .andExpect(jsonPath("$.success").value(true));
        verify(rateService, times(1)).getCurrentRentalRate();
    }

    @Test
    void getCurrentRentalRate_ShouldReturnError_WhenServiceFails() throws Exception {
        when(rateService.getCurrentRentalRate())
                .thenThrow(new RuntimeException("No active rental rate found"));
        mockMvc.perform(get("/api/v1/rates/current/rental"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getCurrentRentalRate();
    }

    @Test
    void getCurrentLateFeeRate_ShouldReturnLateFeeRate() throws Exception {
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.valueOf(500));
        mockMvc.perform(get("/api/v1/rates/current/late-fee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(500));
        verify(rateService, times(1)).getCurrentLateFeeRate();
    }

    @Test
    void getCurrentRepairRate_ShouldReturnRepairRate() throws Exception {
        when(rateService.getCurrentRepairRate()).thenReturn(BigDecimal.valueOf(2000));
        mockMvc.perform(get("/api/v1/rates/current/repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(2000));
        verify(rateService, times(1)).getCurrentRepairRate();
    }

    // ========== Tests for GET /api/v1/rates/ (List Rates) ==========

    @Test
    void listRates_ShouldReturnAllRates() throws Exception {
        List<RateEntity> rates = Arrays.asList(testRate, createRate(2L, RateEntity.RateType.LATE_FEE_RATE, 500));
        when(rateService.getAllRates()).thenReturn(rates);
        mockMvc.perform(get("/api/v1/rates/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        verify(rateService, times(1)).getAllRates();
    }

    @Test
    void listRates_ShouldReturnError_WhenServiceFails() throws Exception {
        when(rateService.getAllRates()).thenThrow(new RuntimeException("Database error"));
        mockMvc.perform(get("/api/v1/rates/"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getAllRates();
    }

    // ========== Tests for GET /api/v1/rates/{id} ==========

    @Test
    void getRateById_ShouldReturnRate_WhenExists() throws Exception {
        when(rateService.getRateById(1L)).thenReturn(testRate);
        mockMvc.perform(get("/api/v1/rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(rateService, times(1)).getRateById(1L);
    }

    @Test
    void getRateById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(rateService.getRateById(999L)).thenThrow(new RuntimeException("Rate not found"));
        mockMvc.perform(get("/api/v1/rates/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tarifa no encontrada"));
        verify(rateService, times(1)).getRateById(999L);
    }


    // ========== Tests for POST /api/v1/rates/ (Save Rate) ==========

    @Test
    void saveRate_ShouldCreateAndReturnRate() throws Exception {
        RateEntity newRate = createRate(null, RateEntity.RateType.RENTAL_RATE, 1200);
        RateEntity savedRate = createRate(3L, RateEntity.RateType.RENTAL_RATE, 1200);

        when(rateService.createRate(any(RateEntity.class))).thenReturn(savedRate);

        mockMvc.perform(post("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
        verify(rateService, times(1)).createRate(any(RateEntity.class));
    }

    @Test
    void saveRate_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        RateEntity newRate = createRate(null, RateEntity.RateType.RENTAL_RATE, 1200);

        when(rateService.createRate(any(RateEntity.class)))
                .thenThrow(new RuntimeException("Rate amount cannot be negative"));

        mockMvc.perform(post("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).createRate(any(RateEntity.class));
    }

    // ========== Tests for PUT /api/v1/rates/ (Update Rate) ==========

    @Test
    void updateRate_ShouldReturnUpdatedRate() throws Exception {
        RateEntity updatedRate = createRate(1L, RateEntity.RateType.RENTAL_RATE, 1500);

        when(rateService.updateRate(eq(1L), any(RateEntity.class))).thenReturn(updatedRate);

        mockMvc.perform(put("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyAmount").value(1500));
        verify(rateService, times(1)).updateRate(eq(1L), any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldReturnBadRequest_WhenMissingId() throws Exception {
        RateEntity incompleteRate = createRate(null, RateEntity.RateType.RENTAL_RATE, 1500);

        mockMvc.perform(put("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteRate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ID de tarifa es requerido para actualizar"));
        verify(rateService, never()).updateRate(anyLong(), any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        RateEntity updatedRate = createRate(1L, RateEntity.RateType.RENTAL_RATE, 1500);

        when(rateService.updateRate(eq(1L), any(RateEntity.class)))
                .thenThrow(new RuntimeException("Rate ID not found for update"));

        mockMvc.perform(put("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).updateRate(eq(1L), any(RateEntity.class));
    }


    // ========== Tests for DELETE /api/v1/rates/{id} (Deactivate Rate) ==========

    @Test
    void deleteRateById_ShouldDeactivateAndReturnRate() throws Exception {
        RateEntity deactivatedRate = createRate(1L, RateEntity.RateType.RENTAL_RATE, 1000);
        deactivatedRate.setActive(false);

        when(rateService.deactivateRate(1L)).thenReturn(deactivatedRate);

        mockMvc.perform(delete("/api/v1/rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        verify(rateService, times(1)).deactivateRate(1L);
    }

    @Test
    void deleteRateById_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        when(rateService.deactivateRate(1L)).thenThrow(new RuntimeException("Rate is already inactive"));

        mockMvc.perform(delete("/api/v1/rates/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).deactivateRate(1L);
    }

    // ========== Tests for PUT /api/v1/rates/{id}/deactivate (Alias for Deletion) ==========

    @Test
    void deactivateRate_ShouldDeactivateAndReturnRate() throws Exception {
        RateEntity deactivatedRate = createRate(1L, RateEntity.RateType.RENTAL_RATE, 1000);
        deactivatedRate.setActive(false);

        when(rateService.deactivateRate(1L)).thenReturn(deactivatedRate);

        mockMvc.perform(put("/api/v1/rates/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        verify(rateService, times(1)).deactivateRate(1L);
    }

    @Test
    void deactivateRate_ShouldReturnBadRequest_OnException() throws Exception {
        when(rateService.deactivateRate(1L)).thenThrow(new RuntimeException("Cannot deactivate"));

        mockMvc.perform(put("/api/v1/rates/1/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).deactivateRate(1L);
    }

    // ========== Tests for GET /api/v1/rates/type/{type} ==========

    @Test
    void getRatesByType_ShouldReturnRates() throws Exception {
        List<RateEntity> rates = List.of(testRate);
        when(rateService.getRatesByType(RateEntity.RateType.RENTAL_RATE)).thenReturn(rates);

        mockMvc.perform(get("/api/v1/rates/type/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(rateService, times(1)).getRatesByType(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRatesByType_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/rates/type/INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tipo de tarifa inválido: INVALID_TYPE"));
        verify(rateService, never()).getRatesByType(any());
    }

    @Test
    void getRatesByType_ShouldReturnInternalServerError_OnGenericException() throws Exception {
        when(rateService.getRatesByType(RateEntity.RateType.RENTAL_RATE))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/rates/type/RENTAL_RATE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getRatesByType(RateEntity.RateType.RENTAL_RATE);
    }

    // ========== Tests for POST /api/v1/rates/calculate-repair ==========

    @Test
    void calculateRepairCost_ShouldReturnCost() throws Exception {
        BigDecimal replacementValue = BigDecimal.valueOf(100000);
        BigDecimal repairCost = BigDecimal.valueOf(30000);

        when(rateService.calculateRepairCost(replacementValue)).thenReturn(repairCost);

        mockMvc.perform(post("/api/v1/rates/calculate-repair")
                        .param("replacementValue", replacementValue.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repairCost").value(30000))
                .andExpect(jsonPath("$.success").value(true));
        verify(rateService, times(1)).calculateRepairCost(replacementValue);
    }

    @Test
    void calculateRepairCost_ShouldReturnBadRequest_OnException() throws Exception {
        BigDecimal replacementValue = BigDecimal.valueOf(100000);

        when(rateService.calculateRepairCost(replacementValue)).thenThrow(new RuntimeException("Calculation logic failed"));

        mockMvc.perform(post("/api/v1/rates/calculate-repair")
                        .param("replacementValue", replacementValue.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).calculateRepairCost(replacementValue);
    }

    // ========== Tests for GET /api/v1/rates/history/{type} ==========

    @Test
    void getRateHistory_ShouldReturnHistory() throws Exception {
        List<RateEntity> history = Arrays.asList(testRate, createRate(2L, RateEntity.RateType.RENTAL_RATE, 900));
        when(rateService.getRateHistory(RateEntity.RateType.RENTAL_RATE)).thenReturn(history);

        mockMvc.perform(get("/api/v1/rates/history/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        verify(rateService, times(1)).getRateHistory(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRateHistory_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/rates/history/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, never()).getRateHistory(any());
    }

    // ========== Tests for GET /api/v1/rates/exists/active/{type} ==========

    @Test
    void hasActiveRate_ShouldReturnTrue() throws Exception {
        when(rateService.hasActiveRate(RateEntity.RateType.RENTAL_RATE)).thenReturn(true);

        mockMvc.perform(get("/api/v1/rates/exists/active/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
        verify(rateService, times(1)).hasActiveRate(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void hasActiveRate_ShouldReturnFalse() throws Exception {
        when(rateService.hasActiveRate(RateEntity.RateType.REPAIR_RATE)).thenReturn(false);

        mockMvc.perform(get("/api/v1/rates/exists/active/REPAIR_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
        verify(rateService, times(1)).hasActiveRate(RateEntity.RateType.REPAIR_RATE);
    }

    @Test
    void hasActiveRate_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/rates/exists/active/BAD_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, never()).hasActiveRate(any());
    }

    // ========== Tests for GET /api/v1/rates/date-range ==========

    @Test
    void getRatesInDateRange_ShouldReturnRates() throws Exception {
        List<RateEntity> rates = List.of(testRate);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        when(rateService.getRatesInDateRange(eq(start), eq(end))).thenReturn(rates);

        mockMvc.perform(get("/api/v1/rates/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(rateService, times(1)).getRatesInDateRange(eq(start), eq(end));
    }

    @Test
    void getRatesInDateRange_ShouldReturnBadRequest_OnParsingError() throws Exception {
        mockMvc.perform(get("/api/v1/rates/date-range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, never()).getRatesInDateRange(any(), any());
    }

    // ========== Tests for GET /api/v1/rates/test (Utility) ==========

    @Test
    void testEndpoint_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/rates/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rate controller está funcionando"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== Existing tests enhanced/moved for completeness ==========

    @Test
    void getCurrentRentalRate_ShouldReturnZeroRate() throws Exception {
        when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.ZERO);
        mockMvc.perform(get("/api/v1/rates/current/rental"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(0))
                .andExpect(jsonPath("$.success").value(true));
        verify(rateService, times(1)).getCurrentRentalRate();
    }

    // ========== Additional Tests for Better Coverage ==========

    @Test
    void getCurrentLateFeeRate_ShouldReturnError_WhenServiceFails() throws Exception {
        when(rateService.getCurrentLateFeeRate())
                .thenThrow(new RuntimeException("No active late fee rate"));
        mockMvc.perform(get("/api/v1/rates/current/late-fee"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getCurrentLateFeeRate();
    }

    @Test
    void getCurrentRepairRate_ShouldReturnError_WhenServiceFails() throws Exception {
        when(rateService.getCurrentRepairRate())
                .thenThrow(new RuntimeException("No active repair rate"));
        mockMvc.perform(get("/api/v1/rates/current/repair"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getCurrentRepairRate();
    }

    @Test
    void listRates_ShouldReturnEmptyList() throws Exception {
        when(rateService.getAllRates()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/rates/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(rateService, times(1)).getAllRates();
    }

    @Test
    void getRatesByType_ShouldReturnEmptyList() throws Exception {
        when(rateService.getRatesByType(RateEntity.RateType.RENTAL_RATE))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/rates/type/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(rateService, times(1)).getRatesByType(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRateHistory_ShouldReturnEmptyList() throws Exception {
        when(rateService.getRateHistory(RateEntity.RateType.RENTAL_RATE))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/rates/history/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(rateService, times(1)).getRateHistory(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRateHistory_ShouldReturnInternalServerError_OnGenericException() throws Exception {
        when(rateService.getRateHistory(RateEntity.RateType.RENTAL_RATE))
                .thenThrow(new RuntimeException("Database error"));
        mockMvc.perform(get("/api/v1/rates/history/RENTAL_RATE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getRateHistory(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void hasActiveRate_ShouldReturnInternalServerError_OnGenericException() throws Exception {
        when(rateService.hasActiveRate(RateEntity.RateType.RENTAL_RATE))
                .thenThrow(new RuntimeException("Database error"));
        mockMvc.perform(get("/api/v1/rates/exists/active/RENTAL_RATE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).hasActiveRate(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRatesInDateRange_ShouldReturnEmptyList() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        when(rateService.getRatesInDateRange(start, end)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/rates/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(rateService, times(1)).getRatesInDateRange(start, end);
    }

    @Test
    void getRatesInDateRange_ShouldReturnBadRequest_OnInvalidEndDate() throws Exception {
        mockMvc.perform(get("/api/v1/rates/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, never()).getRatesInDateRange(any(), any());
    }

    @Test
    void getRatesInDateRange_ShouldReturnBadRequest_OnServiceException() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        when(rateService.getRatesInDateRange(start, end))
                .thenThrow(new RuntimeException("Invalid date range"));

        mockMvc.perform(get("/api/v1/rates/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).getRatesInDateRange(start, end);
    }

    @Test
    void calculateRepairCost_ShouldReturnCost_WithZeroValue() throws Exception {
        BigDecimal replacementValue = BigDecimal.ZERO;
        BigDecimal repairCost = BigDecimal.ZERO;

        when(rateService.calculateRepairCost(replacementValue)).thenReturn(repairCost);

        mockMvc.perform(post("/api/v1/rates/calculate-repair")
                        .param("replacementValue", replacementValue.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repairCost").value(0))
                .andExpect(jsonPath("$.success").value(true));
        verify(rateService, times(1)).calculateRepairCost(replacementValue);
    }

    @Test
    void calculateRepairCost_ShouldReturnCost_WithLargeValue() throws Exception {
        BigDecimal replacementValue = new BigDecimal("1000000");
        BigDecimal repairCost = new BigDecimal("300000");

        when(rateService.calculateRepairCost(replacementValue)).thenReturn(repairCost);

        mockMvc.perform(post("/api/v1/rates/calculate-repair")
                        .param("replacementValue", replacementValue.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repairCost").value(300000))
                .andExpect(jsonPath("$.success").value(true));
        verify(rateService, times(1)).calculateRepairCost(replacementValue);
    }

    @Test
    void saveRate_ShouldCreateRate_WithAllFields() throws Exception {
        RateEntity newRate = createRate(null, RateEntity.RateType.LATE_FEE_RATE, 800);
        newRate.setEffectiveFrom(LocalDate.now());
        newRate.setActive(true);

        RateEntity savedRate = createRate(10L, RateEntity.RateType.LATE_FEE_RATE, 800);

        when(rateService.createRate(any(RateEntity.class))).thenReturn(savedRate);

        mockMvc.perform(post("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("LATE_FEE_RATE"));
        verify(rateService, times(1)).createRate(any(RateEntity.class));
    }

    @Test
    void updateRate_ShouldUpdateRate_WithDifferentType() throws Exception {
        RateEntity updatedRate = createRate(5L, RateEntity.RateType.REPAIR_RATE, 3000);

        when(rateService.updateRate(eq(5L), any(RateEntity.class))).thenReturn(updatedRate);

        mockMvc.perform(put("/api/v1/rates/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyAmount").value(3000))
                .andExpect(jsonPath("$.type").value("REPAIR_RATE"));
        verify(rateService, times(1)).updateRate(eq(5L), any(RateEntity.class));
    }

    @Test
    void deleteRateById_ShouldReturnBadRequest_WhenRateNotFound() throws Exception {
        when(rateService.deactivateRate(999L))
                .thenThrow(new RuntimeException("Rate not found with ID: 999"));

        mockMvc.perform(delete("/api/v1/rates/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).deactivateRate(999L);
    }

    @Test
    void getRateById_ShouldReturnRate_WithCompleteData() throws Exception {
        RateEntity rate = createRate(10L, RateEntity.RateType.RENTAL_RATE, 1500);
        rate.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        rate.setActive(true);

        when(rateService.getRateById(10L)).thenReturn(rate);

        mockMvc.perform(get("/api/v1/rates/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("RENTAL_RATE"))
                .andExpect(jsonPath("$.dailyAmount").value(1500))
                .andExpect(jsonPath("$.active").value(true));
        verify(rateService, times(1)).getRateById(10L);
    }

    @Test
    void getRatesByType_ShouldReturnMultipleRates() throws Exception {
        List<RateEntity> rates = Arrays.asList(
                createRate(1L, RateEntity.RateType.RENTAL_RATE, 1000),
                createRate(2L, RateEntity.RateType.RENTAL_RATE, 1200),
                createRate(3L, RateEntity.RateType.RENTAL_RATE, 1500)
        );
        when(rateService.getRatesByType(RateEntity.RateType.RENTAL_RATE)).thenReturn(rates);

        mockMvc.perform(get("/api/v1/rates/type/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].dailyAmount").value(1000))
                .andExpect(jsonPath("$[1].dailyAmount").value(1200))
                .andExpect(jsonPath("$[2].dailyAmount").value(1500));
        verify(rateService, times(1)).getRatesByType(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRatesByType_ShouldAcceptLowerCaseType() throws Exception {
        List<RateEntity> rates = List.of(testRate);
        when(rateService.getRatesByType(RateEntity.RateType.RENTAL_RATE)).thenReturn(rates);

        mockMvc.perform(get("/api/v1/rates/type/rental_rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(rateService, times(1)).getRatesByType(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void getRateHistory_ShouldReturnMultipleRates() throws Exception {
        List<RateEntity> history = Arrays.asList(
                createRate(1L, RateEntity.RateType.RENTAL_RATE, 1000),
                createRate(2L, RateEntity.RateType.RENTAL_RATE, 900),
                createRate(3L, RateEntity.RateType.RENTAL_RATE, 800)
        );
        when(rateService.getRateHistory(RateEntity.RateType.RENTAL_RATE)).thenReturn(history);

        mockMvc.perform(get("/api/v1/rates/history/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        verify(rateService, times(1)).getRateHistory(RateEntity.RateType.RENTAL_RATE);
    }

    @Test
    void hasActiveRate_ShouldCheckAllRateTypes() throws Exception {
        when(rateService.hasActiveRate(RateEntity.RateType.RENTAL_RATE)).thenReturn(true);
        when(rateService.hasActiveRate(RateEntity.RateType.LATE_FEE_RATE)).thenReturn(true);
        when(rateService.hasActiveRate(RateEntity.RateType.REPAIR_RATE)).thenReturn(false);

        mockMvc.perform(get("/api/v1/rates/exists/active/RENTAL_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(get("/api/v1/rates/exists/active/LATE_FEE_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(get("/api/v1/rates/exists/active/REPAIR_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void deactivateRate_ShouldReturnInternalServerError_OnGenericException() throws Exception {
        when(rateService.deactivateRate(1L))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(put("/api/v1/rates/1/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verify(rateService, times(1)).deactivateRate(1L);
    }

    @Test
    void getCurrentRentalRate_ShouldReturnSuccess() throws Exception {
        when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.valueOf(1000));
        mockMvc.perform(get("/api/v1/rates/current/rental"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rate").value(1000));
        verify(rateService, times(1)).getCurrentRentalRate();
    }

    @Test
    void getCurrentLateFeeRate_ShouldReturnSuccess() throws Exception {
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.valueOf(500));
        mockMvc.perform(get("/api/v1/rates/current/late-fee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rate").value(500));
        verify(rateService, times(1)).getCurrentLateFeeRate();
    }

    @Test
    void getCurrentRepairRate_ShouldReturnSuccess() throws Exception {
        when(rateService.getCurrentRepairRate()).thenReturn(BigDecimal.valueOf(2000));
        mockMvc.perform(get("/api/v1/rates/current/repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rate").value(2000));
        verify(rateService, times(1)).getCurrentRepairRate();
    }
}