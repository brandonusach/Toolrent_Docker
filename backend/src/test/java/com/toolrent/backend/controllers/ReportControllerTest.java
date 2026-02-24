package com.toolrent.backend.controllers;

import com.toolrent.backend.dto.ActiveLoansReportDTO;
import com.toolrent.backend.dto.OverdueClientsReportDTO;
import com.toolrent.backend.dto.PopularToolsReportDTO;
import com.toolrent.backend.dto.ReportSummaryDTO;
import com.toolrent.backend.services.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;
    private ActiveLoansReportDTO activeLoansReport;
    private OverdueClientsReportDTO overdueClientsReport;
    private PopularToolsReportDTO popularToolsReport;
    private ReportSummaryDTO reportSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();

        // Los DTOs tienen estructuras complejas, se crean vacíos y se mockean las respuestas
        activeLoansReport = new ActiveLoansReportDTO();
        overdueClientsReport = new OverdueClientsReportDTO();
        popularToolsReport = new PopularToolsReportDTO();
        reportSummary = new ReportSummaryDTO();
    }

    // ========== Tests for GET /api/v1/reports/active-loans ==========
    @Test
    void getActiveLoansReport_ShouldReturnReport() throws Exception {
        when(reportService.getActiveLoansReport(any(), any()))
                .thenReturn(activeLoansReport);

        mockMvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getActiveLoansReport(any(), any());
    }

    @Test
    void getActiveLoansReport_ShouldReturnReportWithDates() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(reportService.getActiveLoansReport(eq(startDate), eq(endDate)))
                .thenReturn(activeLoansReport);

        mockMvc.perform(get("/api/v1/reports/active-loans")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getActiveLoansReport(eq(startDate), eq(endDate));
    }

    @Test
    void getActiveLoansReport_ShouldReturnError_WhenServiceFails() throws Exception {
        when(reportService.getActiveLoansReport(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getActiveLoansReport(any(), any());
    }

    // ========== Tests for GET /api/v1/reports/overdue-clients ==========
    @Test
    void getOverdueClientsReport_ShouldReturnReport() throws Exception {
        when(reportService.getOverdueClientsReport(any(), any()))
                .thenReturn(overdueClientsReport);

        mockMvc.perform(get("/api/v1/reports/overdue-clients"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getOverdueClientsReport(any(), any());
    }

    @Test
    void getOverdueClientsReport_ShouldReturnReportWithDates() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(reportService.getOverdueClientsReport(eq(startDate), eq(endDate)))
                .thenReturn(overdueClientsReport);

        mockMvc.perform(get("/api/v1/reports/overdue-clients")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1))
                .getOverdueClientsReport(eq(startDate), eq(endDate));
    }

    @Test
    void getOverdueClientsReport_ShouldReturnError_WhenServiceFails() throws Exception {
        when(reportService.getOverdueClientsReport(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/reports/overdue-clients"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getOverdueClientsReport(any(), any());
    }

    // ========== Tests for GET /api/v1/reports/popular-tools ==========
    @Test
    void getPopularToolsReport_ShouldReturnReport() throws Exception {
        when(reportService.getPopularToolsReport(any(), any(), eq(10)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getPopularToolsReport(any(), any(), eq(10));
    }

    @Test
    void getPopularToolsReport_ShouldReturnReportWithCustomLimit() throws Exception {
        when(reportService.getPopularToolsReport(any(), any(), eq(5)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools")
                        .param("limit", "5"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getPopularToolsReport(any(), any(), eq(5));
    }

    @Test
    void getPopularToolsReport_ShouldReturnReportWithDates() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(reportService.getPopularToolsReport(eq(startDate), eq(endDate), eq(10)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("limit", "10"))
                .andExpect(status().isOk());

        verify(reportService, times(1))
                .getPopularToolsReport(eq(startDate), eq(endDate), eq(10));
    }

    @Test
    void getPopularToolsReport_ShouldReturnError_WhenServiceFails() throws Exception {
        when(reportService.getPopularToolsReport(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/reports/popular-tools"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getPopularToolsReport(any(), any(), anyInt());
    }

    // ========== Tests for GET /api/v1/reports/summary ==========
    @Test
    void getReportSummary_ShouldReturnSummary() throws Exception {
        when(reportService.getGeneralSummary(any(), any()))
                .thenReturn(reportSummary);

        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getGeneralSummary(any(), any());
    }

    @Test
    void getReportSummary_ShouldReturnSummaryWithDates() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(reportService.getGeneralSummary(eq(startDate), eq(endDate)))
                .thenReturn(reportSummary);

        mockMvc.perform(get("/api/v1/reports/summary")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getGeneralSummary(eq(startDate), eq(endDate));
    }

    @Test
    void getReportSummary_ShouldReturnError_WhenServiceFails() throws Exception {
        when(reportService.getGeneralSummary(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getGeneralSummary(any(), any());
    }

    // ========== Tests adicionales para aumentar cobertura ==========
    @Test
    void getActiveLoansReport_ShouldHandleOnlyStartDate() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(7);

        when(reportService.getActiveLoansReport(eq(startDate), any()))
                .thenReturn(activeLoansReport);

        mockMvc.perform(get("/api/v1/reports/active-loans")
                        .param("startDate", startDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getActiveLoansReport(eq(startDate), any());
    }

    @Test
    void getActiveLoansReport_ShouldHandleOnlyEndDate() throws Exception {
        LocalDate endDate = LocalDate.now();

        when(reportService.getActiveLoansReport(any(), eq(endDate)))
                .thenReturn(activeLoansReport);

        mockMvc.perform(get("/api/v1/reports/active-loans")
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getActiveLoansReport(any(), eq(endDate));
    }

    @Test
    void getOverdueClientsReport_ShouldHandleNullPointerException() throws Exception {
        when(reportService.getOverdueClientsReport(any(), any()))
                .thenThrow(new NullPointerException("Null value"));

        mockMvc.perform(get("/api/v1/reports/overdue-clients"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getOverdueClientsReport(any(), any());
    }

    @Test
    void getPopularToolsReport_ShouldHandleLargeLimit() throws Exception {
        when(reportService.getPopularToolsReport(any(), any(), eq(100)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools")
                        .param("limit", "100"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getPopularToolsReport(any(), any(), eq(100));
    }

    @Test
    void getPopularToolsReport_ShouldHandleMinimalLimit() throws Exception {
        when(reportService.getPopularToolsReport(any(), any(), eq(1)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools")
                        .param("limit", "1"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getPopularToolsReport(any(), any(), eq(1));
    }

    @Test
    void getReportSummary_ShouldHandleNullDates() throws Exception {
        when(reportService.getGeneralSummary(isNull(), isNull()))
                .thenReturn(reportSummary);

        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getGeneralSummary(any(), any());
    }

    @Test
    void getActiveLoansReport_ShouldHandleIllegalArgumentException() throws Exception {
        when(reportService.getActiveLoansReport(any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid dates"));

        mockMvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isInternalServerError());

        verify(reportService, times(1)).getActiveLoansReport(any(), any());
    }

    @Test
    void getPopularToolsReport_ShouldHandleAllParameters() throws Exception {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();

        when(reportService.getPopularToolsReport(eq(startDate), eq(endDate), eq(20)))
                .thenReturn(popularToolsReport);

        mockMvc.perform(get("/api/v1/reports/popular-tools")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("limit", "20"))
                .andExpect(status().isOk());

        verify(reportService, times(1))
                .getPopularToolsReport(eq(startDate), eq(endDate), eq(20));
    }

    @Test
    void getReportSummary_ShouldHandleLongDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = LocalDate.now();

        when(reportService.getGeneralSummary(eq(startDate), eq(endDate)))
                .thenReturn(reportSummary);

        mockMvc.perform(get("/api/v1/reports/summary")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1)).getGeneralSummary(eq(startDate), eq(endDate));
    }

    @Test
    void getOverdueClientsReport_ShouldHandleShortDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        when(reportService.getOverdueClientsReport(eq(startDate), eq(endDate)))
                .thenReturn(overdueClientsReport);

        mockMvc.perform(get("/api/v1/reports/overdue-clients")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk());

        verify(reportService, times(1))
                .getOverdueClientsReport(eq(startDate), eq(endDate));
    }
}

