import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { useKeycloak } from "@react-keycloak/web";
import AdminPanel from './components/panel/AdminPanel';
import './App.css';

// Componente que decide qué mostrar según el estado de autenticación
const AppContent = () => {
    const { keycloak, initialized } = useKeycloak();

    // Mostrar loading mientras Keycloak se inicializa
    if (!initialized) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-900" role="status" aria-live="polite" aria-label="Cargando aplicación ToolRent">
                <div className="text-center">
                    <div className="mb-8">
                        <div className="w-20 h-20 bg-orange-500 rounded-lg flex items-center justify-center mx-auto mb-4">
                            <svg className="w-12 h-12 text-white" aria-hidden="true" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                      d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 7.172V5L8 4z" />
                            </svg>
                        </div>
                        <h1 className="text-3xl font-bold text-white mb-2">ToolRent</h1>
                        <p className="text-orange-400">Sistema de Gestión de Herramientas</p>
                    </div>

                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4" aria-hidden="true"></div>
                    <p className="text-white">Inicializando autenticación...</p>
                    <p className="text-gray-400 text-sm mt-2">Conectando con Keycloak</p>
                </div>
            </div>
        );
    }

    // Si no está autenticado, Keycloak maneja automáticamente la redirección al login
    if (!keycloak.authenticated) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-900" role="status" aria-live="polite" aria-label="Redirigiendo al inicio de sesión">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4" aria-hidden="true"></div>
                    <p className="text-white">Redirigiendo al login...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="App">
            <AdminPanel />
        </div>
    );
};

// App principal con Router
function App() {
    try {
        return (
            <Router>
                <Routes>
                    <Route path="/*" element={<AppContent />} />
                </Routes>
            </Router>
        );
    } catch (error) {
        console.error('Error en App:', error);
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-900">
                <div className="text-center">
                    <h1 className="text-red-500 text-2xl">Error en la aplicación</h1>
                    <p className="text-gray-400 mt-2">{error.message}</p>
                    <button
                        onClick={() => window.location.reload()}
                        className="mt-4 bg-orange-500 text-white px-4 py-2 rounded hover:bg-orange-600"
                    >
                        Recargar
                    </button>
                </div>
            </div>
        );
    }
}

export default App;