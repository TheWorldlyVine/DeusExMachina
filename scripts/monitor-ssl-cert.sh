#!/bin/bash
# Monitor SSL certificate status and switch when ready

CERT_NAME="god-in-a-box-ssl-cert"
PROXY_NAME="deus-ex-machina-prod-static-https-proxy"
PROJECT="deus-ex-machina-prod"

echo "Monitoring SSL certificate status for god-in-a-box.com..."
echo "This can take 10-60 minutes. You can close this and run it again later."
echo ""

while true; do
    STATUS=$(gcloud compute ssl-certificates describe $CERT_NAME --global --project=$PROJECT --format="get(managed.status)" 2>/dev/null)
    DOMAIN_STATUS=$(gcloud compute ssl-certificates describe $CERT_NAME --global --project=$PROJECT --format="get(managed.domainStatus)" 2>/dev/null)
    
    echo "[$(date)] Certificate status: $STATUS"
    echo "$DOMAIN_STATUS"
    
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
        echo "   https://www.god-in-a-box.com"
        break
    elif [ "$STATUS" = "FAILED_NOT_VISIBLE" ] || [ "$STATUS" = "FAILED_CAA_CHECKING" ]; then
        echo ""
        echo "❌ Certificate provisioning failed!"
        echo "Common issues:"
        echo "1. DNS not properly configured (A records must point to 34.95.119.251)"
        echo "2. CAA records blocking Google from issuing certificates"
        echo "3. Domain not accessible on port 80 for validation"
        break
    fi
    
    echo "Waiting 30 seconds before next check..."
    sleep 30
done