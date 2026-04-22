import { api } from './client';

export const getAccountAggregates = () => api.get('/view/aggregates/accounts');

export const getFundAggregates = () => api.get('/view/aggregates/funds');


