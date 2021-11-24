echo off
javac App.java
set /p ip=Enter ip: 
set /p port=Enter port:
java App -C %ip% %port%
pause
