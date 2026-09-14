# DCC 详情追溯三列表标准模板改造

## Task Goal

将 DCC 受控文件详情页截图中的三块列表改为标准列表模板：审批路线快照、版本历史、分发状态。列表应使用统一 `UnifiedListTemplate`、稳定 table key、显示字段配置和列宽持久化，避免当前普通表格被全局增强按钮悬浮遮挡。

## Milestones

- [x] 建立任务文档并记录既有脏工作区基线。
- [x] 增加 RED 静态合同，证明三块列表尚未接入标准列表模板。
- [x] 将三块详情列表迁移到 `UnifiedListTemplate` 并保留现有展示/操作逻辑。
- [x] 运行定向静态合同和 `pnpm ts:check`。
- [x] 完成验证报告和收尾记录。

## Expected Verification

- `node tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js`
- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `node tests/e2e/dcc-detail-version-successor-summary-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## Completed Work

- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`：审批路线快照、版本历史、分发状态三块主详情列表已接入 `UnifiedListTemplate`。
- 三块列表均使用稳定 table key、`useUserTableColumns`、显式列配置标记、列宽拖拽持久化和本地分页行源。
- 保留原有版本历史后继版本摘要 helper、分发导出/打印回执按钮和分发行级签收/回收操作。
- `IntRuoyiFronted/tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js`：新增聚焦静态合同。
- `IntRuoyiFronted/tests/e2e/dcc-detail-version-successor-summary-static.spec.js`：收窄版本预览弹窗截取终点，避免误扫后续受控打印弹窗表单。

## Verification Result

- PASS: `node tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js`
- PASS: `node tests/e2e/dcc-traceability-ux-static.spec.js`
- PASS: `node tests/e2e/dcc-detail-version-successor-summary-static.spec.js`
- PASS: `pnpm ts:check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用现有标准列表模板统一列表工具栏、列配置和表格承载方式。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：先新增聚焦静态合同，再做最小模板迁移。
- 适用 `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`：三块列表必须使用稳定 table key 和 `useUserTableColumns`，不能只依赖全局增强按钮。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：当前共享分支存在并发任务，所有暂存/提交必须显式路径。
