@echo off
title BML License Keygen
cd /d "%~dp0"

if not exist "target\bml-license-keygen.jar" (
    echo.
    echo  [INFO] bml-license-keygen.jar not found, building...
    echo.
    call mvn clean package -DskipTests -q
    if errorlevel 1 (
        echo.
        echo  [ERROR] Build failed. Please check JDK 21+ and Maven 3.8.9+
        echo.
        pause
        exit /b 1
    )
    echo  [OK] Build succeeded.
    echo.
)

chcp 65001 >nul 2>&1
java -jar target\bml-license-keygen.jar %*

pause
