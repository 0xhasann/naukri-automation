@echo off
REM LIVE mode: really applies. Daily limit 5 for the first run - raise it in config.yaml later.
cd /d "%~dp0"
call mvn test -Dsuite=testng-e2e.xml -Dmode=LIVE -DdailyApplicationLimit=5
pause
