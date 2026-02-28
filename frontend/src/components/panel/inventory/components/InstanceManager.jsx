// inventory/components/InstanceManager.jsx - PURE VERSION with Repair Functionality
import React, { useState, useEffect } from 'react';
import { X, Package, AlertTriangle, RefreshCw, Wrench, CheckCircle } from 'lucide-react';

const InstanceManager = ({ tool, onClose }) => {
    const [instances, setInstances] = useState([]);
    const [loading, setLoading] = useState(true);

    const API_BASE = 'http://localhost:8081/api';

    useEffect(() => {
        if (tool?.id) {
            loadInstances();
        }
    }, [tool]);

    // Handle overlay click to close modal
    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    // Handle escape key to close
    useEffect(() => {
        const handleEscape = (e) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };

        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, [onClose]);

    const loadInstances = async () => {
        setLoading(true);
        try {
            const response = await fetch(`${API_BASE}/tool-instances/tool/${tool.id}`);
            if (response.ok) {
                const data = await response.json();
                setInstances(Array.isArray(data) ? data : []);
            } else {
                setInstances([]);
                if (response.status !== 404) {
                    alert('Error al cargar instancias de herramientas');
                }
            }
        } catch (error) {
            setInstances([]);
            alert('Error de conexión al cargar instancias');
        } finally {
            setLoading(false);
        }
    };

    const handleRepairInstance = async (instanceId) => {
        if (!confirm('¿Está seguro de marcar esta instancia como reparada? Esto la hará disponible nuevamente.')) {
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/tool-instances/${instanceId}/repair`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                alert('✅ Instancia reparada exitosamente. La herramienta está disponible nuevamente.');
                loadInstances(); // Reload instances to reflect changes
            } else {
                const errorData = await response.text();
                alert(`❌ Error al reparar instancia: ${errorData}`);
            }
        } catch (error) {
            alert('❌ Error de conexión al reparar instancia');
        }
    };

    // Simple status display functions (no business logic)
    const getStatusColor = (status) => {
        const colors = {
            'AVAILABLE': 'bg-green-900 text-green-300',
            'LOANED': 'bg-blue-900 text-blue-300',
            'UNDER_REPAIR': 'bg-yellow-900 text-yellow-300',
            'DECOMMISSIONED': 'bg-red-900 text-red-300'
        };
        return colors[status] || 'bg-slate-900 text-slate-300';
    };

    const getStatusText = (status) => {
        const labels = {
            'AVAILABLE': 'Disponible',
            'LOANED': 'Prestada',
            'UNDER_REPAIR': 'En Reparación',
            'DECOMMISSIONED': 'Dada de Baja'
        };
        return labels[status] || status;
    };

    // Simple counting function (no business logic)
    const getStatusCounts = () => {
        const counts = {
            AVAILABLE: 0,
            LOANED: 0,
            UNDER_REPAIR: 0,
            DECOMMISSIONED: 0
        };

        instances.forEach(instance => {
            counts[instance.status] = (counts[instance.status] || 0) + 1;
        });

        return counts;
    };

    const statusCounts = getStatusCounts();

    return (
        <div
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
            onClick={handleOverlayClick}
        >
            <div className="bg-slate-800 rounded-lg p-6 w-full max-w-5xl max-h-[90vh] overflow-hidden relative">
                {/* Header */}
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h3 className="text-2xl font-bold text-white">
                            Herramientas de: {tool?.name}
                        </h3>
                        <p className="text-slate-400 mt-1">
                            Total: {instances.length} herramientas - Stock Actual: {tool?.currentStock || 0}
                        </p>
                    </div>
                    <div className="flex items-center space-x-3">
                        <button
                            onClick={loadInstances}
                            disabled={loading}
                            className="p-2 bg-slate-700 text-white rounded-lg hover:bg-slate-600 transition-colors disabled:opacity-50"
                            title="Actualizar"
                        >
                            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                        </button>
                        <button
                            onClick={onClose}
                            className="text-slate-400 hover:text-white transition-colors p-2 hover:bg-slate-700 rounded-lg z-10"
                            title="Cerrar"
                        >
                            <X className="h-6 w-6" />
                        </button>
                    </div>
                </div>

                {/* Status Summary */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                    <div className="bg-green-900 bg-opacity-30 rounded-lg p-3">
                        <div className="text-green-400 font-bold text-lg">{statusCounts.AVAILABLE}</div>
                        <div className="text-green-300 text-sm">Disponibles</div>
                    </div>
                    <div className="bg-blue-900 bg-opacity-30 rounded-lg p-3">
                        <div className="text-blue-400 font-bold text-lg">{statusCounts.LOANED}</div>
                        <div className="text-blue-300 text-sm">Prestadas</div>
                    </div>
                    <div className="bg-yellow-900 bg-opacity-30 rounded-lg p-3">
                        <div className="text-yellow-400 font-bold text-lg">{statusCounts.UNDER_REPAIR}</div>
                        <div className="text-yellow-300 text-sm">En Reparación</div>
                    </div>
                    <div className="bg-red-900 bg-opacity-30 rounded-lg p-3">
                        <div className="text-red-400 font-bold text-lg">{statusCounts.DECOMMISSIONED}</div>
                        <div className="text-red-300 text-sm">Dadas de Baja</div>
                    </div>
                </div>

                {/* Instance List */}
                <div className="overflow-y-auto" style={{ maxHeight: 'calc(90vh - 300px)' }}>
                    {loading ? (
                        <div className="text-center py-8">
                            <RefreshCw className="h-8 w-8 text-slate-400 mx-auto mb-4 animate-spin" />
                            <p className="text-slate-400">Cargando herramientas...</p>
                        </div>
                    ) : instances.length === 0 ? (
                        <div className="text-center py-12">
                            <Package className="h-12 w-12 text-slate-500 mx-auto mb-4" />
                            <p className="text-slate-400">No hay herramientas registradas</p>
                            <p className="text-slate-500 text-sm mt-2">
                                Las herramientas se crean automáticamente al agregar stock
                            </p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {instances.map((instance) => (
                                <div
                                    key={instance.id}
                                    className="bg-slate-700 rounded-lg p-4 hover:bg-gray-650 transition-colors"
                                >
                                    <div className="flex flex-col lg:flex-row lg:justify-between lg:items-center space-y-3 lg:space-y-0">
                                        {/* Instance Information */}
                                        <div className="flex items-center space-x-4">
                                            <div>
                                                <p className="text-white font-medium">
                                                    ID: #{instance.id}
                                                </p>
                                                <p className="text-slate-400 text-sm">
                                                    Instancia individual de herramienta
                                                </p>
                                            </div>
                                        </div>

                                        {/* Status Display and Actions */}
                                        <div className="flex items-center space-x-3">
                                            <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(instance.status)}`}>
                                                {getStatusText(instance.status)}
                                            </span>

                                            {/* Repair Button - Only for UNDER_REPAIR status */}
                                            {instance.status === 'UNDER_REPAIR' && (
                                                <button
                                                    onClick={() => handleRepairInstance(instance.id)}
                                                    className="flex items-center space-x-1 px-3 py-1 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs font-medium transition-colors"
                                                    title="Marcar como reparada"
                                                >
                                                    <Wrench className="h-3 w-3" />
                                                    <span>Reparar</span>
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {/* Status Warnings */}
                                    {instance.status === 'LOANED' && (
                                        <div className="flex items-center mt-3 p-2 bg-blue-900 bg-opacity-30 rounded">
                                            <AlertTriangle className="h-4 w-4 text-blue-400 mr-2 flex-shrink-0" />
                                            <span className="text-blue-300 text-xs">
                                                Esta instancia está actualmente prestada
                                            </span>
                                        </div>
                                    )}

                                    {instance.status === 'UNDER_REPAIR' && (
                                        <div className="flex items-center mt-3 p-2 bg-yellow-900 bg-opacity-30 rounded">
                                            <Wrench className="h-4 w-4 text-yellow-400 mr-2 flex-shrink-0" />
                                            <span className="text-yellow-300 text-xs">
                                                Esta instancia fue devuelta con daños. Márquela como reparada cuando esté lista.
                                            </span>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="mt-6 pt-4 border-t border-slate-700/50">
                    <div className="flex flex-col sm:flex-row sm:justify-between items-start sm:items-center text-sm text-slate-400 space-y-1 sm:space-y-0">
                        <span>
                            {instances.length > 0 && `Mostrando ${instances.length} herramienta${instances.length !== 1 ? 's' : ''}`}
                        </span>
                        <span>
                            Última actualización: {new Date().toLocaleTimeString()}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InstanceManager;
