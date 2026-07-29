# Execution Log

## Intent

- User request: 将批次执行详情页主区域的辅助模式改成与“辅助表单预览”一致。
- Target evidence: 配置页按责任主体显示固定 `12 × 9` 辅助表格；详情页当前仅显示 `87 项`扁平字段列表。

## BDD

- BDD: 批次详情按配置网格预览辅助表单 -> Given 当前批记录执行快照包含按责任主体配置的辅助表格 rowKey 和映射字段, When 用户在批次执行详情页切换到辅助模式, Then 主区域应按责任主体显示与配置预览一致的行列网格, And 已映射字段位于配置坐标, And 未映射格子仍占据原配置位置。
- BDD: 批次详情辅助模式保持只读 -> Given 用户正在查看批次详情辅助网格, When 页面渲染字段和当前值, Then 页面不得提供保存、提交、签名或上传动作。
- BDD: 非辅助网格配置明确阻塞 -> Given 执行快照包含无法解析为正式辅助表格坐标的辅助行, When 用户切换辅助模式, Then 页面不得把这些行伪装成与配置预览一致的网格。

## Initial State

- Date: 2026-07-29.
- Branch: `int_main`.
- Existing workspace state: 当前仓库存在并行任务改动；本任务不回滚、不覆盖，并在提交时使用选择性暂存。
- Root cause: 批次详情模板遍历 `selectedPreviewAssistFields` 渲染卡片列表；字段构建逻辑未解析辅助格 rowKey、未按责任主体分组，也未生成空单元格。

## TDD

- RED: `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> FAIL，首个断言证明详情页缺少当前 `USERS/ROLE` 与旧版 `U` 辅助格 rowKey 解析，仍无法按配置坐标构建网格。
- RED: 后续根因复盘发现只靠 `assistRows` 已映射格无法无损还原配置页 `12 × 9` 外圈空白，配置保存响应和运行快照缺少正式 `assistGridRowCount/assistGridColumnCount`。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> PASS，详情页按责任主体和正式尺寸渲染网格，阻止超出尺寸的辅助格。
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#getAndSaveCellRules_suggestsAndPersistsReviewedTypedMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesAssistRowsInExecutionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.

## Milestone Updates

- Task setup: completed.
- Focused regression contract RED: completed.
- Implementation: completed. 批次详情辅助模式不再遍历扁平字段卡片，改为解析 `ASSIST_GRID_(USERS|ROLE)<id>_R<row>_C<column>` 和旧版 `ASSIST_GRID_U<id>_R<row>_C<column>`，按责任主体构建只读表格，并保留未映射格。
- Formal size chain: completed. 辅助表格尺寸随填写配置保存/回读，运行快照冻结 `assistGridRowCount/assistGridColumnCount`，详情页优先使用正式快照尺寸展开完整网格。
- Verification: completed for static contracts, TypeScript, and targeted backend JUnit.
- Real browser visual check: blocked for current runtime data. 当前登录态打开 `http://localhost:8081/mes/pro/feedback/edhr-batch-execution/detail?id=900000000909`，选择“1 粗洗工序”后右侧为“未配置辅助模式”，没有包含正式辅助表格尺寸的新运行快照，无法用该既有批次证明 `12 × 9` 视觉一致性；未做写入型造数。
- Experience consolidation: completed. 更新 `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁` 和 `docs/experience-index.md`，补充 `assistGridRowCount/assistGridColumnCount` 正式尺寸门禁。
- 2026-07-29 12:53:22 +08:00: Continuation verification completed. Re-ran focused frontend contracts and `pnpm ts:check`; all passed.
- 2026-07-29 12:53:22 +08:00: Cleanup preview/apply completed with no deletions pending; task keeps only `task.md`, `execution-log.md`, and `verification-report.md`.
- 2026-07-29 12:53:22 +08:00: Current repository still has unrelated concurrent dirty files under `20260729-admin-submitted-content-e2e` and two frontend E2E specs; they are outside this task and were not staged here.

## Closeout Evidence

- PASS: `node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> PASS.
- PASS: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS.
- PASS: `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS.
- PASS: `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS.
- PASS: `pnpm ts:check` -> PASS.
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-batch-detail-assist-grid-parity --mode preview` -> PASS, no delete candidates.
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-batch-detail-assist-grid-parity --mode apply` -> PASS, main worktree only.
