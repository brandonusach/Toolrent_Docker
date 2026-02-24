package com.toolrent.backend.services;

import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.entities.ToolEntity;
import com.toolrent.backend.entities.ToolInstanceEntity;
import com.toolrent.backend.repositories.CategoryRepository;
import com.toolrent.backend.repositories.ToolInstanceRepository;
import com.toolrent.backend.repositories.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ToolInstanceRepository toolInstanceRepository;

    @Mock
    private KardexMovementService kardexMovementService;

    @InjectMocks
    private ToolService toolService;

    private CategoryEntity testCategory;
    private ToolEntity testTool;

    @BeforeEach
    void setUp() {
        testCategory = new CategoryEntity();
        testCategory.setId(1L);
        testCategory.setName("Power Tools");
        testCategory.setDescription("Electric power tools");

        testTool = new ToolEntity();
        testTool.setId(1L);
        testTool.setName("Drill");
        testTool.setCategory(testCategory);
        testTool.setInitialStock(10);
        testTool.setCurrentStock(10);
        testTool.setReplacementValue(new BigDecimal("5000"));
        testTool.setRentalRate(new BigDecimal("100"));
        testTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
    }

    // ========== Tests for getAllTools ==========
    @Test
    void getAllTools_ShouldReturnAllTools() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(toolRepository.findAllWithCategories()).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.getAllTools();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).findAllWithCategories();
    }

    // ========== Tests for getToolById ==========
    @Test
    void getToolById_ShouldReturnTool_WhenExists() {
        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        Optional<ToolEntity> result = toolService.getToolById(1L);

        assertTrue(result.isPresent());
        assertEquals("Drill", result.get().getName());
        verify(toolRepository, times(1)).findByIdWithCategory(1L);
    }

    @Test
    void getToolById_ShouldReturnEmpty_WhenNotExists() {
        when(toolRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        Optional<ToolEntity> result = toolService.getToolById(999L);

        assertFalse(result.isPresent());
    }

    // ========== Tests for createTool ==========
    @Test
    void createTool_ShouldCreateSuccessfully_WithValidData() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(toolRepository.findByNameIgnoreCaseAndCategory(anyString(), any())).thenReturn(Optional.empty());
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> {
            ToolEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.findByIdWithCategory(any())).thenReturn(Optional.of(newTool));
        when(kardexMovementService.createInitialStockMovement(any(), anyInt())).thenReturn(null);

        ToolEntity result = toolService.createTool(newTool);

        assertNotNull(result);
        assertEquals("Hammer", result.getName());
        assertEquals(5, result.getCurrentStock());
        assertEquals(ToolEntity.ToolStatus.AVAILABLE, result.getStatus());
        verify(toolInstanceRepository, times(5)).save(any(ToolInstanceEntity.class));
        verify(kardexMovementService, times(1)).createInitialStockMovement(any(), eq(5));
    }

    @Test
    void createTool_ShouldThrowException_WhenNameIsNull() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName(null);
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("El nombre de la herramienta es requerido", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenNameIsEmpty() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("  ");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("El nombre de la herramienta es requerido", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenNameTooShort() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("A");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("debe tener entre"));
    }

    @Test
    void createTool_ShouldThrowException_WhenNameTooLong() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("A".repeat(101));
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("debe tener entre"));
    }

    @Test
    void createTool_ShouldThrowException_WhenCategoryIsNull() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(null);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("La categoría es requerida", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenCategoryIdIsNull() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        CategoryEntity categoryWithoutId = new CategoryEntity();
        newTool.setCategory(categoryWithoutId);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("La categoría es requerida", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenInitialStockIsNull() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(null);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("El stock inicial es requerido", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenInitialStockTooLow() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(0);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("El stock inicial debe estar entre"));
    }

    @Test
    void createTool_ShouldThrowException_WhenInitialStockTooHigh() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(1000);
        newTool.setReplacementValue(new BigDecimal("2000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("El stock inicial debe estar entre"));
    }

    @Test
    void createTool_ShouldThrowException_WhenReplacementValueIsNull() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("El valor de reposición es requerido", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenReplacementValueTooLow() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("500"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("debe ser al menos"));
    }

    @Test
    void createTool_ShouldThrowException_WhenReplacementValueTooHigh() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000000"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("no puede exceder"));
    }

    @Test
    void createTool_ShouldThrowException_WhenCategoryNotFound() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Hammer");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertEquals("Categoría no encontrada", exception.getMessage());
    }

    @Test
    void createTool_ShouldThrowException_WhenDuplicateNameInCategory() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Drill");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(toolRepository.findByNameIgnoreCaseAndCategory("Drill", testCategory))
                .thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.createTool(newTool)
        );

        assertTrue(exception.getMessage().contains("Ya existe una herramienta"));
    }

    // ========== Tests for updateTool ==========
    @Test
    void updateTool_ShouldUpdateSuccessfully_WithValidData() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolRepository.findByNameIgnoreCaseAndCategory(anyString(), any())).thenReturn(Optional.empty());
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        ToolEntity result = toolService.updateTool(1L, updatedDetails);

        assertNotNull(result);
        verify(toolRepository, times(1)).save(any(ToolEntity.class));
    }

    @Test
    void updateTool_ShouldThrowException_WhenToolNotFound() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(999L, updatedDetails)
        );

        assertTrue(exception.getMessage().contains("Herramienta no encontrada"));
    }

    @Test
    void updateTool_ShouldThrowException_WhenNameIsNull() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName(null);
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertEquals("El nombre de la herramienta es requerido", exception.getMessage());
    }

    @Test
    void updateTool_ShouldThrowException_WhenRentalRateIsNull() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(null);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertEquals("La tarifa de arriendo es requerida", exception.getMessage());
    }

    @Test
    void updateTool_ShouldThrowException_WhenRentalRateTooLow() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("0.5"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertTrue(exception.getMessage().contains("debe ser al menos"));
    }

    @Test
    void updateTool_ShouldThrowException_WhenRentalRateTooHigh() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("15000"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertTrue(exception.getMessage().contains("no puede exceder"));
    }

    @Test
    void updateTool_ShouldUpdateCategory_WhenNewCategoryProvided() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setId(2L);
        newCategory.setName("Hand Tools");

        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));
        updatedDetails.setCategory(newCategory);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(toolRepository.findByNameIgnoreCaseAndCategory(anyString(), any())).thenReturn(Optional.empty());
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        ToolEntity result = toolService.updateTool(1L, updatedDetails);

        assertNotNull(result);
        verify(categoryRepository, times(1)).findById(2L);
    }

    @Test
    void updateTool_ShouldThrowException_WhenNewCategoryNotFound() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setId(999L);

        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));
        updatedDetails.setCategory(newCategory);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertEquals("Categoría no encontrada", exception.getMessage());
    }

    @Test
    void updateTool_ShouldThrowException_WhenDuplicateNameExists() {
        ToolEntity anotherTool = new ToolEntity();
        anotherTool.setId(2L);
        anotherTool.setName("Hammer");

        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Hammer");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolRepository.findByNameIgnoreCaseAndCategory("Hammer", testCategory))
                .thenReturn(Optional.of(anotherTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertTrue(exception.getMessage().contains("Ya existe otra herramienta"));
    }

    // ========== Tests for deleteTool ==========
    @Test
    void deleteTool_ShouldDeleteSuccessfully() {
        when(toolRepository.findById(1L)).thenReturn(Optional.of(testTool));
        doNothing().when(toolInstanceRepository).deleteByToolId(1L);
        doNothing().when(toolRepository).delete(testTool);

        toolService.deleteTool(1L);

        verify(toolInstanceRepository, times(1)).deleteByToolId(1L);
        verify(toolRepository, times(1)).delete(testTool);
    }

    @Test
    void deleteTool_ShouldThrowException_WhenToolNotFound() {
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.deleteTool(999L)
        );

        assertTrue(exception.getMessage().contains("Herramienta no encontrada"));
    }

    // ========== Tests for addToolStock ==========

    @Test
    void addToolStock_ShouldThrowException_WhenQuantityIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.addToolStock(1L, null)
        );

        assertEquals("La cantidad debe ser mayor a 0", exception.getMessage());
    }

    @Test
    void addToolStock_ShouldThrowException_WhenQuantityIsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.addToolStock(1L, 0)
        );

        assertEquals("La cantidad debe ser mayor a 0", exception.getMessage());
    }

    @Test
    void addToolStock_ShouldThrowException_WhenQuantityExceedsMax() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.addToolStock(1L, 1000)
        );

        assertTrue(exception.getMessage().contains("no puede exceder"));
    }

    @Test
    void addToolStock_ShouldThrowException_WhenToolNotFound() {
        when(toolRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.addToolStock(999L, 5)
        );

        assertTrue(exception.getMessage().contains("Herramienta no encontrada"));
    }

    @Test
    void addToolStock_ShouldThrowException_WhenTotalStockExceedsMax() {
        testTool.setInitialStock(995);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.addToolStock(1L, 10)
        );

        assertTrue(exception.getMessage().contains("El stock total no puede exceder"));
    }

    // ========== Tests for decommissionTool ==========
    @Test
    void decommissionTool_ShouldDecommissionSuccessfully() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity();
            instance.setId((long) i);
            instance.setTool(testTool);
            instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolInstanceRepository.findByToolAndStatus(testTool, ToolInstanceEntity.ToolInstanceStatus.AVAILABLE))
                .thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(testTool))
                .thenReturn(Optional.of(testTool));
        when(kardexMovementService.createDecommissionMovement(any(), anyInt(), anyString(), anyList(), any())).thenReturn(null);

        ToolEntity result = toolService.decommissionTool(1L, 3);

        assertNotNull(result);
        verify(toolInstanceRepository, times(3)).save(any(ToolInstanceEntity.class));
        verify(kardexMovementService, times(1)).createDecommissionMovement(any(), eq(3), anyString(), anyList(), any());
    }

    @Test
    void decommissionTool_ShouldThrowException_WhenQuantityIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.decommissionTool(1L, null)
        );

        assertEquals("La cantidad debe ser mayor a 0", exception.getMessage());
    }

    @Test
    void decommissionTool_ShouldThrowException_WhenQuantityIsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.decommissionTool(1L, 0)
        );

        assertEquals("La cantidad debe ser mayor a 0", exception.getMessage());
    }

    @Test
    void decommissionTool_ShouldThrowException_WhenToolNotFound() {
        when(toolRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.decommissionTool(999L, 3)
        );

        assertTrue(exception.getMessage().contains("Herramienta no encontrada"));
    }

    @Test
    void decommissionTool_ShouldThrowException_WhenNotEnoughAvailableInstances() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        ToolInstanceEntity instance = new ToolInstanceEntity();
        instance.setId(1L);
        instance.setTool(testTool);
        instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
        instances.add(instance);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolInstanceRepository.findByToolAndStatus(testTool, ToolInstanceEntity.ToolInstanceStatus.AVAILABLE))
                .thenReturn(instances);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.decommissionTool(1L, 5)
        );

        assertTrue(exception.getMessage().contains("No hay suficientes instancias disponibles"));
    }

    // ========== Tests for deleteToolInstanceAndUpdateStock ==========
    @Test
    void deleteToolInstanceAndUpdateStock_ShouldDeleteAvailableInstance() {
        ToolInstanceEntity instance = new ToolInstanceEntity();
        instance.setId(1L);
        instance.setTool(testTool);
        instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(toolInstanceRepository).delete(instance);

        toolService.deleteToolInstanceAndUpdateStock(1L);

        verify(toolRepository, times(1)).save(testTool);
        verify(toolInstanceRepository, times(1)).delete(instance);
        assertEquals(9, testTool.getCurrentStock());
        assertEquals(9, testTool.getInitialStock());
    }

    @Test
    void deleteToolInstanceAndUpdateStock_ShouldNotDecreaseStock_WhenInstanceNotAvailable() {
        ToolInstanceEntity instance = new ToolInstanceEntity();
        instance.setId(1L);
        instance.setTool(testTool);
        instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);

        when(toolInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));
        doNothing().when(toolInstanceRepository).delete(instance);

        toolService.deleteToolInstanceAndUpdateStock(1L);

        verify(toolRepository, never()).save(testTool);
        verify(toolInstanceRepository, times(1)).delete(instance);
        assertEquals(10, testTool.getCurrentStock());
    }

    @Test
    void deleteToolInstanceAndUpdateStock_ShouldThrowException_WhenInstanceNotFound() {
        when(toolInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.deleteToolInstanceAndUpdateStock(999L)
        );

        assertTrue(exception.getMessage().contains("Instancia de herramienta no encontrada"));
    }

    // ========== Tests for searchToolsByName ==========
    @Test
    void searchToolsByName_ShouldReturnMatchingTools() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(toolRepository.findByNameContainingIgnoreCase("Drill")).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.searchToolsByName("Drill");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).findByNameContainingIgnoreCase("Drill");
    }

    @Test
    void searchToolsByName_ShouldTrimSearchTerm() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(toolRepository.findByNameContainingIgnoreCase("Drill")).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.searchToolsByName("  Drill  ");

        assertNotNull(result);
        verify(toolRepository, times(1)).findByNameContainingIgnoreCase("Drill");
    }

    @Test
    void searchToolsByName_ShouldThrowException_WhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.searchToolsByName(null)
        );

        assertEquals("El término de búsqueda no puede estar vacío", exception.getMessage());
    }

    @Test
    void searchToolsByName_ShouldThrowException_WhenNameIsEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.searchToolsByName("  ")
        );

        assertEquals("El término de búsqueda no puede estar vacío", exception.getMessage());
    }

    // ========== Tests for getToolsByCategory ==========
    @Test
    void getToolsByCategory_ShouldReturnToolsInCategory() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(toolRepository.findByCategory(testCategory)).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.getToolsByCategory(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).findByCategory(testCategory);
    }

    @Test
    void getToolsByCategory_ShouldThrowException_WhenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.getToolsByCategory(999L)
        );

        assertEquals("Categoría no encontrada", exception.getMessage());
    }

    // ========== Tests for getAvailableTools ==========
    @Test
    void getAvailableTools_ShouldReturnAvailableTools() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(toolRepository.findAvailableTools()).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.getAvailableTools();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).findAvailableTools();
    }

    // ========== Tests for getLowStockTools ==========
    @Test
    void getLowStockTools_ShouldReturnLowStockTools() {
        List<ToolEntity> expectedTools = Arrays.asList(testTool);
        when(toolRepository.findLowStockTools(5)).thenReturn(expectedTools);

        List<ToolEntity> result = toolService.getLowStockTools(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(toolRepository, times(1)).findLowStockTools(5);
    }

    @Test
    void getLowStockTools_ShouldThrowException_WhenThresholdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.getLowStockTools(null)
        );

        assertEquals("El umbral debe ser un número positivo", exception.getMessage());
    }

    @Test
    void getLowStockTools_ShouldThrowException_WhenThresholdIsNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.getLowStockTools(-1)
        );

        assertEquals("El umbral debe ser un número positivo", exception.getMessage());
    }

    // ========== Additional edge case tests ==========
    @Test
    void createTool_ShouldTrimName() {
        ToolEntity newTool = new ToolEntity();
        newTool.setName("  Hammer  ");
        newTool.setCategory(testCategory);
        newTool.setInitialStock(5);
        newTool.setReplacementValue(new BigDecimal("2000"));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(toolRepository.findByNameIgnoreCaseAndCategory("Hammer", testCategory)).thenReturn(Optional.empty());
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> {
            ToolEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            assertEquals("Hammer", saved.getName());
            return saved;
        });
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.findByIdWithCategory(any())).thenReturn(Optional.of(newTool));
        when(kardexMovementService.createInitialStockMovement(any(), anyInt())).thenReturn(null);

        toolService.createTool(newTool);

        verify(toolRepository).save(argThat(tool -> "Hammer".equals(tool.getName())));
    }

    @Test
    void updateTool_ShouldAllowSameNameForSameTool() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolRepository.findByNameIgnoreCaseAndCategory("Drill", testCategory))
                .thenReturn(Optional.of(testTool));
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        ToolEntity result = toolService.updateTool(1L, updatedDetails);

        assertNotNull(result);
        verify(toolRepository, times(1)).save(any(ToolEntity.class));
    }

    @Test
    void addToolStock_ShouldHandleKardexException() {
        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(testTool))
                .thenReturn(Optional.of(testTool));
        when(kardexMovementService.createRestockMovement(any(), anyInt(), anyString(), any()))
                .thenThrow(new RuntimeException("Kardex error"));

        // Should not throw exception even if Kardex fails
        assertDoesNotThrow(() -> toolService.addToolStock(1L, 5));

        verify(toolInstanceRepository, times(5)).save(any(ToolInstanceEntity.class));
    }

    @Test
    void updateTool_ShouldNotUpdateCategory_WhenCategoryIsNull() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(new BigDecimal("6000"));
        updatedDetails.setRentalRate(new BigDecimal("150"));
        updatedDetails.setCategory(null);

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolRepository.findByNameIgnoreCaseAndCategory(anyString(), any())).thenReturn(Optional.empty());
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(testTool);

        toolService.updateTool(1L, updatedDetails);

        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void updateTool_ShouldValidateReplacementValueBeforeRentalRate() {
        ToolEntity updatedDetails = new ToolEntity();
        updatedDetails.setName("Updated Drill");
        updatedDetails.setReplacementValue(null);
        updatedDetails.setRentalRate(new BigDecimal("150"));

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> toolService.updateTool(1L, updatedDetails)
        );

        assertEquals("El valor de reposición es requerido", exception.getMessage());
    }

    @Test
    void decommissionTool_ShouldCollectCorrectInstanceIds() {
        List<ToolInstanceEntity> instances = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ToolInstanceEntity instance = new ToolInstanceEntity();
            instance.setId((long) i);
            instance.setTool(testTool);
            instance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
            instances.add(instance);
        }

        when(toolRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(testTool));
        when(toolInstanceRepository.findByToolAndStatus(testTool, ToolInstanceEntity.ToolInstanceStatus.AVAILABLE))
                .thenReturn(instances);
        when(toolInstanceRepository.save(any(ToolInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(testTool))
                .thenReturn(Optional.of(testTool));

        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        when(kardexMovementService.createDecommissionMovement(any(), anyInt(), anyString(), idsCaptor.capture(), any())).thenReturn(null);

        toolService.decommissionTool(1L, 3);

        List<Long> capturedIds = idsCaptor.getValue();
        assertEquals(3, capturedIds.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), capturedIds);
    }
}