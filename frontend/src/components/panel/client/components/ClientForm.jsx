import React, { useState, useEffect } from 'react';
import { X, Save, User, Hash, Phone, Mail, AlertCircle } from 'lucide-react';
import { getFriendlyError, getFieldErrors } from '../../../../utils/errorUtils';

const ClientForm = ({
                        mode = 'create', // 'create' or 'edit'
                        client = null,
                        onClose,
                        onSubmit
                    }) => {
    const [formData, setFormData] = useState({
        name: '',
        rut: '',
        phone: '',
        email: ''
    });

    // Solo errores del servidor, no validaciones frontend
    const [serverErrors, setServerErrors] = useState({});
    const [generalError, setGeneralError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Llenar el formulario si estamos en modo edición
    useEffect(() => {
        if (mode === 'edit' && client) {
            setFormData({
                name: client.name || '',
                rut: client.rut || '',
                phone: client.phone || '',
                email: client.email || ''
            });
        }
    }, [mode, client]);

    const formatRUTInput = (value) => {
        if (!value) return '';

        // Limpiar entrada manteniendo solo números y K
        const cleaned = value.replace(/[^0-9kK]/g, '').toUpperCase();

        if (cleaned.length <= 1) return cleaned;

        // Separar cuerpo y posible dígito verificador
        const body = cleaned.slice(0, -1);
        const lastChar = cleaned.slice(-1);

        // Formatear cuerpo con puntos
        const formattedBody = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

        // Si hay suficientes caracteres, agregar el guión
        if (cleaned.length > 7) {
            return `${formattedBody}-${lastChar}`;
        }

        return formattedBody + lastChar;
    };

    const formatPhoneInput = (value) => {
        if (!value) return '';

        // Limpiar entrada manteniendo solo números
        const cleaned = value.replace(/[^0-9]/g, '');

        if (cleaned.length <= 1) return cleaned;

        // Formatear según longitud
        if (cleaned.length === 9 && cleaned.startsWith('9')) {
            // Celular: 9 1234 5678
            return cleaned.replace(/(\d{1})(\d{4})(\d{4})/, '$1 $2 $3');
        } else if (cleaned.length === 8) {
            // Fijo: 22 1234 5678
            return cleaned.replace(/(\d{2})(\d{4})(\d{4})/, '$1 $2 $3');
        } else if (cleaned.length > 4) {
            // Formateo progresivo
            return cleaned.replace(/(\d{2})(\d{4})/, '$1 $2');
        }

        return cleaned;
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;

        let formattedValue = value;

        // Aplicar formateo según el campo
        if (name === 'rut') {
            formattedValue = formatRUTInput(value);
        } else if (name === 'phone') {
            formattedValue = formatPhoneInput(value);
        } else if (name === 'name') {
            // Solo letras, espacios, tildes, ñ/Ñ y guión (sin números ni caracteres especiales)
            formattedValue = value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ '\-]/g, '');
        }

        setFormData(prev => ({
            ...prev,
            [name]: formattedValue
        }));

        // Limpiar error del servidor cuando el usuario cambie el campo
        if (serverErrors[name]) {
            setServerErrors(prev => ({
                ...prev,
                [name]: null
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setServerErrors({});
        setGeneralError('');

        try {
            const dataToSubmit = mode === 'edit'
                ? { name: formData.name, phone: formData.phone, email: formData.email }
                : formData;

            await onSubmit(dataToSubmit);
            onClose();
        } catch (error) {
            const fieldErrors = getFieldErrors(error);
            if (fieldErrors) {
                setServerErrors(fieldErrors);
            } else {
                setGeneralError(getFriendlyError(error));
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 py-8 px-4">
            <div className="bg-slate-800 rounded-xl max-w-lg w-full border border-slate-700/50 max-h-[85vh] overflow-y-auto my-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
                    <h2 className="text-xl font-bold text-white">
                        {mode === 'create' ? 'Nuevo Cliente' : 'Editar Cliente'}
                    </h2>
                    <button
                        onClick={onClose}
                        className="text-slate-400 hover:text-white p-1 rounded transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    {/* Error general del servidor */}
                    {(serverErrors.general || generalError) && (
                        <div className="p-3 bg-red-500/20 border border-red-500/30 rounded-lg">
                            <p className="text-red-400 text-sm flex items-center">
                                <AlertCircle size={14} className="mr-2" />
                                {serverErrors.general || generalError}
                            </p>
                        </div>
                    )}

                    {/* Nombre */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <User size={16} className="inline mr-2" />
                            Nombre Completo *
                        </label>
                        <input
                            type="text"
                            name="name"
                            required
                            minLength={2}
                            maxLength={100}
                            value={formData.name}
                            onChange={handleInputChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 ${
                                serverErrors.name 
                                    ? 'border-red-500 focus:ring-red-500' 
                                    : 'border-slate-600 focus:ring-orange-500'
                            } transition-colors`}
                            placeholder="Ingrese el nombre completo"
                            disabled={isSubmitting}
                        />
                        {serverErrors.name && (
                            <p className="mt-1 text-sm text-red-400 flex items-center">
                                <AlertCircle size={14} className="mr-1" />
                                {serverErrors.name}
                            </p>
                        )}
                        {!serverErrors.name && (
                            <p className="text-slate-400 text-xs mt-1">Solo letras y espacios. Ej: Juan Pérez García</p>
                        )}
                    </div>

                    {/* RUT */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <Hash size={16} className="inline mr-2" />
                            RUT *
                        </label>
                        <input
                            type="text"
                            name="rut"
                            required
                            minLength={7}
                            maxLength={12}
                            value={formData.rut}
                            onChange={handleInputChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 ${
                                serverErrors.rut 
                                    ? 'border-red-500 focus:ring-red-500' 
                                    : 'border-slate-600 focus:ring-orange-500'
                            } ${mode === 'edit' ? 'opacity-60 cursor-not-allowed' : ''} transition-colors`}
                            placeholder="Ej: 12345678-9 o 12.345.678-9"
                            disabled={isSubmitting || mode === 'edit'}
                        />
                        {serverErrors.rut && (
                            <p className="mt-1 text-sm text-red-400 flex items-center">
                                <AlertCircle size={14} className="mr-1" />
                                {serverErrors.rut}
                            </p>
                        )}
                        {!serverErrors.rut && mode === 'create' && (
                            <p className="text-slate-400 text-xs mt-1">Formato: 12.345.678-9</p>
                        )}
                        {!serverErrors.rut && mode === 'edit' && (
                            <p className="text-slate-400 text-xs mt-1">El RUT no se puede modificar</p>
                        )}
                    </div>

                    {/* Teléfono */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <Phone size={16} className="inline mr-2" />
                            Teléfono *
                        </label>
                        <input
                            type="text"
                            name="phone"
                            required
                            minLength={8}
                            maxLength={15}
                            value={formData.phone}
                            onChange={handleInputChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 ${
                                serverErrors.phone 
                                    ? 'border-red-500 focus:ring-red-500' 
                                    : 'border-slate-600 focus:ring-orange-500'
                            } transition-colors`}
                            placeholder="Ej: 912345678, +56912345678, 221234567"
                            disabled={isSubmitting}
                        />
                        {serverErrors.phone && (
                            <p className="mt-1 text-sm text-red-400 flex items-center">
                                <AlertCircle size={14} className="mr-1" />
                                {serverErrors.phone}
                            </p>
                        )}
                        {!serverErrors.phone && (
                            <p className="text-slate-400 text-xs mt-1">Celular o fijo chileno</p>
                        )}
                    </div>

                    {/* Email */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <Mail size={16} className="inline mr-2" />
                            Email *
                        </label>
                        <input
                            type="email"
                            name="email"
                            required
                            value={formData.email}
                            onChange={handleInputChange}
                            className={`w-full px-3 py-2 bg-slate-700 border rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 ${
                                serverErrors.email 
                                    ? 'border-red-500 focus:ring-red-500' 
                                    : 'border-slate-600 focus:ring-orange-500'
                            } transition-colors`}
                            placeholder="cliente@email.com"
                            disabled={isSubmitting}
                        />
                        {serverErrors.email && (
                            <p className="mt-1 text-sm text-red-400 flex items-center">
                                <AlertCircle size={14} className="mr-1" />
                                {serverErrors.email}
                            </p>
                        )}
                        {!serverErrors.email && (
                            <p className="text-slate-400 text-xs mt-1">Formato válido de correo electrónico</p>
                        )}
                    </div>


                    {/* Botones */}
                    <div className="flex space-x-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 bg-slate-700 text-slate-300 hover:text-white rounded-lg hover:bg-slate-600 transition-colors border border-slate-600"
                            disabled={isSubmitting}
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="flex-1 px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center font-semibold"
                        >
                            {isSubmitting ? (
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                            ) : (
                                <Save size={16} className="mr-2" />
                            )}
                            {mode === 'create' ? 'Crear Cliente' : 'Actualizar Cliente'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ClientForm;