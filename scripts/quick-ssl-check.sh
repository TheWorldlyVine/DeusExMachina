#!/bin/bash
# Quick SSL certificate check and update script

CERT_NAME="god-in-a-box-main-cert"
PROXY_NAME="deus-ex-machina-prod-static-https-proxy"
PROJECT="deus-ex-machina-prod"

echo "Checking SSL certificate status..."
STATUS=$(gcloud compute ssl-certificates describe $CERT_NAME --global --project=$PROJECT --format="get(managed.status)" 2>/dev/null)
DOMAIN_STATUS=$(gcloud compute ssl-certificates describe $CERT_NAME --global --project=$PROJECT --format="get(managed.domainStatus)" 2>/dev/null)

echo "Certificate status: $STATUS"
echo "Domain status: $DOMAIN_STATUS"

if [ "$STATUS" = "ACTIVE" ]; then
    echo ""
    echo "✅ Certificate is ACTIVE! Updating HTTPS proxy..."
    
    gcloud compute target-https-proxies update $PROXY_NAME \
        --ssl-certificates=$CERT_NAME \
        --global \
        --project=$PROJECT
        
    echo ""
    echo "🎉 Success! Your site is now available at:"
    echo "   https://god-in-a-box.com"
else
    echo ""
    echo "⏳ Certificate is still provisioning. This usually takes 10-20 minutes."
    echo "   Run this script again in a few minutes to check status."
fi