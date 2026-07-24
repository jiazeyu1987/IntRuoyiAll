# Bug Regression Evidence

## Bug

eDHR 创建批次执行在存在已发布工艺路线和草稿配置并存时，会错误依赖当前草稿/当前配置解析批记录任务，导致报错“缺少工艺流程批记录配置流程配置或默认批记录”。用户明确要求创建时读取最新已发布 ACTIVE 工艺路线，创建后冻结，与草稿无关。

## Expected

创建批次执行必须选择当时最新 ACTIVE 工艺路线版本，持久化 `routeVersionId`、`routeVersionNo` 和 `routeSnapshotJson`；批次任务只从该批次冻结快照生成，不再读取当前草稿配置。

## Reproduction

- RED: `mvn.cmd '-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionBatchRecordReportsInsteadOfCurrentDraft' '-Dsurefire.failIfNoSpecifiedTests=false' test -pl yudao-module-mes -am` -> FAIL，旧逻辑抛出 `PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED / 1040750403`。
- RED: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> FAIL，未加载新后端前真实路径无法验证修复；加载后 `route 922185` 进入正式填写规则校验 `1040750243`，不再复现原始缺失配置错误。

## Root Cause

创建批次时虽已持久化路线版本字段，但传统批记录任务解析没有完整支持发布快照中的 `batchRecordReports`，仍会落回当前配置/草稿相关链路；动态表单和传统批记录的上下文校验也混用，导致传统报表任务在草稿配置漂移后被误判缺少默认批记录。

## Regression Test

- `MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionBatchRecordReportsInsteadOfCurrentDraft`
- `MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft`
- `MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask`
- `MesProEdhrBatchExecutionRouteVersionFreezeTest`
- `tests\e2e\edhr-batch-execution-real-flow.e2e.js`

## GREEN

- GREEN: `mvn.cmd '-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionBatchRecordReportsInsteadOfCurrentDraft' '-Dsurefire.failIfNoSpecifiedTests=false' test -pl yudao-module-mes -am` -> PASS。
- GREEN: `mvn.cmd '-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft,MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask,MesProEdhrBatchExecutionRouteVersionFreezeTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -pl yudao-module-mes -am` -> PASS。
- GREEN: `node yudao-module-mes\src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS。
- GREEN: `mvn.cmd -DskipTests package -pl yudao-server -am` -> PASS。
- GREEN: `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实创建批次 `900000000787 / BRS20260724195134`。

## Verification

- PASS: 后端 Jar 已加载到 `48081`，健康检查返回 `{"status":"UP"}`，目标 Jar SHA256 与隔离构建 Jar 一致。
- PASS: 数据库只读核验显示批次 `900000000787` 持久化 `route_id=922186`、`route_version_id=239`、`route_version_no=V2`、`route_snapshot_json` 长度 `40670`。
- PASS: 同一路线 `922186` 当前仍存在 `open_draft_count=1`，但新批次冻结 ACTIVE `239 / V2`。
- PASS: 批次任务 `8` 个，传统批记录任务 `4` 个，已打开执行任务 `4` 个，`blocked_count=0`。

## Risk

修复不引入 fallback、降级或吞异常；传统批记录仍保留正式填写规则校验。E2E 使用测试租户数据，批次号具备 `BRS20260724` 任务前缀，可追踪。

## Blockers

实现和验证已完成；收尾仍待 cleanup、经验沉淀、提交与推送门禁。当前 worktree 存在既有非本任务脏文件，提交前必须按项目规则单独处理。
