export const CONFIG = {
  APP_NAME: 'MF-OMS Invest',
  SSE_ENDPOINT: '/view/stream',
  ACCOUNTS_SEED: Array.from({ length: 10 }, (_, i) => `ACCT${String(i + 1).padStart(5, '0')}`),
}
