@echo off
REM Double-click to run in DRY_RUN mode (search + score only, never applies).
cd /d "%~dp0"
call mvn test -Dsuite=testng-e2e.xml -Dmode=DRY_RUN -DmaxJobsPerSearch=10 -DmaxSearchPagesPerKeyword=1
pause
