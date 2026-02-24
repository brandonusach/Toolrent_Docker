// inventory/components/StockManager.jsx - PURE VERSION
import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, RefreshCw } from 'lucide-react';

const StockManager = ({ tool, type, onUpdateStock, onClose, onSuccess }) => {
    const [quantity, setQuantity] = useState(1);
    const [selectedInstanceId, setSelectedInstanceId] = useState('');
    const [instances, setInstances] = useState([]);
    const [loadingInstances, setLoadingInstances] = useState(false);
    const [loading, setLoading] = useState(false);
    const [serverErrors, setServerErrors] = useState({});

    const isAddStock = type === 'add-stock';
    const API_BASE = 'http://localhost:8081/api';

    // Cargar instancias disponibles al dar de baja
    useEffect(() => {
        if (!isAddStock && tool?.id) {
            loadInstances();
        }
    }, [tool, isAddStock]);

    const loadInstances = async () => {
        setLoadingInstances(true);
        try {
            const response = await fetch(`${API_BASE}/tool-instances/tool/${tool.id}`);
            if (response.ok) {
                const data = await response.json();
                // Filtrar solo las disponibles para dar de baja (no las que ya están dadas de baja)
                const availableInstances = Array.isArray(data)
                    ? data.filter(inst => inst.status !== 'DECOMMISSIONED')
                    : [];
                setInstances(availableInstances);
            } else {
                setInstances([]);
            }
        } catch (error) {
            console.error('Error loading instances:', error);
            setInstances([]);
        } finally {
            setLoadingInstances(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setServerErrors({});

        // Validar si es dar de baja que se haya seleccionado una instancia
        if (!isAddStock && !selectedInstanceId) {
            alert('Debe seleccionar una herramienta específica para dar de baja');
            return;
        }

        setLoading(true);

        try {
            if (isAddStock) {
                // Agregar stock con cantidad
                await onUpdateStock(tool.id, quantity);
                alert(`${quantity} unidad(es) agregadas exitosamente`);
            } else {
                // Dar de baja: enviar el instanceId específico seleccionado
                await onUpdateStock(tool.id, parseInt(selectedInstanceId));
                alert('Herramienta dada de baja exitosamente');
            }

            onSuccess();
        } catch (error) {
            console.error('Error updating stock:', error);

            // Handle server errors
            if (error.response && error.response.data) {
                const errorData = error.response.data;

                if (typeof errorData === 'object' && errorData.fieldErrors) {
                    setServerErrors(errorData.fieldErrors);
                } else if (typeof errorData === 'string') {
                    alert(`Error: ${errorData}`);
                } else {
                    alert('Error: Unknown server error');
                }
            } else {
                alert(`Error: ${error.message || 'Error al actualizar el stock'}`);
            }
        } finally {
            setLoading(false);
        }
    };

    const getTitle = () => {
        return isAddStock ? 'Agregar Stock' : 'Dar de Baja';
    };

    const getIcon = () => {
        return isAddStock ? TrendingUp : TrendingDown;
    };

    const getButtonClass = () => {
        return isAddStock
            ? 'bg-green-600 hover:bg-green-700'
            : 'bg-orange-600 hover:bg-orange-700';
    };

    const getActionText = () => {
        return isAddStock ? 'Agregar Stock' : 'Dar de Baja';
    };

    const Icon = getIcon();

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-gray-800 rounded-lg p-6 w-full max-w-md">
                <div className="flex items-center mb-4">
                    <Icon className="h-6 w-6 text-white mr-2" />
                    <h3 className="text-xl font-bold text-white">{getTitle()}</h3>
                </div>

                {/* Tool Information */}
                <div className="mb-6 p-4 bg-gray-700 rounded-lg">
                    <p className="text-gray-300 mb-2">
                        <span className="font-semibold">Herramienta:</span>
                        <span className="text-white ml-2">{tool.name}</span>
                    </p>
                    <p className="text-gray-300 mb-2">
                        <span className="font-semibold">Stock actual:</span>
                        <span className="text-white ml-2">{tool.currentStock || 0} unidades</span>
                    </p>
                    {/* Remove client-side warnings - let backend handle business logic */}
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {isAddStock ? (
                        /* Quantity Field for Adding Stock */
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Cantidad a agregar *
                            </label>
                            <input
                                type="number"
                                min="1"
                                value={quantity}
                                onChange={(e) => {
                                    setQuantity(parseInt(e.target.value) || 1);
                                    setServerErrors({}); // Clear errors on change
                                }}
                                className={`w-full px-3 py-2 bg-gray-700 border rounded-lg text-white focus:outline-none ${
                                    serverErrors.quantity
                                        ? 'border-red-500 focus:border-red-400'
                                        : 'border-gray-600 focus:border-orange-500'
                                }`}
                                disabled={loading}
                            />
                            {serverErrors.quantity && (
                                <p className="text-red-400 text-xs mt-1">{serverErrors.quantity}</p>
                            )}
                        </div>
                    ) : (
                        /* Instance Selector for Decommissioning */
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Seleccionar herramienta específica a dar de baja *
                            </label>
                            {loadingInstances ? (
                                <div className="flex items-center justify-center py-4 text-gray-400">
                                    <RefreshCw className="h-4 w-4 animate-spin mr-2" />
                                    Cargando herramientas...
                                </div>
                            ) : instances.length === 0 ? (
                                <div className="text-center py-4 text-gray-400">
                                    No hay herramientas disponibles para dar de baja
                                </div>
                            ) : (
                                <select
                                    value={selectedInstanceId}
                                    onChange={(e) => {
                                        setSelectedInstanceId(e.target.value);
                                        setServerErrors({});
                                    }}
                                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-orange-500"
                                    disabled={loading}
                                    required
                                >
                                    <option value="">Seleccione una herramienta...</option>
                                    {instances.map((instance) => (
                                        <option key={instance.id} value={instance.id}>
                                            ID #{instance.id} - Estado: {
                                                instance.status === 'AVAILABLE' ? 'Disponible' :
                                                instance.status === 'LOANED' ? 'Prestada' :
                                                instance.status === 'UNDER_REPAIR' ? 'En Reparación' :
                                                instance.status
                                            }
                                        </option>
                                    ))}
                                </select>
                            )}
                            {serverErrors.instanceId && (
                                <p className="text-red-400 text-xs mt-1">{serverErrors.instanceId}</p>
                            )}
                        </div>
                    )}

                    {/* Buttons */}
                    <div className="flex space-x-3">
                        <button
                            type="submit"
                            disabled={loading || (!isAddStock && instances.length === 0)}
                            className={`flex-1 text-white py-2 px-4 rounded-lg transition-colors flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed ${getButtonClass()}`}
                        >
                            <Icon className="h-4 w-4 mr-2" />
                            {loading ? 'Procesando...' : getActionText()}
                        </button>
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={loading}
                            className="flex-1 bg-gray-600 text-white py-2 px-4 rounded-lg hover:bg-gray-700 transition-colors disabled:opacity-50"
                        >
                            Cancelar
                        </button>
                    </div>
                </form>

                {/* Informational Note */}
                <div className="mt-4 p-3 bg-gray-700 rounded-lg">
                    <p className="text-xs text-gray-400">
                        {isAddStock
                            ? 'Esta acción agregará nuevas herramientas al inventario.'
                            : 'Esta acción dará de baja la herramienta específica seleccionada (se eliminará del stock disponible).'
                        }
                    </p>
                </div>
            </div>
        </div>
    );
};

export default StockManager;