#!/usr/bin/env bash
set -euo pipefail

KEYSTORE="src/main/resources/keystore.jks"
ALIAS="oms"
STOREPASS="${OMS_KEYSTORE_PASS:-changeit}"

mkdir -p src/main/resources

if [ -f "$KEYSTORE" ]; then
  echo "Keystore already exists at $KEYSTORE — skipping generation."
  exit 0
fi

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -keystore "$KEYSTORE" \
  -storepass "$STOREPASS" \
  -keypass "$STOREPASS" \
  -dname "CN=localhost, OU=OMS, O=IIIT, L=Hyderabad, ST=Telangana, C=IN" \
  -noprompt

echo "Keystore generated at $KEYSTORE"
echo "Cert valid for 365 days. Renew before expiry with: rm $KEYSTORE && ./scripts/gen-keystore.sh"
