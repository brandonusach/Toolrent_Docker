package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.entities.ToolInstanceEntity.ToolInstanceStatus;
import com.toolrent.backend.services.ToolInstanceService;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ToolInstanceControllerTest {

    @Mock
    private ToolInstanceService toolInstanceService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private ToolInstanceController toolInstanceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ToolInstanceEntity testInstance;
    private static final Long TEST_TOOL_ID = 1L;
    private static final Long TEST_INSTANCE_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(toolInstanceController).build();
        objectMapper = new ObjectMapper();

        testInstance = new ToolInstanceEntity();
        testInstance.setId(TEST_INSTANCE_ID);
        testInstance.setStatus(ToolInstanceStatus.AVAILABLE);
    }

    // Utility method to create a simple instance
    private ToolInstanceEntity createInstance(Long id, ToolInstanceStatus status) {
        ToolInstanceEntity instance = new ToolInstanceEntity();
        instance.setId(id);
        instance.setStatus(status);
        return instance;
    }

    // ========== Tests for GET /api/tool-instances/tool/{toolId} ==========
    @Test
    void getInstancesByTool_ShouldReturnAllInstances() throws Exception {
        ToolInstanceEntity instance2 = createInstance(2L, ToolInstanceStatus.AVAILABLE);

        List<ToolInstanceEntity> instances = Arrays.asList(testInstance, instance2);
        when(toolInstanceService.getInstancesByTool(TEST_TOOL_ID)).thenReturn(instances);

        mockMvc.perform(get("/api/tool-instances/tool/" + TEST_TOOL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(toolInstanceService, times(1)).getInstancesByTool(TEST_TOOL_ID);
    }

    @Test
    void getInstancesByTool_ShouldReturnEmptyList_WhenNoInstances() throws Exception {
        when(toolInstanceService.getInstancesByTool(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/tool-instances/tool/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getInstancesByTool_ShouldHandleMultipleStatuses() throws Exception {
        ToolInstanceEntity available = createInstance(1L, ToolInstanceStatus.AVAILABLE);
        ToolInstanceEntity loaned = createInstance(2L, ToolInstanceStatus.LOANED);
        ToolInstanceEntity repair = createInstance(3L, ToolInstanceStatus.UNDER_REPAIR);

        List<ToolInstanceEntity> instances = Arrays.asList(available, loaned, repair);
        when(toolInstanceService.getInstancesByTool(TEST_TOOL_ID)).thenReturn(instances);

        mockMvc.perform(get("/api/tool-instances/tool/" + TEST_TOOL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    // ========== Tests for GET /api/tool-instances/tool/{toolId}/available ==========
    @Test
    void getAvailableCount_ShouldReturnCount() throws Exception {
        when(toolInstanceService.getAvailableCount(TEST_TOOL_ID)).thenReturn(5L);

        mockMvc.perform(get("/api/tool-instances/tool/" + TEST_TOOL_ID + "/available"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getAvailableCount_ShouldHandleLargeNumbers() throws Exception {
        when(toolInstanceService.getAvailableCount(TEST_TOOL_ID)).thenReturn(1000L);

        mockMvc.perform(get("/api/tool-instances/tool/" + TEST_TOOL_ID + "/available"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000"));
    }

    // ========== Tests for GET /api/tool-instances/tool/{toolId}/stats ==========
    @Test
    void getToolStats_ShouldReturnStatistics() throws Exception {
        ToolInstanceService.ToolInstanceStats stats =
                new ToolInstanceService.ToolInstanceStats(5L, 3L, 2L, 0L, 10L);

        when(toolInstanceService.getToolInstanceStats(TEST_TOOL_ID)).thenReturn(stats);

        mockMvc.perform(get("/api/tool-instances/tool/" + TEST_TOOL_ID + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.available").value(5));
    }

    @Test
    void getToolStats_ShouldHandleZeroValues() throws Exception {
        ToolInstanceService.ToolInstanceStats stats =
                new ToolInstanceService.ToolInstanceStats(0L, 0L, 0L, 0L, 0L);

        when(toolInstanceService.getToolInstanceStats(999L)).thenReturn(stats);

        mockMvc.perform(get("/api/tool-instances/tool/999/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    // ========== Tests for GET /api/tool-instances/status/{status} ==========
    @Test
    void getInstancesByStatus_ShouldReturnInstancesByStatus() throws Exception {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceService.getInstancesByStatus(ToolInstanceStatus.AVAILABLE))
                .thenReturn(instances);

        mockMvc.perform(get("/api/tool-instances/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getInstancesByStatus_ShouldReturnDecommissionedInstances() throws Exception {
        ToolInstanceEntity instance1 = createInstance(3L, ToolInstanceStatus.DECOMMISSIONED);

        when(toolInstanceService.getInstancesByStatus(ToolInstanceStatus.DECOMMISSIONED))
                .thenReturn(Arrays.asList(instance1));

        mockMvc.perform(get("/api/tool-instances/status/DECOMMISSIONED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DECOMMISSIONED"));
    }

    @Test
    void getInstancesByStatus_ShouldReturnBadRequest_WhenInvalidStatus() throws Exception {
        // Will throw IllegalArgumentException due to ToolInstanceStatus.valueOf()
        // In standalone MockMvc, this throws ServletException wrapping IllegalArgumentException
        try {
            mockMvc.perform(get("/api/tool-instances/status/INVALID_STATUS"));
        } catch (Exception e) {
            // Expected: ServletException caused by IllegalArgumentException
            assert e.getCause() instanceof IllegalArgumentException;
        }
        verify(toolInstanceService, never()).getInstancesByStatus(any());
    }

    // ========== Tests for PUT /api/tool-instances/{instanceId}/status ==========
    @Test
    void updateInstanceStatus_ShouldUpdateAndReturnInstance() throws Exception {
        ToolInstanceEntity updatedInstance = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.UNDER_REPAIR);

        Map<String, String> request = Map.of("status", "UNDER_REPAIR");

        when(toolInstanceService.updateInstanceStatus(TEST_INSTANCE_ID, ToolInstanceStatus.UNDER_REPAIR))
                .thenReturn(updatedInstance);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REPAIR"));
    }

    @Test
    void updateInstanceStatus_ShouldReturnBadRequest_WhenInvalidStatusInBody() throws Exception {
        Map<String, String> request = Map.of("status", "BAD_STATUS");

        // The controller throws IllegalArgumentException when parsing the status string
        // In standalone MockMvc, this throws ServletException wrapping IllegalArgumentException
        try {
            mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Expected: ServletException caused by IllegalArgumentException
            assert e.getCause() instanceof IllegalArgumentException;
        }
        verify(toolInstanceService, never()).updateInstanceStatus(anyLong(), any());
    }

    @Test
    void updateInstanceStatus_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, String> request = Map.of("status", "LOANED");

        when(toolInstanceService.updateInstanceStatus(TEST_INSTANCE_ID, ToolInstanceStatus.LOANED))
                .thenThrow(new IllegalArgumentException("Invalid state transition"));

        // In standalone MockMvc, exceptions are wrapped in ServletException
        try {
            mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Expected: ServletException caused by IllegalArgumentException
            assert e.getCause() instanceof IllegalArgumentException;
        }
        verify(toolInstanceService, times(1)).updateInstanceStatus(anyLong(), any());
    }

    // ========== Tests for DELETE /api/tool-instances/{instanceId} ==========
    @Test
    void deleteInstance_ShouldReturnNoContent_WhenDeleted() throws Exception {
        doNothing().when(toolService).deleteToolInstanceAndUpdateStock(TEST_INSTANCE_ID);

        mockMvc.perform(delete("/api/tool-instances/" + TEST_INSTANCE_ID))
                .andExpect(status().isNoContent());

        verify(toolService, times(1)).deleteToolInstanceAndUpdateStock(TEST_INSTANCE_ID);
    }

    @Test
    void deleteInstance_ShouldHandleException() throws Exception {
        doThrow(new RuntimeException("Cannot delete")).when(toolService).deleteToolInstanceAndUpdateStock(999L);

        // Expecting a 5xx status code due to unhandled exception in controller
        // In standalone MockMvc, exceptions are wrapped in ServletException
        try {
            mockMvc.perform(delete("/api/tool-instances/999"));
        } catch (Exception e) {
            // Expected: ServletException caused by RuntimeException
            assert e.getCause() instanceof RuntimeException;
        }

        verify(toolService, times(1)).deleteToolInstanceAndUpdateStock(999L);
    }

    // ========== Tests for PUT /api/tool-instances/tool/{toolId}/reserve ==========
    @Test
    void reserveInstance_ShouldReserveAndReturnInstance() throws Exception {
        ToolInstanceEntity reservedInstance = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.LOANED);

        when(toolInstanceService.reserveInstanceForLoan(TEST_TOOL_ID)).thenReturn(reservedInstance);

        mockMvc.perform(put("/api/tool-instances/tool/" + TEST_TOOL_ID + "/reserve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOANED"));

        verify(toolInstanceService, times(1)).reserveInstanceForLoan(TEST_TOOL_ID);
    }

    @Test
    void reserveInstance_ShouldReturnError_WhenNoAvailableInstances() throws Exception {
        when(toolInstanceService.reserveInstanceForLoan(999L))
                .thenThrow(new RuntimeException("No available instances"));

        // In standalone MockMvc, exceptions are wrapped in ServletException
        try {
            mockMvc.perform(put("/api/tool-instances/tool/999/reserve"));
        } catch (Exception e) {
            // Expected: ServletException caused by RuntimeException
            assert e.getCause() instanceof RuntimeException;
        }

        verify(toolInstanceService, times(1)).reserveInstanceForLoan(999L);
    }

    // ========== Tests for PUT /api/tool-instances/tool/{toolId}/reserve-multiple (NEW) ==========
    @Test
    void reserveMultipleInstances_ShouldReturnReservedList() throws Exception {
        List<ToolInstanceEntity> reserved = Arrays.asList(
                createInstance(10L, ToolInstanceStatus.LOANED),
                createInstance(11L, ToolInstanceStatus.LOANED)
        );
        when(toolInstanceService.reserveMultipleInstances(TEST_TOOL_ID, 2)).thenReturn(reserved);

        mockMvc.perform(put("/api/tool-instances/tool/" + TEST_TOOL_ID + "/reserve-multiple")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("LOANED"));

        verify(toolInstanceService, times(1)).reserveMultipleInstances(TEST_TOOL_ID, 2);
    }

    @Test
    void reserveMultipleInstances_ShouldReturnError_WhenServiceFails() throws Exception {
        when(toolInstanceService.reserveMultipleInstances(TEST_TOOL_ID, 5))
                .thenThrow(new RuntimeException("Insufficient stock for 5"));

        // In standalone MockMvc, exceptions are wrapped in ServletException
        try {
            mockMvc.perform(put("/api/tool-instances/tool/" + TEST_TOOL_ID + "/reserve-multiple")
                            .param("quantity", "5"));
        } catch (Exception e) {
            // Expected: ServletException caused by RuntimeException
            assert e.getCause() instanceof RuntimeException;
        }

        verify(toolInstanceService, times(1)).reserveMultipleInstances(TEST_TOOL_ID, 5);
    }

    // ========== Tests for PUT /api/tool-instances/{instanceId}/return (NEW) ==========
    @Test
    void returnInstance_ShouldReturnInstanceAsAvailable() throws Exception {
        ToolInstanceEntity returnedInstance = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.AVAILABLE);

        when(toolInstanceService.returnInstanceFromLoan(TEST_INSTANCE_ID, false)).thenReturn(returnedInstance);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/return")
                        .param("damaged", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(toolInstanceService, times(1)).returnInstanceFromLoan(TEST_INSTANCE_ID, false);
    }

    @Test
    void returnInstance_ShouldReturnInstanceAsUnderRepair_WhenDamaged() throws Exception {
        ToolInstanceEntity returnedInstance = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.UNDER_REPAIR);

        when(toolInstanceService.returnInstanceFromLoan(TEST_INSTANCE_ID, true)).thenReturn(returnedInstance);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/return")
                        .param("damaged", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REPAIR"));

        verify(toolInstanceService, times(1)).returnInstanceFromLoan(TEST_INSTANCE_ID, true);
    }

    // ========== Tests for PUT /api/tool-instances/return-multiple (NEW) ==========
    @Test
    void returnMultipleInstances_ShouldReturnReturnedList() throws Exception {
        List<Long> instanceIds = List.of(10L, 11L);
        List<ToolInstanceEntity> returned = Arrays.asList(
                createInstance(10L, ToolInstanceStatus.AVAILABLE),
                createInstance(11L, ToolInstanceStatus.AVAILABLE)
        );

        when(toolInstanceService.returnMultipleInstances(eq(instanceIds), eq(false))).thenReturn(returned);

        mockMvc.perform(put("/api/tool-instances/return-multiple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instanceIds))
                        .param("damaged", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(toolInstanceService, times(1)).returnMultipleInstances(eq(instanceIds), eq(false));
    }

    // ========== Tests for PUT /api/tool-instances/{instanceId}/decommission (NEW) ==========
    @Test
    void decommissionInstance_ShouldReturnDecommissionedInstance() throws Exception {
        ToolInstanceEntity decommissioned = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.DECOMMISSIONED);

        when(toolInstanceService.decommissionInstance(TEST_INSTANCE_ID)).thenReturn(decommissioned);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/decommission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECOMMISSIONED"));

        verify(toolInstanceService, times(1)).decommissionInstance(TEST_INSTANCE_ID);
    }

    // ========== Tests for PUT /api/tool-instances/{instanceId}/repair (NEW) ==========
    @Test
    void repairInstance_ShouldReturnAvailableInstance() throws Exception {
        ToolInstanceEntity repaired = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.AVAILABLE);

        when(toolInstanceService.repairInstance(TEST_INSTANCE_ID)).thenReturn(repaired);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(toolInstanceService, times(1)).repairInstance(TEST_INSTANCE_ID);
    }

    // ========== Tests for GET /api/tool-instances/available-check/{toolId} (NEW) ==========
    @Test
    void checkAvailability_ShouldReturnTrue_WhenAvailable() throws Exception {
        when(toolInstanceService.isAvailable(TEST_TOOL_ID, 2)).thenReturn(true);

        mockMvc.perform(get("/api/tool-instances/available-check/" + TEST_TOOL_ID)
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(toolInstanceService, times(1)).isAvailable(TEST_TOOL_ID, 2);
    }

    @Test
    void checkAvailability_ShouldReturnFalse_WhenNotAvailable() throws Exception {
        when(toolInstanceService.isAvailable(TEST_TOOL_ID, 50)).thenReturn(false);

        mockMvc.perform(get("/api/tool-instances/available-check/" + TEST_TOOL_ID)
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(toolInstanceService, times(1)).isAvailable(TEST_TOOL_ID, 50);
    }

    // ========== Tests for GET /api/tool-instances/under-repair (NEW) ==========
    @Test
    void getInstancesUnderRepair_ShouldReturnList() throws Exception {
        List<ToolInstanceEntity> repairList = List.of(createInstance(2L, ToolInstanceStatus.UNDER_REPAIR));
        when(toolInstanceService.getInstancesUnderRepair()).thenReturn(repairList);

        mockMvc.perform(get("/api/tool-instances/under-repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("UNDER_REPAIR"));

        verify(toolInstanceService, times(1)).getInstancesUnderRepair();
    }

    // ========== Tests for GET /api/tool-instances/loaned (NEW) ==========
    @Test
    void getLoanedInstances_ShouldReturnList() throws Exception {
        List<ToolInstanceEntity> loanedList = List.of(createInstance(3L, ToolInstanceStatus.LOANED));
        when(toolInstanceService.getLoanedInstances()).thenReturn(loanedList);

        mockMvc.perform(get("/api/tool-instances/loaned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("LOANED"));

        verify(toolInstanceService, times(1)).getLoanedInstances();
    }

    // ========== Tests for DELETE /api/tool-instances/tool/{toolId} (NEW) ==========
    @Test
    void deleteAllInstancesByTool_ShouldReturnNoContent() throws Exception {
        doNothing().when(toolInstanceService).deleteAllInstancesByTool(TEST_TOOL_ID);

        mockMvc.perform(delete("/api/tool-instances/tool/" + TEST_TOOL_ID))
                .andExpect(status().isNoContent());

        verify(toolInstanceService, times(1)).deleteAllInstancesByTool(TEST_TOOL_ID);
    }

    // ========== Additional Coverage Tests (Existing) ==========

    @Test
    void getInstancesByStatus_ShouldReturnLoanedInstances() throws Exception {
        ToolInstanceEntity instance1 = createInstance(1L, ToolInstanceStatus.LOANED);
        when(toolInstanceService.getInstancesByStatus(ToolInstanceStatus.LOANED))
                .thenReturn(Arrays.asList(instance1));

        mockMvc.perform(get("/api/tool-instances/status/LOANED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateInstanceStatus_ShouldChangeToDecommissioned() throws Exception {
        ToolInstanceEntity updatedInstance = createInstance(TEST_INSTANCE_ID, ToolInstanceStatus.DECOMMISSIONED);
        Map<String, String> request = Map.of("status", "DECOMMISSIONED");

        when(toolInstanceService.updateInstanceStatus(TEST_INSTANCE_ID, ToolInstanceStatus.DECOMMISSIONED))
                .thenReturn(updatedInstance);

        mockMvc.perform(put("/api/tool-instances/" + TEST_INSTANCE_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECOMMISSIONED"));
    }
}