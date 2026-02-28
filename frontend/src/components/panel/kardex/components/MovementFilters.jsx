import React from 'react';
import { Search, Filter, X } from 'lucide-react';

const MovementFilters = ({ filters, tools, onChange }) => {
    const movementTypes = [
        { value: 'ALL', label: 'Todos los tipos' },
        { value: 'INITIAL_STOCK', label: 'Stock Inicial' },
        { value: 'LOAN', label: 'Préstamo' },
        { value: 'RETURN', label: 'Devolución' },
        { value: 'REPAIR', label: 'Reparación' },
        { value: 'DECOMMISSION', label: 'Baja' },
        { value: 'RESTOCK', label: 'Reabastecimiento' }
    ];

    const handleFilterChange = (field, value) => {
        onChange({ [field]: value });
    };

    const clearFilters = () => {
        onChange({
            search: '',
            type: 'ALL',
            tool: 'ALL',
            dateStart: '',
            dateEnd: ''
        });
    };

    const hasActiveFilters = filters.search || filters.type !== 'ALL' || filters.tool !== 'ALL' || filters.dateStart || filters.dateEnd;

    return (
        <div className="bg-slate-800/50 backdrop-blur rounded-lg p-6 border border-slate-700/50 mb-6">
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                    <Filter className="w-5 h-5 text-slate-400" />
                    <h3 className="text-lg font-medium text-slate-100">Filtros</h3>
                </div>

                {hasActiveFilters && (
                    <button
                        onClick={clearFilters}
                        className="flex items-center gap-2 px-3 py-1 text-sm text-slate-400 hover:text-slate-300 hover:bg-slate-700/50 rounded-lg transition-colors"
                    >
                        <X className="w-4 h-4" />
                        Limpiar filtros
                    </button>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                {/* Search */}
                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Buscar
                    </label>
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-500" />
                        <input
                            type="text"
                            placeholder="Buscar en descripción..."
                            value={filters.search}
                            onChange={(e) => handleFilterChange('search', e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                        />
                    </div>
                </div>

                {/* Movement Type */}
                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Tipo de Movimiento
                    </label>
                    <select
                        value={filters.type}
                        onChange={(e) => handleFilterChange('type', e.target.value)}
                        className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                    >
                        {movementTypes.map(type => (
                            <option key={type.value} value={type.value}>
                                {type.label}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Tool Filter */}
                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Herramienta
                    </label>
                    <select
                        value={filters.tool}
                        onChange={(e) => handleFilterChange('tool', e.target.value)}
                        className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                    >
                        <option value="ALL">Todas las herramientas</option>
                        {tools.map(tool => (
                            <option key={tool.id} value={tool.id}>
                                {tool.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Quick Actions */}
                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Filtros Rápidos
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                        <button
                            onClick={() => handleFilterChange('type', 'LOAN')}
                            className={`px-3 py-2 text-xs rounded-lg transition-colors ${
                                filters.type === 'LOAN'
                                    ? 'bg-red-500/20 text-red-400 border border-red-500/50'
                                    : 'bg-slate-700/50 text-slate-400 border border-slate-600 hover:bg-slate-600'
                            }`}
                        >
                            Solo Préstamos
                        </button>
                        <button
                            onClick={() => handleFilterChange('type', 'RETURN')}
                            className={`px-3 py-2 text-xs rounded-lg transition-colors ${
                                filters.type === 'RETURN'
                                    ? 'bg-green-500/20 text-green-400 border border-green-500/50'
                                    : 'bg-slate-700/50 text-slate-400 border border-slate-600 hover:bg-slate-600'
                            }`}
                        >
                            Solo Devoluciones
                        </button>
                    </div>
                </div>
            </div>

            {/* Date Range Filters */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Fecha desde
                    </label>
                    <input
                        type="date"
                        value={filters.dateStart}
                        onChange={(e) => handleFilterChange('dateStart', e.target.value)}
                        className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                        Fecha hasta
                    </label>
                    <input
                        type="date"
                        value={filters.dateEnd}
                        onChange={(e) => handleFilterChange('dateEnd', e.target.value)}
                        className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                    />
                </div>
            </div>

            {/* Active filters indicator */}
            {hasActiveFilters && (
                <div className="mt-4 pt-4 border-t border-slate-700/50">
                    <div className="flex flex-wrap gap-2">
                        {filters.search && (
                            <span className="px-2 py-1 bg-blue-500/20 text-blue-400 text-xs rounded border border-blue-500/50">
                                Texto: "{filters.search}"
                            </span>
                        )}
                        {filters.type !== 'ALL' && (
                            <span className="px-2 py-1 bg-purple-500/20 text-purple-400 text-xs rounded border border-purple-500/50">
                                Tipo: {movementTypes.find(t => t.value === filters.type)?.label}
                            </span>
                        )}
                        {filters.tool !== 'ALL' && (
                            <span className="px-2 py-1 bg-green-500/20 text-green-400 text-xs rounded border border-green-500/50">
                                Herramienta: {tools.find(t => t.id.toString() === filters.tool)?.name}
                            </span>
                        )}
                        {filters.dateStart && (
                            <span className="px-2 py-1 bg-orange-500/20 text-orange-400 text-xs rounded border border-orange-500/50">
                                Desde: {filters.dateStart}
                            </span>
                        )}
                        {filters.dateEnd && (
                            <span className="px-2 py-1 bg-orange-500/20 text-orange-400 text-xs rounded border border-orange-500/50">
                                Hasta: {filters.dateEnd}
                            </span>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default MovementFilters;
