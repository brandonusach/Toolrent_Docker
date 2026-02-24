package com.toolrent.backend.services;

import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new CategoryEntity();
        testCategory.setId(1L);
        testCategory.setName("Herramientas de Corte");
        testCategory.setDescription("Sierras, cortadoras, amoladoras, etc.");
    }

    // ========== Tests for getAllCategories ==========
    @Test
    void getAllCategories_ShouldReturnAllCategories() {
        CategoryEntity category2 = new CategoryEntity();
        category2.setId(2L);
        category2.setName("Herramientas de Perforación");
        category2.setDescription("Taladros, rotomartillos, brocas, etc.");

        List<CategoryEntity> expectedCategories = Arrays.asList(testCategory, category2);
        when(categoryRepository.findAllOrderByName()).thenReturn(expectedCategories);

        List<CategoryEntity> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(categoryRepository, times(1)).findAllOrderByName();
    }

    @Test
    void getAllCategories_ShouldReturnEmptyList_WhenNoCategories() {
        when(categoryRepository.findAllOrderByName()).thenReturn(List.of());

        List<CategoryEntity> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for getCategoryById ==========
    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        Optional<CategoryEntity> result = categoryService.getCategoryById(1L);

        assertTrue(result.isPresent());
        assertEquals("Herramientas de Corte", result.get().getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    void getCategoryById_ShouldReturnEmpty_WhenNotExists() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<CategoryEntity> result = categoryService.getCategoryById(999L);

        assertFalse(result.isPresent());
    }

    // ========== Tests for createCategory ==========
    @Test
    void createCategory_ShouldCreateSuccessfully_WithValidData() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("Nueva Categoría");
        newCategory.setDescription("Descripción de prueba");

        when(categoryRepository.existsByNameIgnoreCase("Nueva Categoría")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> {
            CategoryEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoryEntity result = categoryService.createCategory(newCategory);

        assertNotNull(result);
        assertEquals("Nueva Categoría", result.getName());
        verify(categoryRepository, times(1)).save(any(CategoryEntity.class));
    }

    @Test
    void createCategory_ShouldThrowException_WhenNameIsNull() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName(null);
        newCategory.setDescription("Descripción");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(newCategory)
        );

        assertTrue(exception.getMessage().contains("Category name is required"));
        verify(categoryRepository, never()).save(any(CategoryEntity.class));
    }

    @Test
    void createCategory_ShouldThrowException_WhenNameIsEmpty() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("   ");
        newCategory.setDescription("Descripción");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(newCategory)
        );

        assertTrue(exception.getMessage().contains("Category name is required"));
        verify(categoryRepository, never()).save(any(CategoryEntity.class));
    }

    @Test
    void createCategory_ShouldThrowException_WhenNameAlreadyExists() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("Herramientas de Corte");
        newCategory.setDescription("Descripción");

        when(categoryRepository.existsByNameIgnoreCase("Herramientas de Corte")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(newCategory)
        );

        assertTrue(exception.getMessage().contains("Category name already exists"));
        verify(categoryRepository, never()).save(any(CategoryEntity.class));
    }

    @Test
    void createCategory_ShouldBeCaseInsensitive_WhenCheckingDuplicates() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("herramientas de corte");
        newCategory.setDescription("Descripción");

        when(categoryRepository.existsByNameIgnoreCase("herramientas de corte")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.createCategory(newCategory)
        );

        assertTrue(exception.getMessage().contains("Category name already exists"));
    }

    // ========== Tests for updateCategory ==========
    @Test
    void updateCategory_ShouldUpdateSuccessfully_WithValidData() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Herramientas de Corte Actualizadas");
        updateDetails.setDescription("Nueva descripción");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCase("Herramientas de Corte Actualizadas")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(testCategory);

        CategoryEntity result = categoryService.updateCategory(1L, updateDetails);

        assertNotNull(result);
        assertEquals("Herramientas de Corte Actualizadas", testCategory.getName());
        assertEquals("Nueva descripción", testCategory.getDescription());
        verify(categoryRepository, times(1)).save(testCategory);
    }

    @Test
    void updateCategory_ShouldAllowSameName_WhenUpdatingSameCategory() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Herramientas de Corte");
        updateDetails.setDescription("Nueva descripción");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(testCategory);

        CategoryEntity result = categoryService.updateCategory(1L, updateDetails);

        assertNotNull(result);
        assertEquals("Nueva descripción", testCategory.getDescription());
        verify(categoryRepository, times(1)).save(testCategory);
    }

    @Test
    void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Nueva Categoría");
        updateDetails.setDescription("Nueva descripción");

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.updateCategory(999L, updateDetails)
        );

        assertTrue(exception.getMessage().contains("Category not found"));
    }

    @Test
    void updateCategory_ShouldThrowException_WhenNewNameAlreadyExists() {
        CategoryEntity existingCategory = new CategoryEntity();
        existingCategory.setId(2L);
        existingCategory.setName("Herramientas de Perforación");

        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Herramientas de Perforación");
        updateDetails.setDescription("Nueva descripción");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCase("Herramientas de Perforación")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.updateCategory(1L, updateDetails)
        );

        assertTrue(exception.getMessage().contains("Category name already exists"));
        verify(categoryRepository, never()).save(any(CategoryEntity.class));
    }

    @Test
    void updateCategory_ShouldBeCaseInsensitive_WhenCheckingDuplicates() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("HERRAMIENTAS DE PERFORACIÓN");
        updateDetails.setDescription("Nueva descripción");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCase("HERRAMIENTAS DE PERFORACIÓN")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.updateCategory(1L, updateDetails)
        );

        assertTrue(exception.getMessage().contains("Category name already exists"));
    }

    // ========== Tests for deleteCategory ==========
    @Test
    void deleteCategory_ShouldDeleteSuccessfully_WhenCategoryExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        doNothing().when(categoryRepository).delete(testCategory);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    void deleteCategory_ShouldThrowException_WhenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> categoryService.deleteCategory(999L)
        );

        assertTrue(exception.getMessage().contains("Category not found"));
        verify(categoryRepository, never()).delete(any(CategoryEntity.class));
    }

    // ========== Tests for searchCategoriesByName ==========
    @Test
    void searchCategoriesByName_ShouldReturnMatchingCategories() {
        CategoryEntity category2 = new CategoryEntity();
        category2.setId(2L);
        category2.setName("Herramientas de Corte Avanzadas");

        List<CategoryEntity> expectedCategories = Arrays.asList(testCategory, category2);
        when(categoryRepository.findByNameContainingIgnoreCase("Corte")).thenReturn(expectedCategories);

        List<CategoryEntity> result = categoryService.searchCategoriesByName("Corte");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(categoryRepository, times(1)).findByNameContainingIgnoreCase("Corte");
    }

    @Test
    void searchCategoriesByName_ShouldReturnEmptyList_WhenNoMatches() {
        when(categoryRepository.findByNameContainingIgnoreCase("NoExiste")).thenReturn(List.of());

        List<CategoryEntity> result = categoryService.searchCategoriesByName("NoExiste");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void searchCategoriesByName_ShouldBeCaseInsensitive() {
        List<CategoryEntity> expectedCategories = Arrays.asList(testCategory);
        when(categoryRepository.findByNameContainingIgnoreCase("CORTE")).thenReturn(expectedCategories);

        List<CategoryEntity> result = categoryService.searchCategoriesByName("CORTE");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== Tests for getCategoryByName ==========
    @Test
    void getCategoryByName_ShouldReturnCategory_WhenExists() {
        when(categoryRepository.findByNameIgnoreCase("Herramientas de Corte"))
                .thenReturn(Optional.of(testCategory));

        Optional<CategoryEntity> result = categoryService.getCategoryByName("Herramientas de Corte");

        assertTrue(result.isPresent());
        assertEquals("Herramientas de Corte", result.get().getName());
        verify(categoryRepository, times(1)).findByNameIgnoreCase("Herramientas de Corte");
    }

    @Test
    void getCategoryByName_ShouldReturnEmpty_WhenNotExists() {
        when(categoryRepository.findByNameIgnoreCase("No Existe")).thenReturn(Optional.empty());

        Optional<CategoryEntity> result = categoryService.getCategoryByName("No Existe");

        assertFalse(result.isPresent());
    }

    @Test
    void getCategoryByName_ShouldBeCaseInsensitive() {
        when(categoryRepository.findByNameIgnoreCase("HERRAMIENTAS DE CORTE"))
                .thenReturn(Optional.of(testCategory));

        Optional<CategoryEntity> result = categoryService.getCategoryByName("HERRAMIENTAS DE CORTE");

        assertTrue(result.isPresent());
    }

    // ========== Tests for categoryNameExists ==========
    @Test
    void categoryNameExists_ShouldReturnTrue_WhenNameExists() {
        when(categoryRepository.existsByNameIgnoreCase("Herramientas de Corte")).thenReturn(true);

        boolean result = categoryService.categoryNameExists("Herramientas de Corte");

        assertTrue(result);
        verify(categoryRepository, times(1)).existsByNameIgnoreCase("Herramientas de Corte");
    }

    @Test
    void categoryNameExists_ShouldReturnFalse_WhenNameDoesNotExist() {
        when(categoryRepository.existsByNameIgnoreCase("No Existe")).thenReturn(false);

        boolean result = categoryService.categoryNameExists("No Existe");

        assertFalse(result);
    }

    @Test
    void categoryNameExists_ShouldBeCaseInsensitive() {
        when(categoryRepository.existsByNameIgnoreCase("herramientas de corte")).thenReturn(true);

        boolean result = categoryService.categoryNameExists("herramientas de corte");

        assertTrue(result);
    }

    // ========== Tests for getCategoryStats ==========
    @Test
    void getCategoryStats_ShouldReturnCorrectStats() {
        when(categoryRepository.count()).thenReturn(15L);

        CategoryService.CategoryStats result = categoryService.getCategoryStats();

        assertNotNull(result);
        assertEquals(15L, result.getTotalCategories());
        verify(categoryRepository, times(1)).count();
    }

    @Test
    void getCategoryStats_ShouldReturnZero_WhenNoCategories() {
        when(categoryRepository.count()).thenReturn(0L);

        CategoryService.CategoryStats result = categoryService.getCategoryStats();

        assertNotNull(result);
        assertEquals(0L, result.getTotalCategories());
    }

    // ========== Tests for run (CommandLineRunner) ==========
    @Test
    void run_ShouldInitializeDefaultCategories() throws Exception {
        // Mock para simular que ninguna categoría existe
        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.count()).thenReturn(15L);

        categoryService.run();

        // Verificar que se intentó guardar 15 categorías por defecto
        verify(categoryRepository, times(15)).save(any(CategoryEntity.class));
        verify(categoryRepository, times(1)).count();
    }

    @Test
    void run_ShouldNotCreateDuplicates_WhenCategoriesAlreadyExist() throws Exception {
        // Mock para simular que todas las categorías ya existen
        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);
        when(categoryRepository.count()).thenReturn(15L);

        categoryService.run();

        // Verificar que no se guardó ninguna categoría
        verify(categoryRepository, never()).save(any(CategoryEntity.class));
        verify(categoryRepository, times(1)).count();
    }

    @Test
    void run_ShouldHandlePartialExistingCategories() throws Exception {
        // Mock para simular que algunas categorías existen y otras no
        when(categoryRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(true, false, false, true, false, true, false, false, true, false, true, false, true, false, true);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.count()).thenReturn(15L);

        categoryService.run();

        // Verificar que se guardaron solo las categorías que no existían (8 en este caso)
        verify(categoryRepository, times(8)).save(any(CategoryEntity.class));
    }

    @Test
    void run_ShouldHandleExceptionDuringSave() throws Exception {
        // Mock para simular que ninguna categoría existe pero falla al guardar una
        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.count()).thenReturn(14L);

        // No debería lanzar excepción, solo registrar el error
        assertDoesNotThrow(() -> categoryService.run());

        // Verificar que se intentaron guardar las 15 categorías
        verify(categoryRepository, times(15)).save(any(CategoryEntity.class));
    }

    // ========== Tests for reinitializeDefaultCategories ==========
    @Test
    void reinitializeDefaultCategories_ShouldCallInitializeDefaultCategories() {
        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);
        when(categoryRepository.count()).thenReturn(15L);

        categoryService.reinitializeDefaultCategories();

        // Verificar que se llamó al método de conteo (parte de la inicialización)
        verify(categoryRepository, times(1)).count();
    }

    @Test
    void reinitializeDefaultCategories_ShouldCreateMissingCategories() {
        // Simular que faltan algunas categorías
        when(categoryRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false, false, true, true, true, true, true, true, true, true, true, true, true, true, true);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.count()).thenReturn(15L);

        categoryService.reinitializeDefaultCategories();

        // Verificar que se crearon solo las 2 categorías faltantes
        verify(categoryRepository, times(2)).save(any(CategoryEntity.class));
    }

    // ========== Edge case tests ==========
    @Test
    void createCategory_ShouldTrimWhitespace_BeforeValidation() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("   Nueva Categoría   ");
        newCategory.setDescription("Descripción");

        when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> categoryService.createCategory(newCategory));
        verify(categoryRepository, times(1)).save(any(CategoryEntity.class));
    }

    @Test
    void updateCategory_ShouldUpdateOnlyName_WhenDescriptionNotChanged() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Nuevo Nombre");
        updateDetails.setDescription("Sierras, cortadoras, amoladoras, etc.");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCase("Nuevo Nombre")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(testCategory);

        categoryService.updateCategory(1L, updateDetails);

        assertEquals("Nuevo Nombre", testCategory.getName());
        assertEquals("Sierras, cortadoras, amoladoras, etc.", testCategory.getDescription());
    }

    @Test
    void searchCategoriesByName_ShouldHandleSpecialCharacters() {
        when(categoryRepository.findByNameContainingIgnoreCase("Corte/Perforación"))
                .thenReturn(List.of());

        List<CategoryEntity> result = categoryService.searchCategoriesByName("Corte/Perforación");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllCategories_ShouldReturnOrderedList() {
        CategoryEntity category1 = new CategoryEntity(1L, "A Categoría", "Desc 1");
        CategoryEntity category2 = new CategoryEntity(2L, "B Categoría", "Desc 2");
        CategoryEntity category3 = new CategoryEntity(3L, "C Categoría", "Desc 3");

        List<CategoryEntity> expectedCategories = Arrays.asList(category1, category2, category3);
        when(categoryRepository.findAllOrderByName()).thenReturn(expectedCategories);

        List<CategoryEntity> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("A Categoría", result.get(0).getName());
        assertEquals("B Categoría", result.get(1).getName());
        assertEquals("C Categoría", result.get(2).getName());
    }

    @Test
    void createCategory_ShouldAllowNullDescription() {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("Categoría Sin Descripción");
        newCategory.setDescription(null);

        when(categoryRepository.existsByNameIgnoreCase("Categoría Sin Descripción")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> categoryService.createCategory(newCategory));
        verify(categoryRepository, times(1)).save(any(CategoryEntity.class));
    }

    @Test
    void updateCategory_ShouldAllowNullDescription() {
        CategoryEntity updateDetails = new CategoryEntity();
        updateDetails.setName("Herramientas de Corte");
        updateDetails.setDescription(null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(testCategory);

        assertDoesNotThrow(() -> categoryService.updateCategory(1L, updateDetails));
        assertNull(testCategory.getDescription());
    }
}