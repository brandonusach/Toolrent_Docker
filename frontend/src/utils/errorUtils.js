/**
 * Convierte un error de Axios/HTTP en un mensaje legible para el usuario.
 * Nunca expone mensajes técnicos como "Request failed with status code 400".
 */
export const getFriendlyError = (error) => {
    // Sin respuesta del servidor (red caída, timeout, CORS)
    if (!error.response) {
        return 'No se pudo conectar con el servidor. Verifica tu conexión e inténtalo de nuevo.';
    }

    const status = error.response.status;
    const data = error.response.data;

    // Si el backend envía fieldErrors, buscar el mensaje más relevante
    if (data && data.fieldErrors) {
        const messages = Object.values(data.fieldErrors);
        if (messages.length > 0) {
            return friendlyFieldMessage(messages[0]);
        }
    }

    // Si el backend envía un mensaje string legible, hacerlo amigable
    if (typeof data === 'string' && data.length > 0 && data.length < 300) {
        return friendlyFieldMessage(data);
    }

    // Si el backend envía { message: "..." }
    if (data && typeof data.message === 'string') {
        return friendlyFieldMessage(data.message);
    }

    // Mensajes amigables por código de estado
    switch (status) {
        case 400:
            return 'Los datos ingresados no son válidos. Revisa los campos e inténtalo de nuevo.';
        case 401:
            return 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
        case 403:
            return 'No tienes permisos para realizar esta acción.';
        case 404:
            return 'El recurso solicitado no fue encontrado.';
        case 409:
            return 'Ya existe un registro con esos datos. Verifica la información ingresada.';
        case 422:
            return 'Los datos ingresados no cumplen con el formato requerido.';
        case 500:
            return 'Ocurrió un error interno en el servidor. Inténtalo más tarde.';
        default:
            return 'Ocurrió un error inesperado. Por favor, inténtalo de nuevo.';
    }
};

/**
 * Convierte mensajes técnicos del backend en mensajes claros para el usuario.
 */
const friendlyFieldMessage = (msg) => {
    if (!msg) return 'Ocurrió un error inesperado.';

    // RUT inválido
    if (msg.includes('RUT chileno inválido') || msg.includes('RUT inválido')) {
        return 'El RUT ingresado no es válido. Verifica el formato y el dígito verificador (ej: 12.345.678-9).';
    }
    // RUT ya existe
    if (msg.includes('Ya existe un cliente con RUT') || msg.includes('Ya existe otro cliente con RUT')) {
        return 'Este RUT ya está registrado en el sistema. No se puede duplicar.';
    }
    // Teléfono inválido
    if (msg.includes('teléfono chileno inválido') || msg.includes('telefono')) {
        return 'El número de teléfono no es válido. Ingresa un celular (ej: 9 1234 5678) o teléfono fijo chileno.';
    }
    // Teléfono ya existe
    if (msg.includes('Ya existe un cliente con el teléfono') || msg.includes('Ya existe otro cliente con el teléfono')) {
        return 'Este número de teléfono ya está registrado para otro cliente.';
    }
    // Email inválido
    if (msg.includes('Formato de email inválido') || msg.includes('email inválido')) {
        return 'El formato del correo electrónico no es válido (ej: nombre@dominio.com).';
    }
    // Email ya existe
    if (msg.includes('Ya existe un cliente con el email') || msg.includes('Ya existe otro cliente con el email')) {
        return 'Este correo electrónico ya está registrado para otro cliente.';
    }
    // Nombre requerido
    if (msg.includes('nombre') && msg.includes('requerido')) {
        return 'El nombre del cliente es obligatorio.';
    }
    // Nombre muy corto
    if (msg.includes('nombre') && msg.includes('2 caracteres')) {
        return 'El nombre ingresado es demasiado corto. Ingresa el nombre completo del cliente.';
    }
    // Mensajes del backend ya legibles (menos de 200 chars)
    if (msg.length < 200) return msg;

    return 'Ocurrió un error al procesar la solicitud.';
};

/**
 * Extrae fieldErrors del backend si existen,
 * o devuelve null si no hay errores por campo.
 * Los mensajes de cada campo también se hacen amigables.
 */
export const getFieldErrors = (error) => {
    if (error.response?.data?.fieldErrors) {
        const raw = error.response.data.fieldErrors;
        // Hacer amigable cada mensaje de campo
        const friendly = {};
        for (const [field, msg] of Object.entries(raw)) {
            friendly[field] = friendlyFieldMessage(msg);
        }
        return friendly;
    }
    return null;
};
