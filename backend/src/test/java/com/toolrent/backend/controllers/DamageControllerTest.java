package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.DamageEntity;
import com.toolrent.backend.entities.LoanEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.services.DamageService;
import com.toolrent.backend.services.LoanService;
import com.toolrent.backend.services.ToolInstanceService;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DamageControllerTest {

    @Mock
    private DamageService damageService;

    @Mock
    private LoanService loanService;

    @Mock
    private ToolInstanceService toolInstanceService;

    @InjectMocks
    private DamageController damageController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private DamageEntity testDamage;
    private LoanEntity testLoan;
    private ToolInstanceEntity testInstance;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(damageController).build();
        objectMapper = new ObjectMapper();

        testLoan = new LoanEntity();
        testLoan.setId(1L);

        testInstance = new ToolInstanceEntity();
        testInstance.setId(1L);
        testInstance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        testDamage = new DamageEntity();
        testDamage.setId(1L);
        testDamage.setDescription("Damage description");
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        testDamage.setIsRepairable(true);
        testDamage.setType(DamageEntity.DamageType.MINOR);
    }

    // ========== Tests for POST /api/damages/report ==========
    @Test
    void reportDamage_ShouldCreateAndReturnDamage() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("loanId", 1L);
        request.put("toolInstanceId", 1L);
        request.put("description", "Tool is broken");

        when(loanService.getLoanById(1L)).thenReturn(testLoan);
        when(toolInstanceService.getInstanceById(1L)).thenReturn(testInstance);
        when(damageService.reportDamage(any(LoanEntity.class),
                any(ToolInstanceEntity.class), eq("Tool is broken")))
                .thenReturn(testDamage);

        mockMvc.perform(post("/api/damages/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Damage reported successfully"))
                .andExpect(jsonPath("$.damageId").value(1))
                .andExpect(jsonPath("$.status").value("REPORTED"));

        verify(loanService, times(1)).getLoanById(1L);
        verify(toolInstanceService, times(1)).getInstanceById(1L);
        verify(damageService, times(1))
                .reportDamage(any(LoanEntity.class), any(ToolInstanceEntity.class), eq("Tool is broken"));
    }

    @Test
    void reportDamage_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("loanId", 1L);
        request.put("toolInstanceId", 1L);
        request.put("description", "Tool is broken");

        when(loanService.getLoanById(1L)).thenReturn(testLoan);
        when(toolInstanceService.getInstanceById(1L)).thenReturn(testInstance);
        when(damageService.reportDamage(any(LoanEntity.class),
                any(ToolInstanceEntity.class), anyString()))
                .thenThrow(new RuntimeException("Cannot report damage"));

        mockMvc.perform(post("/api/damages/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot report damage"));

        verify(damageService, times(1))
                .reportDamage(any(LoanEntity.class), any(ToolInstanceEntity.class), anyString());
    }

    @Test
    void reportDamage_ShouldReturnBadRequest_WhenLoanNotFound() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("loanId", 999L);
        request.put("toolInstanceId", 1L);
        request.put("description", "Test");

        when(loanService.getLoanById(999L))
                .thenThrow(new RuntimeException("Loan not found"));

        mockMvc.perform(post("/api/damages/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(loanService, times(1)).getLoanById(999L);
    }

    @Test
    void reportDamage_ShouldReturnBadRequest_WhenToolInstanceNotFound() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("loanId", 1L);
        request.put("toolInstanceId", 999L);
        request.put("description", "Test");

        when(loanService.getLoanById(1L)).thenReturn(testLoan);
        when(toolInstanceService.getInstanceById(999L))
                .thenThrow(new RuntimeException("Tool instance not found"));

        mockMvc.perform(post("/api/damages/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(toolInstanceService, times(1)).getInstanceById(999L);
    }

    // ========== Tests for PUT /api/damages/{damageId}/assess ==========
    @Test
    void assessDamage_ShouldAssessAndReturnDamage() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("damageType", "MINOR");
        request.put("assessmentDescription", "Small scratch");
        request.put("repairCost", "5000.00");
        request.put("isRepairable", true);

        DamageEntity assessedDamage = new DamageEntity();
        assessedDamage.setId(1L);
        assessedDamage.setType(DamageEntity.DamageType.MINOR);
        assessedDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        assessedDamage.setRepairCost(new BigDecimal("5000.00"));
        assessedDamage.setIsRepairable(true);

        when(damageService.assessDamage(eq(1L), eq(DamageEntity.DamageType.MINOR),
                eq("Small scratch"), any(BigDecimal.class), eq(true)))
                .thenReturn(assessedDamage);

        mockMvc.perform(put("/api/damages/1/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Damage assessed successfully"))
                .andExpect(jsonPath("$.damage.type").value("MINOR"))
                .andExpect(jsonPath("$.damage.status").value("ASSESSED"));

        verify(damageService, times(1))
                .assessDamage(eq(1L), eq(DamageEntity.DamageType.MINOR),
                        eq("Small scratch"), any(BigDecimal.class), eq(true));
    }

    @Test
    void assessDamage_ShouldReturnBadRequest_WhenServiceFails() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("damageType", "MINOR");
        request.put("assessmentDescription", "Small scratch");
        request.put("repairCost", "5000.00");
        request.put("isRepairable", true);

        when(damageService.assessDamage(anyLong(), any(DamageEntity.DamageType.class),
                anyString(), any(BigDecimal.class), anyBoolean()))
                .thenThrow(new RuntimeException("Damage not found"));

        mockMvc.perform(put("/api/damages/999/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Damage not found"));

        verify(damageService, times(1))
                .assessDamage(anyLong(), any(DamageEntity.DamageType.class),
                        anyString(), any(BigDecimal.class), anyBoolean());
    }

    @Test
    void assessDamage_ShouldHandleMajorDamageType() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("damageType", "MAJOR");
        request.put("assessmentDescription", "Completely broken");
        request.put("repairCost", "50000.00");
        request.put("isRepairable", true);

        DamageEntity majorDamage = new DamageEntity();
        majorDamage.setId(1L);
        majorDamage.setType(DamageEntity.DamageType.MAJOR);
        majorDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        majorDamage.setIsRepairable(true);

        when(damageService.assessDamage(eq(1L), eq(DamageEntity.DamageType.MAJOR),
                eq("Completely broken"), any(BigDecimal.class), eq(true)))
                .thenReturn(majorDamage);

        mockMvc.perform(put("/api/damages/1/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damage.type").value("MAJOR"));

        verify(damageService, times(1))
                .assessDamage(eq(1L), eq(DamageEntity.DamageType.MAJOR),
                        eq("Completely broken"), any(BigDecimal.class), eq(true));
    }

    @Test
    void assessDamage_ShouldHandleZeroRepairCost() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("damageType", "MINOR");
        request.put("assessmentDescription", "Very minor");
        request.put("repairCost", "0.00");
        request.put("isRepairable", true);

        DamageEntity damage = new DamageEntity();
        damage.setId(1L);
        damage.setRepairCost(BigDecimal.ZERO);
        damage.setIsRepairable(true);

        when(damageService.assessDamage(eq(1L), any(DamageEntity.DamageType.class),
                anyString(), any(BigDecimal.class), eq(true)))
                .thenReturn(damage);

        mockMvc.perform(put("/api/damages/1/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(damageService, times(1))
                .assessDamage(anyLong(), any(DamageEntity.DamageType.class),
                        anyString(), any(BigDecimal.class), anyBoolean());
    }

    // ========== Tests for PUT /api/damages/{damageId}/start-repair ==========
    @Test
    void startRepair_ShouldStartRepairSuccessfully() throws Exception {
        DamageEntity repairing = new DamageEntity();
        repairing.setId(1L);
        repairing.setStatus(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS);
        repairing.setIsRepairable(true);

        when(damageService.startRepair(1L)).thenReturn(repairing);

        mockMvc.perform(put("/api/damages/1/start-repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repair started successfully"))
                .andExpect(jsonPath("$.status").value("REPAIR_IN_PROGRESS"));

        verify(damageService, times(1)).startRepair(1L);
    }

    @Test
    void startRepair_ShouldReturnBadRequest_WhenFails() throws Exception {
        when(damageService.startRepair(999L))
                .thenThrow(new RuntimeException("Damage not found"));

        mockMvc.perform(put("/api/damages/999/start-repair"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Damage not found"));

        verify(damageService, times(1)).startRepair(999L);
    }

    // ========== Tests for PUT /api/damages/{damageId}/complete-repair ==========
    @Test
    void completeRepair_ShouldCompleteRepairSuccessfully() throws Exception {
        DamageEntity repaired = new DamageEntity();
        repaired.setId(1L);
        repaired.setStatus(DamageEntity.DamageStatus.REPAIRED);
        repaired.setIsRepairable(true);

        when(damageService.completeRepair(1L)).thenReturn(repaired);

        mockMvc.perform(put("/api/damages/1/complete-repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repair completed successfully"))
                .andExpect(jsonPath("$.status").value("REPAIRED"));

        verify(damageService, times(1)).completeRepair(1L);
    }

    @Test
    void completeRepair_ShouldReturnBadRequest_WhenFails() throws Exception {
        when(damageService.completeRepair(999L))
                .thenThrow(new RuntimeException("Cannot complete repair"));

        mockMvc.perform(put("/api/damages/999/complete-repair"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot complete repair"));

        verify(damageService, times(1)).completeRepair(999L);
    }

    // ========== Tests for GET endpoints ==========
    @Test
    void getAllDamages_ShouldReturnDamageList() throws Exception {
        DamageEntity damage2 = new DamageEntity();
        damage2.setId(2L);
        damage2.setIsRepairable(false);
        damage2.setStatus(DamageEntity.DamageStatus.ASSESSED);
        damage2.setType(DamageEntity.DamageType.MAJOR);

        List<DamageEntity> damages = Arrays.asList(testDamage, damage2);
        when(damageService.getAllDamages()).thenReturn(damages);

        mockMvc.perform(get("/api/damages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(damageService, times(1)).getAllDamages();
    }

    @Test
    void getDamageById_ShouldReturnDamage() throws Exception {
        when(damageService.getDamageById(1L)).thenReturn(testDamage);

        mockMvc.perform(get("/api/damages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(damageService, times(1)).getDamageById(1L);
    }

    @Test
    void getDamageById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(damageService.getDamageById(999L))
                .thenThrow(new RuntimeException("Damage not found"));

        mockMvc.perform(get("/api/damages/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Damage not found"));

        verify(damageService, times(1)).getDamageById(999L);
    }

    @Test
    void getDamagesByLoan_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByLoanId(1L)).thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getDamagesByLoanId(1L);
    }

    @Test
    void getDamagesByToolInstance_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByToolInstanceId(1L)).thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-tool-instance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getDamagesByToolInstanceId(1L);
    }

    @Test
    void getDamagesByTool_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByToolId(1L)).thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-tool/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getDamagesByToolId(1L);
    }

    @Test
    void getDamagesByClient_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByClientId(1L)).thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-client/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getDamagesByClientId(1L);
    }

    @Test
    void getDamagesByStatus_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByStatus(DamageEntity.DamageStatus.REPORTED))
                .thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-status/REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1))
                .getDamagesByStatus(DamageEntity.DamageStatus.REPORTED);
    }

    @Test
    void getDamagesByStatus_ShouldReturnBadRequest_WithInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/damages/by-status/INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDamagesByType_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesByType(DamageEntity.DamageType.MINOR))
                .thenReturn(damages);

        mockMvc.perform(get("/api/damages/by-type/MINOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1))
                .getDamagesByType(DamageEntity.DamageType.MINOR);
    }

    @Test
    void getDamagesByType_ShouldReturnBadRequest_WithInvalidType() throws Exception {
        mockMvc.perform(get("/api/damages/by-type/INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ========== Tests for dashboard endpoints ==========
    @Test
    void getPendingAssessments_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getPendingAssessments()).thenReturn(damages);

        mockMvc.perform(get("/api/damages/pending-assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getPendingAssessments();
    }

    @Test
    void getDamagesUnderRepair_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getDamagesUnderRepair()).thenReturn(damages);

        mockMvc.perform(get("/api/damages/under-repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getDamagesUnderRepair();
    }

    @Test
    void getIrreparableDamages_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getIrreparableDamages()).thenReturn(damages);

        mockMvc.perform(get("/api/damages/irreparable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getIrreparableDamages();
    }

    @Test
    void getUrgentDamages_ShouldReturnDamageList() throws Exception {
        List<DamageEntity> damages = Arrays.asList(testDamage);
        when(damageService.getUrgentDamages()).thenReturn(damages);

        mockMvc.perform(get("/api/damages/urgent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(damageService, times(1)).getUrgentDamages();
    }

    // ========== Tests for utility endpoints ==========
    @Test
    void checkPendingDamages_ShouldReturnTrue() throws Exception {
        when(damageService.hasPendingDamages(1L)).thenReturn(true);

        mockMvc.perform(get("/api/damages/tool-instance/1/has-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPendingDamages").value(true));

        verify(damageService, times(1)).hasPendingDamages(1L);
    }

    @Test
    void checkLoanDamages_ShouldReturnTrue() throws Exception {
        when(damageService.loanHasDamages(1L)).thenReturn(true);

        mockMvc.perform(get("/api/damages/loan/1/has-damages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasDamages").value(true));

        verify(damageService, times(1)).loanHasDamages(1L);
    }

    @Test
    void countDamagesByStatus_ShouldReturnCount() throws Exception {
        when(damageService.countDamagesByStatus(DamageEntity.DamageStatus.REPORTED))
                .thenReturn(5L);

        mockMvc.perform(get("/api/damages/count/by-status/REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(damageService, times(1))
                .countDamagesByStatus(DamageEntity.DamageStatus.REPORTED);
    }

    @Test
    void countDamagesByStatus_ShouldReturnBadRequest_WithInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/damages/count/by-status/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid status: INVALID"));
    }

    @Test
    void countDamagesByClient_ShouldReturnCounts() throws Exception {
        when(damageService.countDamagesByClient(1L)).thenReturn(10L);
        when(damageService.countIrreparableDamagesByClient(1L)).thenReturn(2L);

        mockMvc.perform(get("/api/damages/count/by-client/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDamages").value(10))
                .andExpect(jsonPath("$.irreparableDamages").value(2));

        verify(damageService, times(1)).countDamagesByClient(1L);
        verify(damageService, times(1)).countIrreparableDamagesByClient(1L);
    }
}