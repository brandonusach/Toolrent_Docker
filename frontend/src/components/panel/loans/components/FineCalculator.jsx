import React, { useState, useEffect } from 'react';
import {
    X,
    DollarSign,
    Calendar,
    AlertTriangle,
    CheckCircle,
    Clock,
    Loader,
    Calculator,
    User,
    Package2,
    FileText,
    CreditCard
} from 'lucide-react';
import { useFines } from '../hooks/useFines';
import { useRates } from '../../rates/hooks/useRates';
import { formatDateLocal, daysBetween } from '../../../../utils/dateUtils';

const FineCalculator = ({ loan, onClose }) => {
    const {
        getFinesByClient,
        getTotalUnpaidAmount,
        payFine,
        loading
    } = useFines();

    const {
        rates,
        getCurrentRates
    } = useRates();

    const [clientFines, setClientFines] = useState([]);
    const [loanFines, setLoanFines] = useState([]);
    const [totalUnpaid, setTotalUnpaid] = useState(0);
    const [loadingData, setLoadingData] = useState(true);
    const [payingFine, setPayingFine] = useState(null);
    const [error, setError] = useState('');

    useEffect(() => {
        if (loan) {
            loadFinesData();
            getCurrentRates(); // Cargar tarifas actuales
        }
    }, [loan]);

    const loadFinesData = async () => {
        setLoadingData(true);
        try {
            // Cargar multas del cliente
            const fines = await getFinesByClient(loan.client.id);
            setClientFines(fines);

            // Filtrar multas específicas de este préstamo
            const loanSpecificFines = fines.filter(fine => fine.loan?.id === loan.id);
            setLoanFines(loanSpecificFines);

            // Obtener total de multas impagas
            const unpaidTotal = await getTotalUnpaidAmount(loan.client.id);
            setTotalUnpaid(unpaidTotal);
        } catch (err) {
            console.error('Error loading fines:', err);
            setError('Error al cargar las multas');
        } finally {
            setLoadingData(false);
        }
    };

    const handlePayFine = async (fineId) => {
        setPayingFine(fineId);
        setError('');
        try {
            await payFine(fineId);
            await loadFinesData(); // Recargar datos
        } catch (err) {
            console.error('Error paying fine:', err);
            setError(err.message || 'Error al pagar la multa');
        } finally {
            setPayingFine(null);
        }
    };

    const calculateEstimatedFines = () => {
        if (!loan) return { lateFine: 0, damageFine: 0, total: 0 };

        const today = new Date().toISOString().split('T')[0];
        const returnDate = loan.agreedReturnDate;

        let estimations = {
            lateFine: 0,
            damageFine: 0,
            total: 0
        };

        // Solo calcular si está atrasado
        const daysLate = daysBetween(returnDate, today);
        if (daysLate > 0 && loan.status === 'ACTIVE') {
            // Usar la tarifa real de multas del sistema
            const dailyLateFee = rates.current?.LATE_FEE_RATE || 2000;
            estimations.lateFine = daysLate * dailyLateFee;
        }

        // Estimación de multa por daño usando tarifa de reparación
        if (loan.tool?.replacementValue) {
            const repairRate = rates.current?.REPAIR_RATE || 30;
            estimations.damageFine = loan.tool.replacementValue * (repairRate / 100);
        }

        estimations.total = estimations.lateFine + estimations.damageFine;
        return estimations;
    };

    const getFineTypeLabel = (type) => {
        const types = {
            'LATE_RETURN': 'Devolución Tardía',
            'DAMAGE_REPAIR': 'Reparación por Daño',
            'TOOL_REPLACEMENT': 'Reposición de Herramienta'
        };
        return types[type] || type;
    };

    const getFineTypeColor = (type) => {
        const colors = {
            'LATE_RETURN': 'bg-yellow-900 text-yellow-200 border-yellow-700',
            'DAMAGE_REPAIR': 'bg-orange-900 text-orange-200 border-orange-700',
            'TOOL_REPLACEMENT': 'bg-red-900 text-red-200 border-red-700'
        };
        return colors[type] || 'bg-gray-900 text-gray-200 border-gray-700';
    };

    const estimatedFines = calculateEstimatedFines();
    const unpaidClientFines = clientFines.filter(fine => !fine.paid);
    const paidClientFines = clientFines.filter(fine => fine.paid);

    if (!loan) {
        return null;
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-gray-800 rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-gray-700">
                    <div className="flex items-center">
                        <Calculator className="h-6 w-6 text-purple-400 mr-2" />
                        <h2 className="text-xl font-bold text-white">Calculadora de Multas</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white transition-colors"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                <div className="p-6 space-y-6">
                    {/* Información del préstamo */}
                    <div className="bg-gray-700 rounded-lg p-4 border border-gray-600">
                        <h3 className="text-lg font-medium text-white mb-3">Información del Préstamo</h3>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                            <div>
                                <span className="text-gray-400 flex items-center">
                                    <User className="h-4 w-4 mr-1" />
                                    Cliente:
                                </span>
                                <p className="text-white font-medium">{loan.client?.name}</p>
                            </div>
                            <div>
                                <span className="text-gray-400 flex items-center">
                                    <Package2 className="h-4 w-4 mr-1" />
                                    Herramienta:
                                </span>
                                <p className="text-white font-medium">{loan.tool?.name}</p>
                            </div>
                            <div>
                                <span className="text-gray-400 flex items-center">
                                    <Calendar className="h-4 w-4 mr-1" />
                                    Fecha acordada:
                                </span>
                                <p className="text-white font-medium">{formatDateLocal(loan.agreedReturnDate)}</p>
                            </div>
                        </div>
                    </div>

                    {/* Resumen financiero */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="bg-blue-900 rounded-lg p-4 border border-blue-700">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-blue-200 text-sm">Total Multas Impagas</p>
                                    <p className="text-2xl font-bold text-white">${totalUnpaid.toFixed(2)}</p>
                                </div>
                                <DollarSign className="h-8 w-8 text-blue-400" />
                            </div>
                        </div>

                        <div className="bg-yellow-900 rounded-lg p-4 border border-yellow-700">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-yellow-200 text-sm">Multas Pendientes</p>
                                    <p className="text-2xl font-bold text-white">{unpaidClientFines.length}</p>
                                </div>
                                <AlertTriangle className="h-8 w-8 text-yellow-400" />
                            </div>
                        </div>

                        <div className="bg-green-900 rounded-lg p-4 border border-green-700">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-green-200 text-sm">Multas Pagadas</p>
                                    <p className="text-2xl font-bold text-white">{paidClientFines.length}</p>
                                </div>
                                <CheckCircle className="h-8 w-8 text-green-400" />
                            </div>
                        </div>
                    </div>

                    {/* Estimaciones de multas */}
                    {loan.status === 'ACTIVE' && (
                        <div className="bg-purple-900 rounded-lg p-4 border border-purple-700">
                            <h3 className="text-lg font-medium text-white mb-3 flex items-center">
                                <Calculator className="h-5 w-5 mr-2" />
                                Estimación de Multas Automáticas
                            </h3>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                                <div>
                                    <span className="text-purple-200">Multa por Atraso:</span>
                                    <p className="text-white font-medium text-lg">
                                        ${estimatedFines.lateFine.toFixed(2)}
                                    </p>
                                    <p className="text-purple-300 text-xs">
                                        {estimatedFines.lateFine > 0 ?
                                            `Basado en días de atraso` :
                                            'No hay atraso actualmente'
                                        }
                                    </p>
                                </div>
                                <div>
                                    <span className="text-purple-200">Multa por Daño:</span>
                                    <p className="text-white font-medium text-lg">
                                        ${estimatedFines.damageFine.toFixed(2)}
                                    </p>
                                    <p className="text-purple-300 text-xs">
                                        Estimación: {rates.current?.REPAIR_RATE || 30}% del valor de reposición
                                    </p>
                                </div>
                                <div>
                                    <span className="text-purple-200">Total Estimado:</span>
                                    <p className="text-white font-medium text-lg">
                                        ${estimatedFines.total.toFixed(2)}
                                    </p>
                                    <p className="text-purple-300 text-xs">
                                        Multas que se aplicarían automáticamente
                                    </p>
                                </div>
                            </div>
                            <div className="mt-3 text-xs text-purple-300">
                                <AlertTriangle className="h-4 w-4 inline mr-1" />
                                Las multas se calculan automáticamente al procesar la devolución
                            </div>
                        </div>
                    )}

                    {/* Multas específicas de este préstamo */}
                    {loanFines.length > 0 && (
                        <div className="bg-gray-700 rounded-lg p-4 border border-gray-600">
                            <h3 className="text-lg font-medium text-white mb-3">
                                Multas de Este Préstamo
                            </h3>
                            <div className="space-y-3">
                                {loanFines.map((fine) => (
                                    <div key={fine.id} className="bg-gray-600 rounded-lg p-3 border border-gray-500">
                                        <div className="flex items-start justify-between">
                                            <div className="flex-1">
                                                <div className="flex items-center space-x-2 mb-2">
                                                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border ${getFineTypeColor(fine.type)}`}>
                                                        {getFineTypeLabel(fine.type)}
                                                    </span>
                                                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                                                        fine.paid
                                                            ? 'bg-green-900 text-green-200 border-green-700'
                                                            : 'bg-red-900 text-red-200 border-red-700'
                                                    }`}>
                                                        {fine.paid ? 'Pagada' : 'Pendiente'}
                                                    </span>
                                                </div>
                                                <p className="text-white font-medium text-lg">
                                                    ${fine.amount.toFixed(2)}
                                                </p>
                                                <p className="text-gray-300 text-sm">{fine.description}</p>
                                                <div className="text-xs text-gray-400 mt-2">
                                                    <div>Creada: {formatDateLocal(fine.createdAt)}</div>
                                                    <div>Vence: {formatDateLocal(fine.dueDate)}</div>
                                                    {fine.paid && fine.paidDate && (
                                                        <div className="text-green-400">
                                                            Pagada: {formatDateLocal(fine.paidDate)}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                            {!fine.paid && (
                                                <button
                                                    onClick={() => handlePayFine(fine.id)}
                                                    disabled={payingFine === fine.id}
                                                    className="flex items-center px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 transition-colors text-sm"
                                                >
                                                    {payingFine === fine.id ? (
                                                        <Loader className="h-4 w-4 mr-1 animate-spin" />
                                                    ) : (
                                                        <CreditCard className="h-4 w-4 mr-1" />
                                                    )}
                                                    {payingFine === fine.id ? 'Procesando...' : 'Pagar'}
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Todas las multas del cliente */}
                    {loadingData ? (
                        <div className="flex items-center justify-center p-8">
                            <Loader className="h-8 w-8 animate-spin text-purple-500 mr-3" />
                            <span className="text-gray-300">Cargando multas del cliente...</span>
                        </div>
                    ) : (
                        <div className="bg-gray-700 rounded-lg p-4 border border-gray-600">
                            <h3 className="text-lg font-medium text-white mb-3">
                                Historial de Multas del Cliente
                            </h3>

                            {clientFines.length === 0 ? (
                                <div className="text-center py-8">
                                    <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500" />
                                    <p className="text-gray-400">Este cliente no tiene multas registradas</p>
                                </div>
                            ) : (
                                <div className="space-y-3 max-h-64 overflow-y-auto">
                                    {clientFines.map((fine) => (
                                        <div key={fine.id} className="bg-gray-600 rounded-lg p-3 border border-gray-500">
                                            <div className="flex items-start justify-between">
                                                <div className="flex-1">
                                                    <div className="flex items-center space-x-2 mb-2">
                                                        <span className="text-white font-medium">
                                                            Préstamo #{fine.loan?.id || 'N/A'}
                                                        </span>
                                                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border ${getFineTypeColor(fine.type)}`}>
                                                            {getFineTypeLabel(fine.type)}
                                                        </span>
                                                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                                                            fine.paid
                                                                ? 'bg-green-900 text-green-200 border-green-700'
                                                                : fine.dueDate < new Date().toISOString().split('T')[0]
                                                                    ? 'bg-red-900 text-red-200 border-red-700'
                                                                    : 'bg-yellow-900 text-yellow-200 border-yellow-700'
                                                        }`}>
                                                            {fine.paid ? 'Pagada' :
                                                                fine.dueDate < new Date().toISOString().split('T')[0] ? 'Vencida' : 'Pendiente'}
                                                        </span>
                                                    </div>
                                                    <p className="text-white font-medium">
                                                        ${fine.amount.toFixed(2)}
                                                    </p>
                                                    <p className="text-gray-300 text-sm">{fine.description}</p>
                                                    <div className="text-xs text-gray-400 mt-1">
                                                        {formatDateLocal(fine.createdAt)} • Vence: {formatDateLocal(fine.dueDate)}
                                                        {fine.paid && ` • Pagada: ${formatDateLocal(fine.paidDate)}`}
                                                    </div>
                                                </div>
                                                {!fine.paid && (
                                                    <button
                                                        onClick={() => handlePayFine(fine.id)}
                                                        disabled={payingFine === fine.id}
                                                        className="flex items-center px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 transition-colors text-sm"
                                                    >
                                                        {payingFine === fine.id ? (
                                                            <Loader className="h-4 w-4 mr-1 animate-spin" />
                                                        ) : (
                                                            <CreditCard className="h-4 w-4 mr-1" />
                                                        )}
                                                        {payingFine === fine.id ? 'Procesando...' : 'Pagar'}
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Error */}
                    {error && (
                        <div className="bg-red-900 border border-red-700 rounded-md p-3">
                            <div className="flex items-center">
                                <AlertTriangle className="h-4 w-4 text-red-400 mr-2" />
                                <span className="text-red-200 text-sm">{error}</span>
                            </div>
                        </div>
                    )}

                    {/* Footer con información */}
                    <div className="bg-blue-900 border border-blue-700 rounded-lg p-4">
                        <div className="flex items-center text-blue-200 mb-2">
                            <FileText className="h-4 w-4 mr-2" />
                            <span className="font-medium">Información Importante</span>
                        </div>
                        <div className="text-sm text-blue-300 space-y-1">
                            <p>• Las multas se calculan automáticamente al procesar devoluciones</p>
                            <p>• Los clientes con multas pendientes no pueden solicitar nuevos préstamos</p>
                            <p>• Las multas por atraso se basan en la tarifa de multa vigente</p>
                            <p>• Las multas por daño se calculan según el costo de reparación</p>
                        </div>
                    </div>

                    {/* Botón cerrar */}
                    <div className="flex justify-end pt-4 border-t border-gray-700">
                        <button
                            onClick={onClose}
                            className="px-6 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors"
                        >
                            Cerrar
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default FineCalculator;