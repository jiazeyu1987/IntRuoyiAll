# Execution Log

## Intent

- User requested implementation of the System Management backup plan feature.
- Work is isolated in `D:\IntRuoyiWorktree\system-backup-plan` on branch `codex/system-backup-plan` to avoid dirty main-workspace changes.

## Preflight

- Read skills: backup disaster recovery, frontend feature delivery, backend API delivery, database schema delivery, QA test suite.
- Read trigger docs: worktree restrictions, frontend development, backend development, database rules, E2E rules, release backup restore, task closeout, local runtime, login access, PowerShell encoding.
- Read `docs/experience-index.md` after creating the task directory.
- GREEN: experience-preflight -> PASS, matched backup/release, worktree, menu permission, frontend, backend, and E2E gates.

## BDD

- BDD: 管理员查看备份计划 -> Given 管理员有 `system:backup-plan:query` 权限, When 打开系统管理备份计划, Then 页面显示自动备份状态、频率、时间、下次运行、上次结果和历史备份包列表。
- BDD: 管理员保存每天备份计划 -> Given 管理员有 `system:backup-plan:update` 权限, When 选择“每天”并保存 `01:30`, Then 后端写入配置并注册真实调度任务，返回新的下次运行时间。
- BDD: 管理员保存每周备份计划 -> Given 管理员有 `system:backup-plan:update` 权限, When 选择“每周”和星期/时间, Then 后端写入配置并注册每周调度任务。
- BDD: 缺少脚本时阻塞 -> Given 备份脚本路径不存在, When 保存或启用计划, Then 接口返回明确错误，不启用错误任务。
- BDD: 管理员立即备份一次 -> Given 管理员有 `system:backup-plan:execute` 权限, When 确认“现在备份一次”, Then 后端调用现有备份动作并显式传入生产备份确认链路。
- BDD: worktree 运行配置按登记表解析 -> Given 当前分支是 `codex/system-backup-plan` 且端口登记表记录 `profile=int_main/slot=2`, When 运行 branch runtime 脚本, Then 默认和显式 slot 都解析为前端 `8083`、后端 `48083`，登记异常必须 fail-fast。

## RED / GREEN

- RED: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing backup plan controller/service VO and scheduler classes.
- RED: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` -> FAIL, expected missing `src/views/system/backup-plan/index.vue`.
- GREEN: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests, backup plan service and production backup confirmation chain.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py` -> PASS, 4 tests, backup scheduling script contract and menu SQL contract.
- GREEN: `pnpm install --frozen-lockfile` -> PASS after increasing timeout; dependency install produced pnpm ignored-build-scripts warning for package install scripts, but type check did not require those scripts.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `pnpm build:local` -> PASS, Vite build successful and generated ignored `dist` output.
- GREEN: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS.
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS, 372 migrations.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py script\tests\test_release_migration_metadata.py` -> PASS, 6 tests.
- GREEN: `git diff --check` -> PASS with line-ending warnings only.
- RED: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -Slot 2` -> FAIL, branch `codex/system-backup-plan` at `D:\IntRuoyiWorktree\system-backup-plan` was not recognized by fixed branch/path runtime profile rules.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL, expected missing registry-backed runtime context and fail-fast validation.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 3 tests.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1` -> PASS, resolved `profile=int_main`, `slot=2`, frontend `8083`, backend `48083`.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -Slot 2` -> PASS, same registered runtime context.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, guard resolved current worktree to frontend `8083`, backend `48083`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260725-system-backup-plan\bug-regression-evidence.md` -> PASS.
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS, generated `yudao-server-exec.jar` for local runtime.
- GREEN: `Invoke-RestMethod http://127.0.0.1:48083/actuator/health` -> PASS, `status=UP`.
- RED: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --target-path /system/backup-plan` -> FAIL, frontend runtime lacked local API/captcha env injection and login preflight could not complete.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL, branch frontend start script lacked required `VITE_API_URL=/admin-api` and `VITE_APP_CAPTCHA_ENABLE=false` injection.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 4 tests.
- GREEN: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --target-path /system/backup-plan --target-text 备份计划` -> PASS, real login to `芋道源码/admin`.
- RED: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> FAIL, initial script path calculation used repository root `.env` instead of frontend `.env`.
- RED: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> FAIL, page exposed `manifest/manifest.json` technical path in the 保存位置 column.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS, static contract blocks direct manifest/snapshot path display.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS, read-only real page E2E opened `/system/backup-plan`, saw status/history API 200, status card, simple controls, history list, and no `manifest` UI text.
- GREEN: `pnpm ts:check` -> PASS after frontend display/runtime fixes.
- GREEN: `pnpm build:local` -> PASS after increasing timeout to 420s; previous 244s attempt timed out and was not counted as verification.
- GREEN: `git diff --check` -> PASS with CRLF conversion warnings only after latest fixes.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS after latest fixes.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS after latest fixes.
- GREEN: stopped task-owned local runtime processes on ports `8083` and `48083`; remaining listeners count `0`.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1` -> PASS on 2026-07-25 local E2E rerun, resolved `profile=int_main`, `slot=2`, frontend `8083`, backend `48083`.
- GREEN: local backend startup for rerun -> PASS, `http://127.0.0.1:48083/actuator/health` returned `status=UP`, listener pid `49760`, command line pointed to this worktree `yudao-server-exec.jar`.
- GREEN: local frontend startup for rerun -> PASS, `http://127.0.0.1:8083/` returned HTTP `200`, listener pid `54880`, command line pointed to this worktree Vite runtime.
- RED: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --target-path /system/backup-plan --target-text 备份计划` -> FAIL, expected reason: script requires explicit `--tenant`, `--username`, and `--password` parameters and must not infer them silently.
- GREEN: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --tenant <from IntRuoyiFronted\.env> --username <from IntRuoyiFronted\.env> --password <redacted from IntRuoyiFronted\.env> --target-path /system/backup-plan --target-text 备份计划` -> PASS, real login to `芋道源码/admin` without recording the password.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS on 2026-07-25 rerun, real page opened `/system/backup-plan`, status/history API returned 200, simple controls and history table were visible, and `manifest` was not visible.
- GREEN: cleanup after local E2E rerun -> PASS, stopped task-owned pids `54880` and `49760`; remaining listeners on `8083/48083` count `0`.
- GREEN: implementation commit -> PASS, commit `2d669910` (`feat: add system backup plan management`) contains backup plan implementation, tests, SQL, runtime scripts, experience docs, and task evidence.
- GREEN: merge `origin/int_main` -> PASS, commit `8a3bb44a`; only add/add conflicts were task-local `task.md` and `execution-log.md`, resolved by keeping completed evidence and removing upstream placeholder `in_progress/Pending` entries.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py IntRuoyiBackend\script\tests\test_backup_ops_scheduling_tooling.py IntRuoyiBackend\script\tests\test_system_backup_plan_menu_sql.py IntRuoyiBackend\script\tests\test_release_migration_metadata.py` from repo root -> FAIL, expected reason: `test_release_migration_metadata.py` imports backend `script` package and must run from `IntRuoyiBackend` or with equivalent import path.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py script\tests\test_release_migration_metadata.py` from `IntRuoyiBackend` -> PASS, 6 tests after merge.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 4 tests after merge.
- GREEN: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests after merge.
- GREEN: `pnpm ts:check` -> PASS after merge.
- GREEN: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` from `IntRuoyiFronted` -> PASS after merge.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS after merge.
- GREEN: `git diff --check` -> PASS after merge.
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS after merge, regenerated `yudao-server-exec.jar` for local E2E.
- GREEN: local backend startup after merge -> PASS, `http://127.0.0.1:48083/actuator/health` returned `status=UP`, listener pid `51356`.
- GREEN: local frontend startup after merge -> PASS, `http://127.0.0.1:8083/` returned HTTP `200`, listener pid `53156`.
- GREEN: `node scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8083 --tenant <from IntRuoyiFronted\.env> --username <from IntRuoyiFronted\.env> --password <redacted from IntRuoyiFronted\.env> --target-path /system/backup-plan --target-text 备份计划` -> PASS after merge.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-backup-plan-real-readonly.e2e.js` -> PASS after merge, real page opened `/system/backup-plan`, status/history API returned 200, simple controls and history table remained visible, and `manifest` was not visible.
- GREEN: cleanup after post-merge E2E -> PASS, stopped task-owned pids `53156` and `51356`; remaining listeners on `8083/48083` count `0`.
- GREEN: GitHub large object preflight -> PASS, `git rev-list --objects origin/int_main..HEAD | git cat-file --batch-check` found no large blobs; largest task blob was far below GitHub 100 MB limit.
- BLOCKER: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-system-backup-plan --mode preview` -> BLOCKED, preview would keep `task.md`, `execution-log.md`, `verification-report.md` and delete `bug-regression-evidence.md`, but apply/ff-only worktree closeout is blocked because main worktree `E:\IntRuoyi` is dirty.
- BLOCKER DETAIL: `git status --short --branch` in `E:\IntRuoyi` shows modified MES/System/Frontend/E2E/task-doc files and untracked task directories unrelated to this backup-plan task. Per ownership rules, this task did not clean or modify those main-worktree changes.
- GREEN: main workspace dirty baseline -> PASS, commit `ceec4052` saved pre-fusion main-workspace dirty changes before merging backup plan.
- GREEN: follow-up dirty baseline -> PASS, commit `051416cf` saved additional pre-fusion changes generated after the first baseline.
- GREEN: backup plan fused into `int_main` -> PASS, merge commit `61188ec3` merged `origin/codex/system-backup-plan` into `int_main`.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` on `int_main` -> FAIL, expected reason: the worktree-slot test still assumed the current repo path was branch `codex/system-backup-plan` while the fused main workspace is branch `int_main`.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS after making the registered-worktree test call `Resolve-BranchRuntimeContext` with an explicit worktree path and branch, 4 tests.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_system_backup_plan_menu_sql.py script\tests\test_release_migration_metadata.py` from `IntRuoyiBackend` -> PASS, 6 tests on fused `int_main`.
- GREEN: `mvn "-pl" "yudao-module-infra" "-Dtest=BackupPlanServiceImplTest,RuntimeControlOperationActionBackupConfirmTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests on fused `int_main`.
- GREEN: `pnpm ts:check` -> PASS on fused `int_main`.
- GREEN: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` from `IntRuoyiFronted` -> PASS on fused `int_main`.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS on fused `int_main`.
- GREEN: `git diff --check` -> PASS on fused `int_main` before final closeout record.
- GREEN: post-fusion dirty baselines -> PASS, commits `60d17822` and `d84abd4f` saved concurrent Runner/main-workspace changes separately from the backup-plan implementation.
- BLOCKER: worktree deletion remains blocked because active process `51372` (`IntRuoyiFronted\scripts\codex-test-runner.mjs --loop`) keeps `doc\tasks\20260725-codex-runner-void-test\codex-runner-loop.pid` and related non-backup-plan artifacts in the main workspace. The `.pid` file is a runtime artifact and was intentionally not committed.

## Implementation Notes

- Added backend controller and service under infra runtime capability while exposing permissions as `system:backup-plan:*`.
- Added `BackupPlanSchedulerGateway` with Windows Task Scheduler implementation; non-Windows environments fail fast with a clear unsupported scheduler error.
- Fixed scheduler status query behavior so `schtasks /Query` command failure is reported as disabled/config abnormal instead of being interpreted as healthy.
- Fixed branch runtime profile resolution so registered worktrees under `D:\IntRuoyiWorktree\` resolve through `.ports\worktree-ports.json`; duplicate active entries, inactive entries, missing fields, branch mismatch, invalid slot, unknown profile, and port/profile mismatch all fail fast.
- Fixed branch frontend startup to inject `VITE_API_URL=/admin-api` and `VITE_APP_CAPTCHA_ENABLE=false` so worktree runtimes do not depend on untracked `.env.local` files for real E2E login.
- Updated scheduled task registration script to support `backup.frequency = DAILY|WEEKLY`, `backup.weekday`, and production backup confirmation parameters.
- Added frontend page `src/views/system/backup-plan/index.vue` with simple controls only: automatic backup switch, daily/weekly frequency, weekday, time picker, save, refresh, and "现在备份一次".
- Replaced direct backup `manifestPath`/`snapshotPath` display with `formatBackupStorageLabel(row)` so the low-barrier page shows `备份包：<备份编号>` rather than technical manifest paths.
- Added SQL migration `20260725_system_backup_plan_menu.sql` using high menu IDs `901100-901102`, fail-fast ID conflict checks, tenant package merge, and `super_admin` / `tenant_admin` role menu binding.

## Blockers

- RESOLVED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` initially failed because existing migration metadata used `.sql` suffix: `20260723_mes_edhr_assignment_rule_candidate_nullable.sql: 20260614_mes_edhr_work_task_candidate_pool.sql`. Fixed metadata-only dependency string and reran full gate -> PASS.
- RESOLVED: Read-only real Playwright E2E for `系统管理 > 备份计划` now passes on local worktree ports `8083/48083` after local menu SQL application. It validates page visibility and status/history API calls only.
- BLOCKER: Write-type E2E for saving/enabling/disabling schedule and immediate backup is not run because those actions mutate a real Windows scheduled task or trigger backup execution; requires explicit authorized test scheduler environment.
- RESOLVED: `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -Slot 2` initially failed because branch runtime scripts only used fixed branch/path profile inference and ignored the registered worktree port registry. Added registry-backed resolution and fail-fast validation; `show-branch-runtime.ps1`, start scripts, and branch runtime guard now consume the registered context.
- BLOCKER: Production publish/register/verify is not executed because formal production operation requires explicit release authorization.
- RESOLVED: Current branch `codex/system-backup-plan` was behind `origin/int_main` by 15 commits; resolved by merge commit `8a3bb44a`. Current branch is ahead of `origin/int_main` only by local task commits pending closeout/push.
- BLOCKER: worktree cleanup/apply and deletion are blocked by dirty main worktree `E:\IntRuoyi`; current branch can still be pushed for review, but task cannot be marked `completed` or remove the worktree until the main workspace is clean.
- RESOLVED: Backup plan branch was fused into `int_main` with merge commit `61188ec3` and verified on the main workspace.

## Experience Consolidation

- Updated `docs/release-backup-restore.md` with a reusable backup scheduled task status gate: command exit code, enabled state, next run time, last result, task script path, and backup package evidence must all be checked before claiming scheduled backup recovery.
- Updated `docs/experience-index.md` so future tasks matching `IntRuoyi Backup Scheduled`, `NextRunTime N/A`, `schtasks` failure, or old `Task To Run` paths route to the backup plan task gate.
