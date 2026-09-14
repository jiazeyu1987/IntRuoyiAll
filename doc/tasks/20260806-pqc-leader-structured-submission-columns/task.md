# PQC 组长列表结构化提交字段调整

## Task Goal

- 删除 PQC 管理提交列表中的一线PQC表单、审核副本、过程检验汇集、复核判定列。
- PQC 列表必须用结构化列展示一线提交数据，不能把一线表单压成一整段。
- 结构化列必须覆盖生产工单、产品、检验类型/轮次、检验项、检验数量、损耗数量、损耗明细/不良说明、设备、设备编号、接收标准、检验方法、检验判定、参数明细和逐件/样本值。
- 参数明细与逐件/样本值职责拆开：参数明细展示检验项配置上下文，逐件/样本值展示一线实际填写的每件样本。
- 参数值超出冻结上下限时继续标红提醒，但不阻止提交。

## Milestones

- [x] 建立任务目录、BDD 场景和验收口径。
- [x] 更新静态合同，使旧的一整段一线PQC表单列和追溯/汇集/复核列先 RED。
- [x] 修改 PQC 组长提交列表列配置和渲染，删除非当前列表职责列并补齐结构化字段。
- [x] 拆分参数明细和逐件/样本值，避免参数明细重复 30 件样本值。
- [x] 运行聚焦合同、相邻合同、类型检查和 diff hygiene。
- [x] 更新验证报告和收尾阻塞状态。

## Expected Verification

- `node tests\e2e\pqc-leader-list-fill-form-parity-static.spec.js`
- `node tests\e2e\pqc-submission-structured-columns-static.spec.js`
- `node tests\e2e\pqc-leader-item-snapshot-static.spec.js`
- `node tests\e2e\pqc-leader-standard-list-template-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-pqc-leader-structured-submission-columns\frontend-feature-evidence.md`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-leader-structured-submission-columns`

## BDD Scenarios

- BDD: PQC 列表移除整段表单列 -> Given PQC 组长查看提交列表 / When 一线提交记录包含多个检验项 / Then 列表不得显示“一线PQC表单”整段列，而应按结构化列展示提交事实。
- BDD: PQC 列表删除非本列表职责列 -> Given PQC 管理列表只用于查看提交数据和操作 / When 渲染列头 / Then 不显示审核副本、过程检验汇集、复核判定列。
- BDD: PQC 结构化字段不遗漏 -> Given 一线 PQC 提交包含工单、产品、检验项、设备、标准、方法、判定、数量、损耗、不良说明和样本值 / When PQC 组长查看列表 / Then 每类数据都有独立结构化列或现有结构化列承载，超限样本值标红提醒。
- BDD: PQC 参数明细不重复样本值 -> Given 一线 PQC 提交包含长度、压力、外观 30 件样本 / When PQC 管理列表渲染提交行 / Then 参数明细只按检验项展示配置上下文，逐件/样本值才展示每件样本并标红超限值。

## Applicable Gates

- 前端功能交付：BDD -> RED -> GREEN -> REGRESSION，保持现有 API 和列表模板。
- MES PQC 项目级检验快照门禁：PQC 列表从 `pqcItemDetails/itemResults` 和 rawPayload 冻结快照读取设备、编号、标准、方法、上下限和样本值。
- 前端列表跨账号默认列布局统一门禁：默认列配置必须移除不需要的列 key，避免历史列配置继续带出旧列。
- UTF-8/PowerShell 门禁：中文文档和测试按 UTF-8 处理，PowerShell 不使用 `&&`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按一线 PQC 正式提交快照拆成结构化列展示。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

- 已按用户新口径删除一线PQC表单、审核副本、过程检验汇集、复核判定列，并补齐 PQC 结构化提交字段。
- 已继续修正参数明细列，避免与逐件/样本值列重复展示 30 件样本。
- Git 收尾阻塞：当前分支落后 origin 且工作区存在并行脏改动，本轮不执行合并、提交或推送。
