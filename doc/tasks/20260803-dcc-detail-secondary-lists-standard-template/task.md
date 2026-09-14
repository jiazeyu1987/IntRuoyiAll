# DCC 详情次级三列表标准模板改造

## Task Goal

将 DCC 受控文件详情页中的三块列表改为标准列表模板：受控打印记录、培训状态、签核追溯。列表应使用统一 `UnifiedListTemplate`、稳定 table key、显示字段配置和列宽持久化，保持原业务字段、导出/打印和行级操作不变。

## Milestones

- [x] 建立任务文档并记录当前共享分支脏工作区边界。
- [x] 增加 RED 静态合同，证明三块列表尚未全部接入标准列表模板。
- [x] 将受控打印记录、培训状态、签核追溯迁移到 `UnifiedListTemplate`。
- [x] 运行定向静态合同、相邻合同和 `pnpm ts:check`。
- [x] 完成验证报告和收尾记录。

## Expected Verification

- `node tests/e2e/dcc-detail-secondary-lists-standard-template-static.spec.js`
- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `node tests/e2e/dcc-detail-trace-lists-standard-template-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260803-dcc-detail-secondary-lists-standard-template\frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用现有标准列表模板统一列表工具栏、列配置和表格承载方式。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：先新增聚焦静态合同，再做最小模板迁移。
- 适用 `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`：三块列表必须使用稳定 table key、`useUserTableColumns` 和显式表格标识。
- 适用 `docs/frontend-development.md#vue-sfc-泛型箭头函数解析门禁`：本次若新增泛型工具函数，必须使用命名函数而不是 `.vue` 中的泛型箭头函数。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：当前共享分支存在大量非本任务脏文件，所有暂存/提交必须显式路径。

## Dirty Workspace Boundary

- 开始本任务时 `git status --short --branch --untracked-files=all` 显示大量非本任务改动，且分支 `int_main` 已 ahead `origin/int_main`。
- 本任务仅修改任务自有文件、目标详情页和任务专用静态合同；不得 `git add -A` 或混入其它任务文件。
