package com.toolrent.backend.services;

import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.entities.ToolInstanceEntity.ToolInstanceStatus;
import com.toolrent.backend.repositories.ToolInstanceRepository;
import com.toolrent.backend.repositories.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolInstanceServiceTest {

    @Mock
    private ToolInstanceRepository toolInstanceRepository;

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private ToolInstanceService toolInstanceService;

    private ToolEntity testTool;
    private ToolInstanceEntity testInstance;

    @BeforeEach
    void setUp() {
        CategoryEntity testCategory = new CategoryEntity();
        testCategory.setId(1L);
        testCategory.setName("Power Tools");

        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Drill");
        testTool.setCategory(testCategory);
        testTool.setInitialStock(10);
        testTool.setCurrentStock(10);
        testTool.setReplacementValue(new BigDecimal("5000"));
        testTool.setRentalRate(new BigDecimal("100"));
        testTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        testInstance = new ToolInstanceEntity();
        testInstance.setId(1L);
        testInstance.setTool(testTool);
        testInstance.setStatus(ToolInstanceStatus.AVAILABLE);
    }

    // ========== Tests for createInstances ==========
    @Test
    void createInstances_ShouldCreateMultipleInstances_WhenValidQuantity() {
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> {
            ToolInstanceEntity instance = invocation.getArgument(0);
            instance.setId(1L);
            return instance;
        });

        List<ToolInstanceEntity> result = toolInstanceService.createInstances(testTool, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void createInstances_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.createInstances(testTool, 0)
        );

        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }

    @Test
    void createInstances_ShouldThrowException_WhenQuantityIsNegative() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.createInstances(testTool, -5)
        );

        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }

    // ========== Tests for getAvailableCount ==========
    @Test
    void getAvailableCount_ShouldReturnCorrectCount() {
        when(toolInstanceRepository.countAvailableByToolId(1L)).thenReturn(5L);

        Long result = toolInstanceService.getAvailableCount(1L);

        assertEquals(5L, result);
        verify(toolInstanceRepository, times(1)).countAvailableByToolId(1L);
    }

    // ========== Tests for getAvailableInstance ==========
    @Test
    void getAvailableInstance_ShouldReturnInstance_WhenAvailable() {
        when(toolInstanceRepository.findFirstAvailableByToolId(1L)).thenReturn(Optional.of(testInstance));

        Optional<ToolInstanceEntity> result = toolInstanceService.getAvailableInstance(1L);

        assertTrue(result.isPresent());
        assertEquals(testInstance.getId(), result.get().getId());
        verify(toolInstanceRepository, times(1)).findFirstAvailableByToolId(1L);
    }

    @Test
    void getAvailableInstance_ShouldReturnEmpty_WhenNoAvailableInstance() {
        when(toolInstanceRepository.findFirstAvailableByToolId(1L)).thenReturn(Optional.empty());

        Optional<ToolInstanceEntity> result = toolInstanceService.getAvailableInstance(1L);

        assertFalse(result.isPresent());
    }

    // ========== Tests for updateInstanceStatus ==========
    @Test
    void updateInstanceStatus_ShouldUpdateStatus_WhenInstanceExists() {
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.updateInstanceStatus(1L, ToolInstanceStatus.LOANED);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.LOANED, testInstance.getStatus());
        verify(toolInstanceRepository, times(1)).save(testInstance);
    }

    @Test
    void updateInstanceStatus_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.updateInstanceStatus(999L, ToolInstanceStatus.LOANED)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    // ========== Tests for reserveInstanceForLoan ==========
    @Test
    void reserveInstanceForLoan_ShouldReserveInstance_WhenAvailable() {
        when(toolInstanceRepository.findFirstAvailableByToolId(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.reserveInstanceForLoan(1L);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.LOANED, testInstance.getStatus());
        verify(toolInstanceRepository, times(1)).save(testInstance);
    }

    @Test
    void reserveInstanceForLoan_ShouldThrowException_WhenNoAvailableInstance() {
        when(toolInstanceRepository.findFirstAvailableByToolId(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.reserveInstanceForLoan(1L)
        );

        assertTrue(exception.getMessage().contains("No available instances"));
    }

    // ========== Tests for returnInstanceFromLoan ==========
    @Test
    void returnInstanceFromLoan_ShouldMarkAsAvailable_WhenNotDamaged() {
        testInstance.setStatus(ToolInstanceStatus.LOANED);
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.returnInstanceFromLoan(1L, false);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.AVAILABLE, testInstance.getStatus());
        verify(toolInstanceRepository, times(1)).save(testInstance);
    }

    @Test
    void returnInstanceFromLoan_ShouldMarkAsUnderRepair_WhenDamaged() {
        testInstance.setStatus(ToolInstanceStatus.LOANED);
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.returnInstanceFromLoan(1L, true);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.UNDER_REPAIR, testInstance.getStatus());
        verify(toolInstanceRepository, times(1)).save(testInstance);
    }

    @Test
    void returnInstanceFromLoan_ShouldThrowException_WhenInstanceNotLoaned() {
        testInstance.setStatus(ToolInstanceStatus.AVAILABLE);
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.returnInstanceFromLoan(1L, false)
        );

        assertTrue(exception.getMessage().contains("not currently loaned"));
    }

    @Test
    void returnInstanceFromLoan_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.returnInstanceFromLoan(999L, false)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    // ========== Tests for decommissionInstance ==========
    @Test
    void decommissionInstance_ShouldDecommission_WhenInstanceExists() {
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.decommissionInstance(1L);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.DECOMMISSIONED, testInstance.getStatus());
        verify(toolInstanceRepository, times(1)).save(testInstance);
    }

    @Test
    void decommissionInstance_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.decommissionInstance(999L)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    // ========== Tests for getInstancesByTool ==========
    @Test
    void getInstancesByTool_ShouldReturnInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findByToolIdOrderByStatus(1L)).thenReturn(instances);

        List<ToolInstanceEntity> result = toolInstanceService.getInstancesByTool(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolInstanceRepository, times(1)).findByToolIdOrderByStatus(1L);
    }

    // ========== Tests for getInstancesByStatus ==========
    @Test
    void getInstancesByStatus_ShouldReturnInstancesWithStatus() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findByStatus(ToolInstanceStatus.AVAILABLE)).thenReturn(instances);

        List<ToolInstanceEntity> result = toolInstanceService.getInstancesByStatus(ToolInstanceStatus.AVAILABLE);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolInstanceRepository, times(1)).findByStatus(ToolInstanceStatus.AVAILABLE);
    }

    // ========== Tests for isAvailable ==========
    @Test
    void isAvailable_ShouldReturnTrue_WhenEnoughInstancesAvailable() {
        when(toolInstanceRepository.countAvailableByToolId(1L)).thenReturn(5L);

        boolean result = toolInstanceService.isAvailable(1L, 3);

        assertTrue(result);
    }

    @Test
    void isAvailable_ShouldReturnFalse_WhenNotEnoughInstancesAvailable() {
        when(toolInstanceRepository.countAvailableByToolId(1L)).thenReturn(2L);

        boolean result = toolInstanceService.isAvailable(1L, 3);

        assertFalse(result);
    }

    // ========== Tests for getToolInstanceStats ==========
    @Test
    void getToolInstanceStats_ShouldReturnCorrectStats() {
        List<ToolInstanceEntity> instances = new ArrayList<>();

        ToolInstanceEntity available1 = new ToolInstanceEntity(testTool);
        available1.setStatus(ToolInstanceStatus.AVAILABLE);

        ToolInstanceEntity loaned1 = new ToolInstanceEntity(testTool);
        loaned1.setStatus(ToolInstanceStatus.LOANED);

        ToolInstanceEntity repair1 = new ToolInstanceEntity(testTool);
        repair1.setStatus(ToolInstanceStatus.UNDER_REPAIR);

        ToolInstanceEntity decom1 = new ToolInstanceEntity(testTool);
        decom1.setStatus(ToolInstanceStatus.DECOMMISSIONED);

        instances.addAll(Arrays.asList(available1, loaned1, repair1, decom1));

        when(toolInstanceRepository.findByToolIdOrderByStatus(1L)).thenReturn(instances);

        ToolInstanceService.ToolInstanceStats result = toolInstanceService.getToolInstanceStats(1L);

        assertNotNull(result);
        assertEquals(1, result.getAvailable());
        assertEquals(1, result.getLoaned());
        assertEquals(1, result.getUnderRepair());
        assertEquals(1, result.getDecommissioned());
        assertEquals(4, result.getTotal());
    }

    // ========== Tests for reserveMultipleInstances ==========
    @Test
    void reserveMultipleInstances_ShouldReserveInstances_WhenEnoughAvailable() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.reserveMultipleInstances(1L, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void reserveMultipleInstances_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.reserveMultipleInstances(1L, 0)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void reserveMultipleInstances_ShouldThrowException_WhenNotEnoughInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.reserveMultipleInstances(1L, 3)
        );

        assertTrue(exception.getMessage().contains("Not enough available instances"));
    }

    // ========== Tests for returnMultipleInstances ==========
    @Test
    void returnMultipleInstances_ShouldReturnAllInstances() {
        List<Long> instanceIds = Arrays.asList(1L, 2L, 3L);

        ToolInstanceEntity instance1 = new ToolInstanceEntity(testTool);
        instance1.setId(1L);
        instance1.setStatus(ToolInstanceStatus.LOANED);

        ToolInstanceEntity instance2 = new ToolInstanceEntity(testTool);
        instance2.setId(2L);
        instance2.setStatus(ToolInstanceStatus.LOANED);

        ToolInstanceEntity instance3 = new ToolInstanceEntity(testTool);
        instance3.setId(3L);
        instance3.setStatus(ToolInstanceStatus.LOANED);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(instance1));
        when(toolInstanceRepository.findById(2L)).thenReturn(Optional.of(instance2));
        when(toolInstanceRepository.findById(3L)).thenReturn(Optional.of(instance3));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.returnMultipleInstances(instanceIds, false);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    // ========== Tests for getInstancesUnderRepair ==========
    @Test
    void getInstancesUnderRepair_ShouldReturnInstancesUnderRepair() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findInstancesUnderRepair()).thenReturn(instances);

        List<ToolInstanceEntity> result = toolInstanceService.getInstancesUnderRepair();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolInstanceRepository, times(1)).findInstancesUnderRepair();
    }

    // ========== Tests for getLoanedInstances ==========
    @Test
    void getLoanedInstances_ShouldReturnLoanedInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findLoanedInstances()).thenReturn(instances);

        List<ToolInstanceEntity> result = toolInstanceService.getLoanedInstances();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolInstanceRepository, times(1)).findLoanedInstances();
    }

    // ========== Tests for repairInstance ==========
    @Test
    void repairInstance_ShouldRepairAndUpdateToolStatus() {
        testInstance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
        testTool.setCurrentStock(9);
        testTool.setStatus(ToolEntity.ToolStatus.UNDER_REPAIR);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);
        when(toolInstanceRepository.countAvailableByToolId(1L)).thenReturn(1L);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        ToolInstanceEntity result = toolInstanceService.repairInstance(1L);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.AVAILABLE, testInstance.getStatus());
        assertEquals(10, testTool.getCurrentStock());
        assertEquals(ToolEntity.ToolStatus.AVAILABLE, testTool.getStatus());
        verify(toolRepository, times(1)).save(testTool);
    }

    @Test
    void repairInstance_ShouldThrowException_WhenInstanceNotUnderRepair() {
        testInstance.setStatus(ToolInstanceStatus.AVAILABLE);
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.repairInstance(1L)
        );

        assertTrue(exception.getMessage().contains("not under repair"));
    }

    @Test
    void repairInstance_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.repairInstance(999L)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    @Test
    void repairInstance_ShouldHandleException_WhenToolUpdateFails() {
        testInstance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
        testInstance.setTool(null);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        // Should not throw exception even if tool update fails
        assertDoesNotThrow(() -> toolInstanceService.repairInstance(1L));
        assertEquals(ToolInstanceStatus.AVAILABLE, testInstance.getStatus());
    }

    // ========== Tests for deleteAllInstancesByTool ==========
    @Test
    void deleteAllInstancesByTool_ShouldCallRepository() {
        doNothing().when(toolInstanceRepository).deleteByToolId(1L);

        toolInstanceService.deleteAllInstancesByTool(1L);

        verify(toolInstanceRepository, times(1)).deleteByToolId(1L);
    }

    // ========== Tests for deleteInstance ==========
    @Test
    void deleteInstance_ShouldDeleteInstance_WhenExists() {
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        doNothing().when(toolInstanceRepository).delete(testInstance);

        toolInstanceService.deleteInstance(1L);

        verify(toolInstanceRepository, times(1)).delete(testInstance);
    }

    @Test
    void deleteInstance_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.deleteInstance(999L)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    // ========== Tests for getInstanceById ==========
    @Test
    void getInstanceById_ShouldReturnInstance_WhenExists() {
        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));

        ToolInstanceEntity result = toolInstanceService.getInstanceById(1L);

        assertNotNull(result);
        assertEquals(testInstance.getId(), result.getId());
    }

    @Test
    void getInstanceById_ShouldThrowException_WhenNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.getInstanceById(999L)
        );

        assertTrue(exception.getMessage().contains("Tool instance not found"));
    }

    // ========== Tests for decommissionMultipleInstances ==========
    @Test
    void decommissionMultipleInstances_ShouldDecommission_WhenEnoughAvailable() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.decommissionMultipleInstances(1L, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.DECOMMISSIONED, instance.getStatus()));
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void decommissionMultipleInstances_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.decommissionMultipleInstances(1L, 0)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void decommissionMultipleInstances_ShouldThrowException_WhenNotEnoughInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.decommissionMultipleInstances(1L, 3)
        );

        assertTrue(exception.getMessage().contains("Not enough available instances"));
    }

    // ========== Tests for reserveInstancesForLoan ==========
    @Test
    void reserveInstancesForLoan_ShouldReserveInstances() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.reserveInstancesForLoan(1L, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.LOANED, instance.getStatus()));
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void reserveInstancesForLoan_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.reserveInstancesForLoan(1L, 0)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void reserveInstancesForLoan_ShouldThrowException_WhenNotEnoughInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.reserveInstancesForLoan(1L, 3)
        );

        assertTrue(exception.getMessage().contains("Not enough available instances for loan"));
    }

    // ========== Tests for returnInstancesFromLoan ==========
    @Test
    void returnInstancesFromLoan_ShouldReturnInstances_NotDamaged() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.LOANED);
            instances.add(instance);
        }

        when(toolInstanceRepository.findLoanedInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.returnInstancesFromLoan(1L, 3, false);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.AVAILABLE, instance.getStatus()));
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void returnInstancesFromLoan_ShouldReturnInstances_Damaged() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.LOANED);
            instances.add(instance);
        }

        when(toolInstanceRepository.findLoanedInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.returnInstancesFromLoan(1L, 3, true);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.UNDER_REPAIR, instance.getStatus()));
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void returnInstancesFromLoan_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.returnInstancesFromLoan(1L, 0, false)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void returnInstancesFromLoan_ShouldThrowException_WhenNotEnoughLoanedInstances() {
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceRepository.findLoanedInstancesByToolId(1L)).thenReturn(instances);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.returnInstancesFromLoan(1L, 3, false)
        );

        assertTrue(exception.getMessage().contains("Not enough loaned instances"));
    }

    // ========== Tests for repairInstances ==========
    @Test
    void repairInstances_ShouldRepairInstances_WhenUnderRepair() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
            instances.add(instance);
        }

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.repairInstances(1L, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.AVAILABLE, instance.getStatus()));
        verify(toolInstanceRepository, times(2)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void repairInstances_ShouldRepairAllInstances_WhenQuantityExceedsAvailable() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
            instances.add(instance);
        }

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.repairInstances(1L, 5);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(toolInstanceRepository, times(2)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void repairInstances_ShouldReturnEmptyList_WhenNoInstancesUnderRepair() {
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(List.of());

        List<ToolInstanceEntity> result = toolInstanceService.repairInstances(1L, 2);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(toolInstanceRepository, never()).save(any(ToolInstanceEntity.class));
    }

    @Test
    void repairInstances_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.repairInstances(1L, 0)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void repairInstances_ShouldThrowException_WhenQuantityIsNegative() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.repairInstances(1L, -3)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    // ========== Tests for decommissionInstances ==========
    @Test
    void decommissionInstances_ShouldDecommissionLoanedInstances() {
        List<ToolInstanceEntity> loanedInstances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.LOANED);
            loanedInstances.add(instance);
        }

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.LOANED)).thenReturn(loanedInstances);
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(List.of());
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.decommissionInstances(1L, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.DECOMMISSIONED, instance.getStatus()));
        verify(toolInstanceRepository, times(2)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void decommissionInstances_ShouldDecommissionUnderRepairInstances_WhenNoLoanedInstances() {
        List<ToolInstanceEntity> repairInstances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
            repairInstances.add(instance);
        }

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.LOANED)).thenReturn(List.of());
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(repairInstances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.decommissionInstances(1L, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(instance -> assertEquals(ToolInstanceStatus.DECOMMISSIONED, instance.getStatus()));
        verify(toolInstanceRepository, times(2)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void decommissionInstances_ShouldPrioritizeLoanedInstances() {
        List<ToolInstanceEntity> loanedInstances = new ArrayList<>();
        ToolInstanceEntity loaned = new ToolInstanceEntity(testTool);
        loaned.setId(1L);
        loaned.setStatus(ToolInstanceStatus.LOANED);
        loanedInstances.add(loaned);

        List<ToolInstanceEntity> repairInstances = new ArrayList<>();
        ToolInstanceEntity repair = new ToolInstanceEntity(testTool);
        repair.setId(2L);
        repair.setStatus(ToolInstanceStatus.UNDER_REPAIR);
        repairInstances.add(repair);

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.LOANED)).thenReturn(loanedInstances);
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(repairInstances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.decommissionInstances(1L, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId()); // Loaned instance first
        assertEquals(2L, result.get(1).getId()); // Then under repair
    }

    @Test
    void decommissionInstances_ShouldReturnEmptyList_WhenNoInstancesAvailable() {
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.LOANED)).thenReturn(List.of());
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(List.of());

        List<ToolInstanceEntity> result = toolInstanceService.decommissionInstances(1L, 2);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(toolInstanceRepository, never()).save(any(ToolInstanceEntity.class));
    }

    @Test
    void decommissionInstances_ShouldDecommissionLessThanRequested_WhenNotEnoughAvailable() {
        List<ToolInstanceEntity> loanedInstances = new ArrayList<>();
        ToolInstanceEntity loaned = new ToolInstanceEntity(testTool);
        loaned.setId(1L);
        loaned.setStatus(ToolInstanceStatus.LOANED);
        loanedInstances.add(loaned);

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.LOANED)).thenReturn(loanedInstances);
        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(List.of());
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.decommissionInstances(1L, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolInstanceRepository, times(1)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void decommissionInstances_ShouldThrowException_WhenQuantityIsZero() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.decommissionInstances(1L, 0)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void decommissionInstances_ShouldThrowException_WhenQuantityIsNegative() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> toolInstanceService.decommissionInstances(1L, -2)
        );

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    // ========== Tests for ToolInstanceStats inner class ==========
    @Test
    void toolInstanceStats_ShouldReturnCorrectValues() {
        ToolInstanceService.ToolInstanceStats stats = new ToolInstanceService.ToolInstanceStats(5, 3, 2, 1, 11);

        assertEquals(5, stats.getAvailable());
        assertEquals(3, stats.getLoaned());
        assertEquals(2, stats.getUnderRepair());
        assertEquals(1, stats.getDecommissioned());
        assertEquals(11, stats.getTotal());
    }

    // ========== Edge case tests ==========
    @Test
    void createInstances_ShouldCreateWithCorrectInitialStatus() {
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> {
            ToolInstanceEntity instance = invocation.getArgument(0);
            assertEquals(ToolInstanceStatus.AVAILABLE, instance.getStatus());
            assertEquals(testTool, instance.getTool());
            return instance;
        });

        toolInstanceService.createInstances(testTool, 3);

        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void reserveMultipleInstances_ShouldChangeStatusToLoaned() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolInstanceRepository.findAvailableInstancesByToolId(1L)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> {
            ToolInstanceEntity instance = invocation.getArgument(0);
            assertEquals(ToolInstanceStatus.LOANED, instance.getStatus());
            return instance;
        });

        toolInstanceService.reserveMultipleInstances(1L, 3);

        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void repairInstance_ShouldNotUpdateTool_WhenToolIsNull() {
        testInstance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
        testInstance.setTool(null);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(testInstance));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenReturn(testInstance);

        ToolInstanceEntity result = toolInstanceService.repairInstance(1L);

        assertNotNull(result);
        assertEquals(ToolInstanceStatus.AVAILABLE, result.getStatus());
        verify(toolRepository, never()).save(any(ToolEntity.class));
    }

    @Test
    void getToolInstanceStats_ShouldHandleEmptyList() {
        when(toolInstanceRepository.findByToolIdOrderByStatus(1L)).thenReturn(List.of());

        ToolInstanceService.ToolInstanceStats result = toolInstanceService.getToolInstanceStats(1L);

        assertNotNull(result);
        assertEquals(0, result.getAvailable());
        assertEquals(0, result.getLoaned());
        assertEquals(0, result.getUnderRepair());
        assertEquals(0, result.getDecommissioned());
        assertEquals(0, result.getTotal());
    }

    @Test
    void returnMultipleInstances_ShouldHandleEmptyList() {
        List<ToolInstanceEntity> result = toolInstanceService.returnMultipleInstances(List.of(), false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(toolInstanceRepository, never()).save(any(ToolInstanceEntity.class));
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenExactQuantityAvailable() {
        when(toolInstanceRepository.countAvailableByToolId(1L)).thenReturn(3L);

        boolean result = toolInstanceService.isAvailable(1L, 3);

        assertTrue(result);
    }

    @Test
    void repairInstances_ShouldHandlePartialRepair() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity(testTool);
            instance.setId((long) i);
            instance.setStatus(ToolInstanceStatus.UNDER_REPAIR);
            instances.add(instance);
        }

        when(toolInstanceRepository.findByToolIdAndStatus(1L, ToolInstanceStatus.UNDER_REPAIR)).thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ToolInstanceEntity> result = toolInstanceService.repairInstances(1L, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
    }
}