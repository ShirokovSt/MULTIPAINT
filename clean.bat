echo off
call mvn clean
if not "%ERRORLEVEL%" == "0" exit /b
pause