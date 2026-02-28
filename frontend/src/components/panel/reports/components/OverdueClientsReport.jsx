// OverdueClientsReport.jsx - RF6.2: Listar clientes con atrasos
import React, { useState, useMemo } from 'react';
import { AlertTriangle, Users, Search, ArrowUpDown, ArrowUp, ArrowDown, ChevronDown, ChevronUp, ChevronLeft, ChevronRight } from 'lucide-react';

const ITEMS_OPTIONS = [10, 15, 20];

const OverdueClientsReport = ({ data, loading }) => {
    const [sortField, setSortField] = useState('maxDaysOverdue');
    const [sortDirection, setSortDirection] = useState('desc');
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedClient, setSelectedClient] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(10);

    const handleItemsPerPageChange = (val) => { setItemsPerPage(val); setCurrentPage(1); };

    // Procesar y filtrar datos
    const processedData = useMemo(() => {
        if (!data || !data.clients) return { clients: [], summary: {} };

        let filteredClients = [...data.clients];

        // Filtrar por búsqueda
        if (searchTerm) {
            filteredClients = filteredClients.filter(client =>
                client.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                client.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                client.phone?.includes(searchTerm)
            );
        }

        // Ordenar
        filteredClients.sort((a, b) => {
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

        return {
            clients: filteredClients,
            summary: {
                ...data.summary,
                filtered: {
                    totalClients: filteredClients.length,
                    totalOverdueAmount: filteredClients.reduce((acc, client) => acc + (client.totalOverdueAmount || 0), 0),
                    totalOverdueLoans: filteredClients.reduce((acc, client) => acc + (client.loansCount || 0), 0)
                }
            }
        };
    }, [data, searchTerm, sortField, sortDirection]);

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

    const getRiskLevel = (client) => {
        if (client.maxDaysOverdue > 15) {
            return { level: 'ALTO', color: 'bg-red-500/10 text-red-400 border-red-500/20', icon: AlertTriangle };
        } else if (client.maxDaysOverdue > 7) {
            return { level: 'MEDIO', color: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20', icon: AlertTriangle };
        } else {
            return { level: 'BAJO', color: 'bg-orange-500/10 text-orange-400 border-orange-500/20', icon: AlertTriangle };
        }
    };

    const handleClientDetail = (client) => {
        setSelectedClient(selectedClient?.id === client.id ? null : client);
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

    if (!data || !data.clients) {
        return (
            <div className="p-6">
                <div className="text-center py-12">
                    <AlertTriangle className="h-16 w-16 text-slate-600 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-white mb-2">
                        No hay datos disponibles
                    </h3>
                    <p className="text-slate-400">
                        Los datos de clientes con atrasos se cargarán automáticamente.
                    </p>
                </div>
            </div>
        );
    }

    // Paginación calculada antes del return
    const totalPages = Math.ceil(processedData.clients.length / itemsPerPage);
    const pageStart = (currentPage - 1) * itemsPerPage;
    const paginatedClients = processedData.clients.slice(pageStart, pageStart + itemsPerPage);

    return (
        <div className="p-6">
            {/* Header y Resumen */}
            <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <AlertTriangle className="h-5 w-5 text-red-500" />
                        Clientes con Atrasos
                    </h2>
                </div>

                {/* Resumen de estadísticas */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-red-500/10 p-2 rounded-lg">
                                <Users className="h-5 w-5 text-red-500" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Clientes Morosos</p>
                                <p className="text-2xl font-bold text-white">
                                    {processedData.summary.filtered?.totalClients || 0}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-orange-500/10 p-2 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-orange-500" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Préstamos Atrasados</p>
                                <p className="text-2xl font-bold text-white">
                                    {processedData.summary.filtered?.totalOverdueLoans || 0}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-yellow-500/10 p-2 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-yellow-500" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Multas Acumuladas</p>
                                <p className="text-2xl font-bold text-white">
                                    ${processedData.summary.filtered?.totalOverdueAmount || 0}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="flex items-center gap-3">
                            <div className="bg-purple-500/10 p-2 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-purple-400" />
                            </div>
                            <div>
                                <p className="text-sm text-slate-400 font-medium">Promedio Atraso</p>
                                <p className="text-2xl font-bold text-white">
                                    {(processedData.summary.avgDaysOverdue || 0).toFixed(1)} días
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Búsqueda */}
                <div className="mb-6">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Buscar por nombre, email o teléfono..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent text-white placeholder-slate-400"
                        />
                    </div>
                </div>
            </div>

            {/* Tabla de Clientes */}
            {processedData.clients.length === 0 ? (
                <div className="text-center py-12">
                    <Users className="h-12 w-12 text-slate-600 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-white mb-2">¡Excelente!</h3>
                    <p className="text-slate-400">No hay clientes con atrasos</p>
                </div>
            ) : (
                <div className="space-y-4">
                    <div className="overflow-x-auto">
                        <table className="min-w-full bg-slate-800/30 backdrop-blur-sm border border-slate-700 rounded-lg">
                            <thead className="bg-slate-800/50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">
                                        Riesgo
                                    </th>
                                    {[
                                        { key: 'name', label: 'Cliente' },
                                        { key: 'loansCount', label: 'Préstamos' },
                                        { key: 'maxDaysOverdue', label: 'Máx. Atraso' },
                                        { key: 'avgDaysOverdue', label: 'Prom. Atraso' },
                                        { key: 'totalOverdueAmount', label: 'Multas' }
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
                                        Contacto
                                    </th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-300 uppercase tracking-wider">
                                        Acciones
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="bg-slate-800/20 divide-y divide-slate-700">
                                {paginatedClients.map((client) => {
                                    const riskLevel = getRiskLevel(client);
                                    const RiskIcon = riskLevel.icon;
                                    return (
                                        <React.Fragment key={client.id}>
                                            <tr className="hover:bg-slate-700/30 transition-colors">
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <span className={`px-2 py-1 rounded-full text-xs font-medium flex items-center gap-1 w-fit border ${riskLevel.color}`}>
                                                        <RiskIcon className="h-3 w-3" />
                                                        {riskLevel.level}
                                                    </span>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="text-sm font-medium text-white">
                                                        {client.name}
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <span className="font-medium">{client.loansCount}</span> préstamos
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <span className="font-medium text-red-400">{client.maxDaysOverdue}</span> días
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <span className="font-medium">{(client.avgDaysOverdue || 0).toFixed(1)}</span> días
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <span className="font-medium text-red-400">${client.totalOverdueAmount}</span>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap">
                                                    <div className="text-sm text-slate-300">
                                                        <div>{client.email}</div>
                                                        <div className="text-slate-400">{client.phone}</div>
                                                    </div>
                                                </td>
                                                <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-300">
                                                    <button
                                                        onClick={() => handleClientDetail(client)}
                                                        className="text-orange-400 hover:text-orange-300 font-medium flex items-center gap-1 transition-colors"
                                                    >
                                                        {selectedClient?.id === client.id ? (
                                                            <>
                                                                <ChevronUp className="h-4 w-4" />
                                                                Ocultar
                                                            </>
                                                        ) : (
                                                            <>
                                                                <ChevronDown className="h-4 w-4" />
                                                                Ver detalle
                                                            </>
                                                        )}
                                                    </button>
                                                </td>
                                            </tr>

                                            {/* Detalle expandible del cliente */}
                                            {selectedClient?.id === client.id && (
                                                <tr>
                                                    <td colSpan="8" className="px-4 py-4 bg-slate-800/50">
                                                        <div className="space-y-3">
                                                            <h4 className="font-medium text-white">
                                                                Préstamos Atrasados de {client.name}:
                                                            </h4>
                                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                                {client.overdueLoans?.map((loan, index) => (
                                                                    <div key={index} className="bg-slate-700/50 p-3 rounded border border-slate-600">
                                                                        <div className="flex items-center justify-between mb-2">
                                                                            <span className="font-medium text-white">
                                                                                {loan.toolName}
                                                                            </span>
                                                                            <span className="text-red-400 font-medium flex items-center gap-1">
                                                                                <AlertTriangle className="h-3 w-3" />
                                                                                {loan.daysOverdue} días atraso
                                                                            </span>
                                                                        </div>
                                                                        <div className="text-sm text-slate-400">
                                                                            <p>ID Préstamo: #{loan.id}</p>
                                                                            <p>Multa estimada: ${loan.fineAmount || loan.daysOverdue * 5}</p>
                                                                        </div>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </React.Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>

                    {/* Paginación */}
                    {processedData.clients.length > 0 && (
                        <div className="px-4 py-3 border-t border-slate-700/50 flex flex-col sm:flex-row items-center justify-between gap-3 bg-slate-800/40 rounded-b-lg">
                            <div className="flex items-center gap-3 text-sm text-slate-400">
                                <span>Mostrando {pageStart + 1}–{Math.min(pageStart + itemsPerPage, processedData.clients.length)} de {processedData.clients.length} clientes</span>
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

export default OverdueClientsReport;
