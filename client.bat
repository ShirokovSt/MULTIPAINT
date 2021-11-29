@echo off
mvn exec:java -Dexec.mainClass="org.suai.paint.App" -Dexec.args="-C" 
if not "%ERRORLEVEL%" == "0" pause
exit /b
