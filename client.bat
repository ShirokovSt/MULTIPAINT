@echo off
rem set /p ip=Input ip: 
rem set /p port=Input port: 
mvn exec:java -Dexec.mainClass="org.suai.paint.App" -Dexec.args="-C" 
rem %ip% %port%"
if not "%ERRORLEVEL%" == "0" pause
exit /b
