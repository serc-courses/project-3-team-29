import { api } from './client';

export const getSla = () => api.get('/view/sla');
