package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DamageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DamageRepository damageRepository;

    private ClientEntity client;
    private ToolEntity tool;
    private ToolInstanceEntity toolInstance1;
    private ToolInstanceEntity toolInstance2;
    private LoanEntity loan;
    private DamageEntity reportedDamage;
    private DamageEntity repairInProgressDamage;
    private DamageEntity repairedDamage;
    private DamageEntity irreparableDamage;

    @BeforeEach
    void setUp() {
        // Create client
        client = new ClientEntity();
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

        // Create tool
        tool = new ToolEntity();
        tool.setName("Drill");
        tool.setCategory(category);
        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool.setInitialStock(10);
        tool.setCurrentStock(5);
        tool.setReplacementValue(BigDecimal.valueOf(100.0));
        tool.setRentalRate(BigDecimal.valueOf(10.0));
        entityManager.persist(tool);

        // Create tool instances
        toolInstance1 = new ToolInstanceEntity();
        toolInstance1.setTool(tool);
        toolInstance1.setStatus(ToolInstanceEntity.ToolInstanceStatus.LOANED);
        entityManager.persist(toolInstance1);

        toolInstance2 = new ToolInstanceEntity();
        toolInstance2.setTool(tool);
        toolInstance2.setStatus(ToolInstanceEntity.ToolInstanceStatus.UNDER_REPAIR);
        entityManager.persist(toolInstance2);

        // Create loan
        loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setQuantity(1);
        loan.setLoanDate(LocalDate.now().minusDays(10));
        loan.setAgreedReturnDate(LocalDate.now().minusDays(3));
        loan.setDailyRate(BigDecimal.valueOf(10.0));
        loan.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(loan);

        // Create damages
        reportedDamage = new DamageEntity();
        reportedDamage.setLoan(loan);
        reportedDamage.setToolInstance(toolInstance1);
        reportedDamage.setType(DamageEntity.DamageType.MINOR);
        reportedDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        reportedDamage.setDescription("Minor scratch");
        reportedDamage.setReportedAt(LocalDateTime.now().minusDays(2));
        reportedDamage.setIsRepairable(true);
        entityManager.persist(reportedDamage);

        repairInProgressDamage = new DamageEntity();
        repairInProgressDamage.setLoan(loan);
        repairInProgressDamage.setToolInstance(toolInstance2);
        repairInProgressDamage.setType(DamageEntity.DamageType.MAJOR);
        repairInProgressDamage.setStatus(DamageEntity.DamageStatus.REPAIR_IN_PROGRESS);
        repairInProgressDamage.setDescription("Broken handle");
        repairInProgressDamage.setReportedAt(LocalDateTime.now().minusDays(5));
        repairInProgressDamage.setIsRepairable(true);
        entityManager.persist(repairInProgressDamage);

        repairedDamage = new DamageEntity();
        repairedDamage.setLoan(loan);
        repairedDamage.setToolInstance(toolInstance1);
        repairedDamage.setType(DamageEntity.DamageType.MINOR);
        repairedDamage.setStatus(DamageEntity.DamageStatus.REPAIRED);
        repairedDamage.setDescription("Fixed scratch");
        repairedDamage.setReportedAt(LocalDateTime.now().minusDays(10));
        repairedDamage.setIsRepairable(true);
        entityManager.persist(repairedDamage);

        irreparableDamage = new DamageEntity();
        irreparableDamage.setLoan(loan);
        irreparableDamage.setToolInstance(toolInstance2);
        irreparableDamage.setType(DamageEntity.DamageType.IRREPARABLE);
        irreparableDamage.setStatus(DamageEntity.DamageStatus.IRREPARABLE);
        irreparableDamage.setDescription("Motor burned out");
        irreparableDamage.setReportedAt(LocalDateTime.now().minusDays(15));
        irreparableDamage.setIsRepairable(false);
        entityManager.persist(irreparableDamage);

        entityManager.flush();
    }

    @Test
    void testFindByLoanOrderByReportedAtDesc_ShouldReturnDamagesForLoan() {
        // When
        List<DamageEntity> results = damageRepository.findByLoanOrderByReportedAtDesc(loan);

        // Then
        assertThat(results).hasSize(4);
        // Should be ordered by reportedAt descending
        assertThat(results.get(0)).isEqualTo(reportedDamage);
    }

    @Test
    void testFindByLoanIdOrderByReportedAtDesc_ShouldReturnDamagesForLoan() {
        // When
        List<DamageEntity> results = damageRepository.findByLoanIdOrderByReportedAtDesc(loan.getId());

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testFindByToolInstanceOrderByReportedAtDesc_ShouldReturnDamagesForInstance() {
        // When
        List<DamageEntity> results = damageRepository.findByToolInstanceOrderByReportedAtDesc(toolInstance1);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyInAnyOrder(reportedDamage, repairedDamage);
    }

    @Test
    void testFindByToolInstanceIdOrderByReportedAtDesc_ShouldReturnDamagesForInstance() {
        // When
        List<DamageEntity> results = damageRepository.findByToolInstanceIdOrderByReportedAtDesc(toolInstance2.getId());

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByStatusOrderByReportedAtDesc_ShouldReturnDamagesWithStatus() {
        // When
        List<DamageEntity> reported = damageRepository.findByStatusOrderByReportedAtDesc(DamageEntity.DamageStatus.REPORTED);
        List<DamageEntity> repaired = damageRepository.findByStatusOrderByReportedAtDesc(DamageEntity.DamageStatus.REPAIRED);

        // Then
        assertThat(reported).hasSize(1);
        assertThat(repaired).hasSize(1);
    }

    @Test
    void testFindByTypeOrderByReportedAtDesc_ShouldReturnDamagesOfType() {
        // When
        List<DamageEntity> minor = damageRepository.findByTypeOrderByReportedAtDesc(DamageEntity.DamageType.MINOR);
        List<DamageEntity> irreparable = damageRepository.findByTypeOrderByReportedAtDesc(DamageEntity.DamageType.IRREPARABLE);

        // Then
        assertThat(minor).hasSize(2);
        assertThat(irreparable).hasSize(1);
    }

    @Test
    void testFindByClientOrderByReportedAtDesc_ShouldReturnClientDamages() {
        // When
        List<DamageEntity> results = damageRepository.findByClientOrderByReportedAtDesc(client);

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testFindByClientIdOrderByReportedAtDesc_ShouldReturnClientDamages() {
        // When
        List<DamageEntity> results = damageRepository.findByClientIdOrderByReportedAtDesc(client.getId());

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testFindPendingAssessments_ShouldReturnReportedDamages() {
        // When
        List<DamageEntity> results = damageRepository.findPendingAssessments();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(reportedDamage);
    }

    @Test
    void testFindDamagesUnderRepair_ShouldReturnRepairingDamages() {
        // When
        List<DamageEntity> results = damageRepository.findDamagesUnderRepair();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(repairInProgressDamage);
    }

    @Test
    void testFindIrreparableDamages_ShouldReturnIrreparableDamages() {
        // When
        List<DamageEntity> results = damageRepository.findIrreparableDamages();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(irreparableDamage);
    }

    @Test
    void testFindByDateRange_ShouldReturnDamagesInRange() {
        // When
        List<DamageEntity> results = damageRepository.findByDateRange(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now()
        );

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByToolIdOrderByReportedAtDesc_ShouldReturnDamagesForTool() {
        // When
        List<DamageEntity> results = damageRepository.findByToolIdOrderByReportedAtDesc(tool.getId());

        // Then
        assertThat(results).hasSize(4);
    }

    @Test
    void testCountByStatus_ShouldReturnCorrectCount() {
        // When
        long reportedCount = damageRepository.countByStatus(DamageEntity.DamageStatus.REPORTED);
        long repairedCount = damageRepository.countByStatus(DamageEntity.DamageStatus.REPAIRED);

        // Then
        assertThat(reportedCount).isEqualTo(1);
        assertThat(repairedCount).isEqualTo(1);
    }

    @Test
    void testCountByType_ShouldReturnCorrectCount() {
        // When
        long minorCount = damageRepository.countByType(DamageEntity.DamageType.MINOR);
        long irreparableCount = damageRepository.countByType(DamageEntity.DamageType.IRREPARABLE);

        // Then
        assertThat(minorCount).isEqualTo(2);
        assertThat(irreparableCount).isEqualTo(1);
    }

    @Test
    void testCountByClientId_ShouldReturnCorrectCount() {
        // When
        long count = damageRepository.countByClientId(client.getId());

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    void testCountIrreparableDamagesByClientId_ShouldReturnCorrectCount() {
        // When
        long count = damageRepository.countIrreparableDamagesByClientId(client.getId());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindMostRecentByToolInstanceId_ShouldReturnMostRecentDamage() {
        // When
        Optional<DamageEntity> result = damageRepository.findMostRecentByToolInstanceId(toolInstance1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(reportedDamage);
    }

    @Test
    void testHasPendingDamages_WhenPendingExists_ShouldReturnTrue() {
        // When
        boolean hasPending = damageRepository.hasPendingDamages(toolInstance1.getId());

        // Then
        assertThat(hasPending).isTrue();
    }

    @Test
    void testHasPendingDamages_WhenNoPending_ShouldReturnFalse() {
        // Given - mark all as repaired
        reportedDamage.setStatus(DamageEntity.DamageStatus.REPAIRED);
        entityManager.persist(reportedDamage);
        entityManager.flush();

        // When
        boolean hasPending = damageRepository.hasPendingDamages(toolInstance1.getId());

        // Then
        assertThat(hasPending).isFalse();
    }

    @Test
    void testSaveDamage_ShouldPersistDamage() {
        // Given
        DamageEntity newDamage = new DamageEntity();
        newDamage.setLoan(loan);
        newDamage.setToolInstance(toolInstance1);
        newDamage.setType(DamageEntity.DamageType.MINOR);
        newDamage.setStatus(DamageEntity.DamageStatus.REPORTED);
        newDamage.setDescription("New damage");
        newDamage.setReportedAt(LocalDateTime.now());
        newDamage.setIsRepairable(true);

        // When
        DamageEntity saved = damageRepository.save(newDamage);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(damageRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testUpdateDamage_ShouldModifyDamage() {
        // Given
        reportedDamage.setStatus(DamageEntity.DamageStatus.REPAIRED);

        // When
        DamageEntity updated = damageRepository.save(reportedDamage);

        // Then
        assertThat(updated.getStatus()).isEqualTo(DamageEntity.DamageStatus.REPAIRED);
    }

    @Test
    void testDeleteDamage_ShouldRemoveDamage() {
        // Given
        Long damageId = reportedDamage.getId();

        // When
        damageRepository.delete(reportedDamage);

        // Then
        assertThat(damageRepository.findById(damageId)).isEmpty();
    }
}

