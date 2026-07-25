# Execution Log

## 2026-07-25

- User intent: 红框区域当前显示 `-`，期望显示 `来料检报告`。
- Skill: 使用 `bug-regression-fix-loop`，按缺陷复现、RED/GREEN、最小修复执行。
- Trigger docs read: `docs\task-closeout-rules.md`、`docs\frontend-development.md`、`docs\powershell-memory.md`、`docs\powershell-encoding.md`。
- Dirty baseline: 发现任务开始前已有 4 个脏文件，已单独提交基线 `a9b3b74e`，文件清单：
  - `IntRuoyiFronted/tests/e2e/edhr-full-chain-evidence-pack-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js`
  - `doc/tasks/20260725-full-e2e-admin-validation/execution-log.md`
  - `doc/tasks/20260725-full-e2e-admin-validation/task.md`
- Experience gate: `docs\experience-index.md` 未发现；本任务为小范围前端显示修复，记录后继续。
- BDD: 来料检报告节点标题显示 -> Given 当前附件节点类型/名称为来料检报告, When 页面渲染节点头部标题区域, Then 标题区域显示 `来料检报告` 而不是 `-`。
- Root cause: `resolveTaskDisplayName` 对特殊节点仍先读取 `batchRecordReportName/batchRecordReportCode/batchRecordReportId`，当这些字段来自占位值时会覆盖 `specialNodeLabels` 中的 `来料检报告`。
- RED: `node tests\e2e\edhr-special-node-attachment-actions-static.spec.js` -> FAIL, expected reason: 新增标题优先级断言报错 `特殊节点标题必须优先显示来料检报告等节点业务名称，不能被空报告字段占位符覆盖。`
- Implementation: 在 `BatchExecutionDetailPage.vue` 中让特殊节点优先解析 `specialNodeLabels[row.nodeType]`，普通表单继续沿用原报告字段优先级。
- Regression test: 新增 `IntRuoyiFronted/tests/e2e/edhr-special-node-display-name-static.spec.js`，独立覆盖当前节点附件头部和特殊节点显示名称优先级。
- GREEN: `node tests\e2e\edhr-special-node-display-name-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- Note: 旧的 `edhr-special-node-attachment-actions-static.spec.js` 在新增断言通过后继续暴露既存的其它契约失败，因此未作为本次完成门禁；本次门禁改为独立标题回归测试和前端类型检查。
- Commit note: 本任务实现文件已被并发基线提交 `8e9e4ba6` 纳入；任务文档初始记录已被并发基线提交 `d719203b` 纳入。
- Closeout blocker: 当前工作区仍有并发任务改动与本地 ahead 提交，不能安全执行最终推送或全量 closeout。
