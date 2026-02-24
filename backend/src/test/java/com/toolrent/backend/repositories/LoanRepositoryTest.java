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
class LoanRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoanRepository loanRepository;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity tool1;
    private ToolEntity tool2;
    private LoanEntity activeLoan;
    private LoanEntity overdueLoan;
    private LoanEntity completedLoan;

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

        // Create loans
        activeLoan = new LoanEntity();
        activeLoan.setClient(client1);
        activeLoan.setTool(tool1);
        activeLoan.setQuantity(1);
        activeLoan.setLoanDate(LocalDate.now().minusDays(2));
        activeLoan.setAgreedReturnDate(LocalDate.now().plusDays(5));
        activeLoan.setDailyRate(BigDecimal.valueOf(10.0));
        activeLoan.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(activeLoan);

        overdueLoan = new LoanEntity();
        overdueLoan.setClient(client1);
        overdueLoan.setTool(tool2);
        overdueLoan.setQuantity(1);
        overdueLoan.setLoanDate(LocalDate.now().minusDays(10));
        overdueLoan.setAgreedReturnDate(LocalDate.now().minusDays(3));
        overdueLoan.setDailyRate(BigDecimal.valueOf(5.0));
        overdueLoan.setStatus(LoanEntity.LoanStatus.ACTIVE);
        entityManager.persist(overdueLoan);

        completedLoan = new LoanEntity();
        completedLoan.setClient(client2);
        completedLoan.setTool(tool1);
        completedLoan.setQuantity(1);
        completedLoan.setLoanDate(LocalDate.now().minusDays(20));
        completedLoan.setAgreedReturnDate(LocalDate.now().minusDays(15));
        completedLoan.setActualReturnDate(LocalDate.now().minusDays(14));
        completedLoan.setDailyRate(BigDecimal.valueOf(10.0));
        completedLoan.setStatus(LoanEntity.LoanStatus.RETURNED);
        entityManager.persist(completedLoan);

        entityManager.flush();
    }

    @Test
    void testFindOverdueLoansByClient_ShouldReturnOverdueLoans() {
        // When
        List<LoanEntity> results = loanRepository.findOverdueLoansByClient(client1, LocalDate.now());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(overdueLoan);
    }

    @Test
    void testFindOverdueLoansByClient_WhenNoOverdueLoans_ShouldReturnEmpty() {
        // When
        List<LoanEntity> results = loanRepository.findOverdueLoansByClient(client2, LocalDate.now());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void testCountActiveLoansByClientAndTool_ShouldReturnCorrectCount() {
        // When
        long count = loanRepository.countActiveLoansByClientAndTool(client1, tool1);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountActiveLoansByClientAndTool_WhenNoLoans_ShouldReturnZero() {
        // When
        long count = loanRepository.countActiveLoansByClientAndTool(client2, tool2);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testExistsActiveLoanByClientAndTool_WhenLoanExists_ShouldReturnTrue() {
        // When
        boolean exists = loanRepository.existsActiveLoanByClientAndTool(client1, tool1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsActiveLoanByClientAndTool_WhenNoLoan_ShouldReturnFalse() {
        // When
        boolean exists = loanRepository.existsActiveLoanByClientAndTool(client2, tool2);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testCountActiveLoansByClient_ShouldReturnCorrectCount() {
        // When
        long count = loanRepository.countActiveLoansByClient(client1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindByLoanDateBetween_ShouldReturnLoansInRange() {
        // When
        List<LoanEntity> results = loanRepository.findByLoanDateBetween(
                LocalDate.now().minusDays(25),
                LocalDate.now()
        );

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void testFindByLoanDateGreaterThanEqual_ShouldReturnRecentLoans() {
        // When
        List<LoanEntity> results = loanRepository.findByLoanDateGreaterThanEqual(
                LocalDate.now().minusDays(5)
        );

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void testFindByLoanDateLessThanEqual_ShouldReturnOldLoans() {
        // When
        List<LoanEntity> results = loanRepository.findByLoanDateLessThanEqual(
                LocalDate.now().minusDays(5)
        );

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindActiveLoans_ShouldReturnOnlyActiveLoans() {
        // When
        List<LoanEntity> results = loanRepository.findActiveLoans();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyInAnyOrder(activeLoan, overdueLoan);
    }

    @Test
    void testFindOverdueLoans_ShouldReturnOverdueLoans() {
        // When
        List<LoanEntity> results = loanRepository.findOverdueLoans(LocalDate.now());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(overdueLoan);
    }

    @Test
    void testFindByClient_ShouldReturnClientLoans() {
        // When
        List<LoanEntity> results = loanRepository.findByClient(client1);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByTool_ShouldReturnToolLoans() {
        // When
        List<LoanEntity> results = loanRepository.findByTool(tool1);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByStatus_ShouldReturnLoansWithStatus() {
        // When
        List<LoanEntity> activeLoans = loanRepository.findByStatus(LoanEntity.LoanStatus.ACTIVE);
        List<LoanEntity> returnedLoans = loanRepository.findByStatus(LoanEntity.LoanStatus.RETURNED);

        // Then
        assertThat(activeLoans).hasSize(2);
        assertThat(returnedLoans).hasSize(1);
    }

    @Test
    void testSaveLoan_ShouldPersistLoan() {
        // Given
        LoanEntity newLoan = new LoanEntity();
        newLoan.setClient(client2);
        newLoan.setTool(tool2);
        newLoan.setQuantity(1);
        newLoan.setLoanDate(LocalDate.now());
        newLoan.setAgreedReturnDate(LocalDate.now().plusDays(7));
        newLoan.setDailyRate(BigDecimal.valueOf(5.0));
        newLoan.setStatus(LoanEntity.LoanStatus.ACTIVE);

        // When
        LoanEntity saved = loanRepository.save(newLoan);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(loanRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testUpdateLoan_ShouldModifyLoan() {
        // Given
        activeLoan.setActualReturnDate(LocalDate.now());
        activeLoan.setStatus(LoanEntity.LoanStatus.RETURNED);

        // When
        LoanEntity updated = loanRepository.save(activeLoan);

        // Then
        assertThat(updated.getStatus()).isEqualTo(LoanEntity.LoanStatus.RETURNED);
        assertThat(updated.getActualReturnDate()).isNotNull();
    }

    @Test
    void testDeleteLoan_ShouldRemoveLoan() {
        // Given
        Long loanId = completedLoan.getId();

        // When
        loanRepository.delete(completedLoan);

        // Then
        assertThat(loanRepository.findById(loanId)).isEmpty();
    }
}

