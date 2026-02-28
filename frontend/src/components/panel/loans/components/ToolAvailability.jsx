// loans/components/ToolAvailability.jsx - Verificar disponibilidad de herramienta
import React, { useState, useEffect } from 'react';
import {
    Package2,
    CheckCircle,
    XCircle,
    AlertTriangle,
    Loader,
    Info,
    Wrench,
    User
} from 'lucide-react';
import httpClient from "../../../../http-common";

const ToolAvailability = ({ toolId, quantity = 1, clientId, onAvailabilityChange }) => {
    const [availability, setAvailability] = useState(null);
    const [clientToolCheck, setClientToolCheck] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (toolId) {
            validateToolAvailability();
        } else {
            setAvailability(null);
            setClientToolCheck(null);
            if (onAvailabilityChange) {
                onAvailabilityChange(null);
            }
        }
    }, [toolId, quantity, clientId]);

    const validateToolAvailability = async () => {
        setLoading(true);
        setError('');

        try {
            // Verificar disponibilidad de la herramienta
            const toolResponse = await httpClient.get(`/api/v1/loans/tool/${toolId}/availability`, {
                params: { quantity }
            });
            const toolAvailability = toolResponse.data;
            setAvailability(toolAvailability);

            let clientCheck = null;
            // Si hay un cliente seleccionado, verificar si ya tiene préstamo de esta herramienta
            if (clientId && toolAvailability.available) {
                const clientResponse = await httpClient.get(`/api/v1/loans/client/${clientId}/tool/${toolId}/check`);
                clientCheck = clientResponse.data;
                setClientToolCheck(clientCheck);
            }

            // Determinar disponibilidad final
            const finalAvailability = {
                ...toolAvailability,
                clientCheck,
                finallyAvailable: toolAvailability.available && (!clientCheck || !clientCheck.hasActiveLoanForTool)
            };

            if (onAvailabilityChange) {
                onAvailabilityChange(finalAvailability);
            }

        } catch (err) {
            setError(err.response?.data || err.message || 'Error al validar la disponibilidad');
        } finally {
            setLoading(false);
        }
    };

    const getStatusIcon = () => {
        if (loading) return <Loader className="h-5 w-5 animate-spin text-blue-400" />;
        if (error) return <XCircle className="h-5 w-5 text-red-400" />;
        if (!availability) return <Info className="h-5 w-5 text-slate-400" />;

        if (availability.available && (!clientToolCheck || !clientToolCheck.hasActiveLoanForTool)) {
            return <CheckCircle className="h-5 w-5 text-green-400" />;
        } else {
            return <XCircle className="h-5 w-5 text-red-400" />;
        }
    };

    const getStatusText = () => {
        if (loading) return 'Verificando disponibilidad...';
        if (error) return 'Error en la verificación';
        if (!availability) return 'Seleccione una herramienta';

        if (!availability.available) {
            if (availability.issueType === 'TOOL_STATUS') {
                return 'Herramienta no disponible';
            } else if (availability.issueType === 'INSUFFICIENT_STOCK') {
                return 'Stock insuficiente';
            } else {
                return 'No disponible';
            }
        }

        if (clientToolCheck && clientToolCheck.hasActiveLoanForTool) {
            return 'Cliente ya tiene préstamo de esta herramienta';
        }

        return 'Disponible para préstamo';
    };

    const getStatusColor = () => {
        if (loading) return 'border-blue-500 bg-blue-900/30';
        if (error) return 'border-red-500 bg-red-900/30';
        if (!availability) return 'border-slate-500 bg-slate-700';

        if (availability.available && (!clientToolCheck || !clientToolCheck.hasActiveLoanForTool)) {
            return 'border-green-500 bg-green-900';
        } else {
            return 'border-red-500 bg-red-900/30';
        }
    };

    if (!toolId) {
        return (
            <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                <div className="flex items-center text-slate-400">
                    <Package2 className="h-5 w-5 mr-2" />
                    <span>Seleccione una herramienta para verificar disponibilidad</span>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* Estado principal de disponibilidad */}
            <div className={`rounded-lg p-4 border-2 ${getStatusColor()}`}>
                <div className="flex items-center justify-between">
                    <div className="flex items-center">
                        {getStatusIcon()}
                        <div className="ml-3">
                            <h3 className="text-lg font-medium text-white">
                                Estado de Disponibilidad
                            </h3>
                            <p className="text-slate-300">{getStatusText()}</p>
                        </div>
                    </div>

                    {availability && (
                        <div className="text-right">
                            <div className="text-2xl font-bold text-white">
                                {availability.currentStock}
                            </div>
                            <div className="text-sm text-slate-400">unidades</div>
                        </div>
                    )}
                </div>
            </div>

            {/* Error details */}
            {error && (
                <div className="bg-red-900 border border-red-700 rounded-lg p-4">
                    <div className="flex items-center">
                        <AlertTriangle className="h-5 w-5 text-red-400 mr-2" />
                        <span className="text-red-200">{error}</span>
                    </div>
                </div>
            )}

            {/* Detalles de disponibilidad */}
            {availability && (
                <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                    <h4 className="text-white font-medium mb-3 flex items-center">
                        <Wrench className="h-4 w-4 mr-2" />
                        Información de la Herramienta
                    </h4>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                        <div>
                            <span className="text-slate-400">Nombre:</span>
                            <p className="text-white font-medium">{availability.toolName || 'N/A'}</p>
                        </div>

                        <div>
                            <span className="text-slate-400">Estado:</span>
                            <p className={`font-medium ${
                                availability.toolStatus === 'AVAILABLE'
                                    ? 'text-green-400'
                                    : 'text-red-400'
                            }`}>
                                {availability.toolStatus || 'N/A'}
                            </p>
                        </div>

                        <div>
                            <span className="text-slate-400">Stock actual:</span>
                            <p className="text-white font-medium">{availability.currentStock || 0}</p>
                        </div>

                        {availability.maxAvailableQuantity && (
                            <div>
                                <span className="text-slate-400">Máximo disponible:</span>
                                <p className="text-white font-medium">{availability.maxAvailableQuantity}</p>
                            </div>
                        )}

                        {availability.issue && (
                            <div className="md:col-span-2">
                                <span className="text-slate-400">Problema:</span>
                                <p className="text-red-400 text-sm">{availability.issue}</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Verificación del cliente */}
            {clientToolCheck && (
                <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                    <h4 className="text-white font-medium mb-3 flex items-center">
                        <User className="h-4 w-4 mr-2" />
                        Verificación del Cliente
                    </h4>

                    <div className="space-y-3">
                        <div className="flex items-center justify-between">
                            <span className="text-slate-400">¿Tiene préstamo activo de esta herramienta?</span>
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                clientToolCheck.hasActiveLoanForTool
                                    ? 'bg-red-900 text-red-200'
                                    : 'bg-green-900 text-green-200'
                            }`}>
                                {clientToolCheck.hasActiveLoanForTool ? 'SÍ' : 'NO'}
                            </span>
                        </div>

                        <div className="flex items-center justify-between">
                            <span className="text-slate-400">¿Puede solicitar esta herramienta?</span>
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                clientToolCheck.canLoanThisTool
                                    ? 'bg-green-900 text-green-200'
                                    : 'bg-red-900 text-red-200'
                            }`}>
                                {clientToolCheck.canLoanThisTool ? 'SÍ' : 'NO'}
                            </span>
                        </div>

                        {clientToolCheck.hasActiveLoanForTool && clientToolCheck.activeLoanId && (
                            <div className="bg-red-900 border border-red-700 rounded-lg p-3 mt-3">
                                <div className="flex items-center text-red-200 mb-2">
                                    <AlertTriangle className="h-4 w-4 mr-2" />
                                    <span className="font-medium">⛔ No Permitido - Préstamo Activo Existente</span>
                                </div>
                                <div className="text-sm text-red-300 space-y-1">
                                    <div className="mb-2 p-2 bg-red-800 border border-red-600 rounded">
                                        <strong>Un cliente no puede tener más de 1 unidad de la misma herramienta en préstamo simultáneamente.</strong>
                                    </div>
                                    <div>
                                        <span className="font-medium">ID del préstamo actual:</span> #{clientToolCheck.activeLoanId}
                                    </div>
                                    <div>
                                        <span className="font-medium">Fecha de préstamo:</span> {
                                        new Date(clientToolCheck.loanDate).toLocaleDateString('es-ES')
                                    }
                                    </div>
                                    <div>
                                        <span className="font-medium">Fecha acordada de devolución:</span> {
                                        new Date(clientToolCheck.agreedReturnDate).toLocaleDateString('es-ES')
                                    }
                                    </div>
                                </div>
                                <div className="mt-2 text-xs text-red-200 italic">
                                    💡 El cliente debe devolver esta herramienta antes de poder solicitar un nuevo préstamo de la misma.
                                </div>
                            </div>
                        )}

                        <div className="text-xs text-slate-400 mt-2">
                            {clientToolCheck.message}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ToolAvailability;
