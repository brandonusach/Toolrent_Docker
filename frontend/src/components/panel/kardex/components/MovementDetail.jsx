﻿import React from 'react';
import { ArrowLeft, Package, User, Calendar, FileText, TrendingUp, TrendingDown, RotateCcw, AlertCircle } from 'lucide-react';
import { formatDateTime } from '../../../../utils/dateUtils';

const MovementDetail = ({ movement, onBack }) => {

    if (!movement) {
        return (
            <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-8 text-center">
                <AlertCircle className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-slate-300 mb-2">Movimiento no encontrado</h3>
                <p className="text-slate-500">No se pudo cargar la información del movimiento</p>
                <button
                    onClick={onBack}
                    className="mt-4 px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition-colors"
                >
                    Volver
                </button>
            </div>
        );
    }


    const getMovementColor = (type) => {
        const colors = {
            INITIAL_STOCK: 'bg-blue-500/20 text-blue-400 border-blue-500/50',
            LOAN: 'bg-red-500/20 text-red-400 border-red-500/50',
            RETURN: 'bg-green-500/20 text-green-400 border-green-500/50',
            REPAIR: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50',
            DECOMMISSION: 'bg-slate-500/20 text-slate-400 border-slate-500/50',
            RESTOCK: 'bg-purple-500/20 text-purple-400 border-purple-500/50'
        };
        return colors[type] || 'bg-slate-500/20 text-slate-400 border-slate-500/50';
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
            PARTIALLY_AVAILABLE: 'Parcialmente Disponible'
        };
        return labels[status] || status;
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

    const getMovementIcon = (type) => {
        const icons = {
            INITIAL_STOCK: <Package className="w-6 h-6" />,
            LOAN: <TrendingDown className="w-6 h-6" />,
            RETURN: <TrendingUp className="w-6 h-6" />,
            REPAIR: <RotateCcw className="w-6 h-6" />,
            DECOMMISSION: <TrendingDown className="w-6 h-6" />,
            RESTOCK: <TrendingUp className="w-6 h-6" />
        };
        return icons[type] || <Package className="w-6 h-6" />;
    };

    const getStockChangeColor = (stockBefore, stockAfter) => {
        if (stockAfter > stockBefore) return 'text-green-400';
        if (stockAfter < stockBefore) return 'text-red-400';
        return 'text-yellow-400';
    };

    const getStockChangeText = (type, quantity) => {
        switch (type) {
            case 'INITIAL_STOCK':
            case 'RETURN':
            case 'RESTOCK':
                return `+${quantity} unidades`;
            case 'LOAN':
            case 'DECOMMISSION':
                return `-${quantity} unidades`;
            case 'REPAIR':
                return 'Sin cambio de stock';
            default:
                return 'N/A';
        }
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={onBack}
                            className="p-2 text-slate-400 hover:text-slate-300 hover:bg-slate-700/50 rounded-lg transition-colors"
                        >
                            <ArrowLeft className="w-5 h-5" />
                        </button>
                        <div>
                            <h1 className="text-2xl font-bold text-slate-100">
                                Detalle del Movimiento
                            </h1>
                            <p className="text-slate-400 mt-1">ID: #{movement.id}</p>
                        </div>
                    </div>

                    <div className={`px-4 py-2 rounded-lg border ${getMovementColor(movement.type)}`}>
                        <div className="flex items-center gap-2">
                            {getMovementIcon(movement.type)}
                            <span className="font-medium">
                                {getMovementLabel(movement.type)}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Movement Information */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Basic Information */}
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                    <h3 className="text-lg font-medium text-slate-100 mb-4 flex items-center gap-2">
                        <Package className="w-5 h-5" />
                        Información Básica
                    </h3>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Herramienta
                            </label>
                            <div className="flex items-center gap-3">
                                <Package className="w-4 h-4 text-slate-500" />
                                <div>
                                    <p className="text-slate-100 font-medium">
                                        {movement.toolName || movement.tool?.name || 'Herramienta no especificada'}
                                    </p>
                                    {movement.toolInstanceId ? (
                                        <p className="text-sm text-slate-500">
                                            Herramienta: <span className="text-blue-400 font-semibold">#{movement.toolInstanceId}</span>
                                        </p>
                                    ) : (
                                        <p className="text-sm text-slate-500">
                                            ID: #{movement.toolId || movement.tool?.id || 'N/A'}
                                        </p>
                                    )}
                                    {(movement.categoryName || movement.tool?.category?.name) && (
                                        <p className="text-sm text-slate-400">
                                            {movement.categoryName || movement.tool.category.name}
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Fecha y Hora
                            </label>
                            <div className="flex items-center gap-3">
                                <Calendar className="w-4 h-4 text-slate-500" />
                                <p className="text-slate-100">{formatDateTime(movement.createdAt)}</p>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Tipo de Movimiento
                            </label>
                            <div className="flex items-center gap-3">
                                {getMovementIcon(movement.type)}
                                <span className={`px-3 py-1 rounded-lg font-medium ${getMovementColor(movement.type)}`}>
                                    {getMovementLabel(movement.type)}
                                </span>
                            </div>
                        </div>

                        {/* Alerta especial para BAJA y REPARACIÓN */}
                        {(movement.type === 'DECOMMISSION' || movement.type === 'REPAIR') && (
                            <div className={`${
                                movement.type === 'DECOMMISSION' 
                                    ? 'bg-red-500/10 border-red-500/30' 
                                    : 'bg-yellow-500/10 border-yellow-500/30'
                            } border rounded-lg p-3`}>
                                <label className={`block text-sm font-medium mb-2 flex items-center gap-2 ${
                                    movement.type === 'DECOMMISSION' ? 'text-red-400' : 'text-yellow-400'
                                }`}>
                                    <AlertCircle className="w-4 h-4" />
                                    {movement.type === 'DECOMMISSION' ? 'Baja Permanente' : 'En Reparación'}
                                </label>
                                {movement.type === 'DECOMMISSION' ? (
                                    <>
                                        <p className="text-red-300 font-medium">
                                            Instancias Dadas de Baja (Inoperativas)
                                        </p>
                                        <p className="text-xs text-red-400 mt-2">
                                            ⚠️ Las instancias ya no están disponibles para préstamo y el stock NO se incrementó.
                                        </p>
                                    </>
                                ) : (
                                    <>
                                        <p className="text-yellow-300 font-medium">
                                            Herramienta en Reparación
                                        </p>
                                        <p className="text-xs text-yellow-400 mt-2">
                                            🔧 La herramienta no está disponible temporalmente. El stock se restaurará al completar la reparación.
                                        </p>
                                    </>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                {/* Stock Information */}
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                    <h3 className="text-lg font-medium text-slate-100 mb-4 flex items-center gap-2">
                        <TrendingUp className="w-5 h-5" />
                        Información de Stock
                    </h3>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Cantidad
                            </label>
                            <div className="flex items-center gap-3">
                                <div className="bg-slate-700/50 px-3 py-2 rounded-lg">
                                    <span className="text-2xl font-bold text-slate-100">
                                        {movement.quantity}
                                    </span>
                                    <span className="text-sm text-slate-400 ml-1">unidades</span>
                                </div>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Stock Antes
                                </label>
                                <div className="bg-slate-700/50 px-3 py-2 rounded-lg text-center">
                                    <span className="text-xl font-bold text-slate-100">
                                        {movement.stockBefore}
                                    </span>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Stock Después
                                </label>
                                <div className="bg-slate-700/50 px-3 py-2 rounded-lg text-center">
                                    <span className={`text-xl font-bold ${getStockChangeColor(movement.stockBefore, movement.stockAfter)}`}>
                                        {movement.stockAfter}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="bg-slate-700/30 px-3 py-2 rounded-lg">
                            <div className="flex items-center justify-between">
                                <span className="text-slate-400 text-sm">Cambio de Stock:</span>
                                <span className={`font-medium ${getStockChangeColor(movement.stockBefore, movement.stockAfter)}`}>
                                    {getStockChangeText(movement.type, movement.quantity)}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Description and Additional Information */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Description */}
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                    <h3 className="text-lg font-medium text-slate-100 mb-4 flex items-center gap-2">
                        <FileText className="w-5 h-5" />
                        Descripción
                    </h3>

                    {movement.description ? (
                        <div className="bg-slate-700/30 p-4 rounded-lg">
                            <p className="text-slate-200 leading-relaxed">
                                {movement.description}
                            </p>
                        </div>
                    ) : (
                        <div className="bg-slate-700/30 p-4 rounded-lg text-center">
                            <FileText className="w-8 h-8 text-slate-500 mx-auto mb-2" />
                            <p className="text-slate-400">Sin descripción registrada</p>
                        </div>
                    )}
                </div>

                {/* Related Information */}
                <div className="bg-slate-800/50 backdrop-blur rounded-lg border border-slate-700/50 p-6">
                    <h3 className="text-lg font-medium text-slate-100 mb-4 flex items-center gap-2">
                        <User className="w-5 h-5" />
                        Información Relacionada
                    </h3>

                    <div className="space-y-4">
                        {/* Related Loan - Soporta tanto DTO (relatedLoanId) como entidad (relatedLoan) */}
                        {(movement.relatedLoanId || movement.relatedLoan) ? (
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-2">
                                    Préstamo Relacionado
                                </label>
                                <div className="bg-blue-500/20 border border-blue-500/50 p-3 rounded-lg">
                                    <div className="flex items-center gap-2">
                                        <Package className="w-4 h-4 text-blue-400" />
                                        <span className="text-blue-400 font-medium">
                                            Préstamo #{movement.relatedLoanId || movement.relatedLoan.id}
                                        </span>
                                    </div>
                                    {(movement.clientName || movement.relatedLoan?.client?.name) && (
                                        <p className="text-sm text-blue-300 mt-1">
                                            Cliente: {movement.clientName || movement.relatedLoan.client.name}
                                        </p>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-2">
                                    Préstamo Relacionado
                                </label>
                                <div className="bg-slate-700/30 p-3 rounded-lg text-center">
                                    <p className="text-slate-500 text-sm">No relacionado con préstamo</p>
                                </div>
                            </div>
                        )}

                        {/* Movement ID and Timestamp */}
                        <div className="grid grid-cols-1 gap-3">
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    ID del Movimiento
                                </label>
                                <div className="bg-slate-700/30 px-3 py-2 rounded">
                                    <span className="text-slate-300 font-mono">#{movement.id}</span>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Timestamp
                                </label>
                                <div className="bg-slate-700/30 px-3 py-2 rounded">
                                    <span className="text-slate-300 font-mono text-sm">
                                        {movement.createdAt}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-start items-center">
                <button
                    onClick={onBack}
                    className="flex items-center gap-2 px-6 py-3 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition-colors"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Volver a la Lista
                </button>
            </div>
        </div>
    );
};

export default MovementDetail;
