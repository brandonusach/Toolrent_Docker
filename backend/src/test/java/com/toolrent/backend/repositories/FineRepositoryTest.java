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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class FineRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FineRepository fineRepository;

    private ClientEntity client1;
    private ClientEntity client2;
    private LoanEntity loan1;
    private LoanEntity loan2;
    private FineEntity unpaidFine1;
    private FineEntity unpaidFine2;
    private FineEntity paidFine;
    private FineEntity overdueFine;

    @BeforeEach
    void setUp() {
        // Create clients
        client1 = new ClientEntity();
        client1.setRut("12345678-9");
        client1.setName("Juan Pérez");
        client1.setEmail("juan@example.com");
        client1.setPhone("912345678");
        client1.setStatus(ClientEntity.ClientStatus.ACTIVE);
        entityManager.persist(client1);

        client2 = new ClientEntity();
        client2.setRut("98765432-1");
        client2.setName("María González");
        client2.setEmail("maria@example.com");
        client2.setPhone("987654321");
        client2.setStatus(ClientEntity.ClientStatus.ACTIVE);
        entityManager.persist(client2);

        // Create category and tool
        CategoryEntity category = new CategoryEntity();
        category.setName("Power Tools");
        category.setDescription("Electric power tools");
        entityManager.persist(category);

        ToolEntity tool = new ToolEntity();
        tool.setName("Drill");
        tool.setCategory(category);
        tool.setStatus(ToolEntity.ToolStatus.AVAILABLE);
        tool.setInitialStock(10);
        tool.setCurrentStock(5);
        tool.setReplacementValue(BigDecimal.valueOf(100.0));
        tool.setRentalRate(BigDecimal.valueOf(10.0));
        entityManager.persist(tool);

        // Create loans
        loan1 = new LoanEntity();
        loan1.setClient(client1);
        loan1.setTool(tool);
        loan1.setQuantity(1);
        loan1.setLoanDate(LocalDate.now().minusDays(10));
        loan1.setAgreedReturnDate(LocalDate.now().minusDays(3));
        loan1.setDailyRate(BigDecimal.valueOf(10.0));
        loan1.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(loan1);

        loan2 = new LoanEntity();
        loan2.setClient(client2);
        loan2.setTool(tool);
        loan2.setQuantity(1);
        loan2.setLoanDate(LocalDate.now().minusDays(5));
        loan2.setAgreedReturnDate(LocalDate.now().plusDays(2));
        loan2.setDailyRate(BigDecimal.valueOf(10.0));
        loan2.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(loan2);

        // Create fines
        unpaidFine1 = new FineEntity();
        unpaidFine1.setClient(client1);
        unpaidFine1.setLoan(loan1);
        unpaidFine1.setType(FineEntity.FineType.LATE_RETURN);
        unpaidFine1.setAmount(BigDecimal.valueOf(15.0));
        unpaidFine1.setDueDate(LocalDate.now().plusDays(7));
        unpaidFine1.setPaid(false);
        entityManager.persist(unpaidFine1);

        unpaidFine2 = new FineEntity();
        unpaidFine2.setClient(client1);
        unpaidFine2.setLoan(loan1);
        unpaidFine2.setType(FineEntity.FineType.DAMAGE_REPAIR);
        unpaidFine2.setAmount(BigDecimal.valueOf(50.0));
        unpaidFine2.setDueDate(LocalDate.now().plusDays(14));
        unpaidFine2.setPaid(false);
        entityManager.persist(unpaidFine2);

        paidFine = new FineEntity();
        paidFine.setClient(client2);
        paidFine.setLoan(loan2);
        paidFine.setType(FineEntity.FineType.LATE_RETURN);
        paidFine.setAmount(BigDecimal.valueOf(10.0));
        paidFine.setDueDate(LocalDate.now().minusDays(1));
        paidFine.setPaid(true);
        paidFine.setPaidDate(LocalDate.now().minusDays(2));
        entityManager.persist(paidFine);

        overdueFine = new FineEntity();
        overdueFine.setClient(client1);
        overdueFine.setLoan(loan1);
        overdueFine.setType(FineEntity.FineType.TOOL_REPLACEMENT);
        overdueFine.setAmount(BigDecimal.valueOf(200.0));
        overdueFine.setDueDate(LocalDate.now().minusDays(5));
        overdueFine.setPaid(false);
        entityManager.persist(overdueFine);

        entityManager.flush();
    }

    @Test
    void testFindByClient_ShouldReturnClientFines() {
        // When
        List<FineEntity> results = fineRepository.findByClient(client1);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByClientAndPaidFalse_ShouldReturnUnpaidFines() {
        // When
        List<FineEntity> results = fineRepository.findByClientAndPaidFalse(client1);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).containsExactlyInAnyOrder(unpaidFine1, unpaidFine2, overdueFine);
    }

    @Test
    void testFindByClientAndPaidFalse_WhenNoUnpaidFines_ShouldReturnEmpty() {
        // Given - pay all fines
        unpaidFine1.setPaid(true);
        unpaidFine2.setPaid(true);
        overdueFine.setPaid(true);
        entityManager.persist(unpaidFine1);
        entityManager.persist(unpaidFine2);
        entityManager.persist(overdueFine);
        entityManager.flush();

        // When
        List<FineEntity> results = fineRepository.findByClientAndPaidFalse(client1);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByClientAndPaidTrue_ShouldReturnPaidFines() {
        // When
        List<FineEntity> results = fineRepository.findByClientAndPaidTrue(client2);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(paidFine);
    }

    @Test
    void testFindByPaidFalse_ShouldReturnAllUnpaidFines() {
        // When
        List<FineEntity> results = fineRepository.findByPaidFalse();

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByLoan_ShouldReturnLoanFines() {
        // When
        List<FineEntity> results = fineRepository.findByLoan(loan1);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByType_ShouldReturnFinesOfType() {
        // When
        List<FineEntity> lateReturnFines = fineRepository.findByType(FineEntity.FineType.LATE_RETURN);
        List<FineEntity> damageRepairFines = fineRepository.findByType(FineEntity.FineType.DAMAGE_REPAIR);
        List<FineEntity> replacementFines = fineRepository.findByType(FineEntity.FineType.TOOL_REPLACEMENT);

        // Then
        assertThat(lateReturnFines).hasSize(2);
        assertThat(damageRepairFines).hasSize(1);
        assertThat(replacementFines).hasSize(1);
    }

    @Test
    void testCountUnpaidFinesByClient_ShouldReturnCorrectCount() {
        // When
        long count = fineRepository.countUnpaidFinesByClient(client1);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testCountUnpaidFinesByClient_WhenNoUnpaidFines_ShouldReturnZero() {
        // When
        long count = fineRepository.countUnpaidFinesByClient(client2);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testCountByPaidFalse_ShouldReturnCorrectCount() {
        // When
        long count = fineRepository.countByPaidFalse();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testCountByPaidTrue_ShouldReturnCorrectCount() {
        // When
        long count = fineRepository.countByPaidTrue();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountOverdueFines_ShouldReturnCorrectCount() {
        // When
        long count = fineRepository.countOverdueFines(LocalDate.now());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindOverdueFines_ShouldReturnOverdueFines() {
        // When
        List<FineEntity> results = fineRepository.findOverdueFines(LocalDate.now());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(overdueFine);
    }

    @Test
    void testSaveFine_ShouldPersistFine() {
        // Given
        FineEntity newFine = new FineEntity();
        newFine.setClient(client2);
        newFine.setLoan(loan2);
        newFine.setType(FineEntity.FineType.LATE_RETURN);
        newFine.setAmount(BigDecimal.valueOf(20.0));
        newFine.setDueDate(LocalDate.now().plusDays(15));
        newFine.setPaid(false);

        // When
        FineEntity saved = fineRepository.save(newFine);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(fineRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testUpdateFine_ShouldModifyFine() {
        // Given
        unpaidFine1.setPaid(true);
        unpaidFine1.setPaidDate(LocalDate.now());

        // When
        FineEntity updated = fineRepository.save(unpaidFine1);

        // Then
        assertThat(updated.getPaid()).isTrue();
        assertThat(updated.getPaidDate()).isNotNull();
    }

    @Test
    void testDeleteFine_ShouldRemoveFine() {
        // Given
        Long fineId = paidFine.getId();

        // When
        fineRepository.delete(paidFine);

        // Then
        assertThat(fineRepository.findById(fineId)).isEmpty();
    }
}

