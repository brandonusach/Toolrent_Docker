package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.DamageRepository;
import com.toolrent.backend.repositories.FineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class DamageServiceTest {

    @Mock
    private DamageRepository damageRepository;

    @Mock
    private FineRepository fineRepository;

    @Mock
    private ToolInstanceService toolInstanceService;

    @Mock
    private KardexMovementService kardexMovementService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private DamageService damageService;

    private LoanEntity testLoan;
    private ToolInstanceEntity testToolInstance;
    private ToolEntity testTool;
    private ClientEntity testClient;
    private DamageEntity testDamage;

    @BeforeEach
    void setUp() {
        // Setup test client
        testClient = new ClientEntity();
        testClient.setId(1L);
        testClient.setName("Test Client");

        // Setup test tool
        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Test Tool");
        testTool.setReplacementValue(new BigDecimal("500.00"));

        // Setup test tool instance
        testToolInstance = new ToolInstanceEntity();
        testToolInstance.setId(1L);
        testToolInstance.setTool(testTool);
        testToolInstance.setStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);

        // Setup test loan
        testLoan = new LoanEntity();
        testLoan.setId(1L);
        testLoan.setClient(testClient);
        testLoan.setTool(testTool);

        // Setup test damage
        testDamage = new DamageEntity(testLoan, testToolInstance, "Initial damage description");
        testDamage.setId(1L);
    }

    // ========== REPORT DAMAGE TESTS ==========

    @Test
    void testReportDamage_Success() {
        // Arrange
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(toolInstanceService.updateInstanceStatus(anyLong(), any())).thenReturn(testToolInstance);

        // Act
        DamageEntity result = damageService.reportDamage(testLoan, testToolInstance, "Broken handle");

        // Assert
        assertNotNull(result);
        assertEquals(testDamage.getId(), result.getId());
        verify(toolInstanceService).updateInstanceStatus(
                testToolInstance.getId(),
                ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR
        );
        verify(damageRepository).save(any(DamageEntity.class));
    }

    @Test
    void testReportDamage_NullLoan_ThrowsException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                damageService.reportDamage(null, testToolInstance, "Damage description")
        );
        assertEquals("Loan is required for damage report", exception.getMessage());
    }

    @Test
    void testReportDamage_NullToolInstance_ThrowsException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                damageService.reportDamage(testLoan, null, "Damage description")
        );
        assertEquals("Tool instance is required for damage report", exception.getMessage());
    }

    @Test
    void testReportDamage_ToolInstanceMismatch_ThrowsException() {
        // Arrange
        ToolEntity differentTool = new ToolEntity();
        differentTool.setId(999L);
        testToolInstance.setTool(differentTool);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                damageService.reportDamage(testLoan, testToolInstance, "Damage description")
        );
        assertEquals("Tool instance does not match loan tool", exception.getMessage());
    }

    // ========== ASSESS DAMAGE TESTS ==========

    @Test
    void testAssessDamage_RepairableMinor_Success() throws Exception {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        BigDecimal repairCost = new BigDecimal("50.00");

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(fineRepository.save(any(FineEntity.class))).thenReturn(new FineEntity());

        // Act
        DamageEntity result = damageService.assessDamage(
                1L,
                DamageEntity.DamageType.MINOR,
                "Small crack in handle",
                repairCost,
                true
        );

        // Assert
        assertNotNull(result);
        assertEquals(DamageEntity.DamageStatus.ASSESSED, result.getStatus());
        verify(fineRepository).save(any(FineEntity.class));
        verify(damageRepository).save(testDamage);
    }

    @Test
    void testAssessDamage_ZeroCost_NoFineCreated() throws Exception {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        BigDecimal repairCost = BigDecimal.ZERO; // Costo cero

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        // NO mockeamos fineRepository.save porque no debería llamarse

        // Act
        DamageEntity result = damageService.assessDamage(
                1L,
                DamageEntity.DamageType.MINOR,
                "Cosmetic damage",
                repairCost,
                true
        );

        // Assert
        assertNotNull(result);
        verify(fineRepository, never()).save(any(FineEntity.class)); // Verificamos que NO se cree multa
        verify(damageRepository).save(testDamage);
    }

    @Test
    void testAssessDamage_Irreparable_Success() throws Exception {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(fineRepository.save(any(FineEntity.class))).thenReturn(new FineEntity());
        when(toolInstanceService.decommissionInstance(anyLong())).thenReturn(testToolInstance);
        when(kardexMovementService.createDecommissionMovement(any(), anyInt(), anyString(), anyList(), any()))
                .thenReturn(new KardexMovementEntity());
        doNothing().when(toolService).deleteToolInstanceAndUpdateStock(anyLong());

        // Act
        DamageEntity result = damageService.assessDamage(
                1L,
                DamageEntity.DamageType.IRREPARABLE,
                "Completely broken",
                null,
                false
        );

        // Assert
        assertNotNull(result);
        assertEquals(DamageEntity.DamageStatus.IRREPARABLE, result.getStatus());
        verify(toolInstanceService).decommissionInstance(testToolInstance.getId());
        verify(fineRepository).save(any(FineEntity.class));
    }

    @Test
    void testAssessDamage_MarkedIrreparableButTypeMinor_Success() throws Exception {
        // Test case covering: !isRepairable || damageType == IRREPARABLE
        // Here we test !isRepairable with a different type
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(fineRepository.save(any(FineEntity.class))).thenReturn(new FineEntity());
        when(toolInstanceService.decommissionInstance(anyLong())).thenReturn(testToolInstance);
        when(kardexMovementService.createDecommissionMovement(any(), anyInt(), anyString(), anyList(), any()))
                .thenReturn(new KardexMovementEntity());
        doNothing().when(toolService).deleteToolInstanceAndUpdateStock(anyLong());

        // Act
        DamageEntity result = damageService.assessDamage(
                1L,
                DamageEntity.DamageType.MAJOR, // Type is NOT irreparable
                "Too expensive to fix",
                new BigDecimal("1000.00"),
                false // But explicitly marked as not repairable
        );

        // Assert
        assertNotNull(result);
        assertEquals(DamageEntity.DamageStatus.IRREPARABLE, result.getStatus());
        verify(toolInstanceService).decommissionInstance(testToolInstance.getId());
    }

    @Test
    void testAssessDamage_AlreadyAssessed_ThrowsException() {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                damageService.assessDamage(1L, DamageEntity.DamageType.MINOR, "desc", new BigDecimal("50"), true)
        );
        assertEquals("Damage has already been assessed", exception.getMessage());
    }

    @Test
    void testAssessDamage_DamageNotFound_ThrowsException() {
        // Arrange
        when(damageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                damageService.assessDamage(999L, DamageEntity.DamageType.MINOR, "desc", new BigDecimal("50"), true)
        );
        assertTrue(exception.getMessage().contains("Damage not found"));
    }

    // ========== START REPAIR TESTS ==========

    @Test
    void testStartRepair_Success() throws Exception {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        testDamage.setIsRepairable(true);

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(kardexMovementService.createRepairMovement(any(), anyString(), anyLong()))
                .thenReturn(new KardexMovementEntity());

        // Act
        DamageEntity result = damageService.startRepair(1L);

        // Assert
        assertNotNull(result);
        assertEquals(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS, result.getStatus());
        verify(kardexMovementService).createRepairMovement(any(), anyString(), anyLong());
    }

    @Test
    void testStartRepair_NotAssessed_ThrowsException() {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                damageService.startRepair(1L)
        );
        assertEquals("Damage must be assessed before starting repair", exception.getMessage());
    }

    @Test
    void testStartRepair_NotRepairable_ThrowsException() {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        testDamage.setIsRepairable(false);
        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                damageService.startRepair(1L)
        );
        assertEquals("Cannot repair irreparable damage", exception.getMessage());
    }

    // ========== COMPLETE REPAIR TESTS ==========

    @Test
    void testCompleteRepair_Success() throws Exception {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS);

        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        when(damageRepository.save(any(DamageEntity.class))).thenReturn(testDamage);
        when(toolInstanceService.updateInstanceStatus(anyLong(), any())).thenReturn(testToolInstance);

        // Act
        DamageEntity result = damageService.completeRepair(1L);

        // Assert
        assertNotNull(result);
        assertEquals(DamageEntity.DamageStatus.REPAIRED, result.getStatus());
        verify(toolInstanceService).updateInstanceStatus(
                testToolInstance.getId(),
                ToolInstanceEntity.ToolInstanceStatus.AVAILABLE
        );
    }

    @Test
    void testCompleteRepair_NotUnderRepair_ThrowsException() {
        // Arrange
        testDamage.setStatus(DamageEntity.DamageStatus.ASSESSED);
        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                damageService.completeRepair(1L)
        );
        assertEquals("Damage is not under repair", exception.getMessage());
    }

    // ========== QUERY METHODS TESTS (Expanded for Coverage) ==========

    @Test
    void testGetDamageById_Success() throws Exception {
        when(damageRepository.findById(1L)).thenReturn(Optional.of(testDamage));
        DamageEntity result = damageService.getDamageById(1L);
        assertEquals(testDamage.getId(), result.getId());
    }

    @Test
    void testGetDamageById_NotFound_ThrowsException() {
        when(damageRepository.findById(999L)).thenReturn(Optional.empty());
        Exception exception = assertThrows(Exception.class, () ->
                damageService.getDamageById(999L)
        );
        assertTrue(exception.getMessage().contains("Damage not found"));
    }

    @Test
    void testGetAllDamages() {
        when(damageRepository.findAll()).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getAllDamages().size());
    }

    @Test
    void testGetDamagesByLoan() {
        when(damageRepository.findByLoanOrderByReportedAtDesc(testLoan)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByLoan(testLoan).size());
    }

    @Test
    void testGetDamagesByLoanId() {
        when(damageRepository.findByLoanIdOrderByReportedAtDesc(1L)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByLoanId(1L).size());
    }

    @Test
    void testGetDamagesByToolInstance() {
        when(damageRepository.findByToolInstanceOrderByReportedAtDesc(testToolInstance)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByToolInstance(testToolInstance).size());
    }

    @Test
    void testGetDamagesByToolInstanceId() {
        when(damageRepository.findByToolInstanceIdOrderByReportedAtDesc(1L)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByToolInstanceId(1L).size());
    }

    @Test
    void testGetDamagesByClient() {
        when(damageRepository.findByClientOrderByReportedAtDesc(testClient)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByClient(testClient).size());
    }

    @Test
    void testGetDamagesByClientId() {
        when(damageRepository.findByClientIdOrderByReportedAtDesc(1L)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByClientId(1L).size());
    }

    @Test
    void testGetDamagesByToolId() {
        when(damageRepository.findByToolIdOrderByReportedAtDesc(1L)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByToolId(1L).size());
    }

    @Test
    void testGetDamagesByStatus() {
        when(damageRepository.findByStatusOrderByReportedAtDesc(DamageEntity.DamageStatus.REPORTED))
                .thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByStatus(DamageEntity.DamageStatus.REPORTED).size());
    }

    @Test
    void testGetDamagesByType() {
        when(damageRepository.findByTypeOrderByReportedAtDesc(DamageEntity.DamageType.MINOR))
                .thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByType(DamageEntity.DamageType.MINOR).size());
    }

    // ========== DASHBOARD AND REPORTS TESTS ==========

    @Test
    void testGetPendingAssessments() {
        when(damageRepository.findPendingAssessments()).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getPendingAssessments().size());
    }

    @Test
    void testGetDamagesUnderRepair() {
        when(damageRepository.findDamagesUnderRepair()).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesUnderRepair().size());
    }

    @Test
    void testGetIrreparableDamages() {
        when(damageRepository.findIrreparableDamages()).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getIrreparableDamages().size());
    }

    @Test
    void testGetDamagesByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(damageRepository.findByDateRange(start, end)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getDamagesByDateRange(start, end).size());
    }

    @Test
    void testGetUrgentDamages() {
        // Needs careful mocking of repository with any date because of LocalDateTime.now() inside service
        when(damageRepository.findUrgentDamages(any(LocalDateTime.class))).thenReturn(Arrays.asList(testDamage));
        List<DamageEntity> result = damageService.getUrgentDamages();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(damageRepository).findUrgentDamages(any(LocalDateTime.class));
    }

    @Test
    void testGetStagnantAssessments() {
        when(damageRepository.findStagnantAssessments(any(LocalDateTime.class))).thenReturn(Arrays.asList(testDamage));
        List<DamageEntity> result = damageService.getStagnantAssessments();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOverdueRepairs() {
        when(damageRepository.findOverdueRepairs(any(LocalDateTime.class))).thenReturn(Arrays.asList(testDamage));
        List<DamageEntity> result = damageService.getOverdueRepairs();
        assertNotNull(result);
        assertEquals(1, result.size());
    }


    // ========== STATISTICS TESTS ==========

    @Test
    void testGetDamageStatsByTool() {
        List<Object[]> mockStats = new ArrayList<>();
        mockStats.add(new Object[]{"Tool A", 5L});
        when(damageRepository.getDamageStatsByTool()).thenReturn(mockStats);

        List<Object[]> result = damageService.getDamageStatsByTool();
        assertEquals(1, result.size());
        assertEquals("Tool A", result.get(0)[0]);
    }

    @Test
    void testGetDamageStatsByClient() {
        List<Object[]> mockStats = new ArrayList<>();
        mockStats.add(new Object[]{"Client A", 3L});
        when(damageRepository.getDamageStatsByClient()).thenReturn(mockStats);

        List<Object[]> result = damageService.getDamageStatsByClient();
        assertEquals(1, result.size());
    }

    @Test
    void testGetMonthlyDamageTrend() {
        List<Object[]> mockStats = new ArrayList<>();
        when(damageRepository.getMonthlyDamageTrend()).thenReturn(mockStats);
        List<Object[]> result = damageService.getMonthlyDamageTrend();
        assertNotNull(result);
    }

    @Test
    void testCalculateTotalRepairCostInPeriod() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        BigDecimal expectedCost = new BigDecimal("150.00");
        when(damageRepository.calculateTotalRepairCostInPeriod(start, end)).thenReturn(expectedCost);

        BigDecimal result = damageService.calculateTotalRepairCostInPeriod(start, end);
        assertEquals(expectedCost, result);
    }

    @Test
    void testCountDamagesByStatus() {
        when(damageRepository.countByStatus(DamageEntity.DamageStatus.REPORTED)).thenReturn(5L);
        assertEquals(5L, damageService.countDamagesByStatus(DamageEntity.DamageStatus.REPORTED));
    }

    @Test
    void testCountDamagesByClient() {
        when(damageRepository.countByClientId(1L)).thenReturn(3L);
        assertEquals(3L, damageService.countDamagesByClient(1L));
    }

    @Test
    void testCountIrreparableDamagesByClient() {
        when(damageRepository.countIrreparableDamagesByClientId(1L)).thenReturn(2L);
        assertEquals(2L, damageService.countIrreparableDamagesByClient(1L));
    }

    // ========== BUSINESS LOGIC TESTS ==========

    @Test
    void testHasPendingDamages() {
        when(damageRepository.hasPendingDamages(1L)).thenReturn(true);
        assertTrue(damageService.hasPendingDamages(1L));
    }

    @Test
    void testLoanHasDamages() {
        when(damageRepository.existsByLoanId(1L)).thenReturn(true);
        assertTrue(damageService.loanHasDamages(1L));
    }

    @Test
    void testGetActiveDamagesByToolInstance() {
        when(damageRepository.findActiveDamagesByToolInstanceId(1L)).thenReturn(Arrays.asList(testDamage));
        assertEquals(1, damageService.getActiveDamagesByToolInstance(1L).size());
    }

    @Test
    void testGetMostRecentDamageByToolInstance() {
        when(damageRepository.findMostRecentByToolInstanceId(1L)).thenReturn(Optional.of(testDamage));
        Optional<DamageEntity> result = damageService.getMostRecentDamageByToolInstance(1L);
        assertTrue(result.isPresent());
        assertEquals(testDamage.getId(), result.get().getId());
    }

    // ========== DASHBOARD SUMMARY TEST ==========

    @Test
    void testGetDamageDashboardSummary() {
        // Arrange
        when(damageRepository.countByStatus(DamageEntity.DamageStatus.REPORTED)).thenReturn(5L);
        when(damageRepository.countByStatus(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS)).thenReturn(3L);
        when(damageRepository.findIrreparableDamages()).thenReturn(Arrays.asList(testDamage, testDamage));
        when(damageRepository.calculateTotalRepairCostInPeriod(any(), any()))
                .thenReturn(new BigDecimal("250.00"));

        // Act
        DamageService.DamageDashboardSummary result = damageService.getDamageDashboardSummary();

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.pendingAssessments);
        assertEquals(3L, result.underRepair);
        assertEquals(2L, result.irreparable);
        assertEquals(new BigDecimal("250.00"), result.monthlyRepairCost);
    }

    @Test
    void testGetDamageDashboardSummary_NullCost() {
        // Arrange
        when(damageRepository.countByStatus(any())).thenReturn(0L);
        when(damageRepository.findIrreparableDamages()).thenReturn(Collections.emptyList());
        when(damageRepository.calculateTotalRepairCostInPeriod(any(), any())).thenReturn(null);

        // Act
        DamageService.DamageDashboardSummary result = damageService.getDamageDashboardSummary();

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.monthlyRepairCost);
    }
}