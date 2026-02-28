import React, { useEffect, useState } from 'react';
import { X, User, Phone, Calendar, Shield } from 'lucide-react';
import { useLoans } from '../../loans/hooks/useLoans';

const ClientDetail = ({ client, isOpen, onClose }) => {
    const [stats, setStats] = useState({
        activeLoans: 0,
        completedLoans: 0,
        overdueLoans: 0,
        totalDaysOverdue: 0
    });
    const [loadingStats, setLoadingStats] = useState(true);

    const { getLoansByClient } = useLoans();

    useEffect(() => {
        if (isOpen && client) {
            loadClientStatistics();
        }
    }, [isOpen, client]);

    const loadClientStatistics = async () => {
        if (!client || !client.id) return;

        setLoadingStats(true);
        try {
            const clientLoans = await getLoansByClient(client.id);

            // Calcular estadísticas
            const active = clientLoans.filter(loan =>
                loan.status === 'ACTIVE' || loan.status === 'active'
            ).length;

            const completed = clientLoans.filter(loan =>
                loan.status === 'RETURNED' || loan.status === 'returned'
            ).length;

            const overdue = clientLoans.filter(loan => {
                if (loan.status !== 'ACTIVE' && loan.status !== 'active') return false;
                const agreedDate = new Date(loan.agreedReturnDate);
                const today = new Date();
                return agreedDate < today;
            }).length;

            // Calcular días de retraso promedio
            const overdueLoans = clientLoans.filter(loan => {
                if (loan.status !== 'ACTIVE' && loan.status !== 'active') return false;
                const agreedDate = new Date(loan.agreedReturnDate);
                const today = new Date();
                return agreedDate < today;
            });

            let totalDaysOverdue = 0;
            if (overdueLoans.length > 0) {
                overdueLoans.forEach(loan => {
                    const agreedDate = new Date(loan.agreedReturnDate);
                    const today = new Date();
                    const daysOverdue = Math.floor((today - agreedDate) / (1000 * 60 * 60 * 24));
                    totalDaysOverdue += daysOverdue;
                });
            }

            setStats({
                activeLoans: active,
                completedLoans: completed,
                overdueLoans: overdue,
                totalDaysOverdue: overdueLoans.length > 0
                    ? Math.round(totalDaysOverdue / overdueLoans.length)
                    : 0
            });
        } catch (error) {
            setStats({
                activeLoans: 0,
                completedLoans: 0,
                overdueLoans: 0,
                totalDaysOverdue: 0
            });
        } finally {
            setLoadingStats(false);
        }
    };

    if (!isOpen || !client) return null;

    const formatRUT = (rut) => {
        if (!rut) return 'Sin RUT';
        return rut;
    };

    const formatPhone = (phone) => {
        if (!phone) return 'Sin teléfono';
        const cleanPhone = phone.replace(/^\+56/, '');
        if (cleanPhone.length === 9 && cleanPhone.startsWith('9')) {
            return `+56 ${cleanPhone.substring(0, 1)} ${cleanPhone.substring(1, 5)} ${cleanPhone.substring(5)}`;
        } else if (cleanPhone.length === 8) {
            return `+56 ${cleanPhone.substring(0, 2)} ${cleanPhone.substring(2, 6)} ${cleanPhone.substring(6)}`;
        }
        return phone;
    };

    const getStatusInfo = (status) => {
        const statusConfig = {
            ACTIVE: {
                color: 'text-green-400',
                bgColor: 'bg-green-500/20',
                borderColor: 'border-green-500/30',
                label: 'Cliente Activo',
                description: 'Cliente habilitado para realizar transacciones'
            },
            RESTRICTED: {
                color: 'text-red-400',
                bgColor: 'bg-red-500/20',
                borderColor: 'border-red-500/30',
                label: 'Cliente Restringido',
                description: 'Cliente con restricciones por atrasos en pagos'
            }
        };
        return statusConfig[status] || statusConfig.RESTRICTED;
    };

    const statusInfo = getStatusInfo(client.status);

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 py-8 px-4">
            <div className="bg-slate-800 rounded-xl max-w-3xl w-full border border-slate-700/50 max-h-[85vh] overflow-y-auto my-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
                    <div className="flex items-center space-x-4">
                        <div className="h-12 w-12 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center">
                            <span className="text-white text-lg font-semibold">
                                {client.name ? client.name.substring(0, 2).toUpperCase() : 'CL'}
                            </span>
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-white">
                                {client.name}
                            </h2>
                            <p className="text-slate-400 text-sm">
                                ID: {client.id}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-700 transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="p-6 space-y-6">
                    {/* Status Card */}
                    <div className={`p-4 rounded-lg border ${statusInfo.bgColor} ${statusInfo.borderColor}`}>
                        <div className="flex items-center space-x-3 mb-2">
                            <Shield className={statusInfo.color} size={20} />
                            <h3 className={`font-semibold ${statusInfo.color}`}>
                                {statusInfo.label}
                            </h3>
                        </div>
                        <p className="text-slate-300 text-sm">
                            {statusInfo.description}
                        </p>
                    </div>

                    {/* Personal Information */}
                    <div className="bg-slate-750 p-4 rounded-lg border border-slate-600">
                        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
                            <User className="mr-2" size={20} />
                            Información Personal
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Nombre Completo
                                </label>
                                <p className="text-white">{client.name}</p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    RUT
                                </label>
                                <p className="text-white font-mono">{formatRUT(client.rut)}</p>
                            </div>
                        </div>
                    </div>

                    {/* Contact Information */}
                    <div className="bg-slate-750 p-4 rounded-lg border border-slate-600">
                        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
                            <Phone className="mr-2" size={20} />
                            Información de Contacto
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Teléfono
                                </label>
                                <p className="text-white">{formatPhone(client.phone)}</p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Email
                                </label>
                                <p className="text-white break-all">{client.email}</p>
                            </div>
                        </div>
                    </div>

                    {/* Account Activity - CON DATOS REALES */}
                    <div className="bg-slate-750 p-4 rounded-lg border border-slate-600">
                        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
                            <Calendar className="mr-2" size={20} />
                            Actividad de la Cuenta
                        </h3>
                        {loadingStats ? (
                            <div className="text-center py-8">
                                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
                                <p className="text-slate-400 mt-2">Cargando estadísticas...</p>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="text-center p-3 bg-slate-800 rounded-lg">
                                    <p className="text-2xl font-bold text-blue-400">{stats.activeLoans}</p>
                                    <p className="text-sm text-slate-400">Préstamos Activos</p>
                                </div>
                                <div className="text-center p-3 bg-slate-800 rounded-lg">
                                    <p className="text-2xl font-bold text-green-400">{stats.completedLoans}</p>
                                    <p className="text-sm text-slate-400">Préstamos Completados</p>
                                </div>
                                <div className="text-center p-3 bg-slate-800 rounded-lg">
                                    <p className={`text-2xl font-bold ${stats.totalDaysOverdue > 0 ? 'text-red-400' : 'text-yellow-400'}`}>
                                        {stats.totalDaysOverdue}
                                    </p>
                                    <p className="text-sm text-slate-400">Días de Retraso Promedio</p>
                                </div>
                            </div>
                        )}

                        {/* Alerta si hay préstamos vencidos */}
                        {!loadingStats && stats.overdueLoans > 0 && (
                            <div className="mt-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
                                <p className="text-red-400 text-sm">
                                    ⚠️ Este cliente tiene {stats.overdueLoans} préstamo(s) vencido(s)
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Footer */}
                <div className="px-6 py-4 border-t border-slate-700/50 bg-slate-750 rounded-b-xl">
                    <button
                        onClick={onClose}
                        className="w-full px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-500 transition-colors"
                    >
                        Cerrar
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ClientDetail;
