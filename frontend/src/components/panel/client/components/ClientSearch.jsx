import React, { useState, useEffect, useCallback } from 'react';
import { Search, X, RotateCcw } from 'lucide-react';

const ClientSearch = ({ onSearch, onClear }) => {
    const [searchCriteria, setSearchCriteria] = useState({
        general: '',
        status: 'ALL'
    });

    const [activeFiltersCount, setActiveFiltersCount] = useState(0);

    // Debounced search
    const [debouncedCriteria, setDebouncedCriteria] = useState(searchCriteria);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedCriteria(searchCriteria);
        }, 300);

        return () => clearTimeout(timer);
    }, [searchCriteria]);

    // Execute search when debounced criteria changes
    useEffect(() => {
        if (onSearch) {
            onSearch(debouncedCriteria);
        }
    }, [debouncedCriteria, onSearch]);

    // Count active filters
    useEffect(() => {
        let count = 0;
        if (searchCriteria.general && searchCriteria.general.trim() !== '') count++;
        if (searchCriteria.status && searchCriteria.status !== 'ALL') count++;
        setActiveFiltersCount(count);
    }, [searchCriteria]);

    const handleInputChange = useCallback((field, value) => {
        setSearchCriteria(prev => ({
            ...prev,
            [field]: value
        }));
    }, []);

    const clearAllFilters = useCallback(() => {
        const clearedCriteria = {
            general: '',
            status: 'ALL'
        };
        setSearchCriteria(clearedCriteria);
        if (onClear) {
            onClear();
        }
    }, [onClear]);


    return (
        <div className="bg-slate-800 rounded-xl border border-slate-700/50">
            <div className="p-6">
                <div className="flex flex-col sm:flex-row gap-4">
                    {/* General Search */}
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-3 text-slate-400" size={20} />
                        <input
                            type="text"
                            placeholder="Buscar por nombre o RUT..."
                            value={searchCriteria.general}
                            onChange={(e) => handleInputChange('general', e.target.value)}
                            className="w-full pl-10 pr-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-all duration-200"
                        />
                        {searchCriteria.general && (
                            <button
                                onClick={() => handleInputChange('general', '')}
                                className="absolute right-3 top-3 text-slate-400 hover:text-white"
                            >
                                <X size={16} />
                            </button>
                        )}
                    </div>

                    {/* Estado Filter */}
                    <div className="sm:w-64">
                        <select
                            value={searchCriteria.status}
                            onChange={(e) => handleInputChange('status', e.target.value)}
                            className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-orange-500 transition-all duration-200"
                        >
                            <option value="ALL">Todos los estados</option>
                            <option value="ACTIVE">Solo Activos</option>
                            <option value="RESTRICTED">Solo Restringidos</option>
                        </select>
                    </div>

                    {/* Clear Button */}
                    {activeFiltersCount > 0 && (
                        <button
                            onClick={clearAllFilters}
                            className="px-4 py-3 bg-red-600 border border-red-600 rounded-lg text-white hover:bg-red-700 transition-colors duration-200 flex items-center gap-2 justify-center"
                        >
                            <RotateCcw size={20} />
                            <span className="hidden sm:inline">Limpiar</span>
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ClientSearch;
