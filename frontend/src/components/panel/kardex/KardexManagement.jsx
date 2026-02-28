﻿import React, { useState, useEffect } from 'react';
import { BarChart3, AlertCircle, CheckCircle, Eye, RefreshCw, Wrench } from 'lucide-react';
import { useKardex } from './hooks/useKardex';
import { useTools } from '../inventory/hooks/useTools';
import MovementsList from './components/MovementsList';
import MovementDetail from './components/MovementDetail';
import KardexByTool from './components/KardexByTool';
import MovementFilters from './components/MovementFilters';

const KardexManagement = () => {
    const [activeView, setActiveView] = useState('list'); // 'list', 'byTool', 'detail'
    const [selectedMovement, setSelectedMovement] = useState(null);
    const [selectedTool, setSelectedTool] = useState(null);
    const [filters, setFilters] = useState({
        search: '',
        type: 'ALL',
        tool: 'ALL',
        dateStart: '',
        dateEnd: ''
    });

    const {
        loading,
        error,
        loadMovements,
        getMovementStatistics,
        filterMovements
    } = useKardex();

    const { tools, loadTools } = useTools();

    // Load data on component mount
    useEffect(() => {
        loadMovements();
        loadTools();
    }, [loadMovements, loadTools]);

    // Get filtered movements
    const filteredMovements = filterMovements(filters.search, filters.type, filters.tool, filters.dateStart, filters.dateEnd);

    // Get statistics
    const stats = getMovementStatistics();

    const handleViewDetail = (movement) => {
        setSelectedMovement(movement);
        setActiveView('detail');
    };

    const handleViewByTool = (tool) => {
        setSelectedTool(tool);
        setActiveView('byTool');
    };

    const handleFiltersChange = (newFilters) => {
        setFilters({ ...filters, ...newFilters });
    };

    const renderHeader = () => (
        <div className="mb-8">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6">
                <div>
                    <h1 className="text-3xl font-bold text-white mb-2">Gestión de Kardex</h1>
                    <p className="text-slate-400">Consulta y auditoría de movimientos de inventario</p>
                </div>

                {/* View buttons */}
                <div className="flex flex-wrap gap-2">
                    <button
                        onClick={() => setActiveView('list')}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                            activeView === 'list'
                                ? 'bg-orange-600 text-white'
                                : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                        }`}
                    >
                        <Eye className="w-4 h-4" />
                        Lista General
                    </button>
                    <button
                        onClick={() => setActiveView('byTool')}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                            activeView === 'byTool'
                                ? 'bg-orange-600 text-white'
                                : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                        }`}
                    >
                        <BarChart3 className="w-4 h-4" />
                        Por Herramienta
                    </button>
                </div>
            </div>

            {/* Statistics cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                    <div className="flex items-start justify-between">
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-slate-400">Total Movimientos</p>
                            <p className="text-3xl font-bold text-white">{stats.total}</p>
                        </div>
                        <div className="p-3 rounded-lg bg-orange-500/10">
                            <BarChart3 className="w-6 h-6 text-orange-400" />
                        </div>
                    </div>
                </div>

                <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                    <div className="flex items-start justify-between">
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-slate-400">Préstamos</p>
                            <p className="text-3xl font-bold text-white">{stats.byType['LOAN'] || 0}</p>
                        </div>
                        <div className="p-3 rounded-lg bg-sky-500/10">
                            <RefreshCw className="w-6 h-6 text-sky-400" />
                        </div>
                    </div>
                </div>

                <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                    <div className="flex items-start justify-between">
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-slate-400">Devoluciones</p>
                            <p className="text-3xl font-bold text-white">{stats.byType['RETURN'] || 0}</p>
                        </div>
                        <div className="p-3 rounded-lg bg-emerald-500/10">
                            <CheckCircle className="w-6 h-6 text-emerald-400" />
                        </div>
                    </div>
                </div>

                <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                    <div className="flex items-start justify-between">
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-slate-400">Reparaciones</p>
                            <p className="text-3xl font-bold text-white">{stats.byType['REPAIR'] || 0}</p>
                        </div>
                        <div className="p-3 rounded-lg bg-yellow-500/10">
                            <Wrench className="w-6 h-6 text-yellow-400" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderContent = () => {
        if (loading) {
            return (
                <div className="bg-slate-800/50 backdrop-blur rounded-lg p-8 border border-slate-700/50">
                    <div className="flex items-center justify-center">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
                        <span className="ml-3 text-slate-300">Cargando movimientos...</span>
                    </div>
                </div>
            );
        }

        if (error) {
            return (
                <div className="bg-slate-800/50 backdrop-blur rounded-lg p-6 border border-red-500/50">
                    <div className="flex items-center gap-3">
                        <AlertCircle className="w-5 h-5 text-red-400" />
                        <div>
                            <h3 className="text-red-400 font-medium">Error al cargar movimientos</h3>
                            <p className="text-slate-300 text-sm mt-1">{error}</p>
                        </div>
                    </div>
                    <button
                        onClick={loadMovements}
                        className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                    >
                        Reintentar
                    </button>
                </div>
            );
        }

        switch (activeView) {
            case 'list':
                return (
                    <>
                        <MovementFilters
                            filters={filters}
                            tools={tools}
                            onChange={handleFiltersChange}
                        />
                        <MovementsList
                            movements={filteredMovements}
                            onViewDetail={handleViewDetail}
                        />
                    </>
                );

            case 'byTool':
                return (
                    <KardexByTool
                        tools={tools}
                        selectedTool={selectedTool}
                        onSelectTool={setSelectedTool}
                        onViewDetail={handleViewDetail}
                    />
                );

            case 'detail':
                return (
                    <MovementDetail
                        movement={selectedMovement}
                        onBack={() => setActiveView('list')}
                    />
                );

            default:
                return null;
        }
    };

    return (
        <div className="space-y-6">
            {renderHeader()}
            {renderContent()}
        </div>
    );
};

export default KardexManagement;
