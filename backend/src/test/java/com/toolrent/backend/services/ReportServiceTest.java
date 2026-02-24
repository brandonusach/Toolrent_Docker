package com.toolrent.backend.services;

import com.toolrent.backend.dto.*;
import com.toolrent.backend.entities.*;
import com.toolrent.backend.repositories.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RateService rateService;

    @InjectMocks
    private ReportService reportService;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity tool1;
    private ToolEntity tool2;
    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        // Setup básico de entidades para usar en los tests
        category = new CategoryEntity(1L, "Construction", "Heavy tools");

        client1 = new ClientEntity();
        client1.setId(1L);
        client1.setName("Juan Perez");
        client1.setEmail("juan@test.com");
        client1.setPhone("123456789");

        client2 = new ClientEntity();
        client2.setId(2L);
        client2.setName("Maria Gomez");

        tool1 = new ToolEntity();
        tool1.setId(10L);
        tool1.setName("Drill");
        tool1.setCategory(category);
        tool1.setRentalRate(BigDecimal.valueOf(1000));

        tool2 = new ToolEntity();
        tool2.setId(20L);
        tool2.setName("Saw");
        tool2.setCategory(category);
        tool2.setRentalRate(BigDecimal.valueOf(1500));
    }

    // --- RF6.1 Active Loans Report Tests ---

    @Test
    void getActiveLoansReport_ShouldCalculateStatisticsCorrectly() {
        LocalDate now = LocalDate.now();

        // Préstamo 1: Activo, a tiempo
        LoanEntity loan1 = createLoan(1L, client1, tool1, now.minusDays(5), now.plusDays(5), null);
        // Préstamo 2: Activo, Atrasado (Debió volver ayer)
        LoanEntity loan2 = createLoan(2L, client2, tool2, now.minusDays(10), now.minusDays(1), null);
        // Préstamo 3: Devuelto (No debe aparecer en activos)
        LoanEntity loan3 = createLoan(3L, client1, tool2, now.minusDays(20), now.minusDays(15), now.minusDays(15));

        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Arrays.asList(loan1, loan2, loan3));

        ActiveLoansReportDTO report = reportService.getActiveLoansReport(now.minusDays(30), now);

        assertNotNull(report);
        assertEquals(2, report.getLoans().size(), "Should exclude returned loans");

        // Validar Resumen
        ActiveLoansReportDTO.ActiveLoansSummaryDTO summary = report.getSummary();
        assertEquals(2, summary.getTotal());
        assertEquals(1, summary.getActive(), "Only 1 loan is active not overdue");
        assertEquals(1, summary.getOverdue(), "1 loan is overdue");

        // Validar DTO individual
        ActiveLoansReportDTO.ActiveLoanDTO overdueLoan = report.getLoans().stream()
                .filter(ActiveLoansReportDTO.ActiveLoanDTO::isOverdue)
                .findFirst().orElseThrow();
        assertEquals("OVERDUE", overdueLoan.getStatus());
        assertTrue(overdueLoan.getDaysOverdue() >= 1);
    }

    // --- RF6.2 Overdue Clients Report Tests ---

    @Test
    void getOverdueClientsReport_ShouldCalculateFinesAndGroup() {
        LocalDate now = LocalDate.now();
        BigDecimal lateFeeRate = BigDecimal.valueOf(500); // $500 multa diaria

        // Configurar tasa de multa
        when(rateService.getCurrentLateFeeRate()).thenReturn(lateFeeRate);

        // Préstamo atrasado 2 días
        LoanEntity loan1 = createLoan(1L, client1, tool1, now.minusDays(10), now.minusDays(2), null);
        // Préstamo atrasado 5 días (mismo cliente)
        LoanEntity loan2 = createLoan(2L, client1, tool2, now.minusDays(10), now.minusDays(5), null);
        // Préstamo activo sin atraso (otro cliente)
        LoanEntity loan3 = createLoan(3L, client2, tool1, now.minusDays(1), now.plusDays(1), null);

        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Arrays.asList(loan1, loan2, loan3));

        OverdueClientsReportDTO report = reportService.getOverdueClientsReport(now.minusDays(30), now);

        // Debe haber solo 1 cliente en la lista de morosos (client1)
        assertEquals(1, report.getClients().size());

        OverdueClientsReportDTO.OverdueClientDTO clientDTO = report.getClients().get(0);
        assertEquals(client1.getName(), clientDTO.getName());
        assertEquals(2, clientDTO.getLoansCount());

        // Validar cálculos de multa: (2 días * 500) + (5 días * 500) = 1000 + 2500 = 3500
        assertEquals(3500.0, clientDTO.getTotalOverdueAmount());
        assertEquals(5, clientDTO.getMaxDaysOverdue()); // El máximo es 5 días
    }

    @Test
    void getOverdueClientsReport_EmptyList() {
        // Configuramos el repositorio para devolver lista vacía
        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Collections.emptyList());

        OverdueClientsReportDTO report = reportService.getOverdueClientsReport(LocalDate.now(), LocalDate.now());

        assertEquals(0, report.getClients().size());
        assertEquals(0.0, report.getSummary().getTotalOverdueAmount());
    }

    // --- RF6.3 Popular Tools Report Tests ---

    @Test
    void getPopularToolsReport_ShouldSortByPopularity() {
        LocalDate now = LocalDate.now();

        // Drill prestado 2 veces
        LoanEntity loan1 = createLoan(1L, client1, tool1, now, now, now);
        LoanEntity loan2 = createLoan(2L, client2, tool1, now, now, now);

        // Saw prestada 1 vez
        LoanEntity loan3 = createLoan(3L, client1, tool2, now, now, now);

        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Arrays.asList(loan1, loan2, loan3));

        PopularToolsReportDTO report = reportService.getPopularToolsReport(now.minusDays(10), now, 5);

        assertEquals(2, report.getTools().size());

        // El primero debe ser Drill (2 préstamos)
        assertEquals("Drill", report.getTools().get(0).getName());
        assertEquals(2, report.getTools().get(0).getTotalLoans());

        // El segundo debe ser Saw (1 préstamo)
        assertEquals("Saw", report.getTools().get(1).getName());

        // Validar resumen
        assertEquals("Drill", report.getSummary().getMostPopularTool().getName());
        assertEquals(1.5, report.getSummary().getAvgLoansPerTool()); // (2+1)/2 = 1.5
    }

    @Test
    void getPopularToolsReport_WithNullToolData() {
        // Caso borde: Préstamo donde la herramienta fue borrada o es nula
        LoanEntity loan = createLoan(1L, client1, null, LocalDate.now(), LocalDate.now(), null);

        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Collections.singletonList(loan));

        PopularToolsReportDTO report = reportService.getPopularToolsReport(LocalDate.now(), LocalDate.now(), 5);

        // Debe filtrar el préstamo con herramienta nula
        assertEquals(0, report.getTools().size());
    }

    // --- General Summary Tests ---

    @Test
    void getGeneralSummary_IntegrationTest() {
        LocalDate now = LocalDate.now();

        // Setup de mocks para que todas las llamadas internas funcionen
        when(rateService.getCurrentLateFeeRate()).thenReturn(BigDecimal.TEN);

        // Crear datos que generen alertas (atraso > 7 dias)
        LoanEntity riskyLoan = createLoan(1L, client1, tool1, now.minusDays(20), now.minusDays(10), null);
        // Este préstamo tiene 10 días de atraso (activa alerta criticalOverdue)

        when(loanRepository.findByLoanDateBetween(any(), any())).thenReturn(Collections.singletonList(riskyLoan));

        ReportSummaryDTO summary = reportService.getGeneralSummary(now.minusDays(30), now);

        assertNotNull(summary);

        // Verificar que los datos fluyen de los reportes internos al sumario general
        assertEquals(1, summary.getActiveLoans().getTotal());
        assertEquals(1, summary.getActiveLoans().getOverdue());

        // Verificar Alertas
        // 1 cliente con atraso > 7 días. CORREGIDO: getCriticalOverdue() en lugar de getCriticalOverdueClients()
        assertEquals(1, summary.getAlerts().getCriticalOverdue());
    }

    // --- Private Helper Method Coverage (getLoansInPeriod Branch Coverage) ---

    @Test
    void getLoansInPeriod_ShouldHandleNullStartDates() {
        // Test branch: startDate == null
        LocalDate endDate = LocalDate.now();

        LoanEntity loanFuture = createLoan(1L, client1, tool1, endDate.plusDays(10), endDate.plusDays(20), null);
        LoanEntity loanPast = createLoan(2L, client1, tool1, endDate.minusDays(10), endDate.minusDays(5), null);

        // Mock findAll para cuando no hay rango completo
        when(loanRepository.findAll()).thenReturn(Arrays.asList(loanFuture, loanPast));

        // Llamamos a través de un método público para invocar el privado
        ActiveLoansReportDTO report = reportService.getActiveLoansReport(null, endDate);

        // Debería filtrar loanFuture
        assertEquals(1, report.getLoans().size());
        assertEquals(loanPast.getId(), report.getLoans().get(0).getId());
    }

    @Test
    void getLoansInPeriod_ShouldHandleNullEndDates() {
        // Test branch: endDate == null
        LocalDate startDate = LocalDate.now();

        LoanEntity loanPast = createLoan(1L, client1, tool1, startDate.minusDays(10), startDate.minusDays(5), null);
        LoanEntity loanFuture = createLoan(2L, client1, tool1, startDate.plusDays(1), startDate.plusDays(5), null);

        when(loanRepository.findAll()).thenReturn(Arrays.asList(loanPast, loanFuture));

        ActiveLoansReportDTO report = reportService.getActiveLoansReport(startDate, null);

        // Debería filtrar loanPast (antes de start date)
        assertEquals(1, report.getLoans().size());
        assertEquals(loanFuture.getId(), report.getLoans().get(0).getId());
    }

    @Test
    void getLoansInPeriod_ShouldHandleBothDatesNull() {
        // Test branch: both null
        LoanEntity loan = createLoan(1L, client1, tool1, LocalDate.now(), LocalDate.now(), null);

        when(loanRepository.findAll()).thenReturn(Collections.singletonList(loan));

        ActiveLoansReportDTO report = reportService.getActiveLoansReport(null, null);

        assertEquals(1, report.getLoans().size());
    }

    // --- Helper methods for Test Data ---

    private LoanEntity createLoan(Long id, ClientEntity client, ToolEntity tool, LocalDate loanDate, LocalDate agreedReturn, LocalDate actualReturn) {
        LoanEntity loan = new LoanEntity();
        loan.setId(id);
        loan.setClient(client);
        loan.setTool(tool);
        loan.setLoanDate(loanDate);
        loan.setAgreedReturnDate(agreedReturn);
        loan.setActualReturnDate(actualReturn);
        loan.setQuantity(1);
        loan.setDailyRate(new BigDecimal("100.00"));

        // Determinar status básico para el test (la lógica real está en el servicio)
        if (actualReturn != null) {
            loan.setStatus(LoanEntity.LoanStatus.RETURNED);
        } else if (agreedReturn != null && agreedReturn.isBefore(LocalDate.now())) {
            loan.setStatus(LoanEntity.LoanStatus.OVERDUE);
        } else {
            loan.setStatus(LoanEntity.LoanStatus.ACTIVE);
        }

        return loan;
    }
}