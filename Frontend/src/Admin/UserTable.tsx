import { memo } from 'react';
import '../Style/Admin/UserTable.css';

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

interface UserTableProps {
    user: User[]; 
    onAction: (id: string, actionType: 'edit' | 'block-unblock' | 'delete' | 'view') => void;
    actionInProgress: boolean;
}

const UserTable = memo(({ user, onAction, actionInProgress }: UserTableProps) => {

    const normalizeRole = (role: string) => {
        if (!role || typeof role !== 'string') return '';
        return role
            .normalize('NFD')               
            .replace(/[\u0300-\u036f]/g, '') 
            .toUpperCase()
            .trim();
    };

    const getRoleLabel = (role: string) => {
        const normalized = normalizeRole(role);
        switch (normalized) {
            case 'ADMIN':
                return 'Administrateur';
            case 'EDITOR':
                return 'Éditeur';
            case 'USER':
                return 'Utilisateur';
            default:
                return normalized;
        }
    };

    const getStatusLabel = (actif: boolean | string) => {
        if (actif === 'true' || actif === true) return 'Actif';
        if (actif === 'false' || actif === false) return 'Inactif';
        return actif;
    };

    if (!Array.isArray(user) || user.length === 0) {
        return (
            <div className='empty-state'>
                <h3 className='mt-4 text-lg font-medium'>Aucun utilisateur trouvé</h3>
                <p className="mt-1 text-sm">Modifiez vos critères de recherche </p>
            </div>
        );
    }

    return (
        <div className="table-container">
            <table className="user-table">
                <thead>
                    <tr>
                        <th>Utilisateur</th>
                        <th>Email</th>
                        <th>Rôle</th>
                        <th>Téléphone</th>
                        <th>Statut</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {user.map((singleUser) => (
                        <tr key={singleUser.id}>
                            <td>{singleUser.nom} {singleUser.prenom}</td>
                            <td>{singleUser.email}</td>
                            <td>
                                {singleUser.roles && singleUser.roles.length > 0
                                    ? singleUser.roles.map(r => getRoleLabel(r.name)).join(', ')
                                    : 'Aucun rôle'
                                }
                            </td>
                            <td>{singleUser.telephone}</td>
                            <td>{getStatusLabel(singleUser.actif)}</td>
                            <td>
                                <div className="actions-cell-container">
                                    <button
                                        onClick={() => onAction(singleUser.id, 'view')}
                                        disabled={actionInProgress}
                                        className="action-button view"
                                    >
                                        Voir
                                    </button>

                                    <button
                                        onClick={() => onAction(singleUser.id, 'edit')}
                                        disabled={actionInProgress}
                                        className="action-button edit"
                                    >
                                        Modifier
                                    </button>
                                    
                                    <button
                                        onClick={() => onAction(singleUser.id, 'block-unblock')}
                                        disabled={actionInProgress}
                                        className={`block-unblock ${singleUser.actif === true || singleUser.actif === 'true' ? 'is-active' : 'is-blocked'}`}
                                    >
                                        {singleUser.actif === true || singleUser.actif === 'true' ? 'Active' : 'Bloquer'}
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
});

export default UserTable;