@echo off
setlocal enabledelayedexpansion

set IDENTITY=127.0.0.1 identity.sep.local
set REGISTRY=127.0.0.1 registry.sep.local
set API=127.0.0.1 api.sep.local

set HOSTS_FILE=%SystemRoot%\System32\drivers\etc\hosts

net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo Vui lòng chạy script này bằng quyền Administrator.
    pause
    exit /b
)

findstr /C:"%IDENTITY%" "%HOSTS_FILE%" >nul
if %errorLevel% NEQ 0 (
    echo %IDENTITY% >> "%HOSTS_FILE%"
    echo Đã thêm: %IDENTITY%
) else (
    echo Đã tồn tại: %IDENTITY%
)

findstr /C:"%REGISTRY%" "%HOSTS_FILE%" >nul
if %errorLevel% NEQ 0 (
    echo %REGISTRY% >> "%HOSTS_FILE%"
    echo Đã thêm: %REGISTRY%
) else (
    echo Đã tồn tại: %REGISTRY%
)

findstr /C:"%API%" "%HOSTS_FILE%" >nul
if %errorLevel% NEQ 0 (
    echo %API% >> "%HOSTS_FILE%"
    echo Đã thêm: %API%
) else (
    echo Đã tồn tại: %API%
)

echo Hoàn tất!
pause
