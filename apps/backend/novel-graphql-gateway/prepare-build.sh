#!/bin/bash
# Prepare the build context for Cloud Build deployment

set -e

echo "Preparing build context for GraphQL gateway deployment..."

# Get the workspace root (3 levels up from this script)
WORKSPACE_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
GATEWAY_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Workspace root: $WORKSPACE_ROOT"
echo "Gateway directory: $GATEWAY_DIR"

# Copy workspace files needed for pnpm
echo "Copying workspace files..."
cp "$WORKSPACE_ROOT/pnpm-workspace.yaml" "$GATEWAY_DIR/" || {
    echo "ERROR: Could not find pnpm-workspace.yaml in workspace root"
    exit 1
}

cp "$WORKSPACE_ROOT/pnpm-lock.yaml" "$GATEWAY_DIR/" || {
    echo "ERROR: Could not find pnpm-lock.yaml in workspace root"
    exit 1
}

echo "Build context prepared successfully!"
echo "Files in gateway directory:"
ls -la "$GATEWAY_DIR/"