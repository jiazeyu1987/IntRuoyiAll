# Backend API Evidence - eDHR 金手指批量作废

## Scope

- Endpoint: `POST /mes/pro/edhr-batch-execution/golden-finger/bulk-void`
- Controller: `MesProEdhrBatchExecutionController#goldenFingerBulkVoid`
- Service: `MesProEdhrBatchExecutionService#goldenFingerBulkVoid`
- Direct effect seam: `MesProEdhrBatchVoidEffectService#precheckPlatformVoidBatchExecution` and `executeDirectPlatformVoidBatchExecution`

## API Contract

- Request VO: `EdhrBatchExecutionGoldenFingerBulkVoidReqVO`
  - `filter`: required current batch execution filter, used for cross-page matching.
  - `reasonCategory`: required void reason category.
  - `reasonText`: required void reason text.
  - `password`: required electronic signature password.
  - `comment`: optional remark.
- Response VO: `EdhrBatchExecutionGoldenFingerBulkVoidRespVO`
  - `matchedCount`, `voidedCount`, `skippedCount`, `items`.
  - Item records batch execution ID/code/status/result/message/change event ID.

## Auth And Validation

- Controller permission: `mes:pro-batch-record-execution:golden-finger`.
- Service-level role/permission guard: `MesProEdhrGoldenFingerPermissionService`.
- Fail-fast validation rejects missing filter/reason/password and no voidable candidates.
- Terminal statuses are excluded from direct void candidates and reported as skipped terminal rows.
- No global BPM/process configuration is modified.

## BDD Scenarios

- `BDD: 金手指批量直通作废 -> Given 当前用户具备金手指权限且筛选结果包含可作废批次 / When 调用批量作废接口 / Then 服务按当前筛选预检并执行直通作废，返回作废与跳过数量`
- `BDD: 非金手指不可调用 -> Given 当前用户不具备金手指权限 / When 调用批量作废接口 / Then 服务拒绝且不查询批次、不执行直通作废`
- `BDD: 无可作废候选失败 -> Given 当前筛选结果只有终态批次 / When 调用批量作废接口 / Then 返回明确失败，不返回默认成功或部分成功假象`

## RED Evidence

- `RED: mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidContractTest,MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> FAIL, 缺少 bulk void VO、服务方法、错误码和接口契约。`

## GREEN Evidence

- `GREEN: mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidContractTest,MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。`

## Contract Verification

- Contract test verifies endpoint path, `POST`, permission annotation, service method and VO getters/setters.
- Service test verifies non-golden-finger rejection, current-filter candidate selection, terminal skips, precheck-before-execute and fail-fast empty candidate behavior.

## Observability And Audit

- Bulk path reuses existing direct platform void effect, preserving signature validation, reason/comment, change event and audit behavior.
- The new service does not create BPM approval requests and does not change global approval configuration.

## Blockers

- Real browser E2E was not run because local services, a confirmed gold-finger test account and traceable writable batch data were not established in this turn.