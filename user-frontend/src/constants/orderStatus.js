export const ORDER_STATUS = {
  PLANNED: 'PLANNED',
  VALIDATED: 'VALIDATED',
  ENRICHED: 'ENRICHED',
  PLACED: 'PLACED',
  BULKED: 'BULKED',
  CONFIRMED: 'CONFIRMED',
  CONTRACTED: 'CONTRACTED',
  BOOKED: 'BOOKED',
  ERRORED: 'ERRORED',
}

export const ORDER_SIDE = {
  BUY: 'BUY',
  SELL: 'SELL',
}

export const STATUS_GROUP = {
  PROCESSING: ['PLANNED', 'VALIDATED', 'ENRICHED', 'PLACED'],
  PENDING: ['BULKED', 'CONFIRMED', 'CONTRACTED'],
  COMPLETED: ['BOOKED'],
  FAILED: ['ERRORED'],
}

export const STATUS_LABEL = {
  PLANNED: 'Processing',
  VALIDATED: 'Processing',
  ENRICHED: 'Processing',
  PLACED: 'Processing',
  BULKED: 'Pending',
  CONFIRMED: 'Pending',
  CONTRACTED: 'Pending',
  BOOKED: 'Completed',
  ERRORED: 'Failed',
}
