@echo off
setlocal

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Administrator privileges required. Right-click deploy_iis.bat and select 'Run as administrator'.
    pause
    exit /b 1
)

echo Stopping IIS...
iisreset /stop

if not exist "C:\inetpub\wwwroot\smartmicrogrid-api" mkdir "C:\inetpub\wwwroot\smartmicrogrid-api"
if not exist "C:\inetpub\wwwroot\smartmicrogrid-api\logs" mkdir "C:\inetpub\wwwroot\smartmicrogrid-api\logs"

echo Deploying API binaries...
xcopy /E /Y /I "%~dp0WebService\SmartMicrogrid.Api\publish\*" "C:\inetpub\wwwroot\smartmicrogrid-api\"

icacls "C:\inetpub\wwwroot\smartmicrogrid-api" /grant "IIS_IUSRS":(OI)(CI)M /grant "Users":(OI)(CI)M /t /q >nul 2>&1

echo Starting IIS...
iisreset /start

echo.
echo Deployment complete.
echo API URL: http://localhost:5000/api
echo Swagger: http://localhost:5000/swagger
echo.
pause
