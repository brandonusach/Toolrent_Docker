// inventory/components/ToolList.jsx - PURE VERSION
import React, { useState } from 'react';
import {
    Plus, Edit2, Eye, Package, AlertTriangle,
    Search, RefreshCw, TrendingUp, TrendingDown,
    ChevronLeft, ChevronRight
} from 'lucide-react';

const ITEMS_OPTIONS = [10, 15, 20];

const ToolList = ({
                      tools,
                      categories,
                      loading,
                      searchTerm,
                      setSearchTerm,
                      categoryFilter,
                      setCategoryFilter,
                      onViewInstances,
                      onEditTool,
                      onAddStock,
                      onDecommission,
                      onAddNew,
                      onRefresh
                  }) => {

    // Simple client-side filtering (only UI logic)
    const filteredTools = tools.filter(tool => {
        const matchesSearch = !searchTerm ||
            tool.name.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesCategory = categoryFilter === 'ALL' ||
            (tool.category && tool.category.id.toString() === categoryFilter);
        return matchesSearch && matchesCategory;
    });

    // Paginación
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    const totalPages = Math.ceil(filteredTools.length / itemsPerPage);
    const startIndex = (currentPage - 1) * itemsPerPage;
    const paginatedTools = filteredTools.slice(startIndex, startIndex + itemsPerPage);

    const handleItemsPerPageChange = (newValue) => {
        setItemsPerPage(newValue);
        setCurrentPage(1);
    };

    return (
        <div>
            {/* Controls */}
            <div className="flex flex-col sm:flex-row gap-4 mb-6">
                <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 h-4 w-4" />
                    <input
                        type="text"
                        placeholder="Buscar herramientas..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-orange-500"
                    />
                </div>

                <select
                    value={categoryFilter}
                    onChange={(e) => setCategoryFilter(e.target.value)}
                    className="px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-orange-500"
                >
                    <option value="ALL">Todas las Categorías</option>
                    {categories.map(category => (
                        <option key={category.id} value={category.id}>{category.name}</option>
                    ))}
                </select>

                <button
                    onClick={onAddNew}
                    className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-5 py-2.5 rounded-lg font-semibold transition-colors duration-200 shadow-lg hover:shadow-orange-500/30"
                >
                    <Plus className="h-4 w-4" />
                    Nueva Herramienta
                </button>

                <button
                    onClick={onRefresh}
                    disabled={loading}
                    className="px-4 py-2 bg-slate-700 text-slate-300 rounded-lg hover:bg-slate-600 transition-colors flex items-center"
                    title="Actualizar lista"
                >
                    <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </button>
            </div>

            {/* Tools Table */}
            <div className="bg-slate-800/60 backdrop-blur-sm rounded-lg border border-slate-700/50 overflow-hidden shadow-lg">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-slate-700/50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Herramienta
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Categoría
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Stock
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Tarifa/Día
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Valor Reposición
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Acciones
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700/50">
                        {paginatedTools.map((tool) => (
                            <tr key={tool.id} className="hover:bg-slate-700/30 transition-colors">
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <div className="text-white font-medium">{tool.name}</div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <div className="text-slate-300">{tool.category?.name || 'Sin categoría'}</div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <div className="flex items-center">
                                        <span className={`text-sm font-medium ${
                                            tool.currentStock <= 0
                                                ? 'text-red-400'
                                                : tool.currentStock <= 2
                                                    ? 'text-orange-400'
                                                    : 'text-slate-300'
                                        }`}>
                                            {tool.currentStock || 0} / {tool.initialStock || 0}
                                        </span>
                                        {tool.currentStock <= 2 && (
                                            <AlertTriangle className="h-4 w-4 text-orange-400 ml-2" />
                                        )}
                                    </div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-slate-300">
                                    ${(tool.rentalRate || 0).toLocaleString('es-CL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-slate-300">
                                    ${(tool.replacementValue || 0).toLocaleString('es-CL')}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                    <div className="flex space-x-2">
                                        <button
                                            onClick={() => onViewInstances(tool)}
                                            className="text-blue-400 hover:text-blue-300 p-1 rounded"
                                            title="Ver Herramientas"
                                        >
                                            <Eye className="h-4 w-4" />
                                        </button>
                                        <button
                                            onClick={() => onEditTool(tool)}
                                            className="text-yellow-400 hover:text-yellow-300 p-1 rounded"
                                            title="Editar"
                                        >
                                            <Edit2 className="h-4 w-4" />
                                        </button>
                                        <button
                                            onClick={() => onAddStock(tool)}
                                            className="text-green-400 hover:text-green-300 p-1 rounded"
                                            title="Agregar Stock"
                                        >
                                            <TrendingUp className="h-4 w-4" />
                                        </button>
                                        <button
                                            onClick={() => onDecommission(tool)}
                                            className="text-orange-400 hover:text-orange-300 p-1 rounded"
                                            title="Dar de Baja (Eliminar)"
                                        >
                                            <TrendingDown className="h-4 w-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                {filteredTools.length === 0 && (
                    <div className="text-center py-12">
                        <Package className="h-12 w-12 text-slate-500 mx-auto mb-4" />
                        <p className="text-slate-400">
                            {loading ? 'Cargando herramientas...' : 'No se encontraron herramientas'}
                        </p>
                    </div>
                )}
            </div>

            {/* Paginación */}
            {filteredTools.length > 0 && (
                <div className="px-6 py-4 border-t border-slate-700/50 flex flex-col sm:flex-row items-center justify-between gap-4 bg-slate-800/40">
                    <div className="flex items-center gap-3 text-sm text-slate-400">
                        <span>
                            Mostrando {startIndex + 1}–{Math.min(startIndex + itemsPerPage, filteredTools.length)} de {filteredTools.length} herramientas
                        </span>
                        <span className="text-slate-600">|</span>
                        <span>Filas por página:</span>
                        <select
                            value={itemsPerPage}
                            onChange={(e) => handleItemsPerPageChange(Number(e.target.value))}
                            className="bg-slate-700 border border-slate-600 text-white rounded px-2 py-1 text-sm focus:outline-none focus:ring-1 focus:ring-orange-500"
                        >
                            {ITEMS_OPTIONS.map(n => (
                                <option key={n} value={n}>{n}</option>
                            ))}
                        </select>
                    </div>

                    <div className="flex items-center gap-1">
                        <button
                            onClick={() => setCurrentPage(1)}
                            disabled={currentPage === 1}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs"
                            title="Primera página"
                        >«</button>
                        <button
                            onClick={() => setCurrentPage(p => p - 1)}
                            disabled={currentPage === 1}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                            title="Página anterior"
                        >
                            <ChevronLeft className="h-4 w-4" />
                        </button>

                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(p => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 1)
                            .reduce((acc, p, idx, arr) => {
                                if (idx > 0 && p - arr[idx - 1] > 1) acc.push('...');
                                acc.push(p);
                                return acc;
                            }, [])
                            .map((item, idx) =>
                                item === '...' ? (
                                    <span key={`e-${idx}`} className="px-2 text-slate-500 text-sm">…</span>
                                ) : (
                                    <button
                                        key={item}
                                        onClick={() => setCurrentPage(item)}
                                        className={`min-w-[2rem] h-8 rounded text-sm font-medium transition-colors ${
                                            currentPage === item
                                                ? 'bg-orange-600 text-white'
                                                : 'text-slate-400 hover:text-white hover:bg-slate-700'
                                        }`}
                                    >{item}</button>
                                )
                            )
                        }

                        <button
                            onClick={() => setCurrentPage(p => p + 1)}
                            disabled={currentPage === totalPages}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                            title="Página siguiente"
                        >
                            <ChevronRight className="h-4 w-4" />
                        </button>
                        <button
                            onClick={() => setCurrentPage(totalPages)}
                            disabled={currentPage === totalPages}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs"
                            title="Última página"
                        >»</button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ToolList;