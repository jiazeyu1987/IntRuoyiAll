# IntRuoyi OPS Guide

## Purpose

This guide summarizes the currently verified operations toolkit for IntRuoyi.

## Main Entry

| Item | Value |
|---|---|
| Unified launcher | `运维工具.bat` |
| Absolute path | `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat` |
| Recommended `cmd` invocation | `cmd /c "D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help"` |
| Repository-root invocation | `运维工具.bat help` |

## Unified Menu

| Menu Level | Options |
|---|---|
| Root menu | `Publish`, `Restart`, `Status`, `Help`, `Cancel` |
| Publish submenu | `Test publish`, `Production publish`, `Cancel` |
| Restart submenu | `Test`, `Production`, `Cancel` |
| Status submenu | `Test`, `Production`, `Cancel` |

## Direct Commands

| Command | Meaning |
|---|---|
| `运维工具.bat test` | Publish current local workspace to the test server |
| `运维工具.bat prod` | Publish current local workspace to the production server after explicit `PROD` confirmation |
| `运维工具.bat test-restart` | Test restart |
| `运维工具.bat prod-restart` | Production restart |
| `运维工具.bat test-status` | Test status |
| `运维工具.bat prod-status` | Production status |
| `运维工具.bat help` | Help page |
| `运维工具.bat cancel` | Safe cancel |

## Environment Targets

| Environment | Host | Runtime Dir | IntRuoyi Frontend | Backend Health | Website Root | Website Showroom |
|---|---|---|---|---|---|---|
| Test | `172.30.30.58` | `/opt/intruoyi/runtime` | `http://172.30.30.58:8081` | `http://172.30.30.58:48081/actuator/health` | `http://172.30.30.58:8083/` | `http://172.30.30.58:8083/showroom` |
| Production | `172.30.30.57` | `/opt/intruoyi/runtime` | `http://172.30.30.57:8081` | `http://172.30.30.57:48081/actuator/health` | `http://172.30.30.57:8083/` | `http://172.30.30.57:8083/showroom` |

## Publish Toolkit

| Script | Purpose | Modes / Notes |
|---|---|---|
| `script\deploy\publish-int-ruoyi.ps1` | Unified publish implementation | Use `-Environment test` for `172.30.30.58`; use `-Environment prod -ConfirmText PROD` for `172.30.30.57`; builds backend/frontend from the local workspace, builds `D:\ProjectPackage\Website`, exports images, syncs MinIO, resets/imports MySQL, deploys `backend/frontend/website`, and prints both IntRuoyi + Website URLs |

## Restart Toolkit

| Script | Purpose | Modes / Notes |
|---|---|---|
| `script\deploy\restart-int-ruoyi-remote.ps1` | Shared restart implementation | Restarts only remote `backend` and `frontend`, no rebuild or data sync, waits for readiness |
| `script\deploy\restart-int-ruoyi-to-test.bat` | Test restart wrapper | Direct restart or `cancel` |
| `script\deploy\restart-int-ruoyi-to-prod.bat` | Production restart wrapper | Targets `172.30.30.57`, requires `PROD` confirmation, supports `cancel` |

## Status Toolkit

| Script | Purpose | Output |
|---|---|---|
| `script\deploy\show-int-ruoyi-remote-status.ps1` | Shared status implementation | Runtime directory presence, `intruoyi-*` container state, backend health, frontend status |
| `script\deploy\show-int-ruoyi-test-status.bat` | Test status wrapper | Current test runtime state |
| `script\deploy\show-int-ruoyi-prod-status.bat` | Production status wrapper | Current production runtime state |

## Safety Notes

| Rule | Explanation |
|---|---|
| Production publish is guarded | `publish-int-ruoyi.ps1 -Environment prod` requires explicit `-ConfirmText PROD` |
| Production restart is guarded | `restart-int-ruoyi-to-prod.bat` requires explicit `PROD` confirmation unless `cancel` is used |
| Use `cancel` for dry navigation | Wrappers and the unified launcher expose `cancel` for safe validation |
| Use status before mutation | `test-status` and `prod-status` provide quick read-only checks before publish or restart |

## Verified Current Runtime

| Environment | Verified State |
|---|---|
| Test | `frontend 172.30.30.58:8081` and `backend 172.30.30.58:48081` are live |
| Production | `frontend 172.30.30.57:8081` and `backend 172.30.30.57:48081` are live |

## Related Evidence

| Evidence | File |
|---|---|
| Test publish evidence | `docs\environments\ci-cd-evidence.md` |
| Production publish evidence | `docs\environments\ci-cd-prod-evidence.md` |
