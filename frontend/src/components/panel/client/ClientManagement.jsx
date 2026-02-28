import { useState, useEffect } from 'react';
import { Plus, Users, UserCheck, UserX } from 'lucide-react';

// --- ESTOS COMPONENTES, HOOKS Y UTILS NO SE MODIFICAN ---
import ClientList from './components/ClientList';
import ClientForm from './components/ClientForm';
import ClientDetail from './components/ClientDetail';
import ClientSearch from './components/ClientSearch';
import { useClients } from './hooks/useClients';
import { CLIENT_STATUS, PERMISSIONS, hasPermission } from './utils/ClientConstants';

const ClientManagement = () => {
    // La lógica existente del componente no necesita cambios
    const isAdmin = true;

    const [selectedClient, setSelectedClient] = useState(null);
    const [showAddModal, setShowAddModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [searchCriteria, setSearchCriteria] = useState({ general: '', status: 'ALL' });
    const { clients, loading, error, loadClients, createClient, updateClient, deleteClient, clearError } = useClients();

    useEffect(() => {
        loadClients();
    }, [loadClients]);

    const handleViewClient = (client) => { setSelectedClient(client); setShowDetailModal(true); };
    const handleEditClient = (client) => { if (hasPermission(isAdmin ? 'admin' : 'user', PERMISSIONS.CLIENT.UPDATE)) { setSelectedClient(client); setShowEditModal(true); } else { alert('No tiene permisos'); } };
    const handleAddClient = () => { if (hasPermission(isAdmin ? 'admin' : 'user', PERMISSIONS.CLIENT.CREATE)) { setShowAddModal(true); } else { alert('No tiene permisos'); } };

    const closeAllModals = () => {
        setSelectedClient(null);
        setShowAddModal(false);
        setShowEditModal(false);
        setShowDetailModal(false);
        if (clearError) clearError();
    };

    const handleCreateClient = async (clientData) => { await createClient(clientData); closeAllModals(); };
    const handleUpdateClient = async (clientId, clientData) => { await updateClient(clientId, clientData); closeAllModals(); };
    const handleDeleteClient = async (client) => { if (hasPermission(isAdmin ? 'admin' : 'user', PERMISSIONS.CLIENT.DELETE) && window.confirm(`¿Estás seguro de que deseas eliminar al cliente "${client.name}"? Esta acción no se puede deshacer.`)) { await deleteClient(client.id); closeAllModals(); } };
    const handleSearch = (criteria) => { setSearchCriteria(criteria); };
    const handleClearSearch = () => { setSearchCriteria({ general: '', status: 'ALL' }); };

    const getFilteredClients = () => {
        let filtered = [...clients];
        if (searchCriteria.general) {
            const term = searchCriteria.general.toLowerCase().trim();
            filtered = filtered.filter(c =>
                c.name?.toLowerCase().includes(term) ||
                c.rut?.toLowerCase().includes(term)
            );
        }
        if (searchCriteria.status && searchCriteria.status !== 'ALL') {
            filtered = filtered.filter(c => c.status === searchCriteria.status);
        }
        return filtered;
    };

    const filteredClients = getFilteredClients();
    const displayStats = {
        totalClients: clients?.length || 0,
        activeClients: clients?.filter(c => c.status === CLIENT_STATUS.ACTIVE).length || 0,
        restrictedClients: clients?.filter(c => c.status === CLIENT_STATUS.RESTRICTED).length || 0,
    };

    // --- EL CÓDIGO JSX SE ACTUALIZA AL NUEVO DISEÑO ---
    return (
        <div>
            {/* Header y Stats */}
            <div className="mb-8">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
                    <div>
                        <h1 className="text-3xl font-bold text-white mb-2">
                            Gestión de Clientes
                        </h1>
                        <p className="text-slate-400">
                            Administra la información y estados de los clientes.
                        </p>
                    </div>

                    {hasPermission(isAdmin ? 'admin' : 'user', PERMISSIONS.CLIENT.CREATE) && (
                        <button
                            onClick={handleAddClient}
                            className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-5 py-2.5 rounded-lg font-semibold transition-colors duration-200 shadow-lg hover:shadow-orange-500/30 mt-4 sm:mt-0"
                        >
                            <Plus size={20} />
                            Nuevo Cliente
                        </button>
                    )}
                </div>

                {/* Stat Cards rediseñadas */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                        <div className="flex items-start justify-between">
                            <div className="space-y-1">
                                <p className="text-sm font-medium text-slate-400">Total Clientes</p>
                                <p className="text-3xl font-bold text-white">{displayStats.totalClients}</p>
                            </div>
                            <div className="p-3 rounded-lg bg-gradient-to-br from-sky-500 to-sky-600">
                                <Users className="h-6 w-6 text-white" />
                            </div>
                        </div>
                    </div>
                    <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                        <div className="flex items-start justify-between">
                            <div className="space-y-1">
                                <p className="text-sm font-medium text-slate-400">Clientes Activos</p>
                                <p className="text-3xl font-bold text-emerald-400">{displayStats.activeClients}</p>
                            </div>
                            <div className="p-3 rounded-lg bg-gradient-to-br from-emerald-500 to-emerald-600">
                                <UserCheck className="h-6 w-6 text-white" />
                            </div>
                        </div>
                    </div>
                    <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
                        <div className="flex items-start justify-between">
                            <div className="space-y-1">
                                <p className="text-sm font-medium text-slate-400">Restringidos</p>
                                <p className="text-3xl font-bold text-red-400">{displayStats.restrictedClients}</p>
                            </div>
                            <div className="p-3 rounded-lg bg-gradient-to-br from-red-500 to-red-600">
                                <UserX className="h-6 w-6 text-white" />
                            </div>
                        </div>
                    </div>
                </div>

                <ClientSearch onSearch={handleSearch} onClear={handleClearSearch} />
            </div>

            {/* Resumen de Resultados */}
            {filteredClients.length !== clients.length && (
                <div className="mb-4 text-slate-400 text-sm">
                    Mostrando {filteredClients.length} de {clients.length} clientes
                </div>
            )}

            {/* Contenedor principal de la lista */}
            <div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 rounded-lg shadow-lg overflow-hidden">
                {loading ? (
                    <div className="flex items-center justify-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
                        <span className="ml-4 text-slate-400 text-lg">Cargando clientes...</span>
                    </div>
                ) : error ? (
                    <div className="flex items-center justify-center py-20">
                        <div className="text-center">
                            <p className="text-red-400 mb-4">Error al cargar clientes: {error}</p>
                            <button onClick={loadClients} className="font-semibold text-orange-400 hover:text-orange-300 transition-colors">
                                Intentar nuevamente
                            </button>
                        </div>
                    </div>
                ) : (
                    <ClientList
                        clients={filteredClients}
                        onViewClient={handleViewClient}
                        onEditClient={handleEditClient}
                        onDeleteClient={handleDeleteClient}
                        isAdmin={isAdmin}
                    />
                )}
            </div>

            {/* Los modales no necesitan cambios de estilo ya que son overlays */}
            {showAddModal && <ClientForm mode="create" onClose={closeAllModals} onSubmit={handleCreateClient} />}
            {showEditModal && selectedClient && <ClientForm mode="edit" client={selectedClient} onClose={closeAllModals} onSubmit={(data) => handleUpdateClient(selectedClient.id, data)} />}
            {showDetailModal && selectedClient && <ClientDetail client={selectedClient} isOpen={showDetailModal} onClose={closeAllModals} />}
        </div>
    );
};

export default ClientManagement;