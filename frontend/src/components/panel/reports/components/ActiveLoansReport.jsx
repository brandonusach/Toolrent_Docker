// ActiveLoansReport.jsx - RF6.1: Listar préstamos activos y estado (vigentes, atrasados)
import React, { useState, useMemo } from 'react';
import { RefreshCw, AlertTriangle, Search, ArrowUpDown, ArrowUp, ArrowDown, ChevronLeft, ChevronRight } from 'lucide-react';
import { formatDateLocal } from '../../../../utils/dateUtils';

const ITEMS_OPTIONS = [10, 15, 20];

const ActiveLoansReport = ({ data, loading }) => {
    const [sortField, setSortField] = useState('daysOverdue');
    const [sortDirection, setSortDirection] = useState('desc');
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    const handleItemsPerPageChange = (val) => { setItemsPerPage(val); setCurrentPage(1); };

    const processedData = useMemo(() => {
        if (!data || !data.loans) return { loans: [], summary: {} };
        let filteredLoans = [...data.loans];
        if (filterStatus !== 'ALL') filteredLoans = filteredLoans.filter(loan => loan.status === filterStatus);
        if (searchTerm) {
            filteredLoans = filteredLoans.filter(loan =>
                loan.clientName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.toolName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                loan.categoryName?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        filteredLoans.sort((a, b) => {
            let aValue = a[sortField];
            let bValue = b[sortField];
            if (sortField === 'daysOverdue' || sortField === 'quantity') {
                aValue = Number(aValue) || 0; bValue = Number(bValue) || 0;
            } else if (sortField === 'agreedReturnDate' || sortField === 'loanDate') {
                aValue = new Date(aValue); bValue = new Date(bValue);
            } else {
                aValue = String(aValue || '').toLowerCase(); bValue = String(bValue || '').toLowerCase();
            }
            if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
            return 0;
        });
        return {
            loans: filteredLoans,
            summary: {
                ...data.summary,
                filtered: {
                    total: filteredLoans.length,
                    active: filteredLoans.filter(l => l.status === 'ACTIVE').length,
                    overdue: filteredLoans.filter(l => l.status === 'OVERDUE').length
                }
            }
        };
    }, [data, filterStatus, searchTerm, sortField, sortDirection]);

    const handleSort = (field) => {
        if (sortField === field) setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        else { setSortField(field); setSortDirection('desc'); }
    };
    const getSortIcon = (field) => {
        if (sortField !== field) return ArrowUpDown;
        return sortDirection === 'asc' ? ArrowUp : ArrowDown;
    };
    const getStatusBadge = (loan) => {
        if (loan.status === 'OVERDUE') return (
            <span className="px-2 py-1 bg-red-500/10 text-red-400 rounded-full text-xs font-medium flex items-center gap-1 border border-red-500/20">
                <AlertTriangle className="h-3 w-3" />Atrasado ({loan.daysOverdue} días)
            </span>
        );
        return (
            <span className="px-2 py-1 bg-green-500/10 text-green-400 rounded-full text-xs font-medium flex items-center gap-1 border border-green-500/20">
                <RefreshCw className="h-3 w-3" />Vigente
            </span>
        );
    };
    const getPriorityColor = (loan) => {
        if (loan.daysOverdue > 7) return 'border-l-red-500 bg-red-500/5';
        if (loan.daysOverdue > 3) return 'border-l-yellow-500 bg-yellow-500/5';
        return 'border-l-orange-500 bg-orange-500/5';
    };

    // Paginación calculada antes del return
    const totalPages = Math.ceil(processedData.loans.length / itemsPerPage);
    const pageStart = (currentPage - 1) * itemsPerPage;
    const paginatedLoans = processedData.loans.slice(pageStart, pageStart + itemsPerPage);

    if (loading) return (
        <div className="p-6"><div className="animate-pulse space-y-4">
            <div className="h-4 bg-slate-700 rounded w-1/4"></div>
            <div className="h-32 bg-slate-700 rounded"></div>
        </div></div>
    );

    if (!data || !data.loans) return (
        <div className="p-6"><div className="text-center py-12">
            <RefreshCw className="h-16 w-16 text-slate-600 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-white mb-2">No hay datos disponibles</h3>
            <p className="text-slate-400">Los datos de préstamos activos se cargarán automáticamente.</p>
        </div></div>
    );

    return (
        <div className="p-6">
            {/* Header y Resumen */}
            <div className="mb-6">
                <h2 className="text-xl font-bold text-white flex items-center gap-2 mb-4">
                    <RefreshCw className="h-5 w-5 text-orange-500" />Préstamos Activos
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    {[
                        { label: 'Total', value: processedData.summary.filtered?.total || 0, icon: RefreshCw, color: 'orange' },
                        { label: 'Vigentes', value: processedData.summary.filtered?.active || 0, icon: RefreshCw, color: 'green' },
                        { label: 'Atrasados', value: processedData.summary.filtered?.overdue || 0, icon: AlertTriangle, color: 'red' },
                        { label: 'Prom. Atraso', value: `${(processedData.summary.avgDaysOverdue || 0).toFixed(1)} días`, icon: AlertTriangle, color: 'yellow' },
                    ].map(({ label, value, icon: Icon, color }) => (
                        <div key={label} className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                            <div className="flex items-center gap-3">
                                <div className={`bg-${color}-500/10 p-2 rounded-lg`}>
                                    <Icon className={`h-5 w-5 text-${color}-500`} />
                                </div>
                                <div>
                                    <p className="text-sm text-slate-400 font-medium">{label}</p>
                                    <p className="text-2xl font-bold text-white">{value}</p>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
                {/* Filtros */}
                <div className="flex flex-col sm:flex-row gap-4 mb-6">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input type="text" placeholder="Buscar por cliente, herramienta o categoría..."
                            value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 text-white placeholder-slate-400" />
                    </div>
                    <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
                        className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 text-white">
                        <option value="ALL">Todos los estados</option>
                        <option value="ACTIVE">Solo vigentes</option>
                        <option value="OVERDUE">Solo atrasados</option>
                    </select>
                </div>
            </div>

            {processedData.loans.length === 0 ? (
                <div className="text-center py-12">
                    <Search className="h-12 w-12 text-slate-600 mx-auto mb-4" />
                    <p className="text-slate-400">No se encontraron préstamos con los filtros aplicados</p>
                </div>
            ) : (
                <div>
                    <div className="overflow-x-auto">
                        <table className="min-w-full bg-slate-800/30 backdrop-blur-sm border border-slate-700 rounded-lg">
                            <thead className="bg-slate-800/50">
                                <tr>
                                    {[
                                        { key: 'clientName', label: 'Cliente' },
                                        { key: 'toolName', label: 'Herramienta' },
                                        { key: 'quantity', label: 'Cantidad' },
                                        { key: 'loanDate', label: 'F. Préstamo' },
                                        { key: 'agreedReturnDate', label: 'F. Devolución' },
                                        { key: 'daysOverdue', label: 'Estado' },
                                    ].map((col) => {
                                        const SortIcon = getSortIcon(col.key);
                                        return (
                                            <th key={col.key} onClick={() => handleSort(col.key)}
                                                className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider cursor-pointer hover:bg-slate-700/50 transition-colors">
                                                <div className="flex items-center gap-1">{col.label}<SortIcon className="h-3 w-3" /></div>
                                            </th>
                                        );
                                    })}
                                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">Notas</th>
                                </tr>
                            </thead>
                            <tbody className="bg-slate-800/20 divide-y divide-slate-700">
                                {paginatedLoans.map((loan) => (
                                    <tr key={loan.id} className={`hover:bg-slate-700/30 border-l-4 ${getPriorityColor(loan)} transition-colors`}>
                                        <td className="px-4 py-4 whitespace-nowrap">
                                            <div className="text-sm font-medium text-white">{loan.clientName}</div>
                                        </td>
                                        <td className="px-4 py-4 whitespace-nowrap">
                                            <div className="text-sm font-medium text-white">{loan.toolName}</div>
                                            <div className="text-sm text-slate-400">{loan.categoryName}</div>
                                        </td>
                                        <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">{loan.quantity}</td>
                                        <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">{formatDateLocal(loan.loanDate)}</td>
                                        <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">{formatDateLocal(loan.agreedReturnDate)}</td>
                                        <td className="px-4 py-4 whitespace-nowrap">{getStatusBadge(loan)}</td>
                                        <td className="px-4 py-4 text-sm text-slate-300 max-w-xs">
                                            <div className="truncate" title={loan.notes}>{loan.notes || 'Sin notas'}</div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Paginación */}
                    {processedData.loans.length > 0 && (
                        <div className="px-4 py-3 border-t border-slate-700/50 flex flex-col sm:flex-row items-center justify-between gap-3 bg-slate-800/40 mt-0">
                            <div className="flex items-center gap-3 text-sm text-slate-400">
                                <span>Mostrando {pageStart + 1}–{Math.min(pageStart + itemsPerPage, processedData.loans.length)} de {processedData.loans.length} préstamos</span>
                                <span className="text-slate-600">|</span>
                                <span>Filas:</span>
                                <select value={itemsPerPage} onChange={(e) => handleItemsPerPageChange(Number(e.target.value))}
                                    className="bg-slate-700 border border-slate-600 text-white rounded px-2 py-1 text-sm focus:outline-none focus:ring-1 focus:ring-orange-500">
                                    {ITEMS_OPTIONS.map(n => <option key={n} value={n}>{n}</option>)}
                                </select>
                            </div>
                            <div className="flex items-center gap-1">
                                <button onClick={() => setCurrentPage(1)} disabled={currentPage === 1}
                                    className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs">«</button>
                                <button onClick={() => setCurrentPage(p => p - 1)} disabled={currentPage === 1}
                                    className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                                    <ChevronLeft className="h-4 w-4" />
                                </button>
                                {Array.from({ length: totalPages }, (_, i) => i + 1)
                                    .filter(p => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 1)
                                    .reduce((acc, p, idx, arr) => { if (idx > 0 && p - arr[idx - 1] > 1) acc.push('...'); acc.push(p); return acc; }, [])
                                    .map((item, idx) => item === '...'
                                        ? <span key={`e${idx}`} className="px-2 text-slate-500 text-sm">…</span>
                                        : <button key={item} onClick={() => setCurrentPage(item)}
                                            className={`min-w-[2rem] h-8 rounded text-sm font-medium transition-colors ${currentPage === item ? 'bg-orange-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-700'}`}>{item}</button>
                                    )}
                                <button onClick={() => setCurrentPage(p => p + 1)} disabled={currentPage === totalPages}
                                    className="p-1.5 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                                    <ChevronRight className="h-4 w-4" />
                                </button>
                                <button onClick={() => setCurrentPage(totalPages)} disabled={currentPage === totalPages}
                                    className="px-2 py-1 rounded text-slate-400 hover:text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-xs">»</button>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default ActiveLoansReport;
