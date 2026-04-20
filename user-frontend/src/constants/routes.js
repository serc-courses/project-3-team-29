export const ROUTES = {
  HOME: '/',
  ORDERS: '/orders',
  PLACE_ORDER: '/orders/new',
  ORDER_DETAIL: '/orders/:orderId',
  FUNDS: '/funds',
  FUND_DETAIL: '/funds/:fundId',
  ACCOUNT: '/account',
  ADVISOR_HOME: '/advisor',
  ADVISOR_CLIENTS: '/advisor/clients',
  ADVISOR_CLIENT_DETAIL: '/advisor/clients/:accountId',
  ADVISOR_NEW_ORDER: '/advisor/orders/new',
  ADVISOR_ORDER_REVIEW: '/advisor/orders/review',
  ADVISOR_ACTIVITY: '/advisor/activity',
}

export const TAB_ITEMS = [
  { label: 'Home', path: ROUTES.HOME, icon: 'home' },
  { label: 'Orders', path: ROUTES.ORDERS, icon: 'orders' },
  { label: 'Funds', path: ROUTES.FUNDS, icon: 'funds' },
  { label: 'Account', path: ROUTES.ACCOUNT, icon: 'account' },
]
