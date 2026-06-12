import api from "../api";

export interface MembreDto {
    id: string;
    nom: string;
    prenom: string;
    email: string;
}

export const getMembres = async (documentId: string): Promise<MembreDto[]> => {
    try {
        const response = await api.get(`/user/documents/${documentId}/groupe/membres`);
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message);
    }
};

export const getDisponibles = async (documentId: string): Promise<MembreDto[]> => {
    try {
        const response = await api.get(`/user/documents/${documentId}/groupe/disponibles`);
        return response.data;
    } catch (error: any) {
        throw new Error(error.response?.data || error.message);
    }
};

export const ajouterMembre = async (documentId: string, nouveauMembreId: string): Promise<void> => {
    try {
        await api.post(`/user/documents/${documentId}/groupe/membres`, null, {
            params: { nouveauMembreId }
        });
    } catch (error: any) {
        throw new Error(error.response?.data || error.message);
    }
};

export const retirerMembre = async (documentId: string, membreId: string): Promise<void> => {
    try {
        await api.delete(`/user/documents/${documentId}/groupe/membres/${membreId}`);
    } catch (error: any) {
        throw new Error(error.response?.data || error.message);
    }
};