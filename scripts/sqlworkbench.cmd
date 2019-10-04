@echo off

setlocal

set JAVA_BINPATH=

if exist "%~dp0jre\bin\java.exe" (
   set JAVA_BINPATH=%~dp0jre\bin\
) else (
   if exist "%WORKBENCH_JDK%\bin\java.exe" (
     set JAVA_BINPATH=%WORKBENCH_JDK%\bin\
   ) else (
     if exist "%JAVA_HOME%\jre\bin\java.exe" (
        set JAVA_BINPATH=%JAVA_HOME%\jre\bin\
     ) else (
       if exist "%JAVA_HOME%\bin\java.exe" set JAVA_BINPATH=%JAVA_HOME%\bin\
     )
   )
)

set wbdir=%~dp0

set cp=%wbdir%sqlworkbench.jar;%wbdir%\ext\*

call :get_memory
set /a max_mem=%free_memory% / 2

if "%1"=="console" goto console_mode

rem for Java 9 or later add the option
rem --add-opens java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED
:gui
start "SQL Workbench/J" "%JAVA_BINPATH%javaw.exe"^
      -Xmx%max_mem%m ^
      -Dvisualvm.display.name=SQLWorkbench/J ^
      -Dsun.awt.keepWorkingSetOnMinimize=true ^
      -Dsun.java2d.dpiaware=true ^
      -Dsun.java2d.noddraw=true ^
      -cp %cp% workbench.WbStarter %*
goto :eof

:console_mode
title SQL Workbench/J

"%JAVA_BINPATH%java.exe" -Dvisualvm.display.name=SQLWorkbench ^
                         -cp %cp% workbench.console.SQLConsole %*

goto :eof

:get_memory

  for /f "skip=1" %%p in ('wmic os get FreePhysicalMemory') do ( 
    set /a free_memory=%%p/1024
    goto :eof
  )
  goto :eof

