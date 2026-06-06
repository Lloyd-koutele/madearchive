import React, { useState, useEffect } from "react";
import { updateUser as updateUserAPI } from "../services/admin/AdminService";
import '../Style/Admin/UpdateUser.css';

interface RoleField {
    name: "ADMIN" | "EDITOR" | "USER";
}

interface UserForm {
    id: string;
    nom: string;
    prenom: string;
    email: string;
    password?: string;
    telephone: string;
    roles: RoleField[];
}

interface UpdateUserProps {
    initialData?: UserForm; 
    onsuccess?: () => void;
}

function UpdateUser({ initialData, onsuccess }: UpdateUserProps) {
    const [user, setUser] = useState<UserForm>({
        id: "", nom: "", prenom: "", email: "", password: "",
        telephone: "", roles: [] 
    });

    const [error, setError] = useState<string>("");
    const [success, setSuccess] = useState<string>("");
    const [showPassword, setShowPassword] = useState<boolean>(false);

    useEffect(() => {
        if (initialData) {
            setUser({
                ...initialData,
                password: "" 
            });
        }
    }, [initialData]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setUser({ ...user, [e.target.name]: e.target.value });
    };

    const handleRoleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value as "ADMIN" | "EDITOR" | "USER";
        const checked = e.target.checked;
        
        let updatedRoles = [...user.roles];

        if (checked) {
            if (!updatedRoles.some(r => r.name === value)) {
                updatedRoles.push({ name: value });
            }
        } else {
            updatedRoles = updatedRoles.filter(r => r.name !== value);
        }

        setUser({ ...user, roles: updatedRoles });
    };

    const validateForm = (): boolean => {
        if (user.telephone.trim().length < 8) {
            setError('Le numéro de téléphone doit contenir au moins 8 caractères');
            return false;
        }
        if (user.password && user.password.length < 6) {
            setError('Le mot de passe doit contenir au moins 6 caractères');
            return false;
        }
        if (!/^\S+@\S+\.\S+$/.test(user.email.trim())) {
            setError('Email invalide');
            return false;
        }
        setError('');
        return true;
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        
        if (!validateForm()) return;

        const userId = user.id;
        if (!userId) {
            setError('ID utilisateur manquant');
            return;
        }

        // Nettoyage des données
        const userToSend: Partial<UserForm> = {
            id: user.id,
            nom: user.nom.trim(),
            prenom: user.prenom.trim(),
            email: user.email.trim().toLowerCase(),
            telephone: user.telephone.trim(),
            roles: user.roles
        };

        if (user.password && user.password.trim() !== "") {
            userToSend.password = user.password;
        }

        setError(''); 
        setSuccess('');

        try {
            await updateUserAPI(userId, userToSend);

            setSuccess('Utilisateur mis à jour avec succès');
            setUser({ id: '', nom: '', prenom: '', password: '', email: '', roles: [], telephone: '' });
            setTimeout(() => onsuccess?.(), 2000);
        } catch (err: any) {
            setError(err.response?.data?.message || err.message || "Erreur lors de la mise à jour de l'utilisateur");
        }
    };

    return (
        <div>
            {error && <div className="form-error">{error}</div>}
            {success && <div className="form-success">{success}</div>}

            <form onSubmit={handleSubmit}>
                <div className="form-grid">

                    {/* NOM */}
                    <div className="form-field">
                        <input
                            id="user-nom"
                            type="text"
                            name="nom"
                            placeholder=" "
                            className="form-field-input"
                            value={user.nom} 
                            onChange={handleChange}
                            required
                        />
                        <label htmlFor="user-nom">Nom</label>
                    </div>

                    {/* PRÉNOM */}
                    <div className="form-field">
                        <input
                            id="user-prenom"
                            type="text"
                            name="prenom"
                            placeholder=" "
                            className="form-field-input"
                            value={user.prenom} 
                            onChange={handleChange}
                            required
                        />
                        <label htmlFor="user-prenom">Prénom</label>
                    </div>

                    {/* EMAIL */}
                    <div className="form-field">
                        <input
                            id="user-email"
                            type="email"
                            name="email"
                            placeholder=" "
                            className="form-field-input"
                            value={user.email} 
                            onChange={handleChange}
                            required
                        />
                        <label htmlFor="user-email">Email</label>
                    </div>

                    {/* TÉLÉPHONE */}
                    <div className="form-field">
                        <input
                            id="user-telephone"
                            type="text"
                            name="telephone"
                            placeholder=" "
                            className="form-field-input"
                            value={user.telephone} 
                            onChange={handleChange}
                            required
                        />
                        <label htmlFor="user-telephone">Téléphone</label>
                    </div>

                    <div className="form-field form-field-password">
                        <input
                            id="user-password"
                            type={showPassword ? "text" : "password"}
                            name="password" 
                            placeholder=" Laissez vide pour ne pas modifier"
                            className="form-field-input"
                            value={user.password || ""}
                            onChange={handleChange}
                        />
                        <label htmlFor="user-password">Mot de passe (optionnel)</label>

                        <button 
                            type="button" 
                            className="password-toggle-btn"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                        >
                            {showPassword ? <i className="fa-solid fa-eye"></i> : <i className="fa-regular fa-eye-slash"></i>}
                        </button>
                    </div>

                    {/* RÔLES */}
                    <fieldset className="form-field-roles">
                        <legend className="roles-label">Rôles de l'utilisateur :</legend>
                        <div className="checkbox-group">
                            <label className="checkbox-label">
                                <input
                                    type="checkbox"
                                    value="ADMIN"
                                    checked={user.roles.some(r => r.name === "ADMIN")}
                                    onChange={handleRoleChange}
                                />
                                Administrateur
                            </label>

                            <label className="checkbox-label">
                                <input
                                    type="checkbox"
                                    value="EDITOR"
                                    checked={user.roles.some(r => r.name === "EDITOR")}
                                    onChange={handleRoleChange}
                                />
                                Éditeur
                            </label>

                            <label className="checkbox-label">
                                <input
                                    type="checkbox"
                                    value="USER"
                                    checked={user.roles.some(r => r.name === "USER")}
                                    onChange={handleRoleChange}
                                />
                                Utilisateur
                            </label>
                        </div>
                    </fieldset>
                    
                    <button type="submit" className="form-submit-btn">
                        Mettre à jour 
                    </button>
                </div>
            </form>
        </div>
    );
}

export default UpdateUser;