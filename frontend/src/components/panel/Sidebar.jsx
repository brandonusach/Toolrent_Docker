import React from 'react';
import {
    Wrench, RefreshCw, Users, DollarSign, FileText,
    BarChart3, Home, AlertTriangle, LogOut
} from 'lucide-react';

export const menuItems = [
    { id: 'dashboard', icon: Home, label: 'Inicio', adminOnly: false },
    { id: 'inventario', icon: Wrench, label: 'Gestión de Inventario', adminOnly: true },
    { id: 'prestamos', icon: RefreshCw, label: 'Préstamos y Devoluciones', adminOnly: false },
    { id: 'clientes', icon: Users, label: 'Gestión de Clientes', adminOnly: true },
    { id: 'tarifas', icon: DollarSign, label: 'Tarifas y Montos', adminOnly: true },
    { id: 'kardex', icon: FileText, label: 'Kardex y Movimientos', adminOnly: true },
    { id: 'reportes', icon: BarChart3, label: 'Reportes y Consultas', adminOnly: false }
];

const Sidebar = ({ user, isAdmin, activeSection, setActiveSection, handleLogout }) => {
    const getRoleDisplayName = () => {
        const roles = user.roles;
        if (roles.includes('administrator') || roles.includes('admin')) return 'Administrador';
        if (roles.includes('employee') || roles.includes('empleado')) return 'Empleado';
        return 'Usuario';
    };

    const getUserInitials = () => {
        if (user.firstName && user.lastName) {
            return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
        }
        return user.username.substring(0, 2).toUpperCase();
    };

    return (
        <aside className="w-72 bg-slate-800/50 backdrop-blur-sm border-r border-slate-700/50 flex flex-col">
            {/* Header */}
            <div className="p-6 flex items-center gap-4 border-b border-slate-700/50">
                <div className="bg-gradient-to-br from-orange-500 to-orange-600 p-3 rounded-lg shadow-lg">
                    <Wrench className="h-8 w-8 text-white" />
                </div>
                <div>
                    <h1 className="text-xl font-bold text-white">ToolRent</h1>
                    <p className="text-sm text-slate-400">Panel de Control</p>
                </div>
            </div>

            {/* Navegación */}
            <nav className="flex-1 p-4 space-y-2">
                {menuItems.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeSection === item.id;

                    return (
                        <button
                            key={item.id}
                            onClick={() => setActiveSection(item.id)}
                            aria-label={`Ir a ${item.label}`}
                            aria-current={isActive ? 'page' : undefined}
                            className={`w-full flex items-center px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ease-in-out relative group ${
                                isActive
                                    ? 'bg-orange-500/10 text-orange-400'
                                    : 'text-slate-400 hover:bg-slate-700/50 hover:text-white'
                            }`}
                        >
                            <span className={`absolute left-0 top-0 h-full w-1 bg-orange-500 rounded-r-full transition-transform duration-300 ease-out ${isActive ? 'scale-y-100' : 'scale-y-0'}`}></span>

                            <Icon className={`h-5 w-5 mr-3 transition-colors ${isActive ? 'text-orange-400' : 'text-slate-500 group-hover:text-white'}`} />
                            <span>{item.label}</span>

                            {item.adminOnly && !isAdmin && (
                                <AlertTriangle className="h-4 w-4 text-yellow-600 ml-auto" title="Acceso restringido" />
                            )}
                        </button>
                    );
                })}
            </nav>

            {/* Perfil de Usuario */}
            <div className="p-4 border-t border-slate-700/50">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className={`h-10 w-10 rounded-full flex items-center justify-center font-bold text-white text-sm bg-gradient-to-br ${
                            isAdmin ? 'from-red-500 to-red-600' : 'from-sky-500 to-sky-600'
                        }`}>
                            {getUserInitials()}
                        </div>
                        <div>
                            <p className="text-white text-sm font-semibold">
                                {user.firstName ? `${user.firstName} ${user.lastName}` : user.username}
                            </p>
                            <p className="text-slate-400 text-xs">{getRoleDisplayName()}</p>
                        </div>
                    </div>
                    <button
                        onClick={handleLogout}
                        className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                        title="Cerrar Sesión"
                        aria-label="Cerrar sesión"
                    >
                        <LogOut className="h-5 w-5" />
                    </button>
                </div>
            </div>
        </aside>
    );
};

// **CORRECCIÓN:** Mueve la línea aquí, después de que 'Sidebar' ha sido definido.
Sidebar.menuItems = menuItems;

export default Sidebar;