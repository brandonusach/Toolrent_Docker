package com.toolrent.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolrent.backend.entities.ClientEntity;
import com.toolrent.backend.services.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ClientEntity testClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
        objectMapper = new ObjectMapper();

        testClient = new ClientEntity();
        testClient.setId(1L);
        testClient.setRut("12345678-9");
        testClient.setName("Juan Pérez");
        testClient.setEmail("juan@example.com");
        testClient.setPhone("912345678");
        testClient.setStatus(ClientEntity.ClientStatus.ACTIVE);
    }

    // ========== Tests for GET /api/v1/clients/ ==========
    @Test
    void listClients_ShouldReturnAllClients() throws Exception {
        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setRut("98765432-1");
        client2.setName("María González");
        client2.setEmail("maria@example.com");
        client2.setPhone("987654321");
        client2.setStatus(ClientEntity.ClientStatus.ACTIVE);

        List<ClientEntity> clients = Arrays.asList(testClient, client2);
        when(clientService.getAllClients()).thenReturn(clients);

        mockMvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rut").value("12345678-9"))
                .andExpect(jsonPath("$[1].rut").value("98765432-1"));

        verify(clientService, times(1)).getAllClients();
    }

    // ========== Tests for GET /api/v1/clients/{id} ==========
    @Test
    void getClientById_ShouldReturnClient_WhenExists() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(testClient);

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rut").value("12345678-9"))
                .andExpect(jsonPath("$.name").value("Juan Pérez"));

        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    void getClientById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(clientService.getClientById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/clients/999"))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).getClientById(999L);
    }

    // ========== Tests for POST /api/v1/clients/ ==========
    @Test
    void saveClient_ShouldCreateAndReturnClient_WhenValid() throws Exception {
        ClientEntity newClient = new ClientEntity();
        newClient.setRut("11111111-1");
        newClient.setName("Pedro Sánchez");
        newClient.setEmail("pedro@example.com");
        newClient.setPhone("911111111");

        ClientEntity savedClient = new ClientEntity();
        savedClient.setId(3L);
        savedClient.setRut("11111111-1");
        savedClient.setName("Pedro Sánchez");
        savedClient.setEmail("pedro@example.com");
        savedClient.setPhone("911111111");
        savedClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        when(clientService.saveClient(any(ClientEntity.class))).thenReturn(savedClient);

        mockMvc.perform(post("/api/v1/clients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.rut").value("11111111-1"))
                .andExpect(jsonPath("$.name").value("Pedro Sánchez"));

        verify(clientService, times(1)).saveClient(any(ClientEntity.class));
    }

    @Test
    void saveClient_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        ClientEntity invalidClient = new ClientEntity();
        invalidClient.setRut("invalid-rut");

        when(clientService.saveClient(any(ClientEntity.class)))
                .thenThrow(new RuntimeException("Validation error"));

        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("rut", "RUT inválido");
        when(clientService.parseFieldErrors(anyString())).thenReturn(fieldErrors);

        mockMvc.perform(post("/api/v1/clients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidClient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rut").value("RUT inválido"));

        verify(clientService, times(1)).saveClient(any(ClientEntity.class));
        verify(clientService, times(1)).parseFieldErrors(anyString());
    }

    // ========== Tests for PUT /api/v1/clients/ ==========
    @Test
    void updateClient_ShouldUpdateAndReturnClient() throws Exception {
        ClientEntity updatedClient = new ClientEntity();
        updatedClient.setId(1L);
        updatedClient.setRut("12345678-9");
        updatedClient.setName("Juan Pérez Actualizado");
        updatedClient.setEmail("juan.nuevo@example.com");
        updatedClient.setPhone("912345678");
        updatedClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        when(clientService.updateClient(any(ClientEntity.class))).thenReturn(updatedClient);

        mockMvc.perform(put("/api/v1/clients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Juan Pérez Actualizado"))
                .andExpect(jsonPath("$.email").value("juan.nuevo@example.com"));

        verify(clientService, times(1)).updateClient(any(ClientEntity.class));
    }

    // ========== Tests for DELETE /api/v1/clients/{id} ==========
    @Test
    void deleteClientById_ShouldReturnNoContent_WhenDeleted() throws Exception {
        when(clientService.deleteClient(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/clients/1"))
                .andExpect(status().isNoContent());

        verify(clientService, times(1)).deleteClient(1L);
    }

    @Test
    void deleteClientById_ShouldReturnBadRequest_WhenDeletionFails() throws Exception {
        when(clientService.deleteClient(999L))
                .thenThrow(new RuntimeException("Cannot delete client"));

        mockMvc.perform(delete("/api/v1/clients/999"))
                .andExpect(status().isBadRequest());

        verify(clientService, times(1)).deleteClient(999L);
    }

    // ========== Tests for GET /api/v1/clients/rut/{rut} ==========
    @Test
    void getClientByRut_ShouldReturnClient_WhenExists() throws Exception {
        when(clientService.getClientByRut("12345678-9")).thenReturn(testClient);

        mockMvc.perform(get("/api/v1/clients/rut/12345678-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value("12345678-9"))
                .andExpect(jsonPath("$.name").value("Juan Pérez"));

        verify(clientService, times(1)).getClientByRut("12345678-9");
    }

    @Test
    void getClientByRut_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(clientService.getClientByRut("99999999-9")).thenReturn(null);

        mockMvc.perform(get("/api/v1/clients/rut/99999999-9"))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).getClientByRut("99999999-9");
    }

    // ========== Tests for GET /api/v1/clients/exists/{rut} ==========
    @Test
    void existsByRut_ShouldReturnTrue_WhenExists() throws Exception {
        when(clientService.existsByRut("12345678-9")).thenReturn(true);

        mockMvc.perform(get("/api/v1/clients/exists/12345678-9"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(clientService, times(1)).existsByRut("12345678-9");
    }

    @Test
    void existsByRut_ShouldReturnFalse_WhenNotExists() throws Exception {
        when(clientService.existsByRut("99999999-9")).thenReturn(false);

        mockMvc.perform(get("/api/v1/clients/exists/99999999-9"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(clientService, times(1)).existsByRut("99999999-9");
    }

    // ========== Tests for PUT /api/v1/clients/{id}/status ==========
    @Test
    void updateClientStatus_ShouldUpdateAndReturnClient() throws Exception {
        ClientEntity updatedClient = new ClientEntity();
        updatedClient.setId(1L);
        updatedClient.setRut("12345678-9");
        updatedClient.setName("Juan Pérez");
        updatedClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);

        when(clientService.changeClientStatus(1L, ClientEntity.ClientStatus.RESTRICTED))
                .thenReturn(updatedClient);

        mockMvc.perform(put("/api/v1/clients/1/status")
                        .param("status", "RESTRICTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESTRICTED"));

        verify(clientService, times(1))
                .changeClientStatus(1L, ClientEntity.ClientStatus.RESTRICTED);
    }

    @Test
    void updateClientStatus_ShouldReturnBadRequest_WhenFails() throws Exception {
        when(clientService.changeClientStatus(999L, ClientEntity.ClientStatus.RESTRICTED))
                .thenThrow(new RuntimeException("Client not found"));

        mockMvc.perform(put("/api/v1/clients/999/status")
                        .param("status", "RESTRICTED"))
                .andExpect(status().isBadRequest());

        verify(clientService, times(1))
                .changeClientStatus(999L, ClientEntity.ClientStatus.RESTRICTED);
    }

    // ========== Tests adicionales para aumentar cobertura ==========
    @Test
    void listClients_ShouldReturnEmptyList_WhenNoClients() throws Exception {
        when(clientService.getAllClients()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(clientService, times(1)).getAllClients();
    }

    @Test
    void updateClient_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        ClientEntity updatedClient = new ClientEntity();
        updatedClient.setId(1L);
        updatedClient.setRut("invalid");

        when(clientService.updateClient(any(ClientEntity.class)))
                .thenThrow(new RuntimeException("Validation error"));

        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("rut", "RUT inválido");
        when(clientService.parseFieldErrors(anyString())).thenReturn(fieldErrors);

        mockMvc.perform(put("/api/v1/clients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedClient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rut").value("RUT inválido"));

        verify(clientService, times(1)).updateClient(any(ClientEntity.class));
    }

    @Test
    void updateClientStatus_ShouldChangeToActive() throws Exception {
        ClientEntity updatedClient = new ClientEntity();
        updatedClient.setId(1L);
        updatedClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        when(clientService.changeClientStatus(1L, ClientEntity.ClientStatus.ACTIVE))
                .thenReturn(updatedClient);

        mockMvc.perform(put("/api/v1/clients/1/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(clientService, times(1))
                .changeClientStatus(1L, ClientEntity.ClientStatus.ACTIVE);
    }

    @Test
    void updateClientStatus_ShouldChangeFromRestrictedToActive() throws Exception {
        ClientEntity restrictedClient = new ClientEntity();
        restrictedClient.setId(2L);
        restrictedClient.setStatus(ClientEntity.ClientStatus.RESTRICTED);

        ClientEntity updatedClient = new ClientEntity();
        updatedClient.setId(2L);
        updatedClient.setStatus(ClientEntity.ClientStatus.ACTIVE);

        when(clientService.changeClientStatus(2L, ClientEntity.ClientStatus.ACTIVE))
                .thenReturn(updatedClient);

        mockMvc.perform(put("/api/v1/clients/2/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(clientService, times(1))
                .changeClientStatus(2L, ClientEntity.ClientStatus.ACTIVE);
    }
}

