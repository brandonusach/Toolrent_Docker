// loans/components/FineManagement.jsx - Gestión completa de multas
import React, { useState, useEffect } from 'react';
import {
    X,
    DollarSign,
    AlertTriangle,
    CheckCircle,
    Calendar,
    User,
    FileText,
    CreditCard,
    Loader,
    Search
} from 'lucide-react';
import { useFines } from '../hooks/useFines';
import httpClient from "../../../../http-common";

const FineManagement = ({ onClose, onSuccess }) => {
    const { loadUnpaidFines, payFine, unpaidFines, loading } = useFines();
    const [clients, setClients] = useState([]);
    const [selectedClientId, setSelectedClientId] = useState('');
    const [clientFines, setClientFines] = useState([]);
    const [loadingClient, setLoadingClient] = useState(false);
    const [payingFineId, setPayingFineId] = useState(null);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        loadClients();
        loadUnpaidFines();
    }, []);

    useEffect(() => {
        if (selectedClientId) {
            loadClientFines(selectedClientId);
        } else {
            setClientFines([]);
        }
    }, [selectedClientId]);

    const loadClients = async () => {
        try {
            const response = await httpClient.get('/api/v1/clients/');
            setClients(response.data || []);
        } catch (err) {
            console.error('Error loading clients:', err);
            setError('Error al cargar clientes');
        }
    };

    const loadClientFines = async (clientId) => {
        setLoadingClient(true);
        setError('');
        try {
            const response = await httpClient.get(`/api/v1/fines/client/${clientId}`);
            const fines = response.data || [];
            setClientFines(fines.filter(f => !f.paid));
        } catch (err) {
            console.error('Error loading client fines:', err);
            setError('Error al cargar multas del cliente');
            setClientFines([]);
        } finally {
            setLoadingClient(false);
        }
    };

    const handlePayFine = async (fineId) => {
        setPayingFineId(fineId);
        setError('');
        setSuccess('');
        try {
            await payFine(fineId);
            setSuccess('Multa pagada exitosamente');

            // Recargar las multas del cliente
            if (selectedClientId) {
                await loadClientFines(selectedClientId);
            }
            await loadUnpaidFines();

            // Notificar éxito después de un momento
            setTimeout(() => {
                if (onSuccess) onSuccess();
            }, 1500);
        } catch (err) {
            console.error('Error paying fine:', err);
            setError(err.message || 'Error al pagar la multa');
        } finally {
            setPayingFineId(null);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const getFineTypeLabel = (type) => {
        const types = {
            'LATE_RETURN': 'Devolución Tardía',
            'DAMAGE_REPAIR': 'Reparación por Daño',
            'TOOL_REPLACEMENT': 'Reposición de Herramienta'
        };
        return types[type] || type;
    };

    const getFineTypeColor = (type) => {
        const colors = {
            'LATE_RETURN': 'text-orange-400',
            'DAMAGE_REPAIR': 'text-yellow-400',
            'TOOL_REPLACEMENT': 'text-red-400'
        };
        return colors[type] || 'text-gray-400';
    };

    const calculateTotalUnpaid = (fines) => {
        return fines.reduce((sum, fine) => sum + (fine.amount || 0), 0);
    };

    // Filtrar clientes por término de búsqueda
    const filteredClients = clients.filter(client => {
        if (!searchTerm) return true;
        const search = searchTerm.toLowerCase();
        return (
            client.firstName?.toLowerCase().includes(search) ||
            client.lastName?.toLowerCase().includes(search) ||
            client.email?.toLowerCase().includes(search) ||
            client.phone?.toLowerCase().includes(search)
        );
    });

    // Clientes con multas pendientes
    const clientsWithFines = clients.filter(client => {
        return unpaidFines.some(fine => fine.client?.id === client.id);
    });

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-start justify-center p-4 pt-8 z-50 overflow-y-auto">
            <div className="bg-gray-800 rounded-lg shadow-xl max-w-6xl w-full mb-8 max-h-[calc(100vh-4rem)]">
                <div className="max-h-[calc(100vh-4rem)] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-gray-700 sticky top-0 bg-gray-800 z-10">
                    <div>
                        <h2 className="text-2xl font-bold text-white mb-1">
                            <DollarSign className="inline h-6 w-6 mr-2" />
                            Gestión de Multas
                        </h2>
                        <p className="text-gray-400 text-sm">
                            Consulta y paga multas pendientes de los clientes
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white transition-colors"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                <div className="p-6 space-y-6">
                    {/* Estadísticas Rápidas */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-gray-700 rounded-lg p-4">
                        <div className="text-center">
                            <div className="text-3xl font-bold text-red-400">
                                {unpaidFines.length}
                            </div>
                            <div className="text-sm text-gray-300">Multas Pendientes</div>
                        </div>
                        <div className="text-center">
                            <div className="text-3xl font-bold text-orange-400">
                                {clientsWithFines.length}
                            </div>
                            <div className="text-sm text-gray-300">Clientes con Multas</div>
                        </div>
                        <div className="text-center">
                            <div className="text-3xl font-bold text-yellow-400">
                                ${calculateTotalUnpaid(unpaidFines).toFixed(2)}
                            </div>
                            <div className="text-sm text-gray-300">Total Pendiente</div>
                        </div>
                    </div>

                    {/* Mensajes */}
                    {error && (
                        <div className="bg-red-900 border border-red-700 text-red-200 p-4 rounded-lg flex items-center">
                            <AlertTriangle className="h-5 w-5 mr-2 flex-shrink-0" />
                            {error}
                        </div>
                    )}

                    {success && (
                        <div className="bg-green-900 border border-green-700 text-green-200 p-4 rounded-lg flex items-center">
                            <CheckCircle className="h-5 w-5 mr-2 flex-shrink-0" />
                            {success}
                        </div>
                    )}

                    {/* Selector de Cliente */}
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">
                            <User className="h-4 w-4 inline mr-1" />
                            Seleccionar Cliente
                        </label>
                        <div className="space-y-2">
                            {/* Buscador */}
                            <div className="relative">
                                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                                <input
                                    type="text"
                                    placeholder="Buscar por nombre, email o teléfono..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="w-full pl-10 pr-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-500"
                                />
                            </div>

                            {/* Selector */}
                            <select
                                value={selectedClientId}
                                onChange={(e) => setSelectedClientId(e.target.value)}
                                className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                            >
                                <option value="">-- Selecciona un cliente --</option>
                                {filteredClients.map(client => {
                                    const hasFines = unpaidFines.some(fine => fine.client?.id === client.id);
                                    return (
                                        <option key={client.id} value={client.id}>
                                            {client.firstName} {client.lastName} - {client.email}
                                            {hasFines ? ' ⚠️ Tiene multas' : ''}
                                        </option>
                                    );
                                })}
                            </select>
                        </div>
                    </div>

                    {/* Loading */}
                    {loadingClient && (
                        <div className="flex items-center justify-center py-8">
                            <Loader className="h-8 w-8 animate-spin text-orange-500" />
                            <span className="ml-2 text-gray-300">Cargando multas...</span>
                        </div>
                    )}

                    {/* Lista de Multas del Cliente */}
                    {selectedClientId && !loadingClient && (
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h3 className="text-lg font-semibold text-white">
                                    Multas Pendientes
                                </h3>
                                {clientFines.length > 0 && (
                                    <div className="text-sm text-gray-300">
                                        Total: <span className="font-bold text-yellow-400">
                                            ${calculateTotalUnpaid(clientFines).toFixed(2)}
                                        </span>
                                    </div>
                                )}
                            </div>

                            {clientFines.length === 0 ? (
                                <div className="bg-gray-700 rounded-lg p-8 text-center">
                                    <CheckCircle className="h-12 w-12 text-green-400 mx-auto mb-3" />
                                    <p className="text-gray-300">Este cliente no tiene multas pendientes</p>
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    {clientFines.map(fine => (
                                        <div
                                            key={fine.id}
                                            className="bg-gray-700 border border-gray-600 rounded-lg p-4"
                                        >
                                            <div className="flex items-start justify-between">
                                                <div className="flex-1 space-y-2">
                                                    {/* Tipo y Monto */}
                                                    <div className="flex items-center space-x-3">
                                                        <span className={`font-bold text-lg ${getFineTypeColor(fine.type)}`}>
                                                            {getFineTypeLabel(fine.type)}
                                                        </span>
                                                        <span className="text-2xl font-bold text-yellow-400">
                                                            ${(fine.amount || 0).toFixed(2)}
                                                        </span>
                                                    </div>

                                                    {/* Descripción */}
                                                    {fine.description && (
                                                        <div className="flex items-start text-gray-300">
                                                            <FileText className="h-4 w-4 mr-2 mt-0.5 flex-shrink-0" />
                                                            <span className="text-sm">{fine.description}</span>
                                                        </div>
                                                    )}

                                                    {/* Fecha de Emisión */}
                                                    <div className="flex items-center text-gray-400 text-sm">
                                                        <Calendar className="h-4 w-4 mr-2" />
                                                        Emitida: {formatDate(fine.issueDate)}
                                                    </div>

                                                    {/* Préstamo Relacionado */}
                                                    {fine.loan && (
                                                        <div className="flex items-center text-gray-400 text-sm">
                                                            <FileText className="h-4 w-4 mr-2" />
                                                            Préstamo ID: {fine.loan.id}
                                                        </div>
                                                    )}
                                                </div>

                                                {/* Botón de Pago */}
                                                <button
                                                    onClick={() => handlePayFine(fine.id)}
                                                    disabled={payingFineId === fine.id}
                                                    className="ml-4 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                                                >
                                                    {payingFineId === fine.id ? (
                                                        <>
                                                            <Loader className="h-4 w-4 mr-2 animate-spin" />
                                                            Pagando...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <CreditCard className="h-4 w-4 mr-2" />
                                                            Pagar
                                                        </>
                                                    )}
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Vista General de Todos los Clientes con Multas */}
                    {!selectedClientId && !loading && (
                        <div className="space-y-4">
                            <h3 className="text-lg font-semibold text-white">
                                Clientes con Multas Pendientes
                            </h3>
                            {clientsWithFines.length === 0 ? (
                                <div className="bg-gray-700 rounded-lg p-8 text-center">
                                    <CheckCircle className="h-12 w-12 text-green-400 mx-auto mb-3" />
                                    <p className="text-gray-300">No hay multas pendientes</p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    {clientsWithFines.map(client => {
                                        const clientUnpaidFines = unpaidFines.filter(
                                            fine => fine.client?.id === client.id
                                        );
                                        const totalAmount = calculateTotalUnpaid(clientUnpaidFines);

                                        return (
                                            <div
                                                key={client.id}
                                                className="bg-gray-700 border border-gray-600 rounded-lg p-4 hover:border-orange-500 transition-colors cursor-pointer"
                                                onClick={() => setSelectedClientId(client.id.toString())}
                                            >
                                                <div className="flex items-center justify-between mb-2">
                                                    <div className="font-semibold text-white">
                                                        {client.firstName} {client.lastName}
                                                    </div>
                                                    <span className="text-xs px-2 py-1 bg-red-900 text-red-200 rounded">
                                                        {clientUnpaidFines.length} multa(s)
                                                    </span>
                                                </div>
                                                <div className="text-sm text-gray-400 mb-2">
                                                    {client.email}
                                                </div>
                                                <div className="text-xl font-bold text-yellow-400">
                                                    ${totalAmount.toFixed(2)}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="flex justify-end p-6 border-t border-gray-700">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
                    >
                        Cerrar
                    </button>
                </div>
                </div>
            </div>
        </div>
    );
};

export default FineManagement;

