package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.ClientRepository;
import com.toolrent.backend.repositories.FineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock
    private FineRepository fineRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private ToolService toolService;

    @Mock
    private ToolInstanceService toolInstanceService;

    @InjectMocks
    private FineService fineService;

    private ClientEntity testClient;
    private ToolEntity testTool;
    private LoanEntity testLoan;
    private FineEntity testFine;

    @BeforeEach
    void setUp() {
        // Setup Client
        testClient = new ClientEntity();
        testClient.setId(1L);
        testClient.setName("Test Client");
        testClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        // Setup Tool
        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Test Tool");
        testTool.setReplacementValue(new BigDecimal("100.00"));
        testTool.setInitialStock(10);
        testTool.setCurrentStock(9);
        testTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        // Setup Loan
        testLoan = new LoanEntity();
        testLoan.setId(1L);
        testLoan.setClient(testClient);
        testLoan.setTool(testTool);
        testLoan.setQuantity(1);

        // Setup Fine
        testFine = new FineEntity();
        testFine.setId(1L);
        testFine.setClient(testClient);
        testFine.setLoan(testLoan);
        testFine.setAmount(new BigDecimal("20.00"));
        testFine.setPaid(false);
        testFine.setDueDate(LocalDate.now().plusDays(30));
        testFine.setType(FineEntity.FineType.LATE_RETURN);
        testFine.setCreatedAt(LocalDateTime.now());
    }

    // ========== CLIENT RESTRICTION TESTS ==========

    @Test
    void testClientHasUnpaidFines_True() {
        when(fineRepository.countUnpaidFinesByClient(testClient)).thenReturn(2L);
        assertTrue(fineService.clientHasUnpaidFines(testClient));
    }

    @Test
    void testClientHasUnpaidFines_False() {
        when(fineRepository.countUnpaidFinesByClient(testClient)).thenReturn(0L);
        assertFalse(fineService.clientHasUnpaidFines(testClient));
    }

    @Test
    void testClientHasUnpaidFines_NullClient() {
        assertFalse(fineService.clientHasUnpaidFines(null));
    }

    @Test
    void testGetTotalUnpaidAmount() {
        when(fineRepository.getTotalUnpaidAmountByClient(testClient)).thenReturn(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("50.00"), fineService.getTotalUnpaidAmount(testClient));
    }

    @Test
    void testGetTotalUnpaidAmount_NullOrError() {
        assertEquals(BigDecimal.ZERO, fineService.getTotalUnpaidAmount(null));

        when(fineRepository.getTotalUnpaidAmountByClient(any())).thenThrow(new RuntimeException("DB Error"));
        assertEquals(BigDecimal.ZERO, fineService.getTotalUnpaidAmount(testClient));
    }

    @Test
    void testCheckClientRestrictions_Restricted() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);

        FineEntity overdueFine = new FineEntity();
        overdueFine.setDueDate(LocalDate.now().minusDays(1)); // Vencida
        overdueFine.setPaid(false);

        when(fineRepository.findByClientAndPaidFalse(testClient)).thenReturn(Arrays.asList(testFine, overdueFine));
        when(fineRepository.getTotalUnpaidAmountByClient(testClient)).thenReturn(new BigDecimal("100.00"));

        // Act
        Map<String, Object> result = fineService.checkClientRestrictions(1L);

        // Assert
        assertTrue((Boolean) result.get("isRestricted"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("RESTRICTED", result.get("clientStatus"));
        assertEquals(2, result.get("unpaidFinesCount"));
        assertEquals(1L, result.get("overdueFinesCount")); // 1 vencida
    }

    @Test
    void testCheckClientRestrictions_NotRestricted() {
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(fineRepository.findByClientAndPaidFalse(testClient)).thenReturn(Collections.emptyList());

        Map<String, Object> result = fineService.checkClientRestrictions(1L);

        assertFalse((Boolean) result.get("isRestricted"));
        assertTrue((Boolean) result.get("canRequestLoan"));
        assertEquals("ACTIVE", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_InvalidId() {
        Map<String, Object> result = fineService.checkClientRestrictions(null);
        assertEquals("INVALID", result.get("clientStatus"));
        assertTrue((Boolean) result.get("isRestricted"));

        result = fineService.checkClientRestrictions(-1L);
        assertEquals("INVALID", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_ClientNotFound() {
        when(clientService.getClientById(99L)).thenReturn(null);
        Map<String, Object> result = fineService.checkClientRestrictions(99L);
        assertEquals("NOT_FOUND", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_Exception() {
        when(clientService.getClientById(1L)).thenThrow(new RuntimeException("Error"));
        Map<String, Object> result = fineService.checkClientRestrictions(1L);
        assertEquals("ERROR", result.get("clientStatus"));
        // En caso de error, el servicio actual permite el préstamo (fallback seguro)
        assertTrue((Boolean) result.get("canRequestLoan"));
    }

    // ========== GET METHODS TESTS ==========

    @Test
    void testGetFinesByClient() {
        when(fineRepository.findByClient(testClient)).thenReturn(Arrays.asList(testFine));
        List<FineEntity> result = fineService.getFinesByClient(testClient);
        assertEquals(1, result.size());
    }

    @Test
    void testGetFinesByClient_Null() {
        assertTrue(fineService.getFinesByClient(null).isEmpty());
    }

    @Test
    void testGetUnpaidFinesByClient() {
        when(fineRepository.findByClientAndPaidFalse(testClient)).thenReturn(Arrays.asList(testFine));
        List<FineEntity> result = fineService.getUnpaidFinesByClient(testClient);
        assertEquals(1, result.size());
    }

    @Test
    void testGetFinesByLoan() {
        when(fineRepository.findByLoan(testLoan)).thenReturn(Arrays.asList(testFine));
        List<FineEntity> result = fineService.getFinesByLoan(testLoan);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllFines() {
        when(fineRepository.findAll()).thenReturn(Arrays.asList(testFine));
        List<FineEntity> result = fineService.getAllFines();
        assertEquals(1, result.size());
    }

    @Test
    void testGetFineById_Success() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        FineEntity result = fineService.getFineById(1L);
        assertNotNull(result);
    }

    @Test
    void testGetFineById_NotFound() {
        when(fineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> fineService.getFineById(99L));
    }

    // ========== CRUD & PAYMENTS TESTS ==========

    @Test
    void testCreateFine_Success() {
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity newFine = new FineEntity();
        newFine.setClient(testClient);
        newFine.setAmount(BigDecimal.TEN);

        FineEntity result = fineService.createFine(newFine);

        assertNotNull(result.getCreatedAt());
        verify(fineRepository).save(any(FineEntity.class));
    }

    @Test
    void testCreateFine_UpdatesClientStatus() {
        // Test que verifica si se actualiza el estado del cliente a RESTRICTED
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> {
            FineEntity f = (FineEntity) i.getArguments()[0];
            return f;
        });

        testClient.setStatus(ClientEntity.ClientStatus.ACTIVE);
        FineEntity newFine = new FineEntity();
        newFine.setClient(testClient);
        newFine.setPaid(false);
        newFine.setAmount(BigDecimal.TEN);

        fineService.createFine(newFine);

        assertEquals(ClientEntity.ClientStatus.RESTRICTED, testClient.getStatus());
        verify(clientRepository).save(testClient);
    }

    @Test
    void testPayFine_Success() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity result = fineService.payFine(1L);

        assertTrue(result.getPaid());
        assertNotNull(result.getPaidDate());
    }

    @Test
    void testPayFine_AlreadyPaid() {
        testFine.setPaid(true);
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));

        assertThrows(RuntimeException.class, () -> fineService.payFine(1L));
    }

    @Test
    void testPayFine_RestoresClientStatus() {
        // Caso: Cliente Restringido paga su última multa
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        testFine.setPaid(false);

        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        // Mockear que NO tiene más multas impagas
        when(fineRepository.findByClientAndPaidFalse(testClient)).thenReturn(Collections.emptyList());

        fineService.payFine(1L);

        assertEquals(ClientEntity.ClientStatus.ACTIVE, testClient.getStatus());
        verify(clientRepository).save(testClient);
    }

    @Test
    void testPayFine_ClientRemainsRestricted() {
        // Caso: Cliente Restringido paga una multa, pero tiene otras
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        testFine.setPaid(false);

        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        // Mockear que TODAVÍA tiene multas impagas
        when(fineRepository.findByClientAndPaidFalse(testClient)).thenReturn(Arrays.asList(new FineEntity()));

        fineService.payFine(1L);

        // No debe cambiar a ACTIVE
        verify(clientRepository, never()).save(testClient);
    }

    @Test
    void testUpdateFine() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenReturn(testFine);

        FineEntity result = fineService.updateFine(1L, "New Desc", LocalDate.now().plusDays(60));

        assertEquals("New Desc", result.getDescription());
        assertEquals(LocalDate.now().plusDays(60), result.getDueDate());
    }

    @Test
    void testDeleteFine_Success() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));

        fineService.deleteFine(1L);

        verify(fineRepository).delete(testFine);
    }

    @Test
    void testDeleteFine_PaidError() {
        testFine.setPaid(true);
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));

        assertThrows(RuntimeException.class, () -> fineService.deleteFine(1L));
    }

    // ========== SPECIAL LOGIC: DAMAGE & TOOLS ==========

    @Test
    void testPayFine_IrreparableDamage_DecommissionsTool() {
        // Configurar multa por daño irreparable
        testFine.setType(FineEntity.FineType.TOOL_REPLACEMENT);
        testFine.setDamageType(FineEntity.DamageType.IRREPARABLE);

        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenReturn(testFine);

        // Mockear servicios de herramientas
        when(toolInstanceService.decommissionInstances(eq(testTool.getId()), eq(1))).thenReturn(Collections.emptyList());
        when(toolInstanceService.getAvailableCount(testTool.getId())).thenReturn(0L);
        when(toolInstanceService.getInstancesByStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED)).thenReturn(Collections.emptyList());

        fineService.payFine(1L);

        // Verificar que se intentó dar de baja la instancia
        verify(toolInstanceService).decommissionInstances(testTool.getId(), 1);
        // Verificar que se actualizó la herramienta
        verify(toolService).updateTool(eq(testTool.getId()), any(ToolEntity.class));
    }

    @Test
    void testPayFine_MinorDamage_DoesNotDecommission() {
        testFine.setType(FineEntity.FineType.DAMAGE_REPAIR);
        testFine.setDamageType(FineEntity.DamageType.MINOR);

        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(FineEntity.class))).thenReturn(testFine);

        fineService.payFine(1L);

        // Verificar que NO se llama al servicio de decommissioning
        verify(toolInstanceService, never()).decommissionInstances(anyLong(), anyInt());
    }

    @Test
    void testCreateDamageFineWithType_Minor() {
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity result = fineService.createDamageFineWithType(
                testLoan,
                FineEntity.DamageType.MINOR,
                "Minor scratch"
        );

        assertEquals(FineEntity.FineType.DAMAGE_REPAIR, result.getType());
        assertEquals(FineEntity.DamageType.MINOR, result.getDamageType());
        // 20% de 100.00 = 20.00
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getAmount()));
    }

    @Test
    void testCreateDamageFineWithType_Irreparable() {
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity result = fineService.createDamageFineWithType(
                testLoan,
                FineEntity.DamageType.IRREPARABLE,
                "Total loss"
        );

        assertEquals(FineEntity.FineType.TOOL_REPLACEMENT, result.getType());
        assertEquals(FineEntity.DamageType.IRREPARABLE, result.getDamageType());
        // 100% de 100.00 = 100.00
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getAmount()));
    }

    @Test
    void testCreateLateFine() {
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity result = fineService.createLateFine(testLoan, 5, new BigDecimal("10.00"));

        assertEquals(FineEntity.FineType.LATE_RETURN, result.getType());
        // 5 days * 10.00 = 50.00
        assertEquals(0, new BigDecimal("50.00").compareTo(result.getAmount()));
    }

    @Test
    void testCreateDamageFine() {
        when(fineRepository.save(any(FineEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        FineEntity result = fineService.createDamageFine(testLoan, new BigDecimal("75.00"), "Broken");

        assertEquals(FineEntity.FineType.DAMAGE_REPAIR, result.getType());
        assertEquals(new BigDecimal("75.00"), result.getAmount());
    }

    // ========== STATISTICS & SEARCH TESTS ==========

    @Test
    void testGetFineStatistics() {
        when(fineRepository.count()).thenReturn(10L);
        when(fineRepository.countByPaidFalse()).thenReturn(4L);
        when(fineRepository.countByPaidTrue()).thenReturn(6L);
        when(fineRepository.getTotalUnpaidAmount()).thenReturn(new BigDecimal("200.00"));

        Map<String, Object> stats = fineService.getFineStatistics();

        assertEquals(10L, stats.get("totalFines"));
        assertEquals(4L, stats.get("unpaidFines"));
        assertEquals(new BigDecimal("200.00"), stats.get("totalUnpaidAmount"));
    }

    @Test
    void testGetFineStatistics_Error() {
        when(fineRepository.count()).thenThrow(new RuntimeException("DB Error"));
        Map<String, Object> stats = fineService.getFineStatistics();
        assertTrue(stats.containsKey("error"));
        assertEquals(0, stats.get("totalFines"));
    }

    @Test
    void testGetOverdueFines() {
        when(fineRepository.findOverdueFines(any(LocalDate.class))).thenReturn(Arrays.asList(testFine));
        assertEquals(1, fineService.getOverdueFines().size());
    }

    @Test
    void testGetFinesByType() {
        when(fineRepository.findByType(FineEntity.FineType.LATE_RETURN)).thenReturn(Arrays.asList(testFine));
        assertEquals(1, fineService.getFinesByType(FineEntity.FineType.LATE_RETURN).size());
    }

    @Test
    void testGetFinesInDateRange() {
        when(fineRepository.findByDateRange(any(), any())).thenReturn(Arrays.asList(testFine));
        assertEquals(1, fineService.getFinesInDateRange(LocalDateTime.now(), LocalDateTime.now()).size());
    }

    @Test
    void testCancelFine() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        fineService.cancelFine(1L);
        verify(fineRepository).delete(testFine);
    }
}