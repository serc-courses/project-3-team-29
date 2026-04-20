import { api } from './client';

export const planOrders = (orders) => api.post('/orders/plan', orders);

export const listOrders = () => api.get('/orders');

export const getOrderStatus = (orderID) => api.get(`/orders/status?orderID=${encodeURIComponent(orderID)}`);

export const getOrderViews = (filters = {}) => {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.append(key, value);
  });
  const query = params.toString();
  return api.get(`/view/orders${query ? `?${query}` : ''}`);
};
