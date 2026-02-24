package com.toolrent.backend.services;

import com.toolrent.backend.entities.ClientEntity;
import com.toolrent.backend.entities.ClientEntity.ClientStatus;
import com.toolrent.backend.repositories.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientEntity testClient;

    // RUT Válido: 12.345.678-5
    // RUT Válido con K: 16.292.682-K

    @BeforeEach
    void setUp() {
        testClient = new ClientEntity();
        testClient.setId(1L);
        testClient.setName("Juan Pérez");
        // CORRECCIÓN: Usamos un RUT válido (DV 5)
        testClient.setRut("12345678-5");
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");
        testClient.setStatus(ClientStatus.ACTIVE);
    }

    // ========== Tests for getAllClients ==========
    @Test
    void getAllClients_ShouldReturnAllClients() {
        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setName("María González");
        client2.setRut("98765432-1"); // Asumiendo formato simple para este mock
        client2.setPhone("+56987654321");
        client2.setEmail("maria.gonzalez@example.com");
        client2.setStatus(ClientStatus.ACTIVE);

        List<ClientEntity> expectedClients = Arrays.asList(testClient, client2);
        when(clientRepository.findAll()).thenReturn(expectedClients);

        List<ClientEntity> result = clientService.getAllClients();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(clientRepository, times(1)).findAll();
    }

    @Test
    void getAllClients_ShouldReturnEmptyList_WhenNoClients() {
        when(clientRepository.findAll()).thenReturn(List.of());

        List<ClientEntity> result = clientService.getAllClients();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for getClientById ==========
    @Test
    void getClientById_ShouldReturnClient_WhenExists() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        ClientEntity result = clientService.getClientById(1L);

        assertNotNull(result);
        assertEquals("Juan Pérez", result.getName());
        verify(clientRepository, times(1)).findById(1L);
    }

    @Test
    void getClientById_ShouldReturnNull_WhenNotExists() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        ClientEntity result = clientService.getClientById(999L);

        assertNull(result);
    }

    // ========== Tests for getClientByRut ==========
    @Test
    void getClientByRut_ShouldReturnClient_WhenExists() {
        String normalizedRut = "123456785"; // RUT Normalizado correcto
        when(clientRepository.findByRut(normalizedRut)).thenReturn(testClient);

        ClientEntity result = clientService.getClientByRut("12.345.678-5");

        assertNotNull(result);
        assertEquals("Juan Pérez", result.getName());
        verify(clientRepository, times(1)).findByRut(normalizedRut);
    }

    @Test
    void getClientByRut_ShouldNormalizeRut_BeforeSearch() {
        String normalizedRut = "123456785";
        when(clientRepository.findByRut(normalizedRut)).thenReturn(testClient);

        clientService.getClientByRut("12.345.678-5");

        verify(clientRepository, times(1)).findByRut(normalizedRut);
    }

    @Test
    void getClientByRut_ShouldReturnNull_WhenRutIsNull() {
        ClientEntity result = clientService.getClientByRut(null);

        assertNull(result);
        verify(clientRepository, never()).findByRut(anyString());
    }

    @Test
    void getClientByRut_ShouldReturnNull_WhenRutIsEmpty() {
        ClientEntity result = clientService.getClientByRut("   ");

        assertNull(result);
        verify(clientRepository, never()).findByRut(anyString());
    }

    // ========== Tests for getClientsByName ==========
    @Test
    void getClientsByName_ShouldReturnMatchingClients() {
        List<ClientEntity> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findByNameContainingIgnoreCase("Juan")).thenReturn(expectedClients);

        List<ClientEntity> result = clientService.getClientsByName("Juan");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(clientRepository, times(1)).findByNameContainingIgnoreCase("Juan");
    }

    @Test
    void getClientsByName_ShouldReturnEmptyList_WhenNoMatches() {
        when(clientRepository.findByNameContainingIgnoreCase("NoExiste")).thenReturn(List.of());

        List<ClientEntity> result = clientService.getClientsByName("NoExiste");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Tests for getClientsByStatus ==========
    @Test
    void getClientsByStatus_ShouldReturnClientsWithStatus() {
        List<ClientEntity> expectedClients = Arrays.asList(testClient);
        when(clientRepository.findByStatus(ClientStatus.ACTIVE)).thenReturn(expectedClients);

        List<ClientEntity> result = clientService.getClientsByStatus(ClientStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(clientRepository, times(1)).findByStatus(ClientStatus.ACTIVE);
    }

    // ========== Tests for existsByRut ==========
    @Test
    void existsByRut_ShouldReturnTrue_WhenRutExists() {
        String normalizedRut = "123456785";
        when(clientRepository.existsByRut(normalizedRut)).thenReturn(true);

        boolean result = clientService.existsByRut("12.345.678-5");

        assertTrue(result);
        verify(clientRepository, times(1)).existsByRut(normalizedRut);
    }

    @Test
    void existsByRut_ShouldReturnFalse_WhenRutDoesNotExist() {
        String normalizedRut = "987654321";
        when(clientRepository.existsByRut(normalizedRut)).thenReturn(false);

        boolean result = clientService.existsByRut("98.765.432-1");

        assertFalse(result);
    }

    @Test
    void existsByRut_ShouldReturnFalse_WhenRutIsNull() {
        boolean result = clientService.existsByRut(null);

        assertFalse(result);
        verify(clientRepository, never()).existsByRut(anyString());
    }

    @Test
    void existsByRut_ShouldReturnFalse_WhenRutIsEmpty() {
        boolean result = clientService.existsByRut("   ");

        assertFalse(result);
        verify(clientRepository, never()).existsByRut(anyString());
    }

    // ========== Tests for saveClient ==========
    @Test
    void saveClient_ShouldSaveSuccessfully_WithValidData() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        // CORRECCIÓN: RUT válido (11.111.111-1 es válido)
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ClientEntity result = clientService.saveClient(newClient);

        assertNotNull(result);
        assertEquals("Pedro Soto", result.getName());
        assertEquals("111111111", result.getRut());
        assertEquals("+56911111111", result.getPhone());
        assertEquals("pedro.soto@example.com", result.getEmail());
        assertEquals(ClientStatus.ACTIVE, result.getStatus());
        verify(clientRepository, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void saveClient_ShouldSetDefaultStatus_WhenNotProvided() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");
        newClient.setStatus(null);

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientEntity result = clientService.saveClient(newClient);

        assertEquals(ClientStatus.ACTIVE, result.getStatus());
    }

    @Test
    void saveClient_ShouldNormalizeData_BeforeSaving() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("  Pedro Soto  ");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("911111111");
        newClient.setEmail("PEDRO.SOTO@EXAMPLE.COM");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientEntity result = clientService.saveClient(newClient);

        assertEquals("Pedro Soto", result.getName());
        assertEquals("111111111", result.getRut());
        assertEquals("+56911111111", result.getPhone());
        assertEquals("pedro.soto@example.com", result.getEmail());
    }

    @Test
    void saveClient_ShouldThrowException_WhenClientIsNull() {
        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(null)
        );

        assertTrue(exception.getMessage().contains("Los datos del cliente son requeridos"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenNameIsNull() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName(null);
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("nombre del cliente es requerido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenNameIsEmpty() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("   ");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("nombre del cliente es requerido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenNameIsTooShort() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("A");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("debe tener al menos 2 caracteres"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenRutIsNull() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut(null);
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT del cliente es requerido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenRutIsInvalid() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("12.345.678-0"); // Dígito verificador incorrecto
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT chileno inválido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenPhoneIsNull() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone(null);
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("teléfono del cliente es requerido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenPhoneIsInvalid() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("123456"); // Teléfono inválido
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("teléfono chileno inválido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenEmailIsNull() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail(null);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("email del cliente es requerido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenEmailIsInvalid() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("email-invalido");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("email inválido"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenRutAlreadyExists() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        // CORRECCIÓN: Usar RUT válido para que pase la validación inicial
        newClient.setRut("12.345.678-5");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut("123456785")).thenReturn(true);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe un cliente con RUT") ||
                exception.getMessage().contains("Ya existe un cliente con"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenPhoneAlreadyExists() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56912345678");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(Arrays.asList(testClient));

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe un cliente con el teléfono"));
    }

    @Test
    void saveClient_ShouldThrowException_WhenEmailAlreadyExists() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("juan.perez@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.existsByEmail("juan.perez@example.com")).thenReturn(true);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe un cliente con el email"));
    }

    @Test
    void saveClient_ShouldAcceptValidCellPhone() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("987654321"); // Celular válido
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> clientService.saveClient(newClient));
    }

    @Test
    void saveClient_ShouldAcceptValidLandlinePhone() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("22234567");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> clientService.saveClient(newClient));
    }

    // ========== Tests for updateClient ==========
    @Test
    void updateClient_ShouldUpdateSuccessfully_WithValidData() throws Exception {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(1L);
        updateClient.setName("Juan Pérez Actualizado");
        // CORRECCIÓN: RUT válido
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("juan.perez@example.com");
        updateClient.setStatus(ClientStatus.ACTIVE);

        testClient.setRut("123456785"); // Normalizado
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(updateClient);

        ClientEntity result = clientService.updateClient(updateClient);

        assertNotNull(result);
        assertEquals("Juan Pérez Actualizado", result.getName());
        verify(clientRepository, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void updateClient_ShouldThrowException_WhenIdIsNull() {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(null);
        updateClient.setName("Juan Pérez");
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("juan.perez@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.updateClient(updateClient)
        );

        assertTrue(exception.getMessage().contains("ID del cliente es requerido"));
    }

    @Test
    void updateClient_ShouldThrowException_WhenClientNotFound() {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(999L);
        updateClient.setName("Juan Pérez");
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.updateClient(updateClient)
        );

        assertTrue(exception.getMessage().contains("no existe"));
    }

    @Test
    void updateClient_ShouldAllowSameRut_WhenUpdatingSameClient() throws Exception {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(1L);
        updateClient.setName("Juan Pérez Actualizado");
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("juan.perez@example.com");

        testClient.setRut("123456785");
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(updateClient);

        assertDoesNotThrow(() -> clientService.updateClient(updateClient));
    }

    @Test
    void updateClient_ShouldThrowException_WhenNewRutAlreadyExists() {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(1L);
        updateClient.setName("Juan Pérez");
        // CORRECCIÓN: Nuevo RUT válido
        updateClient.setRut("11.111.111-1");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("juan.perez@example.com");

        testClient.setRut("123456785");
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.existsByRut("111111111")).thenReturn(true);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.updateClient(updateClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe otro cliente con RUT") ||
                exception.getMessage().contains("Ya existe otro cliente con"));
    }

    @Test
    void updateClient_ShouldThrowException_WhenNewPhoneAlreadyExists() {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(1L);
        updateClient.setName("Juan Pérez");
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56987654321"); // Nuevo teléfono
        updateClient.setEmail("juan.perez@example.com");

        ClientEntity otherClient = new ClientEntity();
        otherClient.setId(2L);
        otherClient.setPhone("+56987654321");

        testClient.setRut("123456785");
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.findAll()).thenReturn(Arrays.asList(testClient, otherClient));

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.updateClient(updateClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe otro cliente con el teléfono") ||
                exception.getMessage().contains("Ya existe otro cliente con"));
    }

    @Test
    void updateClient_ShouldThrowException_WhenNewEmailAlreadyExists() {
        ClientEntity updateClient = new ClientEntity();
        updateClient.setId(1L);
        updateClient.setName("Juan Pérez");
        updateClient.setRut("12.345.678-5");
        updateClient.setPhone("+56912345678");
        updateClient.setEmail("other.email@example.com"); // Nuevo email

        testClient.setRut("123456785");
        testClient.setPhone("+56912345678");
        testClient.setEmail("juan.perez@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.existsByEmail("other.email@example.com")).thenReturn(true);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.updateClient(updateClient)
        );

        assertTrue(exception.getMessage().contains("Ya existe otro cliente con el email") ||
                exception.getMessage().contains("Ya existe otro cliente con"));
    }

    // ========== Tests for deleteClient ==========
    @Test
    void deleteClient_ShouldDeleteSuccessfully_WhenClientExists() throws Exception {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        doNothing().when(clientRepository).deleteById(1L);

        boolean result = clientService.deleteClient(1L);

        assertTrue(result);
        verify(clientRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteClient_ShouldThrowException_WhenClientNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.deleteClient(999L)
        );

        assertTrue(exception.getMessage().contains("no existe"));
        verify(clientRepository, never()).deleteById(anyLong());
    }

    // ========== Tests for changeClientStatus ==========
    @Test
    void changeClientStatus_ShouldChangeStatusSuccessfully() throws Exception {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(testClient);

        ClientEntity result = clientService.changeClientStatus(1L, ClientStatus.RESTRICTED);

        assertNotNull(result);
        assertEquals(ClientStatus.RESTRICTED, testClient.getStatus());
        verify(clientRepository, times(1)).save(testClient);
    }

    @Test
    void changeClientStatus_ShouldThrowException_WhenClientNotFound() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.changeClientStatus(999L, ClientStatus.RESTRICTED)
        );

        assertTrue(exception.getMessage().contains("no existe"));
    }

    // ========== Tests for getFormattedRut ==========
    @Test
    void getFormattedRut_ShouldFormatCorrectly() {
        String result = clientService.getFormattedRut("123456785");

        assertEquals("12.345.678-5", result);
    }

    @Test
    void getFormattedRut_ShouldHandleAlreadyFormattedRut() {
        String result = clientService.getFormattedRut("12.345.678-5");

        assertEquals("12.345.678-5", result);
    }

    // ========== Tests for parseFieldErrors ==========
    @Test
    void parseFieldErrors_ShouldParseNameError() {
        Map<String, String> result = clientService.parseFieldErrors("El nombre del cliente es requerido");

        assertTrue(result.containsKey("name"));
        assertTrue(result.get("name").contains("nombre"));
    }

    @Test
    void parseFieldErrors_ShouldParseRutError() {
        Map<String, String> result = clientService.parseFieldErrors("RUT chileno inválido");

        assertTrue(result.containsKey("rut"));
        assertTrue(result.get("rut").contains("RUT"));
    }

    @Test
    void parseFieldErrors_ShouldParsePhoneError() {
        Map<String, String> result = clientService.parseFieldErrors("Número de teléfono inválido");

        assertTrue(result.containsKey("phone"));
        assertTrue(result.get("phone").contains("teléfono"));
    }

    @Test
    void parseFieldErrors_ShouldParseEmailError() {
        Map<String, String> result = clientService.parseFieldErrors("Formato de email inválido");

        assertTrue(result.containsKey("email"));
        assertTrue(result.get("email").contains("email"));
    }

    @Test
    void parseFieldErrors_ShouldReturnGeneral_WhenCannotIdentifyField() {
        Map<String, String> result = clientService.parseFieldErrors("Error desconocido");

        assertTrue(result.containsKey("general"));
    }

    @Test
    void parseFieldErrors_ShouldHandleNullMessage() {
        Map<String, String> result = clientService.parseFieldErrors(null);

        assertTrue(result.containsKey("general"));
        assertEquals("Error desconocido", result.get("general"));
    }

    @Test
    void parseFieldErrors_ShouldHandleEmptyMessage() {
        Map<String, String> result = clientService.parseFieldErrors("   ");

        assertTrue(result.containsKey("general"));
    }

    // ========== Edge case tests - RUT validation ==========

    @Test
    void saveClient_ShouldRejectRutWithInvalidCheckDigit() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        // RUT Inválido a propósito
        newClient.setRut("12.345.678-0");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT chileno inválido"));
    }

    @Test
    void saveClient_ShouldRejectRutTooShort() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("123-4");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT chileno inválido"));
    }

    @Test
    void saveClient_ShouldRejectRutTooLong() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("123.456.789.0-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT chileno inválido"));
    }

    @Test
    void saveClient_ShouldRejectRutWithLetters() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("12.ABC.678-9");
        newClient.setPhone("+56911111111");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("RUT chileno inválido"));
    }

    // ========== Edge case tests - Phone validation ==========
    @Test
    void saveClient_ShouldAcceptPhoneWithCountryCode() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56912345678");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> clientService.saveClient(newClient));
    }

    @Test
    void saveClient_ShouldAcceptPhoneWithoutCountryCode() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("912345678");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);
            // Verificar que se agregó el código de país
            assertTrue(saved.getPhone().startsWith("+56"));
            return saved;
        });

        clientService.saveClient(newClient);
    }

    @Test
    void saveClient_ShouldAcceptPhoneWithSpacesAndDashes() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("9 1234-5678");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> clientService.saveClient(newClient));
    }

    @Test
    void saveClient_ShouldAcceptLandlinePhoneRegional() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("32234567");
        newClient.setEmail("pedro.soto@example.com");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> clientService.saveClient(newClient));
    }

    @Test
    void saveClient_ShouldRejectPhoneNotStartingWith9() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("812345678"); // No empieza con 9
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("teléfono chileno inválido"));
    }

    @Test
    void saveClient_ShouldRejectPhoneTooShort() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("91234");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("teléfono chileno inválido"));
    }

    @Test
    void saveClient_ShouldRejectPhoneTooLong() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("91234567890");
        newClient.setEmail("pedro.soto@example.com");

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("teléfono chileno inválido"));
    }

    // ========== Edge case tests - Email validation ==========
    @Test
    void saveClient_ShouldAcceptValidEmailFormats() throws Exception {
        String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user_name@example.com",
                "123@example.com"
        };

        for (String email : validEmails) {
            ClientEntity newClient = new ClientEntity();
            newClient.setName("Pedro Soto");
            newClient.setRut("11.111.111-1");
            newClient.setPhone("+56911111111");
            newClient.setEmail(email);

            when(clientRepository.existsByRut(anyString())).thenReturn(false);
            when(clientRepository.existsByEmail(anyString())).thenReturn(false);
            when(clientRepository.findAll()).thenReturn(List.of());
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> clientService.saveClient(newClient),
                    "Should accept email: " + email);
        }
    }

    @Test
    void saveClient_ShouldRejectInvalidEmailFormats() {
        String[] invalidEmails = {
                "email",
                "@example.com",
                "email@",
                "email@.com",
                "email@example",
                "email @example.com" // email con espacio
        };

        for (String email : invalidEmails) {
            ClientEntity newClient = new ClientEntity();
            newClient.setName("Pedro Soto");
            newClient.setRut("11.111.111-1");
            newClient.setPhone("+56911111111");
            newClient.setEmail(email);

            Exception exception = assertThrows(
                    Exception.class,
                    () -> clientService.saveClient(newClient),
                    "Should reject email: " + email
            );

            assertTrue(exception.getMessage().contains("email inválido") ||
                    exception.getMessage().contains("email del cliente es requerido"));
        }
    }

    @Test
    void saveClient_ShouldConvertEmailToLowercase() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("Pedro Soto");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("+56911111111");
        newClient.setEmail("PEDRO.SOTO@EXAMPLE.COM");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);
            assertEquals("pedro.soto@example.com", saved.getEmail());
            return saved;
        });

        clientService.saveClient(newClient);
    }

    // ========== Integration-like tests ==========
    @Test
    void saveClient_ShouldHandleCompleteWorkflow() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("  Pedro Soto  ");
        newClient.setRut("11.111.111-1");
        newClient.setPhone("911111111");
        newClient.setEmail("PEDRO.SOTO@EXAMPLE.COM");

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);
            saved.setId(1L);

            // Verificar normalización completa
            assertEquals("Pedro Soto", saved.getName());
            assertEquals("111111111", saved.getRut());
            assertEquals("+56911111111", saved.getPhone());
            assertEquals("pedro.soto@example.com", saved.getEmail());
            assertEquals(ClientStatus.ACTIVE, saved.getStatus());

            return saved;
        });

        ClientEntity result = clientService.saveClient(newClient);

        assertNotNull(result);
        assertNotNull(result.getId());
        verify(clientRepository, times(1)).save(any(ClientEntity.class));
    }


    @Test
    void getClientByRut_ShouldHandleDifferentFormats() {
        String[] rutFormats = {
                "12.345.678-5",
                "12345678-5",
                "123456785"
        };

        for (String rut : rutFormats) {
            when(clientRepository.findByRut("123456785")).thenReturn(testClient);

            ClientEntity result = clientService.getClientByRut(rut);

            assertNotNull(result, "Should find client with RUT format: " + rut);
            assertEquals("Juan Pérez", result.getName());
        }
    }

    @Test
    void existsByRut_ShouldHandleDifferentFormats() {
        String[] rutFormats = {
                "12.345.678-5",
                "12345678-5",
                "123456785"
        };

        when(clientRepository.existsByRut("123456785")).thenReturn(true);

        for (String rut : rutFormats) {
            boolean result = clientService.existsByRut(rut);

            assertTrue(result, "Should recognize RUT format: " + rut);
        }
    }

    @Test
    void saveClient_ShouldHandleTrimming() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setName("   Pedro   Soto   ");
        newClient.setRut("  11.111.111-1  ");
        newClient.setPhone("  +56911111111  ");
        newClient.setEmail("pedro.soto@example.com"); // Sin espacios en el email

        when(clientRepository.existsByRut(anyString())).thenReturn(false);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);

            // Verificar que se eliminaron espacios
            assertFalse(saved.getName().startsWith(" "));
            assertFalse(saved.getName().endsWith(" "));
            assertFalse(saved.getEmail().contains(" "));

            return saved;
        });

        clientService.saveClient(newClient);

        verify(clientRepository, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void getFormattedRut_ShouldHandleShortRut() {
        String result = clientService.getFormattedRut("12345");

        // Si el RUT es muy corto, debería devolverlo sin formato o manejarlo apropiadamente
        assertNotNull(result);
    }

    @Test
    void getFormattedRut_ShouldHandleNullRut() {
        String result = clientService.getFormattedRut(null);

        assertNull(result);
    }

    @Test
    void changeClientStatus_ShouldToggleBetweenStatuses() throws Exception {
        // Cambiar de ACTIVE a RESTRICTED
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(testClient);

        clientService.changeClientStatus(1L, ClientStatus.RESTRICTED);
        assertEquals(ClientStatus.RESTRICTED, testClient.getStatus());

        // Cambiar de RESTRICTED a ACTIVE
        clientService.changeClientStatus(1L, ClientStatus.ACTIVE);
        assertEquals(ClientStatus.ACTIVE, testClient.getStatus());

        verify(clientRepository, times(2)).save(testClient);
    }

    @Test
    void deleteClient_ShouldHandleRepositoryException() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        doThrow(new RuntimeException("Database error")).when(clientRepository).deleteById(1L);

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.deleteClient(1L)
        );

        assertTrue(exception.getMessage().contains("Error al eliminar cliente"));
    }

    @Test
    void saveClient_ShouldValidateAllFieldsBeforeCheckingUniqueness() {
        ClientEntity newClient = new ClientEntity();
        newClient.setName(null); // Esto debería fallar primero
        newClient.setRut("12.345.678-5"); // RUT que ya existe
        newClient.setPhone("+56911111111");
        newClient.setEmail("test@example.com");

        // No mockeamos existsByRut porque debería fallar antes

        Exception exception = assertThrows(
                Exception.class,
                () -> clientService.saveClient(newClient)
        );

        assertTrue(exception.getMessage().contains("nombre"));
        verify(clientRepository, never()).existsByRut(anyString());
    }
}