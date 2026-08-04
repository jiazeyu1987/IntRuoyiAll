# Frontend Feature Evidence

## Feature Goal

将 QA 规程页面改为 DCC 项目代码驱动的通用配置页，移除页面级压力泵固定来源。

## Non-Goals

- 不实现后端保存和发布。
- 不修改数据库结构。
- 不使用默认项目代码、产品名称推断或 mock 数据。

## Requirements

- `FE-1`：页面提供可搜索的启用 DCC 项目代码选择器。
- `FE-2`：选择后只读展示项目代码、项目名称和 `productMasterId`。
- `FE-3`：产品名称由所选 DCC 项目带出，不允许自由输入。
- `FE-4`：未选择 DCC 项目时发布完整性检查失败。
- `FE-5`：DCC 查询失败时显示可见错误，不加载固定压力泵数据。
- `FE-6`：现有首检、巡检、末检和检验项目配置能力保持。

## Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- API: `IntRuoyiFronted/src/api/dcc/controlledFile/projectCodes.ts`
- Contract: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## API Contract

- `getProjectCodePage({ pageNo, pageSize, status, keyword })`
- Response item: `id`, `projectCode`, `projectName`, `productMasterId`, `status`
- Loading state: 项目选择器显示 loading。
- Empty state: 显示“暂无启用的 DCC 项目代码”。
- Error state: 页面显示错误 Alert，并保留重试入口。

## BDD Scenarios

- `FE-BDD-1`：Given 启用 DCC 项目存在 / When 选择项目 / Then 带出正式产品范围。
- `FE-BDD-2`：Given 未选择项目 / When 发布前检查 / Then 阻塞发布。
- `FE-BDD-3`：Given DCC API 失败 / When 页面初始化 / Then 显式报错且不使用固定示例。

## RED

- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL。
- Expected reason: 页面缺少 `data-qa-regulation-dcc-project`，仍保留固定 `data-qa-regulation-pressure-pump-source`。

## GREEN

- Pending.

## Verification Checklist

- Responsive layout: pending.
- Accessibility labels: pending.
- Loading state: pending.
- Empty state: pending.
- Error state: pending.
- Permission: 复用现有页面路由权限，不新增权限。
- E2E: 本任务先以专用静态契约和 TypeScript 检查验证；不执行写入型 E2E。

## Blockers

- QA 正式保存/发布后端接口仍未接入；页面继续明确提示“未写入后台”。
