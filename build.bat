@echo off
rem One command to build and test authlas-java on Windows.
rem It builds the authlas Rust core and the JNI bridge, bundles the native libraries into the jar, and runs the tests.
rem Usage:  build.bat            build and test
rem         build.bat package    build the jar only (no tests)
rem Needs: JDK 17+, Maven, Rust (cargo), Visual Studio Build Tools (C++), and OpenSSL 3 under OPENSSL_DIR.
setlocal
if not defined OPENSSL_DIR set "OPENSSL_DIR=%USERPROFILE%\vcpkg\installed\x64-windows"
if not defined AUTHLAS_RUST_DIR set "AUTHLAS_RUST_DIR=%~dp0..\authlas-rust"
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
set "VCVARS=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
set "PATH=%JAVA_HOME%\bin;%USERPROFILE%\.cargo\bin;%PATH%"
cd /d "%~dp0"

if not exist "%OPENSSL_DIR%\include\openssl\err.h" (
  echo OpenSSL 3 not found at "%OPENSSL_DIR%". Set OPENSSL_DIR to the folder holding include\ and lib\.
  exit /b 1
)
if not exist "%AUTHLAS_RUST_DIR%\Cargo.toml" (
  echo authlas-rust not found at "%AUTHLAS_RUST_DIR%". Clone it there or set AUTHLAS_RUST_DIR.
  exit /b 1
)
call "%VCVARS%" >nul || (echo MSVC not found. Install Visual Studio Build Tools with the C++ workload. & exit /b 1)

rem 1. Rust core (core, C ABI and C transport in one DLL).
set "OPENSSL_DIR=%OPENSSL_DIR%"
pushd "%AUTHLAS_RUST_DIR%"
cargo build --release --no-default-features --features backend-openssl,capi,net
if errorlevel 1 (popd & exit /b 1)
popd

rem 2. JNI bridge against the core's C ABI header and import library.
set "JNI_OUT=%~dp0build"
if not exist "%JNI_OUT%" mkdir "%JNI_OUT%"
cl /nologo /LD /W3 /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" /I"%AUTHLAS_RUST_DIR%\include" native\authlas_jni.c /Fe:"%JNI_OUT%\authlas_jni.dll" /Fo:"%JNI_OUT%\authlas_jni.obj" /link "%AUTHLAS_RUST_DIR%\target\release\authlas.dll.lib"
if errorlevel 1 exit /b 1

rem 3. Bundle the native libraries into the jar.
set "NATIVE=%~dp0src\main\resources\natives\windows-x64"
if not exist "%NATIVE%" mkdir "%NATIVE%"
copy /y "%AUTHLAS_RUST_DIR%\target\release\authlas.dll" "%NATIVE%\" >nul
copy /y "%JNI_OUT%\authlas_jni.dll" "%NATIVE%\" >nul
copy /y "%OPENSSL_DIR%\bin\libssl-3-x64.dll" "%NATIVE%\" >nul
copy /y "%OPENSSL_DIR%\bin\libcrypto-3-x64.dll" "%NATIVE%\" >nul

rem 4. Maven: compile, test, package.
if /i "%~1"=="package" (
  call mvn -B -ntp -DskipTests package
) else (
  call mvn -B -ntp verify
)
if errorlevel 1 exit /b %errorlevel%
echo Build and tests OK.
endlocal
