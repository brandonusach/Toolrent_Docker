// loans/components/ReturnForm.jsx - VERSIÓN CORREGIDA con formateo de fechas
import React, { useState, useEffect } from 'react';
import { X, Package, AlertTriangle, FileText, CheckCircle, Loader } from 'lucide-react';
import { formatDateLocal, daysBetween } from '../../../../utils/dateUtils';
import { useRates } from '../../rates/hooks/useRates';

const ReturnForm = ({ loan, onSubmit, onClose, onSuccess }) => {
    const [returnData, setReturnData] = useState({
        damaged: false,
        damageType: 'MINOR',
        notes: ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [lateInfo, setLateInfo] = useState(null);

    // Hook para obtener tarifas actuales
    const { rates, getCurrentRates } = useRates();

    // Cargar tarifas actuales al abrir el formulario
    useEffect(() => {
        getCurrentRates();
    }, [getCurrentRates]);

    // Calcular información de atraso
    useEffect(() => {
        if (loan) {
            const today = new Date().toISOString().split('T')[0];
            const agreedDate = loan.agreedReturnDate;
            const daysLate = daysBetween(agreedDate, today);
            const isLate = daysLate > 0;

            if (isLate) {
                setLateInfo({
                    isLate: true,
                    daysLate: daysLate,
                    agreedDate: formatDateLocal(agreedDate)
                });
            } else {
                setLateInfo({
                    isLate: false,
                    daysEarly: Math.abs(daysLate),
                    agreedDate: formatDateLocal(agreedDate)
                });
            }
        }
    }, [loan]);

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setReturnData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setLoading(true);
        setError('');

        try {
            await onSubmit(loan.id, returnData);
            onSuccess();
        } catch (err) {
            console.error('Error processing return:', err);
            setError(err.message || 'Error al procesar la devolución');
        } finally {
            setLoading(false);
        }
    };

    if (!loan) {
        return null;
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-start justify-center p-4 pt-8 z-50 overflow-y-auto">
            <div className="bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full mb-8 max-h-[calc(100vh-4rem)]">
                <div className="max-h-[calc(100vh-4rem)] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-gray-700 sticky top-0 bg-gray-800 z-10">
                    <h2 className="text-xl font-bold text-white">Procesar Devolución</h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white transition-colors"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-6">
                    {/* Información del préstamo */}
                    <div className="bg-gray-700 rounded-lg p-4 border border-gray-600">
                        <h3 className="text-lg font-medium text-white mb-3">Información del Préstamo</h3>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                            <div>
                                <span className="text-gray-400">Cliente:</span>
                                <p className="text-white font-medium">{loan.client?.name}</p>
                            </div>

                            <div>
                                <span className="text-gray-400">Herramienta:</span>
                                <p className="text-white font-medium">{loan.tool?.name}</p>
                            </div>

                            <div>
                                <span className="text-gray-400">Fecha de préstamo:</span>
                                <p className="text-white font-medium">
                                    {formatDateLocal(loan.loanDate, { day: 'numeric', month: 'long', year: 'numeric' })}
                                </p>
                            </div>

                            <div>
                                <span className="text-gray-400">Fecha acordada:</span>
                                <p className="text-white font-medium">
                                    {formatDateLocal(loan.agreedReturnDate, { day: 'numeric', month: 'long', year: 'numeric' })}
                                </p>
                            </div>

                            <div>
                                <span className="text-gray-400">Tarifa de arriendo:</span>
                                <p className="text-white font-medium">${loan.dailyRate}/día</p>
                            </div>
                        </div>

                        {loan.notes && (
                            <div className="mt-3">
                                <span className="text-gray-400">Notas del préstamo:</span>
                                <p className="text-white text-sm bg-gray-600 rounded p-2 mt-1">{loan.notes}</p>
                            </div>
                        )}
                    </div>

                    {/* Estado del tiempo */}
                    {lateInfo && (
                        <div className={`rounded-lg p-4 border ${
                            lateInfo.isLate
                                ? 'bg-red-900 border-red-700'
                                : 'bg-green-900 border-green-700'
                        }`}>
                            <div className="flex items-center">
                                {lateInfo.isLate ? (
                                    <AlertTriangle className="h-5 w-5 text-red-400 mr-2" />
                                ) : (
                                    <CheckCircle className="h-5 w-5 text-green-400 mr-2" />
                                )}

                                <div className="flex-1">
                                    <h4 className={`font-medium ${
                                        lateInfo.isLate ? 'text-red-200' : 'text-green-200'
                                    }`}>
                                        {lateInfo.isLate
                                            ? `Préstamo Atrasado - ${lateInfo.daysLate} día(s)`
                                            : `Devolución Puntual - ${lateInfo.daysEarly} día(s) antes`
                                        }
                                    </h4>
                                    <p className={`text-sm ${
                                        lateInfo.isLate ? 'text-red-300' : 'text-green-300'
                                    }`}>
                                        {lateInfo.isLate
                                            ? `Se aplicará automáticamente una multa por atraso`
                                            : `No se aplicarán multas por atraso`
                                        }
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Estado de la herramienta */}
                    <div>
                        <label className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                name="damaged"
                                checked={returnData.damaged}
                                onChange={handleInputChange}
                                className="h-4 w-4 text-orange-600 bg-gray-700 border-gray-600 rounded focus:ring-orange-500"
                            />
                            <div className="flex items-center">
                                <Package className="h-4 w-4 text-gray-400 mr-2" />
                                <span className="text-white font-medium">La herramienta presenta daños</span>
                            </div>
                        </label>

                        {returnData.damaged && (
                            <div className="mt-3 ml-7 space-y-3">
                                {/* Tipo de daño */}
                                <div className="bg-gray-700 border border-gray-600 rounded-lg p-3">
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Tipo de daño
                                    </label>
                                    <div className="space-y-2">
                                        <label className="flex items-center space-x-2 cursor-pointer">
                                            <input
                                                type="radio"
                                                name="damageType"
                                                value="MINOR"
                                                checked={returnData.damageType === 'MINOR'}
                                                onChange={handleInputChange}
                                                className="h-4 w-4 text-orange-600 bg-gray-700 border-gray-600 focus:ring-orange-500"
                                            />
                                            <div>
                                                <span className="text-white font-medium">Daño Leve (Reparable)</span>
                                                <p className="text-xs text-gray-400">Se aplicará multa por costo de reparación. El administrador debe marcar la herramienta como reparada manualmente cuando esté lista.</p>
                                            </div>
                                        </label>
                                        <label className="flex items-center space-x-2 cursor-pointer">
                                            <input
                                                type="radio"
                                                name="damageType"
                                                value="IRREPARABLE"
                                                checked={returnData.damageType === 'IRREPARABLE'}
                                                onChange={handleInputChange}
                                                className="h-4 w-4 text-red-600 bg-gray-700 border-gray-600 focus:ring-red-500"
                                            />
                                            <div>
                                                <span className="text-white font-medium">Daño Irreparable</span>
                                                <p className="text-xs text-gray-400">Se cobrará el valor completo de reposición. La herramienta será dada de baja después del pago.</p>
                                            </div>
                                        </label>
                                    </div>
                                </div>

                                {/* Alertas según tipo de daño */}
                                {returnData.damageType === 'MINOR' ? (
                                    <div className="bg-yellow-900 border border-yellow-700 rounded-lg p-3">
                                        <div className="flex items-start text-yellow-200 text-sm">
                                            <AlertTriangle className="h-4 w-4 mr-2 mt-0.5 flex-shrink-0" />
                                            <div>
                                                <p className="font-medium">Flujo de reparación y multa</p>
                                                <p className="text-xs text-yellow-300 mt-1">
                                                    <strong>1.</strong> La herramienta pasa a estado "En Reparación" (no disponible).<br/>
                                                    <strong>2.</strong> Se crea una multa del {rates.current?.REPAIR_RATE || 30}% del valor de reposición (${(loan.tool?.replacementValue * ((rates.current?.REPAIR_RATE || 30) / 100)).toFixed(2)}).<br/>
                                                    <strong>3.</strong> La herramienta como "Reparada" desde el inventario cuando esté físicamente lista.<br/>
                                                    <strong>4.</strong> La herramienta queda disponible aunque la multa siga pendiente de pago.
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="bg-red-900 border border-red-700 rounded-lg p-3">
                                        <div className="flex items-start text-red-200 text-sm">
                                            <AlertTriangle className="h-4 w-4 mr-2 mt-0.5 flex-shrink-0" />
                                            <div>
                                                <p className="font-medium">Cobro de reposición total</p>
                                                <p className="text-xs text-red-300 mt-1">
                                                    Se cobrará el valor completo de reposición (${loan.tool?.replacementValue.toFixed(2)}).
                                                    La herramienta será dada de baja permanentemente inmediatamente al procesar esta devolución.
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Notas de devolución */}
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-2">
                            <FileText className="h-4 w-4 inline mr-1" />
                            Notas de Devolución
                            {returnData.damaged && <span className="text-red-400 ml-1">*</span>}
                        </label>
                        <textarea
                            name="notes"
                            value={returnData.notes}
                            onChange={handleInputChange}
                            rows={4}
                            required={returnData.damaged}
                            className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                            placeholder={
                                returnData.damaged
                                    ? "Describa detalladamente los daños encontrados en la herramienta..."
                                    : "Observaciones sobre el estado de la herramienta y el proceso de devolución..."
                            }
                        />
                        {returnData.damaged && (
                            <p className="text-xs text-red-400 mt-1">
                                * Requerido cuando la herramienta presenta daños
                            </p>
                        )}
                    </div>

                    {/* Error */}
                    {error && (
                        <div className="bg-red-900 border border-red-700 rounded-md p-3">
                            <div className="flex items-center">
                                <AlertTriangle className="h-4 w-4 text-red-400 mr-2" />
                                <span className="text-red-200 text-sm">{error}</span>
                            </div>
                        </div>
                    )}

                    {/* Buttons */}
                    <div className="flex justify-end space-x-3 pt-4 border-t border-gray-700">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={loading || (returnData.damaged && !returnData.notes.trim())}
                            className="px-6 py-2 bg-orange-600 text-white rounded-md hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center"
                        >
                            {loading && <Loader className="h-4 w-4 mr-2 animate-spin" />}
                            {loading ? 'Procesando...' : 'Procesar Devolución'}
                        </button>
                    </div>
                </form>
                </div>
            </div>
        </div>
    );
};

export default ReturnForm;