import { api } from './client';

export const confirmOrders = () => api.post('/orders/confirm', {});

export const bookOrders = () => api.post('/orders/book', {});

export const replayProjections = () => api.post('/view/replay', {});
