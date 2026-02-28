import React, { lazy, Suspense } from 'react';
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate, useLocation } from 'react-router-dom';

// Importa los componentes base (siempre necesarios, no lazy)
import Sidebar from './Sidebar';
import AccessDenied from './AccessDenied';
import Placeholder from './Placeholder';

// Lazy loading: solo se cargan cuando el usuario navega a esa sección
const DashboardView = lazy(() => import('./DashboardView'));
const InventoryManagement = lazy(() => import('./inventory/InventoryManagement'));
const ClientManagement = lazy(() => import('./client/ClientManagement'));
const RateManagement = lazy(() => import('./rates/RateManagement'));
const LoanManagement = lazy(() => import('./loans/LoanManagement'));
const KardexManagement = lazy(() => import('./kardex/KardexManagement'));
const ReportManagement = lazy(() => import('./reports/ReportManagement'));

// Mapa de rutas a componentes
const SECTION_ROUTES = {
    '/dashboard':  { component: (isAdmin) => <DashboardView isAdmin={isAdmin} />, adminOnly: false },
    '/inventario': { component: () => <InventoryManagement />, adminOnly: true },
    '/prestamos':  { component: () => <LoanManagement />, adminOnly: false },
    '/clientes':   { component: () => <ClientManagement />, adminOnly: true },
    '/tarifas':    { component: () => <RateManagement />, adminOnly: true },
    '/kardex':     { component: () => <KardexManagement />, adminOnly: true },
    '/reportes':   { component: () => <ReportManagement />, adminOnly: false },
};

// Spinner de fallback mientras carga la sección
const SectionLoader = () => (
    <div className="flex items-center justify-center h-64" role="status" aria-live="polite" aria-label="Cargando sección">
        <div className="text-center">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-orange-500 mx-auto mb-3" aria-hidden="true"></div>
            <p className="text-slate-400 text-sm">Cargando...</p>
        </div>
    </div>
);

const AdminPanel = () => {
    const { keycloak } = useKeycloak();
    const navigate = useNavigate();
    const location = useLocation();

    // La sección activa se deriva de la URL actual
    const activeSection = location.pathname === '/' ? 'dashboard' : location.pathname.replace('/', '');

    // --- Lógica de usuario ---
    const getUserInfo = () => ({
        username: keycloak.tokenParsed?.preferred_username || 'Usuario',
        email: keycloak.tokenParsed?.email || '',
        firstName: keycloak.tokenParsed?.given_name || '',
        lastName: keycloak.tokenParsed?.family_name || '',
        roles: keycloak.tokenParsed?.realm_access?.roles || []
    });

    const user = getUserInfo();
    const isAdmin = user.roles.includes('administrator') || user.roles.includes('admin');

    // Cambiar sección navega a la ruta correspondiente
    const setActiveSection = (sectionId) => {
        navigate(`/${sectionId}`);
    };

    const handleLogout = () => {
        if (window.confirm('¿Estás seguro de que deseas cerrar sesión?')) {
            keycloak.logout({ redirectUri: window.location.origin });
        }
    };

    // --- Renderizado del contenido principal ---
    const renderMainContent = () => {
        const path = location.pathname === '/' ? '/dashboard' : location.pathname;
        const section = SECTION_ROUTES[path];

        if (!section) {
            // Ruta desconocida → redirigir a dashboard
            const menuItem = Sidebar.menuItems.find(item => item.id === 'dashboard');
            return <Placeholder sectionLabel={menuItem?.label} />;
        }

        if (section.adminOnly && !isAdmin) {
            const menuItem = Sidebar.menuItems.find(item => `/${item.id}` === path);
            return (
                <AccessDenied
                    userRole={user.roles.join(', ')}
                    sectionLabel={menuItem?.label}
                    onNavigate={() => navigate('/dashboard')}
                />
            );
        }

        return section.component(isAdmin);
    };

    return (
        <div className="min-h-screen bg-slate-900 text-slate-300 flex">
            <Sidebar
                user={user}
                isAdmin={isAdmin}
                activeSection={activeSection}
                setActiveSection={setActiveSection}
                handleLogout={handleLogout}
            />
            <div className="flex-1 flex flex-col overflow-hidden">
                <main className="flex-1 p-6 lg:p-8 overflow-auto">
                    <div className="animate-fadeIn">
                        <Suspense fallback={<SectionLoader />}>
                            {renderMainContent()}
                        </Suspense>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default AdminPanel;