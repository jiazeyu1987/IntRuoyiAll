# Bug Regression Evidence

## Bug summary and expected behavior

- Bug 1: 备份计划页手动备份网关硬编码 `targetEnvironment=prod` 和 `prodConfirmText=PROD`，导致普通页面按钮绕过运行控制台生产确认门禁。
- Expected: 备份计划手动备份必须使用已批准的备份计划配置目标环境；当前最小闭环只允许 `backup.repositoryEnvironment=test`，不得由网关自行补生产确认。

- Bug 2: 审查证据导出 PASS 判定未要求最新备份点源 `manifest` 和 `checksums` 文件真实存在，可能在证据包缺关键源文件时仍输出 `overallVerdict=PASS`。
- Expected: 缺 manifest 或 checksum 清单必须输出 `overallVerdict=BLOCKED`，并在 blockers 中列明缺失项。

## Reproduction command or path

- Static analysis path: `RuntimeControlBackupPlanOperationGateway.backupNow()`、`BackupPlanServiceImpl.backupNow()`、`BackupEvidenceExportService.exportLatest()`。
- RED command: `mvn.cmd -pl yudao-module-infra '-Dtest=BackupEvidenceExportServiceTest#exportLatestShouldBlockWhenSourceManifestOrChecksumIsMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## Root Cause

- Bug 1 root cause: 备份计划网关把运行控制台请求参数写死为正式服和 `PROD`，没有从 `backup-ops.config.json` 的 `backup.repositoryEnvironment` 取得目标环境。
- Bug 2 root cause: `BackupEvidenceExportService.isPass()` 只检查计划状态、最新备份点 recoverability 和演练状态，遗漏了证据包源文件存在性门禁。

## Regression test added or updated

- Added `RuntimeControlBackupPlanOperationGatewayTest.backupNowShouldPassConfiguredTargetWithoutHardcodedProdConfirmation`。
- Updated `BackupPlanServiceImplTest.backupNowShouldDelegateToRuntimeOperationGateway`。
- Updated `BackupPlanMinimalClosureTest.backupNowShouldForwardExplicitKind`。
- Added `BackupEvidenceExportServiceTest.exportLatestShouldBlockWhenSourceManifestOrChecksumIsMissing`。

## RED command and expected failure

- RED: `mvn.cmd -pl yudao-module-infra '-Dtest=BackupEvidenceExportServiceTest#exportLatestShouldBlockWhenSourceManifestOrChecksumIsMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，expected `BLOCKED` but was `PASS`。

## Verification

### GREEN command and passing result

- GREEN: `mvn.cmd -pl yudao-module-infra '-Dtest=BackupPlanServiceImplTest,BackupPlanMinimalClosureTest,RuntimeControlBackupPlanOperationGatewayTest,BackupEvidenceExportServiceTest,WindowsBackupPlanSchedulerGatewayTest,RuntimeBackupDrillServiceImplTest,RuntimeControlOperationActionBackupConfirmTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，46 tests。

## Risk and regression scope

- Risk: 手动备份目标环境改为读取计划配置后，缺失或非法 `backup.repositoryEnvironment` 会 fail fast；这是预期门禁，不是 fallback。
- Regression scope: 备份计划保存/启用、手动备份参数传递、审查证据 ZIP PASS/BLOCKED 结论、全量/增量备份合同、恢复演练候选。

## Blockers and follow-up actions

- No code blocker for the static-analysis fixes.
- 生产级真实备份、恢复演练写入和正式证据生成仍需明确 `PROD` 与目标环境前置。
