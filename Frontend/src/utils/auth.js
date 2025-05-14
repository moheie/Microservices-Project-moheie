// File: src/utils/auth.js
export const getAuthHeaders = () => {
    const token = sessionStorage.getItem('token');
    if (!token) {
        throw new Error('Authorization token is missing.');
    }
    return {
        Authorization: `Bearer ${token}`,
    };
};