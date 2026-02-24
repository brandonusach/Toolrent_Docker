package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.*;
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
class ToolInstanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ToolInstanceRepository toolInstanceRepository;

    private ToolEntity tool;
    private ToolInstanceEntity instance1;
    private ToolInstanceEntity instance2;
    private ToolInstanceEntity instance3;

    @BeforeEach
    void setUp() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Power Tools");
        category.setDescription("Electric power tools");
        entityManager.persist(category);

        tool = new ToolEntity();
        tool.setName("Drill");
        tool.setCategory(category);
        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool.setInitialStock(10);
        tool.setCurrentStock(3);
        tool.setReplacementValue(BigDecimal.valueOf(100.0));
        tool.setRentalRate(BigDecimal.valueOf(10.0));
        entityManager.persist(tool);

        instance1 = new ToolInstanceEntity();
        instance1.setTool(tool);
        instance1.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        instance2 = new ToolInstanceEntity();
        instance2.setTool(tool);
        instance2.setStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);

        instance3 = new ToolInstanceEntity();
        instance3.setTool(tool);
        instance3.setStatus(ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);

        entityManager.persist(instance1);
        entityManager.persist(instance2);
        entityManager.persist(instance3);
        entityManager.flush();
    }

    @Test
    void testFindByToolId_ShouldReturnAllInstancesOfTool() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findByToolId(tool.getId());

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByStatus_ShouldReturnInstancesWithStatus() {
        // When
        List<ToolInstanceEntity> available = toolInstanceRepository.findByStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
        List<ToolInstanceEntity> loaned = toolInstanceRepository.findByStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);

        // Then
        assertThat(available).hasSize(1);
        assertThat(loaned).hasSize(1);
        assertThat(available.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
        assertThat(loaned.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.LOANED);
    }

    @Test
    void testFindByToolIdAndStatus_ShouldReturnMatchingInstances() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findByToolIdAndStatus(
                tool.getId(),
                ToolInstanceEntity.ToolInstanceStatus.AVAILABLE
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
    }

    @Test
    void testFindByToolAndStatus_ShouldReturnMatchingInstances() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findByToolAndStatus(
                tool,
                ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);
    }

    @Test
    void testCountByToolIdAndStatus_ShouldReturnCorrectCount() {
        // When
        Long count = toolInstanceRepository.countByToolIdAndStatus(
                tool.getId(),
                ToolInstanceEntity.ToolInstanceStatus.AVAILABLE
        );

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountAvailableByToolId_ShouldReturnCorrectCount() {
        // When
        Long count = toolInstanceRepository.countAvailableByToolId(tool.getId());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindFirstAvailableByToolId_WhenAvailableExists_ShouldReturnInstance() {
        // When
        Optional<ToolInstanceEntity> result = toolInstanceRepository.findFirstAvailableByToolId(tool.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
    }

    @Test
    void testFindFirstAvailableByToolId_WhenNoAvailableExists_ShouldReturnEmpty() {
        // Given - mark all as loaned
        instance1.setStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);
        entityManager.persist(instance1);
        entityManager.flush();

        // When
        Optional<ToolInstanceEntity> result = toolInstanceRepository.findFirstAvailableByToolId(tool.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByStatusOrderByToolIdAsc_ShouldReturnOrderedInstances() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findByStatusOrderByToolIdAsc(
                ToolInstanceEntity.ToolInstanceStatus.AVAILABLE
        );

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);
    }

    @Test
    void testFindInstancesUnderRepair_ShouldReturnRepairingInstances() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findInstancesUnderRepair();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);
    }

    @Test
    void testCountByToolId_ShouldReturnTotalCount() {
        // When
        Long count = toolInstanceRepository.countByToolId(tool.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testFindLoanedInstances_ShouldReturnLoanedInstances() {
        // When
        List<ToolInstanceEntity> results = toolInstanceRepository.findLoanedInstances();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(ToolInstanceEntity.ToolInstanceStatus.LOANED);
    }

    @Test
    void testSaveToolInstance_ShouldPersistInstance() {
        // Given
        ToolInstanceEntity newInstance = new ToolInstanceEntity();
        newInstance.setTool(tool);
        newInstance.setStatus(ToolInstanceEntity.ToolInstanceStatus.AVAILABLE);

        // When
        ToolInstanceEntity saved = toolInstanceRepository.save(newInstance);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(toolInstanceRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testDeleteToolInstance_ShouldRemoveInstance() {
        // Given
        Long instanceId = instance1.getId();

        // When
        toolInstanceRepository.delete(instance1);

        // Then
        assertThat(toolInstanceRepository.findById(instanceId)).isEmpty();
    }
}

