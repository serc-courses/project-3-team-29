export const ROUTES = {
  DASHBOARD: '/',
  ORDERS: '/orders',
  NEW_ORDER: '/orders/new',
  BULK_ORDERS: '/bulk-orders',
  FUNDS: '/funds',
  ACCOUNTS: '/accounts',
  USERS: '/users',
  RECONCILIATION: '/reconciliation',
  PORTFOLIO: '/portfolio',
  AGGREGATE_FUNDS: '/aggregate-funds',
};

export const NAV_ITEMS = [
  { label: 'Dashboard', path: ROUTES.DASHBOARD, icon: 'dashboard' },
  { label: 'Orders', path: ROUTES.ORDERS, icon: 'orders' },
  { label: 'Bulk Orders', path: ROUTES.BULK_ORDERS, icon: 'bulk' },
  { label: 'Funds', path: ROUTES.FUNDS, icon: 'funds' },
  { label: 'Aggregate Accounts', path: ROUTES.ACCOUNTS, icon: 'accounts' },
  { label: 'Users', path: ROUTES.USERS, icon: 'users' },
  { label: 'Aggregate Funds', path: ROUTES.AGGREGATE_FUNDS, icon: 'funds' },
  { label: 'Reconciliation', path: ROUTES.RECONCILIATION, icon: 'reconciliation' },
  { label: 'Portfolio P/L', path: ROUTES.PORTFOLIO, icon: 'portfolio' },
];
