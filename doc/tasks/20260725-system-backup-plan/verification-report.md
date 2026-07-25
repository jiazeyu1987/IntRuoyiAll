# Verification Report

## Scope

- Feature: `系统管理 > 备份计划`
- Worktree: `D:\IntRuoyiWorktree\system-backup-plan`
- Branch: `codex/system-backup-plan`

## Passed Verification

- Backend targeted tests: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- Backup script and menu SQL tests: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py` -> PASS, 4 tests.
- Frontend dependency restore: `pnpm install --frozen-lockfile` -> PASS; pnpm reported ignored build scripts, but no lockfile change was produced.
- Frontend type check: `pnpm ts:check` -> PASS.
- Frontend build: `pnpm build:local` -> PASS.
- Frontend build after latest UI/runtime fixes: `pnpm build:local` -> PASS after increasing timeout to 420s; previous 244s attempt timed out and was not counted.
- Frontend static contract: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS.
- Frontend read-only real E2E: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS, real login to `芋道源码/admin`, opened `/system/backup-plan`, verified status/history API 200, simple controls, history columns, and no `manifest` text on the page.
- Local backend runtime: `Invoke-RestMethod http://127.0.0.1:48083/actuator/health` -> PASS, `status=UP`.
- Local frontend runtime: `Invoke-WebRequest http://127.0.0.1:8083/` -> PASS, HTTP 200.
- Official login preflight: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --target-path /system/backup-plan --target-text 备份计划` -> PASS.
- Migration policy gate: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS, 372 migrations.
- Branch runtime profile regression: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 4 tests.
- Branch runtime context: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1` -> PASS, `profile=int_main`, `slot=2`, frontend `8083`, backend `48083`.
- Branch runtime context explicit slot: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -Slot 2` -> PASS, same registered context.
- Branch runtime port guard: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- Whitespace check: `git diff --check` -> PASS with CRLF conversion warnings only.

## Latest Local E2E Rerun

- Date: `2026-07-25`.
- Runtime context: `profile=int_main`, `slot=2`, frontend `8083`, backend `48083`.
- Backend runtime: `http://127.0.0.1:48083/actuator/health` -> PASS, `status=UP`, listener pid `49760`, command line pointed to this worktree Jar.
- Frontend runtime: `http://127.0.0.1:8083/` -> PASS, HTTP `200`, listener pid `54880`, command line pointed to this worktree Vite runtime.
- Login preflight: first run without explicit tenant/user/password failed as expected; rerun with values loaded from `IntRuoyiFronted\.env` and password redacted -> PASS, real login to `芋道源码/admin`.
- Read-only real E2E: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS, opened `/system/backup-plan`, verified backup status/history API 200, simple user-facing controls, history table columns, and no visible `manifest` technical text.
- Screenshot evidence: `output\playwright\system-backup-plan-readonly.png`.
- Cleanup: stopped task-owned pids `54880` and `49760`; ports `8083/48083` have no remaining listeners.

## Post-Merge Verification

- Merge: `origin/int_main` merged into `codex/system-backup-plan` with commit `8a3bb44a`; task-local doc conflicts were resolved by preserving completed evidence.
- Backend script tests: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py script\tests\test_release_migration_metadata.py` from `IntRuoyiBackend` -> PASS, 6 tests.
- Branch runtime regression: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 4 tests.
- Backend targeted tests: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- Frontend type check: `pnpm ts:check` -> PASS.
- Frontend static contract: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS.
- Branch runtime guard: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- Whitespace check: `git diff --check` -> PASS.
- Backend package for E2E: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server-exec.jar`.
- Local backend runtime: `http://127.0.0.1:48083/actuator/health` -> PASS, `status=UP`, listener pid `51356`.
- Local frontend runtime: `http://127.0.0.1:8083/` -> PASS, HTTP `200`, listener pid `53156`.
- Login preflight: explicit tenant/user/password loaded from `IntRuoyiFronted\.env` with password redacted -> PASS, real login to `芋道源码/admin`.
- Read-only real E2E: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS, verified `/system/backup-plan`, status/history API 200, simple controls, history table columns, and no visible `manifest` technical text.
- Runtime cleanup: stopped task-owned pids `53156` and `51356`; ports `8083/48083` have no remaining listeners.
- GitHub large object preflight: `git rev-list --objects origin/int_main..HEAD | git cat-file --batch-check` -> PASS, no blob near 100 MB.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-system-backup-plan --mode preview` -> BLOCKED because main worktree `E:\IntRuoyi` is dirty and cannot receive ff-only merge. No cleanup apply was run.

## Release-Level Blockers

- Write-type real Playwright E2E is not complete: saving/enabling/disabling schedule and immediate backup mutate a real Windows scheduled task or trigger backup execution, so they require an explicitly authorized test scheduler environment.
- Production release, task registration, `NextRunTime` verification, and optional immediate backup verification require explicit production authorization.
- Current branch `codex/system-backup-plan` is merged with `origin/int_main` and is ahead locally; final closeout still requires cleanup commit and `git push origin codex/system-backup-plan`.
- Worktree closeout and deletion remain blocked until the main worktree `E:\IntRuoyi` is clean.

## Release Recommendation

- Code implementation is ready for reviewer inspection.
- Do not announce production scheduled backup recovery until release is authorized, the menu SQL is applied, the Windows task is registered/enabled, `NextRunTime` is non-empty, and backup history shows a successful new package.
- Do not run write-type backup plan E2E on this workstation unless the target Windows scheduled task is explicitly authorized as a test scheduler.
