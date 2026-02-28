﻿import React, { useState } from 'react';
import { Eye, ArrowUp, ArrowDown, RotateCcw, Minus, Plus, Wrench, AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react';
import { formatDateTime } from '../../../../utils/dateUtils';

const ITEMS_OPTIONS = [10, 15, 20];

const MovementsList = ({ movements, onViewDetail }) => {
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    const handleItemsPerPageChange = (val) => { setItemsPerPage(val); setCurrentPage(1); };

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
            PARTIALLY_AVAILABLE: 'Parcialmente Disponible'
        };
        return labels[status] || status;
    };

    const getMovementIcon = (type) => {
        const icons = {
            INITIAL_STOCK: <Plus className="w-4 h-4" />,
            LOAN: <ArrowDown className="w-4 h-4" />,
            RETURN: <ArrowUp className="w-4 h-4" />,
            REPAIR: <Wrench className="w-4 h-4" />,
            DECOMMISSION: <Minus className="w-4 h-4" />,
            RESTOCK: <Plus className="w-4 h-4" />
        };
        return icons[type] || <RotateCcw className="w-4 h-4" />;
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

    const getStockChangeColor = (type) => {
        switch (type) {
            case 'INITIAL_STOCK':
            case 'RETURN':
            case 'RESTOCK':
                return 'text-green-400';
            case 'LOAN':
            case 'DECOMMISSION':
                return 'text-red-400';
            case 'REPAIR':
                return 'text-yellow-400';
            default:
                return 'text-slate-400';
        }
    };

    const getStockChangePrefix = (type) => {
        switch (type) {
            case 'INITIAL_STOCK':
            case 'RETURN':
            case 'RESTOCK':
                return '+';
            case 'LOAN':
            case 'DECOMMISSION':
                return '-';
            case 'REPAIR':
                return '=';
            default:
                return '';
        }
    };

    if (movements.length === 0) {
        return (
            <div className="bg-slate-800/50 backdrop-blur rounded-lg p-8 border border-slate-700/50 text-center">
                <RotateCcw className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-slate-300 mb-2">No hay movimientos</h3>
                <p className="text-slate-500">No se encontraron movimientos con los filtros aplicados.</p>
            </div>
        );
    }

    // Paginación calculada antes del return
    const totalPages = Math.ceil(movements.length / itemsPerPage);
    const pageStart = (currentPage - 1) * itemsPerPage;
    const paginatedMovements = movements.slice(pageStart, pageStart + itemsPerPage);

    return (
        <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50">
            <div className="p-4 border-b border-slate-700/50">
                <h3 className="text-lg font-medium text-slate-100">
                    Movimientos del Kardex ({movements.length})
                </h3>
            </div>

            <div className="divide-y divide-slate-700/50">
                {paginatedMovements.map((movement) => (
                    <div
                        key={movement.id}
                        className="p-4 hover:bg-slate-700/30 transition-colors group"
                    >
                        <div className="flex items-start justify-between">
                            <div className="flex items-start gap-4 flex-1">
                                {/* Movement type icon */}
                                <div className={`p-2 rounded-lg ${getMovementColor(movement.type)}`}>
                                    {getMovementIcon(movement.type)}
                                </div>

                                {/* Movement details */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-3 mb-2 flex-wrap">
                                        <h4 className="font-medium text-slate-100">
                                            {movement.toolName || 'Herramienta desconocida'}
                                        </h4>
                                        <span className={`px-2 py-1 rounded text-xs font-medium ${getMovementColor(movement.type)}`}>
                                            {getMovementLabel(movement.type)}
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                                        {/* Quantity and stock change */}
                                        <div>
                                            <p className="text-slate-400 mb-1">Cantidad / Cambio</p>
                                            <div className="flex items-center gap-2">
                                                <span className="text-slate-300 font-medium">
                                                    {movement.quantity}
                                                </span>
                                                <span className={`font-medium ${getStockChangeColor(movement.type)}`}>
                                                    ({getStockChangePrefix(movement.type)}{movement.type === 'REPAIR' ? '0' : movement.quantity})
                                                </span>
                                            </div>
                                        </div>

                                        {/* Stock before/after */}
                                        <div>
                                            <p className="text-slate-400 mb-1">Stock: Antes → Después</p>
                                            <span className="text-slate-300">
                                                {movement.stockBefore} → {movement.stockAfter}
                                            </span>
                                        </div>

                                        {/* Date */}
                                        <div>
                                            <p className="text-slate-400 mb-1">Fecha</p>
                                            <span className="text-slate-300">
                                                {formatDateTime(movement.createdAt)}
                                            </span>
                                        </div>
                                    </div>

                                    {/* Description */}
                                    {movement.description && (
                                        <div className="mt-3">
                                            <p className="text-slate-400 text-sm mb-1">
                                                {movement.type === 'DECOMMISSION' ? 'Razón de la Baja' :
                                                 movement.type === 'REPAIR' ? 'Detalle de Reparación' : 'Descripción'}
                                            </p>
                                            <p className={`text-sm ${
                                                movement.type === 'DECOMMISSION' ? 'text-red-300 font-medium' : 
                                                movement.type === 'REPAIR' ? 'text-yellow-300 font-medium' : 
                                                'text-slate-300'
                                            }`}>
                                                {movement.description}
                                            </p>
                                            {movement.type === 'DECOMMISSION' && (
                                                <div className="mt-2 flex items-start gap-2 p-2 bg-red-500/10 border border-red-500/20 rounded">
                                                    <AlertTriangle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                                                    <div className="text-xs text-red-400">
                                                        <p className="font-medium">Herramienta dada de baja permanentemente</p>
                                                        <p className="mt-1 text-red-300">
                                                            Stock NO se incrementa.
                                                            {movement.instanceStatus === 'DECOMMISSIONED' && ' Instancias marcadas como inoperativas.'}
                                                        </p>
                                                    </div>
                                                </div>
                                            )}
                                            {movement.type === 'REPAIR' && (
                                                <div className="mt-2 flex items-start gap-2 p-2 bg-yellow-500/10 border border-yellow-500/20 rounded">
                                                    <AlertTriangle className="w-4 h-4 text-yellow-400 flex-shrink-0 mt-0.5" />
                                                    <div className="text-xs text-yellow-400">
                                                        <p className="font-medium">Herramienta en reparación</p>
                                                        <p className="mt-1 text-yellow-300">
                                                            No disponible temporalmente. Stock se restaurará al completar la reparación.
                                                        </p>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    {/* Related loan info - usando nueva estructura DTO */}
                                    {movement.relatedLoanId && (
                                        <div className="mt-2 flex items-center gap-2">
                                            <span className="text-xs text-blue-400 bg-blue-500/20 px-2 py-1 rounded">
                                                Préstamo #{movement.relatedLoanId}
                                            </span>
                                            {movement.clientName && (
                                                <span className="text-xs text-slate-400">
                                                    Cliente: {movement.clientName}
                                                </span>
                                            )}
                                        </div>
                                    )}

                                    {/* Category info if available */}
                                    {movement.categoryName && (
                                        <div className="mt-1">
                                            <span className="text-xs text-slate-500">
                                                Categoría: {movement.categoryName}
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Actions */}
                            <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button
                                    onClick={() => onViewDetail(movement)}
                                    className="p-2 text-slate-400 hover:text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                                    title="Ver detalle"
                                >
                                    <Eye className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Paginación */}
            {movements.length > 0 && (
                <div className="px-4 py-3 border-t border-slate-700/50 flex flex-col sm:flex-row items-center justify-between gap-3 bg-slate-800/40">
                    <div className="flex items-center gap-3 text-sm text-slate-400">
                        <span>Mostrando {pageStart + 1}–{Math.min(pageStart + itemsPerPage, movements.length)} de {movements.length}</span>
                        <span className="text-slate-600">|</span>
                        <span>Filas:</span>
                        <select value={itemsPerPage} onChange={(e) => handleItemsPerPageChange(Number(e.target.value))}
                            className="bg-slate-700 border border-slate-600 text-white rounded px-2 py-1 text-sm focus:outline-none focus:ring-1 focus:ring-orange-500">
                            {ITEMS_OPTIONS.map(n => <option key={n} value={n}>{n}</option>)}
                        </select>
                    </div>
                    <div className="flex items-center gap-1">
                        <button onClick={() => setCurrentPage(1)} disabled={currentPage === 1}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs">«</button>
                        <button onClick={() => setCurrentPage(p => p - 1)} disabled={currentPage === 1}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                            <ChevronLeft className="h-4 w-4" />
                        </button>
                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(p => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 1)
                            .reduce((acc, p, idx, arr) => { if (idx > 0 && p - arr[idx-1] > 1) acc.push('...'); acc.push(p); return acc; }, [])
                            .map((item, idx) => item === '...'
                                ? <span key={`e${idx}`} className="px-2 text-slate-500 text-sm">…</span>
                                : <button key={item} onClick={() => setCurrentPage(item)}
                                    className={`min-w-[2rem] h-8 rounded text-sm font-medium transition-colors ${currentPage === item ? 'bg-orange-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-700'}`}>{item}</button>
                            )}
                        <button onClick={() => setCurrentPage(p => p + 1)} disabled={currentPage === totalPages}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                            <ChevronRight className="h-4 w-4" />
                        </button>
                        <button onClick={() => setCurrentPage(totalPages)} disabled={currentPage === totalPages}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs">»</button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MovementsList;
