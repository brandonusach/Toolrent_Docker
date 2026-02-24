package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.dto.KardexMovementDTO;
import com.toolrent.backend.entities.KardexMovementEntity;
import com.toolrent.backend.entities.LoanEntity;
import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.services.KardexMovementService;
import com.toolrent.backend.services.ToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KardexMovementControllerTest {

    @Mock
    private KardexMovementService kardexMovementService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private KardexMovementController kardexMovementController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ToolEntity testTool;
    private KardexMovementEntity testMovement;
    private KardexMovementEntity testLoanMovement;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(kardexMovementController).build();
        objectMapper = new ObjectMapper();

        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Taladro");

        testMovement = new KardexMovementEntity();
        testMovement.setId(1L);
        testMovement.setType(KardexMovementEntity.MovementType.INITIAL_STOCK);
        testMovement.setQuantity(10);
        testMovement.setStockBefore(0);
        testMovement.setStockAfter(10);
        testMovement.setCreatedAt(LocalDateTime.now());

        testLoanMovement = new KardexMovementEntity();
        testLoanMovement.setId(2L);
        testLoanMovement.setType(KardexMovementEntity.MovementType.LOAN);
        testLoanMovement.setQuantity(2);
        testLoanMovement.setStockBefore(10);
        testLoanMovement.setStockAfter(8);
        testLoanMovement.setCreatedAt(LocalDateTime.now());
    }

    // Utility method to create a basic movement entity
    private KardexMovementEntity createMovement(Long id, KardexMovementEntity.MovementType type, int quantity) {
        KardexMovementEntity movement = new KardexMovementEntity();
        movement.setId(id);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setStockBefore(10);
        // Calculate stock after based on movement type
        int stockChange = switch (type) {
            case INITIAL_STOCK, RETURN, RESTOCK -> quantity;
            case LOAN, DECOMMISSION -> -quantity;
            case REPAIR -> 0;
        };
        movement.setStockAfter(10 + stockChange);
        movement.setCreatedAt(LocalDateTime.now());
        movement.setDescription(type.toString().toLowerCase() + " movement");
        return movement;
    }


    // ========== Tests for POST /api/kardex-movements/initial-stock (FIXED/IMPROVED) ==========

    @Test
    void createInitialStockMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 10, "userId", 1L);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createInitialStockMovement(any(ToolEntity.class), eq(10)))
                .thenReturn(testMovement);

        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("INITIAL_STOCK"));

        verify(toolService, times(1)).getToolById(1L);
    }

    @Test
    void createInitialStockMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of("toolId", 999L, "quantity", 10, "userId", 1L);

        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));

        verify(toolService, times(1)).getToolById(999L);
    }

    @Test
    void createInitialStockMovement_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 10, "userId", 1L);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createInitialStockMovement(any(ToolEntity.class), eq(10)))
                .thenThrow(new RuntimeException("Initial stock already exists"));

        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Initial stock already exists")); // Check RuntimeException message
    }

    // FIX for createInitialStockMovement_ShouldHandleLargeQuantity
    @Test
    void createInitialStockMovement_ShouldHandleLargeQuantity() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 1000, "userId", 1L);

        KardexMovementEntity largeMovement = createMovement(3L, KardexMovementEntity.MovementType.INITIAL_STOCK, 1000);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createInitialStockMovement(any(ToolEntity.class), eq(1000)))
                .thenReturn(largeMovement);

        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(1000));

        verify(kardexMovementService, times(1))
                .createInitialStockMovement(any(ToolEntity.class), eq(1000));
    }

    // FIX for createInitialStockMovement_ShouldHandleMinimalQuantity
    @Test
    void createInitialStockMovement_ShouldHandleMinimalQuantity() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 1, "userId", 1L);

        KardexMovementEntity movement = createMovement(4L, KardexMovementEntity.MovementType.INITIAL_STOCK, 1);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createInitialStockMovement(any(ToolEntity.class), eq(1)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(1));

        verify(kardexMovementService, times(1))
                .createInitialStockMovement(any(ToolEntity.class), eq(1));
    }

    // FIX for createInitialStockMovement_ShouldHandleNullPointerException
    @Test
    void createInitialStockMovement_ShouldHandleNullPointerException() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 10, "userId", 1L);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createInitialStockMovement(any(ToolEntity.class), eq(10)))
                .thenThrow(new NullPointerException("Null value"));

        // NullPointerException is a RuntimeException, caught by the first catch block (BAD_REQUEST)
        mockMvc.perform(post("/api/kardex-movements/initial-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Null value"));

        verify(kardexMovementService, times(1))
                .createInitialStockMovement(any(ToolEntity.class), eq(10));
    }

    // ========== Tests for POST /api/kardex-movements/loan (FIXED/IMPROVED) ==========

    @Test
    void createLoanMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 2, "description", "Loan movement", "loanId", 1L);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createLoanMovement(
                any(ToolEntity.class), eq(2), eq("Loan movement"), any(LoanEntity.class)))
                .thenReturn(testLoanMovement);

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("LOAN"));

        verify(kardexMovementService, times(1))
                .createLoanMovement(any(ToolEntity.class), eq(2), eq("Loan movement"), any(LoanEntity.class));
    }

    // FIX for createLoanMovement_ShouldHandleEmptyDescription
    @Test
    void createLoanMovement_ShouldHandleEmptyDescription() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 2, "description", "");

        KardexMovementEntity movement = createMovement(5L, KardexMovementEntity.MovementType.LOAN, 2);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createLoanMovement(
                any(ToolEntity.class), eq(2), eq(""), isNull()))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(kardexMovementService, times(1))
                .createLoanMovement(any(ToolEntity.class), eq(2), eq(""), isNull());
    }

    @Test
    void createLoanMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of("toolId", 999L, "quantity", 2, "description", "Loan movement");

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    @Test
    void createLoanMovement_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 2, "description", "Loan movement");

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createLoanMovement(
                any(ToolEntity.class), eq(2), eq("Loan movement"), any()))
                .thenThrow(new RuntimeException("Insufficient stock"));

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient stock"));
    }

    // FIX for createLoanMovement_ShouldHandleWithLoanId
    @Test
    void createLoanMovement_ShouldHandleWithLoanId() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 3, "description", "Loan with ID", "loanId", 5L);

        KardexMovementEntity movement = createMovement(6L, KardexMovementEntity.MovementType.LOAN, 3);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createLoanMovement(
                any(ToolEntity.class), eq(3), eq("Loan with ID"), any(LoanEntity.class)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(3));

        verify(kardexMovementService, times(1))
                .createLoanMovement(any(ToolEntity.class), eq(3), eq("Loan with ID"), any(LoanEntity.class));
    }

    // FIX for createLoanMovement_ShouldHandleWithoutLoanId
    @Test
    void createLoanMovement_ShouldHandleWithoutLoanId() throws Exception {
        Map<String, Object> request = Map.of("toolId", 1L, "quantity", 1, "description", "Manual loan");

        KardexMovementEntity movement = createMovement(7L, KardexMovementEntity.MovementType.LOAN, 1);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createLoanMovement(
                any(ToolEntity.class), eq(1), eq("Manual loan"), isNull()))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(kardexMovementService, times(1))
                .createLoanMovement(any(ToolEntity.class), eq(1), eq("Manual loan"), isNull());
    }


    // ========== Tests for POST /api/kardex-movements/return ==========

    @Test
    void createReturnMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 2, "description", "Return ok",
                "userId", 1L, "loanId", 10L, "instanceIds", List.of(101L, 102L),
                "isDamaged", false
        );
        KardexMovementEntity movement = createMovement(8L, KardexMovementEntity.MovementType.RETURN, 2);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createReturnMovement(
                any(ToolEntity.class), eq(2), eq("Return ok"), any(LoanEntity.class),
                anyList(), eq(false)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RETURN"));

        verify(kardexMovementService, times(1))
                .createReturnMovement(any(ToolEntity.class), eq(2), eq("Return ok"), any(LoanEntity.class), anyList(), eq(false));
    }

    @Test
    void createReturnMovement_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 2, "description", "Return ok",
                "userId", 1L, "loanId", 10L, "instanceIds", List.of(101L, 102L),
                "isDamaged", true
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createReturnMovement(
                any(ToolEntity.class), eq(2), anyString(), any(LoanEntity.class),
                anyList(), eq(true)))
                .thenThrow(new RuntimeException("Instances mismatch"));

        mockMvc.perform(post("/api/kardex-movements/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Instances mismatch"));
    }

    @Test
    void createReturnMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 999L, "quantity", 2, "description", "Return ok",
                "userId", 1L, "loanId", 10L, "instanceIds", List.of(101L, 102L),
                "isDamaged", false
        );

        mockMvc.perform(post("/api/kardex-movements/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    @Test
    void createReturnMovement_ShouldHandleNullIsDamaged() throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("toolId", 1L);
        request.put("quantity", 2);
        request.put("description", "Return");
        request.put("userId", 1L);
        request.put("loanId", 10L);
        request.put("instanceIds", List.of(101L, 102L));
        // isDamaged is null

        KardexMovementEntity movement = createMovement(8L, KardexMovementEntity.MovementType.RETURN, 2);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createReturnMovement(
                any(ToolEntity.class), eq(2), anyString(), any(LoanEntity.class),
                anyList(), eq(false)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // ========== Tests for POST /api/kardex-movements/decommission ==========

    @Test
    void createDecommissionMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 1, "description", "Broken",
                "userId", 1L, "instanceIds", List.of(201L)
        );
        KardexMovementEntity movement = createMovement(9L, KardexMovementEntity.MovementType.DECOMMISSION, 1);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createDecommissionMovement(
                any(ToolEntity.class), eq(1), eq("Broken"), anyList(), any()))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/decommission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DECOMMISSION"));
    }

    @Test
    void createDecommissionMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 999L, "quantity", 1, "description", "Broken",
                "userId", 1L, "instanceIds", List.of(201L)
        );

        mockMvc.perform(post("/api/kardex-movements/decommission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    @Test
    void createDecommissionMovement_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 1, "description", "Broken",
                "userId", 1L, "instanceIds", List.of(201L)
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createDecommissionMovement(
                any(ToolEntity.class), eq(1), eq("Broken"), anyList(), any()))
                .thenThrow(new RuntimeException("Invalid instances"));

        mockMvc.perform(post("/api/kardex-movements/decommission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid instances"));
    }

    // ========== Tests for POST /api/kardex-movements/restock ==========

    @Test
    void createRestockMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 5, "description", "New purchase",
                "userId", 1L
        );
        KardexMovementEntity movement = createMovement(10L, KardexMovementEntity.MovementType.RESTOCK, 5);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createRestockMovement(
                any(ToolEntity.class), eq(5), eq("New purchase"), any()))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RESTOCK"));
    }

    @Test
    void createRestockMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 999L, "quantity", 5, "description", "New purchase",
                "userId", 1L
        );

        mockMvc.perform(post("/api/kardex-movements/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    @Test
    void createRestockMovement_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "quantity", 5, "description", "New purchase",
                "userId", 1L
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createRestockMovement(
                any(ToolEntity.class), eq(5), eq("New purchase"), any()))
                .thenThrow(new RuntimeException("Validation failed"));

        mockMvc.perform(post("/api/kardex-movements/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Validation failed"));
    }

    // ========== Tests for POST /api/kardex-movements/repair ==========

    @Test
    void createRepairMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "description", "Instance 301 repaired",
                "userId", 1L, "instanceId", 301L
        );
        KardexMovementEntity movement = createMovement(11L, KardexMovementEntity.MovementType.REPAIR, 0);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createRepairMovement(
                any(ToolEntity.class), eq("Instance 301 repaired"), eq(301L)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REPAIR"));
    }

    @Test
    void createRepairMovement_ShouldReturnBadRequest_WhenInstanceIdIsNull() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "description", "Generic repair",
                "userId", 1L
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createRepairMovement(
                any(ToolEntity.class), anyString(), isNull()))
                .thenThrow(new RuntimeException("Instance ID is required"));

        mockMvc.perform(post("/api/kardex-movements/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Instance ID is required"));
    }

    @Test
    void createRepairMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 999L, "description", "Repair",
                "userId", 1L, "instanceId", 301L
        );

        mockMvc.perform(post("/api/kardex-movements/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    // ========== Tests for POST /api/kardex-movements/general ==========

    @Test
    void createGeneralMovement_ShouldCreateAndReturnMovement() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "type", "RESTOCK", "quantity", 1,
                "description", "Inventory adjustment", "userId", 1L
        );
        KardexMovementEntity movement = createMovement(12L, KardexMovementEntity.MovementType.RESTOCK, 1);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createMovement(
                any(ToolEntity.class), eq(KardexMovementEntity.MovementType.RESTOCK),
                eq(1), eq("Inventory adjustment"), isNull()))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RESTOCK"));
    }

    @Test
    void createGeneralMovement_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "type", "INVALID_TYPE", "quantity", 1,
                "description", "Test", "userId", 1L
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));

        mockMvc.perform(post("/api/kardex-movements/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid movement type"));
    }

    @Test
    void createGeneralMovement_ShouldReturnBadRequest_WhenToolNotFound() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 999L, "type", "RESTOCK", "quantity", 1,
                "description", "Test", "userId", 1L
        );

        mockMvc.perform(post("/api/kardex-movements/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    @Test
    void createGeneralMovement_ShouldHandleWithLoanId() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "type", "LOAN", "quantity", 2,
                "description", "Loan with ID", "userId", 1L, "loanId", 5L
        );
        KardexMovementEntity movement = createMovement(13L, KardexMovementEntity.MovementType.LOAN, 2);

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createMovement(
                any(ToolEntity.class), eq(KardexMovementEntity.MovementType.LOAN),
                eq(2), eq("Loan with ID"), any(LoanEntity.class)))
                .thenReturn(movement);

        mockMvc.perform(post("/api/kardex-movements/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(kardexMovementService, times(1))
                .createMovement(any(ToolEntity.class), eq(KardexMovementEntity.MovementType.LOAN),
                        eq(2), eq("Loan with ID"), any(LoanEntity.class));
    }

    @Test
    void createGeneralMovement_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = Map.of(
                "toolId", 1L, "type", "LOAN", "quantity", 10,
                "description", "Test", "userId", 1L
        );

        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.createMovement(
                any(ToolEntity.class), eq(KardexMovementEntity.MovementType.LOAN),
                eq(10), eq("Test"), isNull()))
                .thenThrow(new RuntimeException("Insufficient stock"));

        mockMvc.perform(post("/api/kardex-movements/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient stock"));
    }

    // ========== Tests for GET Endpoints ==========

    // --- GET /api/kardex-movements
    @Test
    void getAllMovements_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement, testLoanMovement);

        // Mock the DTO conversion to avoid real DTO issues and verify flow
        when(kardexMovementService.getAllMovements()).thenReturn(movements);

        // As DTO conversion is done within the controller, we mock the DTO creation logic
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO())
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        verify(kardexMovementService, times(1)).getAllMovements();
    }

    @Test
    void getAllMovements_ShouldHandleDTOConversionError() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement);
        when(kardexMovementService.getAllMovements()).thenReturn(movements);

        // Mock DTO conversion to throw exception - controller should handle it gracefully
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenThrow(new RuntimeException("Conversion error"));

            mockMvc.perform(get("/api/kardex-movements"))
                    .andExpect(status().isOk()); // Controller creates basic DTO on error
        }

        verify(kardexMovementService, times(1)).getAllMovements();
    }

    @Test
    void getAllMovements_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(kardexMovementService.getAllMovements()).thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/kardex-movements"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Error retrieving movements")));
    }

    // --- GET /api/kardex-movements/{id}
    @Test
    void getMovementById_ShouldReturnMovement() throws Exception {
        when(kardexMovementService.getMovementById(1L)).thenReturn(testMovement);

        mockMvc.perform(get("/api/kardex-movements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(kardexMovementService, times(1)).getMovementById(1L);
    }

    @Test
    void getMovementById_ShouldReturnNotFound_WhenMovementNotExists() throws Exception {
        when(kardexMovementService.getMovementById(999L)).thenThrow(new RuntimeException("Movement not found"));

        mockMvc.perform(get("/api/kardex-movements/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Movement not found"));

        verify(kardexMovementService, times(1)).getMovementById(999L);
    }


    // --- GET /api/kardex-movements/tool/{toolId}
    @Test
    void getMovementHistoryByTool_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement);
        when(kardexMovementService.getMovementHistoryByTool(1L)).thenReturn(movements);

        // Mock DTO conversion
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements/tool/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(kardexMovementService, times(1)).getMovementHistoryByTool(1L);
    }

    @Test
    void getMovementHistoryByTool_ShouldReturnBadRequest_WhenRuntimeExceptionOccurs() throws Exception {
        when(kardexMovementService.getMovementHistoryByTool(999L))
                .thenThrow(new RuntimeException("Tool not found"));

        mockMvc.perform(get("/api/kardex-movements/tool/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tool not found"));
    }

    // --- GET /api/kardex-movements/date-range
    @Test
    void getMovementsByDateRange_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement);
        when(kardexMovementService.getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(movements);

        // Mock DTO conversion
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements/date-range")
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(kardexMovementService, times(1)).getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getMovementsByDateRange_ShouldReturnInternalServerError_OnParsingError() throws Exception {
        mockMvc.perform(get("/api/kardex-movements/date-range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("Error parsing dates or retrieving movements:")));
    }

    // --- GET /api/kardex-movements/date-range-datetime
    @Test
    void getMovementsByDateTimeRange_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement);
        when(kardexMovementService.getMovementsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(movements);

        // Mock DTO conversion
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements/date-range-datetime")
                            .param("startDateTime", "2024-01-01T00:00:00")
                            .param("endDateTime", "2024-01-31T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(kardexMovementService, times(1)).getMovementsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getMovementsByDateTimeRange_ShouldReturnInternalServerError_OnParsingError() throws Exception {
        mockMvc.perform(get("/api/kardex-movements/date-range-datetime")
                        .param("startDateTime", "invalid-datetime")
                        .param("endDateTime", "2024-01-31T23:59:59"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Error parsing datetimes or retrieving movements")));
    }

    // --- GET /api/kardex-movements/type/{type}
    @Test
    void getMovementsByType_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testMovement);
        when(kardexMovementService.getMovementsByType(KardexMovementEntity.MovementType.INITIAL_STOCK))
                .thenReturn(movements);

        // Mock DTO conversion
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements/type/INITIAL_STOCK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(kardexMovementService, times(1)).getMovementsByType(KardexMovementEntity.MovementType.INITIAL_STOCK);
    }

    @Test
    void getMovementsByType_ShouldReturnBadRequest_WhenInvalidType() throws Exception {
        mockMvc.perform(get("/api/kardex-movements/type/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid movement type: INVALID"));
    }

    @Test
    void getMovementsByType_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
        when(kardexMovementService.getMovementsByType(KardexMovementEntity.MovementType.INITIAL_STOCK))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/kardex-movements/type/INITIAL_STOCK"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving movements by type"));
    }

    // --- GET /api/kardex-movements/loan/{loanId}
    @Test
    void getMovementsByLoanId_ShouldReturnListOfDTOs() throws Exception {
        List<KardexMovementEntity> movements = List.of(testLoanMovement);
        when(kardexMovementService.getMovementsByLoanId(10L)).thenReturn(movements);

        // Mock DTO conversion
        try (var mockedStatic = mockStatic(KardexMovementDTO.class)) {
            mockedStatic.when(() -> KardexMovementDTO.fromEntity(any(KardexMovementEntity.class)))
                    .thenReturn(new KardexMovementDTO());

            mockMvc.perform(get("/api/kardex-movements/loan/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(kardexMovementService, times(1)).getMovementsByLoanId(10L);
    }

    @Test
    void getMovementsByLoanId_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
        when(kardexMovementService.getMovementsByLoanId(10L))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/kardex-movements/loan/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving movements by loan"));
    }

    // --- GET /api/kardex-movements/tool/{toolId}/verify-consistency
    @Test
    void verifyStockConsistency_ShouldReturnTrue() throws Exception {
        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(kardexMovementService.verifyStockConsistency(testTool)).thenReturn(true);

        mockMvc.perform(get("/api/kardex-movements/tool/1/verify-consistency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true));

        verify(kardexMovementService, times(1)).verifyStockConsistency(testTool);
    }

    @Test
    void verifyStockConsistency_ShouldReturnNotFound_WhenToolNotExists() throws Exception {
        mockMvc.perform(get("/api/kardex-movements/tool/999/verify-consistency"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tool not found"));
    }

    // --- GET /api/kardex-movements/tool/{toolId}/audit-report
    @Test
    void generateAuditReport_ShouldReturnReport() throws Exception {
        KardexMovementService.KardexAuditReport mockReport = mock(KardexMovementService.KardexAuditReport.class);
        when(kardexMovementService.generateAuditReport(1L)).thenReturn(mockReport);

        mockMvc.perform(get("/api/kardex-movements/tool/1/audit-report"))
                .andExpect(status().isOk());

        verify(kardexMovementService, times(1)).generateAuditReport(1L);
    }

    @Test
    void generateAuditReport_ShouldReturnNotFound_WhenServiceFails() throws Exception {
        when(kardexMovementService.generateAuditReport(999L)).thenThrow(new RuntimeException("Tool not found"));

        mockMvc.perform(get("/api/kardex-movements/tool/999/audit-report"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Tool not found"));
    }

    // --- GET /api/kardex-movements/recent
    @Test
    void getRecentMovements_ShouldReturnLimitedList() throws Exception {
        List<KardexMovementEntity> allMovements = Arrays.asList(
                testMovement, testLoanMovement, createMovement(13L, KardexMovementEntity.MovementType.RESTOCK, 1)
        );
        when(kardexMovementService.getAllMovements()).thenReturn(allMovements);

        mockMvc.perform(get("/api/kardex-movements/recent").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1)); // Should return the first two
    }

    @Test
    void getRecentMovements_ShouldReturnInternalServerError_OnException() throws Exception {
        when(kardexMovementService.getAllMovements()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/kardex-movements/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getRecentMovements_ShouldUseDefaultLimit() throws Exception {
        List<KardexMovementEntity> allMovements = Arrays.asList(
                testMovement, testLoanMovement
        );
        when(kardexMovementService.getAllMovements()).thenReturn(allMovements);

        mockMvc.perform(get("/api/kardex-movements/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- GET /api/kardex-movements/statistics/by-type
    @Test
    void getMovementStatisticsByType_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(get("/api/kardex-movements/statistics/by-type"))
                .andExpect(status().isNotImplemented());
    }
}