import React from 'react';
import { Wrench } from 'lucide-react';

const DashboardView = ({ isAdmin }) => {
    return (
        <div className="flex items-center justify-center min-h-[60vh]">
            <div className="text-center space-y-6 max-w-2xl">
                <div className="flex justify-center mb-6">
                    <div className="p-4 rounded-full bg-gradient-to-br from-orange-500 to-orange-600 shadow-lg">
                        <Wrench className="h-12 w-12 text-white" />
                    </div>
                </div>
                <h2 className="text-4xl font-bold text-white mb-4">
                    Sistema de Gestión de Herramientas
                </h2>
                <p className="text-lg text-slate-400 leading-relaxed">
                    Plataforma integral para el control y administración de tu inventario de herramientas y operaciones de renta
                </p>
                <div className="pt-4">
                    <p className="text-sm text-slate-500">
                        Selecciona una opción del menú lateral para comenzar
                    </p>
                </div>
            </div>
        </div>
    );
};

export default DashboardView;