import api from "../api"

export const createUser = async (userData) =>{
    try{
        const response = await api.post('/admin/users/create-user', userData);
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const updateUser = async(id, userData) => {
    try{
        if (!id) {
            throw new Error('ID utilisateur manquant');
        }
        const response = await api.put(`/admin/users/update-user/${id}`, userData);
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const updateUserStatus = async(id, userData) => {
    try{
        if (!id) {
            throw new Error('ID utilisateur manquant');
        }
        const response = await api.put(`/admin/users/status/${id}`, userData);
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const getAllUsers = async() =>{
    try{
        const response = await api.get('/admin/users');
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const getActiveUsers = async() =>{
    try{
        const response = await api.get('/admin/users/actifs');
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const getInActiveUsers = async() =>{
    try{
        const response = await api.get('/admin/users/inactifs');
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}

export const getUser = async(id, userData) => {
    try{
        if (!id) {
            throw new Error('ID utilisateur manquant');
        }
        const response = await api.get(`/admin/users/${id}`, userData);
        return response.data;
    }catch(error){
        console.error('Détails de l\'erreur:', error.response?.data || error);
        throw error.response?.data?.message
                ? new Error(error.response.data.message)
                : error;
    }
}