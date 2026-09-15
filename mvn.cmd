@echo off
set "MVN_PATH=mvn"
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    if exist "%USERPROFILE%\.maven\maven-3.10.0-rc-1\bin\mvn.cmd" (
        set "MVN_PATH=%USERPROFILE%\.maven\maven-3.10.0-rc-1\bin\mvn.cmd"
    )
)
"%MVN_PATH%" %*
