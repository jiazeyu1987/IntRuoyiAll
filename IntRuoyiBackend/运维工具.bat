@echo off
setlocal
chcp 65001 >nul

set "ROOT=%~dp0"
set "PUBLISH_PS1=%ROOT%script\deploy\publish-int-ruoyi.ps1"
set "TEST_RESTART_BAT=%ROOT%script\deploy\restart-int-ruoyi-to-test.bat"
set "PROD_RESTART_BAT=%ROOT%script\deploy\restart-int-ruoyi-to-prod.bat"
set "TEST_STATUS_BAT=%ROOT%script\deploy\show-int-ruoyi-test-status.bat"
set "PROD_STATUS_BAT=%ROOT%script\deploy\show-int-ruoyi-prod-status.bat"

if not exist "%PUBLISH_PS1%" (
  echo [FAIL] Missing unified publish script: %PUBLISH_PS1%
  exit /b 1
)

if not exist "%TEST_RESTART_BAT%" (
  echo [FAIL] Missing test restart wrapper: %TEST_RESTART_BAT%
  exit /b 1
)

if not exist "%PROD_RESTART_BAT%" (
  echo [FAIL] Missing production restart wrapper: %PROD_RESTART_BAT%
  exit /b 1
)

if not exist "%TEST_STATUS_BAT%" (
  echo [FAIL] Missing test status wrapper: %TEST_STATUS_BAT%
  exit /b 1
)

if not exist "%PROD_STATUS_BAT%" (
  echo [FAIL] Missing production status wrapper: %PROD_STATUS_BAT%
  exit /b 1
)

if /i "%~1"=="test" goto route_test
if /i "%~1"=="prod" goto route_prod
if /i "%~1"=="test-restart" goto route_test_restart
if /i "%~1"=="prod-restart" goto route_prod_restart
if /i "%~1"=="test-status" goto route_test_status
if /i "%~1"=="prod-status" goto route_prod_status
if /i "%~1"=="help" goto show_help
if /i "%~1"=="/?" goto show_help
if /i "%~1"=="cancel" goto cancel

echo.
echo ==============================
echo IntRuoyi Ops Tool
echo ==============================
echo 1. Publish
echo 2. Restart
echo 3. Status
echo 4. Help
echo 5. Cancel
echo.
set /p CHOICE=Choose option number:

if "%CHOICE%"=="1" goto menu_publish
if "%CHOICE%"=="2" goto menu_restart
if "%CHOICE%"=="3" goto menu_status
if "%CHOICE%"=="4" goto show_help
if "%CHOICE%"=="5" goto cancel

echo [FAIL] Invalid option: %CHOICE%
exit /b 1

:menu_publish
echo.
echo [Publish]
echo 1. Test publish
echo 2. Production publish
echo 3. Cancel
echo.
set /p SUB_CHOICE=Choose publish target:
if "%SUB_CHOICE%"=="1" goto route_test
if "%SUB_CHOICE%"=="2" goto route_prod
if "%SUB_CHOICE%"=="3" goto cancel
echo [FAIL] Invalid publish option: %SUB_CHOICE%
exit /b 1

:menu_restart
echo.
echo [Restart]
echo 1. Test
echo 2. Production
echo 3. Cancel
echo.
set /p SUB_CHOICE=Choose restart target:
if "%SUB_CHOICE%"=="1" goto route_test_restart
if "%SUB_CHOICE%"=="2" goto route_prod_restart
if "%SUB_CHOICE%"=="3" goto cancel
echo [FAIL] Invalid restart option: %SUB_CHOICE%
exit /b 1

:menu_status
echo.
echo [Status]
echo 1. Test
echo 2. Production
echo 3. Cancel
echo.
set /p SUB_CHOICE=Choose status target:
if "%SUB_CHOICE%"=="1" goto route_test_status
if "%SUB_CHOICE%"=="2" goto route_prod_status
if "%SUB_CHOICE%"=="3" goto cancel
echo [FAIL] Invalid status option: %SUB_CHOICE%
exit /b 1

:show_help
echo.
echo ==============================
echo IntRuoyi Ops Help
echo ==============================
echo Direct commands:
echo   %~nx0 test
echo   %~nx0 prod
echo   %~nx0 test-restart
echo   %~nx0 prod-restart
echo   %~nx0 test-status
echo   %~nx0 prod-status
echo   %~nx0 cancel
echo.
echo Menu flow:
echo   1. Publish  -^> Test publish / Production publish / Cancel
echo   2. Restart  -^> Test / Production / Cancel
echo   3. Status   -^> Test / Production / Cancel
echo   4. Help
echo   5. Cancel
exit /b 0

:route_test
powershell -NoProfile -ExecutionPolicy Bypass -File "%PUBLISH_PS1%" -Environment test %2 %3 %4 %5 %6 %7 %8 %9
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:route_prod
if /i "%~2"=="PROD" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%PUBLISH_PS1%" -Environment prod -ConfirmText PROD %3 %4 %5 %6 %7 %8 %9
) else (
  echo [WARN] This action publishes the current local workspace directly to production.
  set /p PROD_CONFIRM=Type PROD to continue:
  powershell -NoProfile -ExecutionPolicy Bypass -File "%PUBLISH_PS1%" -Environment prod -ConfirmText "%PROD_CONFIRM%" %2 %3 %4 %5 %6 %7 %8 %9
)
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:route_test_restart
call "%TEST_RESTART_BAT%" %2 %3 %4 %5 %6 %7 %8 %9
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:route_prod_restart
call "%PROD_RESTART_BAT%" %2 %3 %4 %5 %6 %7 %8 %9
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:route_test_status
call "%TEST_STATUS_BAT%" %2 %3 %4 %5 %6 %7 %8 %9
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:route_prod_status
call "%PROD_STATUS_BAT%" %2 %3 %4 %5 %6 %7 %8 %9
set "EXIT_CODE=%ERRORLEVEL%"
exit /b %EXIT_CODE%

:cancel
echo [INFO] Ops launcher cancelled.
exit /b 0
