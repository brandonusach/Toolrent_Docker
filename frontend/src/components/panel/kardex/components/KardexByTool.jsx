import React, { useState, useEffect } from 'react';
import { Package, Search, CheckCircle, AlertCircle, TrendingUp, TrendingDown, ArrowUp, ArrowDown, RotateCcw, Minus, Plus, Wrench } from 'lucide-react';
import { useKardex } from '../hooks/useKardex';
import { formatDateTime } from '../../../../utils/dateUtils';

const KardexByTool = ({ tools, selectedTool, onSelectTool, onViewDetail }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [toolMovements, setToolMovements] = useState([]);
    const [loading, setLoading] = useState(false);
    const [auditReport, setAuditReport] = useState(null);

    const { getMovementsByTool, verifyStockConsistency, generateAuditReport } = useKardex();

    // Filter tools based on search
    const filteredTools = tools.filter(tool =>
        tool.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        tool.category?.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    // Load movements for selected tool
    useEffect(() => {
        if (selectedTool) {
            loadToolMovements();
            loadAuditReport();
        }
    }, [selectedTool]);

    const loadToolMovements = async () => {
        if (!selectedTool) return;

        setLoading(true);
        try {
            const movements = await getMovementsByTool(selectedTool.id);
            setToolMovements(movements);
        } catch (error) {
            setToolMovements([]);
        } finally {
            setLoading(false);
        }
    };

    const loadAuditReport = async () => {
        if (!selectedTool) return;

        try {
            const report = await generateAuditReport(selectedTool.id);
            setAuditReport(report);
        } catch (error) {
            setAuditReport(null);
        }
    };

    const checkConsistency = async () => {
        if (!selectedTool) return;

        try {
            await verifyStockConsistency(selectedTool.id);
            await loadAuditReport(); // Reload to get updated consistency status
        } catch (error) {
        }
    };


    const getToolStatusBadge = (status) => {
        const badges = {
            AVAILABLE: 'bg-green-500/10 text-green-400 border-green-500/30',
            LOANED: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
            IN_REPAIR: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
            DECOMMISSIONED: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
            PARTIALLY_AVAILABLE: 'bg-orange-500/10 text-orange-400 border-orange-500/30'
        };
        return badges[status] || 'bg-slate-500/10 text-slate-400 border-slate-500/30';
    };

    const getToolStatusLabel = (status) => {
        const labels = {
            AVAILABLE: 'Disponible',
            LOANED: 'Prestada',
            IN_REPAIR: 'En Reparación',
            DECOMMISSIONED: 'Dada de Baja',
            PARTIALLY_AVAILABLE: 'Parcial'
        };
        return labels[status] || status;
    };

    const getMovementIcon = (type) => {
        switch (type) {
            case 'INITIAL_STOCK':
                return <Plus className="w-5 h-5 text-blue-400" />;
            case 'LOAN':
                return <ArrowDown className="w-5 h-5 text-red-400" />;
            case 'RETURN':
                return <ArrowUp className="w-5 h-5 text-green-400" />;
            case 'REPAIR':
                return <Wrench className="w-5 h-5 text-yellow-400" />;
            case 'DECOMMISSION':
                return <Minus className="w-5 h-5 text-slate-400" />;
            case 'RESTOCK':
                return <Plus className="w-5 h-5 text-purple-400" />;
            default:
                return <Package className="w-5 h-5 text-slate-400" />;
        }
    };

    const getMovementLabel = (type) => {
        const labels = {
            INITIAL_STOCK: 'Stock Inicial',
            LOAN: 'Préstamo',
            RETURN: 'Devolución',
            REPAIR: 'Reparación',
            DECOMMISSION: 'Baja',
            RESTOCK: 'Reabastecimiento'
        };
        return labels[type] || type;
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Tools List */}
            <div className="lg:col-span-1">
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50">
                    <div className="p-4 border-b border-slate-700/50">
                        <h3 className="text-lg font-medium text-slate-100 mb-3">
                            Seleccionar Herramienta
                        </h3>
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-500" />
                            <input
                                type="text"
                                placeholder="Buscar herramienta..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-orange-500"
                            />
                        </div>
                    </div>

                    <div className="max-h-96 overflow-y-auto">
                        {filteredTools.map((tool) => (
                            <button
                                key={tool.id}
                                onClick={() => onSelectTool(tool)}
                                className={`w-full p-4 text-left hover:bg-slate-700/30 border-b border-slate-700/50 transition-colors ${
                                    selectedTool?.id === tool.id ? 'bg-blue-500/20 border-blue-500/50' : ''
                                }`}
                            >
                                <div className="flex items-center justify-between">
                                    <div>
                                        <h4 className="font-medium text-slate-100">{tool.name}</h4>
                                        <p className="text-sm text-slate-400">{tool.category?.name}</p>
                                        <div className="flex items-center gap-2 mt-1">
                                            <span className="text-xs text-slate-500">
                                                Stock: {tool.currentStock}/{tool.initialStock}
                                            </span>
                                            <span className={`text-xs px-2 py-1 rounded ${
                                                tool.status === 'AVAILABLE'
                                                    ? 'bg-green-500/20 text-green-400'
                                                    : 'bg-yellow-500/20 text-yellow-400'
                                            }`}>
                                                {tool.status}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Tool Details and Movements */}
            <div className="lg:col-span-2 space-y-6">
                {selectedTool ? (
                    <>
                        {/* Tool Summary */}
                        <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                            <div className="mb-4">
                                <h3 className="text-xl font-bold text-slate-100">
                                    {selectedTool.name}
                                </h3>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="bg-slate-700/30 p-4 rounded-lg">
                                    <h4 className="text-sm text-slate-400 mb-1">Stock Actual</h4>
                                    <p className="text-2xl font-bold text-slate-100">
                                        {selectedTool.currentStock}
                                    </p>
                                    <p className="text-xs text-slate-500">
                                        de {selectedTool.initialStock} inicial
                                    </p>
                                </div>

                                <div className="bg-slate-700/30 p-4 rounded-lg">
                                    <h4 className="text-sm text-slate-400 mb-1">Total Movimientos</h4>
                                    <p className="text-2xl font-bold text-slate-100">
                                        {toolMovements.length}
                                    </p>
                                </div>

                                <div className="bg-slate-700/30 p-4 rounded-lg">
                                    <h4 className="text-sm text-slate-400 mb-1">Categoría</h4>
                                    <p className="text-lg font-medium text-slate-100">
                                        {selectedTool.category?.name || 'Sin categoría'}
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Movements List */}
                        <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50">
                            <div className="p-4 border-b border-slate-700/50">
                                <h3 className="text-lg font-medium text-slate-100">
                                    Historial de Movimientos
                                </h3>
                            </div>

                            {loading ? (
                                <div className="p-8 text-center">
                                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                                    <p className="text-slate-400">Cargando movimientos...</p>
                                </div>
                            ) : toolMovements.length === 0 ? (
                                <div className="p-8 text-center">
                                    <Package className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                                    <p className="text-slate-400">No hay movimientos registrados</p>
                                </div>
                            ) : (
                                <div className="max-h-96 overflow-y-auto">
                                    {toolMovements.map((movement) => (
                                        <div
                                            key={movement.id}
                                            className="p-4 border-b border-slate-700/50 hover:bg-slate-700/20 cursor-pointer transition-colors"
                                            onClick={() => onViewDetail(movement)}
                                        >
                                            <div className="flex items-center justify-between">
                                                <div className="flex items-center gap-3">
                                                    {getMovementIcon(movement.type)}
                                                    <div>
                                                        <h4 className="font-medium text-slate-100 mb-1">
                                                            {getMovementLabel(movement.type)}
                                                        </h4>
                                                        <p className="text-sm text-slate-400">
                                                            {formatDateTime(movement.createdAt)}
                                                        </p>
                                                        {movement.description && (
                                                            <p className={`text-xs mt-1 ${movement.type === 'DECOMMISSION' ? 'text-red-400 font-medium' : 'text-slate-500'}`}>
                                                                {movement.description}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>

                                                <div className="text-right">
                                                    <div className="flex items-center gap-2">
                                                        <span className="text-sm text-slate-300">
                                                            Cant: {movement.quantity}
                                                        </span>
                                                        <span className="text-sm text-slate-500">
                                                            Stock: {movement.stockBefore} → {movement.stockAfter}
                                                        </span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-8 text-center">
                        <Package className="w-16 h-16 text-slate-500 mx-auto mb-4" />
                        <h3 className="text-lg font-medium text-slate-300 mb-2">
                            Selecciona una herramienta
                        </h3>
                        <p className="text-slate-500">
                            Elige una herramienta de la lista para ver su historial de movimientos
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default KardexByTool;
