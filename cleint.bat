echo off
set /p ip=Input ip: 
set /p port=Input port: 
mvn exec:java -Dexec.mainClass="org.suai.paint.App" -Dexec.args="-C %ip% %port%"
if not "%ERRORLEVEL%" == "0" exit /b
pause
