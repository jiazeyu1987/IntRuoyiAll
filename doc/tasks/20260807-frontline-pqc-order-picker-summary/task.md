# 一线 PQC 订单选择卡片摘要

## Task Goal

将一线 PQC “选择订单”弹框中的订单卡片改为三行摘要，分别完整显示订单编码、产品名称和生产数量；缩小卡片内部字体，保留现有全部活跃订单集合、订单号搜索、选中态和整卡点击行为。

## Milestones

- [x] M1：建立 BDD、前端契约和 RED 证据。
- [x] M2：实现订单卡片三行摘要及完整文字样式。
- [x] M3：完成静态回归、TypeScript 和真实页面视口验收。
- [x] M4：归档技能证据、沉淀经验并完成任务清理。

## Expected Verification

- 聚焦静态合同：编码、产品、数量三行均来自同一 `FrontlineActiveOrderVO`，数量沿用正式去尾零格式化。
- 既有全部活跃订单搜索、订单选择器布局、订单切换和全屏合同保持通过。
- `pnpm ts:check` 通过。
- Playwright 真实只读路径：打开订单选择器，在 1440x900、1920x1080 和 PQC 全屏状态检查卡片三项信息与接口一致、文字完整可见、卡片不重叠。
- frontend evidence validator PASS。

## Applicable Experience Gates

- 订单候选仍使用全部生产组长 ACTIVE 订单集合，不改变搜索、选中或切换数据源。
- 产品名称和数量直接读取现有正式活跃订单响应，不添加请求或占位 fallback。
- 真实视口验收必须记录卡片与三行值的 DOM 边界、换行和溢出样式，不能只凭截图判断。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；在订单候选结构中保留正式订单对象，由专用三行模板展示同源数据。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260807-frontline-pqc-order-picker-summary/pqc-order-picker-summary-real.e2e.cjs

## Cleanup Keep

- doc/tasks/20260807-frontline-pqc-order-picker-summary/task.md
- doc/tasks/20260807-frontline-pqc-order-picker-summary/execution-log.md
- doc/tasks/20260807-frontline-pqc-order-picker-summary/verification-report.md
- output/playwright/20260807-frontline-pqc-order-picker-summary/pqc-order-picker-1440x900.png
- output/playwright/20260807-frontline-pqc-order-picker-summary/pqc-order-picker-1920x1080.png
- output/playwright/20260807-frontline-pqc-order-picker-summary/pqc-order-picker-fullscreen.png
- output/playwright/20260807-frontline-pqc-order-picker-summary/result.json

## Current Status

completed - 三行订单摘要、紧凑字体、选中态对比度和真实页面多视口验收均已完成；技能证据已归档，项目经验已合并，任务临时产物已清理。
