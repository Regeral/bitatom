@echo off
rem Build C crypto-core into crypto_core.dll (Windows x64, MinGW-w64 gcc)
rem and copy the DLL to the desktop app resources, where JNA finds it.
cd /d "%~dp0"

where gcc >nul 2>nul || (echo [ERROR] gcc not found. Install MinGW-w64 and add its bin folder to PATH. & exit /b 1)

if not exist build mkdir build
echo === Compiling crypto_core.dll ===
gcc -O2 -Wall -shared -o build\crypto_core.dll crypto_api.c encrypt_aes.c auth_gen.c password_hash.c monocypher.c monocypher-ed25519.c -ladvapi32 -static-libgcc
if errorlevel 1 (echo [ERROR] DLL build failed & exit /b 1)

echo === Building console test crypto_test.exe ===
gcc -O2 -o build\crypto_test.exe main.c encrypt_aes.c auth_gen.c password_hash.c monocypher.c monocypher-ed25519.c -ladvapi32
if errorlevel 1 (echo [ERROR] test build failed & exit /b 1)
build\crypto_test.exe

set DEST=..\app\desktopApp\src\main\resources\win32-x86-64
if not exist "%DEST%" mkdir "%DEST%"
copy /Y build\crypto_core.dll "%DEST%\crypto_core.dll" >nul
echo === Done: DLL copied to %DEST% ===
