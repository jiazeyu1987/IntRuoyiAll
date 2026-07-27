# Execution Log

## 2026-07-27

- User intent: 发布到测试服务器。
- Target scope: only test server `172.30.30.58`; forbidden actions are prod, backup, `mark-tested`, `promote-prod`, and `promote-backup`.
- Rules loaded: `ci-cd-environment-delivery`, `docs/server-access.md`, `docs/release-backup-restore.md`, `docs/task-closeout-rules.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/powershell-memory.md`, `docs/powershell-encoding.md`, `docs/database-rules.md`, `docs/backend-development.md`, `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md`, `docs/experience-index.md`.
- BDD: 测试服发布 OnlyOffice 修复 -> Given 发布基线包含 DCC OnlyOffice 下载地址修复, When 发布到测试服务器, Then 测试服后端和前端运行在同一 releaseTag，后端 health 为 UP，前端 HTTP 200，且 release-info/manifest 指向本轮发布提交。
- Baseline: initial frozen `origin/int_main` commit `9562dca4982007f36c302aaa99847a59d6a4c28e` contains `application-local.yaml` `public-file-base-url` default `http://host.docker.internal:${server.port}`.
- Release worktree: `D:\IntRuoyiWorktree\onlyoffice-test-release-20260727`.
- Release branch: `codex/20260727-onlyoffice-test-release`.
- ReleaseTag: `release-20260727-onlyoffice-test-r260727-1445`.
- Current main workspace: local `HEAD` is ahead of `origin/int_main` and has unrelated dirty/untracked concurrent task files; this release will not use dirty main workspace as build input.
- GREEN: experience-preflight -> PASS；matched test-only release, server access, worktree, migration metadata, PowerShell, and local OnlyOffice Docker URL gates.
- RED: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260727-onlyoffice-test-server-release-migration-policy-gate.json` -> FAIL, invalid release-migration `type=config-seed` in `20260725_mes_edhr_recordbook_global_setting.sql`; same invalid enum also existed in `20260726_mes_edhr_release_dossier_requirements.sql`.
- Change: updated both `infra_config` configuration SQL metadata headers from `type=config-seed` to allowed `type=config`; SQL runtime logic was not changed.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260727-onlyoffice-test-server-release\migration-policy-gate.json` -> PASS, status `passed`, migrationCount `382`.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_release_migration_policy_gate.py IntRuoyiBackend\script\tests\test_release_migration_metadata.py IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` from repo root -> FAIL, `ModuleNotFoundError: No module named 'script'`; command working directory was incorrect, product code unchanged.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py script\tests\test_release_migration_metadata.py script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` from `IntRuoyiBackend` -> PASS, `12 passed in 0.89s`.
- RED: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, reactor `maven-dependency-plugin:unpack` cannot unpack not-yet-packaged reactor artifact in `yudao-server`.
- GREEN: `mvn -pl yudao-server "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.
- Experience consolidation: existing `docs/experience-index.md` and release build lessons already contain the migration metadata allowed-type gate; no new long-term document is needed.
- Worktree slot reservation: `reserve-worktree-slot.ps1 -Name onlyoffice-test-release-20260727 -Profile int_main` -> slot `3`, frontend `8084`, backend `48084`; services were not started.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, branch runtime port guard passed for `codex/20260727-onlyoffice-test-release/int_main`.
- Implementation commit: `24bc99fef31ef1ef9ea6f479775b032452af2dfb` (`fix: align release migration metadata types`) pushed to `origin/codex/20260727-onlyoffice-test-release`.
- RED: `publish-int-ruoyi.ps1 -Mode build-release -Component intruoyi -ReleaseTag release-20260727-onlyoffice-test-r260727-1445 -Environment test -ServerHost 172.30.30.58 -TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59 -SkipDatabaseSync -SkipMinioSync` -> FAIL after backend package success, `Missing frontend Vite CLI: D:\IntRuoyiWorktree\onlyoffice-test-release-20260727\yudao-ui-admin-vue3\node_modules\vite\bin\vite.js`.
- Root cause: release script still preferred legacy sibling frontend folder `yudao-ui-admin-vue3`; current project worktree contains `IntRuoyiFronted`.
- Change: `publish-int-ruoyi.ps1` now resolves `IntRuoyiFronted` first and keeps legacy `yudao-ui-admin-vue3` only when the current folder is absent.
- GREEN: `corepack pnpm@10.25.0 install --frozen-lockfile` from `IntRuoyiFronted` -> PASS, lockfile unchanged and `node_modules` restored for release build.
