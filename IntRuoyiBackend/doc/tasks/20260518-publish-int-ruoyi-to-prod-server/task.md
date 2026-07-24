# Task: Publish IntRuoyi To Production Server

## Goal

Publish the current local IntRuoyi backend and Vue3 frontend state to the production server through the dedicated production `.bat` wrapper and verify the live runtime.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this production release task.
- Record BDD scenarios for the real production release path before executing it.
- Verify the production target slot and fail fast if the remote runtime location or ports are unsafe.
- Execute the production `.bat` wrapper with explicit confirmation bypass for operator-driven automation.
- Verify backend, frontend, and synchronized file access on the production server.
- Record production release evidence and closeout preview.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-int-ruoyi-prod-bat/task.md`
- Status before this task: completed.
- Impact: the production wrapper task is already closed, so this real production release can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed, create this production release task package, and check the remote production slot.
- [x] M2: Record BDD and RED/green release evidence for the real production path.
- [x] M3: Run the production `.bat` wrapper against the real production server.
- [x] M4: Verify the live production runtime and record closeout evidence.

## Expected Verification

- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat default --yes`
- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- Production backend health on `http://172.30.30.57:48081/actuator/health`
- Production frontend reachability on `http://172.30.30.57:8081/`
- Production synchronized file proof on `http://172.30.30.57:9000/yudao/...`

## Current Status

Completed on 2026-05-18. The current local IntRuoyi state has been published into the isolated production runtime on `172.30.30.57`, and live verification has completed.

## Final Verification Result

- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat default --yes`
- PASS: production backend health `http://172.30.30.57:48081/actuator/health`
- PASS: production frontend reachability `http://172.30.30.57:8081/`
- PASS: production file-object reachability `http://172.30.30.57:9000/yudao/dcc/original/20260518/审核会签.pdf`
- PASS: remote container state:
  - `intruoyi-backend:20260518_225505`
  - `intruoyi-frontend:20260518_225505`
  - `intruoyi-mysql` healthy
  - `intruoyi-redis` healthy

## Blocker And Impact

- Blocker: none.
- Impact: none.
