# Test Report

## Scope

验证 `D:\IntRuoyiWorktree\tmp_auth_20260907` 中备份恢复最小闭环实现是否满足本地开发验证门禁。未执行真实备份、远程服务器、数据库写入或 Playwright E2E。

## Results

| Area | Command | Result |
| --- | --- | --- |
| Target worktree | `D:\IntRuoyiWorktree\tmp_auth_20260907` / `codex/tmp_auth_20260907` / `int_main slot=30` | PASS |
| PowerShell / Python backup contracts | `python -X utf8 -m pytest --basetemp .pytest-temp script\tests\test_backup_minimal_closure.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_backup_ops_tooling.py script\tests\test_system_backup_plan_menu_sql.py -q` | PASS，123 tests |
| Java infra services | `mvn.cmd -pl yudao-module-infra '-Dtest=BackupPlanServiceImplTest,WindowsBackupPlanSchedulerGatewayTest,BackupEvidenceExportServiceTest,BackupPlanMinimalClosureTest,RuntimeBackupDrillServiceImplTest,RuntimeControlOperationActionBackupConfirmTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | PASS，44 tests，BUILD SUCCESS |
| Frontend dependency install | `pnpm install --frozen-lockfile` | PASS，lockfile 未变 |
| Frontend static contract | `node tests\e2e\system-backup-plan-standard-list-static.spec.js` | PASS |
| Frontend minimal closure static contract | `node tests\e2e\system-backup-minimal-closure-static.spec.js` | PASS |
| Frontend type check | `pnpm ts:check` | PASS |

## Coverage Notes

- FULL / INCREMENTAL explicit backup kind and no silent fallback are covered.
- MySQL dump gzip/SHA-256 pre-restore verification and object payload verification before mirror are covered.
- Manifest chain identity, base/parent backup IDs, source/repository evidence and whole-chain retention are covered.
- Separate full and incremental scheduled tasks are covered.
- Recovery candidate / `RECOVERABLE` status requires rehearsal evidence and manifest validation.
- Evidence ZIP generation covers fixed entries, hash manifest, secret redaction, PASS and BLOCKED outcomes.
- Evidence manifest now covers `overallVerdict`、`chainId`、`targetBackupId` and package file hashes.
- Plan status and save request cover `retentionSource` and `qualityApprovalRef`; missing approval evidence remains BLOCKED instead of default PASS.
- Frontend covers full/incremental buttons, backup chain display, retention source display/input, quality approval reference display/input, independent evidence-export permission and blob download.

## Not Run

- Real backup execution, remote server interaction, database writes and Playwright E2E were not run because this turn does not include explicit authorization for those actions.
