@echo off
chcp 65001 >nul

if exist "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" (
    set "JCOMP=C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
    set "JRUN=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
) else (
    set "JCOMP=javac"
    set "JRUN=java"
)

if "%1"=="clean" (
    del /f /q Cursar.class config\*.class 2>nul
    echo Limpeza concluida!
    goto end
)
if "%1"=="compile" (
    "%JCOMP%" -encoding UTF-8 Cursar.java config/*.java
    echo Compilacao concluida!
    goto end
)

"%JCOMP%" -encoding UTF-8 Cursar.java config/*.java
if %ERRORLEVEL% EQU 0 (
    "%JRUN%" "-Dfile.encoding=UTF-8" Cursar
)
:end
