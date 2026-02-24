package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.KardexMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field; // Import necesario para la corrección
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KardexMovementServiceTest {

    @Mock
    private KardexMovementRepository kardexMovementRepository;

    @Mock
    private ToolInstanceService toolInstanceService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private KardexMovementService kardexMovementService;

    private ToolEntity testTool;
    private ToolInstanceEntity testInstance;
    private LoanEntity testLoan;
    private KardexMovementEntity testMovement;

    @BeforeEach
    void setUp() {
        // --- INYECCIÓN MANUAL DE DEPENDENCIA (CORRECCIÓN) ---
        // Como ToolService se inyecta por campo (@Autowired @Lazy) y no por constructor,
        // Mockito a veces falla en inyectarlo automáticamente cuando hay mezcla de estrategias.
        try {
            Field toolServiceField = KardexMovementService.class.getDeclaredField("toolService");
            toolServiceField.setAccessible(true);
            toolServiceField.set(kardexMovementService, toolService);
        } catch (Exception e) {
            throw new RuntimeException("Error al inyectar toolService mock mediante reflexión", e);
        }
        // -----------------------------------------------------

        // Setup Tool
        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Test Tool");
        testTool.setCurrentStock(10);
        testTool.setInitialStock(10);

        // Setup Instance
        testInstance = new ToolInstanceEntity();
        testInstance.setId(1L);
        testInstance.setTool(testTool);
        testInstance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        // Setup Loan
        testLoan = new LoanEntity();
        testLoan.setId(1L);
        testLoan.setTool(testTool);
        testLoan.setQuantity(1);

        // Setup Movement
        testMovement = new KardexMovementEntity();
        testMovement.setId(1L);
        testMovement.setTool(testTool);
        testMovement.setType(KardexMovementEntity.MovementType.INITIAL_STOCK);
        testMovement.setQuantity(10);
        testMovement.setStockBefore(0);
        testMovement.setStockAfter(10);
        testMovement.setCreatedAt(LocalDateTime.now());
    }

    // ========== GENERAL MOVEMENT CREATION TESTS ==========

    @Test
    void testCreateMovement_Success() {
        // Arrange
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        KardexMovementEntity result = kardexMovementService.createMovement(
                testTool,
                KardexMovementEntity.MovementType.RESTOCK,
                5,
                "Restock",
                null,
                10 // Stock before
        );

        // Assert
        assertNotNull(result);
        assertEquals(KardexMovementEntity.MovementType.RESTOCK, result.getType());
        assertEquals(10, result.getStockBefore());
        assertEquals(15, result.getStockAfter()); // 10 + 5
        verify(kardexMovementRepository).save(any(KardexMovementEntity.class));
    }

    @Test
    void testCreateMovement_UsesToolCurrentStock() {
        // Arrange
        testTool.setCurrentStock(50);
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        KardexMovementEntity result = kardexMovementService.createMovement(
                testTool,
                KardexMovementEntity.MovementType.LOAN,
                5,
                "Loan out"
        );

        // Assert
        assertEquals(50, result.getStockBefore());
        assertEquals(45, result.getStockAfter()); // 50 - 5
    }

    @Test
    void testCreateMovement_Validations() {
        // Null Tool
        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createMovement(null, KardexMovementEntity.MovementType.LOAN, 1, "desc"));

        // Null Type
        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createMovement(testTool, null, 1, "desc"));

        // Negative Quantity
        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createMovement(testTool, KardexMovementEntity.MovementType.LOAN, -1, "desc"));
    }

    // ========== SPECIFIC MOVEMENT TYPES TESTS ==========

    @Test
    void testCreateInitialStockMovement() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        KardexMovementEntity result = kardexMovementService.createInitialStockMovement(testTool, 100);

        assertEquals(KardexMovementEntity.MovementType.INITIAL_STOCK, result.getType());
        assertEquals(100, result.getQuantity());
        // Assumes stock before is tool.currentStock (10)
        assertEquals(10, result.getStockBefore());
        assertEquals(110, result.getStockAfter());
    }

    @Test
    void testCreateLoanMovement() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        KardexMovementEntity result = kardexMovementService.createLoanMovement(testTool, 2, "Loan", testLoan);

        assertEquals(KardexMovementEntity.MovementType.LOAN, result.getType());
        assertEquals(testLoan, result.getRelatedLoan());
        assertEquals(8, result.getStockAfter()); // 10 - 2
    }

    @Test
    void testCreateReturnMovement_WithInstances() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        List<Long> instanceIds = Arrays.asList(1L, 2L);
        when(toolInstanceService.returnMultipleInstances(eq(instanceIds), eq(false)))
                .thenReturn(new ArrayList<>()); // Mock return

        KardexMovementEntity result = kardexMovementService.createReturnMovement(
                testTool, 2, "Return", testLoan, instanceIds, false
        );

        assertEquals(KardexMovementEntity.MovementType.RETURN, result.getType());
        assertTrue(result.getDescription().contains("Instancias devueltas"));
        verify(toolInstanceService).returnMultipleInstances(eq(instanceIds), eq(false));
    }

    @Test
    void testCreateReturnMovement_InstanceError() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        List<Long> instanceIds = Arrays.asList(1L);
        doThrow(new RuntimeException("Instance error")).when(toolInstanceService).returnMultipleInstances(any(), anyBoolean());

        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createReturnMovement(testTool, 1, "Desc", testLoan, instanceIds, false)
        );
    }

    @Test
    void testCreateDecommissionMovement_WithInstances() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        List<Long> instanceIds = Arrays.asList(1L);

        KardexMovementEntity result = kardexMovementService.createDecommissionMovement(
                testTool, 1, "Broken", instanceIds, null
        );

        assertEquals(KardexMovementEntity.MovementType.DECOMMISSION, result.getType());
        assertTrue(result.getDescription().contains("Instancias dadas de baja"));
    }

    @Test
    void testCreateRestockMovement_WithInstances() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(toolInstanceService.createInstances(any(), anyInt())).thenReturn(Collections.singletonList(testInstance));

        KardexMovementEntity result = kardexMovementService.createRestockMovement(testTool, 5, "New shipment", null);

        assertEquals(KardexMovementEntity.MovementType.RESTOCK, result.getType());
        assertTrue(result.getDescription().contains("Nuevas instancias creadas"));
        verify(toolInstanceService).createInstances(testTool, 5);
    }

    @Test
    void testCreateRestockMovement_Error() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doThrow(new RuntimeException("Error")).when(toolInstanceService).createInstances(any(), anyInt());

        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createRestockMovement(testTool, 5, "Desc", null)
        );
    }

    @Test
    void testCreateRepairMovement_WithInstance() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        KardexMovementEntity result = kardexMovementService.createRepairMovement(testTool, "Fixing", 1L);

        assertEquals(KardexMovementEntity.MovementType.REPAIR, result.getType());
        assertEquals(0, result.getQuantity()); // Repair quantity is 0
        assertEquals(10, result.getStockAfter()); // Stock doesn't change
        verify(toolInstanceService).updateInstanceStatus(1L, ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);
    }

    @Test
    void testCreateRepairMovement_Error() {
        when(kardexMovementRepository.save(any(KardexMovementEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        doThrow(new RuntimeException("Error")).when(toolInstanceService).updateInstanceStatus(anyLong(), any());

        assertThrows(RuntimeException.class, () ->
                kardexMovementService.createRepairMovement(testTool, "Desc", 1L)
        );
    }

    // ========== QUERY METHODS TESTS ==========

    @Test
    void testGetMovementHistoryByToolId() {
        when(kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.singletonList(testMovement));
        List<KardexMovementEntity> result = kardexMovementService.getMovementHistoryByTool(1L);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetMovementHistoryByToolId_Invalid() {
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementHistoryByTool(-1L));
    }

    @Test
    void testGetMovementHistoryByToolEntity() {
        when(kardexMovementRepository.findByToolOrderByCreatedAtDesc(testTool)).thenReturn(Collections.singletonList(testMovement));
        List<KardexMovementEntity> result = kardexMovementService.getMovementHistoryByTool(testTool);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetMovementHistoryByToolEntity_Null() {
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementHistoryByTool((ToolEntity) null));
    }

    @Test
    void testGetMovementsByDateRange_LocalDateTime() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(kardexMovementRepository.findByDateRangeOrderByCreatedAtDesc(start, end)).thenReturn(Collections.singletonList(testMovement));

        List<KardexMovementEntity> result = kardexMovementService.getMovementsByDateRange(start, end);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetMovementsByDateRange_LocalDate() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();
        when(kardexMovementRepository.findByDateRangeOrderByCreatedAtDesc(any(), any())).thenReturn(Collections.singletonList(testMovement));

        List<KardexMovementEntity> result = kardexMovementService.getMovementsByDateRange(start, end);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetMovementsByDateRange_Validation() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementsByDateRange(null, now));
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementsByDateRange(now, null));
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementsByDateRange(now, now.minusDays(1)));
    }

    @Test
    void testGetAllMovements() {
        when(kardexMovementRepository.findAll()).thenReturn(Collections.singletonList(testMovement));
        assertFalse(kardexMovementService.getAllMovements().isEmpty());
    }

    @Test
    void testGetMovementById() {
        when(kardexMovementRepository.findById(1L)).thenReturn(Optional.of(testMovement));
        assertNotNull(kardexMovementService.getMovementById(1L));
    }

    @Test
    void testGetMovementById_NotFound() {
        when(kardexMovementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> kardexMovementService.getMovementById(99L));
    }

    @Test
    void testDeleteMovementsByLoan() {
        when(kardexMovementRepository.findByRelatedLoanIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.singletonList(testMovement));
        kardexMovementService.deleteMovementsByLoan(1L);
        verify(kardexMovementRepository).deleteAll(anyList());
    }

    @Test
    void testGetMovementsByType() {
        when(kardexMovementRepository.findByTypeOrderByCreatedAtDesc(any())).thenReturn(Collections.singletonList(testMovement));
        assertFalse(kardexMovementService.getMovementsByType(KardexMovementEntity.MovementType.LOAN).isEmpty());
    }

    // ========== AUDIT AND CONSISTENCY TESTS ==========

    @Test
    void testVerifyStockConsistency_True() {
        testTool.setCurrentStock(5);
        when(toolInstanceService.getAvailableCount(1L)).thenReturn(5L);
        assertTrue(kardexMovementService.verifyStockConsistency(testTool));
    }

    @Test
    void testVerifyStockConsistency_False() {
        testTool.setCurrentStock(5);
        when(toolInstanceService.getAvailableCount(1L)).thenReturn(4L);
        assertFalse(kardexMovementService.verifyStockConsistency(testTool));
    }

    @Test
    void testGenerateAuditReport_Success() {
        // Mock ToolService (Ahora se inyecta correctamente vía reflexión en setUp)
        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));

        // Mock Instance Service
        ToolInstanceService.ToolInstanceStats stats = new ToolInstanceService.ToolInstanceStats(10L, 5L, 3L, 2L, 0L);
        when(toolInstanceService.getToolInstanceStats(1L)).thenReturn(stats);
        when(toolInstanceService.getAvailableCount(1L)).thenReturn(5L); // Used in verifyStockConsistency

        // Mock Repository
        when(kardexMovementRepository.getLastStockByToolList(1L)).thenReturn(Collections.singletonList(10));
        when(kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.singletonList(testMovement));

        KardexMovementService.KardexAuditReport report = kardexMovementService.generateAuditReport(1L);

        assertNotNull(report);
        assertEquals(testTool, report.tool);
        assertEquals(stats, report.instanceStats);
        assertEquals(10, report.lastKardexStock);
        assertFalse(report.isConsistent); // 10 stock vs 5 available
    }

    @Test
    void testGenerateAuditReport_NoStockHistory() {
        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));
        when(toolInstanceService.getToolInstanceStats(1L)).thenReturn(new ToolInstanceService.ToolInstanceStats(0L,0L,0L,0L,0L));
        when(toolInstanceService.getAvailableCount(1L)).thenReturn(10L);
        when(kardexMovementRepository.getLastStockByToolList(1L)).thenReturn(Collections.emptyList());

        KardexMovementService.KardexAuditReport report = kardexMovementService.generateAuditReport(1L);

        assertNull(report.lastKardexStock);
    }

    @Test
    void testGenerateAuditReport_Error() {
        // Al inyectar correctamente el mock en setUp, ahora esta llamada a getToolById
        // sí se ejecutará, lanzará la excepción, y el test pasará sin "UnnecessaryStubbingException"
        when(toolService.getToolById(1L)).thenThrow(new RuntimeException("DB Error"));

        // Verificamos que el servicio envuelva la excepción correctamente
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> kardexMovementService.generateAuditReport(1L));

        assertTrue(thrown.getMessage().contains("Error al generar reporte de auditoría"));
    }
}