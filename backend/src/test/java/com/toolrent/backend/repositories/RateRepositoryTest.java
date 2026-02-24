package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.RateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RateRepository rateRepository;

    private RateEntity dailyRate;
    private RateEntity weeklyRate;
    private RateEntity monthlyRate;
    private RateEntity expiredRate;

    @BeforeEach
    void setUp() {
        dailyRate = new RateEntity();
        dailyRate.setType(RateEntity.RateType.RENTAL_RATE);
        dailyRate.setDailyAmount(BigDecimal.valueOf(10.0));
        dailyRate.setActive(true);
        dailyRate.setEffectiveFrom(LocalDate.now().minusMonths(1));
        dailyRate.setEffectiveTo(null);
        dailyRate.setCreatedBy("admin");

        weeklyRate = new RateEntity();
        weeklyRate.setType(RateEntity.RateType.LATE_FEE_RATE);
        weeklyRate.setDailyAmount(BigDecimal.valueOf(5.0));
        weeklyRate.setActive(true);
        weeklyRate.setEffectiveFrom(LocalDate.now().minusMonths(1));
        weeklyRate.setEffectiveTo(null);
        weeklyRate.setCreatedBy("admin");

        monthlyRate = new RateEntity();
        monthlyRate.setType(RateEntity.RateType.REPAIR_RATE);
        monthlyRate.setDailyAmount(BigDecimal.valueOf(20.0));
        monthlyRate.setActive(false);
        monthlyRate.setEffectiveFrom(LocalDate.now().minusMonths(2));
        monthlyRate.setEffectiveTo(LocalDate.now().minusMonths(1));
        monthlyRate.setCreatedBy("admin");

        expiredRate = new RateEntity();
        expiredRate.setType(RateEntity.RateType.RENTAL_RATE);
        expiredRate.setDailyAmount(BigDecimal.valueOf(8.0));
        expiredRate.setActive(false);
        expiredRate.setEffectiveFrom(LocalDate.now().minusMonths(3));
        expiredRate.setEffectiveTo(LocalDate.now().minusMonths(2));
        expiredRate.setCreatedBy("admin");

        entityManager.persist(dailyRate);
        entityManager.persist(weeklyRate);
        entityManager.persist(monthlyRate);
        entityManager.persist(expiredRate);
        entityManager.flush();
    }

    @Test
    void testFindByType_ShouldReturnRatesOfType() {
        // When
        List<RateEntity> results = rateRepository.findByType(RateEntity.RateType.RENTAL_RATE);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByActive_ShouldReturnActiveRates() {
        // When
        List<RateEntity> activeRates = rateRepository.findByActive(true);
        List<RateEntity> inactiveRates = rateRepository.findByActive(false);

        // Then
        assertThat(activeRates).hasSize(2);
        assertThat(inactiveRates).hasSize(2);
    }

    @Test
    void testFindByTypeAndActive_ShouldReturnMatchingRates() {
        // When
        List<RateEntity> results = rateRepository.findByTypeAndActive(RateEntity.RateType.RENTAL_RATE, true);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDailyAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    }

    @Test
    void testFindActiveRateByTypeAndDate_WhenRateExists_ShouldReturnRate() {
        // When
        Optional<RateEntity> result = rateRepository.findActiveRateByTypeAndDate(
                RateEntity.RateType.RENTAL_RATE,
                LocalDate.now()
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDailyAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    }

    @Test
    void testFindActiveRateByTypeAndDate_WhenNoRateExists_ShouldReturnEmpty() {
        // When
        Optional<RateEntity> result = rateRepository.findActiveRateByTypeAndDate(
                RateEntity.RateType.RENTAL_RATE,
                LocalDate.now().minusYears(1)
        );

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindCurrentActiveRateByType_WhenRateExists_ShouldReturnRate() {
        // When
        Optional<RateEntity> result = rateRepository.findCurrentActiveRateByType(RateEntity.RateType.LATE_FEE_RATE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDailyAmount()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
    }

    @Test
    void testFindCurrentActiveRateByType_WhenNoActiveRate_ShouldReturnEmpty() {
        // When
        Optional<RateEntity> result = rateRepository.findCurrentActiveRateByType(RateEntity.RateType.REPAIR_RATE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindRatesInDateRange_ShouldReturnRatesInRange() {
        // When
        List<RateEntity> results = rateRepository.findRatesInDateRange(
                LocalDate.now().minusMonths(2),
                LocalDate.now()
        );

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void testFindRatesByTypeInDateRange_ShouldReturnMatchingRates() {
        // When
        List<RateEntity> results = rateRepository.findRatesByTypeInDateRange(
                RateEntity.RateType.RENTAL_RATE,
                LocalDate.now().minusMonths(3),
                LocalDate.now()
        );

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testExistsByTypeAndActiveTrue_WhenActiveRateExists_ShouldReturnTrue() {
        // When
        boolean exists = rateRepository.existsByTypeAndActiveTrue(RateEntity.RateType.RENTAL_RATE);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByTypeAndActiveTrue_WhenNoActiveRate_ShouldReturnFalse() {
        // When
        boolean exists = rateRepository.existsByTypeAndActiveTrue(RateEntity.RateType.REPAIR_RATE);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testCountActiveRatesByTypeAndDate_ShouldReturnCorrectCount() {
        // When
        long count = rateRepository.countActiveRatesByTypeAndDate(
                RateEntity.RateType.RENTAL_RATE,
                LocalDate.now()
        );

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testSaveRate_ShouldPersistRate() {
        // Given
        RateEntity newRate = new RateEntity();
        newRate.setType(RateEntity.RateType.RENTAL_RATE);
        newRate.setDailyAmount(BigDecimal.valueOf(2.0));
        newRate.setActive(true);
        newRate.setEffectiveFrom(LocalDate.now());
        newRate.setEffectiveTo(null);
        newRate.setCreatedBy("admin");

        // When
        RateEntity saved = rateRepository.save(newRate);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(rateRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testUpdateRate_ShouldModifyRate() {
        // Given
        dailyRate.setDailyAmount(BigDecimal.valueOf(12.0));

        // When
        RateEntity updated = rateRepository.save(dailyRate);

        // Then
        assertThat(updated.getDailyAmount()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
    }

    @Test
    void testDeleteRate_ShouldRemoveRate() {
        // Given
        Long rateId = dailyRate.getId();

        // When
        rateRepository.delete(dailyRate);

        // Then
        assertThat(rateRepository.findById(rateId)).isEmpty();
    }
}

