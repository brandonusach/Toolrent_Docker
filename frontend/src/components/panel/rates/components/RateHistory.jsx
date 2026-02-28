import React, { useState, useEffect } from 'react';
import { X, Calendar, DollarSign, Calculator, Settings, Clock, CheckCircle, XCircle } from 'lucide-react';
import { RATE_TYPES, RATE_TYPE_CONFIG, formatCurrency, formatDate } from '../utils/rateConstants';

const RateHistory = ({
                         rateType = null,
                         isOpen = false,
                         onClose,
                         onGetHistory,
                         rates = {}
                     }) => {
    const [historyData, setHistoryData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedType, setSelectedType] = useState(rateType);
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortOrder, setSortOrder] = useState('desc');
    const [filterActive, setFilterActive] = useState('all'); // all, active, inactive

    // Cargar historial cuando se abra el modal o cambie el tipo
    useEffect(() => {
        if (isOpen && selectedType) {
            loadHistory();
        }
    }, [isOpen, selectedType]);

    const loadHistory = async () => {
        setLoading(true);
        try {
            const history = await onGetHistory(selectedType);
            setHistoryData(history || []);
        } catch (error) {
            setHistoryData([]);
        } finally {
            setLoading(false);
        }
    };

    // Filtrar y ordenar datos
    const getFilteredAndSortedData = () => {
        let filtered = [...historyData];

        // Filtrar por estado activo
        if (filterActive === 'active') {
            filtered = filtered.filter(rate => rate.active);
        } else if (filterActive === 'inactive') {
            filtered = filtered.filter(rate => !rate.active);
        }

        // Ordenar
        filtered.sort((a, b) => {
            let aValue = a[sortBy];
            let bValue = b[sortBy];

            // Convertir fechas para ordenar
            if (sortBy.includes('Date') || sortBy === 'createdAt') {
                aValue = new Date(aValue);
                bValue = new Date(bValue);
            }

            if (sortOrder === 'asc') {
                return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
            } else {
                return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
            }
        });

        return filtered;
    };

    // Obtener configuración del tipo de tarifa
    const getTypeConfig = (type) => {
        return RATE_TYPE_CONFIG[type] || {
            label: 'Tarifa',
            color: 'gray',
            icon: 'DollarSign'
        };
    };

    // Obtener icono del tipo
    const getTypeIcon = (type) => {
        const config = getTypeConfig(type);
        switch (config.icon) {
            case 'Calculator':
                return Calculator;
            case 'Settings':
                return Settings;
            default:
                return DollarSign;
        }
    };

    // Formatear período de vigencia
    const formatValidityPeriod = (rate) => {
        const start = formatDate(rate.effectiveFrom);
        const end = rate.effectiveTo ? formatDate(rate.effectiveTo) : 'Indefinido';
        return `${start} - ${end}`;
    };

    // Verificar si la tarifa está actualmente vigente
    const isCurrentlyActive = (rate) => {
        const today = new Date();
        const effectiveFrom = new Date(rate.effectiveFrom);
        const effectiveTo = rate.effectiveTo ? new Date(rate.effectiveTo) : null;

        return rate.active &&
            today >= effectiveFrom &&
            (!effectiveTo || today <= effectiveTo);
    };

    // Obtener estado visual de la tarifa
    const getRateStatus = (rate) => {
        if (!rate.active) {
            return { label: 'Inactiva', color: 'gray', icon: XCircle };
        }
        if (isCurrentlyActive(rate)) {
            return { label: 'Vigente', color: 'green', icon: CheckCircle };
        }
        return { label: 'Programada', color: 'blue', icon: Clock };
    };

    if (!isOpen) return null;

    const typeConfig = selectedType ? getTypeConfig(selectedType) : null;
    const TypeIcon = selectedType ? getTypeIcon(selectedType) : DollarSign;
    const filteredData = getFilteredAndSortedData();

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-slate-800 rounded-xl border border-slate-700/50 w-full max-w-6xl max-h-[90vh] overflow-hidden">
                {/* Header */}
                <div className="p-6 border-b border-slate-700/50">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            {typeConfig && (
                                <div className={`bg-${typeConfig.color}-500/20 p-2 rounded-lg`}>
                                    <TypeIcon className={`text-${typeConfig.color}-400`} size={24} />
                                </div>
                            )}
                            <div>
                                <h2 className="text-xl font-semibold text-white">
                                    Historial de {typeConfig ? typeConfig.label : 'Tarifas'}
                                </h2>
                                <p className="text-slate-400 text-sm">
                                    Registro histórico de cambios y configuraciones
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={onClose}
                            className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                        >
                            <X size={20} />
                        </button>
                    </div>
                </div>

                {/* Controles */}
                <div className="p-4 border-b border-slate-700/50 bg-slate-750">
                    <div className="flex flex-wrap items-center gap-4">
                        {/* Selector de tipo */}
                        {!rateType && (
                            <div className="flex items-center gap-2">
                                <label className="text-sm font-medium text-slate-300">Tipo:</label>
                                <select
                                    value={selectedType || ''}
                                    onChange={(e) => setSelectedType(e.target.value)}
                                    className="bg-slate-700 border border-slate-600 rounded px-3 py-1.5 text-white text-sm focus:outline-none focus:border-blue-500"
                                >
                                    <option value="">Seleccionar tipo</option>
                                    {Object.entries(RATE_TYPES).map(([key, value]) => (
                                        <option key={key} value={value}>
                                            {RATE_TYPE_CONFIG[value].label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}

                        {/* Filtro por estado */}
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-medium text-slate-300">Estado:</label>
                            <select
                                value={filterActive}
                                onChange={(e) => setFilterActive(e.target.value)}
                                className="bg-slate-700 border border-slate-600 rounded px-3 py-1.5 text-white text-sm focus:outline-none focus:border-blue-500"
                            >
                                <option value="all">Todas</option>
                                <option value="active">Solo Activas</option>
                                <option value="inactive">Solo Inactivas</option>
                            </select>
                        </div>

                        {/* Ordenar por */}
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-medium text-slate-300">Ordenar:</label>
                            <select
                                value={sortBy}
                                onChange={(e) => setSortBy(e.target.value)}
                                className="bg-slate-700 border border-slate-600 rounded px-3 py-1.5 text-white text-sm focus:outline-none focus:border-blue-500"
                            >
                                <option value="createdAt">Fecha Creación</option>
                                <option value="effectiveFrom">Fecha Inicio</option>
                                <option value="dailyAmount">Monto</option>
                            </select>
                            <button
                                onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                                className="px-2 py-1.5 text-slate-400 hover:text-white border border-slate-600 hover:border-slate-500 rounded text-sm transition-colors"
                            >
                                {sortOrder === 'asc' ? '↑' : '↓'}
                            </button>
                        </div>

                        {/* Botón actualizar */}
                        <button
                            onClick={loadHistory}
                            disabled={loading || !selectedType}
                            className="ml-auto px-4 py-2 bg-orange-600 hover:bg-orange-700 disabled:bg-slate-600 text-white rounded text-sm transition-colors"
                        >
                            {loading ? 'Cargando...' : 'Actualizar'}
                        </button>
                    </div>
                </div>

                {/* Contenido */}
                <div className="p-6 overflow-y-auto max-h-96">
                    {loading ? (
                        <div className="flex items-center justify-center py-12">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
                            <span className="ml-3 text-slate-400">Cargando historial...</span>
                        </div>
                    ) : !selectedType ? (
                        <div className="text-center py-8 text-slate-400">
                            <Calendar className="mx-auto h-12 w-12 mb-2" />
                            <p>Selecciona un tipo de tarifa para ver su historial</p>
                        </div>
                    ) : filteredData.length === 0 ? (
                        <div className="text-center py-8 text-slate-400">
                            <Calendar className="mx-auto h-12 w-12 mb-2" />
                            <p>No hay registros en el historial</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {filteredData.map((rate) => {
                                const status = getRateStatus(rate);
                                const StatusIcon = status.icon;

                                return (
                                    <div
                                        key={rate.id}
                                        className={`p-4 rounded-lg border ${
                                            isCurrentlyActive(rate)
                                                ? `bg-${typeConfig.color}-500/5 border-${typeConfig.color}-500/30`
                                                : 'bg-slate-750 border-slate-600'
                                        }`}
                                    >
                                        <div className="flex items-start justify-between">
                                            <div className="flex-1">
                                                <div className="flex items-center gap-3 mb-2">
                                                    <span className="text-xl font-bold text-white">
                                                        {selectedType === RATE_TYPES.REPAIR_RATE
                                                            ? `${rate.dailyAmount}%`
                                                            : formatCurrency(rate.dailyAmount)
                                                        }
                                                    </span>
                                                    <div className={`flex items-center gap-1 px-2 py-1 bg-${status.color}-500/20 text-${status.color}-400 text-xs rounded-full`}>
                                                        <StatusIcon size={12} />
                                                        <span>{status.label}</span>
                                                    </div>
                                                    {isCurrentlyActive(rate) && (
                                                        <span className="px-2 py-1 bg-yellow-500/20 text-yellow-400 text-xs rounded-full">
                                                            Actual
                                                        </span>
                                                    )}
                                                </div>

                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                                                    <div>
                                                        <p className="text-slate-400 mb-1">Período de Vigencia:</p>
                                                        <p className="text-slate-300">
                                                            <Calendar size={14} className="inline mr-1" />
                                                            {formatValidityPeriod(rate)}
                                                        </p>
                                                    </div>

                                                    <div>
                                                        <p className="text-slate-400 mb-1">Fecha de Creación:</p>
                                                        <p className="text-slate-300">
                                                            <Clock size={14} className="inline mr-1" />
                                                            {formatDate(rate.createdAt)}
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>

                                            <div className="text-right">
                                                <p className="text-xs text-slate-500">ID: {rate.id}</p>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Footer con estadísticas */}
                {filteredData.length > 0 && (
                    <div className="p-4 border-t border-slate-700/50 bg-slate-750">
                        <div className="flex justify-between items-center text-sm text-slate-400">
                            <span>
                                Mostrando {filteredData.length} de {historyData.length} registros
                            </span>
                            <div className="flex gap-4">
                                <span>
                                    Activas: {historyData.filter(r => r.active).length}
                                </span>
                                <span>
                                    Inactivas: {historyData.filter(r => !r.active).length}
                                </span>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default RateHistory;

