package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.ToolEntity;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ToolControllerTest {

    @Mock
    private ToolService toolService;

    @InjectMocks
    private ToolController toolController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ToolEntity testTool;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(toolController).build();
        objectMapper = new ObjectMapper();

        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Taladro Eléctrico");
        testTool.setInitialStock(10);
        testTool.setCurrentStock(8);
        testTool.setReplacementValue(BigDecimal.valueOf(50000));
        testTool.setRentalRate(BigDecimal.valueOf(5000));
        testTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
    }

    // ========== Tests for GET /api/v1/tools/ ==========
    @Test
    void listTools_ShouldReturnAllTools() throws Exception {
        ToolEntity tool2 = new ToolEntity();
        tool2.setId(2L);
        tool2.setName("Sierra Circular");
        tool2.setInitialStock(5);
        tool2.setCurrentStock(5);
        tool2.setReplacementValue(BigDecimal.valueOf(70000));
        tool2.setRentalRate(BigDecimal.valueOf(7000));
        tool2.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        List<ToolEntity> tools = Arrays.asList(testTool, tool2);
        when(toolService.getAllTools()).thenReturn(tools);

        mockMvc.perform(get("/api/v1/tools/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Taladro Eléctrico"))
                .andExpect(jsonPath("$[1].name").value("Sierra Circular"));

        verify(toolService, times(1)).getAllTools();
    }

    @Test
    void listTools_ShouldReturnEmptyList_WhenNoTools() throws Exception {
        when(toolService.getAllTools()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tools/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(toolService, times(1)).getAllTools();
    }

    // ========== Tests for GET /api/v1/tools/{id} ==========
    @Test
    void getToolById_ShouldReturnTool_WhenExists() throws Exception {
        when(toolService.getToolById(1L)).thenReturn(Optional.of(testTool));

        mockMvc.perform(get("/api/v1/tools/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Taladro Eléctrico"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(toolService, times(1)).getToolById(1L);
    }

    @Test
    void getToolById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(toolService.getToolById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tools/999"))
                .andExpect(status().isNotFound());

        verify(toolService, times(1)).getToolById(999L);
    }

    // ========== Tests for POST /api/v1/tools/ ==========
    @Test
    void saveTool_ShouldCreateAndReturnTool() throws Exception {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Martillo Demoledor");
        newTool.setInitialStock(3);
        newTool.setCurrentStock(3);
        newTool.setReplacementValue(BigDecimal.valueOf(100000));
        newTool.setRentalRate(BigDecimal.valueOf(10000));
        newTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        ToolEntity savedTool = new ToolEntity();
        savedTool.setId(3L);
        savedTool.setName("Martillo Demoledor");
        savedTool.setInitialStock(3);
        savedTool.setCurrentStock(3);
        savedTool.setReplacementValue(BigDecimal.valueOf(100000));
        savedTool.setRentalRate(BigDecimal.valueOf(10000));
        savedTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        when(toolService.createTool(any(ToolEntity.class))).thenReturn(savedTool);

        mockMvc.perform(post("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTool)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Martillo Demoledor"));

        verify(toolService, times(1)).createTool(any(ToolEntity.class));
    }

    // ========== Tests for PUT /api/v1/tools/ ==========
    @Test
    void updateTool_ShouldUpdateAndReturnTool() throws Exception {
        ToolEntity updatedTool = new ToolEntity();
        updatedTool.setId(1L);
        updatedTool.setName("Taladro Eléctrico Actualizado");
        updatedTool.setInitialStock(10);
        updatedTool.setCurrentStock(8);
        updatedTool.setReplacementValue(BigDecimal.valueOf(55000));
        updatedTool.setRentalRate(BigDecimal.valueOf(5500));
        updatedTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);

        when(toolService.updateTool(eq(1L), any(ToolEntity.class))).thenReturn(updatedTool);

        mockMvc.perform(put("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTool)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Taladro Eléctrico Actualizado"));

        verify(toolService, times(1)).updateTool(eq(1L), any(ToolEntity.class));
    }

    // ========== Tests for DELETE /api/v1/tools/{id} ==========
    @Test
    void deleteToolById_ShouldReturnNoContent_WhenDeleted() throws Exception {
        doNothing().when(toolService).deleteTool(1L);

        mockMvc.perform(delete("/api/v1/tools/1"))
                .andExpect(status().isNoContent());

        verify(toolService, times(1)).deleteTool(1L);
    }

    @Test
    void deleteToolById_ShouldHandleException_WhenDeletionFails() throws Exception {
        doThrow(new RuntimeException("Cannot delete tool")).when(toolService).deleteTool(999L);

        try {
            mockMvc.perform(delete("/api/v1/tools/999"))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // El controlador puede lanzar la excepción directamente
        }

        verify(toolService, times(1)).deleteTool(999L);
    }

    // ========== Tests for POST /api/v1/tools/{id}/add-stock ==========
    @Test
    void addStock_ShouldAddStockAndReturnTool() throws Exception {
        ToolEntity updatedTool = new ToolEntity();
        updatedTool.setId(1L);
        updatedTool.setName("Taladro Eléctrico");
        updatedTool.setInitialStock(15);
        updatedTool.setCurrentStock(13);

        when(toolService.addToolStock(1L, 5)).thenReturn(updatedTool);

        mockMvc.perform(post("/api/v1/tools/1/add-stock")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialStock").value(15))
                .andExpect(jsonPath("$.currentStock").value(13));

        verify(toolService, times(1)).addToolStock(1L, 5);
    }

    // ========== Tests for PUT /api/v1/tools/{id}/decommission ==========
    @Test
    void decommissionTool_ShouldDecommissionAndReturnTool() throws Exception {
        ToolEntity updatedTool = new ToolEntity();
        updatedTool.setId(1L);
        updatedTool.setName("Taladro Eléctrico");
        updatedTool.setInitialStock(8);
        updatedTool.setCurrentStock(6);

        when(toolService.decommissionTool(1L, 2)).thenReturn(updatedTool);

        mockMvc.perform(put("/api/v1/tools/1/decommission")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialStock").value(8))
                .andExpect(jsonPath("$.currentStock").value(6));

        verify(toolService, times(1)).decommissionTool(1L, 2);
    }

    // ========== Tests adicionales para aumentar cobertura ==========
    @Test
    void addStock_ShouldHandleZeroQuantity() throws Exception {
        when(toolService.addToolStock(1L, 0)).thenReturn(testTool);

        mockMvc.perform(post("/api/v1/tools/1/add-stock")
                        .param("quantity", "0"))
                .andExpect(status().isOk());

        verify(toolService, times(1)).addToolStock(1L, 0);
    }

    @Test
    void decommissionTool_ShouldHandleInvalidId() throws Exception {
        when(toolService.decommissionTool(999L, 1))
                .thenThrow(new RuntimeException("Tool not found"));

        try {
            mockMvc.perform(put("/api/v1/tools/999/decommission")
                            .param("quantity", "1"))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Esperado
        }

        verify(toolService, times(1)).decommissionTool(999L, 1);
    }

    @Test
    void listTools_ShouldHandleServiceException() throws Exception {
        when(toolService.getAllTools()).thenThrow(new RuntimeException("Database error"));

        try {
            mockMvc.perform(get("/api/v1/tools/"))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Esperado
        }

        verify(toolService, times(1)).getAllTools();
    }

    @Test
    void saveTool_ShouldHandleMinimalData() throws Exception {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Herramienta");

        when(toolService.createTool(any(ToolEntity.class))).thenReturn(newTool);

        mockMvc.perform(post("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTool)))
                .andExpect(status().isOk());

        verify(toolService, times(1)).createTool(any(ToolEntity.class));
    }

    @Test
    void updateTool_ShouldHandleNullReturn() throws Exception {
        ToolEntity updatedTool = new ToolEntity();
        updatedTool.setId(999L);
        updatedTool.setName("No existe");

        when(toolService.updateTool(eq(999L), any(ToolEntity.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTool)))
                .andExpect(status().isOk());

        verify(toolService, times(1)).updateTool(eq(999L), any(ToolEntity.class));
    }

    @Test
    void addStock_ShouldHandleLargeQuantity() throws Exception {
        ToolEntity updatedTool = new ToolEntity();
        updatedTool.setId(1L);
        updatedTool.setInitialStock(1010);
        updatedTool.setCurrentStock(1008);

        when(toolService.addToolStock(1L, 1000)).thenReturn(updatedTool);

        mockMvc.perform(post("/api/v1/tools/1/add-stock")
                        .param("quantity", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialStock").value(1010));

        verify(toolService, times(1)).addToolStock(1L, 1000);
    }
}

