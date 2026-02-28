// PopularToolsReport.jsx - RF6.3: Ranking herramientas más prestadas
import React, { useState, useMemo } from 'react';
import { BarChart3, Wrench, Search, ArrowUpDown, ArrowUp, ArrowDown, Users, Clock } from 'lucide-react';

const PopularToolsReport = ({ data, loading }) => {
    const [viewMode, setViewMode] = useState('table'); // 'table' | 'chart'
    const [sortField, setSortField] = useState('totalLoans');
    const [sortDirection, setSortDirection] = useState('desc');
    const [searchTerm, setSearchTerm] = useState('');
    const [limitResults, setLimitResults] = useState(10);

    // Procesar y filtrar datos
    const processedData = useMemo(() => {
        if (!data || !data.tools) return { tools: [], summary: {} };

        let filteredTools = [...data.tools];

        // Filtrar por búsqueda
        if (searchTerm) {
            filteredTools = filteredTools.filter(tool =>
                tool.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                tool.categoryName?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        // Ordenar
        filteredTools.sort((a, b) => {
            let aValue = a[sortField];
            let bValue = b[sortField];

            if (typeof aValue === 'number') {
                aValue = Number(aValue) || 0;
                bValue = Number(bValue) || 0;
            } else {
                aValue = String(aValue || '').toLowerCase();
                bValue = String(bValue || '').toLowerCase();
            }

            if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
            return 0;
        });

        // Limitar resultados
        const limitedTools = filteredTools.slice(0, limitResults);

        return {
            tools: limitedTools,
            allTools: filteredTools,
            summary: {
                ...data.summary,
                filtered: {
                    totalTools: limitedTools.length,
                    totalLoans: limitedTools.reduce((acc, tool) => acc + (tool.totalLoans || 0), 0),
                    totalQuantity: limitedTools.reduce((acc, tool) => acc + (tool.totalQuantity || 0), 0)
                }
            }
        };
    }, [data, searchTerm, sortField, sortDirection, limitResults]);

    const handleSort = (field) => {
        if (sortField === field) {
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            setSortField(field);
            setSortDirection('desc');
        }
    };

    const getSortIcon = (field) => {
        if (sortField !== field) return ArrowUpDown;
        return sortDirection === 'asc' ? ArrowUp : ArrowDown;
    };

    const getPopularityIcon = (position) => {
        if (position === 0) return { icon: '🥇', color: 'text-yellow-400', isMedal: true };
        if (position === 1) return { icon: '🥈', color: 'text-slate-300', isMedal: true };
        if (position === 2) return { icon: '🥉', color: 'text-orange-400', isMedal: true };
        return { icon: null, color: 'text-slate-400', isMedal: false };
    };

    const getPopularityBar = (tool, maxLoans) => {
        const percentage = maxLoans > 0 ? (tool.totalLoans / maxLoans) * 100 : 0;
        return (
            <div className="w-full bg-slate-700 rounded-full h-2">
                <div
                    className="bg-gradient-to-r from-orange-500 to-orange-400 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${percentage}%` }}
                ></div>
            </div>
        );
    };

    const getCategoryColor = (categoryName) => {
        const colors = {
            'Herramientas Eléctricas': 'bg-blue-500/10 text-blue-400 border-blue-500/20',
            'Herramientas Manuales': 'bg-green-500/10 text-green-400 border-green-500/20',
            'Equipos de Medición': 'bg-purple-500/10 text-purple-400 border-purple-500/20',
            'Herramientas de Corte': 'bg-red-500/10 text-red-400 border-red-500/20',
            'Equipos de Seguridad': 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
        };
        return colors[categoryName] || 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    };

    if (loading) {
        return (
            <div className="p-6">
                <div className="animate-pulse space-y-4">
                    <div className="h-4 bg-slate-700 rounded w-1/4"></div>
                    <div className="h-32 bg-slate-700 rounded"></div>
                </div>
            </div>
        );
    }

    if (!data || !data.tools) {
        return (
            <div className="p-6">
                <div className="text-center py-12">
                    <BarChart3 className="h-16 w-16 text-slate-600 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-white mb-2">
                        No hay datos disponibles
                    </h3>
                    <p className="text-slate-400">
                        Los datos del ranking de herramientas se cargarán automáticamente.
                    </p>
                </div>
            </div>
        );
    }

    const maxLoans = processedData.tools.length > 0 ? Math.max(...processedData.tools.map(t => t.totalLoans || 0)) : 0;

    return (
        <div className="p-6">
            {/* Header y Resumen */}
            <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <BarChart3 className="h-5 w-5 text-green-500" />
                        Ranking de Herramientas Más Prestadas
                    </h2>
                </div>

                {/* Resumen de estadísticas */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-blue-500/10 p-2 rounded-lg">
                                <Wrench className="h-5 w-5 text-blue-400" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Herramientas Analizadas</p>
                                <p className="text-2xl font-bold text-white">
                                    {processedData.summary.totalToolsAnalyzed || 0}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-green-500/10 p-2 rounded-lg">
                                <BarChart3 className="h-5 w-5 text-green-400" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Total Préstamos</p>
                                <p className="text-2xl font-bold text-white">
                                    {processedData.summary.totalLoansAnalyzed || 0}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-purple-500/10 p-2 rounded-lg">
                                <BarChart3 className="h-5 w-5 text-purple-400" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Promedio por Herramienta</p>
                                <p className="text-2xl font-bold text-white">
                                    {(processedData.summary.avgLoansPerTool || 0).toFixed(1)}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-yellow-500/10 p-2 rounded-lg">
                                <BarChart3 className="h-5 w-5 text-yellow-400" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Más Popular</p>
                                <p className="text-lg font-bold text-white truncate">
                                    {processedData.summary.mostPopularTool?.name || 'N/A'}
                                </p>
                                <p className="text-xs text-slate-500">
                                    {processedData.summary.mostPopularTool?.totalLoans || 0} préstamos
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Controles */}
                <div className="flex flex-col lg:flex-row gap-4 mb-6">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Buscar herramienta o categoría..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent text-white placeholder-slate-400"
                        />
                    </div>

                    <div className="flex gap-2">
                        <select
                            value={limitResults}
                            onChange={(e) => setLimitResults(Number(e.target.value))}
                            className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 text-white"
                        >
                            <option value={5}>Top 5</option>
                            <option value={10}>Top 10</option>
                            <option value={20}>Top 20</option>
                            <option value={50}>Top 50</option>
                        </select>

                        <div className="flex bg-slate-800 border border-slate-700 rounded-md overflow-hidden">
                            <button
                                onClick={() => setViewMode('table')}
                                className={`px-3 py-2 text-sm flex items-center gap-1 transition-colors ${
                                    viewMode === 'table' 
                                        ? 'bg-orange-500/20 text-orange-400' 
                                        : 'text-slate-400 hover:text-slate-300'
                                }`}
                            >
                                <BarChart3 className="h-4 w-4" />
                                Tabla
                            </button>
                            <button
                                onClick={() => setViewMode('chart')}
                                className={`px-3 py-2 text-sm flex items-center gap-1 transition-colors ${
                                    viewMode === 'chart' 
                                        ? 'bg-orange-500/20 text-orange-400' 
                                        : 'text-slate-400 hover:text-slate-300'
                                }`}
                            >
                                <BarChart3 className="h-4 w-4" />
                                Gráfico
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Contenido Principal */}
            {processedData.tools.length === 0 ? (
                <div className="text-center py-12">
                    <Search className="h-12 w-12 text-slate-600 mx-auto mb-4" />
                    <p className="text-slate-400">No se encontraron herramientas con los filtros aplicados</p>
                </div>
            ) : (
                <>
                    {/* Vista de Tabla */}
                    {viewMode === 'table' && (
                        <div className="overflow-x-auto">
                            <table className="min-w-full bg-slate-800/30 backdrop-blur-sm border border-slate-700 rounded-lg">
                                <thead className="bg-slate-800/50">
                                    <tr>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">
                                            Posición
                                        </th>
                                        {[
                                            { key: 'name', label: 'Herramienta' },
                                            { key: 'totalLoans', label: 'Préstamos' },
                                            { key: 'uniqueClients', label: 'Clientes Únicos' },
                                            { key: 'avgLoanDuration', label: 'Duración Prom.' }
                                        ].map((column) => {
                                            const SortIcon = getSortIcon(column.key);
                                            return (
                                                <th
                                                    key={column.key}
                                                    className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider cursor-pointer hover:bg-slate-700/50 transition-colors"
                                                    onClick={() => handleSort(column.key)}
                                                >
                                                    <div className="flex items-center gap-1">
                                                        {column.label}
                                                        <SortIcon className="h-3 w-3" />
                                                    </div>
                                                </th>
                                            );
                                        })}
                                        <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">
                                            Categoría
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">
                                            Popularidad
                                        </th>
                                    </tr>
                                </thead>
                                <tbody className="bg-slate-800/20 divide-y divide-slate-700">
                                    {processedData.tools.map((tool, index) => {
                                        const popularityIcon = getPopularityIcon(index);
                                        return (
                                            <tr key={tool.id} className="hover:bg-slate-700/30 transition-colors">
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="flex items-center gap-2">
                                                        {popularityIcon.isMedal ? (
                                                            <span className={`text-2xl ${popularityIcon.color}`}>
                                                                {popularityIcon.icon}
                                                            </span>
                                                        ) : (
                                                            <span className={`text-sm font-bold ${popularityIcon.color}`}>
                                                                #{index + 1}
                                                            </span>
                                                        )}
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="text-sm font-medium text-white">
                                                        {tool.name}
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="flex items-center gap-2">
                                                        <span className="text-sm font-bold text-orange-400">
                                                            {tool.totalLoans}
                                                        </span>
                                                        <div className="w-16">
                                                            {getPopularityBar(tool, maxLoans)}
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <div className="flex items-center gap-1">
                                                        <Users className="h-3 w-3 text-purple-400" />
                                                        {tool.uniqueClients}
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <div className="flex items-center gap-1">
                                                        <Clock className="h-3 w-3 text-blue-400" />
                                                        {(tool.avgLoanDuration || 0).toFixed(1)} días
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <span className={`px-2 py-1 text-xs font-medium rounded-full border ${getCategoryColor(tool.categoryName)}`}>
                                                        {tool.categoryName}
                                                    </span>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="flex items-center">
                                                        <div className="flex-1 bg-slate-700 rounded-full h-2 mr-2">
                                                            <div
                                                                className="bg-gradient-to-r from-yellow-500 to-orange-500 h-2 rounded-full"
                                                                style={{ width: `${Math.min((tool.popularityScore || 0) / 100 * 100, 100)}%` }}
                                                            ></div>
                                                        </div>
                                                        <span className="text-xs text-slate-400 min-w-max">
                                                            {(tool.popularityScore || 0).toFixed(1)}
                                                        </span>
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Vista de Gráfico de Barras */}
                    {viewMode === 'chart' && (
                        <div className="bg-slate-800/30 backdrop-blur-sm p-6 rounded-lg border border-slate-700">
                            <h3 className="text-lg font-medium text-white mb-4 flex items-center gap-2">
                                <BarChart3 className="h-5 w-5 text-orange-500" />
                                Gráfico de Barras - Top {limitResults} Herramientas
                            </h3>
                            <div className="space-y-4">
                                {processedData.tools.map((tool, index) => {
                                    const popularityIcon = getPopularityIcon(index);
                                    return (
                                        <div key={tool.id} className="flex items-center gap-4">
                                            <div className="flex items-center gap-2 w-8">
                                                {popularityIcon.isMedal ? (
                                                    <span className={`text-lg ${popularityIcon.color}`}>
                                                        {popularityIcon.icon}
                                                    </span>
                                                ) : (
                                                    <span className={`text-sm font-bold ${popularityIcon.color}`}>
                                                        #{index + 1}
                                                    </span>
                                                )}
                                            </div>

                                            <div className="min-w-0 flex-1">
                                                <div className="flex items-center justify-between mb-1">
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-medium text-white truncate">
                                                            {tool.name}
                                                        </span>
                                                        <span className={`px-2 py-1 text-xs font-medium rounded-full border ${getCategoryColor(tool.categoryName)}`}>
                                                            {tool.categoryName}
                                                        </span>
                                                    </div>
                                                    <span className="font-bold text-orange-400 text-sm">
                                                        {tool.totalLoans} préstamos
                                                    </span>
                                                </div>

                                                <div className="w-full bg-slate-700 rounded-full h-6 relative">
                                                    <div
                                                        className="bg-gradient-to-r from-orange-500 to-orange-400 h-6 rounded-full transition-all duration-500 flex items-center justify-end pr-2"
                                                        style={{ width: `${maxLoans > 0 ? (tool.totalLoans / maxLoans) * 100 : 0}%` }}
                                                    >
                                                        <span className="text-white text-xs font-medium">
                                                            {((tool.totalLoans / (processedData.summary.totalLoansAnalyzed || 1)) * 100).toFixed(1)}%
                                                        </span>
                                                    </div>
                                                </div>

                                                <div className="flex items-center justify-between mt-1 text-xs text-slate-400">
                                                    <span className="flex items-center gap-1">
                                                        <Users className="h-3 w-3" />
                                                        {tool.uniqueClients} clientes únicos
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <Clock className="h-3 w-3" />
                                                        {(tool.avgLoanDuration || 0).toFixed(1)} días promedio
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default PopularToolsReport;
