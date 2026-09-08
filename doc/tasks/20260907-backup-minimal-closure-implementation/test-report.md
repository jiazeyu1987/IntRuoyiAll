# Test Report

## Scope

验证 `D:\IntRuoyiWorktree\tmp_auth_20260907` 中备份恢复最小闭环实现是否满足本地开发验证门禁。已执行真实前端只读 Playwright E2E；未执行真实备份、恢复演练写入、保存计划写入或生产级证据生成。

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
| Frontend real read-only E2E | `node tests\e2e\system-backup-plan-real-readonly.e2e.js` against `http://127.0.0.1:8164` | PASS，覆盖登录、页面进入、status/history API、保存期限来源和质量批准引用回显 |
| Post-rebase regression | Java 44 tests + Python 123 tests + frontend static/type checks after `git rebase int_main` | PASS |
| Static-analysis fix regression | `mvn.cmd -pl yudao-module-infra '-Dtest=BackupPlanServiceImplTest,BackupPlanMinimalClosureTest,RuntimeControlBackupPlanOperationGatewayTest,BackupEvidenceExportServiceTest,WindowsBackupPlanSchedulerGatewayTest,RuntimeBackupDrillServiceImplTest,RuntimeControlOperationActionBackupConfirmTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | PASS，46 tests |

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
- Real read-only E2E covers the current worktree runtime ports `8164/48164` and verifies retention source / quality approval reference回显 from backend status data. Test password is intentionally not recorded.

## Not Run

- Real backup execution, restore rehearsal write, save-plan write and production-grade evidence generation were not run. Production-grade actions still require explicit `PROD` confirmation and target environment preconditions.
