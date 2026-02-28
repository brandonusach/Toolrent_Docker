// hooks/useClients.js - Siguiendo exactamente el patrón del profesor
import { useState, useCallback } from 'react';
import httpClient from "../../../../http-common";

export const useClients = () => {
    const [clients, setClients] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Load all clients
    const loadClients = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await httpClient.get('/api/v1/clients/');
            setClients(response.data || []);
        } catch (err) {
            console.error('Error loading clients:', err);
            setError(err.message);
            setClients([]);
        } finally {
            setLoading(false);
        }
    }, []);

    // Create new client - igual que profesor con create()
    const createClient = useCallback(async (clientData) => {
        try {
            const response = await httpClient.post('/api/v1/clients/', clientData);
            const newClient = response.data;
            setClients(prevClients => [...prevClients, newClient]);
            return newClient;
        } catch (err) {
            // Propagar el error original para que ClientForm pueda acceder a error.response.data
            throw err;
        }
    }, []);

    // Update client - igual que profesor con update() - envía objeto completo
    const updateClient = useCallback(async (clientId, clientData) => {
        try {
            // Patrón del profesor: PUT sin ID en URL, objeto completo con ID
            const clientWithId = { ...clientData, id: clientId };
            const response = await httpClient.put('/api/v1/clients/', clientWithId);
            const updatedClient = response.data;

            setClients(prevClients =>
                prevClients.map(client =>
                    client.id === clientId ? updatedClient : client
                )
            );
            return updatedClient;
        } catch (err) {
            console.error('Error updating client:', err);
            // Propagar el error original para que ClientForm pueda acceder a error.response.data
            throw err;
        }
    }, []);

    // Delete client - igual que profesor con remove()
    const deleteClient = useCallback(async (clientId) => {
        try {
            await httpClient.delete(`/api/v1/clients/${clientId}`);
            setClients(prevClients => prevClients.filter(client => client.id !== clientId));
            return true;
        } catch (err) {
            throw err;
        }
    }, []);

    // Change client status - específico de tu negocio
    const updateClientStatus = useCallback(async (clientId, status) => {
        try {
            const response = await httpClient.put(`/api/v1/clients/${clientId}/status`, null, {
                params: { status }
            });
            const updatedClient = response.data;

            setClients(prevClients =>
                prevClients.map(client =>
                    client.id === clientId ? updatedClient : client
                )
            );
            return updatedClient;
        } catch (err) {
            console.error('Error updating client status:', err);
            const error = new Error(err.message || 'Error al cambiar estado del cliente');
            error.response = { data: err.response?.data || err.message };
            throw error;
        }
    }, []);

    // Get client by ID
    const getClientById = useCallback((clientId) => {
        return clients.find(client => client.id === parseInt(clientId));
    }, [clients]);

    // Get client by RUT - específico de tu negocio
    const getClientByRut = useCallback(async (rut) => {
        try {
            const response = await httpClient.get(`/api/v1/clients/rut/${encodeURIComponent(rut)}`);
            return response.data;
        } catch (err) {
            if (err.response?.status === 404) {
                return null;
            }
            console.error('Error getting client by RUT:', err);
            throw new Error(err.message || 'Error al buscar cliente por RUT');
        }
    }, []);

    // Check if client exists by RUT
    const existsByRut = useCallback(async (rut) => {
        try {
            const response = await httpClient.get(`/api/v1/clients/exists/${encodeURIComponent(rut)}`);
            return response.data;
        } catch (err) {
            console.error('Error checking RUT existence:', err);
            return false;
        }
    }, []);

    // Filter clients
    const filterClients = useCallback((searchTerm, statusFilter) => {
        return clients.filter(client => {
            const matchesSearch = !searchTerm ||
                client.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                client.rut.toLowerCase().includes(searchTerm.toLowerCase()) ||
                client.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
                client.phone.toLowerCase().includes(searchTerm.toLowerCase());

            const matchesStatus = statusFilter === 'ALL' ||
                client.status === statusFilter;

            return matchesSearch && matchesStatus;
        });
    }, [clients]);

    return {
        // State
        clients,
        loading,
        error,

        // CRUD operations - nombres iguales al profesor
        loadClients,
        createClient,
        updateClient,
        deleteClient,

        // Client specific operations
        updateClientStatus,
        getClientByRut,
        existsByRut,

        // Utilities
        getClientById,
        filterClients
    };
};