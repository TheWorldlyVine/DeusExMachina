#!/usr/bin/env python3
import json
import avro.schema
import avro.io
import io

# AVRO schema from Pub/Sub
schema_str = '''
{
  "type": "record",
  "name": "EmailMessage",
  "fields": [
    {"name": "messageId", "type": "string", "doc": "Unique identifier for the message"},
    {"name": "timestamp", "type": "string", "doc": "ISO 8601 timestamp"},
    {"name": "emailType", "type": "string", "doc": "Type of email (e.g., VERIFICATION_EMAIL, ORDER_CONFIRMATION)"},
    {
      "name": "recipient",
      "type": {
        "type": "record",
        "name": "Recipient",
        "fields": [
          {"name": "email", "type": "string"},
          {"name": "displayName", "type": ["null", "string"], "default": null}
        ]
      }
    },
    {
      "name": "sender",
      "type": ["null", {
        "type": "record",
        "name": "Sender",
        "fields": [
          {"name": "email", "type": "string"},
          {"name": "name", "type": ["null", "string"], "default": null}
        ]
      }],
      "default": null,
      "doc": "Optional custom sender, defaults to system sender"
    },
    {
      "name": "templateData",
      "type": {"type": "map", "values": "string"},
      "doc": "Template variables for email rendering"
    },
    {
      "name": "metadata",
      "type": {
        "type": "record",
        "name": "Metadata",
        "fields": [
          {"name": "userId", "type": ["null", "string"], "default": null},
          {"name": "correlationId", "type": "string"},
          {"name": "retryCount", "type": "int", "default": 0},
          {"name": "source", "type": "string", "doc": "Source service/function"},
          {"name": "priority", "type": "string", "default": "normal", "doc": "Email priority: high, normal, low"}
        ]
      }
    },
    {
      "name": "attachments",
      "type": ["null", {
        "type": "array",
        "items": {
          "type": "record",
          "name": "Attachment",
          "fields": [
            {"name": "filename", "type": "string"},
            {"name": "contentType", "type": "string"},
            {"name": "data", "type": "string", "doc": "Base64 encoded data"}
          ]
        }
      }],
      "default": null,
      "doc": "Optional file attachments"
    }
  ]
}
'''

# The JSON from our logs
test_json = {
  "messageId": "45c00a10-ba8f-49f2-b2aa-464d3b82bf08",
  "timestamp": "2025-08-02T18:47:43.978Z",
  "emailType": "VERIFICATION_EMAIL",
  "recipient": {
    "email": "mrburnsa+jkl@icloud.com",
    "displayName": "mrburnsa+jkl@icloud.com"
  },
  "sender": None,
  "templateData": {
    "expiryTime": "2025-08-03T18:47:43.977709736Z",
    "token": "F34H6gRG-NDotjfhrHx7RqBa0bRXL0S7U_k0Kh0V0aI",
    "actionUrl": "https://app.deusexmachina.com/verify?token=F34H6gRG-NDotjfhrHx7RqBa0bRXL0S7U_k0Kh0V0aI"
  },
  "metadata": {
    "userId": None,
    "correlationId": "5cd82199-da52-45d8-83c7-42c37ed53fab",
    "retryCount": 0,
    "source": "auth-function",
    "priority": "high"
  },
  "attachments": None
}

try:
    # Parse the AVRO schema
    schema = avro.schema.parse(schema_str)
    print("Schema parsed successfully")
    
    # For JSON encoding, we need to validate differently
    # Let's manually check each field
    print("\nValidating JSON structure...")
    
    # Try different variations to find what works
    variations = [
        ("Original JSON", test_json),
        ("Without null fields", {k: v for k, v in test_json.items() if v is not None}),
        ("With empty sender object", {**test_json, "sender": {}}),
        ("With attachments as empty array", {**test_json, "attachments": []}),
    ]
    
    for name, data in variations:
        print(f"\n{name}:")
        print(json.dumps(data, indent=2))
        
        # Try to validate
        try:
            # Convert to bytes for AVRO
            writer = avro.io.DatumWriter(schema)
            bytes_writer = io.BytesIO()
            encoder = avro.io.BinaryEncoder(bytes_writer)
            writer.write(data, encoder)
            print("✅ Valid for AVRO binary encoding")
        except Exception as e:
            print(f"❌ Invalid: {str(e)}")

except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()