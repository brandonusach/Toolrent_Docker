﻿import React, { useState, useEffect } from 'react';
import { getFriendlyError, getFieldErrors } from '../../../../utils/errorUtils';

const ToolForm = ({ mode, tool, categories, onSubmit, onClose, existingTools }) => {
    const [formData, setFormData] = useState({
        name: '',
        categoryId: '',
        initialStock: '',
        replacementValue: 1000
    });

    const [loading, setLoading] = useState(false);
    const [serverErrors, setServerErrors] = useState({});
    const [clientErrors, setClientErrors] = useState({});
    const [generalError, setGeneralError] = useState('');

    // Initialize form data
    useEffect(() => {
        if (mode === 'edit' && tool) {
            setFormData({
                name: tool.name || '',
                categoryId: tool.category?.id || '',
                initialStock: tool.initialStock || 1,
                replacementValue: tool.replacementValue || 1000,
                rentalRate: tool.rentalRate || ''
            });
        } else {
            setFormData({
                name: '',
                categoryId: '',
                initialStock: '',
                replacementValue: 1000,
                rentalRate: ''
            });
        }
        setServerErrors({});
        setClientErrors({});
    }, [mode, tool]);

    // Validar duplicados en el cliente
    const checkDuplicates = () => {
        if (!existingTools || !formData.name || !formData.categoryId) {
            return true;
        }

        const trimmedName = formData.name.trim().toLowerCase();
        const categoryId = parseInt(formData.categoryId);

        const duplicate = existingTools.find(t => {
            // En modo edición, excluir la herramienta actual
            if (mode === 'edit' && tool && t.id === tool.id) {
                return false;
            }

            return t.name.toLowerCase() === trimmedName &&
                   t.category?.id === categoryId;
        });

        if (duplicate) {
            const categoryName = categories.find(c => c.id === categoryId)?.name || 'desconocida';
            setClientErrors({
                duplicate: `Ya existe una herramienta llamada "${formData.name}" en la categoría "${categoryName}"`
            });
            return false;
        }

        setClientErrors({});
        return true;
    };

    // Limpiar errores al cambiar nombre o categoría
    const handleNameChange = (e) => {
        setFormData({...formData, name: e.target.value});
        setClientErrors({});
    };

    const handleCategoryChange = (e) => {
        setFormData({...formData, categoryId: e.target.value});
        setClientErrors({});
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setServerErrors({});
        setClientErrors({});
        setGeneralError('');

        if (!checkDuplicates()) return;

        setLoading(true);

        try {
            const payload = {
                name: formData.name,
                category: { id: parseInt(formData.categoryId) },
                initialStock: parseInt(formData.initialStock),
                replacementValue: parseFloat(formData.replacementValue),
                rentalRate: parseFloat(formData.rentalRate)
            };
            if (mode === 'edit') {
                await onSubmit(tool.id, payload);
            } else {
                await onSubmit(payload);
            }
            onClose();
        } catch (error) {
            const fieldErrors = getFieldErrors(error);
            if (fieldErrors) {
                setServerErrors(fieldErrors);
            } else {
                setGeneralError(getFriendlyError(error));
            }
        } finally {
            setLoading(false);
        }
    };

    const title = mode === 'edit' ? 'Editar Herramienta' : 'Registrar Nueva Herramienta';
    const submitText = mode === 'edit' ? 'Actualizar' : 'Registrar';

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-slate-800 rounded-lg p-6 w-full max-w-md">
                <h3 className="text-xl font-bold text-white mb-4">{title}</h3>

                {/* Error general */}
                {generalError && (
                    <div className="mb-4 p-3 bg-red-900/30 border border-red-500 rounded-lg">
                        <p className="text-red-400 text-sm flex items-center gap-2">
                            <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                            </svg>
                            {generalError}
                        </p>
                    </div>
                )}

                {/* Alert de duplicado */}
                {clientErrors.duplicate && (
                    <div className="mb-4 p-3 bg-red-900/30 border border-red-500 rounded-lg">
                        <p className="text-red-400 text-sm flex items-center">
                            <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                            </svg>
                            {clientErrors.duplicate}
                        </p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    {/* Name Field */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Nombre *
                        </label>
                        <input
                            type="text"
                            required
                            minLength={2}
                            maxLength={100}
                            value={formData.name}
                            onChange={handleNameChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white focus:outline-none ${
                                serverErrors.name || clientErrors.duplicate
                                    ? 'border-red-500 focus:border-red-400'
                                    : 'border-slate-600 focus:border-orange-500'
                            }`}
                            placeholder="Nombre de la herramienta"
                            disabled={loading}
                        />
                        {serverErrors.name && (
                            <p className="text-red-400 text-xs mt-1">{serverErrors.name}</p>
                        )}
                    </div>

                    {/* Category Field */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Categoría *
                        </label>
                        <select
                            required
                            value={formData.categoryId}
                            onChange={handleCategoryChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white focus:outline-none ${
                                serverErrors.category || clientErrors.duplicate
                                    ? 'border-red-500 focus:border-red-400'
                                    : 'border-slate-600 focus:border-orange-500'
                            }`}
                            disabled={loading}
                        >
                            <option value="">Seleccionar categoría</option>
                            {categories.map(category => (
                                <option key={category.id} value={category.id}>
                                    {category.name}
                                </option>
                            ))}
                        </select>
                        {serverErrors.category && (
                            <p className="text-red-400 text-xs mt-1">{serverErrors.category}</p>
                        )}
                    </div>

                    {/* Initial Stock Field */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Stock Inicial *
                        </label>
                        <input
                            type="number"
                            required
                            min="1"
                            max="999"
                            value={formData.initialStock}
                            onChange={(e) => setFormData({
                                ...formData,
                                initialStock: e.target.value
                            })}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white focus:outline-none ${
                                serverErrors.initialStock
                                    ? 'border-red-500 focus:border-red-400'
                                    : 'border-slate-600 focus:border-orange-500'
                            }`}
                            placeholder="Ej: 22"
                            disabled={loading}
                        />
                        {serverErrors.initialStock && (
                            <p className="text-red-400 text-xs mt-1">{serverErrors.initialStock}</p>
                        )}
                        <p className="text-slate-400 text-xs mt-1">Stock entre 1 y 999</p>
                    </div>

                    {/* Replacement Value Field */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Valor de Reposición *
                        </label>
                        <input
                            type="number"
                            required
                            min="1000"
                            max="1000000"
                            step="1"
                            value={formData.replacementValue}
                            onChange={(e) => setFormData({
                                ...formData,
                                replacementValue: e.target.value
                            })}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white focus:outline-none ${
                                serverErrors.replacementValue
                                    ? 'border-red-500 focus:border-red-400'
                                    : 'border-slate-600 focus:border-orange-500'
                            }`}
                            placeholder="1000"
                            disabled={loading}
                        />
                        {serverErrors.replacementValue && (
                            <p className="text-red-400 text-xs mt-1">{serverErrors.replacementValue}</p>
                        )}
                        <p className="text-slate-400 text-xs mt-1">Valor entre $1.000 y $1.000.000</p>
                    </div>

                    {/* Rental Rate Field */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            Tarifa de Arriendo (por día) *
                        </label>
                        <input
                            type="number"
                            required
                            min="1"
                            max="10000"
                            step="0.01"
                            value={formData.rentalRate}
                            onChange={(e) => setFormData({
                                ...formData,
                                rentalRate: e.target.value
                            })}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white focus:outline-none ${
                                serverErrors.rentalRate
                                    ? 'border-red-500 focus:border-red-400'
                                    : 'border-slate-600 focus:border-orange-500'
                            }`}
                            placeholder="100"
                            disabled={loading}
                        />
                        {serverErrors.rentalRate && (
                            <p className="text-red-400 text-xs mt-1">{serverErrors.rentalRate}</p>
                        )}
                        <p className="text-slate-400 text-xs mt-1">Tarifa diaria entre $1 y $10.000</p>
                    </div>

                    {/* Buttons */}
                    <div className="flex space-x-3 mt-6">
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 bg-orange-600 text-white py-2 px-4 rounded-lg hover:bg-orange-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? 'Procesando...' : submitText}
                        </button>
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={loading}
                            className="flex-1 bg-slate-600 text-white py-2 px-4 rounded-lg hover:bg-slate-700 transition-colors disabled:opacity-50"
                        >
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ToolForm;
