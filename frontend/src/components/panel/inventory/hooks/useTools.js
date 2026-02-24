// hooks/useTools.js - Siguiendo exactamente el patrón del profesor
import { useState, useCallback } from 'react';
import httpClient from "../../../../http-common";

export const useTools = () => {
    const [tools, setTools] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Load all tools
    const loadTools = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await httpClient.get('/api/v1/tools/');
            setTools(response.data || []);
        } catch (err) {
            console.error('Error loading tools:', err);
            setError(err.message);
            setTools([]);
        } finally {
            setLoading(false);
        }
    }, []);

    // Create new tool - igual que profesor con create()
    const createTool = useCallback(async (toolData) => {
        try {
            const response = await httpClient.post('/api/v1/tools/', toolData);
            const newTool = response.data;

            setTools(prevTools => [...prevTools, newTool]);
            return newTool;
        } catch (err) {
            console.error('Error creating tool:', err);
            throw new Error(err.message || 'Error al crear la herramienta');
        }
    }, []);

    // Update tool - igual que profesor con update() - envía objeto completo
    const updateTool = useCallback(async (toolId, toolData) => {
        try {
            // Patrón del profesor: PUT sin ID en URL, objeto completo con ID
            const toolWithId = { ...toolData, id: toolId };
            const response = await httpClient.put('/api/v1/tools/', toolWithId);
            const updatedTool = response.data;

            setTools(prevTools =>
                prevTools.map(tool =>
                    tool.id === toolId ? updatedTool : tool
                )
            );
            return updatedTool;
        } catch (err) {
            console.error('Error updating tool:', err);
            throw new Error(err.message || 'Error al actualizar la herramienta');
        }
    }, []);

    // Delete tool - igual que profesor con remove()
    const deleteTool = useCallback(async (toolId) => {
        try {
            await httpClient.delete(`/api/v1/tools/${toolId}`);
            setTools(prevTools => prevTools.filter(tool => tool.id !== toolId));
            return true;
        } catch (err) {
            console.error('Error deleting tool:', err);
            throw new Error(err.message || 'Error al eliminar la herramienta');
        }
    }, []);

    // Add stock - específico de tu negocio
    const updateStock = useCallback(async (toolId, quantity) => {
        try {
            const response = await httpClient.post(`/api/v1/tools/${toolId}/add-stock`, null, {
                params: { quantity }
            });
            const updatedTool = response.data;

            setTools(prevTools =>
                prevTools.map(tool =>
                    tool.id === toolId ? updatedTool : tool
                )
            );
            return updatedTool;
        } catch (err) {
            console.error('Error adding stock:', err);
            const error = new Error(err.message || 'Error al agregar stock');
            error.response = { data: err.response?.data || err.message };
            throw error;
        }
    }, []);

    // Decommission tool - específico de tu negocio
    const decommissionTool = useCallback(async (toolId, instanceId) => {
        try {
            const response = await httpClient.put(`/api/v1/tools/${toolId}/decommission`, null, {
                params: { instanceId }
            });
            const updatedTool = response.data;

            setTools(prevTools =>
                prevTools.map(tool =>
                    tool.id === toolId ? updatedTool : tool
                )
            );
            return updatedTool;
        } catch (err) {
            console.error('Error decommissioning tool:', err);
            const error = new Error(err.message || 'Error al dar de baja');
            error.response = { data: err.response?.data || err.message };
            throw error;
        }
    }, []);

    // Get tool by ID
    const getToolById = useCallback((toolId) => {
        return tools.find(tool => tool.id === parseInt(toolId));
    }, [tools]);

    // Filter tools
    const filterTools = useCallback((searchTerm, categoryFilter) => {
        return tools.filter(tool => {
            const matchesSearch = !searchTerm ||
                tool.name.toLowerCase().includes(searchTerm.toLowerCase());
            const matchesCategory = categoryFilter === 'ALL' ||
                (tool.category && tool.category.id.toString() === categoryFilter);

            return matchesSearch && matchesCategory;
        });
    }, [tools]);

    return {
        // State
        tools,
        loading,
        error,

        // CRUD operations - nombres iguales al profesor
        loadTools,
        createTool,
        updateTool,
        deleteTool,

        // Stock operations
        updateStock,
        decommissionTool,

        // Utilities
        getToolById,
        filterTools
    };
};