// loans/components/LoanForm.jsx - Formulario para crear préstamos con validación visual
import React, { useState, useEffect } from 'react';
import { X, User, Wrench, Calendar, FileText, AlertCircle, CheckCircle, Loader } from 'lucide-react';
import httpClient from "../../../../http-common";
import ToolAvailability from './ToolAvailability';
import ClientValidation from './ClientValidation';

const LoanForm = ({ onSubmit, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({
        clientId: '',
        toolId: '',
        agreedReturnDate: '',
        notes: ''
    });

    const [clients, setClients] = useState([]);
    const [tools, setTools] = useState([]);
    const [toolAvailability, setToolAvailability] = useState(null);
    const [clientValidation, setClientValidation] = useState(null);
    const [loading, setLoading] = useState(false);
    const [validating, setValidating] = useState(false);
    const [error, setError] = useState('');

    // Cargar datos iniciales
    useEffect(() => {
        loadClients();
        loadTools();
    }, []);

    // Validar cuando cambien los parámetros principales
    useEffect(() => {
        if (formData.clientId && formData.toolId) {
            validateComprehensive();
        }
    }, [formData.clientId, formData.toolId]);

    const loadClients = async () => {
        try {
            const response = await httpClient.get('/api/v1/clients/');
            console.log('Clients loaded:', response.data); // Debug
            // Mostrar TODOS los clientes activos, incluso los que tienen multas
            // La restricción se mostrará en el componente ClientValidation
            const activeClients = response.data.filter(client => client.status === 'ACTIVE');
            setClients(activeClients);
        } catch (err) {
            console.error('Error loading clients:', err);
            setError('Error al cargar los clientes');
        }
    };

    const loadTools = async () => {
        try {
            const response = await httpClient.get('/api/v1/tools/');
            console.log('Tools loaded:', response.data); // Debug
            const availableTools = response.data.filter(tool =>
                tool.status === 'AVAILABLE' && tool.currentStock > 0
            );
            setTools(availableTools);
        } catch (err) {
            console.error('Error loading tools:', err);
            setError('Error al cargar las herramientas');
        }
    };

    const validateComprehensive = async () => {
        setValidating(true);
        try {
            // Verificar si el endpoint existe, si no, hacer validación manual
            const response = await httpClient.post('/api/v1/loans/validate-comprehensive', {
                clientId: parseInt(formData.clientId),
                toolId: parseInt(formData.toolId),
                quantity: 1  // Cantidad siempre fija en 1
            }).catch(() => {
                // Si falla, usar validación basada en los componentes individuales
                return {
                    data: {
                        canCreateLoan: Boolean(
                            clientValidation?.eligible &&
                            toolAvailability?.finallyAvailable
                        ),
                        clientEligible: clientValidation?.eligible || false,
                        toolAvailable: toolAvailability?.available || false,
                        hasExistingLoanForTool: toolAvailability?.clientCheck?.hasActiveLoanForTool || false,
                        clientIssue: clientValidation?.eligible ? null : 'Cliente no elegible',
                        toolIssue: toolAvailability?.available ? null : 'Herramienta no disponible'
                    }
                };
            });

            // No necesitamos setComprehensiveValidation ya que usamos los componentes individuales
        } catch (err) {
            console.error('Error validating loan:', err);
        } finally {
            setValidating(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        setError('');
    };

    const handleToolAvailabilityChange = (availability) => {
        setToolAvailability(availability);
    };

    const handleClientValidationChange = (validation) => {
        setClientValidation(validation);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validación más detallada
        if (!formData.clientId) {
            setError('Por favor selecciona un cliente');
            return;
        }

        if (!formData.toolId) {
            setError('Por favor selecciona una herramienta');
            return;
        }

        if (!formData.agreedReturnDate) {
            setError('Por favor selecciona la fecha de devolución');
            return;
        }


        // Validar usando los datos disponibles
        const clientEligible = clientValidation?.eligible !== false;
        const toolAvailable = toolAvailability?.finallyAvailable !== false;
        const canCreate = Boolean(clientEligible && toolAvailable);

        console.log('Validation check:', {
            clientEligible,
            toolAvailable,
            canCreate,
            clientValidation,
            toolAvailability
        });

        if (!canCreate) {
            let errorMsg = 'No se puede crear el préstamo: ';
            if (!clientEligible) errorMsg += 'Cliente no elegible. ';
            if (!toolAvailable) errorMsg += 'Herramienta no disponible. ';
            setError(errorMsg);
            return;
        }

        setLoading(true);
        setError('');

        try {
            // Preparar datos con validación robusta
            const loanData = {
                clientId: Number(formData.clientId),
                toolId: Number(formData.toolId),
                quantity: 1,  // Cantidad siempre fija en 1
                agreedReturnDate: formData.agreedReturnDate,  // Enviar formato YYYY-MM-DD simple
                notes: (formData.notes || '').trim()
            };

            // Validar que los números sean válidos
            if (isNaN(loanData.clientId) || loanData.clientId <= 0) {
                throw new Error('ID de cliente inválido');
            }
            if (isNaN(loanData.toolId) || loanData.toolId <= 0) {
                throw new Error('ID de herramienta inválido');
            }

            // Validar fecha
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const returnDate = new Date(formData.agreedReturnDate + 'T00:00:00');
            if (returnDate < today) {
                throw new Error('La fecha de devolución no puede ser anterior a hoy');
            }

            console.log('Sending loan data:', loanData); // Debug

            const result = await onSubmit(loanData);
            console.log('Loan creation result:', result); // Debug

            onSuccess();
        } catch (err) {
            console.error('Error creating loan:', err);
            const errorMsg = err.response?.data?.message ||
                err.response?.data ||
                err.message ||
                'Error al crear el préstamo';
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    const getValidationIcon = (isValid) => {
        if (validating) return <Loader className="h-4 w-4 animate-spin text-orange-400" />;
        return isValid ?
            <CheckCircle className="h-4 w-4 text-green-400" /> :
            <AlertCircle className="h-4 w-4 text-red-400" />;
    };

    const selectedClient = clients.find(c => c.id === parseInt(formData.clientId));
    const selectedTool = tools.find(t => t.id === parseInt(formData.toolId));

    // Determinar fecha mínima (mañana)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const minDate = tomorrow.toISOString().split('T')[0];

    // Determinar fecha máxima (30 días desde hoy)
    const maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 30);
    const maxDateStr = maxDate.toISOString().split('T')[0];

    // Helper function to safely render values
    const safeRender = (value, fallback = 'N/A') => {
        if (value === null || value === undefined) return fallback;
        if (typeof value === 'object') return JSON.stringify(value);
        return String(value);
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-start justify-center p-4 pt-8 z-50 overflow-y-auto">
            <div className="bg-gray-800 rounded-lg shadow-xl max-w-4xl w-full mb-8 max-h-[calc(100vh-4rem)]">
                <div className="max-h-[calc(100vh-4rem)] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-gray-700 sticky top-0 bg-gray-800 z-10">
                    <h2 className="text-xl font-bold text-white">Crear Nuevo Préstamo</h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-white transition-colors"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-6">
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Columna izquierda - Formulario */}
                        <div className="space-y-6">
                            {/* Cliente */}
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-2">
                                    <User className="h-4 w-4 inline mr-1" />
                                    Cliente
                                </label>
                                <select
                                    name="clientId"
                                    value={formData.clientId}
                                    onChange={handleInputChange}
                                    required
                                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                                >
                                    <option value="">Seleccionar cliente...</option>
                                    {clients.map(client => (
                                        <option key={client.id} value={client.id}>
                                            {safeRender(client.name)} - {safeRender(client.email)}
                                        </option>
                                    ))}
                                </select>
                                {selectedClient && (
                                    <p className="text-sm text-gray-400 mt-1">
                                        Estado: {safeRender(selectedClient.status)} | Teléfono: {safeRender(selectedClient.phone)}
                                    </p>
                                )}
                            </div>

                            {/* Herramienta */}
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-2">
                                    <Wrench className="h-4 w-4 inline mr-1" />
                                    Herramienta
                                </label>
                                <select
                                    name="toolId"
                                    value={formData.toolId}
                                    onChange={handleInputChange}
                                    required
                                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                                >
                                    <option value="">Seleccionar herramienta...</option>
                                    {tools.map(tool => (
                                        <option key={tool.id} value={tool.id}>
                                            {safeRender(tool.name)} - Stock: {safeRender(tool.currentStock)} - ${safeRender(tool.rentalRate)}/día
                                        </option>
                                    ))}
                                </select>
                                {selectedTool && (
                                    <p className="text-sm text-gray-400 mt-1">
                                        Categoría: {safeRender(selectedTool.category?.name)} | Tarifa: ${safeRender(selectedTool.rentalRate)}/día | Estado: {safeRender(selectedTool.status)}
                                    </p>
                                )}
                            </div>


                            {/* Fecha de devolución acordada */}
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-2">
                                    <Calendar className="h-4 w-4 inline mr-1" />
                                    Fecha de Devolución Acordada
                                </label>
                                <input
                                    type="date"
                                    name="agreedReturnDate"
                                    value={formData.agreedReturnDate}
                                    onChange={handleInputChange}
                                    min={minDate}
                                    max={maxDateStr}
                                    required
                                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                                />
                                <p className="text-xs text-gray-400 mt-1">
                                    Debe ser entre mañana y {maxDate.toLocaleDateString('es-ES')}
                                </p>
                            </div>

                            {/* Notas */}
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-2">
                                    <FileText className="h-4 w-4 inline mr-1" />
                                    Notas (Opcional)
                                </label>
                                <textarea
                                    name="notes"
                                    value={formData.notes}
                                    onChange={handleInputChange}
                                    rows={3}
                                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-md text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
                                    placeholder="Información adicional sobre el préstamo..."
                                />
                            </div>
                        </div>

                        {/* Columna derecha - Validaciones */}
                        <div className="space-y-6">
                            {/* Validación del cliente */}
                            {formData.clientId && (
                                <ClientValidation
                                    clientId={parseInt(formData.clientId)}
                                    onValidationChange={handleClientValidationChange}
                                />
                            )}

                            {/* Disponibilidad de herramienta */}
                            {formData.toolId && (
                                <ToolAvailability
                                    toolId={parseInt(formData.toolId)}
                                    quantity={1}
                                    clientId={formData.clientId ? parseInt(formData.clientId) : null}
                                    onAvailabilityChange={handleToolAvailabilityChange}
                                />
                            )}
                        </div>
                    </div>

                    {/* Validación comprensiva simplificada */}
                    {(clientValidation || toolAvailability) && (
                        <div className="bg-gray-700 rounded-lg p-4 border border-gray-600">
                            <h3 className="text-sm font-medium text-white mb-3">Resumen de Validación</h3>
                            <div className="space-y-2">
                                <div className="flex items-center">
                                    {getValidationIcon(clientValidation?.eligible)}
                                    <span className={`ml-2 text-sm ${
                                        clientValidation?.eligible ? 'text-green-400' : 'text-red-400'
                                    }`}>
                                        Cliente elegible
                                    </span>
                                    {clientValidation && !clientValidation.eligible && (
                                        <span className="ml-2 text-xs text-gray-400">
                                            - Revisar restricciones
                                        </span>
                                    )}
                                </div>

                                <div className="flex items-center">
                                    {getValidationIcon(toolAvailability?.available)}
                                    <span className={`ml-2 text-sm ${
                                        toolAvailability?.available ? 'text-green-400' : 'text-red-400'
                                    }`}>
                                        Herramienta disponible
                                    </span>
                                    {toolAvailability?.issue && (
                                        <span className="ml-2 text-xs text-gray-400">
                                            - {toolAvailability.issue}
                                        </span>
                                    )}
                                </div>

                                <div className="flex items-center">
                                    {getValidationIcon(!toolAvailability?.clientCheck?.hasActiveLoanForTool)}
                                    <span className={`ml-2 text-sm ${
                                        !toolAvailability?.clientCheck?.hasActiveLoanForTool ? 'text-green-400' : 'text-red-400'
                                    }`}>
                                        Sin préstamo existente de esta herramienta
                                    </span>
                                </div>

                                {/* Estado final */}
                                <div className={`mt-4 p-3 rounded-lg border ${
                                    (clientValidation?.eligible && toolAvailability?.finallyAvailable)
                                        ? 'bg-green-900 border-green-700'
                                        : 'bg-red-900 border-red-700'
                                }`}>
                                    <div className="flex items-center">
                                        {(clientValidation?.eligible && toolAvailability?.finallyAvailable) ? (
                                            <CheckCircle className="h-5 w-5 text-green-400 mr-2" />
                                        ) : (
                                            <AlertCircle className="h-5 w-5 text-red-400 mr-2" />
                                        )}
                                        <span className={`font-medium ${
                                            (clientValidation?.eligible && toolAvailability?.finallyAvailable) ? 'text-green-200' : 'text-red-200'
                                        }`}>
                                            {(clientValidation?.eligible && toolAvailability?.finallyAvailable)
                                                ? 'Préstamo puede ser creado'
                                                : 'No se puede crear el préstamo'
                                            }
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Error */}
                    {error && (
                        <div className="bg-red-900 border border-red-700 rounded-md p-3">
                            <div className="flex items-center">
                                <AlertCircle className="h-4 w-4 text-red-400 mr-2" />
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
                            disabled={loading || !(clientValidation?.eligible && toolAvailability?.finallyAvailable) || validating}
                            className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center"
                        >
                            {loading && <Loader className="h-4 w-4 mr-2 animate-spin" />}
                            {loading ? 'Creando...' : 'Crear Préstamo'}
                        </button>
                    </div>
                </form>
                </div>
            </div>
        </div>
    );
};

export default LoanForm;