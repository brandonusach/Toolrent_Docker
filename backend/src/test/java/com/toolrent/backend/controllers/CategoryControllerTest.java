package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        objectMapper = new ObjectMapper();

        testCategory = new CategoryEntity();
        testCategory.setId(1L);
        testCategory.setName("Herramientas de Corte");
        testCategory.setDescription("Sierras, cortadoras, amoladoras");
    }

    // ========== Tests for GET /api/v1/categories/ ==========
    @Test
    void listCategories_ShouldReturnAllCategories() throws Exception {
        CategoryEntity category2 = new CategoryEntity();
        category2.setId(2L);
        category2.setName("Herramientas de Perforación");
        category2.setDescription("Taladros, rotomartillos");

        List<CategoryEntity> categories = Arrays.asList(testCategory, category2);
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/v1/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Herramientas de Corte"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void listCategories_ShouldReturnEmptyList_WhenNoCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService, times(1)).getAllCategories();
    }

    // ========== Tests for GET /api/v1/categories/{id} ==========
    @Test
    void getCategoryById_ShouldReturnCategory_WhenExists() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Herramientas de Corte"))
                .andExpect(jsonPath("$.description").value("Sierras, cortadoras, amoladoras"));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    void getCategoryById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(categoryService.getCategoryById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/categories/999"))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategoryById(999L);
    }

    // ========== Tests for POST /api/v1/categories/ ==========
    @Test
    void saveCategory_ShouldCreateAndReturnCategory() throws Exception {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("Nueva Categoría");
        newCategory.setDescription("Descripción nueva");

        CategoryEntity savedCategory = new CategoryEntity();
        savedCategory.setId(3L);
        savedCategory.setName("Nueva Categoría");
        savedCategory.setDescription("Descripción nueva");

        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(savedCategory);

        mockMvc.perform(post("/api/v1/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Nueva Categoría"))
                .andExpect(jsonPath("$.description").value("Descripción nueva"));

        verify(categoryService, times(1)).createCategory(any(CategoryEntity.class));
    }

    // ========== Tests for PUT /api/v1/categories/ ==========
    @Test
    void updateCategory_ShouldUpdateAndReturnCategory() throws Exception {
        CategoryEntity updatedCategory = new CategoryEntity();
        updatedCategory.setId(1L);
        updatedCategory.setName("Categoría Actualizada");
        updatedCategory.setDescription("Descripción actualizada");

        when(categoryService.updateCategory(eq(1L), any(CategoryEntity.class)))
                .thenReturn(updatedCategory);

        mockMvc.perform(put("/api/v1/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Categoría Actualizada"))
                .andExpect(jsonPath("$.description").value("Descripción actualizada"));

        verify(categoryService, times(1)).updateCategory(eq(1L), any(CategoryEntity.class));
    }

    // ========== Tests for DELETE /api/v1/categories/{id} ==========
    @Test
    void deleteCategoryById_ShouldReturnNoContent_WhenDeleted() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    void deleteCategoryById_ShouldHandleException_WhenDeletionFails() throws Exception {
        doThrow(new RuntimeException("No se puede eliminar")).when(categoryService).deleteCategory(999L);

        try {
            mockMvc.perform(delete("/api/v1/categories/999"))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // El controlador puede lanzar la excepción directamente
        }

        verify(categoryService, times(1)).deleteCategory(999L);
    }

    // ========== Tests adicionales para aumentar cobertura ==========
    @Test
    void saveCategory_ShouldHandleNullName() throws Exception {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setDescription("Solo descripción");

        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(newCategory);

        mockMvc.perform(post("/api/v1/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).createCategory(any(CategoryEntity.class));
    }

    @Test
    void updateCategory_ShouldHandleNullReturn() throws Exception {
        CategoryEntity updatedCategory = new CategoryEntity();
        updatedCategory.setId(999L);
        updatedCategory.setName("No existe");

        when(categoryService.updateCategory(eq(999L), any(CategoryEntity.class)))
                .thenReturn(null);

        mockMvc.perform(put("/api/v1/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).updateCategory(eq(999L), any(CategoryEntity.class));
    }

    @Test
    void getCategoryById_ShouldHandleInvalidId() throws Exception {
        when(categoryService.getCategoryById(0L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/categories/0"))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategoryById(0L);
    }

    @Test
    void listCategories_ShouldHandleServiceException() throws Exception {
        when(categoryService.getAllCategories()).thenThrow(new RuntimeException("Database error"));

        try {
            mockMvc.perform(get("/api/v1/categories/"))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Esperado
        }

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void saveCategory_ShouldAcceptEmptyDescription() throws Exception {
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setName("Solo nombre");
        newCategory.setDescription("");

        CategoryEntity savedCategory = new CategoryEntity();
        savedCategory.setId(5L);
        savedCategory.setName("Solo nombre");
        savedCategory.setDescription("");

        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(savedCategory);

        mockMvc.perform(post("/api/v1/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Solo nombre"));

        verify(categoryService, times(1)).createCategory(any(CategoryEntity.class));
    }
}

