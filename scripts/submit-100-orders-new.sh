#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

API_BASE_URL="${OMS_API_BASE_URL:-http://localhost:8080}"

if ! command -v curl &> /dev/null; then
  echo "Error: curl not found in PATH."
  exit 1
fi

# Define 10 accountIDs and 5 fundIDs
ACCOUNT_IDS=(
  "ACCT00001" "ACCT00002" "ACCT00003" "ACCT00004" "ACCT00005"
  "ACCT00006" "ACCT00007" "ACCT00008" "ACCT00009" "ACCT00010"
)

FUND_IDS=(
  "FND001" "FND002" "FND003" "FND004" "FND005"
)

tmp_payload="$(mktemp)"
tmp_response="$(mktemp)"
trap 'rm -f "$tmp_payload" "$tmp_response"' EXIT

printf '[\n' > "$tmp_payload"
order_index=0
buy_count=0
sell_count=0

for i in {1..100}; do
  # Cycle through accounts (10 different accounts)
  account_idx=$(( (i - 1) % 10 ))
  account_id="${ACCOUNT_IDS[$account_idx]}"
  
  # Cycle through funds (5 different funds)
  fund_idx=$(( (i - 1) % 5 ))
  fund_id="${FUND_IDS[$fund_idx]}"
  
  # Alternate between BUY and SELL
  if (( i % 2 == 1 )); then
    order_side="BUY"
    (( buy_count++ )) || true
  else
    order_side="SELL"
    (( sell_count++ )) || true
  fi
  
  # Generate amount based on order index
  amount=$(( 1000 + (i - 1) * 10 ))
  
  # Add comma before all orders except the first
  if [[ $order_index -gt 0 ]]; then
    printf ',\n' >> "$tmp_payload"
  fi
  
  # Append order without orderID and quantity
  printf '  {"productID":"%s","amount":%s,"accountID":"%s","orderSide":"%s"}' \
    "$fund_id" "$amount" "$account_id" "$order_side" >> "$tmp_payload"
  
  (( order_index++ )) || true
done

printf '\n]\n' >> "$tmp_payload"

total_orders=$order_index
if [[ "$total_orders" -ne 100 ]]; then
  echo "Error: Generated invalid order count. total=$total_orders (expected 100)"
  exit 1
fi

if [[ "$((buy_count + sell_count))" -ne 100 ]]; then
  echo "Error: Buy + Sell count mismatch. buy=$buy_count sell=$sell_count total=$((buy_count + sell_count))"
  exit 1
fi

echo "Submitting $total_orders orders to $API_BASE_URL/orders/plan"
echo "BUY=$buy_count SELL=$sell_count"
echo "Accounts: ${#ACCOUNT_IDS[@]} different accounts"
echo "Funds: ${#FUND_IDS[@]} different funds"
echo ""

http_status="$(curl -sS -o "$tmp_response" -w "%{http_code}" \
  -X POST "$API_BASE_URL/orders/plan" \
  -H "Content-Type: application/json" \
  --data-binary "@$tmp_payload")"

if [[ "$http_status" != "201" ]]; then
  echo "Error: /orders/plan returned HTTP $http_status"
  cat "$tmp_response"
  exit 1
fi

echo "Success: Submitted $total_orders orders ($buy_count BUY, $sell_count SELL)"
echo "Using 10 accounts across 5 funds"
cat "$tmp_response"
