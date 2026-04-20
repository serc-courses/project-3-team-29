import { api } from './client';

export const getBulkOrderViews = (filters = {}) => {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.append(key, value);
  });
  const query = params.toString();
  return api.get(`/view/bulk-orders${query ? `?${query}` : ''}`);
};
