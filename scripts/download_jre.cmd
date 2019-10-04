@echo off
echo This batchfile will download the most recent Java 12 JRE (64bit) from https://adoptopenjdk.net/
echo to be used with SQL Workbench/J
echo.

if exist "%~dp0jre" (
  echo "A JRE directory already exists."
  echo "Please remove (or rename) this directory before running this batch file"
  goto :eof
)
set /P continue="Do you want to continue? (Y/N) "

if /I "%continue%"=="y" goto make_jre
if /I "%continue%"=="yes" goto make_jre

goto :eof

:make_jre
@powershell.exe -noprofile -executionpolicy bypass -file download_jre.ps1

setlocal

FOR /F " usebackq delims==" %%i IN (`dir /ad /b jdk*`) DO set jdkdir=%%i
rem echo %jdkdir%

FOR /F " usebackq delims==" %%i IN (`dir /b OpenJDK*.zip`) DO set zipfile=%%i

echo Validating ZIP file
certutil -hashfile %zipfile%.sha256.txt sha256 > nul
rem echo level: %errorlevel%

if errorlevel 1 (
  echo The integrity of the file %zipfile% could not be validated - the checksum did not match
  goto :eof
)

ren %jdkdir% jre
echo.
echo JRE created in %~dp0jre
echo You can delete the ZIP archive %zipfile% now
echo.

