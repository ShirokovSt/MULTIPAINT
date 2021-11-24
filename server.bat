echo off
call mvn exec:java -Dexec.mainClass="org.suai.paint.App" -Dexec.args="-S"
if not "%ERRORLEVEL%" == "0" exit /b
pause
