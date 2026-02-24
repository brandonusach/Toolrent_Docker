package com.toolrent.backend.repositories;

import com.toolrent.backend.entities.ClientEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    private ClientEntity client1;
    private ClientEntity client2;
    private ClientEntity client3;

    @BeforeEach
    void setUp() {
        client1 = new ClientEntity();
        client1.setRut("12345678-9");
        client1.setName("Juan Pérez");
        client1.setEmail("juan@example.com");
        client1.setPhone("912345678");
        client1.setStatus(ClientEntity.ClientStatus.ACTIVE);

        client2 = new ClientEntity();
        client2.setRut("98765432-1");
        client2.setName("María González");
        client2.setEmail("maria@example.com");
        client2.setPhone("987654321");
        client2.setStatus(ClientEntity.ClientStatus.ACTIVE);

        client3 = new ClientEntity();
        client3.setRut("11111111-1");
        client3.setName("Pedro Martínez");
        client3.setEmail("pedro@example.com");
        client3.setPhone("911111111");
        client3.setStatus(ClientEntity.ClientStatus.RESTRICTED);

        entityManager.persist(client1);
        entityManager.persist(client2);
        entityManager.persist(client3);
        entityManager.flush();
    }

    @Test
    void testExistsByRut_WhenRutExists_ShouldReturnTrue() {
        // When
        boolean exists = clientRepository.existsByRut("12345678-9");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByRut_WhenRutDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = clientRepository.existsByRut("00000000-0");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByPhone_WhenPhoneExists_ShouldReturnTrue() {
        // When
        boolean exists = clientRepository.existsByPhone("912345678");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByPhone_WhenPhoneDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = clientRepository.existsByPhone("999999999");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // When
        boolean exists = clientRepository.existsByEmail("juan@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = clientRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByRut_WhenRutExists_ShouldReturnClient() {
        // When
        ClientEntity result = clientRepository.findByRut("12345678-9");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Juan Pérez");
    }

    @Test
    void testFindByRut_WhenRutDoesNotExist_ShouldReturnNull() {
        // When
        ClientEntity result = clientRepository.findByRut("00000000-0");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testFindByNameContainingIgnoreCase_ShouldReturnMatchingClients() {
        // When
        List<ClientEntity> results = clientRepository.findByNameContainingIgnoreCase("pérez");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Juan Pérez");
    }

    @Test
    void testFindByNameContainingIgnoreCase_WithPartialMatch_ShouldReturnMatchingClients() {
        // When
        List<ClientEntity> results = clientRepository.findByNameContainingIgnoreCase("mar");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ClientEntity::getName)
                .containsExactlyInAnyOrder("María González", "Pedro Martínez");
    }

    @Test
    void testFindByStatus_ShouldReturnClientsWithStatus() {
        // When
        List<ClientEntity> activeClients = clientRepository.findByStatus(ClientEntity.ClientStatus.ACTIVE);
        List<ClientEntity> restrictedClients = clientRepository.findByStatus(ClientEntity.ClientStatus.RESTRICTED);

        // Then
        assertThat(activeClients).hasSize(2);
        assertThat(restrictedClients).hasSize(1);
        assertThat(restrictedClients.get(0).getName()).isEqualTo("Pedro Martínez");
    }

    @Test
    void testFindByRutNativeQuery_WhenRutExists_ShouldReturnClient() {
        // When
        ClientEntity result = clientRepository.findByRutNativeQuery("98765432-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("María González");
    }

    @Test
    void testFindByRutNativeQuery_WhenRutDoesNotExist_ShouldReturnNull() {
        // When
        ClientEntity result = clientRepository.findByRutNativeQuery("00000000-0");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testSaveClient_ShouldPersistClient() {
        // Given
        ClientEntity newClient = new ClientEntity();
        newClient.setRut("22222222-2");
        newClient.setName("Ana López");
        newClient.setEmail("ana@example.com");
        newClient.setPhone("922222222");
        newClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        // When
        ClientEntity saved = clientRepository.save(newClient);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(clientRepository.findByRut("22222222-2")).isNotNull();
    }

    @Test
    void testDeleteClient_ShouldRemoveClient() {
        // Given
        Long clientId = client1.getId();

        // When
        clientRepository.delete(client1);

        // Then
        assertThat(clientRepository.findById(clientId)).isEmpty();
    }
}

