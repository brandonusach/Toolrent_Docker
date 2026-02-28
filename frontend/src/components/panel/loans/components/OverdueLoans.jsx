import React, { useState, useMemo, useEffect } from 'react';
import {
    AlertTriangle,
    Calendar,
    User,
    Package2,
    DollarSign,
    Phone,
    Mail,
    Clock,
    RefreshCw,
    Loader,
    Search,
    SortAsc,
    SortDesc
} from 'lucide-react';
import { formatDateLocal, daysBetween } from '../../../../utils/dateUtils';
import { useRates } from '../../rates/hooks/useRates';

const OverdueLoans = ({ loans, loading, onReturnTool, onRefresh }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [sortBy, setSortBy] = useState('daysOverdue');
    const [sortOrder, setSortOrder] = useState('desc');

    const {
        rates,
        getCurrentRates
    } = useRates();

    // Cargar tarifas actuales al montar el componente
    useEffect(() => {
        getCurrentRates();
    }, []);

    // Calcular días de atraso y procesar préstamos
    const processedLoans = useMemo(() => {
        if (!loans) return [];

        const today = new Date().toISOString().split('T')[0];

        // Obtener tarifa real de multas del sistema
        const lateFeeRate = rates.current?.LATE_FEE_RATE || 2000;

        return loans.map(loan => {
            const daysOverdue = Math.abs(daysBetween(loan.agreedReturnDate, today));
            const loanDuration = Math.abs(daysBetween(loan.loanDate, today));

            // 💰 ESTIMACIÓN de multa por atraso usando tarifa real del sistema
            // La multa REAL se calcula en el backend al procesar la devolución
            // Fórmula: daysLate * lateFeeRate (configurada en RateService)
            const estimatedLateFine = daysOverdue * lateFeeRate;

            return {
                ...loan,
                daysOverdue,
                loanDuration,
                estimatedLateFine,
                urgencyLevel: daysOverdue > 7 ? 'high' : daysOverdue > 3 ? 'medium' : 'low'
            };
        });
    }, [loans, rates.current]);

    // Filtrar y ordenar
    const filteredLoans = useMemo(() => {
        let filtered = processedLoans.filter(loan => {
            if (!searchTerm) return true;
            return (
                loan.client?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.tool?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.id?.toString().includes(searchTerm)
            );
        });

        // Ordenar
        filtered.sort((a, b) => {
            let aValue = a[sortBy];
            let bValue = b[sortBy];

            if (sortBy === 'clientName') {
                aValue = a.client?.name || '';
                bValue = b.client?.name || '';
            } else if (sortBy === 'toolName') {
                aValue = a.tool?.name || '';
                bValue = b.tool?.name || '';
            } else if (sortBy.includes('Date')) {
                aValue = new Date(aValue);
                bValue = new Date(bValue);
            }

            if (aValue < bValue) return sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        return filtered;
    }, [processedLoans, searchTerm, sortBy, sortOrder]);

    const getUrgencyColor = (urgencyLevel) => {
        switch (urgencyLevel) {
            case 'high':
                return 'text-red-400 bg-red-900 border-red-700';
            case 'medium':
                return 'text-yellow-400 bg-yellow-900 border-yellow-700';
            default:
                return 'text-orange-400 bg-orange-900 border-orange-700';
        }
    };

    const getUrgencyLabel = (urgencyLevel) => {
        switch (urgencyLevel) {
            case 'high':
                return 'Crítico';
            case 'medium':
                return 'Alto';
            default:
                return 'Medio';
        }
    };

    const handleSort = (field) => {
        if (sortBy === field) {
            setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
        } else {
            setSortBy(field);
            setSortOrder('desc');
        }
    };

    // Estadísticas rápidas
    const stats = useMemo(() => {
        const total = filteredLoans.length;
        const critical = filteredLoans.filter(l => l.urgencyLevel === 'high').length;
        const totalEstimatedFines = filteredLoans.reduce((sum, loan) => sum + loan.estimatedLateFine, 0);
        const avgDaysOverdue = total > 0
            ? filteredLoans.reduce((sum, loan) => sum + loan.daysOverdue, 0) / total
            : 0;

        return { total, critical, totalEstimatedFines, avgDaysOverdue };
    }, [filteredLoans]);

    if (loading) {
        return (
            <div className="bg-slate-800 rounded-lg border border-slate-700/50 p-8">
                <div className="flex items-center justify-center">
                    <Loader className="h-8 w-8 animate-spin text-red-500 mr-3" />
                    <span className="text-slate-300">Cargando préstamos atrasados...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-slate-800 rounded-lg border border-slate-700/50">
            {/* Header con estadísticas */}
            <div className="p-6 border-b border-slate-700/50">
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center">
                        <AlertTriangle className="h-6 w-6 text-red-400 mr-2" />
                        <h3 className="text-lg font-semibold text-white">Préstamos Atrasados</h3>
                    </div>
                    <button
                        onClick={onRefresh}
                        className="flex items-center px-3 py-1 bg-slate-700 text-white rounded hover:bg-slate-600 transition-colors"
                    >
                        <RefreshCw className="h-4 w-4 mr-1" />
                        Actualizar
                    </button>
                </div>

                {/* Estadísticas */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    <div className="bg-red-900 rounded-lg p-3 border border-red-700">
                        <div className="text-red-200 text-sm">Total Atrasados</div>
                        <div className="text-2xl font-bold text-white">{stats.total}</div>
                    </div>
                    <div className="bg-red-900 rounded-lg p-3 border border-red-700">
                        <div className="text-red-200 text-sm">Críticos (+7 días)</div>
                        <div className="text-2xl font-bold text-white">{stats.critical}</div>
                    </div>
                    <div className="bg-yellow-900 rounded-lg p-3 border border-yellow-700">
                        <div className="text-yellow-200 text-sm">Multas Estimadas</div>
                        <div className="text-2xl font-bold text-white">${stats.totalEstimatedFines.toFixed(2)}</div>
                    </div>
                    <div className="bg-orange-900 rounded-lg p-3 border border-orange-700">
                        <div className="text-orange-200 text-sm">Promedio Días</div>
                        <div className="text-2xl font-bold text-white">{stats.avgDaysOverdue.toFixed(1)}</div>
                    </div>
                </div>

                {/* Filtros y búsqueda */}
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Buscar por cliente, herramienta o ID..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                        />
                    </div>

                    <div className="flex items-center space-x-2">
                        <span className="text-slate-400 text-sm">Ordenar por:</span>
                        <button
                            onClick={() => handleSort('daysOverdue')}
                            className={`flex items-center px-3 py-2 rounded-md text-sm transition-colors ${
                                sortBy === 'daysOverdue'
                                    ? 'bg-red-600 text-white'
                                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                            }`}
                        >
                            Días atraso
                            {sortBy === 'daysOverdue' && (
                                sortOrder === 'asc' ? <SortAsc className="h-4 w-4 ml-1" /> : <SortDesc className="h-4 w-4 ml-1" />
                            )}
                        </button>
                        <button
                            onClick={() => handleSort('clientName')}
                            className={`flex items-center px-3 py-2 rounded-md text-sm transition-colors ${
                                sortBy === 'clientName'
                                    ? 'bg-red-600 text-white'
                                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                            }`}
                        >
                            Cliente
                            {sortBy === 'clientName' && (
                                sortOrder === 'asc' ? <SortAsc className="h-4 w-4 ml-1" /> : <SortDesc className="h-4 w-4 ml-1" />
                            )}
                        </button>
                        <button
                            onClick={() => handleSort('estimatedLateFine')}
                            className={`flex items-center px-3 py-2 rounded-md text-sm transition-colors ${
                                sortBy === 'estimatedLateFine'
                                    ? 'bg-red-600 text-white'
                                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                            }`}
                        >
                            Multa est.
                            {sortBy === 'estimatedLateFine' && (
                                sortOrder === 'asc' ? <SortAsc className="h-4 w-4 ml-1" /> : <SortDesc className="h-4 w-4 ml-1" />
                            )}
                        </button>
                    </div>
                </div>
            </div>

            {/* Lista de préstamos atrasados */}
            <div className="divide-y divide-slate-700/50">
                {filteredLoans.length === 0 ? (
                    <div className="p-8 text-center text-slate-400">
                        <AlertTriangle className="h-12 w-12 mx-auto mb-4 text-slate-500" />
                        <p>No hay préstamos atrasados</p>
                        {searchTerm && (
                            <p className="text-sm mt-2">
                                Intenta con otros términos de búsqueda
                            </p>
                        )}
                    </div>
                ) : (
                    filteredLoans.map((loan) => (
                        <div key={loan.id} className="p-6 hover:bg-slate-750 transition-colors">
                            <div className="flex items-start justify-between">
                                {/* Información principal */}
                                <div className="flex-1">
                                    <div className="flex items-center space-x-3 mb-3">
                                        <span className="text-white font-bold">#{loan.id}</span>

                                        {/* Badge de urgencia */}
                                        <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${getUrgencyColor(loan.urgencyLevel)}`}>
                                            <AlertTriangle className="h-4 w-4 mr-1" />
                                            {getUrgencyLabel(loan.urgencyLevel)} - {loan.daysOverdue} días
                                        </span>

                                        {/* Multa estimada */}
                                        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-900 text-purple-200 border border-purple-700">
                                            <DollarSign className="h-3 w-3 mr-1" />
                                            ${loan.estimatedLateFine.toFixed(2)} est.
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
                                        {/* Cliente */}
                                        <div className="bg-slate-700 rounded-lg p-3">
                                            <span className="text-slate-400 flex items-center mb-1">
                                                <User className="h-4 w-4 mr-1" />
                                                Cliente:
                                            </span>
                                            <p className="text-white font-medium">{loan.client?.name}</p>
                                            <div className="flex items-center text-xs text-slate-400 mt-1">
                                                <Mail className="h-3 w-3 mr-1" />
                                                {loan.client?.email}
                                            </div>
                                            {loan.client?.phone && (
                                                <div className="flex items-center text-xs text-slate-400 mt-1">
                                                    <Phone className="h-3 w-3 mr-1" />
                                                    {loan.client.phone}
                                                </div>
                                            )}
                                        </div>

                                        {/* Herramienta */}
                                        <div className="bg-slate-700 rounded-lg p-3">
                                            <span className="text-slate-400 flex items-center mb-1">
                                                <Package2 className="h-4 w-4 mr-1" />
                                                Herramienta:
                                            </span>
                                            <p className="text-white font-medium">{loan.tool?.name}</p>
                                            <p className="text-slate-400 text-xs">Valor: ${loan.tool?.replacementValue}</p>
                                        </div>

                                        {/* Fechas y tiempo */}
                                        <div className="bg-slate-700 rounded-lg p-3">
                                            <span className="text-slate-400 flex items-center mb-1">
                                                <Calendar className="h-4 w-4 mr-1" />
                                                Fechas:
                                            </span>
                                            <p className="text-white text-xs">
                                                Préstamo: {formatDateLocal(loan.loanDate)}
                                            </p>
                                            <p className="text-red-400 text-xs font-medium">
                                                Debía devolver: {formatDateLocal(loan.agreedReturnDate)}
                                            </p>
                                            <p className="text-slate-400 text-xs">
                                                <Clock className="h-3 w-3 inline mr-1" />
                                                Duración: {loan.loanDuration} días
                                            </p>
                                        </div>
                                    </div>

                                    {/* Notas del préstamo */}
                                    {loan.notes && (
                                        <div className="mt-3 bg-slate-700 rounded-lg p-3">
                                            <span className="text-slate-400 text-sm font-medium">Notas:</span>
                                            <p className="text-slate-300 text-sm mt-1">{loan.notes}</p>
                                        </div>
                                    )}

                                    {/* Información de costos */}
                                    <div className="mt-3 bg-yellow-900 border border-yellow-700 rounded-lg p-3">
                                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
                                            <div>
                                                <span className="text-yellow-200">Tarifa diaria:</span>
                                                <p className="text-white font-medium">${loan.dailyRate}</p>
                                            </div>
                                            <div>
                                                <span className="text-yellow-200">Días prestado:</span>
                                                <p className="text-white font-medium">{loan.loanDuration}</p>
                                            </div>
                                            <div>
                                                <span className="text-yellow-200">Días atraso:</span>
                                                <p className="text-red-400 font-medium">{loan.daysOverdue}</p>
                                            </div>
                                            <div>
                                                <span className="text-yellow-200">Multa estimada:</span>
                                                <p className="text-red-400 font-medium">${loan.estimatedLateFine.toFixed(2)}</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* Acciones */}
                                <div className="flex flex-col items-end space-y-2 ml-4">
                                    <button
                                        onClick={() => onReturnTool(loan)}
                                        className="flex items-center px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition-colors text-sm font-medium"
                                    >
                                        <Package2 className="h-4 w-4 mr-2" />
                                        Procesar Devolución
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default OverdueLoans;
