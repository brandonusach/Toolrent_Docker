import React, { useState } from 'react';
import { X, AlertTriangle, UserCheck, UserX, Info } from 'lucide-react';

const ClientStatus = ({
                          client,
                          isOpen,
                          onClose,
                          onConfirm,
                          isLoading = false
                      }) => {
    const [selectedStatus, setSelectedStatus] = useState(client?.status || 'ACTIVE');
    const [reason, setReason] = useState('');

    if (!isOpen || !client) return null;

    const statusOptions = [
        {
            value: 'ACTIVE',
            label: 'Activo',
            description: 'Cliente habilitado para realizar transacciones',
            icon: UserCheck,
            color: 'text-green-400',
            bgColor: 'bg-green-500/10',
            borderColor: 'border-green-500/30'
        },
        {
            value: 'RESTRICTED',
            label: 'Restringido',
            description: 'Cliente con restricciones por atrasos en pagos',
            icon: UserX,
            color: 'text-red-400',
            bgColor: 'bg-red-500/10',
            borderColor: 'border-red-500/30'
        }
    ];

    const currentStatusConfig = statusOptions.find(option => option.value === client.status);
    const newStatusConfig = statusOptions.find(option => option.value === selectedStatus);
    const isStatusChanging = client.status !== selectedStatus;

    const getChangeMessage = () => {
        if (selectedStatus === 'RESTRICTED') {
            return {
                title: '⚠️ Restringir Cliente',
                message: 'Esta acción restringirá al cliente para realizar nuevas transacciones.',
                color: 'text-red-400',
                bgColor: 'bg-red-500/10',
                borderColor: 'border-red-500/30'
            };
        } else {
            return {
                title: '✅ Activar Cliente',
                message: 'Esta acción habilitará al cliente para realizar transacciones normalmente.',
                color: 'text-green-400',
                bgColor: 'bg-green-500/10',
                borderColor: 'border-green-500/30'
            };
        }
    };

    const changeInfo = getChangeMessage();

    const handleConfirm = () => {
        if (onConfirm && isStatusChanging) {
            onConfirm(client.id, selectedStatus, reason);
        }
    };

    const resetForm = () => {
        setSelectedStatus(client.status);
        setReason('');
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 py-8 px-4">
            <div className="bg-slate-800 rounded-xl max-w-md w-full border border-slate-700/50 max-h-[85vh] overflow-y-auto my-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
                    <h2 className="text-xl font-bold text-white">
                        Cambiar Estado del Cliente
                    </h2>
                    <button
                        onClick={handleClose}
                        className="text-slate-400 hover:text-white p-1 rounded"
                        disabled={isLoading}
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="p-6 space-y-6">
                    {/* Client Info */}
                    <div className="bg-slate-750 p-4 rounded-lg border border-slate-600">
                        <div className="flex items-center space-x-3">
                            <div className="h-10 w-10 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center">
                                <span className="text-white text-sm font-semibold">
                                    {client.name ? client.name.substring(0, 2).toUpperCase() : 'CL'}
                                </span>
                            </div>
                            <div>
                                <p className="text-white font-medium">{client.name}</p>
                                <p className="text-slate-400 text-sm">RUT: {client.rut}</p>
                            </div>
                        </div>
                    </div>

                    {/* Current Status */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-3">
                            Estado Actual
                        </label>
                        <div className={`p-3 rounded-lg border ${currentStatusConfig.bgColor} ${currentStatusConfig.borderColor}`}>
                            <div className="flex items-center space-x-2">
                                <currentStatusConfig.icon className={currentStatusConfig.color} size={20} />
                                <span className={`font-medium ${currentStatusConfig.color}`}>
                                    {currentStatusConfig.label}
                                </span>
                            </div>
                            <p className="text-slate-300 text-sm mt-1">
                                {currentStatusConfig.description}
                            </p>
                        </div>
                    </div>

                    {/* New Status Selection */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-3">
                            Nuevo Estado
                        </label>
                        <div className="space-y-2">
                            {statusOptions.map((option) => (
                                <label
                                    key={option.value}
                                    className={`flex items-start p-3 border rounded-lg cursor-pointer transition-all ${
                                        selectedStatus === option.value
                                            ? `${option.bgColor} ${option.borderColor}`
                                            : 'border-slate-600 hover:border-slate-500'
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="status"
                                        value={option.value}
                                        checked={selectedStatus === option.value}
                                        onChange={(e) => setSelectedStatus(e.target.value)}
                                        className="sr-only"
                                        disabled={isLoading}
                                    />
                                    <div className="flex items-start space-x-3 w-full">
                                        <option.icon
                                            className={selectedStatus === option.value ? option.color : 'text-slate-400'}
                                            size={20}
                                        />
                                        <div>
                                            <p className={`font-medium ${
                                                selectedStatus === option.value ? option.color : 'text-white'
                                            }`}>
                                                {option.label}
                                            </p>
                                            <p className="text-slate-400 text-sm">
                                                {option.description}
                                            </p>
                                        </div>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Change Confirmation */}
                    {isStatusChanging && (
                        <div className={`p-4 rounded-lg border ${changeInfo.bgColor} ${changeInfo.borderColor}`}>
                            <div className="flex items-start space-x-3">
                                <AlertTriangle className={changeInfo.color} size={20} />
                                <div>
                                    <p className={`font-medium ${changeInfo.color} mb-1`}>
                                        {changeInfo.title}
                                    </p>
                                    <p className="text-slate-300 text-sm">
                                        {changeInfo.message}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Reason (optional for restrictions) */}
                    {selectedStatus === 'RESTRICTED' && (
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Motivo de Restricción (Opcional)
                            </label>
                            <textarea
                                value={reason}
                                onChange={(e) => setReason(e.target.value)}
                                placeholder="Ej: Cliente con 3 préstamos vencidos..."
                                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-orange-500 resize-none"
                                rows="3"
                                disabled={isLoading}
                            />
                        </div>
                    )}

                    {/* Info Note */}
                    <div className="bg-blue-500/10 border border-blue-500/30 p-3 rounded-lg">
                        <div className="flex items-start space-x-2">
                            <Info className="text-blue-400 mt-0.5" size={16} />
                            <p className="text-blue-400 text-sm">
                                Los clientes restringidos no podrán realizar nuevos préstamos hasta que se active su cuenta nuevamente.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="flex space-x-3 p-6 border-t border-slate-700/50">
                    <button
                        type="button"
                        onClick={handleClose}
                        className="flex-1 px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-500 transition-colors"
                        disabled={isLoading}
                    >
                        Cancelar
                    </button>
                    <button
                        type="button"
                        onClick={handleConfirm}
                        disabled={!isStatusChanging || isLoading}
                        className={`flex-1 px-4 py-2 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center ${
                            selectedStatus === 'RESTRICTED'
                                ? 'bg-red-600 hover:bg-red-700 text-white'
                                : 'bg-green-600 hover:bg-green-700 text-white'
                        }`}
                    >
                        {isLoading ? (
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                        ) : null}
                        {selectedStatus === 'RESTRICTED' ? 'Restringir Cliente' : 'Activar Cliente'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ClientStatus;
