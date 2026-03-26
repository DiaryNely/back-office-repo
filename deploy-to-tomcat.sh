#!/bin/bash

echo "================================================"
echo "Compiling and Deploying Project to Tomcat"
echo "================================================"

# Set project variables
PROJECT_NAME="back-office"
TOMCAT_HOME="/opt/tomcat"  # À adapter selon votre installation
WEBAPPS_DIR="$TOMCAT_HOME/webapps"

# Verify Tomcat directory exists
if [ ! -d "$TOMCAT_HOME" ]; then
    echo "Error: TOMCAT_HOME '$TOMCAT_HOME' does not exist!"
    echo "Please update TOMCAT_HOME in this script."
    exit 1
fi

# Build framework first
echo "Building framework..."
cd framework
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "Framework build failed!"
    cd ..
    exit 1
fi
cd ..

# Navigate to backend directory
cd backend

# Clean and build with Maven
echo "Cleaning and building backend..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed!"
    cd ..
    exit 1
fi

# Check if WAR file exists
if [ ! -f target/*.war ]; then
    echo "WAR file not found in target directory!"
    cd ..
    exit 1
fi

# Stop Tomcat
echo "Stopping Tomcat..."
"$TOMCAT_HOME/bin/shutdown.sh"
sleep 5

# Remove old deployment
echo "Removing old deployment..."
if [ -d "$WEBAPPS_DIR/$PROJECT_NAME" ]; then
    rm -rf "$WEBAPPS_DIR/$PROJECT_NAME"
fi
if [ -f "$WEBAPPS_DIR/$PROJECT_NAME.war" ]; then
    rm -f "$WEBAPPS_DIR/$PROJECT_NAME.war"
fi

# Deploy WAR to Tomcat
echo "Deploying to Tomcat..."
cp target/*.war "$WEBAPPS_DIR/$PROJECT_NAME.war"

# Return to project root
cd ..

# Start Tomcat
echo "Starting Tomcat..."
"$TOMCAT_HOME/bin/startup.sh"

echo "================================================"
echo "Deployment completed successfully!"
echo "Access your app at: http://localhost:8080/$PROJECT_NAME"
echo "================================================"
