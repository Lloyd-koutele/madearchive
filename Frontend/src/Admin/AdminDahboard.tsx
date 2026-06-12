import { useState, useEffect } from 'react';
import CreateUser from "./CreateUser";
import Sidebar from "../Page/Sidebar";
import UserTable from "./UserTable";
import UpdateUser from "./UpdateUser";
import Modal from "../Page/Modal";
import Profile from "../Page/Profil";
import TypeDocumentList from "../document/TypedocumentList";
import CreateTypeDocument from "../document/Createtypedocument";
import { getAllUsers as AllUsers } from "../services/admin/AdminService";
import { updateUserStatus as updateStatus } from "../services/admin/AdminService";
import "../style/Admin/AdminDashboard.css";
import { getCurrentUserInfo } from "../auth/authService";
import FilterUsers from "../hooks/FilterUsers";
import Pagination from "../hooks/Pagination";

interface RoleField {
    name: "ADMIN" | "EDITOR" | "USER";
}

interface User {
    id: string;
    nom: string;
    prenom: string;
    email: string;
    telephone: string;
    actif: boolean | string;
    roles: RoleField[];
}

interface UserFilters {
    nom: string;
    prenom: string;
    email: string;
    telephone: string;
    roles: string[];
}

// Vues disponibles dans le main-content
type AdminView = 'users' | 'profile' | 'type-documents';

function AdminDashboard() {
    const [users, setUsers] = useState<User[]>([]);
    const [isCreateUserModalOpen, setIsCreateUserModalOpen] = useState(false);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
    const [isViewModalOpen, setIsViewModalOpen] = useState(false);
    const [isCreateTdModalOpen, setIsCreateTdModalOpen] = useState(false);

    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [viewingUser, setViewingUser] = useState<User | null>(null);

    const [actionInProgress, setActionInProgress] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const userInfo = getCurrentUserInfo();
    const [currentView, setCurrentView] = useState<AdminView>('users');
    const [tdRefresh, setTdRefresh] = useState(0);

    const [filters, setFilters] = useState<UserFilters>({
        nom: '', prenom: '', email: '', telephone: '', roles: []
    });
    const [currentPage, setCurrentPage] = useState(1);
    const ITEMS_PER_PAGE = 10;

    useEffect(() => { fetchUsers(); }, []);

    useEffect(() => {
        if (error || success) {
            const t = setTimeout(() => { setError(''); setSuccess(''); }, 2500);
            return () => clearTimeout(t);
        }
    }, [error, success]);

    const fetchUsers = async () => {
        try {
            const data = await AllUsers();
            setUsers(data);
        } catch {
            setError("Erreur lors de la récupération des utilisateurs");
        }
    };

    const handleAction = async (userId: string, action: 'edit' | 'block-unblock' | 'delete' | 'view') => {
        setError(''); setSuccess('');
        const targetUser = users.find(u => u.id === userId);
        if (!targetUser) return;

        if (action === 'view') { setViewingUser(targetUser); setIsViewModalOpen(true); return; }
        if (action === 'edit') { setSelectedUser(targetUser); setIsUpdateModalOpen(true); return; }

        if (action === 'block-unblock') {
            setActionInProgress(true);
            try {
                const isActif = targetUser.actif === true || targetUser.actif === 'true';
                await updateStatus(userId, { actif: !isActif });
                setSuccess("Statut mis à jour avec succès");
                await fetchUsers();
            } catch {
                setError("Erreur lors du changement de statut");
            } finally {
                setActionInProgress(false);
            }
        }
    };

    const handleCloseModal = () => {
        setIsCreateUserModalOpen(false);
        setIsUpdateModalOpen(false);
        setIsViewModalOpen(false);
        setIsCreateTdModalOpen(false);
        setSelectedUser(null);
        setViewingUser(null);
    };

    const handleUserUpdated = () => {
        fetchUsers();
        handleCloseModal();
        setSuccess("Opération effectuée avec succès");
    };

    const handleTdCreated = () => {
        handleCloseModal();
        setTdRefresh(r => r + 1);
        setSuccess("Type de document créé avec succès");
    };

    const filteredUsers = users.filter(u =>
        (u.nom?.toLowerCase() || '').includes(filters.nom.toLowerCase()) &&
        (u.prenom?.toLowerCase() || '').includes(filters.prenom.toLowerCase()) &&
        (u.email?.toLowerCase() || '').includes(filters.email.toLowerCase()) &&
        (u.telephone?.toLowerCase() || '').includes(filters.telephone.toLowerCase()) &&
        (filters.roles.length === 0 || u.roles?.some(r => filters.roles.includes(r.name)))
    );

    const totalPages = Math.ceil(filteredUsers.length / ITEMS_PER_PAGE);
    const paginatedUsers = filteredUsers.slice(
        (currentPage - 1) * ITEMS_PER_PAGE,
        currentPage * ITEMS_PER_PAGE
    );

    return (
        <div className="admin-dashboard">
            <div className="admin-body">

                <Sidebar title={userInfo?.role || "ADMINISTRATEUR"}>
                    <nav className="sidebar-nav">
                        <div>
                            {/* Profil */}
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('profile')}
                                    className={`sidebar-btn ${currentView === 'profile' ? 'active-tab' : ''}`}
                                >
                                    👤 Mon Profil
                                </button>
                            </div>

                            {/* Section Utilisateurs */}
                            <div className="sidebar-section-label">Utilisateurs</div>
                            <div className="main-header">
                                <button
                                    onClick={() => { setCurrentView('users'); setIsCreateUserModalOpen(true); }}
                                    className="sidebar-btn"
                                >
                                    Créer un utilisateur
                                </button>
                            </div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('users')}
                                    className={`sidebar-btn ${currentView === 'users' ? 'active-tab' : ''}`}
                                >
                                    Liste des utilisateurs
                                </button>
                            </div>

                            {/* Section Types de documents */}
                            <div className="sidebar-section-label">Documents</div>
                            <div className="main-header">
                                <button
                                    onClick={() => { setCurrentView('type-documents'); setIsCreateTdModalOpen(true); }}
                                    className="sidebar-btn"
                                >
                                    Créer un type
                                </button>
                            </div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('type-documents')}
                                    className={`sidebar-btn ${currentView === 'type-documents' ? 'active-tab' : ''}`}
                                >
                                    Types de documents
                                </button>
                            </div>

                            {error && <div className="alert alert-error">{error}</div>}
                            {success && <div className="alert alert-success">{success}</div>}
                        </div>
                    </nav>
                </Sidebar>

                {/* Main content */}
                <div className="main-content">

                    {currentView === 'profile' && (
                        <Profile userId={userInfo?.id} />
                    )}

                    {currentView === 'users' && (
                        <>
                            <FilterUsers
                                filters={filters}
                                onChange={(f) => { setFilters(f); setCurrentPage(1); }}
                            />
                            <p className="users-count">
                                <span>{filteredUsers.length}</span> utilisateur{filteredUsers.length > 1 ? 's' : ''}
                                {filteredUsers.length !== users.length && <> sur <span>{users.length}</span></>}
                            </p>
                            <UserTable
                                user={paginatedUsers}
                                onAction={(id, action) => handleAction(id, action as any)}
                                actionInProgress={actionInProgress}
                            />
                            <Pagination
                                currentPage={currentPage}
                                totalPages={totalPages}
                                onChange={setCurrentPage}
                            />
                        </>
                    )}

                    {currentView === 'type-documents' && (
                        <TypeDocumentList refreshTrigger={tdRefresh} />
                    )}
                </div>

                {/* Modal : créer utilisateur */}
                <Modal isOpen={isCreateUserModalOpen} onClose={handleCloseModal} title="Créer un utilisateur">
                    <CreateUser onsuccess={handleUserUpdated} />
                </Modal>

                {/* Modal : modifier utilisateur */}
                <Modal isOpen={isUpdateModalOpen} onClose={handleCloseModal} title="Mettre à jour un utilisateur">
                    {selectedUser && <UpdateUser initialData={selectedUser} onsuccess={handleUserUpdated} />}
                </Modal>

                {/* Modal : voir utilisateur */}
                <Modal isOpen={isViewModalOpen} onClose={handleCloseModal} title="Détails de l'utilisateur">
                    {viewingUser && (
                        <div className="user-details-card">
                            <div className="details-row"><strong>Identifiant :</strong> {viewingUser.id}</div>
                            <div className="details-row"><strong>Nom complet :</strong> {viewingUser.nom} {viewingUser.prenom}</div>
                            <div className="details-row"><strong>Email :</strong> {viewingUser.email}</div>
                            <div className="details-row"><strong>Téléphone :</strong> {viewingUser.telephone}</div>
                            <div className="details-row">
                                <strong>Statut :</strong>
                                <span className={`status-tag ${viewingUser.actif === true || viewingUser.actif === 'true' ? 'active' : 'inactive'}`}>
                                    {viewingUser.actif === true || viewingUser.actif === 'true' ? 'Actif' : 'Bloqué'}
                                </span>
                            </div>
                            <div className="details-row">
                                <strong>Rôles :</strong> {viewingUser.roles?.map(r => r.name).join(', ') || 'Aucun'}
                            </div>
                            <button onClick={handleCloseModal} className="details-close-btn">Fermer</button>
                        </div>
                    )}
                </Modal>

                {/* Modal : créer type de document */}
                <Modal isOpen={isCreateTdModalOpen} onClose={handleCloseModal} title="Créer un type de document">
                    <CreateTypeDocument onsuccess={handleTdCreated} />
                </Modal>

            </div>
        </div>
    );
}

export default AdminDashboard;
