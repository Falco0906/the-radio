#!/bin/bash

# Build script for The Radio
set -e

echo "🎵 Building The Radio..."

# Build frontend
echo "📦 Building frontend..."
cd frontend
npm install
npm run build
cd ..

# Build backend (with frontend included)
echo "☕ Building backend..."
cd backend
mvn clean package -DskipTests
cd ..

echo "✅ Build complete!"
echo ""
echo "Backend JAR: backend/target/the-radio-1.0.0.jar"
echo "Frontend build: frontend/dist/"
echo ""
echo "To run:"
echo "  Backend: java -jar backend/target/the-radio-1.0.0.jar"
echo "  Or use: docker-compose up"

