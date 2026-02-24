import axios from "axios";
import keycloak from "./auth/keycloak";

const isProduction = import.meta.env.PROD;
const toolRentBackendServer = import.meta.env.VITE_TOOLRENT_BACKEND_SERVER;
const toolRentBackendPort = import.meta.env.VITE_TOOLRENT_BACKEND_PORT;

console.log('Modo de producción:', isProduction);
console.log('Backend Server:', toolRentBackendServer);
console.log('Backend Port:', toolRentBackendPort);

// En producción, usar variables de entorno configuradas en Docker
// En desarrollo, usar el proxy de Vite (string vacío)
const baseURL = isProduction
    ? `http://${toolRentBackendServer}:${toolRentBackendPort}`
    : ''; // Proxy de Vite maneja las rutas /api

console.log('URL base configurada:', baseURL || 'PROXY VITE');

const api = axios.create({
    baseURL,
    headers: {
        'Content-Type': 'application/json'
    }
});

api.interceptors.request.use(async (config) => {
    console.log('=== DEBUG AUTH ===');
    console.log('Keycloak authenticated:', keycloak.authenticated);

    if (keycloak.authenticated) {
        await keycloak.updateToken(30);
        config.headers.Authorization = `Bearer ${keycloak.token}`;

        if (keycloak.tokenParsed) {
            console.log('Usuario:', keycloak.tokenParsed.preferred_username);
            console.log('Roles del cliente:', keycloak.tokenParsed.resource_access);
            console.log('Roles del realm:', keycloak.tokenParsed.realm_access);
        }

        console.log('Authorization header agregado');
    } else {
        console.log('Usuario NO autenticado');
    }

    console.log('Request URL:', config.url);
    console.log('==================');

    return config;
}, (error) => {
    console.error('Error en interceptor:', error);
    return Promise.reject(error);
});

export default api;