import { api } from './client';

export const getFunds =()=>api.get('/funds');