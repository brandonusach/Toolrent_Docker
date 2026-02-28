﻿// ReportManagement.jsx - Contenedor principal para reportes y consultas
import React, { useState, useEffect } from 'react';
import { useReports } from './hooks/useReports';
import ActiveLoansReport from './components/ActiveLoansReport';
import OverdueClientsReport from './components/OverdueClientsReport';
import PopularToolsReport from './components/PopularToolsReport';
import { BarChart3, RefreshCw, AlertTriangle } from 'lucide-react';

const ReportManagement = () => {
    const {
        loading,
        error,
        reportData,
        getActiveLoansReport,
        getOverdueClientsReport,
        getPopularToolsReport,
        generateGeneralSummary,
        clearReportData
    } = useReports();

    const [activeTab, setActiveTab] = useState('active-loans');
    const [generalSummary, setGeneralSummary] = useState(null);

    // Cargar todos los datos al inicializar (sin filtros de fecha)
    useEffect(() => {
        const loadInitialData = async () => {
            try {
                const summary = await generateGeneralSummary();
                setGeneralSummary(summary);

                // Cargar también los datos del tab inicial
                await getActiveLoansReport();
            } catch (err) {
            }
        };

        loadInitialData();
    }, [generateGeneralSummary, getActiveLoansReport]);

    // Manejar cambio de pestaña
    const handleTabChange = async (tab) => {
        setActiveTab(tab);

        // Cargar datos para la nueva pestaña inmediatamente
        try {
            switch (tab) {
                case 'active-loans':
                    await getActiveLoansReport();
                    break;
                case 'overdue-clients':
                    await getOverdueClientsReport();
                    break;
                case 'popular-tools':
                    await getPopularToolsReport();
                    break;
                default:
                    break;
            }
        } catch (err) {
        }
    };

    // Limpiar todos los datos
    const handleClearData = () => {
        clearReportData();
        setGeneralSummary(null);
    };

    const tabs = [
        {
            id: 'active-loans',
            label: 'Préstamos Activos',
            icon: RefreshCw,
            description: 'RF6.1: Estado de préstamos vigentes y atrasados'
        },
        {
            id: 'overdue-clients',
            label: 'Clientes con Atrasos',
            icon: AlertTriangle,
            description: 'RF6.2: Listado de clientes morosos'
        },
        {
            id: 'popular-tools',
            label: 'Herramientas Populares',
            icon: BarChart3,
            description: 'RF6.3: Ranking de herramientas más prestadas'
        }
    ];

    return (
        <div>
            {/* Header */}
            <div className="mb-8">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-white mb-2">
                            Reportes y Consultas
                        </h1>
                        <p className="text-slate-400">
                            Visualización de información clave para la toma de decisiones
                        </p>
                    </div>

                    <button
                        onClick={handleClearData}
                        className="px-4 py-2 bg-slate-700 text-slate-300 rounded-lg hover:bg-slate-600 transition-colors border border-slate-600"
                        title="Limpiar todos los datos"
                    >
                        <RefreshCw className="h-4 w-4 mr-2 inline" />
                        Limpiar
                    </button>
                </div>

                {/* Resumen General */}
                {generalSummary && (
                    <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                            <div className="flex items-start justify-between">
                                <div className="space-y-1">
                                    <p className="text-sm font-medium text-slate-400">Préstamos Activos</p>
                                    <p className="text-3xl font-bold text-white">
                                        {generalSummary.activeLoans?.total || 0}
                                    </p>
                                    <p className="text-sm text-slate-400">
                                        {generalSummary.activeLoans?.overdue || 0} atrasados
                                    </p>
                                </div>
                                <div className="p-3 rounded-lg bg-orange-500/10">
                                    <RefreshCw className="h-6 w-6 text-orange-500" />
                                </div>
                            </div>
                        </div>

                        <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                            <div className="flex items-start justify-between">
                                <div className="space-y-1">
                                    <p className="text-sm font-medium text-slate-400">Clientes Morosos</p>
                                    <p className="text-3xl font-bold text-white">
                                        {generalSummary.overdueClients?.totalClients || 0}
                                    </p>
                                    <p className="text-sm text-slate-400">
                                        ${generalSummary.overdueClients?.totalOverdueAmount || 0} en multas
                                    </p>
                                </div>
                                <div className="p-3 rounded-lg bg-red-500/10">
                                    <AlertTriangle className="h-6 w-6 text-red-500" />
                                </div>
                            </div>
                        </div>

                        <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                            <div className="flex items-start justify-between">
                                <div className="space-y-1">
                                    <p className="text-sm font-medium text-slate-400">Herramienta Top</p>
                                    <p className="text-3xl font-bold text-white truncate">
                                        {generalSummary.popularTools?.mostPopularTool?.name || 'N/A'}
                                    </p>
                                    <p className="text-sm text-slate-400">
                                        {generalSummary.popularTools?.mostPopularTool?.totalLoans || 0} préstamos
                                    </p>
                                </div>
                                <div className="p-3 rounded-lg bg-orange-500/10">
                                    <BarChart3 className="h-6 w-6 text-orange-500" />
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* Pestañas de Navegación */}
            <div className="mb-6">
                <div className="border-b border-slate-700">
                    <nav className="-mb-px flex space-x-8">
                        {tabs.map((tab) => {
                            const IconComponent = tab.icon;
                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => handleTabChange(tab.id)}
                                    className={`whitespace-nowrap py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                                        activeTab === tab.id
                                            ? 'border-orange-500 text-orange-400'
                                            : 'border-transparent text-slate-400 hover:text-slate-300 hover:border-slate-600'
                                    }`}
                                >
                                    <span className="flex items-center gap-2">
                                        <IconComponent className="h-4 w-4" />
                                        <span>{tab.label}</span>
                                    </span>
                                </button>
                            );
                        })}
                    </nav>
                </div>

                {/* Descripción de la pestaña activa */}
                <div className="mt-3 text-sm text-slate-400">
                    {tabs.find(tab => tab.id === activeTab)?.description}
                </div>
            </div>

            {/* Indicador de Carga Global */}
            {loading && (
                <div className="mb-4 p-4 bg-slate-800 border border-slate-700 rounded-lg">
                    <div className="flex items-center gap-3">
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-orange-500"></div>
                        <span className="text-slate-300">Cargando datos del reporte...</span>
                    </div>
                </div>
            )}

            {/* Mensaje de Error Global */}
            {error && (
                <div className="mb-4 p-4 bg-slate-800 border border-slate-700 rounded-lg">
                    <div className="flex items-center gap-3">
                        <AlertTriangle className="h-5 w-5 text-yellow-500" />
                        <div>
                            <p className="text-yellow-500 font-medium">Aviso</p>
                            <p className="text-slate-400 text-sm">{error}</p>
                        </div>
                    </div>
                </div>
            )}

            {/* Contenido de las Pestañas */}
            <div className="bg-slate-800/50 backdrop-blur-sm rounded-lg border border-slate-700/50">
                {activeTab === 'active-loans' && (
                    <ActiveLoansReport
                        data={reportData.activeLoans}
                        loading={loading}
                    />
                )}

                {activeTab === 'overdue-clients' && (
                    <OverdueClientsReport
                        data={reportData.overdueClients}
                        loading={loading}
                    />
                )}

                {activeTab === 'popular-tools' && (
                    <PopularToolsReport
                        data={reportData.popularTools}
                        loading={loading}
                    />
                )}
            </div>
        </div>
    );
};

export default ReportManagement;
