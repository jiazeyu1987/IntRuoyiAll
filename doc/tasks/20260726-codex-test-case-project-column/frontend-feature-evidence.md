# Frontend Feature Evidence

## Feature Goal

- 测试管理列表新增 `项目` 列，展示当前测试项所属项目。

## Non-goals

- 不调整测试记录页面。
- 不改变 Runner 执行逻辑。
- 不引入前端 mock 分类。

## Acceptance

- 测试管理标准列表模板展示 `项目` 列。
- 快速过滤支持按 `项目` 查询。
- 新增/编辑测试项时 `项目` 为必填项，选项仅为 `智能排产`、`文控`、`批记录`。
- 旧数据尚未回填 `project` 时，列表仍按测试项名称、方法和目标项解析显示三值项目名，不渲染空标签。

## BDD

- BDD: 测试管理列表展示项目归属 -> Given 当前测试管理已有测试项, When 用户打开测试管理列表, Then 列表以标准列表模板展示 `项目` 列且每个当前测试项归属三类项目之一。

## Verification

- `pnpm e2e:system:codex-test-management:static` -> PASS。
- `pnpm ts:check` -> PASS。
- 用户截图反馈后的复验：`pnpm e2e:system:codex-test-management:static` -> PASS；`pnpm ts:check` -> PASS。

## Evidence

- Requirements: 列表新增 `项目` 列；当前项目限定为 `智能排产`、`文控`、`批记录`。
- Entry point: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`，标准列表模板 `system.codexTestManagement.cases`。
- API contract: `CodexTestProject = '智能排产' | '文控' | '批记录'`；`CodexTestCaseVO.project` 和分页 `project` 过滤已暴露。
- UI states: 表格列、快速过滤和新增/编辑表单均使用同一项目选项；项目为表单必填项。
- Display behavior: 项目列使用 `resolveCaseProject(row)` 输出 `智能排产`、`批记录`、`文控`，避免旧数据空 project 显示为空标签。
- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，项目字段缺失。
- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- Regression: `pnpm ts:check` -> PASS。
- Blockers: 未执行真实 Runner/E2E；本任务只改列表展示和数据契约，不启动真实自动测试流程。
