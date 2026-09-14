# Test Report

## Scope

验证 `D:\IntRuoyiWorktree\backup_20260907` 中备份恢复最小闭环实现是否满足本地开发验证门禁。未执行真实备份、远程服务器、数据库写入或 Playwright E2E。

## Results

| Area | Command | Result |
| --- | --- | --- |
| PowerShell / Python backup contracts | 备份工具、调度、manifest、演练、DCC builder/chain、菜单 SQL 相关 9 个测试文件 | PASS，160 tests |
| Java infra services | `mvn.cmd -pl yudao-module-infra '-Dtest=BackupPlanServiceImplTest,WindowsBackupPlanSchedulerGatewayTest,BackupEvidenceExportServiceTest,BackupPlanMinimalClosureTest,RuntimeBackupDrillServiceImplTest,RuntimeControlOperationActionBackupConfirmTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | PASS，46 tests，BUILD SUCCESS |
| Backend package | `mvn.cmd -pl yudao-module-infra -am '-DskipTests' package -q` | PASS |
| PowerShell / config | 10 个 PowerShell AST、内嵌 retention Python、2 个配置 JSON | PASS |
| Worktree port gate | `scripts\preflight\branch-runtime-port-guard.ps1` | PASS，8162/48162 |
| Frontend static contract | `node tests\e2e\system-backup-plan-standard-list-static.spec.js` | PASS |
| Frontend minimal closure static contract | `node tests\e2e\system-backup-minimal-closure-static.spec.js` | PASS |
| Frontend target lint | `pnpm exec eslint src/api/system/backupPlan/index.ts src/views/system/backup-plan/index.vue` | PASS |
| Frontend full type check | `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit` | BASELINE BLOCKED，4 个错误均在未修改 DCC 上传页，主工作区同样复现 |

## Coverage Notes

- FULL / INCREMENTAL explicit backup kind and no silent fallback are covered.
- MySQL、对象和 DCC 使用同一直接父点；新增量先验证完整既有祖先链。
- DCC 快照与 MySQL/对象处于同一停服窗口；生产确认只来自受保护 secret。
- 计划注册失败会回滚配置并禁用相关任务；证据导出使用单次内存快照并拒绝变化后的 manifest。
- MySQL dump gzip/SHA-256 pre-restore verification and object payload verification before mirror are covered.
- Manifest chain identity, base/parent backup IDs, source/repository evidence and whole-chain retention are covered.
- Separate full and incremental scheduled tasks are covered.
- Recovery candidate / `RECOVERABLE` status requires rehearsal evidence and manifest validation.
- Evidence ZIP generation covers fixed entries, hash manifest, secret redaction, PASS and BLOCKED outcomes.
- Evidence ZIP resolves the complete FULL-to-target chain; a missing or unreadable parent forces BLOCKED and each member carries its raw manifest hash and payload index.
- Frontend covers full/incremental buttons, backup chain display, independent evidence-export permission and blob download.

## Not Run

- Real backup execution, remote server interaction, database writes and Playwright E2E were not run because this turn does not include explicit authorization for those actions.
- 全仓 TypeScript 基线错误：`src/views/dcc/controlled-file/upload/index.vue` 第 1364、1368、1379、1436 行的 `formData.versionNo` 可能为 `undefined`；不属于本任务修改范围。
