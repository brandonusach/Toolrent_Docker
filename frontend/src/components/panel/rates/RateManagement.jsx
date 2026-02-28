﻿import React, { useState, useEffect } from 'react';
import { Calculator, Settings } from 'lucide-react';

// Componentes
import FineRateConfig from './components/FineRateConfig';
import ReplacementValues from './components/ReplacementValues';

// Hooks
import { useRates } from './hooks/useRates';

// Constantes
import { PERMISSIONS, hasPermission } from './utils/rateConstants';

const RateManagement = () => {
    // Por ahora asumimos que el usuario es admin
    const isAdmin = true;

    // State para tab activa
    const [activeTab, setActiveTab] = useState('fine');


    // Data hooks
    const {
        rates,
        loading,
        error,
        loadRates,
        createRate,
        updateRate,
        deactivateRate,
        getCurrentRates,
        clearError
    } = useRates();

    // Cargar datos iniciales
    useEffect(() => {
        loadRates();
        getCurrentRates();
    }, [loadRates, getCurrentRates]);

    // Tabs de configuración
    const tabs = [
        {
            id: 'fine',
            name: 'Multa por Atraso',
            icon: Calculator,
            component: FineRateConfig,
            description: 'Configurar tarifa diaria de multa'
        },
        {
            id: 'replacement',
            name: 'Valores Reposición',
            icon: Settings,
            component: ReplacementValues,
            description: 'Configurar porcentaje de reparación'
        }
    ];


    // Stats para mostrar en el header
    const currentRates = rates.current || {};
    const stats = {
        lateFeeRate: currentRates.LATE_FEE_RATE || 0,
        repairRate: currentRates.REPAIR_RATE || 0
    };

    if (!hasPermission(isAdmin ? 'admin' : 'user', PERMISSIONS.RATE.VIEW)) {
        return (
            <div>
                <div className="flex items-center justify-center py-12">
                    <div className="text-center">
                        <p className="text-red-400 mb-2">No tiene permisos para acceder a la gestión de tarifas</p>
                        <p className="text-slate-400">Contacte al administrador para obtener acceso</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div>
            {/* Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-white mb-2">
                    Gestión de Tarifas Globales
                </h1>
                <p className="text-slate-400">
                    Administra multas por atraso y tarifas de reparación del sistema (solo Administradores)
                </p>
                <p className="text-sm text-slate-500 mt-1 mb-6">
                    Nota: Las tarifas de arriendo se configuran individualmente en cada herramienta
                </p>

                {/* Current Rates Summary */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    <div className="bg-slate-800/60 backdrop-blur-sm p-6 rounded-lg border border-slate-700/50 shadow-lg">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-slate-400 text-sm font-medium">Multa por Atraso</p>
                                <p className="text-3xl font-bold text-white">${stats.lateFeeRate.toLocaleString()}</p>
                                <p className="text-sm text-slate-400">por día</p>
                            </div>
                            <div className="bg-red-500/20 p-3 rounded-lg">
                                <Calculator className="text-red-400" size={24} />
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/60 backdrop-blur-sm p-6 rounded-lg border border-slate-700/50 shadow-lg">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-slate-400 text-sm font-medium">Tarifa Reparación</p>
                                <p className="text-3xl font-bold text-white">{stats.repairRate}%</p>
                                <p className="text-sm text-slate-400">del valor reposición</p>
                            </div>
                            <div className="bg-orange-500/20 p-3 rounded-lg">
                                <Settings className="text-orange-400" size={24} />
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Error Display */}
            {error && (
                <div className="mb-6 bg-red-500/10 border border-red-500/30 text-red-400 p-4 rounded-lg">
                    <div className="flex items-center justify-between">
                        <span>{error}</span>
                        <button
                            onClick={clearError}
                            className="text-red-300 hover:text-red-100"
                        >
                            ×
                        </button>
                    </div>
                </div>
            )}

            {/* Navigation Tabs */}
            <div className="mb-6">
                <nav className="flex space-x-1 bg-slate-800 p-1 rounded-lg border border-slate-700/50">
                    {tabs.map((tab) => {
                        const Icon = tab.icon;
                        return (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium transition-all duration-200 ${
                                    activeTab === tab.id
                                        ? 'bg-orange-600 text-white shadow-lg'
                                        : 'text-slate-400 hover:text-white hover:bg-slate-700'
                                }`}
                            >
                                <Icon size={18} />
                                {tab.name}
                            </button>
                        );
                    })}
                </nav>
            </div>

            {/* Tab Content */}
            <div className="bg-slate-800/60 backdrop-blur-sm rounded-lg border border-slate-700/50 shadow-lg">
                {loading ? (
                    <div className="flex items-center justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
                        <span className="ml-4 text-slate-400">Cargando tarifas...</span>
                    </div>
                ) : (
                    <>
                        {activeTab === 'fine' && (
                            <FineRateConfig
                                currentRate={currentRates.LATE_FEE_RATE}
                                rates={rates}
                                onCreate={createRate}
                                onUpdate={updateRate}
                                onDeactivate={deactivateRate}
                                loading={loading}
                                isAdmin={isAdmin}
                            />
                        )}
                        {activeTab === 'replacement' && (
                            <ReplacementValues
                                currentRate={currentRates.REPAIR_RATE}
                                rates={rates}
                                onCreate={createRate}
                                onUpdate={updateRate}
                                onDeactivate={deactivateRate}
                                loading={loading}
                                isAdmin={isAdmin}
                            />
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default RateManagement;
