package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.ClientEntity; // Added for client-related tests
import com.toolrent.backend.entities.FineEntity;
import com.toolrent.backend.services.FineService;
import com.toolrent.backend.services.ClientService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FineControllerTest {

    @Mock
    private FineService fineService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private FineController fineController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private FineEntity testFine;
    private ClientEntity testClient;

    @BeforeEach
    void setUp() {
        // Initializes MockMvc for standalone setup of the controller
        mockMvc = MockMvcBuilders.standaloneSetup(fineController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Setup test FineEntity
        testFine = new FineEntity();
        testFine.setId(1L);
        testFine.setType(FineEntity.FineType.LATE_RETURN);
        testFine.setAmount(BigDecimal.valueOf(5000));
        testFine.setDescription("Late return fine");
        testFine.setPaid(false);
        testFine.setDueDate(LocalDate.now().plusDays(7)); // Set a due date

        // Setup test ClientEntity for client-related tests
        testClient = new ClientEntity();
        testClient.setId(10L);
        testClient.setName("Test Client");
    }

    // Utility method to create a FineEntity
    private FineEntity createFine(Long id, FineEntity.FineType type, double amount) {
        FineEntity fine = new FineEntity();
        fine.setId(id);
        fine.setType(type);
        fine.setAmount(BigDecimal.valueOf(amount));
        fine.setPaid(false);
        fine.setDueDate(LocalDate.now().plusDays(7)); // Set a due date
        return fine;
    }

    // ========== Tests for GET /api/v1/fines/ ==========

    // FIX: Changed jsonPath to correctly handle the success case (was failing due to multiple runs)
    @Test
    void listFines_ShouldReturnAllFines() throws Exception {
        FineEntity fine2 = createFine(2L, FineEntity.FineType.TOOL_REPLACEMENT, 10000);

        List<FineEntity> fines = Arrays.asList(testFine, fine2);
        when(fineService.getAllFines()).thenReturn(fines);

        mockMvc.perform(get("/api/v1/fines/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("LATE_RETURN"))
                .andExpect(jsonPath("$[1].type").value("TOOL_REPLACEMENT"));

        verify(fineService, times(1)).getAllFines();
    }

    @Test
    void listFines_ShouldReturnEmptyList_WhenNoFines() throws Exception {
        when(fineService.getAllFines()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/fines/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getAllFines();
    }

    // FIX: Updated to check the structured 500 error response body
    @Test
    void listFines_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getAllFines())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/fines/"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(true)) // Check for 'error: true'
                .andExpect(jsonPath("$.message").exists()) // Check for 'message' field
                .andExpect(jsonPath("$.data").isArray()); // Check for 'data: []'

        verify(fineService, times(1)).getAllFines();
    }

    // Additional check for listFines
    @Test
    void listFines_ShouldHandleMultipleFines() throws Exception {
        List<FineEntity> fines = Arrays.asList(
                createFine(1L, FineEntity.FineType.LATE_RETURN, 1000),
                createFine(2L, FineEntity.FineType.TOOL_REPLACEMENT, 2000),
                createFine(3L, FineEntity.FineType.DAMAGE_REPAIR, 3000)
        );

        when(fineService.getAllFines()).thenReturn(fines);

        mockMvc.perform(get("/api/v1/fines/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(fineService, times(1)).getAllFines();
    }

    // Additional check for empty response (same as listFines_ShouldReturnEmptyList_WhenNoFines)
    @Test
    void listFines_ShouldHandleEmptyResponse() throws Exception {
        when(fineService.getAllFines()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/fines/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getAllFines();
    }

    // ========== Tests for GET /api/v1/fines/{id} ==========

    // FIX: Removed the exception throw from service mock to fix the test, as it should return a FineEntity
    @Test
    void getFineById_ShouldReturnFine_WhenExists() throws Exception {
        when(fineService.getFineById(1L)).thenReturn(testFine);

        mockMvc.perform(get("/api/v1/fines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("LATE_RETURN"))
                .andExpect(jsonPath("$.amount").value(5000));

        verify(fineService, times(1)).getFineById(1L);
    }

    // Check for paid fine
    @Test
    void getFineById_ShouldReturnPaidFine() throws Exception {
        testFine.setPaid(true);
        when(fineService.getFineById(1L)).thenReturn(testFine);

        mockMvc.perform(get("/api/v1/fines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));

        verify(fineService, times(1)).getFineById(1L);
    }

    // FIX: Updated to check the structured 500 error response body
    @Test
    void getFineById_ShouldReturnError_WhenNotExists() throws Exception {
        when(fineService.getFineById(999L))
                .thenThrow(new RuntimeException("Fine not found"));

        mockMvc.perform(get("/api/v1/fines/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(fineService, times(1)).getFineById(999L);
    }

    // ========== New Tests for Uncovered Endpoints ==========

    // --- PUT /api/v1/fines/
    @Test
    void updateFine_ShouldReturnUpdatedFine() throws Exception {
        Map<String, Object> updates = new HashMap<>();
        updates.put("id", 1L);
        updates.put("description", "Updated desc");
        updates.put("dueDate", LocalDate.of(2025, 12, 31).toString());

        FineEntity updatedFine = createFine(1L, FineEntity.FineType.LATE_RETURN, 5000);
        updatedFine.setDescription("Updated desc");

        when(fineService.updateFine(anyLong(), anyString(), any(LocalDate.class))).thenReturn(updatedFine);

        mockMvc.perform(put("/api/v1/fines/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated desc"));

        verify(fineService, times(1)).updateFine(eq(1L), eq("Updated desc"), any(LocalDate.class));
    }

    @Test
    void updateFine_ShouldReturnError_WhenUpdateFails() throws Exception {
        Map<String, Object> updates = new HashMap<>();
        updates.put("id", 1L);

        when(fineService.updateFine(eq(1L), isNull(), isNull()))
                .thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(put("/api/v1/fines/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("")); // Expect an empty body (null in the controller)

        verify(fineService, times(1)).updateFine(eq(1L), isNull(), isNull());
    }

    // --- DELETE /api/v1/fines/{id}
    @Test
    void deleteFineById_ShouldReturnOk() throws Exception {
        doNothing().when(fineService).deleteFine(1L);

        mockMvc.perform(delete("/api/v1/fines/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Fine deleted successfully"));

        verify(fineService, times(1)).deleteFine(1L);
    }

    @Test
    void deleteFineById_ShouldReturnError_WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Delete fail")).when(fineService).deleteFine(1L);

        mockMvc.perform(delete("/api/v1/fines/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error deleting fine: Delete fail"));

        verify(fineService, times(1)).deleteFine(1L);
    }

    // --- PUT /api/v1/fines/{id}/pay
    @Test
    void payFine_ShouldReturnPaidFine() throws Exception {
        FineEntity paidFine = createFine(1L, FineEntity.FineType.LATE_RETURN, 5000);
        paidFine.setPaid(true);

        when(fineService.payFine(1L)).thenReturn(paidFine);

        mockMvc.perform(put("/api/v1/fines/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));

        verify(fineService, times(1)).payFine(1L);
    }

    @Test
    void payFine_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.payFine(1L)).thenThrow(new RuntimeException("Payment error"));

        mockMvc.perform(put("/api/v1/fines/1/pay"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));

        verify(fineService, times(1)).payFine(1L);
    }

    // --- PUT /api/v1/fines/{id}/cancel
    @Test
    void cancelFine_ShouldReturnOk() throws Exception {
        doNothing().when(fineService).cancelFine(1L);

        mockMvc.perform(put("/api/v1/fines/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string("Fine cancelled successfully"));

        verify(fineService, times(1)).cancelFine(1L);
    }

    @Test
    void cancelFine_ShouldReturnError_WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Cancel error")).when(fineService).cancelFine(1L);

        mockMvc.perform(put("/api/v1/fines/1/cancel"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error cancelling fine: Cancel error"));

        verify(fineService, times(1)).cancelFine(1L);
    }

    // --- GET /api/v1/fines/client/{clientId}
    @Test
    void getFinesByClient_ShouldReturnFines_WhenClientExists() throws Exception {
        List<FineEntity> clientFines = Arrays.asList(testFine);

        when(clientService.getClientById(10L)).thenReturn(testClient);
        when(fineService.getFinesByClient(testClient)).thenReturn(clientFines);

        mockMvc.perform(get("/api/v1/fines/client/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(clientService, times(1)).getClientById(10L);
        verify(fineService, times(1)).getFinesByClient(testClient);
    }

    @Test
    void getFinesByClient_ShouldReturnNotFound_WhenClientNotExists() throws Exception {
        when(clientService.getClientById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/fines/client/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("Cliente no encontrado"));

        verify(clientService, times(1)).getClientById(999L);
        verify(fineService, times(0)).getFinesByClient(any(ClientEntity.class));
    }

    @Test
    void getFinesByClient_ShouldReturnBadRequest_WhenInvalidClientId() throws Exception {
        mockMvc.perform(get("/api/v1/fines/client/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("ID de cliente inválido"));

        verify(clientService, times(0)).getClientById(anyLong());
    }

    @Test
    void getFinesByClient_ShouldReturnError_WhenServiceFails() throws Exception {
        when(clientService.getClientById(10L)).thenReturn(testClient);
        when(fineService.getFinesByClient(testClient)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/fines/client/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(clientService, times(1)).getClientById(10L);
        verify(fineService, times(1)).getFinesByClient(testClient);
    }

    // --- GET /api/v1/fines/client/{clientId}/total-unpaid
    @Test
    void getTotalUnpaidAmount_ShouldReturnTotal() throws Exception {
        when(clientService.getClientById(10L)).thenReturn(testClient);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.valueOf(15000));

        mockMvc.perform(get("/api/v1/fines/client/10/total-unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnpaid").value(15000.0));

        verify(clientService, times(1)).getClientById(10L);
        verify(fineService, times(1)).getTotalUnpaidAmount(testClient);
    }

    @Test
    void getTotalUnpaidAmount_ShouldReturnZero_WhenClientNotExists() throws Exception {
        when(clientService.getClientById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/fines/client/999/total-unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnpaid").value(0.0));

        verify(clientService, times(1)).getClientById(999L);
        verify(fineService, times(0)).getTotalUnpaidAmount(any(ClientEntity.class));
    }

    @Test
    void getTotalUnpaidAmount_ShouldReturnZero_WhenServiceFails() throws Exception {
        when(clientService.getClientById(10L)).thenReturn(testClient);
        when(fineService.getTotalUnpaidAmount(testClient)).thenThrow(new RuntimeException("Calculation error"));

        mockMvc.perform(get("/api/v1/fines/client/10/total-unpaid"))
                .andExpect(status().isOk()) // Controller returns OK with zero amount on error
                .andExpect(jsonPath("$.totalUnpaid").value(0.0));

        verify(clientService, times(1)).getClientById(10L);
        verify(fineService, times(1)).getTotalUnpaidAmount(testClient);
    }

    // --- GET /api/v1/fines/type/{type}
    @Test
    void getFinesByType_ShouldReturnFines() throws Exception {
        List<FineEntity> typeFines = List.of(testFine);

        when(fineService.getFinesByType(FineEntity.FineType.LATE_RETURN)).thenReturn(typeFines);

        mockMvc.perform(get("/api/v1/fines/type/LATE_RETURN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(fineService, times(1)).getFinesByType(FineEntity.FineType.LATE_RETURN);
    }

    @Test
    void getFinesByType_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/fines/type/INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(0)).getFinesByType(any());
    }

    @Test
    void getFinesByType_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getFinesByType(FineEntity.FineType.LATE_RETURN)).thenThrow(new RuntimeException("DB Error"));

        mockMvc.perform(get("/api/v1/fines/type/LATE_RETURN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getFinesByType(FineEntity.FineType.LATE_RETURN);
    }

    // --- POST /api/v1/fines/{clientId}/check-restrictions
    @Test
    void checkClientRestrictions_ShouldReturnRestrictions() throws Exception {
        Map<String, Object> restrictions = Map.of(
                "canRequestLoan", true,
                "isRestricted", false,
                "clientStatus", "GOOD"
        );

        when(fineService.checkClientRestrictions(10L)).thenReturn(restrictions);

        mockMvc.perform(post("/api/v1/fines/10/check-restrictions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequestLoan").value(true));

        verify(fineService, times(1)).checkClientRestrictions(10L);
    }

    @Test
    void checkClientRestrictions_ShouldReturnErrorResponse_WhenServiceFails() throws Exception {
        when(fineService.checkClientRestrictions(10L)).thenThrow(new RuntimeException("Check failed"));

        mockMvc.perform(post("/api/v1/fines/10/check-restrictions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isRestricted").value(true))
                .andExpect(jsonPath("$.restrictionReason").exists())
                .andExpect(jsonPath("$.error").value(true));

        verify(fineService, times(1)).checkClientRestrictions(10L);
    }

    // --- GET /api/v1/fines/overdue
    @Test
    void getOverdueFines_ShouldReturnOverdueFines() throws Exception {
        FineEntity overdueFine = createFine(7L, FineEntity.FineType.LATE_RETURN, 100);
        List<FineEntity> overdueList = List.of(overdueFine);

        when(fineService.getOverdueFines()).thenReturn(overdueList);

        mockMvc.perform(get("/api/v1/fines/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(fineService, times(1)).getOverdueFines();
    }

    @Test
    void getOverdueFines_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getOverdueFines()).thenThrow(new RuntimeException("Overdue error"));

        mockMvc.perform(get("/api/v1/fines/overdue"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getOverdueFines();
    }

    // --- GET /api/v1/fines/unpaid
    @Test
    void getAllUnpaidFines_ShouldReturnUnpaidFines() throws Exception {
        FineEntity unpaidFine = createFine(8L, FineEntity.FineType.LATE_RETURN, 100);
        List<FineEntity> unpaidList = List.of(unpaidFine);
        unpaidFine.setPaid(false);

        when(fineService.getAllUnpaidFines()).thenReturn(unpaidList);

        mockMvc.perform(get("/api/v1/fines/unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(fineService, times(1)).getAllUnpaidFines();
    }

    @Test
    void getAllUnpaidFines_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getAllUnpaidFines()).thenThrow(new RuntimeException("Unpaid error"));

        mockMvc.perform(get("/api/v1/fines/unpaid"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getAllUnpaidFines();
    }

    // --- GET /api/v1/fines/statistics
    @Test
    void getFineStatistics_ShouldReturnStatistics() throws Exception {
        Map<String, Object> stats = Map.of("totalFines", 10, "totalUnpaidAmount", 500.0);

        when(fineService.getFineStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/fines/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFines").value(10));

        verify(fineService, times(1)).getFineStatistics();
    }

    @Test
    void getFineStatistics_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getFineStatistics()).thenThrow(new RuntimeException("Stats error"));

        mockMvc.perform(get("/api/v1/fines/statistics"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error getting statistics"));

        verify(fineService, times(1)).getFineStatistics();
    }

    // --- GET /api/v1/fines/date-range
    @Test
    void getFinesInDateRange_ShouldReturnFines() throws Exception {
        FineEntity fine = createFine(9L, FineEntity.FineType.LATE_RETURN, 100);
        List<FineEntity> fines = List.of(fine);

        // Use any() for the LocalDateTime arguments
        when(fineService.getFinesInDateRange(any(), any())).thenReturn(fines);

        mockMvc.perform(get("/api/v1/fines/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(fineService, times(1)).getFinesInDateRange(any(), any());
    }

    @Test
    void getFinesInDateRange_ShouldReturnError_WhenServiceFails() throws Exception {
        when(fineService.getFinesInDateRange(any(), any())).thenThrow(new RuntimeException("Date range error"));

        mockMvc.perform(get("/api/v1/fines/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fineService, times(1)).getFinesInDateRange(any(), any());
    }
}