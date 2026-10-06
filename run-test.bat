@echo off
REM TEST mode: like DRY_RUN plus a rehearsal of the answers. Never clicks Apply.
cd /d "%~dp0"
call mvn test -Dsuite=testng-e2e.xml -Dmode=TEST -DmaxJobsPerSearch=10
pause
