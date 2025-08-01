# Shared Java Utilities

Common utilities and constants shared across all DeusExMachina backend services.

## Features

### Constants
- **ErrorCodes**: Centralized error codes for consistent error handling across services

### Utils
- **JsonUtils**: JSON serialization/deserialization utilities using Gson
- **ResponseUtils**: HTTP response handling utilities for Cloud Functions
- **ValidationUtils**: Common validation operations and sanitization

### Validation
- Email validation
- Phone number validation (E.164 format)
- UUID validation
- Alphanumeric validation
- Length and range validation
- Input sanitization for security

## Usage

Add as a dependency in your `build.gradle`:

```gradle
dependencies {
    implementation project(':apps:backend:shared')
}
```

### Example Usage

```java
// Using JsonUtils
String json = JsonUtils.toJson(myObject);
MyClass obj = JsonUtils.fromJson(jsonString, MyClass.class);

// Using ValidationUtils
if (ValidationUtils.isValidEmail(email)) {
    // Process email
}

String sanitized = ValidationUtils.sanitize(userInput);

// Using ResponseUtils in Cloud Functions
ResponseUtils.setCorsHeaders(response, "*");
ResponseUtils.sendSuccess(response, data);
ResponseUtils.sendError(response, 400, ErrorCodes.VALIDATION_INVALID_FORMAT, "Invalid email format");
```

## Testing

Run tests:
```bash
./gradlew :apps:backend:shared:test
```

## Building

Build the library:
```bash
./gradlew :apps:backend:shared:build
```

This will create:
- JAR file with compiled classes
- Sources JAR for debugging
- Javadoc JAR for documentation