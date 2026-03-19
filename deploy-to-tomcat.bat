@echo off
echo ================================================
echo Compiling and Deploying Project to Tomcat
echo ================================================

REM Set project variables
set PROJECT_NAME=back-office
set TOMCAT_HOME=C:\apache-tomcat-10.1.50\apache-tomcat-10.1.50
set WEBAPPS_DIR=%TOMCAT_HOME%\webapps

REM Build framework first
echo Building framework...
cd framework
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Framework build failed!
    cd ..
    pause
    exit /b 1
)
cd ..

REM Navigate to backend directory
cd backend

REM Clean and build with Maven
echo Cleaning and building backend...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    cd ..
    pause
    exit /b 1
)


REM Check if WAR file exists
if not exist target\*.war (
    echo WAR file not found in target directory!
    cd ..
    pause
    exit /b 1
)

REM Stop Tomcat
echo Stopping Tomcat...
call %TOMCAT_HOME%\bin\shutdown.bat
timeout /t 5

REM Remove old deployment
echo Removing old deployment...
if exist %WEBAPPS_DIR%\%PROJECT_NAME% rmdir /s /q %WEBAPPS_DIR%\%PROJECT_NAME%
if exist %WEBAPPS_DIR%\%PROJECT_NAME%.war del /q %WEBAPPS_DIR%\%PROJECT_NAME%.war

REM Deploy WAR to Tomcat
echo Deploying to Tomcat...
for %%f in (target\*.war) do (
    copy /y "%%f" %WEBAPPS_DIR%\%PROJECT_NAME%.war
)

REM Return to project root
cd ..

REM Start Tomcat
echo Starting Tomcat...
call %TOMCAT_HOME%\bin\startup.bat

echo ================================================
echo Deployment completed successfully!
echo Access your app at: http://localhost:8080/%PROJECT_NAME%
echo ================================================
pause