# Task: Reverify IntRuoyi Test Publish Script End To End

## Goal

Make the IntRuoyi test-server publish script itself reliably complete the full local-to-test release flow, then prove it by running the final script end to end against the real test server.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this follow-up verification task.
- Record BDD scenarios for script-driven release behavior before changing the publish tooling.
- Add or extend a failing script test for any missing publish preconditions in the current script.
- Apply only the minimal publish-script fixes needed for a full end-to-end test-server release.
- Run the updated script against the real test server and verify backend, frontend, and file-object reachability.
- Record CI/CD evidence and closeout preview for this follow-up verification task.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-publish-int-ruoyi-to-test-server/task.md`
- Status before this task: completed.
- Impact: the previous deployment task is already closed, so this verification follow-up can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this follow-up task package.
- [x] M2: Record BDD scenarios and add RED script verification for any missing publish steps.
- [x] M3: Fix the publish script and script tests with the minimal tooling changes.
- [x] M4: Run the final publish script end to end against the real test server.
- [x] M5: Record verification evidence, run closeout preview, and prepare a task-scoped commit.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- Remote backend health check on `http://172.30.30.58:48081/actuator/health`
- Remote frontend reachability on `http://172.30.30.58:8081/`
- Remote file-object reachability on `http://172.30.30.58:9000/yudao/...`
- `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\docs\environments\ci-cd-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-publish-script-reverify --mode preview`

## Current Status

Completed on 2026-05-18. The final `publish-int-ruoyi-to-test.ps1` script has been reverified end to end against the real test server without manual continuation.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- PASS: script output ended with:
  - `Publish completed.`
  - `Frontend: http://172.30.30.58:8081`
  - `Backend health: http://172.30.30.58:48081/actuator/health`
- PASS: remote backend readiness `http://127.0.0.1:48081/actuator/health`
- PASS: remote frontend readiness `http://127.0.0.1:8081/`
- PASS: local external reachability `http://172.30.30.58:48081/actuator/health` and `http://172.30.30.58:8081/`
- PASS: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\docs\environments\ci-cd-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-publish-script-reverify --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
