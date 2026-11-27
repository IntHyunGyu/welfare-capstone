@echo off
rem Run Maven on JDK 17 for this project only; the machine default JDK stays untouched.
rem Usage: mvn17 spring-boot:run "-Dspring-boot.run.profiles=local"
setlocal

if defined JDK17_HOME (
    set "JAVA_HOME=%JDK17_HOME%"
) else (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%D"
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo JDK 17 not found. Install Eclipse Temurin 17 or set JDK17_HOME to its folder. 1>&2
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
call mvn %*
exit /b %ERRORLEVEL%
