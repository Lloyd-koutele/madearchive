import axios from 'axios';

// URL de base de ton serveur Spring Boot
const BASE_URL = 'http://localhost:8080/api';

// Création d'une instance Axios
const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Intercepteur de requête : ajoute le token Bearer
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Intercepteur de réponse : gère les erreurs 401 (token expiré)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const url = error.config?.url || '';
        const isAuthRoute = url.includes('/login') || url.includes('/logout');

        if (error.response?.status === 401 && !isAuthRoute) {
            localStorage.clear();
            window.location.replace('/login');
        }
        return Promise.reject(error);
    }
);

export default api;
