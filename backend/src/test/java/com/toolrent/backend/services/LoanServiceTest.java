package com.toolrent.backend.services;

import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.LoanRepository;
import com.toolrent.backend.repositories.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ToolService toolService;

    @Mock
    private ClientService clientService;

    @Mock
    private FineService fineService;

    @Mock
    private RateService rateService;

    @Mock
    private KardexMovementService kardexMovementService;

    @Mock
    private ToolInstanceService toolInstanceService;

    @InjectMocks
    private LoanService loanService;

    private ClientEntity testClient;
    private ToolEntity testTool;
    private LoanEntity testLoan;
    private ToolInstanceEntity testInstance;

    @BeforeEach
    void setUp() {
        // Configurar cliente de prueba
        testClient = new ClientEntity();
        testClient.setId(1L);
        testClient.setName("Juan Pérez");
        testClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        // Configurar herramienta de prueba
        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Taladro eléctrico");
        testTool.setCurrentStock(5);
        testTool.setInitialStock(10);
        testTool.setRentalRate(BigDecimal.valueOf(50.0));
        testTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        // Configurar préstamo de prueba
        testLoan = new LoanEntity();
        testLoan.setId(1L);
        testLoan.setClient(testClient);
        testLoan.setTool(testTool);
        testLoan.setQuantity(1);
        testLoan.setLoanDate(LocalDate.now().minusDays(2));
        testLoan.setAgreedReturnDate(LocalDate.now().plusDays(5));
        testLoan.setDailyRate(BigDecimal.valueOf(50.0));
        testLoan.setStatus(LoanEntity.LoanStatus.ACTIVE);

        // Configurar instancia de herramienta
        testInstance = new ToolInstanceEntity();
        testInstance.setId(1L);
        testInstance.setTool(testTool);
        testInstance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
    }

    @Test
    void testGetAllLoans_Success() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findAll()).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getAllLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testLoan.getId(), result.get(0).getId());
        verify(loanRepository, times(1)).findAll();
    }

    @Test
    void testGetAllLoans_Exception() {
        // Arrange
        when(loanRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act
        List<LoanEntity> result = loanService.getAllLoans();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLoanById_Success() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // Act
        LoanEntity result = loanService.getLoanById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testLoan.getId(), result.getId());
        verify(loanRepository, times(1)).findById(1L);
    }

    @Test
    void testGetLoanById_NotFound() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.getLoanById(1L));
    }

    @Test
    void testCheckClientRestrictions_ValidClient() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.ZERO);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert - Usar assertEquals con Objects para evitar problemas de tipo
        assertTrue((Boolean) result.get("eligible"));
        assertTrue((Boolean) result.get("canRequestLoan"));
        assertEquals(2L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(5L, ((Number) result.get("maxAllowed")).longValue());
        assertEquals(3L, ((Number) result.get("remainingLoanSlots")).longValue());
        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    void testCheckClientRestrictions_InvalidClientId() {
        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(-1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("ID de cliente inválido", result.get("restriction"));
    }

    @Test
    void testCheckClientRestrictions_ClientNotFound() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(null);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("Cliente no encontrado", result.get("restriction"));
    }

    @Test
    void testCheckClientRestrictions_ClientHasOverdueLoans() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Arrays.asList(testLoan));
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertTrue((Boolean) result.get("hasOverdueLoans"));
        assertTrue(((String) result.get("restriction")).contains("préstamo(s) vencido(s)"));
    }

    @Test
    void testCheckClientRestrictions_ClientNotActive() {
        // Arrange
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert - CORREGIDO: Cambiar "INACTIVE" por "RESTRICTED"
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertTrue(((String) result.get("restriction")).contains("Cliente no está activo"));
        assertEquals("RESTRICTED", result.get("clientStatus")); // Cambiado de "INACTIVE" a "RESTRICTED"
    }

    @Test
    void testCheckClientRestrictions_ClientHasUnpaidFines() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(true);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.valueOf(150.50));

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Debug: imprimir el resultado para ver qué contiene
        System.out.println("Resultado: " + result);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertTrue((Boolean) result.get("hasUnpaidFines"));

        // Usar compareTo para BigDecimal en lugar de equals
        assertEquals(0, ((BigDecimal) result.get("unpaidFinesAmount")).compareTo(BigDecimal.valueOf(150.50)));

        String restriction = (String) result.get("restriction");
        assertTrue(restriction.contains("multas impagas"),
                "El mensaje de restricción debería contener 'multas impagas'. Actual: " + restriction);

        // CORREGIDO: Buscar "150.5" en lugar de "150.50"
        assertTrue(restriction.contains("150.5"),
                "El mensaje de restricción debería contener '150.5'. Actual: " + restriction);
    }

    @Test
    void testCheckClientRestrictions_MaxActiveLoansReached() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(5L); // Límite alcanzado
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals(5L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(0L, ((Number) result.get("remainingLoanSlots")).longValue());
        assertTrue(((String) result.get("restriction")).contains("límite de 5 préstamos activos"));
    }

    @Test
    void testCheckClientRestrictions_MultipleRestrictions() {
        // Arrange
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(5L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Arrays.asList(testLoan, testLoan)); // 2 préstamos vencidos
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(true);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.valueOf(200.0));

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));

        String restriction = (String) result.get("restriction");
        assertTrue(restriction.contains("Cliente no está activo"));
        assertTrue(restriction.contains("2 préstamo(s) vencido(s)"));
        assertTrue(restriction.contains("multas impagas por $200.0"));
        assertTrue(restriction.contains("límite de 5 préstamos activos"));

        assertEquals(2, ((Number) result.get("overdueLoansCount")).intValue());
        assertEquals(BigDecimal.valueOf(200.0), result.get("unpaidFinesAmount"));
    }

    @Test
    void testCheckClientRestrictions_FineServiceException() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenThrow(new RuntimeException("Fine service unavailable"));
        // No mock para getTotalUnpaidAmount ya que la excepción ocurre antes

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert - Debería ser elegible porque asumimos no hay multas cuando hay error
        assertTrue((Boolean) result.get("eligible"));
        assertTrue((Boolean) result.get("canRequestLoan"));
        assertFalse((Boolean) result.get("hasUnpaidFines"));
        assertEquals(BigDecimal.ZERO, result.get("unpaidFinesAmount"));
        assertEquals("Cliente elegible para nuevos préstamos", result.get("message"));
    }

    @Test
    void testCheckClientRestrictions_NullClientId() {
        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(null);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("ID de cliente inválido", result.get("restriction"));
        assertEquals("INVALID", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_ZeroClientId() {
        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(0L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("ID de cliente inválido", result.get("restriction"));
        assertEquals("INVALID", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_ExceptionInMainLogic() {
        // Arrange
        when(clientService.getClientById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertTrue(((String) result.get("restriction")).contains("Error al verificar restricciones"));
        assertEquals("ERROR", result.get("clientStatus"));
        assertTrue((Boolean) result.get("error"));
    }

    @Test
    void testCheckClientRestrictions_EligibleWithZeroActiveLoans() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(0L); // Sin préstamos activos
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.ZERO);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertTrue((Boolean) result.get("eligible"));
        assertTrue((Boolean) result.get("canRequestLoan"));
        assertEquals(0L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(5L, ((Number) result.get("remainingLoanSlots")).longValue());
        assertEquals("Cliente elegible para nuevos préstamos", result.get("message"));
        assertEquals("ACTIVE", result.get("clientStatus"));
    }

    @Test
    void testCheckClientRestrictions_EligibleWithFourActiveLoans() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(4L); // 4 de 5 préstamos
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(fineService.getTotalUnpaidAmount(testClient)).thenReturn(BigDecimal.ZERO);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertTrue((Boolean) result.get("eligible"));
        assertTrue((Boolean) result.get("canRequestLoan"));
        assertEquals(4L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(1L, ((Number) result.get("remainingLoanSlots")).longValue());
        assertEquals("Cliente elegible para nuevos préstamos", result.get("message"));
    }

    @Test
    void testCheckClientRestrictions_ExactlyFiveActiveLoans() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(5L); // Límite exacto
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals(5L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(0L, ((Number) result.get("remainingLoanSlots")).longValue());
        assertTrue(((String) result.get("restriction")).contains("límite de 5 préstamos activos"));
    }

    @Test
    void testCheckClientRestrictions_MoreThanFiveActiveLoans() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(7L); // Más del límite
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals(7L, ((Number) result.get("currentActiveLoans")).longValue());
        assertEquals(0L, ((Number) result.get("remainingLoanSlots")).longValue()); // Math.max(0, 5-7) = 0
        assertTrue(((String) result.get("restriction")).contains("límite de 5 préstamos activos"));
    }

    @Test
    void testCheckClientRestrictions_ClientRestrictedStatus() {
        // Arrange
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(1L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientRestrictions(1L);

        // Assert - CORREGIDO: El estado debería ser "RESTRICTED", no "ERROR"
        assertFalse((Boolean) result.get("eligible"));
        assertFalse((Boolean) result.get("canRequestLoan"));
        assertEquals("RESTRICTED", result.get("clientStatus")); // Cambiado de "ERROR" a "RESTRICTED"
        assertTrue(((String) result.get("restriction")).contains("Cliente no está activo")); // Verificar el mensaje de restricción
    }

    @Test
    void testCheckToolAvailability_ValidTool() {
        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, 1);

        // Assert
        assertTrue((Boolean) result.get("available"));
        assertEquals(5, ((Number) result.get("currentStock")).intValue());
        assertEquals("Herramienta disponible para préstamo", result.get("message"));
    }

    @Test
    void testCheckToolAvailability_ToolNotFound() {
        // Act
        Map<String, Object> result = loanService.checkToolAvailability(null, 1);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertEquals("Herramienta no encontrada", result.get("issue"));
    }

    @Test
    void testCheckToolAvailability_InsufficientQuantity() {
        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, 0);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertEquals("Cantidad debe ser mayor a 0", result.get("issue"));
    }

    @Test
    void testCheckToolAvailability_QuantityNotOne() {
        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, 2);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertEquals("Solo se permite prestar 1 unidad por préstamo", result.get("issue"));
    }

    @Test
    void testCheckToolAvailability_ToolNotAvailable() {
        // Arrange
        testTool.setStatus(ToolEntity.ToolStatus.LOANED);

        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, 1);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertTrue(((String) result.get("issue")).contains("no está disponible"));
    }

    @Test
    void testGetActiveLoans_Success() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findActiveLoans()).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getActiveLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LoanEntity.LoanStatus.ACTIVE, result.get(0).getStatus());
        verify(loanRepository, times(1)).findActiveLoans();
    }

    @Test
    void testGetActiveLoans_FallbackToFindByStatus() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findActiveLoans()).thenThrow(new RuntimeException("Query error"));
        when(loanRepository.findByStatus(LoanEntity.LoanStatus.ACTIVE)).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getActiveLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOverdueLoans_Success() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getOverdueLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(loanRepository, times(1)).findOverdueLoans(LocalDate.now());
    }

    @Test
    void testGetLoansByClient_Success() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findByClient(testClient)).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getLoansByClient(testClient);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(loanRepository, times(1)).findByClient(testClient);
    }

    @Test
    void testGetLoansByClient_NullClient() {
        // Act
        List<LoanEntity> result = loanService.getLoansByClient(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckClientToolLoan_NoActiveLoan() {
        // Arrange
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);

        // Act
        Map<String, Object> result = loanService.checkClientToolLoan(testClient, testTool);

        // Assert
        assertFalse((Boolean) result.get("hasActiveLoanForTool"));
        assertTrue((Boolean) result.get("canLoanThisTool"));
        assertEquals("Cliente puede solicitar un préstamo de esta herramienta", result.get("message"));
    }

    @Test
    void testCheckClientToolLoan_WithActiveLoan() {
        // Arrange
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(true);
        when(loanRepository.findByClient(testClient)).thenReturn(Arrays.asList(testLoan));

        // Act
        Map<String, Object> result = loanService.checkClientToolLoan(testClient, testTool);

        // Assert
        assertTrue((Boolean) result.get("hasActiveLoanForTool"));
        assertFalse((Boolean) result.get("canLoanThisTool"));
        assertEquals("Cliente ya tiene un préstamo activo de esta herramienta", result.get("message"));
    }

    @Test
    void testGetActiveLoanCount_Success() {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(testClient);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(3L);

        // Act
        Map<String, Object> result = loanService.getActiveLoanCount(1L);

        // Assert - Usar Number para evitar problemas de tipo
        assertEquals(3L, ((Number) result.get("activeLoanCount")).longValue());
        assertEquals(5L, ((Number) result.get("maxAllowed")).longValue());
        assertTrue((Boolean) result.get("canRequestMore"));
    }

    @Test
    void testGetActiveLoanCount_InvalidClientId() {
        // Act
        Map<String, Object> result = loanService.getActiveLoanCount(-1L);

        // Assert
        assertEquals(0L, ((Number) result.get("activeLoanCount")).longValue());
        assertFalse((Boolean) result.get("canRequestMore"));
        assertEquals("ID de cliente inválido", result.get("error"));
    }

    @Test
    void testGetCurrentRates_Success() {
        // Arrange
        when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.valueOf(50.0));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.valueOf(10.0));
        when(rateService.getCurrentRepairRate()).thenReturn(BigDecimal.valueOf(0.2));

        // Act
        Map<String, Object> result = loanService.getCurrentRates();

        // Assert
        assertEquals(BigDecimal.valueOf(50.0), result.get("rentalRate"));
        assertEquals(BigDecimal.valueOf(10.0), result.get("lateFeeRate"));
        assertEquals(BigDecimal.valueOf(0.2), result.get("repairRate"));
    }

    @Test
    void testGetCurrentRates_Exception() {
        // Arrange
        when(rateService.getCurrentRentalRate()).thenThrow(new RuntimeException("Service error"));

        // Act
        Map<String, Object> result = loanService.getCurrentRates();

        // Assert
        assertEquals(BigDecimal.valueOf(100.0), result.get("rentalRate"));
        assertEquals("Usando valores por defecto", result.get("error"));
    }

    @Test
    void testCreateLoan_Success() {
        // Arrange
        testLoan.setLoanDate(null); // Para probar auto-set
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        // Act
        LoanEntity result = loanService.createLoan(testLoan);

        // Assert
        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getLoanDate());
        assertEquals(LoanEntity.LoanStatus.ACTIVE, result.getStatus());
        assertEquals(BigDecimal.valueOf(50.0), result.getDailyRate());
        verify(loanRepository, times(1)).save(testLoan);
        verify(toolRepository, times(1)).save(testTool);
    }

    @Test
    void testCreateLoan_InvalidData() {
        // Arrange
        testLoan.setClient(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ClientNotActive() {
        // Arrange
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        testLoan.setClient(testClient);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_QuantityNotOne() {
        // Arrange
        testLoan.setQuantity(2);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_WithToolInstances() {
        // Arrange
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(toolInstanceService.reserveInstancesForLoan(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(testInstance));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        // Act
        LoanEntity result = loanService.createLoan(testLoan);

        // Assert
        assertNotNull(result);
        verify(toolInstanceService, times(1)).reserveInstancesForLoan(1L, 1);
    }



    @Test
    void testUpdateLoan_Success() {
        // Arrange
        LoanEntity updatedLoan = new LoanEntity();
        updatedLoan.setAgreedReturnDate(LocalDate.now().plusDays(10));
        updatedLoan.setNotes("Updated notes");

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);

        // Act
        LoanEntity result = loanService.updateLoan(1L, updatedLoan);

        // Assert
        assertNotNull(result);
        verify(loanRepository, times(1)).save(testLoan);
    }

    @Test
    void testUpdateLoan_NotFound() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.updateLoan(1L, testLoan));
    }

    @Test
    void testDeleteLoan_Success() {
        // Arrange
        testLoan.setStatus(LoanEntity.LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(fineService.getFinesByLoan(testLoan)).thenReturn(Collections.emptyList());

        // Act
        loanService.deleteLoan(1L);

        // Assert
        verify(loanRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteLoan_ActiveLoan() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.deleteLoan(1L));
    }

    @Test
    void testReturnTool_SuccessGoodCondition() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);

        // Act
        LoanEntity result = loanService.returnTool(1L, false, "MINOR", "Returned in good condition");

        // Assert
        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getActualReturnDate());
        assertEquals(LoanEntity.LoanStatus.RETURNED, result.getStatus());
        verify(loanRepository, times(1)).save(testLoan);
    }



    @Test
    void testReturnTool_Overdue() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(5));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);

        // CORREGIDO: Configurar el mock para createLateFine
        when(fineService.createLateFine(any(LoanEntity.class), anyLong(), any(BigDecimal.class)))
                .thenReturn(new FineEntity());

        // Act
        LoanEntity result = loanService.returnTool(1L, false, "MINOR", null);

        // Assert
        assertNotNull(result);
        assertEquals(LoanEntity.LoanStatus.OVERDUE, result.getStatus());
        verify(fineService, times(1)).createLateFine(any(LoanEntity.class), anyLong(), any(BigDecimal.class));
    }


    @Test
    void testGetLoansByTool_Success() {
        // Arrange
        List<LoanEntity> expectedLoans = Arrays.asList(testLoan);
        when(loanRepository.findByTool(testTool)).thenReturn(expectedLoans);

        // Act
        List<LoanEntity> result = loanService.getLoansByTool(testTool);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(loanRepository, times(1)).findByTool(testTool);
    }

    @Test
    void testGetLoanSummary_Success() {
        // Arrange
        List<LoanEntity> allLoans = Arrays.asList(testLoan);
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(loanRepository.findActiveLoans()).thenReturn(allLoans);
        when(loanRepository.findOverdueLoans(LocalDate.now())).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = loanService.getLoanSummary();

        // Assert
        assertNotNull(result);
        assertEquals(1, ((Number) result.get("totalLoans")).intValue());
        assertEquals(1, ((Number) result.get("activeLoans")).intValue());
        assertEquals(0, ((Number) result.get("overdueLoans")).intValue());
    }

    @Test
    void testGetLoanValidationSummary_Valid() {
        // Arrange
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.valueOf(50.0));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.valueOf(10.0));

        // Act
        LoanService.LoanValidationSummary result = loanService.getLoanValidationSummary(testClient, testTool, 1);

        // Assert
        assertTrue(result.isClientEligible());
        assertTrue(result.isToolAvailable());
        assertFalse(result.isHasExistingLoanForTool());
        assertTrue(result.canCreateLoan());
    }

    @Test
    void testGenerateOverdueFinesForAllLoans_NoOverdueLoans() {
        // Arrange
        when(loanService.getOverdueLoans()).thenReturn(Collections.emptyList());
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);

        // Act
        Map<String, Object> result = loanService.generateOverdueFinesForAllLoans();

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals(0, ((Number) result.get("overdueLoans")).intValue());
        assertEquals(0, ((Number) result.get("finesCreated")).intValue());
    }

    @Test
    void testGetOverdueFinesStatistics() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(3));
        List<LoanEntity> overdueLoans = Arrays.asList(testLoan);

        when(loanService.getOverdueLoans()).thenReturn(overdueLoans);
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);
        when(fineService.getFinesByLoan(testLoan)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = loanService.getOverdueFinesStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(1, ((Number) result.get("overdueLoans")).intValue());
        assertNotNull(result.get("totalPotentialAmount"));
    }

    @Test
    void testTryReserveToolInstancesAndGetFirst_Success() {
        // Arrange
        List<ToolInstanceEntity> instances = Arrays.asList(testInstance);
        when(toolInstanceService.reserveInstancesForLoan(1L, 1)).thenReturn(instances);

        // Usar ReflectionTestUtils para llamar al método protegido
        ToolInstanceEntity result = (ToolInstanceEntity) ReflectionTestUtils.invokeMethod(
                loanService, "tryReserveToolInstancesAndGetFirst", 1L, 1);

        // Assert
        assertNotNull(result);
        assertEquals(testInstance.getId(), result.getId());
    }

    @Test
    void testTryReserveToolInstancesAndGetFirst_ServiceNotAvailable() {
        // Configurar el servicio como null
        LoanService serviceWithNullInstance = new LoanService();

        // Usar ReflectionTestUtils para llamar al método protegido
        ToolInstanceEntity result = (ToolInstanceEntity) ReflectionTestUtils.invokeMethod(
                serviceWithNullInstance, "tryReserveToolInstancesAndGetFirst", 1L, 1);

        // Assert
        assertNull(result);
    }

    @Test
    void testCheckClientToolLoan_NullClient() {
        // Act
        Map<String, Object> result = loanService.checkClientToolLoan(null, testTool);

        // Assert
        assertFalse((Boolean) result.get("hasActiveLoanForTool"));
        assertFalse((Boolean) result.get("canLoanThisTool"));
        assertEquals("Cliente no válido", result.get("message"));
        assertEquals("Cliente no encontrado", result.get("error"));
    }

    @Test
    void testCheckClientToolLoan_NullTool() {
        // Act
        Map<String, Object> result = loanService.checkClientToolLoan(testClient, null);

        // Assert
        assertFalse((Boolean) result.get("hasActiveLoanForTool"));
        assertFalse((Boolean) result.get("canLoanThisTool"));
        assertEquals("Herramienta no válida", result.get("message"));
        assertEquals("Herramienta no encontrada", result.get("error"));
    }

    @Test
    void testCheckClientToolLoan_Exception() {
        // Arrange
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> result = loanService.checkClientToolLoan(testClient, testTool);

        // Assert
        assertFalse((Boolean) result.get("hasActiveLoanForTool"));
        assertFalse((Boolean) result.get("canLoanThisTool"));
        assertEquals("Error en la verificación", result.get("message"));
        assertTrue(((String) result.get("error")).contains("Error al verificar préstamo"));
    }

    @Test
    void testGetActiveLoanCount_NullClientId() {
        // Act
        Map<String, Object> result = loanService.getActiveLoanCount(null);

        // Assert
        assertEquals(0L, ((Number) result.get("activeLoanCount")).longValue());
        assertFalse((Boolean) result.get("canRequestMore"));
        assertEquals("ID de cliente inválido", result.get("error"));
    }

    @Test
    void testGetActiveLoanCount_ClientNotFound() {
        // Arrange
        when(clientService.getClientById(999L)).thenReturn(null);

        // Act
        Map<String, Object> result = loanService.getActiveLoanCount(999L);

        // Assert
        assertEquals(0L, ((Number) result.get("activeLoanCount")).longValue());
        assertFalse((Boolean) result.get("canRequestMore"));
        assertEquals("Cliente no encontrado", result.get("error"));
    }

    @Test
    void testGetActiveLoanCount_Exception() {
        // Arrange
        when(clientService.getClientById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> result = loanService.getActiveLoanCount(1L);

        // Assert
        assertEquals(0L, ((Number) result.get("activeLoanCount")).longValue());
        assertFalse((Boolean) result.get("canRequestMore"));
        assertTrue(((String) result.get("error")).contains("Error al contar préstamos activos"));
    }

    @Test
    void testGetActiveLoans_FinalFallbackToFindAll() {
        // Arrange
        when(loanRepository.findActiveLoans()).thenThrow(new RuntimeException("Query error"));
        when(loanRepository.findByStatus(LoanEntity.LoanStatus.ACTIVE))
                .thenThrow(new RuntimeException("Fallback error"));

        List<LoanEntity> allLoans = Arrays.asList(testLoan);
        when(loanRepository.findAll()).thenReturn(allLoans);

        // Act
        List<LoanEntity> result = loanService.getActiveLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveLoans_AllMethodsFail() {
        // Arrange
        when(loanRepository.findActiveLoans()).thenThrow(new RuntimeException("Query error"));
        when(loanRepository.findByStatus(LoanEntity.LoanStatus.ACTIVE))
                .thenThrow(new RuntimeException("Fallback error"));
        when(loanRepository.findAll()).thenThrow(new RuntimeException("Final fallback error"));

        // Act
        List<LoanEntity> result = loanService.getActiveLoans();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetOverdueLoans_Exception() {
        // Arrange
        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Query error"));

        // Mock getActiveLoans para el fallback
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(1));
        when(loanRepository.findActiveLoans()).thenReturn(Arrays.asList(testLoan));

        // Act
        List<LoanEntity> result = loanService.getOverdueLoans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOverdueLoans_FallbackException() {
        // Arrange
        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Query error"));
        when(loanRepository.findActiveLoans()).thenThrow(new RuntimeException("Fallback error"));

        // Act
        List<LoanEntity> result = loanService.getOverdueLoans();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckToolAvailability_Exception() {
        // Arrange - Crear una herramienta que lance excepción al acceder a sus propiedades
        ToolEntity problematicTool = mock(ToolEntity.class);
        when(problematicTool.getStatus()).thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> result = loanService.checkToolAvailability(problematicTool, 1);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertTrue(((String) result.get("issue")).contains("Error al verificar disponibilidad"));
        assertTrue((Boolean) result.get("error"));
    }

    @Test
    void testCheckToolAvailability_NullQuantity() {
        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, null);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertEquals("Cantidad debe ser mayor a 0", result.get("issue"));
    }

    @Test
    void testCheckToolAvailability_InsufficientStock() {
        // Arrange
        testTool.setCurrentStock(0);

        // Act
        Map<String, Object> result = loanService.checkToolAvailability(testTool, 1);

        // Assert
        assertFalse((Boolean) result.get("available"));
        assertTrue(((String) result.get("issue")).contains("Stock insuficiente"));
        assertEquals("INSUFFICIENT_STOCK", result.get("issueType"));
    }

    @Test
    void testCreateLoan_InvalidQuantity() {
        // Arrange
        testLoan.setQuantity(0);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ToolNotAvailable() {
        // Arrange
        testTool.setStatus(ToolEntity.ToolStatus.UNDER_REPAIR);
        testLoan.setTool(testTool);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ClientHasExistingLoan() {
        // Arrange
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(true);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_MaxActiveLoansReached() {
        // Arrange
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(5L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_InvalidReturnDate() {
        // Arrange
        testLoan.setLoanDate(LocalDate.now());
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(1));
        lenient().when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        lenient().when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        lenient().when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ReturnDateEqualsLoanDate() {
        // Arrange
        testLoan.setLoanDate(LocalDate.now());
        testLoan.setAgreedReturnDate(LocalDate.now());
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ClientHasOverdueLoans() {
        // Arrange
        lenient().when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Arrays.asList(testLoan));
        lenient().when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }


    @Test
    void testCreateLoan_InsufficientStock() {
        // Arrange
        testTool.setCurrentStock(0);
        testLoan.setTool(testTool);
        lenient().when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        lenient().when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        lenient().when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_NullData() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(null));
    }

    @Test
    void testCreateLoan_NullTool() {
        // Arrange
        testLoan.setTool(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_NullAgreedReturnDate() {
        // Arrange
        testLoan.setAgreedReturnDate(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_InvalidRentalRate() {
        // Arrange
        testTool.setRentalRate(BigDecimal.ZERO);
        testLoan.setTool(testTool);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.createLoan(testLoan));
    }

    @Test
    void testCreateLoan_ToolStockBecomesZero() {
        // Arrange
        testTool.setCurrentStock(1); // Solo 1 disponible
        testLoan.setQuantity(1);
        when(loanRepository.countActiveLoansByClient(testClient)).thenReturn(2L);
        when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Collections.emptyList());
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(false);
        when(fineService.clientHasUnpaidFines(testClient)).thenReturn(false);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        // Act
        LoanEntity result = loanService.createLoan(testLoan);

        // Assert
        assertNotNull(result);
        assertEquals(0, testTool.getCurrentStock());
        assertEquals(ToolEntity.ToolStatus.LOANED, testTool.getStatus());
    }

    @Test
    void testUpdateLoan_NotActive() {
        // Arrange
        testLoan.setStatus(LoanEntity.LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.updateLoan(1L, testLoan));
    }

    @Test
    void testUpdateLoan_PastReturnDate() {
        // Arrange
        LoanEntity updatedLoan = new LoanEntity();
        updatedLoan.setAgreedReturnDate(LocalDate.now().minusDays(1));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loanService.updateLoan(1L, updatedLoan));
    }


    @Test
    void testReturnTool_NotActive() {
        // Arrange
        testLoan.setStatus(LoanEntity.LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> loanService.returnTool(1L, false, "MINOR", null));
    }

    @Test
    void testReturnTool_DamagedIrreparable() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);
        when(toolInstanceService.decommissionInstances(anyLong(), anyInt()))
                .thenReturn(Arrays.asList(testInstance));
        when(toolInstanceService.getAvailableCount(anyLong())).thenReturn(0L);
        when(toolInstanceService.getInstancesByStatus(any())).thenReturn(Collections.emptyList());

        // Act
        LoanEntity result = loanService.returnTool(1L, true, "IRREPARABLE", "Broken beyond repair");

        // Assert
        assertNotNull(result);
        assertEquals(LoanEntity.LoanStatus.DAMAGED, result.getStatus());
        verify(toolInstanceService, times(1)).decommissionInstances(1L, 1);
    }

    @Test
    void testReturnTool_DamagedMinor() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);
        when(toolInstanceService.returnInstancesFromLoan(anyLong(), anyInt(), anyBoolean()))
                .thenReturn(Arrays.asList(testInstance));
        when(toolInstanceService.getAvailableCount(anyLong())).thenReturn(0L);
        when(toolInstanceService.getInstancesByStatus(any())).thenReturn(Collections.emptyList());

        // Act
        LoanEntity result = loanService.returnTool(1L, true, "MINOR", "Small scratch");

        // Assert
        assertNotNull(result);
        assertEquals(LoanEntity.LoanStatus.DAMAGED, result.getStatus());
        verify(toolInstanceService, times(1)).returnInstancesFromLoan(1L, 1, true);
    }

    @Test
    void testReturnTool_WithNotesAdded() {
        // Arrange
        testLoan.setNotes("Initial notes");
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);

        // Act
        LoanEntity result = loanService.returnTool(1L, false, "MINOR", "Additional notes");

        // Assert
        assertNotNull(result);
        assertTrue(result.getNotes().contains("Additional notes"));
    }

    @Test
    void testGetLoansByTool_NullTool() {
        // Act
        List<LoanEntity> result = loanService.getLoansByTool(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLoansByTool_Exception() {
        // Arrange
        when(loanRepository.findByTool(testTool)).thenThrow(new RuntimeException("Database error"));

        // Act
        List<LoanEntity> result = loanService.getLoansByTool(testTool);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLoanSummary_WithMultipleStatuses() {
        // Arrange
        LoanEntity returnedLoan = new LoanEntity();
        returnedLoan.setId(2L);
        returnedLoan.setStatus(LoanEntity.LoanStatus.RETURNED);
        returnedLoan.setLoanDate(LocalDate.now());
        returnedLoan.setClient(testClient);

        LoanEntity damagedLoan = new LoanEntity();
        damagedLoan.setId(3L);
        damagedLoan.setStatus(LoanEntity.LoanStatus.DAMAGED);
        damagedLoan.setLoanDate(LocalDate.now().minusMonths(1));
        damagedLoan.setClient(testClient);

        List<LoanEntity> allLoans = Arrays.asList(testLoan, returnedLoan, damagedLoan);
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(loanRepository.findActiveLoans()).thenReturn(Arrays.asList(testLoan));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = loanService.getLoanSummary();

        // Assert
        assertNotNull(result);
        assertEquals(3, ((Number) result.get("totalLoans")).intValue());
        assertEquals(1, ((Number) result.get("activeLoans")).intValue());
        assertEquals(1L, ((Number) result.get("returnedLoans")).longValue());
        assertEquals(1L, ((Number) result.get("damagedLoans")).longValue());
        assertEquals(2L, ((Number) result.get("loansThisMonth")).longValue()); // returnedLoan y testLoan
    }

    @Test
    void testGetLoanValidationSummary_AllInvalid() {
        // Arrange
        testClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);
        testTool.setStatus(ToolEntity.ToolStatus.UNDER_REPAIR);
        lenient().when(loanRepository.findOverdueLoansByClient(testClient, LocalDate.now()))
                .thenReturn(Arrays.asList(testLoan));
        when(loanRepository.existsActiveLoanByClientAndTool(testClient, testTool)).thenReturn(true);
        lenient().when(rateService.getCurrentRentalRate()).thenReturn(BigDecimal.valueOf(50.0));
        lenient().when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.valueOf(10.0));

        // Act
        LoanService.LoanValidationSummary result =
            loanService.getLoanValidationSummary(testClient, testTool, 1);

        // Assert
        assertFalse(result.isClientEligible());
        assertFalse(result.isToolAvailable());
        assertTrue(result.isHasExistingLoanForTool());
        assertFalse(result.canCreateLoan());
    }

    @Test
    void testGetLoanValidationSummary_Exception() {
        // Arrange
        when(loanRepository.findOverdueLoansByClient(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // El método intenta validar el tool también, debemos mockearlo
        lenient().when(loanRepository.existsActiveLoanByClientAndTool(any(), any())).thenReturn(false);

        // Act
        LoanService.LoanValidationSummary result =
            loanService.getLoanValidationSummary(testClient, testTool, 1);

        // Assert
        assertFalse(result.isClientEligible());
        assertTrue(result.isToolAvailable()); // La herramienta está disponible
        assertNotNull(result.getClientIssue());
    }

    @Test
    void testGenerateOverdueFinesForAllLoans_WithOverdueLoans() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(3));
        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(Arrays.asList(testLoan));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);
        when(fineService.getFinesByLoan(testLoan)).thenReturn(Collections.emptyList());

        FineEntity newFine = new FineEntity();
        newFine.setId(1L);
        when(fineService.createFine(any(FineEntity.class))).thenReturn(newFine);

        // Act
        Map<String, Object> result = loanService.generateOverdueFinesForAllLoans();

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals(1, ((Number) result.get("overdueLoans")).intValue());
        assertEquals(1, ((Number) result.get("finesCreated")).intValue());
    }

    @Test
    void testGenerateOverdueFinesForAllLoans_UpdateExistingFine() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(5));

        FineEntity existingFine = new FineEntity();
        existingFine.setId(1L);
        existingFine.setType(FineEntity.FineType.LATE_RETURN);
        existingFine.setAmount(BigDecimal.valueOf(30)); // 3 días anteriores
        existingFine.setPaid(false);

        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(Arrays.asList(testLoan));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);
        when(fineService.getFinesByLoan(testLoan)).thenReturn(Arrays.asList(existingFine));
        when(fineService.updateFine(anyLong(), anyString(), any())).thenReturn(existingFine);

        // Act
        Map<String, Object> result = loanService.generateOverdueFinesForAllLoans();

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals(1, ((Number) result.get("overdueLoans")).intValue());
        assertEquals(0, ((Number) result.get("finesCreated")).intValue());
        assertEquals(1, ((Number) result.get("finesUpdated")).intValue());
    }

    @Test
    void testGenerateOverdueFinesForAllLoans_WithErrors() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(3));
        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(Arrays.asList(testLoan));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);
        when(fineService.getFinesByLoan(testLoan))
                .thenThrow(new RuntimeException("Fine service error"));

        // Act
        Map<String, Object> result = loanService.generateOverdueFinesForAllLoans();

        // Assert
        assertTrue((Boolean) result.get("success"));
        assertEquals(1, ((Number) result.get("errors")).intValue());
    }

    @Test
    void testGenerateOrUpdateOverdueFine_NotOverdue() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().plusDays(5));

        // Act
        boolean result = loanService.generateOrUpdateOverdueFine(
            testLoan, BigDecimal.TEN, LocalDate.now());

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetOverdueFinesStatistics_WithMixedFines() {
        // Arrange
        testLoan.setAgreedReturnDate(LocalDate.now().minusDays(3));

        LoanEntity loan2 = new LoanEntity();
        loan2.setId(2L);
        loan2.setAgreedReturnDate(LocalDate.now().minusDays(2));

        FineEntity fine1 = new FineEntity();
        fine1.setType(FineEntity.FineType.LATE_RETURN);
        fine1.setAmount(BigDecimal.valueOf(30));
        fine1.setPaid(false);

        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(Arrays.asList(testLoan, loan2));
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);
        when(fineService.getFinesByLoan(testLoan)).thenReturn(Arrays.asList(fine1));
        when(fineService.getFinesByLoan(loan2)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = loanService.getOverdueFinesStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(2, ((Number) result.get("overdueLoans")).intValue());
        assertEquals(1, ((Number) result.get("loansWithActiveFine")).intValue());
        assertEquals(1, ((Number) result.get("loansWithoutFine")).intValue());
        assertTrue((Boolean) result.get("needsUpdate"));
    }

    @Test
    void testGetOverdueFinesStatistics_Exception() {
        // Arrange
        // getOverdueLoans tiene fallbacks que devuelven lista vacía en caso de error
        when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(loanRepository.findActiveLoans()).thenThrow(new RuntimeException("Fallback error"));
        when(loanRepository.findByStatus(any())).thenThrow(new RuntimeException("Fallback2 error"));
        when(loanRepository.findAll()).thenThrow(new RuntimeException("Final fallback error"));

        // Act
        Map<String, Object> result = loanService.getOverdueFinesStatistics();

        // Assert
        assertNotNull(result);
        // Sin préstamos vencidos, no habrá error sino valores en 0
        assertEquals(0, ((Number) result.get("overdueLoans")).intValue());
    }

    @Test
    void testTryReserveToolInstancesAndGetFirst_Exception() {
        // Arrange
        when(toolInstanceService.reserveInstancesForLoan(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Instance reservation failed"));

        // Act
        ToolInstanceEntity result = (ToolInstanceEntity) ReflectionTestUtils.invokeMethod(
                loanService, "tryReserveToolInstancesAndGetFirst", 1L, 1);

        // Assert
        assertNull(result);
    }

    @Test
    void testReturnTool_ToolInstanceServiceException() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(testLoan);
        when(toolService.updateTool(anyLong(), any(ToolEntity.class))).thenReturn(testTool);
        when(toolInstanceService.returnInstancesFromLoan(anyLong(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("Instance service error"));

        // Act
        LoanEntity result = loanService.returnTool(1L, false, "MINOR", null);

        // Assert
        assertNotNull(result);
        assertEquals(LoanEntity.LoanStatus.RETURNED, result.getStatus());
    }
}