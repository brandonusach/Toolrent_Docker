package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class KardexMovementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private KardexMovementRepository kardexMovementRepository;

    private ToolEntity tool1;
    private ToolEntity tool2;
    private LoanEntity loan;
    private KardexMovementEntity initialStock;
    private KardexMovementEntity loanMovement;
    private KardexMovementEntity returnMovement;
    private KardexMovementEntity restockMovement;

    @BeforeEach
    void setUp() {
        // Create client
        ClientEntity client = new ClientEntity();
        client.setRut("12345678-9");
        client.setName("Juan Pérez");
        client.setEmail("juan@example.com");
        client.setPhone("912345678");
        client.setStatus(ClientEntity.ClientStatus.ACTIVE);
        entityManager.persist(client);

        // Create category
        CategoryEntity category = new CategoryEntity();
        category.setName("Power Tools");
        category.setDescription("Electric power tools");
        entityManager.persist(category);

        // Create tools
        tool1 = new ToolEntity();
        tool1.setName("Drill");
        tool1.setCategory(category);
        tool1.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool1.setInitialStock(10);
        tool1.setCurrentStock(5);
        tool1.setReplacementValue(BigDecimal.valueOf(100.0));
        tool1.setRentalRate(BigDecimal.valueOf(10.0));
        entityManager.persist(tool1);

        tool2 = new ToolEntity();
        tool2.setName("Hammer");
        tool2.setCategory(category);
        tool2.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool2.setInitialStock(5);
        tool2.setCurrentStock(3);
        tool2.setReplacementValue(BigDecimal.valueOf(50.0));
        tool2.setRentalRate(BigDecimal.valueOf(5.0));
        entityManager.persist(tool2);

        // Create loan
        loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool1);
        loan.setQuantity(1);
        loan.setLoanDate(LocalDate.now().minusDays(5));
        loan.setAgreedReturnDate(LocalDate.now().plusDays(2));
        loan.setDailyRate(BigDecimal.valueOf(10.0));
        loan.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(loan);

        // Create kardex movements
        initialStock = new KardexMovementEntity();
        initialStock.setTool(tool1);
        initialStock.setType(KardexMovementEntity.MovementType.INITIAL_STOCK);
        initialStock.setQuantity(10);
        initialStock.setStockBefore(0);
        initialStock.setStockAfter(10);
        initialStock.setDescription("Initial stock");
        initialStock.setCreatedAt(LocalDateTime.now().minusDays(30));
        entityManager.persist(initialStock);

        loanMovement = new KardexMovementEntity();
        loanMovement.setTool(tool1);
        loanMovement.setType(KardexMovementEntity.MovementType.LOAN);
        loanMovement.setQuantity(-1);
        loanMovement.setStockBefore(10);
        loanMovement.setStockAfter(9);
        loanMovement.setRelatedLoan(loan);
        loanMovement.setDescription("Loan to client");
        loanMovement.setCreatedAt(LocalDateTime.now().minusDays(5));
        entityManager.persist(loanMovement);

        returnMovement = new KardexMovementEntity();
        returnMovement.setTool(tool1);
        returnMovement.setType(KardexMovementEntity.MovementType.RETURN);
        returnMovement.setQuantity(1);
        returnMovement.setStockBefore(9);
        returnMovement.setStockAfter(10);
        returnMovement.setRelatedLoan(loan);
        returnMovement.setDescription("Return from client");
        returnMovement.setCreatedAt(LocalDateTime.now().minusDays(2));
        entityManager.persist(returnMovement);

        restockMovement = new KardexMovementEntity();
        restockMovement.setTool(tool2);
        restockMovement.setType(KardexMovementEntity.MovementType.RESTOCK);
        restockMovement.setQuantity(5);
        restockMovement.setStockBefore(0);
        restockMovement.setStockAfter(5);
        restockMovement.setDescription("New stock arrival");
        restockMovement.setCreatedAt(LocalDateTime.now().minusDays(10));
        entityManager.persist(restockMovement);

        entityManager.flush();
    }

    @Test
    void testFindByToolOrderByCreatedAtDesc_ShouldReturnMovementsForTool() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByToolOrderByCreatedAtDesc(tool1);

        // Then
        assertThat(results).hasSize(3);
        // Should be ordered by createdAt descending
        assertThat(results.get(0)).isEqualTo(returnMovement);
        assertThat(results.get(1)).isEqualTo(loanMovement);
        assertThat(results.get(2)).isEqualTo(initialStock);
    }

    @Test
    void testFindByToolIdOrderByCreatedAtDesc_ShouldReturnMovementsForTool() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(tool1.getId());

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByDateRangeOrderByCreatedAtDesc_ShouldReturnMovementsInRange() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByDateRangeOrderByCreatedAtDesc(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now()
        );

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyInAnyOrder(loanMovement, returnMovement);
    }

    @Test
    void testFindByToolAndDateRangeOrderByCreatedAtDesc_ShouldReturnMatchingMovements() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByToolAndDateRangeOrderByCreatedAtDesc(
                tool1,
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now()
        );

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByToolIdAndDateRangeOrderByCreatedAtDesc_ShouldReturnMatchingMovements() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByToolIdAndDateRangeOrderByCreatedAtDesc(
                tool1.getId(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now()
        );

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByTypeOrderByCreatedAtDesc_ShouldReturnMovementsOfType() {
        // When
        List<KardexMovementEntity> loanMovements = kardexMovementRepository.findByTypeOrderByCreatedAtDesc(
                KardexMovementEntity.MovementType.LOAN
        );
        List<KardexMovementEntity> returnMovements = kardexMovementRepository.findByTypeOrderByCreatedAtDesc(
                KardexMovementEntity.MovementType.RETURN
        );

        // Then
        assertThat(loanMovements).hasSize(1);
        assertThat(returnMovements).hasSize(1);
    }

    @Test
    void testFindByRelatedLoanOrderByCreatedAtDesc_ShouldReturnMovementsForLoan() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByRelatedLoanOrderByCreatedAtDesc(loan);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyInAnyOrder(loanMovement, returnMovement);
    }

    @Test
    void testFindByRelatedLoanIdOrderByCreatedAtDesc_ShouldReturnMovementsForLoan() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findByRelatedLoanIdOrderByCreatedAtDesc(loan.getId());

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindLatestMovements_ShouldReturnRecentMovements() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findLatestMovements(PageRequest.of(0, 2));

        // Then
        assertThat(results).hasSize(2);
        // Should be ordered by createdAt descending
        assertThat(results.get(0)).isEqualTo(returnMovement);
    }

    @Test
    void testFindStockMovements_ShouldReturnOnlyStockAffectingMovements() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findStockMovements();

        // Then
        assertThat(results).hasSize(4);
        assertThat(results).extracting(KardexMovementEntity::getType)
                .containsOnly(
                        KardexMovementEntity.MovementType.INITIAL_STOCK,
                        KardexMovementEntity.MovementType.LOAN,
                        KardexMovementEntity.MovementType.RETURN,
                        KardexMovementEntity.MovementType.RESTOCK
                );
    }

    @Test
    void testFindStockMovementsByTool_ShouldReturnStockMovementsForTool() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findStockMovementsByTool(tool1.getId());

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindLastMovementByTool_ShouldReturnMostRecentMovement() {
        // When
        List<KardexMovementEntity> results = kardexMovementRepository.findLastMovementByTool(tool1.getId());

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0)).isEqualTo(returnMovement);
    }

    @Test
    void testGetMovementSummaryByDateRange_ShouldReturnSummary() {
        // When
        List<Object[]> results = kardexMovementRepository.getMovementSummaryByDateRange(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now()
        );

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    void testSaveKardexMovement_ShouldPersistMovement() {
        // Given
        KardexMovementEntity newMovement = new KardexMovementEntity();
        newMovement.setTool(tool2);
        newMovement.setType(KardexMovementEntity.MovementType.RESTOCK);
        newMovement.setQuantity(3);
        newMovement.setStockBefore(5);
        newMovement.setStockAfter(8);
        newMovement.setDescription("Additional stock");
        newMovement.setCreatedAt(LocalDateTime.now());

        // When
        KardexMovementEntity saved = kardexMovementRepository.save(newMovement);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(kardexMovementRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testDeleteKardexMovement_ShouldRemoveMovement() {
        // Given
        Long movementId = restockMovement.getId();

        // When
        kardexMovementRepository.delete(restockMovement);

        // Then
        assertThat(kardexMovementRepository.findById(movementId)).isEmpty();
    }

    @Test
    void testFindByToolIdOrderByCreatedAtDesc_WithMultipleTools_ShouldReturnOnlyToolMovements() {
        // When
        List<KardexMovementEntity> tool1Results = kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(tool1.getId());
        List<KardexMovementEntity> tool2Results = kardexMovementRepository.findByToolIdOrderByCreatedAtDesc(tool2.getId());

        // Then
        assertThat(tool1Results).hasSize(3);
        assertThat(tool2Results).hasSize(1);
        assertThat(tool2Results.get(0)).isEqualTo(restockMovement);
    }

    @Test
    void testCountMovementsByType_ShouldReturnStatistics() {
        // When
        List<Object[]> results = kardexMovementRepository.countMovementsByType();

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    void testCountMovementsByTool_ShouldReturnStatistics() {
        // When
        List<Object[]> results = kardexMovementRepository.countMovementsByTool();

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }
}

