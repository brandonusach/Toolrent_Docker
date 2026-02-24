package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.CategoryEntity;
import com.toolrent.backend.entities.ToolEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ToolRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ToolRepository toolRepository;

    private CategoryEntity category;
    private ToolEntity tool1;
    private ToolEntity tool2;
    private ToolEntity tool3;

    @BeforeEach
    void setUp() {
        category = new CategoryEntity();
        category.setName("Power Tools");
        category.setDescription("Electric power tools");
        entityManager.persist(category);

        tool1 = new ToolEntity();
        tool1.setName("Drill");
        tool1.setCategory(category);
        tool1.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool1.setInitialStock(10);
        tool1.setCurrentStock(5);
        tool1.setReplacementValue(BigDecimal.valueOf(100.0));
        tool1.setRentalRate(BigDecimal.valueOf(10.0));

        tool2 = new ToolEntity();
        tool2.setName("Hammer");
        tool2.setCategory(category);
        tool2.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool2.setInitialStock(5);
        tool2.setCurrentStock(1);
        tool2.setReplacementValue(BigDecimal.valueOf(50.0));
        tool2.setRentalRate(BigDecimal.valueOf(5.0));

        tool3 = new ToolEntity();
        tool3.setName("Saw");
        tool3.setCategory(category);
        tool3.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool3.setInitialStock(2);
        tool3.setCurrentStock(0);
        tool3.setReplacementValue(BigDecimal.valueOf(80.0));
        tool3.setRentalRate(BigDecimal.valueOf(8.0));

        entityManager.persist(tool1);
        entityManager.persist(tool2);
        entityManager.persist(tool3);
        entityManager.flush();
    }

    @Test
    void testFindByIdWithCategory_WhenToolExists_ShouldReturnToolWithCategory() {
        // When
        Optional<ToolEntity> result = toolRepository.findByIdWithCategory(tool1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Drill");
        assertThat(result.get().getCategory()).isNotNull();
        assertThat(result.get().getCategory().getName()).isEqualTo("Power Tools");
    }

    @Test
    void testFindByIdWithCategory_WhenToolDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<ToolEntity> result = toolRepository.findByIdWithCategory(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindAllWithCategories_ShouldReturnAllToolsWithCategories() {
        // When
        List<ToolEntity> results = toolRepository.findAllWithCategories();

        // Then
        assertThat(results).hasSize(3);
        results.forEach(tool -> assertThat(tool.getCategory()).isNotNull());
    }

    @Test
    void testFindByNameContainingIgnoreCase_ShouldReturnMatchingTools() {
        // When
        List<ToolEntity> results = toolRepository.findByNameContainingIgnoreCase("drill");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Drill");
    }

    @Test
    void testFindByCategory_ShouldReturnToolsInCategory() {
        // When
        List<ToolEntity> results = toolRepository.findByCategory(category);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByCategoryId_ShouldReturnToolsInCategory() {
        // When
        List<ToolEntity> results = toolRepository.findByCategoryId(category.getId());

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByStatus_ShouldReturnToolsWithStatus() {
        // Given - change tool3 to DECOMMISSIONED status for testing
        tool3.setStatus(ToolEntity.ToolStatus.DECOMMISSIONED);
        entityManager.persist(tool3);
        entityManager.flush();

        // When
        List<ToolEntity> availableTools = toolRepository.findByStatus(ToolEntity.ToolStatus.AVAILABLE);
        List<ToolEntity> decommissionedTools = toolRepository.findByStatus(ToolEntity.ToolStatus.DECOMMISSIONED);

        // Then
        assertThat(availableTools).hasSize(2);
        assertThat(decommissionedTools).hasSize(1);
        assertThat(decommissionedTools.get(0).getName()).isEqualTo("Saw");
    }

    @Test
    void testFindAvailableTools_ShouldReturnToolsAvailableAndInStock() {
        // When
        List<ToolEntity> results = toolRepository.findAvailableTools();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ToolEntity::getName)
                .containsExactlyInAnyOrder("Drill", "Hammer");
    }

    @Test
    void testFindLowStockTools_ShouldReturnToolsBelowThreshold() {
        // When
        List<ToolEntity> results = toolRepository.findLowStockTools(2);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ToolEntity::getName)
                .containsExactlyInAnyOrder("Hammer", "Saw");
    }

    @Test
    void testExistsByNameIgnoreCase_WhenNameExists_ShouldReturnTrue() {
        // When
        boolean exists = toolRepository.existsByNameIgnoreCase("DRILL");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByNameIgnoreCase_WhenNameDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = toolRepository.existsByNameIgnoreCase("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByCategoryAndStatus_ShouldReturnMatchingTools() {
        // When
        List<ToolEntity> results = toolRepository.findByCategoryAndStatus(category, ToolEntity.ToolStatus.AVAILABLE);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(ToolEntity::getName)
                .containsExactlyInAnyOrder("Drill", "Hammer", "Saw");
    }

    @Test
    void testCountByStatus_ShouldReturnCorrectCount() {
        // Given - change tool3 to DECOMMISSIONED status for testing
        tool3.setStatus(ToolEntity.ToolStatus.DECOMMISSIONED);
        entityManager.persist(tool3);
        entityManager.flush();

        // When
        long availableCount = toolRepository.countByStatus(ToolEntity.ToolStatus.AVAILABLE);
        long decommissionedCount = toolRepository.countByStatus(ToolEntity.ToolStatus.DECOMMISSIONED);

        // Then
        assertThat(availableCount).isEqualTo(2);
        assertThat(decommissionedCount).isEqualTo(1);
    }

    @Test
    void testSaveTool_ShouldPersistTool() {
        // Given
        ToolEntity newTool = new ToolEntity();
        newTool.setName("Screwdriver");
        newTool.setCategory(category);
        newTool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        newTool.setInitialStock(10);
        newTool.setCurrentStock(10);
        newTool.setReplacementValue(BigDecimal.valueOf(30.0));
        newTool.setRentalRate(BigDecimal.valueOf(3.0));

        // When
        ToolEntity saved = toolRepository.save(newTool);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(toolRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testDeleteTool_ShouldRemoveTool() {
        // Given
        Long toolId = tool1.getId();

        // When
        toolRepository.delete(tool1);

        // Then
        assertThat(toolRepository.findById(toolId)).isEmpty();
    }
}

