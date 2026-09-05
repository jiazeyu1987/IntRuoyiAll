# 活跃订单工序提交详情独立页面

## Task Goal

点击活跃订单池里的“详情”时，进入独立页面展示该活跃订单的一线生产提交、一线 PQC 提交和领料单信息，不再以弹框形式展示。

## Milestones

1. 建立 BDD/TDD 任务记录和静态合同测试。
2. 抽取现有详情展示为可复用页面组件。
3. 将列表“详情”入口改为路由跳转，新增独立详情页路由。
4. 运行静态合同、类型检查和必要前端验证。
5. 将详情页下方表格调整为标准列表壳层，页面主体不得横向溢出。

## Expected Verification

- 静态合同测试证明“详情”使用路由跳转，不再显示详情弹窗。
- `pnpm --dir IntRuoyiFronted ts:check` 通过。
- frontend-feature evidence validator 通过。
- 详情页表格布局静态合同通过，禁止表格列和工序 Tab 撑破页面宽度。

## Current Status

completed - 实现提交 `5c969a5ff` 已进入 `int_main`，cleanup apply 已删除本任务临时 evidence，保留 task/execution/verification 记录。

## Cleanup Candidates
- doc/tasks/20260905-active-order-detail-standalone-page/frontend-feature-evidence.md

## 设计约束检查

- 不改后端 API 合同。
- 不引入 mock 数据或 fallback。
- 保留现有详情页内容结构，包括生产/PQC 主 Tab、各自工序 Tab、领料单展示和设备信息展示。
