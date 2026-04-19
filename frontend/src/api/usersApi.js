import { api } from './client';

export const getUsers = () => api.get('/view/users');
