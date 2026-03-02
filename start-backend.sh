#!/bin/bash

# Load environment variables from .env file
export $(cat /Users/macbookair/Downloads/radio/backend/.env | grep -v '^#' | xargs)

# Start the backend
cd /Users/macbookair/Downloads/radio/backend
mvn spring-boot:run
