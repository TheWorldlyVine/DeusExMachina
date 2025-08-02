# JSON Schema Comparison

## Actual JSON being sent (from logs):
```json
{
  "messageId": "45c00a10-ba8f-49f2-b2aa-464d3b82bf08",
  "timestamp": "2025-08-02T18:47:43.978Z",
  "emailType": "VERIFICATION_EMAIL",
  "recipient": {
    "email": "mrburnsa+jkl@icloud.com",
    "displayName": "mrburnsa+jkl@icloud.com"
  },
  "sender": null,
  "templateData": {
    "expiryTime": "2025-08-03T18:47:43.977709736Z",
    "token": "F34H6gRG-NDotjfhrHx7RqBa0bRXL0S7U_k0Kh0V0aI",
    "actionUrl": "https://app.deusexmachina.com/verify?token=F34H6gRG-NDotjfhrHx7RqBa0bRXL0S7U_k0Kh0V0aI"
  },
  "metadata": {
    "userId": null,
    "correlationId": "5cd82199-da52-45d8-83c7-42c37ed53fab",
    "retryCount": 0,
    "source": "auth-function",
    "priority": "high"
  },
  "attachments": null
}
```

## AVRO Schema Requirements:

1. **messageId**: string ✅
2. **timestamp**: string (ISO 8601) ✅ 
3. **emailType**: string ✅
4. **recipient**: record with:
   - email: string ✅
   - displayName: nullable string ✅
5. **sender**: nullable record ✅ (sending null)
6. **templateData**: map of strings ✅
7. **metadata**: record with:
   - userId: nullable string ✅ (sending null)
   - correlationId: string ✅
   - retryCount: int ✅ (sending 0)
   - source: string ✅
   - priority: string ✅ (lowercase "high")
8. **attachments**: nullable array ✅ (sending null)

## Potential Issues:

1. **Field ordering?** - AVRO might be strict about field order
2. **Null vs missing?** - Some fields might need to be omitted rather than null
3. **Timestamp format?** - The format includes milliseconds `.978Z` - maybe it expects a different precision?