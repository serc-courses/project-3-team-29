export const ROUTES = {
  DASHBOARD: '/',
  ORDERS: '/orders',
  NEW_ORDER: '/orders/new',
  BULK_ORDERS: '/bulk-orders',
  FUNDS: '/funds',
  ACCOUNTS: '/accounts',
  USERS: '/users',
};

export const NAV_ITEMS = [
  { label: 'Dashboard', path: ROUTES.DASHBOARD, icon: 'dashboard' },
  { label: 'Orders', path: ROUTES.ORDERS, icon: 'orders' },
  { label: 'Bulk Orders', path: ROUTES.BULK_ORDERS, icon: 'bulk' },
  { label: 'Funds', path: ROUTES.FUNDS, icon: 'funds' },
  { label: 'Accounts', path: ROUTES.ACCOUNTS, icon: 'accounts' },
  { label: 'Users', path: ROUTES.USERS, icon: 'users' },
];
