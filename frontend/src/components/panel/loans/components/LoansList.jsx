// loans/components/LoansList.jsx - VERSIÓN CORREGIDA con formateo de fechas
import React, { useState, useMemo } from 'react';
import {
    Search,
    RefreshCw,
    Package2,
    Calendar,
    User,
    AlertTriangle,
    CheckCircle,
    DollarSign,
    FileText,
    Clock,
    Loader,
    ChevronLeft,
    ChevronRight
} from 'lucide-react';
import { formatDateLocal, daysBetween } from '../../../../utils/dateUtils';

const ITEMS_OPTIONS = [10, 15, 20];

const LoansList = ({
                       loans,
                       loading,
                       title = "Lista de Préstamos",
                       emptyMessage = "No hay préstamos",
                       onReturnTool,
                       onRefresh,
                       showReturnButton = false
                   }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('all');
    const [sortBy, setSortBy] = useState('loanDate');
    const [sortOrder, setSortOrder] = useState('desc');
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    const handleItemsPerPageChange = (newValue) => {
        setItemsPerPage(newValue);
        setCurrentPage(1);
    };

    // Filtrar y ordenar préstamos
    const filteredLoans = useMemo(() => {
        if (!loans) return [];

        let filtered = loans.filter(loan => {
            const matchesSearch =
                loan.client?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.tool?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.id?.toString().includes(searchTerm);

            // 🔧 CORRECCIÓN: Filtro de préstamos atrasados
            // Los préstamos atrasados son aquellos ACTIVOS con fecha vencida
            // El estado OVERDUE solo se usa para préstamos ya devueltos con retraso
            let matchesStatus = true;
            if (statusFilter === 'OVERDUE') {
                // Préstamos atrasados: ACTIVOS y con fecha de devolución vencida
                const today = new Date().toISOString().split('T')[0];
                matchesStatus = loan.status === 'ACTIVE' && loan.agreedReturnDate < today;
            } else if (statusFilter !== 'all') {
                matchesStatus = loan.status === statusFilter;
            }

            return matchesSearch && matchesStatus;
        });

        // Ordenar
        filtered.sort((a, b) => {
            let aValue = a[sortBy];
            let bValue = b[sortBy];

            // Manejar fechas
            if (sortBy.includes('Date')) {
                aValue = new Date(aValue);
                bValue = new Date(bValue);
            }

            // Manejar valores anidados
            if (sortBy === 'clientName') {
                aValue = a.client?.name || '';
                bValue = b.client?.name || '';
            } else if (sortBy === 'toolName') {
                aValue = a.tool?.name || '';
                bValue = b.tool?.name || '';
            }

            if (aValue < bValue) return sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        return filtered;
    }, [loans, searchTerm, statusFilter, sortBy, sortOrder]);

    const getStatusBadge = (status) => {
        const badges = {
            'ACTIVE': { color: 'bg-green-100 text-green-800', icon: Clock, label: 'Activo' },
            'RETURNED': { color: 'bg-blue-100 text-blue-800', icon: CheckCircle, label: 'Devuelto' },
            'OVERDUE': { color: 'bg-red-100 text-red-800', icon: AlertTriangle, label: 'Atrasado' },
            'DAMAGED': { color: 'bg-yellow-100 text-yellow-800', icon: AlertTriangle, label: 'Con Daños' }
        };

        const badge = badges[status] || { color: 'bg-slate-200 text-slate-800', icon: Clock, label: status };
        const Icon = badge.icon;

        return (
            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${badge.color}`}>
                <Icon className="h-3 w-3 mr-1" />
                {badge.label}
            </span>
        );
    };

    const isOverdue = (loan) => {
        if (loan.status !== 'ACTIVE') return false;
        const today = new Date().toISOString().split('T')[0];
        return loan.agreedReturnDate < today;
    };

    const getDaysUntilReturn = (loan) => {
        const today = new Date().toISOString().split('T')[0];
        return daysBetween(today, loan.agreedReturnDate);
    };

    if (loading) {
        return (
            <div className="bg-slate-800/60 backdrop-blur-sm rounded-lg border border-slate-700/50 p-8">
                <div className="flex items-center justify-center">
                    <Loader className="h-8 w-8 animate-spin text-orange-500 mr-3" />
                    <span className="text-slate-300">Cargando préstamos...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-slate-800/60 backdrop-blur-sm rounded-lg border border-slate-700/50 shadow-lg">
            {/* Header */}
            <div className="p-6 border-b border-slate-700/50">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-white">{title}</h3>
                    <button
                        onClick={onRefresh}
                        className="flex items-center px-3 py-1 bg-slate-700 text-slate-300 hover:text-white rounded hover:bg-slate-600 transition-colors"
                    >
                        <RefreshCw className="h-4 w-4 mr-1" />
                        Actualizar
                    </button>
                </div>

                {/* Filtros */}
                <div className="flex flex-col sm:flex-row gap-4">
                    {/* Búsqueda */}
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Buscar por cliente, herramienta o ID..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-700 border border-slate-600 rounded-md text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-orange-500"
                        />
                    </div>

                    {/* Filtro por estado */}
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                    >
                        <option value="all">Todos los estados</option>
                        <option value="ACTIVE">Activos</option>
                        <option value="RETURNED">Devueltos</option>
                        <option value="OVERDUE">Atrasados</option>
                        <option value="DAMAGED">Con Daños</option>
                    </select>

                    {/* Ordenamiento */}
                    <select
                        value={`${sortBy}-${sortOrder}`}
                        onChange={(e) => {
                            const [field, order] = e.target.value.split('-');
                            setSortBy(field);
                            setSortOrder(order);
                        }}
                        className="px-3 py-2 bg-slate-700 border border-slate-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                    >
                        <option value="loanDate-desc">Fecha préstamo (Más reciente)</option>
                        <option value="loanDate-asc">Fecha préstamo (Más antigua)</option>
                        <option value="agreedReturnDate-asc">Fecha devolución (Próxima)</option>
                        <option value="agreedReturnDate-desc">Fecha devolución (Lejana)</option>
                        <option value="clientName-asc">Cliente (A-Z)</option>
                        <option value="clientName-desc">Cliente (Z-A)</option>
                        <option value="toolName-asc">Herramienta (A-Z)</option>
                        <option value="toolName-desc">Herramienta (Z-A)</option>
                    </select>
                </div>

                {/* Contador de resultados */}
                <div className="mt-4 text-sm text-slate-400">
                    Mostrando {filteredLoans.length} de {loans?.length || 0} préstamos
                </div>
            </div>

            {/* Lista */}
            {(() => {
                const totalPages = Math.ceil(filteredLoans.length / itemsPerPage);
                const startIndex = (currentPage - 1) * itemsPerPage;
                const paginatedLoans = filteredLoans.slice(startIndex, startIndex + itemsPerPage);

                return (
                <>
            <div className="divide-y divide-slate-700/50">
                {filteredLoans.length === 0 ? (
                    <div className="p-8 text-center text-slate-400">
                        <Package2 className="h-12 w-12 mx-auto mb-4 text-slate-500" />
                        <p>{emptyMessage}</p>
                        {searchTerm && (
                            <p className="text-sm mt-2">
                                Intenta con otros términos de búsqueda
                            </p>
                        )}
                    </div>
                ) : (
                    paginatedLoans.map((loan) => {
                        const overdue = isOverdue(loan);
                        const daysUntilReturn = getDaysUntilReturn(loan);

                        return (
                            <div key={loan.id} className="p-6 hover:bg-slate-750 transition-colors">
                                <div className="flex items-start justify-between">
                                    {/* Información principal */}
                                    <div className="flex-1">
                                        <div className="flex items-center space-x-3 mb-2">
                                            <span className="text-white font-medium">#{loan.id}</span>
                                            {getStatusBadge(loan.status)}
                                            {overdue && (
                                                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-900 text-red-200">
                                                    <AlertTriangle className="h-3 w-3 mr-1" />
                                                    {Math.abs(daysUntilReturn)} días de atraso
                                                </span>
                                            )}
                                            {loan.status === 'ACTIVE' && !overdue && daysUntilReturn <= 3 && (
                                                <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-900 text-yellow-200">
                                                    <Clock className="h-3 w-3 mr-1" />
                                                    Vence en {daysUntilReturn} día(s)
                                                </span>
                                            )}
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
                                            <div>
                                                <span className="text-slate-400 flex items-center">
                                                    <User className="h-4 w-4 mr-1" />
                                                    Cliente:
                                                </span>
                                                <p className="text-white font-medium">{loan.client?.name}</p>
                                                <p className="text-slate-400 text-xs">{loan.client?.email}</p>
                                            </div>

                                            <div>
                                                <span className="text-slate-400 flex items-center">
                                                    <Package2 className="h-4 w-4 mr-1" />
                                                    Herramienta:
                                                </span>
                                                <p className="text-white font-medium">{loan.tool?.name}</p>
                                            </div>

                                            <div>
                                                <span className="text-slate-400 flex items-center">
                                                    <Calendar className="h-4 w-4 mr-1" />
                                                    Fechas:
                                                </span>
                                                <p className="text-white text-xs">
                                                    Préstamo: {formatDateLocal(loan.loanDate)}
                                                </p>
                                                <p className="text-white text-xs">
                                                    Devolución: {formatDateLocal(loan.agreedReturnDate)}
                                                </p>
                                                {loan.actualReturnDate && (
                                                    <p className="text-green-400 text-xs">
                                                        Devuelto: {formatDateLocal(loan.actualReturnDate)}
                                                    </p>
                                                )}
                                            </div>

                                            <div>
                                                <span className="text-slate-400 flex items-center">
                                                    <DollarSign className="h-4 w-4 mr-1" />
                                                    Tarifa:
                                                </span>
                                                <p className="text-white font-medium">${loan.dailyRate}/día</p>
                                                {loan.status === 'ACTIVE' && (
                                                    <p className="text-slate-400 text-xs">
                                                        {Math.max(0, -daysUntilReturn + 1)} día(s) transcurrido(s)
                                                    </p>
                                                )}
                                            </div>
                                        </div>

                                        {loan.notes && (
                                            <div className="mt-3">
                                                <span className="text-slate-400 text-sm flex items-center">
                                                    <FileText className="h-4 w-4 mr-1" />
                                                    Notas:
                                                </span>
                                                <p className="text-slate-300 text-sm bg-slate-700 rounded p-2 mt-1">
                                                    {loan.notes}
                                                </p>
                                            </div>
                                        )}
                                    </div>

                                    {/* Acciones */}
                                    <div className="flex items-center space-x-2 ml-4">
                                        {showReturnButton && loan.status === 'ACTIVE' && onReturnTool && (
                                            <button
                                                onClick={() => onReturnTool(loan)}
                                                className="flex items-center px-3 py-1 bg-orange-600 text-white rounded hover:bg-orange-700 transition-colors text-sm"
                                            >
                                                <Package2 className="h-4 w-4 mr-1" />
                                                Devolver
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {/* Paginación */}
            {filteredLoans.length > 0 && (
                <div className="px-6 py-4 border-t border-slate-700/50 flex flex-col sm:flex-row items-center justify-between gap-4 bg-slate-800/40">
                    <div className="flex items-center gap-3 text-sm text-slate-400">
                        <span>
                            Mostrando {startIndex + 1}–{Math.min(startIndex + itemsPerPage, filteredLoans.length)} de {filteredLoans.length} préstamos
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
                        <button onClick={() => setCurrentPage(1)} disabled={currentPage === 1}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs" title="Primera página">«</button>
                        <button onClick={() => setCurrentPage(p => p - 1)} disabled={currentPage === 1}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors" title="Página anterior">
                            <ChevronLeft className="h-4 w-4" />
                        </button>
                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(p => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 1)
                            .reduce((acc, p, idx, arr) => {
                                if (idx > 0 && p - arr[idx - 1] > 1) acc.push('...');
                                acc.push(p); return acc;
                            }, [])
                            .map((item, idx) =>
                                item === '...' ? (
                                    <span key={`e-${idx}`} className="px-2 text-slate-500 text-sm">…</span>
                                ) : (
                                    <button key={item} onClick={() => setCurrentPage(item)}
                                        className={`min-w-[2rem] h-8 rounded text-sm font-medium transition-colors ${currentPage === item ? 'bg-orange-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-700'}`}>
                                        {item}
                                    </button>
                                )
                            )
                        }
                        <button onClick={() => setCurrentPage(p => p + 1)} disabled={currentPage === totalPages}
                            className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors" title="Página siguiente">
                            <ChevronRight className="h-4 w-4" />
                        </button>
                        <button onClick={() => setCurrentPage(totalPages)} disabled={currentPage === totalPages}
                            className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs" title="Última página">»</button>
                    </div>
                </div>
            )}
            </>
                );
            })()}
        </div>
    );
};

export default LoansList;
