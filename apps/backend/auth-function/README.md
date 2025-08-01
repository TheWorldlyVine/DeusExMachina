# Auth Function

Google Cloud Function for handling user authentication in the DeusExMachina platform.

## Features

- User login with JWT token generation
- Token verification
- Token refresh functionality
- CORS support for web applications

## Endpoints

### POST /auth/login
Authenticate user and receive JWT tokens.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "token": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresIn": 3600
}
```

### GET/POST /auth/verify
Verify a JWT token.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "valid": true,
  "username": "string",
  "expiresAt": 1234567890000
}
```

### POST /auth/refresh
Get a new JWT token using a refresh token.

**Request Body:**
```json
{
  "refreshToken": "refresh-token"
}
```

**Response:**
```json
{
  "token": "new-jwt-token",
  "expiresIn": 3600
}
```

## Local Development

Run the function locally:
```bash
./gradlew :apps:backend:auth-function:runFunction
```

The function will be available at http://localhost:8080

## Testing

Run tests:
```bash
./gradlew :apps:backend:auth-function:test
```

## Environment Variables

- `JWT_SECRET`: Secret key for signing JWT tokens (required in production)

## Deployment

This function is deployed automatically via GitHub Actions when changes are pushed to the main branch.