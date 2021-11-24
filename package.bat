echo off
call mvn package
if not "%ERRORLEVEL%" == "0" exit /b
pause
