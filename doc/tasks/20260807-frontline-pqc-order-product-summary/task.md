# 一线 PQC 订单产品信息展示

## Task Goal

一线 PQC 选择活跃生产订单后，在顶部信息栏完整展示正式生产订单号、产品名称和生产数量；仅压缩顶部信息栏字体，保持主检验区触控字号不变。

## Milestones

- [x] M1：建立 BDD、后端接口契约和前端布局 RED 测试。
- [x] M2：扩展活跃订单接口，返回正式工单生产数量并对缺失正式数据 fail fast。
- [x] M3：实现订单摘要卡和顶部栏紧凑响应式布局。
- [x] M4：完成后端静态、前端静态、TypeScript 与真实 Playwright 验证；聚焦 Maven 因共享目标目录持续并发写入未取得可归因的 Surefire 结果，已单独记录。
- [x] M5：归档技能证据、沉淀项目经验并完成任务清理。

## Expected Verification

- MES 后端聚焦 JUnit：活跃订单数量读取、响应映射、缺失/非正数量失败。
- 前端静态契约：订单号、产品名称、产品数量、切换联动、无省略号和紧凑布局。
- 前端 `pnpm ts:check` 与既有 PQC 活跃订单/选择器回归。
- Playwright 真实只读路径：在 1920x1080、1440x900 和 PQC 全屏状态核对接口数据、文字可见性及无重叠。
- backend/frontend evidence validator PASS。

## Applicable Experience Gates

- 保持 PQC 活跃订单来源为所有生产组长 ACTIVE 订单的统一集合，不按当前用户或单一组长缩小范围。
- 活跃订单正式数据缺失必须显式失败，不使用默认数量、额外查询或前端占位掩盖。
- E2E 必须通过真实前端路径，API 只用于只读数据对照；运行端口必须确认属于 `E:\IntRuoyi` 的 `int_main`。
- 静态合同、TypeScript 和真实 Playwright 结果必须分别记录，不互相冒充。
- 顶部固定信息栏的视口验收必须计入左侧导航与页面内边距，记录顶部栏、卡片和值节点的真实 DOM 边界，并在普通页面与业务全屏状态分别采集。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接扩展现有活跃订单正式数据契约，避免选择后再次请求工单详情。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260807-frontline-pqc-order-product-summary/pqc-order-product-summary-real.e2e.cjs
- output/tmp/20260807-frontline-pqc-order-product-summary-backend
- output/tmp/20260807-frontline-pqc-order-product-summary-git
- output/tmp/20260807-frontline-pqc-order-product-summary-git.tar
- output/tmp/20260807-frontline-pqc-order-product-summary-mes
- output/tmp/20260807-frontline-pqc-order-product-summary-runtime-inspect

## Cleanup Keep

- doc/tasks/20260807-frontline-pqc-order-product-summary/task.md
- doc/tasks/20260807-frontline-pqc-order-product-summary/execution-log.md
- doc/tasks/20260807-frontline-pqc-order-product-summary/verification-report.md
- output/playwright/20260807-frontline-pqc-order-product-summary/pqc-order-summary-1440x900.png
- output/playwright/20260807-frontline-pqc-order-product-summary/pqc-order-summary-1920x1080.png
- output/playwright/20260807-frontline-pqc-order-product-summary/pqc-order-summary-fullscreen.png
- output/playwright/20260807-frontline-pqc-order-product-summary/result.json
- output/runtime/int_main/backend-runtime-control-20260807-frontline-pqc-order-product-summary.jar

## Current Status

completed - 功能、静态合同、TypeScript、真实页面和三视口验收已通过；技能证据摘要已归档，项目经验已合并，任务临时产物已清理。聚焦 Maven 的共享目标目录并发缺口保留在验证报告中。
