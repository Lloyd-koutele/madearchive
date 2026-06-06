import React from 'react';
import '../style/hooks/FilterUsers.css';

interface UserFilters {
    nom: string;
    prenom: string;
    email: string;
    telephone: string;
    roles: string[]; 
}

interface FilterUsersProps {
    filters: UserFilters;
    onChange: (updatedFilters: UserFilters) => void;
}

function FilterUsers({ filters, onChange }: FilterUsersProps) {
    
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onChange({ ...filters, [e.target.name]: e.target.value });
    };

    const handleRoleToggle = (role: string) => {
        const currentRoles = [...filters.roles];
        const index = currentRoles.indexOf(role);

        if (index > -1) {
            currentRoles.splice(index, 1); 
        } else {
            currentRoles.push(role); 
        }

        onChange({ ...filters, roles: currentRoles });
    };

    return (
        <div className="filter-bar">
            <input 
                className="filter-input" 
                name="nom" 
                placeholder="Nom"
                aria-label="Filtrer par nom"
                value={filters.nom} 
                onChange={handleChange} 
            />
            <input 
                className="filter-input" 
                name="prenom" 
                placeholder="Prénom"
                aria-label="Filtrer par prénom"
                value={filters.prenom} 
                onChange={handleChange} 
            />
            <input 
                className="filter-input" 
                name="email" 
                placeholder="Email"
                aria-label="Filtrer par email"
                value={filters.email} 
                onChange={handleChange} 
            />
            <input 
                className="filter-input" 
                name="telephone" 
                placeholder="Téléphone"
                aria-label="Filtrer par numéro de téléphone"
                value={filters.telephone} 
                onChange={handleChange} 
            />

            <div className="filter-roles-container" role="group" aria-label="Filtrer par rôles (choix multiples)">
                <span className="filter-roles-label">Rôles :</span>
                {['ADMIN', 'EDITOR', 'USER'].map((role) => {
                    const isChecked = filters.roles.includes(role);
                    const labelMap: Record<string, string> = {
                        ADMIN: 'Administrateur',
                        EDITOR: 'Éditeur',
                        USER: 'Utilisateur'
                    };

                    return (
                        <label key={role} className={`filter-role-badge ${isChecked ? 'active' : ''}`}>
                            <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={() => handleRoleToggle(role)}
                                className="sr-only" // Cache la case native au profit d'un joli badge CSS
                            />
                            {labelMap[role]}
                        </label>
                    );
                })}
            </div>

            <button 
                className="filter-reset-btn"
                type="button"
                onClick={() => onChange({ nom: '', prenom: '', email: '', telephone: '', roles: [] })}
            >
                Réinitialiser
            </button>
        </div>
    );
}

export default FilterUsers;