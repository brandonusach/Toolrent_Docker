package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.CategoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void whenFindByNameIgnoreCase_thenReturnCategory() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Eléctricas", "Categoría de herramientas eléctricas");
        entityManager.persistAndFlush(category);

        // when
        Optional<CategoryEntity> found = categoryRepository.findByNameIgnoreCase("Herramientas Eléctricas");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Herramientas Eléctricas");
    }

    @Test
    public void whenFindByNameIgnoreCase_WithDifferentCase_thenReturnCategory() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Eléctricas", "Categoría de herramientas eléctricas");
        entityManager.persistAndFlush(category);

        // when
        Optional<CategoryEntity> found = categoryRepository.findByNameIgnoreCase("HERRAMIENTAS ELÉCTRICAS");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Herramientas Eléctricas");
    }

    @Test
    public void whenFindByNameIgnoreCase_WithNonExisting_thenReturnEmpty() {
        // when
        Optional<CategoryEntity> found = categoryRepository.findByNameIgnoreCase("Categoría Inexistente");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    public void whenExistsByNameIgnoreCase_WithExistingName_thenReturnTrue() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Manuales", "Categoría de herramientas manuales");
        entityManager.persistAndFlush(category);

        // when
        boolean exists = categoryRepository.existsByNameIgnoreCase("Herramientas Manuales");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenExistsByNameIgnoreCase_WithDifferentCase_thenReturnTrue() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Manuales", "Categoría de herramientas manuales");
        entityManager.persistAndFlush(category);

        // when
        boolean exists = categoryRepository.existsByNameIgnoreCase("herramientas manuales");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenExistsByNameIgnoreCase_WithNonExisting_thenReturnFalse() {
        // when
        boolean exists = categoryRepository.existsByNameIgnoreCase("Categoría Inexistente");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    public void whenFindByNameContainingIgnoreCase_thenReturnCategories() {
        // given
        CategoryEntity category1 = new CategoryEntity(null, "Herramientas Eléctricas", "Descripción 1");
        CategoryEntity category2 = new CategoryEntity(null, "Herramientas Manuales", "Descripción 2");
        CategoryEntity category3 = new CategoryEntity(null, "Equipos de Seguridad", "Descripción 3");
        entityManager.persist(category1);
        entityManager.persist(category2);
        entityManager.persist(category3);
        entityManager.flush();

        // when
        List<CategoryEntity> found = categoryRepository.findByNameContainingIgnoreCase("Herramientas");

        // then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(CategoryEntity::getName)
                .containsExactlyInAnyOrder("Herramientas Eléctricas", "Herramientas Manuales");
    }

    @Test
    public void whenFindByNameContainingIgnoreCase_WithDifferentCase_thenReturnCategories() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Eléctricas", "Descripción");
        entityManager.persistAndFlush(category);

        // when
        List<CategoryEntity> found = categoryRepository.findByNameContainingIgnoreCase("ELÉCTRICAS");

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Herramientas Eléctricas");
    }

    @Test
    public void whenFindByNameContainingIgnoreCase_WithNonExisting_thenReturnEmpty() {
        // given
        CategoryEntity category = new CategoryEntity(null, "Herramientas Eléctricas", "Descripción");
        entityManager.persistAndFlush(category);

        // when
        List<CategoryEntity> found = categoryRepository.findByNameContainingIgnoreCase("Inexistente");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    public void whenFindAllOrderByName_thenReturnCategoriesOrderedByName() {
        // given
        CategoryEntity category1 = new CategoryEntity(null, "Equipos de Seguridad", "Descripción 1");
        CategoryEntity category2 = new CategoryEntity(null, "Herramientas Eléctricas", "Descripción 2");
        CategoryEntity category3 = new CategoryEntity(null, "Accesorios", "Descripción 3");
        entityManager.persist(category1);
        entityManager.persist(category2);
        entityManager.persist(category3);
        entityManager.flush();

        // when
        List<CategoryEntity> found = categoryRepository.findAllOrderByName();

        // then
        assertThat(found).hasSize(3);
        assertThat(found).extracting(CategoryEntity::getName)
                .containsExactly("Accesorios", "Equipos de Seguridad", "Herramientas Eléctricas");
    }

    @Test
    public void whenFindAllOrderByName_WithNoCategories_thenReturnEmpty() {
        // when
        List<CategoryEntity> found = categoryRepository.findAllOrderByName();

        // then
        assertThat(found).isEmpty();
    }
}