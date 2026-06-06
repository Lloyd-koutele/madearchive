import { useState, useEffect } from 'react';
import CreateUser from "./CreateUser";
import Sidebar from "../Page/Sidebar";
import UserTable from "./UserTable";
import UpdateUser from "./UpdateUser";
import Modal from "../Page/Modal";
import Profile from "../Page/Profil";
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
    roles: string[]; // Synchronisé avec la modification de FilterUsers
}

function AdminDashboard() {
    const [users, setUsers] = useState<User[]>([]);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState<boolean>(false);
    const [isViewModalOpen, setIsViewModalOpen] = useState<boolean>(false);
    
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [viewingUser, setViewingUser] = useState<User | null>(null);
    
    const [actionInProgress, setActionInProgress] = useState<boolean>(false);
    const [error, setError] = useState<string>("");
    const [success, setSuccess] = useState<string>("");
    
    const userInfo = getCurrentUserInfo(); 
    const [isOpen, setIsOpen] = useState<boolean>(true);
    
    // 💡 Gestion de l'affichage dans le main-content ('list' affiche le tableau complet, 'profile' affiche le composant de l'admin)
    const [currentView, setCurrentView] = useState<'list' | 'profile'>('list');
    
    const [filters, setFilters] = useState<UserFilters>({
        nom: '', prenom: '', email: '', telephone: '', roles: []
    });

    const [currentPage, setCurrentPage] = useState<number>(1);
    const ITEMS_PER_PAGE = 10;

    useEffect(() => {
        fetchUsers();
    }, []);

    useEffect(() => {
        if (error || success) {
            const timer = setTimeout(() => {
                setError("");
                setSuccess("");
            }, 2000);
            return () => clearTimeout(timer);
        }
    }, [error, success]);

    const fetchUsers = async () => {
        try {
            const data = await AllUsers();
            setUsers(data);
        } catch (err) {
            console.error("Erreur lors de la récupération des utilisateurs: ", err);
            setError("Erreur lors de la récupération des utilisateurs");
        }
    };

    const handleAction = async (userId: string, action: 'edit' | 'block-unblock' | 'delete' | 'view') => {
        setError("");
        setSuccess("");
        
        const targetUser = users.find((u) => u.id === userId);
        if (!targetUser) return;

        if (action === "view") {
            setViewingUser(targetUser);
            setIsViewModalOpen(true);
            return;
        }

        if (action === "edit") {
            setSelectedUser(targetUser);
            setIsUpdateModalOpen(true);
            return;
        }

        if (action === "block-unblock") {
            setActionInProgress(true);
            try {
                const isActif = targetUser.actif === true || targetUser.actif === "true";
                const newStatus = !isActif;
                await updateStatus(userId, { actif: newStatus }); 
                
                setSuccess("Statut de l'utilisateur mis à jour avec succès");
                await fetchUsers();
            } catch (err) {
                console.error("Erreur lors du changement de statut: ", err);
                setError("Erreur lors du changement de statut");
            } finally {
                setActionInProgress(false);
            }
        }
    };

    const handleCloseModal = () => {
        setIsCreateModalOpen(false);
        setIsUpdateModalOpen(false);
        setIsViewModalOpen(false);
        setSelectedUser(null);
        setViewingUser(null);
    };

    const handleUserUpdated = () => {
        fetchUsers();
        handleCloseModal();
        setSuccess("Opération effectuée avec succès");
    };

    const handleFilterChange = (newFilters: UserFilters) => {
        setFilters(newFilters);
        setCurrentPage(1);
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
                {}
                <Sidebar title={userInfo?.role || "ADMINISTRATEUR"}>
                    <nav className="sidebar-nav">
                        <div>
                            <div className="main-header">
                                <button
                                    onClick={() => setCurrentView('profile')}
                                    className={`sidebar-btn ${currentView === 'profile' ? 'active-tab' : ''}`}
                                >
                                    👤 Mon Profil
                                </button>
                            </div>

                            <div className="main-header">
                                <button
                                    onClick={() => {
                                        setCurrentView('list'); // S'assure que la liste se ré-affiche au besoin
                                        setIsCreateModalOpen(true);
                                    }}
                                    className="sidebar-btn"
                                >
                                    Créer un utilisateur
                                </button>
                            </div>

                            <div className="main-header">
                                <button
                                    onClick={() => {
                                        setCurrentView('list'); // Forcer le retour visuel sur la liste
                                        setIsOpen(!isOpen);
                                    }}
                                    className={`sidebar-btn ${currentView === 'list' && isOpen ? 'active-tab' : ''}`}
                                >
                                    {isOpen && currentView === 'list' ? "Masquer la liste" : "Liste des utilisateurs"}
                                </button>
                            </div>

                            {error && <div className="alert alert-error">{error}</div>}
                            {success && <div className="alert alert-success">{success}</div>}
                        </div>
                    </nav>
                </Sidebar>

                <div className='main-content'>
                    {/* 💡 Aiguillage dynamique du contenu principal */}
                    {currentView === 'list' ? (
                        isOpen && (
                            <>
                                <FilterUsers filters={filters} onChange={handleFilterChange} />

                                <p className="users-count">
                                    <span>{filteredUsers.length}</span> utilisateur{filteredUsers.length > 1 ? 's' : ''}
                                    {filteredUsers.length !== users.length && <> sur <span>{users.length}</span></>}
                                </p>

                                <UserTable
                                    user={paginatedUsers}
                                    // 💡 Modification de la signature pour inclure l'action 'view'
                                    onAction={(id, action) => handleAction(id, action as any)}
                                    actionInProgress={actionInProgress}
                                />

                                <Pagination
                                    currentPage={currentPage}
                                    totalPages={totalPages}
                                    onChange={setCurrentPage}
                                />
                            </>
                        )
                    ) : (
                        /* 💡 Rendu de ton composant profil autonome, sans aucun style inline */
                        <Profile userId={userInfo?.id} />
                    )}
                </div>

                {/* 1. MODAL : CRÉATION */}
                <Modal
                    isOpen={isCreateModalOpen}
                    onClose={handleCloseModal}
                    title="Créer un utilisateur"
                >
                    {/* 💡 Correction 3 : 'onSuccess' harmonisé en 'onsuccess' */}
                    <CreateUser onsuccess={handleUserUpdated} />
                </Modal>

                {/* 2. MODAL : MODIFICATION */}
                <Modal
                    isOpen={isUpdateModalOpen}
                    onClose={handleCloseModal}
                    title="Mettre à jour un utilisateur"
                >
                    {selectedUser && (
                        /* 💡 Correction 4 : Transfert via 'initialData' et 'onsuccess' */
                        <UpdateUser
                            initialData={selectedUser}
                            onsuccess={handleUserUpdated}
                        />
                    )}
                </Modal>

                {/* 3. 💡 NOUVEAU MODAL : AFFICHAGE DÉTAILLÉ */}
                <Modal
                    isOpen={isViewModalOpen}
                    onClose={handleCloseModal}
                    title="Détails de l'utilisateur"
                >
                    {viewingUser && (
                        <div className="user-details-card">
                            <div className="details-row"><strong>Identifiant unique :</strong> {viewingUser.id}</div>
                            <div className="details-row"><strong>Nom complet :</strong> {viewingUser.nom} {viewingUser.prenom}</div>
                            <div className="details-row"><strong>Adresse Email :</strong> {viewingUser.email}</div>
                            <div className="details-row"><strong>Numéro de Téléphone :</strong> {viewingUser.telephone}</div>
                            <div className="details-row">
                                <strong>Statut du Compte :</strong> 
                                <span className={`status-tag ${viewingUser.actif === true || viewingUser.actif === 'true' ? 'active' : 'inactive'}`}>
                                    {viewingUser.actif === true || viewingUser.actif === 'true' ? ' Actif' : ' Bloqué / Inactif'}
                                </span>
                            </div>
                            <div className="details-row">
                                <strong>Rôles attribués :</strong> {viewingUser.roles?.map(r => r.name).join(', ') || 'Aucun'}
                            </div>
                            <button onClick={handleCloseModal} className="details-close-btn">
                                Fermer
                            </button>
                        </div>
                    )}
                </Modal>
            </div>
        </div>
    );
}

export default AdminDashboard;