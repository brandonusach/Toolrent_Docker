﻿import React, { useState } from 'react';
import { Calendar, Download, BarChart3, TrendingUp, TrendingDown, Package, AlertCircle } from 'lucide-react';
import { useKardex } from '../hooks/useKardex';
import { formatDateTime } from '../../../../utils/dateUtils';
import { getFriendlyError } from '../../../../utils/errorUtils';

const DateRangeReport = ({ onViewDetail }) => {
    const [dateRange, setDateRange] = useState({ startDate: '', endDate: '' });
    const [movements, setMovements] = useState([]);
    const [loading, setLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);
    const [searchError, setSearchError] = useState('');

    const { getMovementsByDateRange } = useKardex();

    const handleDateChange = (field, value) => {
        setDateRange(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const searchMovements = async () => {
        setSearchError('');

        if (!dateRange.startDate || !dateRange.endDate) {
            setSearchError('Debes seleccionar ambas fechas para buscar movimientos.');
            return;
        }

        if (new Date(dateRange.startDate) > new Date(dateRange.endDate)) {
            setSearchError('La fecha de inicio debe ser anterior o igual a la fecha de fin.');
            return;
        }

        setLoading(true);
        try {
            const result = await getMovementsByDateRange(dateRange.startDate, dateRange.endDate);
            setMovements(result);
            setHasSearched(true);
        } catch (error) {
            setSearchError(getFriendlyError(error));
        } finally {
            setLoading(false);
        }
    };

    const clearSearch = () => {
        setDateRange({ startDate: '', endDate: '' });
        setMovements([]);
        setHasSearched(false);
        setSearchError('');
    };

    const getQuickDateRange = (days) => {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(endDate.getDate() - days);

        return {
            startDate: startDate.toISOString().split('T')[0],
            endDate: endDate.toISOString().split('T')[0]
        };
    };

    const setQuickRange = (days) => {
        const range = getQuickDateRange(days);
        setDateRange(range);
    };

    const generateStatistics = () => {
        const stats = {
            total: movements.length,
            byType: {},
            byDay: {},
            stockChanges: {
                increases: 0,
                decreases: 0,
                totalIncrease: 0,
                totalDecrease: 0
            }
        };

        movements.forEach(movement => {
            // Count by type
            stats.byType[movement.type] = (stats.byType[movement.type] || 0) + 1;

            // Count by day
            const day = movement.createdAt.split('T')[0];
            stats.byDay[day] = (stats.byDay[day] || 0) + 1;

            // Stock changes
            const change = movement.stockAfter - movement.stockBefore;
            if (change > 0) {
                stats.stockChanges.increases++;
                stats.stockChanges.totalIncrease += change;
            } else if (change < 0) {
                stats.stockChanges.decreases++;
                stats.stockChanges.totalDecrease += Math.abs(change);
            }
        });

        return stats;
    };


    const getMovementColor = (type) => {
        const colors = {
            INITIAL_STOCK: 'bg-blue-500/20 text-blue-400',
            LOAN: 'bg-red-500/20 text-red-400',
            RETURN: 'bg-green-500/20 text-green-400',
            REPAIR: 'bg-yellow-500/20 text-yellow-400',
            DECOMMISSION: 'bg-slate-500/20 text-slate-400',
            RESTOCK: 'bg-purple-500/20 text-purple-400'
        };
        return colors[type] || 'bg-slate-500/20 text-slate-400';
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

    const stats = hasSearched ? generateStatistics() : null;

    return (
        <div className="space-y-6">
            {/* Date Range Selection */}
            <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                <div className="flex items-center gap-2 mb-4">
                    <Calendar className="w-5 h-5 text-slate-400" />
                    <h3 className="text-lg font-medium text-slate-100">Reporte por Rango de Fechas</h3>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Fecha de inicio
                        </label>
                        <input
                            type="date"
                            value={dateRange.startDate}
                            onChange={(e) => { handleDateChange('startDate', e.target.value); setSearchError(''); }}
                            className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Fecha de fin
                        </label>
                        <input
                            type="date"
                            value={dateRange.endDate}
                            onChange={(e) => { handleDateChange('endDate', e.target.value); setSearchError(''); }}
                            className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                        />
                    </div>

                    <div className="flex flex-col justify-end">
                        <button
                            onClick={searchMovements}
                            disabled={loading || !dateRange.startDate || !dateRange.endDate}
                            className="w-full px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 disabled:bg-slate-600 disabled:cursor-not-allowed transition-colors"
                        >
                            {loading ? 'Buscando...' : 'Generar Reporte'}
                        </button>
                    </div>

                    <div className="flex flex-col justify-end">
                        {hasSearched && (
                            <button
                                onClick={clearSearch}
                                className="w-full px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition-colors"
                            >
                                Limpiar
                            </button>
                        )}
                    </div>
                </div>

                {/* Error de búsqueda */}
                {searchError && (
                    <div className="mt-3 p-3 bg-red-900/30 border border-red-500/50 rounded-lg flex items-start gap-2">
                        <AlertCircle className="h-4 w-4 text-red-400 flex-shrink-0 mt-0.5" />
                        <p className="text-red-400 text-sm">{searchError}</p>
                    </div>
                )}

                {/* Quick Date Ranges */}
                <div className="flex flex-wrap gap-2 mt-4">
                    <button
                        onClick={() => setQuickRange(7)}
                        className="px-3 py-2 bg-slate-700/50 text-slate-300 rounded-lg hover:bg-slate-600 transition-colors text-sm"
                    >
                        Últimos 7 días
                    </button>
                    <button
                        onClick={() => setQuickRange(30)}
                        className="px-3 py-2 bg-slate-700/50 text-slate-300 rounded-lg hover:bg-slate-600 transition-colors text-sm"
                    >
                        Últimos 30 días
                    </button>
                    <button
                        onClick={() => setQuickRange(90)}
                        className="px-3 py-2 bg-slate-700/50 text-slate-300 rounded-lg hover:bg-slate-600 transition-colors text-sm"
                    >
                        Últimos 3 meses
                    </button>
                </div>
            </div>

            {/* Statistics Summary */}
            {hasSearched && stats && (
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-medium text-slate-100">Resumen del Período</h3>
                        <div className="text-sm text-slate-400">
                            {dateRange.startDate} al {dateRange.endDate}
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                        <div className="bg-slate-700/30 p-4 rounded-lg">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-slate-400 text-sm">Total Movimientos</p>
                                    <p className="text-2xl font-bold text-slate-100">{stats.total}</p>
                                </div>
                                <Package className="w-8 h-8 text-blue-400" />
                            </div>
                        </div>

                        <div className="bg-slate-700/30 p-4 rounded-lg">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-slate-400 text-sm">Incrementos Stock</p>
                                    <p className="text-2xl font-bold text-green-400">{stats.stockChanges.increases}</p>
                                    <p className="text-xs text-slate-500">+{stats.stockChanges.totalIncrease} unidades</p>
                                </div>
                                <TrendingUp className="w-8 h-8 text-green-400" />
                            </div>
                        </div>

                        <div className="bg-slate-700/30 p-4 rounded-lg">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-slate-400 text-sm">Decrementos Stock</p>
                                    <p className="text-2xl font-bold text-red-400">{stats.stockChanges.decreases}</p>
                                    <p className="text-xs text-slate-500">-{stats.stockChanges.totalDecrease} unidades</p>
                                </div>
                                <TrendingDown className="w-8 h-8 text-red-400" />
                            </div>
                        </div>

                        <div className="bg-slate-700/30 p-4 rounded-lg">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-slate-400 text-sm">Promedio Diario</p>
                                    <p className="text-2xl font-bold text-slate-100">
                                        {Math.ceil(stats.total / Math.max(1, Object.keys(stats.byDay).length))}
                                    </p>
                                </div>
                                <BarChart3 className="w-8 h-8 text-purple-400" />
                            </div>
                        </div>
                    </div>

                    {/* Movement Types Distribution */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <h4 className="text-md font-medium text-slate-300 mb-3">Distribución por Tipo</h4>
                            <div className="space-y-2">
                                {Object.entries(stats.byType).map(([type, count]) => (
                                    <div key={type} className="flex items-center justify-between py-2 px-3 bg-slate-700/30 rounded">
                                        <span className={`px-2 py-1 rounded text-xs font-medium ${getMovementColor(type)}`}>
                                            {getMovementLabel(type)}
                                        </span>
                                        <span className="text-slate-300 font-medium">{count}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div>
                            <h4 className="text-md font-medium text-slate-300 mb-3">Actividad por Día</h4>
                            <div className="space-y-2 max-h-48 overflow-y-auto">
                                {Object.entries(stats.byDay)
                                    .sort(([a], [b]) => new Date(b) - new Date(a))
                                    .map(([day, count]) => (
                                        <div key={day} className="flex items-center justify-between py-2 px-3 bg-slate-700/30 rounded">
                                            <span className="text-slate-300">{day}</span>
                                            <span className="text-slate-400">{count} movimientos</span>
                                        </div>
                                    ))}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Movements List */}
            {hasSearched && (
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50">
                    <div className="p-4 border-b border-slate-700/50">
                        <div className="flex items-center justify-between">
                            <h3 className="text-lg font-medium text-slate-100">
                                Movimientos Encontrados ({movements.length})
                            </h3>
                            <button
                                disabled
                                title="Funcionalidad de exportación disponible próximamente"
                                className="flex items-center gap-2 px-3 py-2 bg-slate-600 text-slate-400 rounded-lg cursor-not-allowed opacity-60"
                            >
                                <Download className="w-4 h-4" />
                                Exportar
                            </button>
                        </div>
                    </div>

                    {loading ? (
                        <div className="p-8 text-center">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                            <p className="text-slate-400">Generando reporte...</p>
                        </div>
                    ) : movements.length === 0 ? (
                        <div className="p-8 text-center">
                            <Calendar className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                            <p className="text-slate-400">No se encontraron movimientos en el rango seleccionado</p>
                        </div>
                    ) : (
                        <div className="divide-y divide-slate-700/50 max-h-96 overflow-y-auto">
                            {movements.map((movement) => (
                                <div
                                    key={movement.id}
                                    className="p-4 hover:bg-slate-700/20 cursor-pointer transition-colors"
                                    onClick={() => onViewDetail(movement)}
                                >
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center gap-4">
                                            <span className={`px-2 py-1 rounded text-xs font-medium ${getMovementColor(movement.type)}`}>
                                                {getMovementLabel(movement.type)}
                                            </span>
                                            <div>
                                                <h4 className="font-medium text-slate-100">{movement.tool.name}</h4>
                                                {movement.type === 'DECOMMISSION' && movement.description && (
                                                    <p className="text-xs text-red-400 font-medium mt-1">
                                                        ⚠️ {movement.description}
                                                    </p>
                                                )}
                                                <p className="text-sm text-slate-400">{formatDateTime(movement.createdAt)}</p>
                                            </div>
                                        </div>

                                        <div className="text-right">
                                            <div className="text-sm text-slate-300">
                                                Cantidad: {movement.quantity}
                                            </div>
                                            <div className="text-xs text-slate-500">
                                                Stock: {movement.stockBefore} → {movement.stockAfter}
                                            </div>
                                        </div>
                                    </div>

                                    {movement.description && (
                                        <div className="mt-2 ml-16">
                                            <p className="text-sm text-slate-500">{movement.description}</p>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {!hasSearched && (
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-8 text-center">
                    <Calendar className="w-16 h-16 text-slate-500 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-slate-300 mb-2">
                        Selecciona un rango de fechas
                    </h3>
                    <p className="text-slate-500">
                        Elige las fechas de inicio y fin para generar el reporte de movimientos
                    </p>
                </div>
            )}
        </div>
    );
};

export default DateRangeReport;
