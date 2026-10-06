#!/bin/bash
# usage: ./run.sh DRY_RUN | TEST | LIVE
cd "$(dirname "$0")"
mvn test -Dsuite=testng-e2e.xml -Dmode="${1:-DRY_RUN}" "${@:2}"
