// loans/components/ClientValidation.jsx - VERSION CORREGIDA para manejar errores del backend
import React, { useState, useEffect } from 'react';
import {
    User,
    AlertTriangle,
    CheckCircle,
    XCircle,
    DollarSign,
    Clock,
    Loader,
    Shield,
    Wifi,
    WifiOff,
    CreditCard
} from 'lucide-react';
import { useLoans } from '../hooks/useLoans';
import { useFines } from '../hooks/useFines';

const ClientValidation = ({ clientId, onValidationChange }) => {
    const { getLoansByClient, checkClientRestrictions } = useLoans();
    const { getFinesByClient, getTotalUnpaidAmount, checkClientRestrictions: checkFineRestrictions, payFine } = useFines();

    const [validation, setValidation] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [connectionStatus, setConnectionStatus] = useState('checking');
    const [unpaidFines, setUnpaidFines] = useState([]);
    const [payingFine, setPayingFine] = useState(null);

    useEffect(() => {
        if (clientId) {
            validateClient();
        } else {
            setValidation(null);
            setConnectionStatus('idle');
            if (onValidationChange) {
                onValidationChange(null);
            }
        }
    }, [clientId]);

    const validateClient = async () => {
        setLoading(true);
        setError('');
        setConnectionStatus('checking');

        try {

            // Primer intento: usar el endpoint principal de restricciones
            let loanRestrictions = null;
            let fineRestrictions = null;
            let clientLoans = [];
            let clientFines = [];
            let totalUnpaid = 0;

            // Intentar obtener restricciones de préstamos
            try {
                loanRestrictions = await checkClientRestrictions(clientId);
                setConnectionStatus('connected');
            } catch (loanErr) {
                setConnectionStatus('partial');
            }

            // Intentar obtener préstamos del cliente
            try {
                clientLoans = await getLoansByClient(clientId);
            } catch (loansErr) {
            }

            // Intentar obtener restricciones de multas
            try {
                fineRestrictions = await checkFineRestrictions(clientId);
            } catch (fineErr) {
                // Fallback: intentar obtener multas manualmente
                try {
                    clientFines = await getFinesByClient(clientId);
                    const unpaidFinesList = clientFines.filter(f => !f.paid);
                    setUnpaidFines(unpaidFinesList); // Guardar multas impagas
                    totalUnpaid = await getTotalUnpaidAmount(clientId);
                    fineRestrictions = {
                        canRequestLoan: unpaidFinesList.length === 0,
                        hasUnpaidFines: unpaidFinesList.length > 0,
                        unpaidFinesCount: unpaidFinesList.length,
                        totalUnpaidAmount: totalUnpaid,
                        fallback: true
                    };
                } catch (fallbackErr) {
                }
            }

            // Cargar multas impagas para mostrar detalles
            if (fineRestrictions?.hasUnpaidFines) {
                try {
                    if (clientFines.length === 0) {
                        clientFines = await getFinesByClient(clientId);
                    }
                    setUnpaidFines(clientFines.filter(f => !f.paid));
                } catch (err) {
                }
            }

            // Si no pudimos obtener restricciones de préstamos, hacer cálculo manual
            if (!loanRestrictions && clientLoans.length >= 0) {
                const activeLoans = clientLoans.filter(loan =>
                    loan.status === 'ACTIVE' || loan.status === 'active'
                );

                loanRestrictions = {
                    eligible: activeLoans.length < 5,
                    canRequestLoan: activeLoans.length < 5,
                    currentActiveLoans: activeLoans.length,
                    maxAllowedLoans: 5,
                    remainingLoanSlots: Math.max(0, 5 - activeLoans.length),
                    restriction: activeLoans.length >= 5 ? 'Cliente ha alcanzado el límite de préstamos activos' : null,
                    clientStatus: activeLoans.length >= 5 ? 'RESTRICTED' : 'ACTIVE',
                    fallback: true
                };
                setConnectionStatus('offline');
            }

            // Si no pudimos obtener restricciones de multas, asumir que no hay
            if (!fineRestrictions) {
                fineRestrictions = {
                    canRequestLoan: true,
                    hasUnpaidFines: false,
                    unpaidFinesCount: 0,
                    totalUnpaidAmount: 0,
                    isRestricted: false,
                    clientStatus: 'ACTIVE',
                    fallback: true,
                    message: 'No se pudieron verificar multas - asumiendo sin restricciones'
                };
            }

            // Combinar validación
            const combinedValidation = {
                eligible: (loanRestrictions?.eligible || false) && (fineRestrictions?.canRequestLoan || false),
                canRequestLoan: (loanRestrictions?.canRequestLoan || false) && (fineRestrictions?.canRequestLoan || false),
                loanRestrictions: loanRestrictions || {
                    eligible: false,
                    currentActiveLoans: 0,
                    maxAllowedLoans: 5,
                    restriction: 'No se pudo verificar estado de préstamos'
                },
                fineRestrictions: fineRestrictions,
                summary: generateValidationSummary(loanRestrictions, fineRestrictions),
                connectionStatus: connectionStatus,
                dataSource: {
                    loans: loanRestrictions?.fallback ? 'fallback' : 'api',
                    fines: fineRestrictions?.fallback ? 'fallback' : 'api'
                }
            };

            setValidation(combinedValidation);

            if (onValidationChange) {
                onValidationChange(combinedValidation);
            }

        } catch (err) {
            setError(err.message || 'Error al validar el cliente');
            setConnectionStatus('error');

            // Validación de emergencia - permitir el préstamo pero con advertencia
            const emergencyValidation = {
                eligible: true, // Permitir en caso de error
                canRequestLoan: true,
                loanRestrictions: {
                    eligible: true,
                    currentActiveLoans: 0,
                    maxAllowedLoans: 5,
                    remainingLoanSlots: 5,
                    restriction: null,
                    clientStatus: 'UNKNOWN',
                    emergency: true
                },
                fineRestrictions: {
                    canRequestLoan: true,
                    hasUnpaidFines: false,
                    unpaidFinesCount: 0,
                    totalUnpaidAmount: 0,
                    emergency: true
                },
                summary: {
                    issues: [{
                        type: 'error',
                        icon: AlertTriangle,
                        title: 'Error de Validación',
                        description: 'No se pudo verificar completamente las restricciones',
                        details: 'Proceda con precaución - verifique manualmente si es necesario'
                    }],
                    warnings: [],
                    info: []
                },
                emergency: true,
                error: err.message
            };

            setValidation(emergencyValidation);
            if (onValidationChange) {
                onValidationChange(emergencyValidation);
            }
        } finally {
            setLoading(false);
        }
    };

    const handlePayFine = async (fineId) => {
        setPayingFine(fineId);
        setError('');
        try {
            await payFine(fineId);
            // Revalidar el cliente después de pagar la multa
            await validateClient();
        } catch (err) {
            setError(err.message || 'Error al pagar la multa');
        } finally {
            setPayingFine(null);
        }
    };

    const generateValidationSummary = (loanRestrictions, fineRestrictions) => {
        const issues = [];
        const warnings = [];
        const info = [];

        if (!loanRestrictions || !fineRestrictions) {
            warnings.push({
                type: 'warning',
                icon: WifiOff,
                title: 'Validación Parcial',
                description: 'No se pudieron verificar todas las restricciones',
                details: 'Algunos servicios no están disponibles'
            });
            return { issues, warnings, info };
        }

        // Verificar restricciones de multas
        if (fineRestrictions.isRestricted || fineRestrictions.hasUnpaidFines) {
            issues.push({
                type: 'error',
                icon: DollarSign,
                title: '🚫 Cliente Restringido - Multas Pendientes',
                description: `Este cliente tiene ${fineRestrictions.unpaidFinesCount || 0} multa(s) por $${(fineRestrictions.totalUnpaidAmount || 0).toFixed(2)}`,
                details: '⚠️ NO PUEDE solicitar préstamos hasta pagar todas las multas. Use el botón "Gestionar Multas" para procesar el pago.'
            });
        }

        // Verificar límite de préstamos
        if (loanRestrictions.currentActiveLoans) {
            const remaining = loanRestrictions.remainingLoanSlots || 0;
            if (remaining <= 1 && remaining > 0) {
                warnings.push({
                    type: 'warning',
                    icon: AlertTriangle,
                    title: 'Límite de Préstamos',
                    description: `${loanRestrictions.currentActiveLoans} de ${loanRestrictions.maxAllowedLoans || 5} préstamos activos`,
                    details: `Solo puede solicitar ${remaining} préstamo(s) más`
                });
            } else if (remaining <= 0) {
                issues.push({
                    type: 'error',
                    icon: XCircle,
                    title: 'Límite Alcanzado',
                    description: `${loanRestrictions.currentActiveLoans} de ${loanRestrictions.maxAllowedLoans || 5} préstamos activos`,
                    details: 'Ha alcanzado el límite máximo de préstamos'
                });
            } else {
                info.push({
                    type: 'info',
                    icon: Clock,
                    title: 'Préstamos Activos',
                    description: `${loanRestrictions.currentActiveLoans} de ${loanRestrictions.maxAllowedLoans || 5} préstamos activos`,
                    details: `Puede solicitar ${remaining} préstamo(s) más`
                });
            }
        }

        // Estado general
        if (issues.length === 0 && warnings.length === 0) {
            info.push({
                type: 'success',
                icon: CheckCircle,
                title: 'Cliente Elegible',
                description: 'El cliente cumple todos los requisitos',
                details: 'Puede procesar préstamos sin restricciones'
            });
        }

        // Advertencia sobre datos de fallback
        if (loanRestrictions?.fallback || fineRestrictions?.fallback) {
            warnings.push({
                type: 'warning',
                icon: Wifi,
                title: 'Datos de Respaldo',
                description: 'Algunos datos provienen de validación local',
                details: 'La validación puede no estar completamente actualizada'
            });
        }

        return { issues, warnings, info };
    };

    const getStatusColor = (type) => {
        const colors = {
            success: 'text-green-400 bg-green-900 border-green-700',
            warning: 'text-yellow-400 bg-yellow-900 border-yellow-700',
            error: 'text-red-400 bg-red-900 border-red-700',
            info: 'text-blue-400 bg-blue-900 border-blue-700'
        };
        return colors[type] || colors.info;
    };

    const getConnectionIcon = () => {
        switch (connectionStatus) {
            case 'connected': return <Wifi className="h-4 w-4 text-green-400" />;
            case 'partial': return <Wifi className="h-4 w-4 text-yellow-400" />;
            case 'offline': return <WifiOff className="h-4 w-4 text-orange-400" />;
            case 'error': return <WifiOff className="h-4 w-4 text-red-400" />;
            default: return <Loader className="h-4 w-4 text-slate-400 animate-spin" />;
        }
    };

    const renderValidationItem = (item) => {
        const Icon = item.icon;
        const colorClasses = getStatusColor(item.type);

        return (
            <div key={item.title} className={`rounded-lg p-4 border ${colorClasses}`}>
                <div className="flex items-start">
                    <Icon className="h-5 w-5 mr-3 mt-0.5 flex-shrink-0" />
                    <div className="flex-1">
                        <h4 className="font-medium text-white">{item.title}</h4>
                        <p className="text-sm mt-1 opacity-90">{item.description}</p>
                        {item.details && (
                            <p className="text-xs mt-2 opacity-75">{item.details}</p>
                        )}
                    </div>
                </div>
            </div>
        );
    };

    if (!clientId) {
        return (
            <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                <div className="flex items-center text-slate-400">
                    <User className="h-5 w-5 mr-2" />
                    <span>Seleccione un cliente para validar su elegibilidad</span>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                <div className="flex items-center text-slate-300">
                    <Loader className="h-5 w-5 mr-2 animate-spin" />
                    <span>Validando elegibilidad del cliente...</span>
                </div>
            </div>
        );
    }

    if (error && !validation) {
        return (
            <div className="bg-red-900 rounded-lg p-4 border border-red-700">
                <div className="flex items-center text-red-200">
                    <AlertTriangle className="h-5 w-5 mr-2" />
                    <div>
                        <div className="font-medium">Error de Validación</div>
                        <div className="text-sm">{error}</div>
                    </div>
                </div>
            </div>
        );
    }

    if (!validation) {
        return null;
    }

    return (
        <div className="space-y-4">
            {/* Header de validación con estado de conexión */}
            <div className="bg-slate-700 rounded-lg p-4 border border-slate-600">
                <div className="flex items-center justify-between">
                    <div className="flex items-center">
                        <Shield className="h-5 w-5 mr-2 text-slate-400" />
                        <h3 className="text-lg font-medium text-white">Validación de Cliente</h3>
                        <div className="ml-2 flex items-center">
                            {getConnectionIcon()}
                        </div>
                    </div>
                    <div className={`flex items-center px-3 py-1 rounded-full text-sm font-medium border ${
                        validation.eligible
                            ? 'bg-green-900 text-green-200 border-green-700'
                            : 'bg-red-900 text-red-200 border-red-700'
                    }`}>
                        {validation.eligible ? (
                            <CheckCircle className="h-4 w-4 mr-1" />
                        ) : (
                            <XCircle className="h-4 w-4 mr-1" />
                        )}
                        {validation.eligible ? 'Elegible' : 'No Elegible'}
                    </div>
                </div>

                {validation.emergency && (
                    <div className="mt-2 text-xs text-yellow-300">
                        ⚠️ Validación de emergencia - Verifique manualmente las restricciones
                    </div>
                )}
            </div>

            {/* Resumen rápido */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-slate-700 rounded-lg p-3 border border-slate-600">
                    <div className="text-slate-400 text-sm">Estado del Cliente</div>
                    <div className={`text-lg font-medium ${
                        validation.fineRestrictions?.isRestricted ? 'text-red-400' : 'text-green-400'
                    }`}>
                        {validation.fineRestrictions?.clientStatus || 'ACTIVE'}
                    </div>
                </div>

                <div className="bg-slate-700 rounded-lg p-3 border border-slate-600">
                    <div className="text-slate-400 text-sm">Préstamos Activos</div>
                    <div className="text-lg font-medium text-white">
                        {validation.loanRestrictions?.currentActiveLoans || 0} / {validation.loanRestrictions?.maxAllowedLoans || 5}
                    </div>
                </div>

                <div className="bg-slate-700 rounded-lg p-3 border border-slate-600">
                    <div className="text-slate-400 text-sm">Total Multas</div>
                    <div className={`text-lg font-medium ${
                        (validation.fineRestrictions?.totalUnpaidAmount || 0) > 0 ? 'text-red-400' : 'text-green-400'
                    }`}>
                        ${(validation.fineRestrictions?.totalUnpaidAmount || 0).toFixed(2)}
                    </div>
                </div>
            </div>

            {/* Validación detallada */}
            <div className="space-y-3">
                {/* Errores/Problemas */}
                {validation.summary?.issues?.map(item => renderValidationItem(item))}

                {/* Advertencias */}
                {validation.summary?.warnings?.map(item => renderValidationItem(item))}

                {/* Información */}
                {validation.summary?.info?.map(item => renderValidationItem(item))}
            </div>

            {/* Multas Impagas - Sección para pagar multas */}
            {unpaidFines.length > 0 && (
                <div className="bg-red-900 rounded-lg p-4 border border-red-700">
                    <div className="flex items-center justify-between mb-3">
                        <h4 className="text-red-200 font-medium flex items-center">
                            <DollarSign className="h-5 w-5 mr-2" />
                            Multas Pendientes de Pago ({unpaidFines.length})
                        </h4>
                        <span className="text-red-200 font-bold text-lg">
                            Total: ${validation.fineRestrictions?.totalUnpaidAmount?.toFixed(2) || '0.00'}
                        </span>
                    </div>
                    <div className="space-y-2">
                        {unpaidFines.map(fine => (
                            <div key={fine.id} className="bg-red-800 rounded-lg p-3 border border-red-600">
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center mb-1">
                                            <span className={`px-2 py-0.5 rounded text-xs font-medium mr-2 ${
                                                fine.type === 'LATE_RETURN' 
                                                    ? 'bg-orange-900 text-orange-200 border border-orange-700' 
                                                    : 'bg-yellow-900 text-yellow-200 border border-yellow-700'
                                            }`}>
                                                {fine.type === 'LATE_RETURN' ? '⏰ Atraso' : '🔧 Daño'}
                                            </span>
                                            <span className="text-red-100 font-bold text-lg">
                                                ${fine.amount?.toFixed(2)}
                                            </span>
                                        </div>
                                        <p className="text-red-200 text-sm mt-1">{fine.description}</p>
                                        <div className="text-xs text-red-300 mt-2 space-y-0.5">
                                            <div>📅 Vence: {new Date(fine.dueDate).toLocaleDateString('es-ES', {
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric'
                                            })}</div>
                                            {fine.loan && (
                                                <div>📋 Préstamo #{fine.loan.id}</div>
                                            )}
                                            <div>🕒 Creada: {new Date(fine.createdAt).toLocaleDateString('es-ES')}</div>
                                        </div>
                                    </div>
                                    <button
                                        onClick={() => handlePayFine(fine.id)}
                                        disabled={payingFine === fine.id}
                                        className="ml-3 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center text-sm font-medium shadow-lg"
                                    >
                                        {payingFine === fine.id ? (
                                            <>
                                                <Loader className="h-4 w-4 mr-1.5 animate-spin" />
                                                Procesando...
                                            </>
                                        ) : (
                                            <>
                                                <CreditCard className="h-4 w-4 mr-1.5" />
                                                Pagar Multa
                                            </>
                                        )}
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="mt-3 bg-red-800 border border-red-600 rounded-lg p-3">
                        <div className="flex items-start text-red-200 text-sm">
                            <AlertTriangle className="h-4 w-4 mr-2 mt-0.5 flex-shrink-0" />
                            <div>
                                <p className="font-medium">Importante:</p>
                                <p className="text-xs text-red-300 mt-1">
                                    Debes pagar todas las multas pendientes para volver al estado ACTIVO y poder solicitar nuevos préstamos.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Acciones recomendadas */}
            {!validation.eligible && !validation.emergency && (
                <div className="bg-orange-900 rounded-lg p-4 border border-orange-700">
                    <h4 className="text-orange-200 font-medium mb-2">Acciones Recomendadas:</h4>
                    <ul className="text-orange-300 text-sm space-y-1">
                        {validation.fineRestrictions?.hasUnpaidFines && (
                            <li>• Gestionar el pago de las multas pendientes</li>
                        )}
                        {validation.loanRestrictions?.currentActiveLoans >= 5 && (
                            <li>• Esperar a que se devuelvan algunos préstamos activos</li>
                        )}
                        {validation.loanRestrictions?.restriction && (
                            <li>• Revisar el estado general del cliente</li>
                        )}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default ClientValidation;
