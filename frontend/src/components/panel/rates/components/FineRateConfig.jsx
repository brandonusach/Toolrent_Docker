import React, { useState, useEffect } from 'react';
import { getFriendlyError } from '../../../../utils/errorUtils';
import { Calculator, Calendar, Save, X, AlertTriangle, CheckCircle } from 'lucide-react';
import { RATE_TYPES, formatCurrency, validateAmount, validateDateRange } from '../utils/rateConstants';

const FineRateConfig = ({
                            currentRate = 0,
                            rates = {},
                            onCreate,
                            onUpdate,
                            onDeactivate,
                            loading = false,
                            isAdmin = false
                        }) => {
    const [formData, setFormData] = useState({
        type: RATE_TYPES.LATE_FEE_RATE,
        dailyAmount: '',
        effectiveFrom: '',
        effectiveTo: '',
        active: true
    });
    const [errors, setErrors] = useState({});
    const [showForm, setShowForm] = useState(false);
    const [editingId, setEditingId] = useState(null);

    // Obtener tarifas de multa del estado
    const fineRates = rates.all?.filter(rate => rate.type === RATE_TYPES.LATE_FEE_RATE) || [];
    const activeFineRates = fineRates.filter(rate => rate.active);
    const currentRateData = activeFineRates.find(rate => rate.isCurrentlyActive) || activeFineRates[0];

    useEffect(() => {
        if (!showForm) {
            setFormData({
                type: RATE_TYPES.LATE_FEE_RATE,
                dailyAmount: '',
                effectiveFrom: '',
                effectiveTo: '',
                active: true
            });
            setEditingId(null);
            setErrors({});
        }
    }, [showForm]);

    const handleInputChange = (field, value) => {
        setFormData(prev => ({
            ...prev,
            [field]: value
        }));

        if (errors[field]) {
            setErrors(prev => ({
                ...prev,
                [field]: null
            }));
        }
    };

    const validateForm = () => {
        const newErrors = {};

        const amountError = validateAmount(parseFloat(formData.dailyAmount), RATE_TYPES.LATE_FEE_RATE);
        if (amountError) {
            newErrors.dailyAmount = amountError;
        }

        const dateError = validateDateRange(formData.effectiveFrom, formData.effectiveTo);
        if (dateError) {
            newErrors.effectiveFrom = dateError;
        }

        if (!editingId && formData.effectiveFrom) {
            // Obtener fecha local correcta (sin problemas de zona horaria)
            const now = new Date();
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            const selectedDate = new Date(formData.effectiveFrom + 'T00:00:00');

            if (selectedDate < today) {
                newErrors.effectiveFrom = 'La fecha de inicio debe ser hoy o una fecha futura';
            }
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        if (e) e.preventDefault();

        if (!validateForm()) {
            return;
        }

        try {
            const rateData = {
                ...formData,
                dailyAmount: parseFloat(formData.dailyAmount)
            };

            if (editingId) {
                await onUpdate(editingId, rateData);
            } else {
                await onCreate(rateData);
            }

            setShowForm(false);
        } catch (error) {
            setErrors({ submit: getFriendlyError(error) });
        }
    };

    const handleEdit = (rate) => {
        setFormData({
            type: rate.type,
            dailyAmount: rate.dailyAmount.toString(),
            effectiveFrom: rate.effectiveFrom,
            effectiveTo: rate.effectiveTo || '',
            active: rate.active
        });
        setEditingId(rate.id);
        setShowForm(true);
    };

    const handleDeactivate = async (rateId) => {
        if (window.confirm('¿Está seguro de que quiere desactivar esta tarifa?')) {
            try {
                await onDeactivate(rateId);
            } catch (error) {
                setErrors({ general: getFriendlyError(error) });
            }
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '';
        return new Date(dateString).toLocaleDateString('es-CL');
    };

    return (
        <div className="p-6">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                    <div className="bg-red-500/20 p-2 rounded-lg">
                        <Calculator className="text-red-400" size={24} />
                    </div>
                    <div>
                        <h2 className="text-xl font-semibold text-white">Tarifa de Multa por Atraso</h2>
                        <p className="text-slate-400 text-sm">
                            Configurar el costo diario por devolución tardía de herramientas
                        </p>
                    </div>
                </div>

                {isAdmin && (
                    <button
                        onClick={() => setShowForm(!showForm)}
                        className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-4 py-2 rounded-lg transition-colors"
                    >
                        {showForm ? <X size={18} /> : <Calculator size={18} />}
                        {showForm ? 'Cancelar' : 'Nueva Tarifa'}
                    </button>
                )}
            </div>

            {/* Error general */}
            {errors.general && (
                <div className="mb-4 bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg flex items-center gap-2">
                    <AlertTriangle size={18} />
                    {errors.general}
                </div>
            )}

            {/* Tarifa actual */}
            <div className="mb-6 bg-slate-750 p-4 rounded-lg border border-slate-600">
                <div className="flex items-center justify-between">
                    <div>
                        <h3 className="font-medium text-white mb-1">Tarifa Actual</h3>
                        <p className="text-2xl font-bold text-red-400">
                            {formatCurrency(currentRate)}
                            <span className="text-sm text-slate-400 font-normal ml-1">por día</span>
                        </p>
                        {currentRateData && (
                            <p className="text-sm text-slate-400 mt-1">
                                Vigente desde: {formatDate(currentRateData.effectiveFrom)}
                                {currentRateData.effectiveTo && (
                                    <span> hasta {formatDate(currentRateData.effectiveTo)}</span>
                                )}
                            </p>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <CheckCircle className="text-red-400" size={20} />
                        <span className="text-red-400 text-sm">Activa</span>
                    </div>
                </div>
            </div>

            {/* Formulario */}
            {showForm && isAdmin && (
                <div className="mb-6 bg-slate-750 p-6 rounded-lg border border-slate-600">
                    <h3 className="text-lg font-medium text-white mb-4">
                        {editingId ? 'Editar Tarifa' : 'Nueva Tarifa de Multa por Atraso'}
                    </h3>

                    <div className="space-y-4">
                        {errors.submit && (
                            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg">
                                {errors.submit}
                            </div>
                        )}

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {/* Monto diario */}
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Monto Diario *
                                </label>
                                <div className="relative">
                                    <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400">
                                        $
                                    </span>
                                    <input
                                        type="number"
                                        value={formData.dailyAmount}
                                        onChange={(e) => handleInputChange('dailyAmount', e.target.value)}
                                        placeholder="2000"
                                        min="0"
                                        step="100"
                                        className={`w-full bg-slate-700 border ${
                                            errors.dailyAmount ? 'border-red-500' : 'border-slate-600'
                                        } rounded-lg px-4 pl-8 py-2.5 text-white placeholder-slate-400 focus:outline-none focus:border-blue-500`}
                                    />
                                </div>
                                {errors.dailyAmount && (
                                    <p className="mt-1 text-sm text-red-400">{errors.dailyAmount}</p>
                                )}
                            </div>

                            {/* Fecha efectiva desde */}
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Vigente Desde *
                                </label>
                                <input
                                    type="date"
                                    value={formData.effectiveFrom}
                                    onChange={(e) => handleInputChange('effectiveFrom', e.target.value)}
                                    className={`w-full bg-slate-700 border ${
                                        errors.effectiveFrom ? 'border-red-500' : 'border-slate-600'
                                    } rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-blue-500`}
                                />
                                {errors.effectiveFrom && (
                                    <p className="mt-1 text-sm text-red-400">{errors.effectiveFrom}</p>
                                )}
                            </div>

                            {/* Fecha efectiva hasta */}
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Vigente Hasta (Opcional)
                                </label>
                                <input
                                    type="date"
                                    value={formData.effectiveTo}
                                    onChange={(e) => handleInputChange('effectiveTo', e.target.value)}
                                    className="w-full bg-slate-700 border border-slate-600 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-blue-500"
                                />
                                <p className="mt-1 text-xs text-slate-400">
                                    Si se deja vacío, la tarifa será válida indefinidamente
                                </p>
                            </div>

                            {/* Estado activo */}
                            <div className="flex items-center">
                                <label className="flex items-center gap-2 text-sm text-slate-300">
                                    <input
                                        type="checkbox"
                                        checked={formData.active}
                                        onChange={(e) => handleInputChange('active', e.target.checked)}
                                        className="rounded border-slate-600 text-blue-500 focus:ring-orange-500 focus:ring-offset-gray-800"
                                    />
                                    Activar inmediatamente
                                </label>
                            </div>
                        </div>

                        <div className="flex justify-end gap-3 pt-4">
                            <button
                                type="button"
                                onClick={() => setShowForm(false)}
                                className="px-4 py-2 text-slate-400 hover:text-white border border-slate-600 hover:border-slate-500 rounded-lg transition-colors"
                            >
                                Cancelar
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={loading}
                                className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 disabled:bg-slate-600 text-white px-6 py-2 rounded-lg transition-colors"
                            >
                                <Save size={18} />
                                {loading ? 'Guardando...' : (editingId ? 'Actualizar' : 'Crear Tarifa')}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Lista de tarifas */}
            <div>
                <h3 className="text-lg font-medium text-white mb-4">Tarifas de Multa Configuradas</h3>

                {fineRates.length === 0 ? (
                    <div className="text-center py-8 text-slate-400">
                        <Calculator className="mx-auto h-12 w-12 mb-2" />
                        <p>No hay tarifas de multa configuradas</p>
                    </div>
                ) : (
                    <div className="space-y-3">
                        {fineRates.map((rate) => (
                            <div
                                key={rate.id}
                                className={`p-4 rounded-lg border ${
                                    rate.active
                                        ? 'bg-red-500/5 border-red-500/30'
                                        : 'bg-slate-750 border-slate-600'
                                }`}
                            >
                                <div className="flex items-center justify-between">
                                    <div>
                                        <div className="flex items-center gap-3">
                                            <span className="text-xl font-bold text-white">
                                                {formatCurrency(rate.dailyAmount)}
                                            </span>
                                            {rate.active && (
                                                <span className="px-2 py-1 bg-red-500/20 text-red-400 text-xs rounded-full">
                                                    Activa
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-sm text-slate-400 mt-1">
                                            <Calendar size={14} className="inline mr-1" />
                                            Desde: {formatDate(rate.effectiveFrom)}
                                            {rate.effectiveTo && (
                                                <span> • Hasta: {formatDate(rate.effectiveTo)}</span>
                                            )}
                                        </p>
                                        {rate.createdAt && (
                                            <p className="text-xs text-slate-500 mt-1">
                                                Creada: {formatDate(rate.createdAt)}
                                            </p>
                                        )}
                                    </div>

                                    {isAdmin && (
                                        <div className="flex gap-2">
                                            <button
                                                onClick={() => handleEdit(rate)}
                                                className="px-3 py-1.5 text-blue-400 hover:text-blue-300 border border-blue-500/30 hover:border-blue-500/50 rounded text-sm transition-colors"
                                            >
                                                Editar
                                            </button>
                                            {rate.active && (
                                                <button
                                                    onClick={() => handleDeactivate(rate.id)}
                                                    className="px-3 py-1.5 text-red-400 hover:text-red-300 border border-red-500/30 hover:border-red-500/50 rounded text-sm transition-colors"
                                                >
                                                    Desactivar
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default FineRateConfig;
